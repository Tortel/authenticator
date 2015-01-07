package com.tortel.authenticator.activity;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.tortel.authenticator.R;

/**
 * Activity that
 */
public class HowItWorksActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fragment_activity);

        Fragment fragment = getFragmentManager().findFragmentById(R.id.content_frame);
        if(fragment == null){
            fragment = new EnterPasswordFrag();
            getFragmentManager().beginTransaction().replace(R.id.content_frame, fragment).commitAllowingStateLoss();
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

        /**
         * Get the id of the detail text to show
         * @return
         */
        protected abstract int getDetailsResource();

        /**
         * Get the next fragment to show
         * @return
         */
        protected abstract IntroFragment getNextFragment();

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(getLayoutResource(), container, false);

            TextView textView = (TextView) view.findViewById(R.id.details);
            textView.setText(Html.fromHtml(getActivity().getString(getDetailsResource())));

            Button button = (Button) view.findViewById(R.id.next_button);
            button.setOnClickListener(this);

            return view;
        }

        @Override
        public void onClick(View v) {
            IntroFragment frag = getNextFragment();
            if(frag != null){
                getFragmentManager().beginTransaction().replace(R.id.content_frame, frag).commitAllowingStateLoss();
            } else {
                getActivity().finish();
            }
        }
    }

    public static class EnterPasswordFrag extends IntroFragment {
        @Override
        protected int getLayoutResource() {
            return R.layout.howitworks_enter_password;
        }

        @Override
        protected int getDetailsResource() {
            return R.string.howitworks_page_enter_password_details;
        }

        @Override
        protected IntroFragment getNextFragment() {
            return new EnterCodeFrag();
        }
    }

    public static class EnterCodeFrag extends IntroFragment {

        @Override
        protected int getLayoutResource() {
            return R.layout.howitworks_enter_code;
        }

        @Override
        protected int getDetailsResource() {
            return R.string.howitworks_page_enter_code_details;
        }

        @Override
        protected IntroFragment getNextFragment() {
            return new VerifyDeviceFrag();
        }
    }

    public static class VerifyDeviceFrag extends IntroFragment {

        @Override
        protected int getLayoutResource() {
            return R.layout.howitworks_verify_device;
        }

        @Override
        protected int getDetailsResource() {
            return R.string.howitworks_page_verify_device_details;
        }

        @Override
        protected IntroFragment getNextFragment() {
            return null;
        }
    }


}
