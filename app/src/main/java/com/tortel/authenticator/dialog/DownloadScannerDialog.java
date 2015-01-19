package com.tortel.authenticator.dialog;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.tortel.authenticator.R;
import com.tortel.authenticator.common.utils.Utilities;

/**
 * Fragment for offering to download the barcode scanner app
 */
public class DownloadScannerDialog extends DialogFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
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
        builder.title(R.string.install_dialog_title);
        builder.content(R.string.install_dialog_message);
        builder.positiveText(R.string.install_button);
        builder.negativeText(R.string.cancel);
        builder.callback(callback);

        return builder.build();
    }

    private MaterialDialog.ButtonCallback callback = new MaterialDialog.ButtonCallback() {
        @Override
        public void onPositive(MaterialDialog dialog) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri
                    .parse(Utilities.ZXING_MARKET));
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) { // if no
                // Market
                // app
                intent = new Intent(Intent.ACTION_VIEW, Uri
                        .parse(Utilities.ZXING_DIRECT));
                startActivity(intent);
            }
        }

        @Override
        public void onNegative(MaterialDialog dialog) {
            dialog.dismiss();
        }
    };
}
