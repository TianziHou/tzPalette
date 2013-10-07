package com.tzapps.tzpalette.ui;

import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.tzapps.common.ui.OnFragmentStatusChangedListener;
import com.tzapps.tzpalette.R;
import com.tzapps.tzpalette.data.PaletteData;
import com.tzapps.tzpalette.data.PaletteDataHelper;
import com.tzapps.tzpalette.data.PaletteDataComparator;
import com.tzapps.tzpalette.debug.MyDebug;

public class PaletteCardActivity extends Activity implements OnFragmentStatusChangedListener
{
    private static final String TAG = "PaletteCardActivity";

    private static final int PALETTE_CARD_EDIT_RESULT = 0;
    
    private ViewPager mViewPager;
    private PaletteCardAdapter mCardAdapter;
    private PaletteDataHelper mDataHelper;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        mViewPager = new ViewPager(this);
        mViewPager.setId(R.id.palette_card_pager);
        setContentView(mViewPager);
        
        mDataHelper = PaletteDataHelper.getInstance(this);
        
        mCardAdapter = new PaletteCardAdapter(this, mViewPager);
        
        long dataId = getIntent().getExtras().getLong(MainActivity.PALETTE_DATA_ID);
        mCardAdapter.setCurrentCard(dataId);
        
        // Make sure we're running on Honeycomb or higher to use ActionBar APIs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            // Show the Up button in the action bar.
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.palette_card_view_actions, menu);

        // Locate MenuItem with ShareActionProvider
        //MenuItem item = menu.findItem(R.id.action_share);

        // Fetch and store ShareActionProvider
        //mShareActionProvider = (ShareActionProvider) item.getActionProvider();

        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        PaletteData data = null;
        
        switch (item.getItemId())
        {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
                
            case R.id.action_edit:
                data = mCardAdapter.getCurrentData();
                if (data != null)
                {
                    if (MyDebug.LOG)
                        Log.d(TAG, "edit palette card: " + data);
                    
                    openEditView(data.getId());
                }
                return true;
                
            case R.id.action_sendEmail:
                if (MyDebug.LOG)
                    Log.d(TAG, "send email");
                return true;
                
            case R.id.action_export:
                if (MyDebug.LOG)
                    Log.d(TAG, "export palette card");
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentViewCreated(Fragment fragment){}
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        // Check which request we are responding to
        switch (requestCode)
        {
            case PALETTE_CARD_EDIT_RESULT:
                if (resultCode == RESULT_OK)
                {
                    long dataId = intent.getLongExtra(MainActivity.PALETTE_DATA_ID, Long.valueOf(-1));
                    
                    if (dataId != -1)
                    {
                        mCardAdapter.updateCard(dataId);
                    }
                }
                break;
        }
    }
    
    private void openEditView(long dataId)
    {
        Intent intent = new Intent(this, PaletteEditActivity.class);
        
        intent.putExtra(MainActivity.PALETTE_DATA_ID, dataId);
        
        startActivityForResult(intent, PALETTE_CARD_EDIT_RESULT);
    }
    
    private class PaletteCardAdapter extends FragmentPagerAdapter
    {
        private Context mContext;
        private ViewPager mViewPager;
        private List<PaletteData> dataList;
        private PaletteDataHelper mDataHelper;

        public PaletteCardAdapter(Activity activity, ViewPager pager)
        {
            super(activity.getFragmentManager());
            mContext = activity;
            mViewPager = pager;
            mDataHelper = PaletteDataHelper.getInstance(mContext);
            
            dataList = mDataHelper.getAllData();
            Collections.sort(dataList, new PaletteDataComparator.UpdatedTime());
            
            mViewPager.setAdapter(this);
        }

        public void setCurrentCard(long dataId)
        {
            int index = 0;
            
            for (int i = 0; i < dataList.size(); i++)
            {
                PaletteData data = dataList.get(i);
                
                if (data.getId() == dataId)
                {
                    index = i;
                    break;
                }
            }
            
            mViewPager.setCurrentItem(index);
        }
        
        public PaletteData getCurrentData()
        {
            int index = mViewPager.getCurrentItem();
            
            return dataList.get(index);
        }
        
        public void updateCard(long dataId)
        {
            for (PaletteData d : dataList)
            {
                if (d.getId() == dataId)
                {
                    d.copy(mDataHelper.get(dataId));
                    Collections.sort(dataList, new PaletteDataComparator.UpdatedTime());
                    notifyDataSetChanged();
                    break;
                }
            }
        }

        @Override
        public Fragment getItem(int position)
        {
            PaletteData data = dataList.get(position);
            
            PaletteCardFragment fragment = (PaletteCardFragment)Fragment.instantiate(mContext, 
                                        PaletteCardFragment.class.getName(), null);
            fragment.setData(data);
            
            return fragment;
        }
        
        @Override
        public Object instantiateItem(ViewGroup container, int position)
        {
            PaletteCardFragment fragment = (PaletteCardFragment)super.instantiateItem(container, position);
            
            PaletteData data = dataList.get(position);
            fragment.setData(data);
            
            return fragment;
        }
        
        @Override
        public int getCount()
        {
            return dataList.size();
        }
    }

}
