package com.tortel.authenticator.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.wearable.view.CardScrollView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tortel.authenticator.R;
import com.tortel.authenticator.common.data.AccountInfo;
import com.tortel.authenticator.common.exception.OtpSourceException;
import com.tortel.authenticator.common.otp.OtpSource;
import com.tortel.authenticator.common.otp.TotpClock;
import com.tortel.authenticator.common.otp.TotpCountdownTask;
import com.tortel.authenticator.common.otp.TotpCounter;
import com.tortel.authenticator.common.utils.DependencyInjector;
import com.tortel.authenticator.common.utils.Log;
import com.tortel.authenticator.common.utils.Utilities;
import com.tortel.authenticator.common.view.CountdownIndicator;

/**
 * Fragment that displays the account/OTP info
 */
public class CodeFragment extends Fragment {
    public static final String ACCOUNT = "account";

    /**
     * Frequency (milliseconds) with which TOTP countdown indicators are
     * updated.
     */
    private static final long TOTP_COUNTDOWN_REFRESH_PERIOD = 400;

    /**
     * Counter used for generating TOTP verification codes.
     */
    private TotpCounter mTotpCounter;

    /**
     * Clock used for generating TOTP verification codes.
     */
    private TotpClock mTotpClock;

    /**
     * Task that periodically notifies this activity about the amount of time
     * remaining until the TOTP codes refresh. The task also notifies this
     * activity when TOTP codes refresh.
     */
    private TotpCountdownTask mTotpCountdownTask;

    private OtpSource mOtpProvider;
    private double mTotpCountdownPhase;
    private Handler mHandler;

    private TextView mNameView;
    private TextView mCodeView;
    private CountdownIndicator mIndicator;

    private AccountInfo mAccountInfo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mAccountInfo = args.getParcelable(ACCOUNT);

        mHandler = new Handler();
        mOtpProvider = DependencyInjector.getOtpProvider();
        mTotpCounter = mOtpProvider.getTotpCounter();
        mTotpClock = mOtpProvider.getTotpClock();
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

    @Override
    public void onPause() {
        super.onPause();
        stopTotpCountdownTask();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCode();
        startTotpCountdownTask();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTotpCountdownTask();
    }

    private void refreshCode(){
        try{
            mAccountInfo.setCode(mOtpProvider.getNextTotpCode(mAccountInfo.getSecret()));
        } catch (OtpSourceException e) {
            Log.e("Exception generating code", e);
            mAccountInfo.setCode("Error");
        }
        mCodeView.setText(mAccountInfo.getCode());
    }

    private void startTotpCountdownTask() {
        stopTotpCountdownTask();

        mTotpCountdownTask = new TotpCountdownTask(mTotpCounter, mTotpClock,
                TOTP_COUNTDOWN_REFRESH_PERIOD);
        mTotpCountdownTask.setListener(new TotpCountdownTask.Listener() {
            @Override
            public void onTotpCountdown(long millisRemaining) {
                setTotpCountdownPhaseFromTimeTillNextValue(millisRemaining);
            }

            @Override
            public void onTotpCounterValueChanged() {
                refreshCode();
            }
        });

        mTotpCountdownTask.startAndNotifyListener();
    }

    private void stopTotpCountdownTask() {
        if (mTotpCountdownTask != null) {
            mTotpCountdownTask.stop();
            mTotpCountdownTask = null;
        }
    }

    private void setTotpCountdownPhaseFromTimeTillNextValue(long millisRemaining) {
        setTotpCountdownPhase(((double) millisRemaining)
                / Utilities.secondsToMillis(mTotpCounter.getTimeStep()));
    }

    private void setTotpCountdownPhase(double phase) {
        mTotpCountdownPhase = phase;
        mIndicator.setPhase(mTotpCountdownPhase);
    }
}
