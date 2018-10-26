package com.tortel.authenticator.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AlertDialog;
import android.text.Html;
import android.widget.TextView;

import com.tortel.authenticator.R;
import com.tortel.authenticator.activity.MainActivity;
import com.tortel.authenticator.common.data.AccountDb;
import com.tortel.authenticator.common.utils.DependencyInjector;

/**
 * Dialog that confirms and deletes an account
 */
public class ConfirmDeleteDialog extends DialogFragment {
    public static final String USERNAME = "user";
    public static final String ID = "id";

    private String username;
    private int id;

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
        super.onDestroyView();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.remove_account_dialog_title, username));

        TextView textView = new TextView(getActivity());
        textView.setText(Html.fromHtml(getString(R.string.remove_account_dialog_message)));

        builder.setView(textView);

        builder.setPositiveButton(R.string.ok, callback);
        builder.setNegativeButton(R.string.cancel, callback);

        return builder.create();
    }

    private DialogInterface.OnClickListener callback = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                AccountDb accountDb = DependencyInjector.getAccountDb();
                accountDb.delete(id);
                Intent intent = new Intent(MainActivity.ACCOUNT_DELETED);
                intent.putExtra(MainActivity.ACCOUNT_ID, id);
                LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
                broadcastManager.sendBroadcast(intent);
            }

            dismiss();
        }
    };
}
