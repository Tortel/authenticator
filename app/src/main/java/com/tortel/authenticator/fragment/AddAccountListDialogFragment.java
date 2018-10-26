package com.tortel.authenticator.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tortel.authenticator.R;
import com.tortel.authenticator.activity.AddAccountActivity;

public class AddAccountListDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_addacount_list_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TextView button = view.findViewById(R.id.scan_barcode);
        button.setOnClickListener(this);
        button = view.findViewById(R.id.enter_code);
        button.setOnClickListener(this);
    }


    @Override
    public void onClick(View view) {
        Intent intent = null;
        switch (view.getId()) {
            case R.id.scan_barcode:
                intent = new Intent(getActivity(), AddAccountActivity.class);
                break;
            case R.id.enter_code:
                intent = new Intent(getActivity(), AddAccountActivity.class);
                break;
        }

        getActivity().startActivity(intent);

        this.dismiss();
    }
}
