package com.tortel.authenticator.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AlertDialog;

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

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.save_key_message);
        builder.setMessage(user);
        builder.setPositiveButton(R.string.ok, callback);
        builder.setNegativeButton(R.string.cancel, callback);

        return builder.create();
    }

    private DialogInterface.OnClickListener callback = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                AccountDb accountDb = DependencyInjector.getAccountDb();
                accountDb.update(null, user, secret, type, counter);

                // Send the notification
                Intent intent = new Intent(MainActivity.ACCOUNT_CREATED);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);

                dismiss();
                getActivity().finish();
            } else {
                dismiss();
            }
        }
    };
}
