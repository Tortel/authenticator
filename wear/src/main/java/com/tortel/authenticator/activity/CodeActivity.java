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
import com.tortel.authenticator.fragment.CodeFragment;

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
        private SparseArray<Fragment> frags = new SparseArray<>();

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
            if(frags.get(row) == null){
                AccountInfo info = mAccounts.get(row);
                Bundle args = new Bundle();
                args.putParcelable(CodeFragment.ACCOUNT, info);
                Fragment frag = new CodeFragment();
                frag.setArguments(args);
                frags.put(row, frag);
            }
            return frags.get(row);
        }
    }
}
