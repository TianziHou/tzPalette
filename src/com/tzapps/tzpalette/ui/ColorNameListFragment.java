package com.tzapps.tzpalette.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.tzapps.common.ui.BaseFragment;
import com.tzapps.common.utils.StringUtils;
import com.tzapps.tzpalette.R;
import com.tzapps.tzpalette.data.ColorNameListHelper;
import com.tzapps.tzpalette.ui.dialog.ColorInfoDialogFragment;
import com.tzapps.tzpalette.ui.view.ColorNameListView;
import com.tzapps.tzpalette.utils.TzPaletteUtils;

public class ColorNameListFragment extends BaseFragment implements OnItemClickListener, OnItemLongClickListener
{
    private static final String TAG = "ColorNameListFragment";
    
    private ColorNameListHelper mColorNameHelper;
    private ColorNameListView mColorNameList;
    private String mQuery;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);
        
        mColorNameHelper = ColorNameListHelper.getInstance(getActivity());

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.color_name_list_view, container, false);
        
        mColorNameList = (ColorNameListView) view.findViewById(R.id.color_name_list);
        
        if (StringUtils.isEmpty(mQuery))
            mColorNameList.setColors(mColorNameHelper.getAll());
        else
            mColorNameList.setColors(mColorNameHelper.search(mQuery));
        
        mColorNameList.setOnItemClickListener(this);
        mColorNameList.setOnItemLongClickListener(this);
        
        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        int color = mColorNameList.getColor(position);
        
        // Show color info detail dialog
        ColorInfoDialogFragment dialogFrag = ColorInfoDialogFragment.newInstance(color);
        dialogFrag.show(getFragmentManager(), "colorInfoDialog");
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
    {
        int color = mColorNameList.getColor(position);
        
        // Copy color info into clipboard
        TzPaletteUtils.copyColorToClipboard(getActivity(), color);
        
        return true;
    }
    
    public void search(String query)
    {
        mQuery = query;
        
        if (StringUtils.isEmpty(query))
            mColorNameList.setColors(mColorNameHelper.getAll());
        else
            mColorNameList.setColors(mColorNameHelper.search(query));
    }
}
