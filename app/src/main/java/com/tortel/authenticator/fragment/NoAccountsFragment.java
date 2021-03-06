package com.tortel.authenticator.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.tortel.authenticator.R;
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.no_accounts, container, false);

        // Set the listeners
        Button button = view.findViewById(R.id.how_it_works_button);
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
