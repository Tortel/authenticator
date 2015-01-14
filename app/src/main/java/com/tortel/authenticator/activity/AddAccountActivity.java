package com.tortel.authenticator.activity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
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
import com.tortel.authenticator.dialog.ConfirmSaveDialog;
import com.tortel.authenticator.dialog.DownloadScannerDialog;
import com.tortel.authenticator.dialog.InvalidQRDialog;
import com.tortel.authenticator.dialog.InvalidSecretDialog;
import com.tortel.authenticator.utils.Base32String;
import com.tortel.authenticator.R;
import com.tortel.authenticator.utils.Log;
import com.tortel.authenticator.utils.Utilities;

import java.util.Locale;

/**
 * Activity for handling the flow of adding a new account
 */
public class AddAccountActivity extends ActionBarActivity {
    // Scan barcode request id
    private static final int SCAN_REQUEST = 31337;

    private static final String OTP_SCHEME = "otpauth";
    private static final String TOTP = "totp"; // time-based
    private static final String HOTP = "hotp"; // counter-based
    private static final String SECRET_PARAM = "secret";
    private static final String COUNTER_PARAM = "counter";

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("AddAccountActivity onActivityResult");
        if (requestCode == SCAN_REQUEST && resultCode == Activity.RESULT_OK) {
            // Grab the scan results and convert it into a URI
            String scanResult = (data != null) ? data.getStringExtra("SCAN_RESULT") : null;
            Uri uri = (scanResult != null) ? Uri.parse(scanResult) : null;
            interpretScanResult(uri);
        }
    }

    /**
     * Interprets the QR code that was scanned by the user. Decides whether to
     * launch the key provisioning sequence or the OTP seed setting sequence.
     *
     * @param scanResult        a URI holding the contents of the QR scan result
     */
    private void interpretScanResult(Uri scanResult) {
        // The scan result is expected to be a URL that adds an account.

        // Sanity check
        if (scanResult == null) {
            showInvalidQRDialog();
            return;
        }

        // See if the URL is an account setup URL containing a shared secret
        if (OTP_SCHEME.equals(scanResult.getScheme()) && scanResult.getAuthority() != null) {
            parseSecret(scanResult);
        } else {
            showInvalidQRDialog();
        }
    }

    /**
     * Shows the invalid QR code dialog
     */
    private void showInvalidQRDialog(){
        DialogFragment dialog = new InvalidQRDialog();
        dialog.show(getSupportFragmentManager(), "error");
    }

    /**
     * Shows the invalid secret dialog
     */
    private void showInvalidSecretDialog(){
        DialogFragment dialog = new InvalidSecretDialog();
        dialog.show(getSupportFragmentManager(), "error");
    }

    /**
     * Parses a secret value from a URI. The format will be:
     * <p/>
     * otpauth://totp/user@example.com?secret=FFF...
     * otpauth://hotp/user@example.com?secret=FFF...&counter=123
     *
     * @param uri               The URI containing the secret key
     */
    private void parseSecret(Uri uri) {
        final String scheme = uri.getScheme().toLowerCase(Locale.ENGLISH);
        final String path = uri.getPath();
        final String authority = uri.getAuthority();
        final String user;
        final String secret;
        final AccountDb.OtpType type;
        final int counter;

        if (!OTP_SCHEME.equals(scheme)) {
            Log.e("Invalid or missing scheme in uri");
            showInvalidQRDialog();
            return;
        }

        if (TOTP.equals(authority)) {
            type = AccountDb.OtpType.TOTP;
            counter = AccountDb.DEFAULT_HOTP_COUNTER; // only interesting for
            // HOTP
        } else if (HOTP.equals(authority)) {
            type = AccountDb.OtpType.HOTP;
            String counterParameter = uri.getQueryParameter(COUNTER_PARAM);
            if (counterParameter != null) {
                try {
                    counter = Integer.parseInt(counterParameter);
                } catch (NumberFormatException e) {
                    Log.e("Invalid counter in uri");
                    showInvalidQRDialog();
                    return;
                }
            } else {
                counter = AccountDb.DEFAULT_HOTP_COUNTER;
            }
        } else {
            Log.e("Invalid or missing authority in uri");
            showInvalidQRDialog();
            return;
        }

        user = validateAndGetUserInPath(path);
        if (user == null) {
            Log.e("Missing user id in uri");
            showInvalidQRDialog();
            return;
        }

        secret = uri.getQueryParameter(SECRET_PARAM);

        if (secret == null || secret.length() == 0) {
            Log.e("Secret key not found in URI");
            showInvalidSecretDialog();
            return;
        }

        if (AccountDb.getSigningOracle(secret) == null) {
            Log.e("Invalid secret key");
            showInvalidSecretDialog();
            return;
        }

        // Set up the dialog's arguments
        Bundle args = new Bundle();
        args.putString(ConfirmSaveDialog.USER, user);
        args.putString(ConfirmSaveDialog.SECRET, secret);
        args.putSerializable(ConfirmSaveDialog.TYPE, type);
        args.putInt(ConfirmSaveDialog.COUNTER, counter);

        DialogFragment dialog = new ConfirmSaveDialog();
        dialog.setArguments(args);

        // Show it
        getSupportFragmentManager().beginTransaction()
                .add(dialog, "save").commitAllowingStateLoss();
    }

    /**
     * Gets the username from the path
     * @param path
     * @return
     */
    private String validateAndGetUserInPath(String path) {
        if (path == null || !path.startsWith("/")) {
            return null;
        }
        // path is "/user", so remove leading "/", and trailing white spaces
        String user = path.substring(1).trim();
        if (user.length() == 0) {
            return null; // only white spaces.
        }
        return user;
    }

    /**
     * Starts the scan intent, or prompts the user to download the scanner app
     */
    public void startScanIntent(){
        Log.d("Starting scan barcode intent");

        Intent intentScan = new Intent("com.google.zxing.client.android.SCAN");
        intentScan.putExtra("SCAN_MODE", "QR_CODE_MODE");
        intentScan.putExtra("SAVE_HISTORY", false);
        try {
            startActivityForResult(intentScan, SCAN_REQUEST);
        } catch (ActivityNotFoundException error) {
            DialogFragment dialog = new DownloadScannerDialog();
            dialog.show(getSupportFragmentManager(), "download");
        }
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
                    AddAccountActivity activity = (AddAccountActivity) getActivity();
                    activity.startScanIntent();
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
