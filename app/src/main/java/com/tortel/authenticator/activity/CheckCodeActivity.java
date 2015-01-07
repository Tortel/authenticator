/*
 * Copyright 2009 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tortel.authenticator.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.tortel.authenticator.AccountDb;
import com.tortel.authenticator.Base32String;
import com.tortel.authenticator.Base32String.DecodingException;
import com.tortel.authenticator.PasscodeGenerator;
import com.tortel.authenticator.R;
import com.tortel.authenticator.testability.DependencyInjector;

import java.security.GeneralSecurityException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * The activity that displays the integrity check value for a key. The user is
 * passed in via the extra bundle in "user".
 *
 * @author sweis@google.com (Steve Weis)
 */
public class CheckCodeActivity extends ActionBarActivity {
    public static final String EXTRA_ID = "id";

    private TextView mCheckCodeTextView;
    private TextView mCodeTextView;
    private TextView mCounterValue;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.check_code);
        mCodeTextView = (TextView) findViewById(R.id.code_value);
        mCheckCodeTextView = (TextView) findViewById(R.id.check_code);
        mCounterValue = (TextView) findViewById(R.id.counter_value);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        int id = extras.getInt(EXTRA_ID);

        AccountDb accountDb = DependencyInjector.getAccountDb();
        AccountDb.OtpType type = accountDb.getType(id);
        if (type == AccountDb.OtpType.HOTP) {
            mCounterValue.setText(accountDb.getCounter(id).toString());
            findViewById(R.id.counter_area).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.counter_area).setVisibility(View.GONE);
        }

        String secret = accountDb.getSecret(id);
        String checkCode = null;
        String errorMessage = null;
        try {
            checkCode = getCheckCode(secret);
        } catch (GeneralSecurityException e) {
            errorMessage = getString(R.string.general_security_exception);
        } catch (DecodingException e) {
            errorMessage = getString(R.string.decoding_exception);
        }
        if (errorMessage != null) {
            mCheckCodeTextView.setText(errorMessage);
            return;
        }
        mCodeTextView.setText(checkCode);
        String checkCodeMessage = String.format(getString(R.string.check_code),
                TextUtils.htmlEncode(accountDb.getEmail(id)));
        CharSequence styledCheckCode = Html.fromHtml(checkCodeMessage);
        mCheckCodeTextView.setText(styledCheckCode);
        mCheckCodeTextView.setVisibility(View.VISIBLE);
        findViewById(R.id.code_area).setVisibility(View.VISIBLE);
    }

    static String getCheckCode(String secret) throws GeneralSecurityException, DecodingException {
        final byte[] keyBytes = Base32String.decode(secret);
        Mac mac = Mac.getInstance("HMACSHA1");
        mac.init(new SecretKeySpec(keyBytes, ""));
        PasscodeGenerator pcg = new PasscodeGenerator(mac);
        return pcg.generateResponseCode(0L);
    }

}
