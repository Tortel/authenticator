package com.tortel.authenticator.fragment;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.tortel.authenticator.AccountDb;
import com.tortel.authenticator.R;
import com.tortel.authenticator.exception.OtpSourceException;
import com.tortel.authenticator.otp.OtpSource;
import com.tortel.authenticator.otp.TotpClock;
import com.tortel.authenticator.otp.TotpCountdownTask;
import com.tortel.authenticator.otp.TotpCounter;
import com.tortel.authenticator.utils.DependencyInjector;
import com.tortel.authenticator.utils.Log;
import com.tortel.authenticator.utils.Utilities;
import com.tortel.authenticator.view.CountdownIndicator;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that shows the OTP codes for all the accounts
 */
public class CodeListFragment extends Fragment {
    /**
     * Scale to use for the text displaying the PIN numbers.
     */
    private static final float PIN_TEXT_SCALEX_NORMAL = 1.0f;
    /**
     * Underscores are shown slightly smaller.
     */
    private static final float PIN_TEXT_SCALEX_UNDERSCORE = 0.87f;
    /**
     * Frequency (milliseconds) with which TOTP countdown indicators are
     * updated.
     */
    private static final long TOTP_COUNTDOWN_REFRESH_PERIOD = 200;

    /**
     * Minimum amount of time (milliseconds) that has to elapse from the moment
     * a HOTP code is generated for an account until the moment the next code
     * can be generated for the account. This is to prevent the user from
     * generating too many HOTP codes in a short period of time.
     */
    private static final long HOTP_MIN_TIME_INTERVAL_BETWEEN_CODES = 5000;

    /**
     * The maximum amount of time (milliseconds) for which a HOTP code is
     * displayed after it's been generated.
     */
    private static final long HOTP_DISPLAY_TIMEOUT = 2 * 60 * 1000;

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

    /**
     * Phase of TOTP countdown indicators. The phase is in {@code [0, 1]} with
     * {@code 1} meaning full time step remaining until the code refreshes, and
     * {@code 0} meaning the code is refreshing right now.
     */
    private double mTotpCountdownPhase;
    private AccountDb mAccountDb;
    private OtpSource mOtpProvider;

    private RecyclerView.Adapter mAdapter;
    private List<PinInfo> mPinInfo;

    private Handler mHandler;

    private PinInfo mSelectedInfo;
    private ActionMode mActionMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mPinInfo = new ArrayList<>();

        mAccountDb = DependencyInjector.getAccountDb();
        mOtpProvider = DependencyInjector.getOtpProvider();

        mTotpCounter = mOtpProvider.getTotpCounter();
        mTotpClock = mOtpProvider.getTotpClock();

