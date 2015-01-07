package com.tortel.authenticator.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;

import com.tortel.authenticator.AccountDb;
import com.tortel.authenticator.AuthenticatorActivity;
import com.tortel.authenticator.Base32String;
import com.tortel.authenticator.R;

/**
 * Activity for handling the flow of adding a new account
 */
public class AddAccountActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fragment_activity);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FragmentManager fragmentManager = getSupportFragmentManager();
        if(fragmentManager.findFragmentById(R.id.content_frame) == null){
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, new NewAccountFragment()).commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class NewAccountFragment extends Fragment implements View.OnClickListener, TextWatcher {
        private static final int MIN_KEY_BYTES = 10;
        private EditText accountName;
        private EditText accountCode;
        private boolean timeBased = true;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.new_account, container, false);
            accountName = (EditText) view.findViewById(R.id.create_account_name);
            accountCode = (EditText) view.findViewById(R.id.create_account_code);

            Button button = (Button) view.findViewById(R.id.button_scan_barcode);
            button.setOnClickListener(this);
            button = (Button) view.findViewById(R.id.button_add_account);
            button.setOnClickListener(this);

            RadioButton radio = (RadioButton) view.findViewById(R.id.create_account_counter_base);
            radio.setOnClickListener(this);
            radio = (RadioButton) view.findViewById(R.id.create_account_time_base);
            radio.setOnClickListener(this);

            return view;
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.button_scan_barcode:
                    Activity activity = getActivity();
                    Intent intent = AuthenticatorActivity.getLaunchIntentActionScanBarcode(activity);
                    activity.startActivity(intent);
                    activity.finish();
                    return;
                case R.id.button_add_account:
                    addAccount();
                    return;
                case R.id.create_account_time_base:
                    timeBased = true;
                    return;
                case R.id.create_account_counter_base:
                    timeBased = false;
                    return;
            }
        }

        private void addAccount() {
            // Verify the account name
            String name = accountName.getText().toString().trim();
            if("".equals(name)){
                accountName.setError(getString(R.string.no_name));
                return;
            } else {
                accountName.setError(null);
            }

            AccountDb.OtpType mode = timeBased ? AccountDb.OtpType.TOTP : AccountDb.OtpType.HOTP;

            // Verify the key
            if (validateKeyAndUpdateStatus(true)) {
                AuthenticatorActivity.saveSecret(getActivity(), null,
                        name, getEnteredKey(), mode,
                        AccountDb.DEFAULT_HOTP_COUNTER);
                getActivity().finish();
            }
        }

        /*
         * Return key entered by user, replacing visually similar characters 1 and 0.
         */
        private String getEnteredKey() {
            String enteredKey = accountCode.getText().toString();
            return enteredKey.replace('1', 'I').replace('0', 'O');
        }

        /*
         * Verify that the input field contains a valid base32 string,
         * and meets minimum key requirements.
         */
        private boolean validateKeyAndUpdateStatus(boolean submitting) {
            String userEnteredKey = getEnteredKey();
            try {
                byte[] decoded = Base32String.decode(userEnteredKey);
                if (decoded.length < MIN_KEY_BYTES) {
                    // If the user is trying to submit a key that's too short, then
                    // display a message saying it's too short.
                    accountCode.setError(submitting ? getString(R.string.enter_key_too_short) : null);
                    return false;
                } else {
                    accountCode.setError(null);
                    return true;
                }
            } catch (Base32String.DecodingException e) {
                accountCode.setError(getString(R.string.enter_key_illegal_char));
                return false;
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // Ignore
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Ignore
        }

        @Override
        public void afterTextChanged(Editable s) {
            validateKeyAndUpdateStatus(false);
        }
    }
}
