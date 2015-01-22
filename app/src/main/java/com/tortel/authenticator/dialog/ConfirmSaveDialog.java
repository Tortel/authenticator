package com.tortel.authenticator.dialog;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;

import com.afollestad.materialdialogs.MaterialDialog;
import com.tortel.authenticator.R;
import com.tortel.authenticator.activity.MainActivity;
import com.tortel.authenticator.common.data.AccountDb;
import com.tortel.authenticator.common.utils.DependencyInjector;

/**
 * Dialog which asks for confirmation before saving an account
 */
public class ConfirmSaveDialog extends DialogFragment {
    public static final String USER = "user";
    public static final String SECRET = "secret";
    public static final String TYPE = "type";
    public static final String COUNTER = "counter";

    private String user;
    private String secret;
    private AccountDb.OtpType type;
    private int counter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        Bundle args = getArguments();
        user = args.getString(USER);
        secret = args.getString(SECRET);
        type = (AccountDb.OtpType) args.getSerializable(TYPE);
        counter = args.getInt(COUNTER);
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        builder.title(R.string.save_key_message);
        builder.content(user);
        builder.positiveText(R.string.ok);
        builder.negativeText(R.string.cancel);
        builder.callback(callback);

        return builder.build();
    }

    private MaterialDialog.ButtonCallback callback = new MaterialDialog.Callback() {
        @Override
        public void onNegative(MaterialDialog materialDialog) {
            dismiss();
        }

        @Override
        public void onPositive(MaterialDialog materialDialog) {
            AccountDb accountDb = DependencyInjector.getAccountDb();
            accountDb.update(null, user, secret, type, counter);

            // Send the notification
            Intent intent = new Intent(MainActivity.ACCOUNT_CREATED);
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);

            dismiss();
            getActivity().finish();
        }
    };
}
