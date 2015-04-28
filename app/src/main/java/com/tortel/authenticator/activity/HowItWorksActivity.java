package com.tortel.authenticator.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.tortel.authenticator.R;

/**
 * Activity that displays the how it works information
 */
public class HowItWorksActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener {
    private static final String TAB = "tab";
    private int currentTab = 0;
    private HowItWorksPager pageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.swipe_fragment_activity);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(savedInstanceState != null){
            currentTab = savedInstanceState.getInt(TAB, 0);
        }

        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pageAdapter = new HowItWorksPager(getSupportFragmentManager());
        pager.setAdapter(pageAdapter);
        pager.setOnPageChangeListener(this);
        pager.setCurrentItem(currentTab);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(TAB, currentTab);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        currentTab = position;
    }

    @Override
    public void onPageSelected(int position) {
        currentTab = position;
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private class HowItWorksPager extends FragmentPagerAdapter {
        IntroFragment frags[] =
                {new EnterPasswordFrag(), new EnterCodeFrag(), new VerifyDeviceFrag()};

        public HowItWorksPager(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return frags.length;
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            return frags[position];
        }
    }

    /**
     * Abstract fragment that the different pages will use
     */
    public abstract static class IntroFragment extends Fragment implements View.OnClickListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        /**
         * Get the id of the layout to inflate
         * @return
         */
        protected abstract int getLayoutResource();

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(getLayoutResource(), container, false);

            Button button = (Button) view.findViewById(R.id.next_button);
            if(button != null) {
                button.setOnClickListener(this);
            }

            return view;
        }

        @Override
        public void onClick(View v) {
            getActivity().finish();
        }
    }

    public static class EnterPasswordFrag extends IntroFragment {
        @Override
        protected int getLayoutResource() {
            return R.layout.howitworks_enter_password;
        }
    }

    public static class EnterCodeFrag extends IntroFragment {
        @Override
        protected int getLayoutResource() {
            return R.layout.howitworks_enter_code;
        }
    }

    public static class VerifyDeviceFrag extends IntroFragment {

        @Override
        protected int getLayoutResource() {
            return R.layout.howitworks_verify_device;
        }
    }


}
