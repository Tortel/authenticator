package com.tortel.authenticator.dialog;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;

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
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.install_dialog_title);
        builder.setMessage(R.string.install_dialog_message);
        builder.setPositiveButton(R.string.install_button, callback);
        builder.setNegativeButton(R.string.cancel, callback);

        return builder.create();
    }

    private DialogInterface.OnClickListener callback = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
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
            dismiss();
        }
    };
}
