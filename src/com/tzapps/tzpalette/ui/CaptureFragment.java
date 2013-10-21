package com.tzapps.tzpalette.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tzapps.common.ui.BaseFragment;
import com.tzapps.tzpalette.R;

public class CaptureFragment extends BaseFragment
{
    private static final String TAG = "CaptureFragment";
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.capture_view, container, false);
        
        return view;
    }
}

