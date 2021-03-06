package com.tzapps.tzpalette.ui;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnActionExpandListener;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.SearchView.OnCloseListener;

import com.tzapps.common.ui.OnFragmentStatusChangedListener;
import com.tzapps.common.utils.ActivityUtils;
import com.tzapps.common.utils.StringUtils;
import com.tzapps.tzpalette.Constants;
import com.tzapps.tzpalette.R;
import com.tzapps.tzpalette.data.ColorNameListHelper;
import com.tzapps.tzpalette.data.PaletteData;
import com.tzapps.tzpalette.data.PaletteDataComparator.Sorter;
import com.tzapps.tzpalette.data.PaletteDataHelper;
import com.tzapps.tzpalette.debug.MyDebug;
import com.tzapps.tzpalette.ui.PaletteListFragment.OnClickPaletteItemListener;
import com.tzapps.tzpalette.ui.dialog.AboutDialogFragment;
import com.tzapps.tzpalette.ui.dialog.PaletteDataOption;
import com.tzapps.tzpalette.ui.dialog.PaletteDataOptionsDialogFragment;
import com.tzapps.tzpalette.ui.dialog.PaletteDataOptionsDialogFragment.OnClickPaletteItemOptionListener;
import com.tzapps.tzpalette.ui.dialog.PaletteDataSortByDialogFragment;
import com.tzapps.tzpalette.ui.dialog.PaletteDataSortByDialogFragment.OnClickPaletteDataSorterListener;

