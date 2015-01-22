package com.tortel.authenticator.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.wearable.view.CardScrollView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tortel.authenticator.R;
import com.tortel.authenticator.common.data.AccountInfo;
import com.tortel.authenticator.common.view.CountdownIndicator;

/**
 * Fragment that displays the account/OTP info
 */
public class CodeFragment extends Fragment {
    public static final String ACCOUNT = "account";

    private TextView mNameView;
    private TextView mCodeView;
    private CountdownIndicator mIndicator;

    private AccountInfo mAccountInfo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mAccountInfo = args.getParcelable(ACCOUNT);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.code_fragment, container, false);
        CardScrollView csv = (CardScrollView) view.findViewById(R.id.card_scroll_view);
        csv.setCardGravity(Gravity.BOTTOM);

        mNameView = (TextView) view.findViewById(R.id.account_name);
        mCodeView = (TextView) view.findViewById(R.id.account_code);
        mIndicator = (CountdownIndicator) view.findViewById(R.id.indicator);
        mIndicator.setPhase(0.75);

        mNameView.setText(mAccountInfo.getName());
        mCodeView.setText(getString(R.string.empty_pin));
        return view;
    }
}
