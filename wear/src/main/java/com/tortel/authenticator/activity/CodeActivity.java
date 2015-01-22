package com.tortel.authenticator.activity;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.wearable.view.CardFragment;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.util.SparseArray;
import android.widget.TextView;

import com.tortel.authenticator.R;
import com.tortel.authenticator.common.data.AccountInfo;
import com.tortel.authenticator.common.exception.OtpSourceException;
import com.tortel.authenticator.common.otp.OtpProvider;
import com.tortel.authenticator.common.otp.OtpSource;
import com.tortel.authenticator.common.utils.DependencyInjector;
import com.tortel.authenticator.common.utils.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity which displays the OTP codes
 */
public class CodeActivity extends Activity {
    private AccountGridPagerAdapter mAdapter;
    private OtpSource mOtpProvider;
    private List<AccountInfo> mAccounts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.grid_layout);

        mAccounts = DependencyInjector.getAccountDb().getAllAccounts();
        mOtpProvider = DependencyInjector.getOtpProvider();

        mAdapter = new AccountGridPagerAdapter();
        GridViewPager pager = (GridViewPager) findViewById(R.id.pager);

        pager.setAdapter(mAdapter);
    }

    public class AccountGridPagerAdapter extends FragmentGridPagerAdapter{
        private SparseArray<CardFragment> frags = new SparseArray<>();

        public AccountGridPagerAdapter(){
            super(getFragmentManager());
        }

        @Override
        public int getRowCount() {
            return mAccounts == null ? 0 : mAccounts.size();
        }

        @Override
        public int getColumnCount(int row) {
            return 1;
        }

        @Override
        public Fragment getFragment(int row, int column) {
            AccountInfo info = mAccounts.get(row);
            String code = getString(R.string.empty_pin);
            try{
                code = mOtpProvider.getNextCode(info.getId());
            } catch (OtpSourceException e) {
                Log.e("Error getting code for " + info.getName(), e);
            }
            return CardFragment.create(info.getName(), code, R.drawable.ic_launcher);
        }
    }
}
