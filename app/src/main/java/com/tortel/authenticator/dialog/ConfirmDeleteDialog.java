package com.tortel.authenticator.dialog;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.tortel.authenticator.AccountDb;
import com.tortel.authenticator.R;
import com.tortel.authenticator.fragment.CodeListFragment;
import com.tortel.authenticator.utils.DependencyInjector;

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

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        builder.title(getString(R.string.remove_account_dialog_title, username));

        TextView textView = new TextView(getActivity());
        textView.setText(Html.fromHtml(getString(R.string.remove_account_dialog_message)));

        builder.customView(textView, true);

        builder.positiveText(R.string.ok);
        builder.negativeText(R.string.cancel);
        builder.callback(callback);

        return builder.build();
    }

    private MaterialDialog.Callback callback = new MaterialDialog.Callback() {
        @Override
        public void onNegative(MaterialDialog materialDialog) {
            dismiss();
        }

        @Override
        public void onPositive(MaterialDialog materialDialog) {
            AccountDb accountDb = DependencyInjector.getAccountDb();
            accountDb.delete(id);
            Intent intent = new Intent(CodeListFragment.ACCOUNT_CHANGED);
            LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity().getBaseContext());
            broadcastManager.sendBroadcast(intent);

            dismiss();
        }
    };
}
