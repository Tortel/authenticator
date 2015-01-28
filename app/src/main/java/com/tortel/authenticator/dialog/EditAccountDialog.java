package com.tortel.authenticator.dialog;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.tortel.authenticator.R;
import com.tortel.authenticator.activity.MainActivity;
import com.tortel.authenticator.common.data.AccountDb;
import com.tortel.authenticator.common.utils.DependencyInjector;

/**
 * Dialog for editing an account
 */
public class EditAccountDialog extends DialogFragment {
    public static final String USERNAME = "user";
    public static final String ID = "id";

    private String username;
    private int id;

    private EditText mEditText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        Bundle args = getArguments();
        username = args.getString(USERNAME);
        id = args.getInt(ID);
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        // Save the username during rotation
        username = mEditText.getText().toString();

        super.onDestroyView();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        builder.title(getString(R.string.rename_message, username));

        mEditText = new EditText(getActivity());
        mEditText.setText(username);

        builder.customView(mEditText, false);

        builder.positiveText(R.string.ok);
        builder.negativeText(R.string.cancel);
        builder.callback(callback);

        return builder.build();
    }

    private MaterialDialog.ButtonCallback callback = new MaterialDialog.ButtonCallback() {
        @Override
        public void onNegative(MaterialDialog materialDialog) {
            dismiss();
        }

        @Override
        public void onPositive(MaterialDialog materialDialog) {
            String newUsername = mEditText.getText().toString().trim();
            // Make sure not to save it as blank
            if("".equals(newUsername)){
                Toast.makeText(getActivity(), R.string.invalid_name, Toast.LENGTH_SHORT).show();
                return;
            }

            AccountDb accountDb = DependencyInjector.getAccountDb();
            accountDb.setEmail(id, newUsername);

            Intent intent = new Intent(MainActivity.ACCOUNT_CHANGED);
            intent.putExtra(MainActivity.ACCOUNT_ID, id);
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);

            dismiss();
        }
    };
}