        mAdapter = new OtpDataAdapter();
        mHandler = new Handler();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopTotpCountdownTask();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUserList();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTotpCountdownTask();
    }

    private void refreshUserList() {
        List<Integer> ids = mAccountDb.getAllIds();

        // Simple check to not constantly clear the list
        if(ids.size() == mPinInfo.size()){
            Log.d("IDs and list of data is same size, not reloading");
            startTotpCountdownTask();
            return;
        }

        for(Integer id : ids){
            try {
                PinInfo info = new PinInfo();
                info.id = id;
                info.pin = getString(R.string.empty_pin);
                info.hotpCodeGenerationAllowed = true;

                info.isHotp = mAccountDb.getType(id) == AccountDb.OtpType.HOTP;
                info.user = mAccountDb.getEmail(id);

                if (!info.isHotp) {
                    info.pin = mOtpProvider.getNextCode(id);
                }

                mPinInfo.add(info);
            } catch(OtpSourceException e){
                Log.e("Exception preparing account", e);
            }
        }
        mAdapter.notifyDataSetChanged();
        startTotpCountdownTask();
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
                refreshVerificationCodes();
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

    private void setTotpCountdownPhase(double phase) {
        mTotpCountdownPhase = phase;
        updateCountdownIndicators();
    }

    private void refreshVerificationCodes() {
        for(PinInfo info : mPinInfo){
            // Only update time-based codes
            if(!info.isHotp) {
                try {
                    info.pin = mOtpProvider.getNextCode(info.id);
                    if (info.holder != null && info.holder.mPinView != null) {
                        info.holder.mPinView.setText(info.pin);
                    }
                } catch (Exception e) {
                    Log.e("Exception updating code for " + info.user);
                }
            }
        }
    }

    private void updateCountdownIndicators() {
        for(PinInfo info : mPinInfo){
            if(info.holder != null && info.holder.mCountdownIndicator != null){
                info.holder.mCountdownIndicator.setPhase(mTotpCountdownPhase);
            }
        }
    }

    private void setTotpCountdownPhaseFromTimeTillNextValue(long millisRemaining) {
        setTotpCountdownPhase(((double) millisRemaining)
                / Utilities.secondsToMillis(mTotpCounter.getTimeStep()));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.otp_list, container, false);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);

        // Use 1 column for portrait, 2 for landscape
        int columns = getActivity().getResources().getConfiguration()
                .orientation == Configuration.ORIENTATION_PORTRAIT ? 1 : 2;
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getContext(), columns,
                LinearLayoutManager.VERTICAL, false);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(mAdapter);

        refreshUserList();

        // If the action mode is not null, it was open during rotation, so redisplay it
        if(mActionMode != null){
            mActionMode = null;
            showActionMode();
        }

        return view;
    }

    private Context getContext(){
        return getActivity().getBaseContext();
    }

    /**
     * Displays the contextual action bar
     */
    private void showActionMode(){
        if(mActionMode == null) {
            ActionBarActivity activity = (ActionBarActivity) getActivity();
            mActionMode = activity.startSupportActionMode(mActionModeCallback);
        }
    }

    /**
     * RecyclerView data adapter
     */
    private class OtpDataAdapter extends RecyclerView.Adapter<OtpViewHolder> {

        @Override
        public OtpViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getActivity().getLayoutInflater().inflate(R.layout.user_row, parent, false);
            return new OtpViewHolder(view);
        }

        @Override
        public void onBindViewHolder(OtpViewHolder holder, int position) {
            holder.setPinInfo(mPinInfo.get(position));
        }

        @Override
        public int getItemCount() {
            return mPinInfo != null ? mPinInfo.size() : 0;
        }
    }

    /**
     * RecyclerView view holder
     */
    private class OtpViewHolder extends RecyclerView.ViewHolder {
        private TextView mUserNameView;
        private TextView mPinView;
        private ImageButton mNextCodeButton;
        private CountdownIndicator mCountdownIndicator;
        private View mContentView;
        private View mView;

        private PinInfo mPinInfo;

        public OtpViewHolder(View view) {
            super(view);
            mView = view;

            mUserNameView = (TextView) view.findViewById(R.id.current_user);
            mPinView = (TextView) view.findViewById(R.id.pin_value);
            mNextCodeButton = (ImageButton) view.findViewById(R.id.next_otp);
            mCountdownIndicator = (CountdownIndicator) view.findViewById(R.id.countdown_icon);

            mContentView = view.findViewById(R.id.row_content);
            mContentView.setOnClickListener(mClickListener);
            mContentView.setOnLongClickListener(mLongClickListener);
        }

        public void setPinInfo(PinInfo pin){
            mPinInfo = pin;
            pin.holder = this;

            if(pin.isHotp){
                // Counter-based code
                mNextCodeButton.setVisibility(View.VISIBLE);
                mCountdownIndicator.setVisibility(View.GONE);

                mNextCodeButton.setEnabled(pin.hotpCodeGenerationAllowed);

                // Scale down the empty pin
                if (getString(R.string.empty_pin).equals(pin.pin)) {
                    mPinView.setTextScaleX(PIN_TEXT_SCALEX_UNDERSCORE);
                } else {
                    mPinView.setTextScaleX(PIN_TEXT_SCALEX_NORMAL);
                }
            } else {
                // Time-based code
                mNextCodeButton.setVisibility(View.GONE);
                mCountdownIndicator.setVisibility(View.VISIBLE);
                mPinView.setTextScaleX(PIN_TEXT_SCALEX_NORMAL);

                mCountdownIndicator.setPhase(mTotpCountdownPhase);
            }

            // Check if the background should be highlighted
            if(mPinInfo.equals(mSelectedInfo)){
                mView.setActivated(true);
            } else {
                mView.setActivated(false);
            }

            mUserNameView.setText(pin.user);
            mPinView.setText(pin.pin);
        }

        private View.OnClickListener mClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mPinInfo.isHotp && mPinInfo.hotpCodeGenerationAllowed){
                    try {
                        // Create final copies for the callbacks
                        final PinInfo info = mPinInfo;
                        final TextView pinView = mPinView;
                        final String pin = mOtpProvider.getNextCode(mPinInfo.id);
                        Log.d("Generating new code for "+info.user);

                        info.pin = pin;
                        pinView.setText(info.pin);
                        mPinView.setTextScaleX(PIN_TEXT_SCALEX_NORMAL);

                        // Prevent generation for a bit
                        mPinInfo.hotpCodeGenerationAllowed = false;
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("Re-enabling code generation for "+info.user);
                                info.hotpCodeGenerationAllowed = true;
                            }
                        }, HOTP_MIN_TIME_INTERVAL_BETWEEN_CODES);

                        // Clear the generated code after
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!pin.equals(info.pin)) {
                                    return;
                                }
                                Log.d("Clearing code for "+info.user);
                                info.pin = getString(R.string.empty_pin);
                                pinView.setText(info.pin);
                                mPinView.setTextScaleX(PIN_TEXT_SCALEX_UNDERSCORE);
                            }
                        }, HOTP_DISPLAY_TIMEOUT);

                    } catch (Exception e) {
                        Log.e("Exception getting next code", e);
                        mPinView.setText("Error generating code");
                    }
                }
            }
        };

        private View.OnLongClickListener mLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // Clear the old selected view
                if(mSelectedInfo != null && mSelectedInfo.holder != null){
                    Log.d("Clearing old selected view");
                    mSelectedInfo.holder.mView.setActivated(false);
                }

                mView.setActivated(true);
                mSelectedInfo = mPinInfo;
                showActionMode();
                return true;
            }
        };

    }

    /**
     * Wrapper that contains relevant pin information
     */
    private static class PinInfo {
        // Account id
        public Integer id;
        // The calculated code, or a placeholder
        public String pin;
        // The username
        public String user;
        // The viewholder displaying this code
        public OtpViewHolder holder;

        // true if hopt (Counter), otherwise its a time-based code
        public boolean isHotp = false;
        // HOTP only: Whether code generation is allowed for this account.
        public boolean hotpCodeGenerationAllowed;

        @Override
        public boolean equals(Object o){
            if(!(o instanceof  PinInfo)){
                return false;
            }
            return id.equals(((PinInfo) o).id);
        }
    }

    /**
     * Manages the contextual action bar
     */
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        // Called when the actionmode is created
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.context_list, menu);
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        // Called when a contextual menu item was selected
        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            switch(menuItem.getItemId()){
                case R.id.context_copy:
                    // TODO
                    return true;
                case R.id.context_edit:
                    // TODO
                    return true;
                case R.id.context_delete:
                    // TODO
                    return true;
                default:
                    Log.d("Other id: "+menuItem.getItemId());
            }
            return false;
        }

        // Called when the action mode is being dismissed
        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            Log.d("Dismissing ActionMode");
            mActionMode = null;
            if(mSelectedInfo != null && mSelectedInfo.holder != null){
                mSelectedInfo.holder.mView.setActivated(false);
                mSelectedInfo = null;
            }
        }
    };
}
