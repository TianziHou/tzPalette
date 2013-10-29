package com.tzapps.tzpalette.ui;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.tzapps.common.ui.BaseListFragment;
import com.tzapps.tzpalette.Constants;
import com.tzapps.tzpalette.R;
import com.tzapps.tzpalette.data.PaletteData;
import com.tzapps.tzpalette.data.PaletteDataComparator.Sorter;
import com.tzapps.tzpalette.data.PaletteDataHelper;
import com.tzapps.tzpalette.debug.MyDebug;
import com.tzapps.tzpalette.ui.task.PaletteThumbWorkerTask;
import com.tzapps.tzpalette.ui.task.PaletteThumbWorkerTask.AsyncDrawable;
import com.tzapps.tzpalette.ui.view.ColorRow;

public class PaletteListFragment extends BaseListFragment implements OnItemClickListener, OnItemLongClickListener
{
    private static final String TAG = "PaletteListFragment";
    
    private PaletteDataAdapter<PaletteData> mAdapter;
    private OnClickPaletteItemListener mCallback;
    private Sorter mSorter = Constants.PALETTE_DATA_SORTER_DEFAULT;
    
    public interface OnClickPaletteItemListener
    {
        void onPaletteItemClick(int position, long dataId, PaletteData data);
        void onPaletteItemLongClick(int position, long dataId, PaletteData data);
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        
        // This makes sure the container activity has implemented
        // the callback interface. If not, it throws an exception
        try
        {
            mCallback = (OnClickPaletteItemListener) activity;
        }
        catch (ClassCastException e)
        {
            throw new ClassCastException(activity.toString()
                    + " must implement OnClickPaletteItemListener");
        }

        List<PaletteData> items = new ArrayList<PaletteData>();

        if (mAdapter == null)
            mAdapter = new PaletteDataAdapter<PaletteData>(activity,
                    R.layout.palette_list_view_item, items);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        setListAdapter(mAdapter);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.palette_list_view, container, false);

        return view;
    }

    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        getListView().setOnItemClickListener(this);
        getListView().setOnItemLongClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        PaletteData data = mAdapter.getItem(position);
        mCallback.onPaletteItemClick(position, data.getId(), data);
    }
    
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
    {
        PaletteData data = mAdapter.getItem(position);
        mCallback.onPaletteItemLongClick(position, data.getId(), data);
        return true;
    }
    
    public void refresh()
    {
        mAdapter.sort(mSorter.getComparator());
        mAdapter.notifyDataSetChanged();
    }
    
    public void removeAll()
    {
        mAdapter.clear();
    }

    public void addAll(List<PaletteData> dataList)
    {
        mAdapter.addAll(dataList);
        mAdapter.sort(mSorter.getComparator());
    }

    public void add(PaletteData data)
    {
        mAdapter.add(data);

        if (MyDebug.LOG)
            Log.d(TAG, "palette data " + data.getId() + " added");

        mAdapter.sort(mSorter.getComparator());
        mAdapter.notifyDataSetChanged();
    }

    public void remove(PaletteData data)
    {
        for (int i = 0; i < mAdapter.getCount(); i++)
        {
            PaletteData d = mAdapter.getItem(i);
            
            if (d.getId() == data.getId())
            {
                mAdapter.remove(d);
                break;
            }
        }

        if (MyDebug.LOG)
            Log.d(TAG, "palette data " + data.getId() + " removed");
        
        mAdapter.notifyDataSetChanged();
    }
    
    public void update(PaletteData data)
    {
        for (int i = 0; i < mAdapter.getCount(); i++)
        {
            PaletteData d = mAdapter.getItem(i);
            
            if (d.getId() == data.getId())
            {
                d.copy(data);
                break;
            }
        }
        
        mAdapter.sort(mSorter.getComparator());
        mAdapter.notifyDataSetChanged();
    }
    
    public void setSorter(Sorter sorter)
    {
        mSorter = sorter;
        refresh();
    }
    
    public Sorter getSorter()
    {
        return mSorter;
    }
    
    public PaletteData getItem(int position)
    {
        return mAdapter.getItem(position);
    }

    public class PaletteDataAdapter<T> extends ArrayAdapter<T>
    {
        private Context mContext;

        public PaletteDataAdapter(Context context, int resource, List<T> objects)
        {
            super(context, resource, objects);

            mContext = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View itemView = convertView;
            PaletteData data = (PaletteData) getItem(position);

            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if (itemView == null)
            {
                itemView = inflater.inflate(R.layout.palette_list_view_item, parent, false);
            }

            /*
             * TODO: currently we will update/refresh all palette data in the visible range
             * when notifyDataSetChanged() is called. Which is inefficient and causes 
             * the incorrect flash fade in/out effect on the existing palette item. So the
             * following updateViewByData() needs to modify/refine to fix this issue
             * later, it needs to have some kind of flag to check whether the current
             * palette data matches the current palette item and refresh it if necessary...     
             */
            updateViewByData(itemView, data, position);

            return itemView;
        }

        private void updateViewByData(View itemView, PaletteData data, int position)
        {
            ImageView thumb         = (ImageView)itemView.findViewById(R.id.palette_item_thumb);
            TextView title          = (TextView)itemView.findViewById(R.id.palette_item_title);
            TextView updated        = (TextView)itemView.findViewById(R.id.palette_item_updated);
            ColorRow colorRow       = (ColorRow)itemView.findViewById(R.id.palette_item_colors);
            ImageView options       = (ImageView)itemView.findViewById(R.id.palette_item_options);
            ImageView favourite     = (ImageView)itemView.findViewById(R.id.palette_item_favourite_icon);
            
            /* set PaletteData id and position info into the options button, so that 
             * we could retrieve it when we need to do perform operations on the palette item
             */
            options.setTag(R.id.TAG_PALETTE_ITEM_POSITION, position);
            options.setTag(R.id.TAG_PALETTE_DATA_ID, data.getId());
            
            if (data.getTitle() != null)
                title.setText(data.getTitle());
            else
                title.setText(mContext.getResources().getString(R.string.palette_title_default));
            
            String dateStr = DateUtils.formatDateTime(mContext, data.getUpdated(), DateUtils.FORMAT_SHOW_TIME |
                                                                                   DateUtils.FORMAT_SHOW_DATE |
                                                                                   DateUtils.FORMAT_NUMERIC_DATE);
            
            String updatedStr = getString(R.string.palette_item_updated);
            updatedStr = String.format(updatedStr, dateStr);
            updated.setText(updatedStr);
            
            //disable the colors grid to make it not clickable
            colorRow.setColors(data.getColors());
            colorRow.setFocusable(false);
            colorRow.setEnabled(false);
            
            if (data.isFavourite())
                favourite.setVisibility(View.VISIBLE);
            else
                favourite.setVisibility(View.INVISIBLE);
            
            loadThumb(data.getId(), thumb);
        }
        
        private void loadThumb(long dataId, ImageView imageView)
        {
            if (PaletteThumbWorkerTask.cancelPotentialWork(dataId, imageView))
            {
                final PaletteThumbWorkerTask task = 
                        new PaletteThumbWorkerTask(mContext, imageView, /*smallThumb*/true);
                
                //TODO: add a place holder bitmap
                final AsyncDrawable asyncDrawble = 
                        new AsyncDrawable(getResources(), null, task);
                imageView.setImageDrawable(asyncDrawble);
                task.execute(dataId);
            }
        }
    }
    
}