public class MainActivity extends Activity implements OnFragmentStatusChangedListener,
        OnClickPaletteItemOptionListener, OnClickPaletteItemListener, OnClickPaletteDataSorterListener
{
    private final static String TAG = "MainActivity";

    /** Called when the user clicks the TakePicture button */
    private static final int TAKE_PHOTE_RESULT = 1;
    /** Called when the user clicks the LoadPicture button */
    private static final int LOAD_PICTURE_RESULT = 2;
    /** Called when the user opens the Palette Edit view */
    private static final int PALETTE_EDIT_RESULT = 3;
    
    private static final int PAGE_CAPTURE_VIEW_POSITION    = 0;
    private static final int PAGE_PALETTE_LIST_POSITION    = 1;
    private static final int PAGE_COLOR_NAME_LIST_POSITION = 2;

    private ViewPager mViewPager;
    private PageAdapter mPageAdapter;
    
    /** temp file used to cache the picture or photo, it should be cleanup when the app is exit. */
    private File mTempFile;
    
    private PaletteDataHelper mDataHelper;
    private Sorter mDataSorter = Constants.PALETTE_DATA_SORTER_DEFAULT;
    private ColorNameListHelper mColorNameHelper;

    private PaletteListFragment mPaletteListFragment;
    private ColorNameListFragment mColorNameListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        ActivityUtils.forceToShowOverflowOptionsOnActoinBar(this);
        
        // Make sure we're running on Honeycomb or higher to use ActionBar APIs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            getActionBar().setTitle(R.string.title_main_view);
        }
        
        // init ColorNameListHelper when the main activity is created...
        ColorNameListHelper.getInstance(this);

        mDataHelper = PaletteDataHelper.getInstance(this);
        mDataHelper.openDb(true);

        mViewPager = (ViewPager) findViewById(R.id.main_pager);

        mPageAdapter = new PageAdapter(this, mViewPager);
        
        mPageAdapter.addPage(getString(R.string.title_capture_view), CaptureFragment.class, null);
        mPageAdapter.addPage(getString(R.string.title_palette_list_view), PaletteListFragment.class,null);
        mPageAdapter.addPage(getString(R.string.title_color_name_list_view), ColorNameListFragment.class, null);
        
        // Open palette list view directly if there has been already record in database
        if (mDataHelper.getDataCount() > 0)
            mPageAdapter.setSelectedPage(PAGE_PALETTE_LIST_POSITION);

        // handle with the intent passed in   
        handleIntent(getIntent());
    }
    
    @Override
    protected void onNewIntent(Intent intent)
    {
        handleIntent(intent);
    }
    
    private void handleIntent(Intent intent)
    {
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null)
        {
            if (type.startsWith("image/"))
                handleSendImage(intent);
        }
        else if (Intent.ACTION_SEARCH.equals(action))
        {
            String query = intent.getStringExtra(SearchManager.QUERY);
            
            if (MyDebug.LOG)
                Log.d(TAG, "handle with search query: " + query);
            
            handleColorNameQuery(query);
        }
    }
    
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        clearTemp();
    }
    
    private void clearTemp()
    {
        if (mTempFile != null)
            mTempFile.delete();
    }

    private void handleSendImage(Intent intent)
    {
        Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);

        if (imageUri != null)
            openEditView(imageUri);
    }
    
    private void handleColorNameQuery(String query)
    {
        if (mColorNameListFragment != null)
            mColorNameListFragment.search(query);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putInt("selectedPage", mPageAdapter.getSelectedPage());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        mPageAdapter.setSelectedPage(savedInstanceState.getInt("selectedPage", 0));
    }
    
    @Override
    public void onResume()
    {
        super.onResume();
        
        if (mPaletteListFragment != null)
        {
            /* 
             * TODO: this operation is very inefficient and cost
             * a lots memory. We need to find a better way to 
             * handle with palette list refresh logic in three cases:
             * 
             * 1. do not need to refresh (e.g. just open a palette 
             * card view and back without any motification)
             * 2. need to have a partial refresh (e.g. a new palette
             * data added)
             * 3. need to update all (e.g. all palette data are deleted
             * in settings view)
             */
            mPaletteListFragment.refresh();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        
        int position = mViewPager.getCurrentItem();
        
        switch (position)
        {
            case PAGE_CAPTURE_VIEW_POSITION:
                inflater.inflate(R.menu.capture_view_actions, menu);
                break;
                
            case PAGE_PALETTE_LIST_POSITION:
                inflater.inflate(R.menu.palette_list_view_actions, menu);
                break;
                
            case PAGE_COLOR_NAME_LIST_POSITION:
                inflater.inflate(R.menu.color_name_list_actions, menu);
                setupSearchView(menu);
                break;
        }

        return super.onCreateOptionsMenu(menu);
    }
    
    private void setupSearchView(Menu menu)
    {
        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        
        MenuItem menuItem = menu.findItem(R.id.action_search);
        menuItem.setOnActionExpandListener(new OnActionExpandListener()
        {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item)
            {
                if (MyDebug.LOG)
                    Log.i(TAG, "onMenuItemActionCollapse " + item.getTitle());
                
                if (mColorNameListFragment != null)
                    mColorNameListFragment.search(null);
                
                return true; // Return true to collapse action view
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item)
            {
                if (MyDebug.LOG)
                    Log.i(TAG, "onMenuItemActionExpand " + item.getTitle());
                
                return true;
            }
        });
        
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle presses on the action bar items
        switch (item.getItemId())
        {                
            case R.id.action_sortBy:
                PaletteDataSortByDialogFragment dialogFrag =
                        PaletteDataSortByDialogFragment.newInstance(getString(R.string.action_sortBy));
                dialogFrag.show(getFragmentManager(), "dialog");
                return true;

            case R.id.action_settings:
                openSettings();
                return true;
               
            case R.id.action_newPalette:
                mPageAdapter.setSelectedPage(PAGE_CAPTURE_VIEW_POSITION);
                return true;
                
            case R.id.action_paletteList:
                mPageAdapter.setSelectedPage(PAGE_PALETTE_LIST_POSITION);
                return true;
                
            case R.id.action_colorNameList:
                mPageAdapter.setSelectedPage(PAGE_COLOR_NAME_LIST_POSITION);
                return true;

            case R.id.action_about:
                showAboutDialog();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    private void openSettings()
    {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    @Override
    public void onPaletteItemOptionClicked(int position, long dataId, PaletteDataOption option)
    {
        assert (mPaletteListFragment != null);

        PaletteData data = mPaletteListFragment.getItem(position);

        if (data == null)
            return;

        switch (option)
        {
            case Rename:
                if (MyDebug.LOG)
                    Log.d(TAG, "Rename palatte item(position=" + position + " , id=" + dataId + ")");
                showRenameDialog(position, dataId);
                break;

            case Delete:
                if (MyDebug.LOG)
                    Log.d(TAG, "Delete palatte item(position=" + position + " , id=" + dataId + ")");
                mPaletteListFragment.remove(data);
                mDataHelper.delete(data);
                break;
                
            case View:
                if (MyDebug.LOG)
                    Log.d(TAG, "View palette item (position=" + position + " , id=" + dataId + ")");
                openPaletteCardView(dataId);
                break;
                
            case Edit:
                if (MyDebug.LOG)
                    Log.d(TAG, "Edit palette item (position=" + position + " , id=" + dataId + ")");
                openEditView(dataId);
                break;
                
            case Favourite:
                if (MyDebug.LOG)
                    Log.d(TAG, "Favourite palette (position=" + position + " , id=" + dataId + ")");
                data.setFavourite(true);
                mDataHelper.update(data, false);
                mPaletteListFragment.update(data);
                break;
                
            case UnFavourite:
                if (MyDebug.LOG)
                    Log.d(TAG, "Unfavourite palette (position=" + position + " , id=" + dataId + ")");
                data.setFavourite(false);
                mDataHelper.update(data, false);
                mPaletteListFragment.update(data);
                break;
        }
    }
    
    @Override
    public void onPaletteDataSorterClicked(Sorter sorter)
    {
        if (MyDebug.LOG)
            Log.d(TAG, "sorter " + sorter + " selected");
        
        mDataSorter = sorter;
        mPaletteListFragment.setSorter(sorter);
    }


    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.palette_item_options:
                long dataId = (Long) view.getTag(R.id.TAG_PALETTE_DATA_ID);
                int itemPosition = (Integer) view.getTag(R.id.TAG_PALETTE_ITEM_POSITION);

                PaletteData data = mPaletteListFragment.getItem(itemPosition);

                if (MyDebug.LOG)
                    Log.d(TAG, "Show options on palette data + " + data);

                PaletteDataOptionsDialogFragment optionDialogFrag =
                        PaletteDataOptionsDialogFragment.newInstance(itemPosition, dataId, data);
                optionDialogFrag.show(getFragmentManager(), "dialog");
                break;
                
            case R.id.btn_loadPicture:
                loadPicture();
                break;
                 
            case R.id.btn_takePhoto:
                takePhoto();
                break;
        }
    }
    
    @Override
    public void onPaletteItemClick(int position, long dataId, PaletteData data)
    {
        if (MyDebug.LOG)
            Log.i(TAG, "palette data " + data.getId() + " clicked");
        
        openPaletteCardView(dataId);
    }
    
    private void openPaletteCardView(long dataId)
    {
        Intent intent = new Intent(this, PaletteCardActivity.class);
        
        intent.putExtra(Constants.PALETTE_DATA_ID, dataId);
        intent.putExtra(Constants.PALETTE_DATA_SORTER_NAME, mDataSorter.getName());
        startActivity(intent);
    }

    @Override
    public void onPaletteItemLongClick(int position, long dataId, PaletteData data)
    {
        if (MyDebug.LOG)
            Log.i(TAG, "palette data " + data.getId() + " long clicked");
        
        PaletteDataOptionsDialogFragment optionDialogFrag =
                PaletteDataOptionsDialogFragment.newInstance(position, dataId, data);
        optionDialogFrag.show(getFragmentManager(), "dialog");
    }


    private void updatePaletteDataTitle(int position, long dataId, String title)
    {
        assert (mPaletteListFragment != null);

        PaletteData data = mPaletteListFragment.getItem(position);

        if (data == null)
            return;

        data.setTitle(title);
        mDataHelper.update(data, /*updateThumb*/false);
        
        data = mDataHelper.get(data.getId());
        mPaletteListFragment.update(data);
    }
    
    private void showAboutDialog()
    {
        // Show about dialog
        AboutDialogFragment dialogFrag = AboutDialogFragment.newInstance(getString(R.string.title_about_view));
        dialogFrag.show(getFragmentManager(), "aboutDialog");
    }

    private void showRenameDialog(final int position, final long dataId)
    {
        assert (mPaletteListFragment != null);

        PaletteData data = mPaletteListFragment.getItem(position);

        if (data == null)
            return;

        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);

        input.setText(data.getTitle());
        input.setSingleLine(true);
        input.setSelectAllOnFocus(true);

        alert.setTitle(R.string.action_rename)
                .setView(input)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        String text = input.getText().toString();
                        updatePaletteDataTitle(position, dataId, text);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);

        final AlertDialog dialog = alert.create();

        input.setOnFocusChangeListener(new OnFocusChangeListener()
            {
                @Override
                public void onFocusChange(View v, boolean hasFocus)
                {
                    if (hasFocus)
                    {
                        dialog.getWindow().setSoftInputMode(
                                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    }
                }
            }
        );

        dialog.show();
    }

    @Override
    public void onFragmentViewCreated(Fragment fragment)
    {
        if (fragment instanceof PaletteListFragment)
        {
            mPaletteListFragment = (PaletteListFragment) fragment;
            mPaletteListFragment.refresh();
        }
        else if (fragment instanceof ColorNameListFragment)
        {
            mColorNameListFragment = (ColorNameListFragment) fragment;
        }
    }

    private File getTempFile(String filename)
    {
        String path = Environment.getExternalStorageDirectory().toString();
        File file = new File(path + File.separator + Constants.FOLDER_HOME + File.separator + 
                             Constants.SUBFOLDER_TEMP + File.separator + filename);
        file.getParentFile().mkdirs();
        
        if (file.exists())
            file.delete();
        
        return file;
    }

    /** Called when the user performs the Take Photo action */
    private void takePhoto()
    {
        if (MyDebug.LOG)
            Log.d(TAG, "take a photo");

        if (ActivityUtils.isIntentAvailable(getBaseContext(), MediaStore.ACTION_IMAGE_CAPTURE))
        {
            mTempFile = getTempFile(Constants.TZPALETTE_TEMP_FILE_NAME);
            
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mTempFile));
            startActivityForResult(takePictureIntent, TAKE_PHOTE_RESULT);
        }
        else
        {
            if (MyDebug.LOG)
                Log.e(TAG, "no camera found");
        }
    }
    
    /** Called when the user performs the Load Picture action */
    private void loadPicture()
    {
        if (MyDebug.LOG)
            Log.d(TAG, "load a picture");
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, LOAD_PICTURE_RESULT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        // Check which request we are responding to
        switch (requestCode)
        {
            case TAKE_PHOTE_RESULT:
                if (resultCode == RESULT_OK)
                {
                    Uri imageUri = Uri.fromFile(mTempFile);
                    
                    if (imageUri != null)
                        openEditView(imageUri);
                }
                break;

            case LOAD_PICTURE_RESULT:
                if (resultCode == RESULT_OK)
                {
                    Uri imageUri = intent.getData();
                    
                    if (imageUri != null)
                        openEditView(imageUri);
                }
                break;
                
            case PALETTE_EDIT_RESULT:
                if (resultCode == RESULT_OK)
                {
                    long dataId = intent.getLongExtra(Constants.PALETTE_DATA_ID, Long.valueOf(-1));
                    boolean addNew = intent.getBooleanExtra(Constants.PALETTE_DATA_ADDNEW, false);
                    
                    PaletteData data = mDataHelper.get(dataId);
                    
                    if (data != null && mPaletteListFragment != null)
                    {
                        if (addNew)
                            mPaletteListFragment.add(data);
                        else
                            mPaletteListFragment.update(data);
                    }
                    
                    // navigate to the palette list view after saving/updating a palette data
                    mPageAdapter.setSelectedPage(PAGE_PALETTE_LIST_POSITION);
                }
                
                /* clean up the temp file when return from the edit view */
                clearTemp();
                break;
        }
    }
    
    private void openEditView(long dataId)
    {
        Intent intent = new Intent(this, PaletteEditActivity.class);
        
        intent.putExtra(Constants.PALETTE_DATA_ID, dataId);
        
        startActivityForResult(intent, PALETTE_EDIT_RESULT);
    }
    
    private void openEditView(Uri selectedImage)
    {
        Intent intent = new Intent(this, PaletteEditActivity.class);
        
        intent.putExtra(Constants.PALETTE_DATA_ID, Long.valueOf(-1));
        intent.setData(selectedImage);
        
        startActivityForResult(intent, PALETTE_EDIT_RESULT);
    }
    
    private void updateOptionMenu()
    {
        invalidateOptionsMenu();
    }

    /**
     * This is a helper class that implements the management of tabs and all details of connecting a
     * ViewPager with associated TabHost. It relies on a trick. Normally a tab host has a simple API
     * for supplying a View or Intent that each tab will show. This is not sufficient for switching
     * between pages. So instead we make the content part of the tab host 0dp high (it is not shown)
     * and the TabsAdapter supplies its own dummy view to show as the tab content. It listens to
     * changes in tabs, and takes care of switch to the correct paged in the ViewPager whenever the
     * selected tab changes.
     */
    public class PageAdapter extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener
    {
        private final Context mContext;
        private final ViewPager mViewPager;
        private final ArrayList<PageInfo> mPages = new ArrayList<PageInfo>();

        final class PageInfo
        {
            private final String title;
            private final Class<?> clss;
            private final Bundle args;

            PageInfo(String _title, Class<?> _class, Bundle _args)
            {
                title = _title;
                clss = _class;
                args = _args;
            }
        }

        public PageAdapter(Activity activity, ViewPager pager)
        {
            super(activity.getFragmentManager());
            mContext = activity;
            mViewPager = pager;
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public int getSelectedPage()
        {
            return mViewPager.getCurrentItem();
        }

        public void setSelectedPage(int position)
        {
            mViewPager.setCurrentItem(position);
        }

        public void addPage(String title, Class<?> clss, Bundle args)
        {
            PageInfo info = new PageInfo(title, clss, args);

            mPages.add(info);
            notifyDataSetChanged();
        }
        
        @Override
        public CharSequence getPageTitle(int position)
        {
            return mPages.get(position).title;
        }

        @Override
        public int getCount()
        {
            return mPages.size();
        }

        @Override
        public Fragment getItem(int position)
        {
            PageInfo info = mPages.get(position);

            return Fragment.instantiate(mContext, info.clss.getName(), info.args);
        }

        @Override
        public void onPageSelected(int position)
        {
            //Invalidate the options menu to re-create them
            updateOptionMenu();
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
        {}

        @Override
        public void onPageScrollStateChanged(int state)
        {}
    }
}
