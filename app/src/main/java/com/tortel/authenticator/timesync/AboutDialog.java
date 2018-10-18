package com.tortel.authenticator.timesync;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.text.Html;
import android.view.ContextThemeWrapper;
import android.widget.TextView;

import com.tortel.authenticator.R;

/**
 * Shows the about text in a dialog
 */
public class AboutDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context wrappedContext = new ContextThemeWrapper(getActivity().getBaseContext(),
                R.style.Base_Theme_AppCompat_Light);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.timesync_about_feature_preference_title);
        builder.setPositiveButton(R.string.ok, null);

        TextView textView = (TextView) getActivity().getLayoutInflater()
                .cloneInContext(wrappedContext).inflate(R.layout.about_dialog, null);
        textView.setText(Html.fromHtml(getActivity().getString(R.string.timesync_about_feature_screen_details)));

        builder.setView(textView);

        return builder.create();
    }
}
