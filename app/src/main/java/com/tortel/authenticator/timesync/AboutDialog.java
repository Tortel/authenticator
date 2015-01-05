package com.tortel.authenticator.timesync;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.view.ContextThemeWrapper;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.tortel.authenticator.R;

/**
 * Shows the about text in a dialog
 */
public class AboutDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context wrappedContext = new ContextThemeWrapper(getActivity().getBaseContext(),
                R.style.Base_Theme_AppCompat_Light);

        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        builder.title(R.string.timesync_about_feature_preference_title);
        builder.positiveText(R.string.ok);

        TextView textView = (TextView) getActivity().getLayoutInflater()
                .cloneInContext(wrappedContext).inflate(R.layout.about_dialog, null);
        textView.setText(Html.fromHtml(getActivity().getString(R.string.timesync_about_feature_screen_details)));

        builder.customView(textView, true);

        return builder.build();
    }
}
