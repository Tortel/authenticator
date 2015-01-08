package com.tortel.authenticator.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.tortel.authenticator.R;
import com.tortel.authenticator.activity.AddAccountActivity;
import com.tortel.authenticator.activity.HowItWorksActivity;
import com.tortel.authenticator.activity.MainActivity;

/**
 * Fragment that is shown when there are no accounts added to the app yet
 */
public class NoAccountsFragment extends Fragment implements View.OnClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.no_accounts, container, false);

        // Set the listeners
        Button button = (Button) view.findViewById(R.id.how_it_works_button);
        button.setOnClickListener(this);
        button = (Button) view.findViewById(R.id.add_account_button);
        button.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v) {
        MainActivity activity = (MainActivity) getActivity();
        switch (v.getId()){
            case R.id.how_it_works_button:
                activity.showHowItWorks();
                return;
            case R.id.add_account_button:
                activity.showAddAccount();
                return;
        }
    }
}
