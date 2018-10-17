package com.tortel.authenticator.fragment;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
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
import android.widget.Toast;

import com.tortel.authenticator.R;
import com.tortel.authenticator.activity.MainActivity;
import com.tortel.authenticator.common.data.AccountInfo;
import com.tortel.authenticator.common.exception.OtpSourceException;
import com.tortel.authenticator.common.otp.OtpSource;
import com.tortel.authenticator.common.otp.TotpClock;
import com.tortel.authenticator.common.otp.TotpCountdownTask;
import com.tortel.authenticator.common.otp.TotpCounter;
import com.tortel.authenticator.common.data.AccountDb;
import com.tortel.authenticator.common.utils.DependencyInjector;
import com.tortel.authenticator.common.utils.Log;
import com.tortel.authenticator.common.utils.Utilities;
import com.tortel.authenticator.dialog.ConfirmDeleteDialog;
import com.tortel.authenticator.dialog.EditAccountDialog;
import com.tortel.authenticator.common.view.CountdownIndicator;

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
    private List<AccountWrapper> mAccountWrapper;

    private Handler mHandler;

    private AccountWrapper mSelectedWrapper;
    private ActionMode mActionMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mAccountWrapper = new ArrayList<>();

        mAccountDb = DependencyInjector.getAccountDb();
        mOtpProvider = DependencyInjector.getOtpProvider();

        mTotpCounter = mOtpProvider.getTotpCounter();
        mTotpClock = mOtpProvider.getTotpClock();

        mAdapter = new OtpDataAdapter();
        mHandler = new Handler();

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getContext());
        // Register the receiver for all events
        broadcastManager.registerReceiver(mAccountChangeReceiver, new IntentFilter(MainActivity.ACCOUNT_CHANGED));
        broadcastManager.registerReceiver(mAccountChangeReceiver, new IntentFilter(MainActivity.ACCOUNT_DELETED));
        broadcastManager.registerReceiver(mAccountChangeReceiver, new IntentFilter(MainActivity.ACCOUNT_CREATED));
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
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getContext());
        broadcastManager.unregisterReceiver(mAccountChangeReceiver);
    }

    private void refreshUserList() {
        List<Integer> allIds = mAccountDb.getAllIds();
        for(AccountWrapper wrapper : mAccountWrapper){
            allIds.remove(Integer.valueOf(wrapper.info.getId()));
        }

        // Anything remaining must be new
        for(int id : allIds){
            Log.d("Adding account to list with id "+id);
            AccountInfo account = mAccountDb.getAccountInfo(id);
            try {
                AccountWrapper wrapper = new AccountWrapper();
                wrapper.info = account;
                account.setCode(getString(R.string.empty_pin));
                wrapper.hotpCodeGenerationAllowed = true;

                if (!account.isHtop()) {
                    account.setCode(mOtpProvider.getNextCode(account.getId()));
                }

                mAccountWrapper.add(wrapper);
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
        for(AccountWrapper wrapper : mAccountWrapper){
            // Only update time-based codes
            if(!wrapper.info.isHtop()) {
                try {
                    wrapper.info.setCode(mOtpProvider.getNextCode(wrapper.info.getId()));
                    if (wrapper.holder != null && wrapper.holder.mPinView != null) {
                        wrapper.holder.mPinView.setText(wrapper.info.getCode());
                    }
                } catch (Exception e) {
                    Log.e("Exception updating code for " + wrapper.info.getName());
                }
            }
        }
    }

    private void updateCountdownIndicators() {
        for(AccountWrapper info : mAccountWrapper){
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

    /**
     * Displays the contextual action bar
     */
    private void showActionMode(){
        if(mActionMode == null) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            mActionMode = activity.startSupportActionMode(mActionModeCallback);
        }
    }

    /**
     * Gets the account wrapper for a provided id
     * @param id the account's id
     * @return the wrapper
     */
    private AccountWrapper getAccountWrappper(int id){
        for(AccountWrapper wrapper : mAccountWrapper){
            if(wrapper.info.getId() == id){
                return wrapper;
            }
        }
        return null;
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
            holder.setPinInfo(mAccountWrapper.get(position));
        }

        @Override
        public int getItemCount() {
            return mAccountWrapper != null ? mAccountWrapper.size() : 0;
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

        private AccountWrapper mAccountWrapper;

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

        public void setPinInfo(AccountWrapper pin){
            mAccountWrapper = pin;
            pin.holder = this;
            AccountInfo info = pin.info;

            if(info.isHtop()){
                // Counter-based code
                mNextCodeButton.setVisibility(View.VISIBLE);
                mCountdownIndicator.setVisibility(View.GONE);

                mNextCodeButton.setEnabled(pin.hotpCodeGenerationAllowed);

                // Scale down the empty pin
                if (getString(R.string.empty_pin).equals(info.getCode())) {
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
            if(mAccountWrapper.equals(mSelectedWrapper)){
                mView.setActivated(true);
            } else {
                mView.setActivated(false);
            }

            mUserNameView.setText(info.getName());
            mPinView.setText(info.getCode());
        }

        private View.OnClickListener mClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If action mode is not null, just change the selection
                if(mActionMode != null){
                    if(mSelectedWrapper != null && mSelectedWrapper.holder != null){
                        mSelectedWrapper.holder.mView.setActivated(false);
                    }
                    mView.setActivated(true);
                    mSelectedWrapper = mAccountWrapper;
                    return;
                }
                final AccountWrapper wrapper = mAccountWrapper;
                final AccountInfo info = mAccountWrapper.info;

                // No action mode - do normal stuff
                if(info.isHtop() && mAccountWrapper.hotpCodeGenerationAllowed){
                    try {
                        // Create final copies for the callbacks
                        final TextView pinView = mPinView;
                        final String pin = mOtpProvider.getNextCode(info.getId());
                        Log.d("Generating new code for "+info.getName());

                        info.setCode(pin);
                        pinView.setText(info.getCode());
                        mPinView.setTextScaleX(PIN_TEXT_SCALEX_NORMAL);

                        // Prevent generation for a bit
                        mAccountWrapper.hotpCodeGenerationAllowed = false;
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("Re-enabling code generation for "+info.getName());
                                wrapper.hotpCodeGenerationAllowed = true;
                            }
                        }, HOTP_MIN_TIME_INTERVAL_BETWEEN_CODES);

                        // Clear the generated code after
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!pin.equals(info.getCode())) {
                                    return;
                                }
                                Log.d("Clearing code for "+info.getName());
                                info.setCode(getString(R.string.empty_pin));
                                pinView.setText(info.getCode());
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
                if(mSelectedWrapper != null && mSelectedWrapper.holder != null){
                    Log.d("Clearing old selected view");
                    mSelectedWrapper.holder.mView.setActivated(false);
                }

                mView.setActivated(true);
                mSelectedWrapper = mAccountWrapper;
                showActionMode();
                return true;
            }
        };

    }

    /**
     * Wrapper that contains relevant pin information
     */
    private static class AccountWrapper {
        public AccountInfo info;
        // The viewholder displaying this code
        public OtpViewHolder holder;

        // HOTP only: Whether code generation is allowed for this account.
        public boolean hotpCodeGenerationAllowed;

        @Override
        public boolean equals(Object o){
            return o instanceof AccountWrapper && info.equals(((AccountWrapper) o).info);
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
            DialogFragment dialog;
            Bundle args;
            AccountInfo info = mSelectedWrapper.info;

            switch(menuItem.getItemId()){
                case R.id.context_copy:
                    ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText(info.getName(), info.getCode());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(getContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                    actionMode.finish();
                    return true;
                case R.id.context_edit:
                    args = new Bundle();
                    args.putInt(ConfirmDeleteDialog.ID, info.getId());
                    args.putString(ConfirmDeleteDialog.USERNAME, info.getName());
                    dialog = new EditAccountDialog();
                    dialog.setArguments(args);

                    dialog.show(getFragmentManager(), "edit");
                    actionMode.finish();
                    return true;
                case R.id.context_delete:
                    args = new Bundle();
                    args.putInt(ConfirmDeleteDialog.ID, info.getId());
                    args.putString(ConfirmDeleteDialog.USERNAME, info.getName());
                    dialog = new ConfirmDeleteDialog();
                    dialog.setArguments(args);

                    dialog.show(getFragmentManager(), "delete");
                    actionMode.finish();
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
            if(mSelectedWrapper != null && mSelectedWrapper.holder != null){
                mSelectedWrapper.holder.mView.setActivated(false);
                mSelectedWrapper = null;
            }
        }
    };

    /**
     * Receiver for an account change, which force refreshes the list
     */
    private BroadcastReceiver mAccountChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("Received account changed intent");
            int id;
            switch(intent.getAction()){
                case MainActivity.ACCOUNT_CHANGED:
                    id = intent.getIntExtra(MainActivity.ACCOUNT_ID, -1);
                    if(id >= 0){
                        Log.d("Refreshing name of account "+id);
                        AccountWrapper wrapper = getAccountWrappper(id);
                        wrapper.info.setName(mAccountDb.getEmail(id));
                        if(wrapper.holder != null && wrapper.holder.mUserNameView != null) {
                            wrapper.holder.mUserNameView.setText(wrapper.info.getName());
                        }
                    }
                    return;
                case MainActivity.ACCOUNT_CREATED:
                    refreshUserList();
                    return;
                case MainActivity.ACCOUNT_DELETED:
                    id = intent.getIntExtra(MainActivity.ACCOUNT_ID, -1);
                    Log.d("Account with id "+id+" deleted, removing from list");
                    if(id >= 0){
                        for(AccountWrapper wrapper : mAccountWrapper){
                            if(wrapper.info.getId() == id){
                                mAccountWrapper.remove(wrapper);
                                mAdapter.notifyDataSetChanged();
                                return;
                            }
                        }
                    }
                    return;
            }
        }
    };
}
