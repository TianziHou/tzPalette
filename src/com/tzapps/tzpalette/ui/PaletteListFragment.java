package com.tzapps.tzpalette.ui;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.tzapps.tzpalette.R;
import com.tzapps.tzpalette.data.PaletteData;
import com.tzapps.tzpalette.data.PaletteDataSource;
import com.tzapps.ui.BaseListFragment;

public class PaletteListFragment extends BaseListFragment implements OnItemClickListener
{
    private static final String TAG = "PaletteListFragment";
    
    private PaletteDataSource mSource;
    PaletteDataAdapter<PaletteData> mAdapter;
    
    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        
        if (mSource == null)
            mSource = new PaletteDataSource(activity);
        
        mSource.open();
        
        List<PaletteData> items = mSource.getAllPaletteData();
        
        if (mAdapter == null)
            mAdapter = new PaletteDataAdapter<PaletteData>(getActivity(), 
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

        Log.d(TAG, "onCreateView()");
        
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.palette_list_view, container, false);
        
        return view;
    }
    
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        
        getListView().setOnItemClickListener(this);
    }
    
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        PaletteData data = mAdapter.getItem(position);
        Log.i(TAG, "palette data " + data.getId() + " clicked");
        
        //mSource.delete(data);
        //adapter.remove(data);
        //adapter.notifyDataSetChanged();
    }
    
    @Override
    public void onResume()
    {
        mSource.open();
        super.onResume();
    }
    
    @Override
    public void onPause()
    {
        mSource.close();
        super.onPause();
    }
    
    public void save(PaletteData data)
    {
        mSource.save(data);
        mAdapter.add(data);
        
        Log.d(TAG, "palette data " + data.getId() + " saved");
        
        mAdapter.notifyDataSetChanged();
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
            PaletteData data = (PaletteData)getItem(position);
            
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            
            if (itemView == null)
            {
                itemView = inflater.inflate(R.layout.palette_list_view_item, parent, false);
            }
            
            updateViewByData(itemView, data);
            
            return itemView;
        }
        
        private void updateViewByData(View itemView, PaletteData data)
        {
            ImageView thumb = (ImageView)itemView.findViewById(R.id.palette_item_thumb);
            TextView title = (TextView)itemView.findViewById(R.id.palette_item_title);
            TextView updated = (TextView)itemView.findViewById(R.id.palette_item_updated);
            PaletteColorGrid colors = (PaletteColorGrid)itemView.findViewById(R.id.palette_item_colors);
            ImageButton options = (ImageButton)itemView.findViewById(R.id.palette_item_options);
            
            // set PaletteData id onto the options button, so that we could retrieve it when
            // we need to do perform operations on the palette item
            options.setTag(data.getId());
            
            if (data.getTitle() != null)
                title.setText(data.getTitle());
            else
                title.setText(mContext.getResources().getString(R.string.palette_title_default));
            
            String dateStr = DateUtils.formatDateTime(mContext, data.getUpdated(), DateUtils.FORMAT_SHOW_TIME |
                                                                                   DateUtils.FORMAT_SHOW_DATE |
                                                                                   DateUtils.FORMAT_NUMERIC_DATE);
            updated.setText(dateStr);
            colors.setColors(data.getColors());
            thumb.setImageBitmap(data.getThumb());
      
            // TODO: update other palette data values into item view
        }
    }  
    
}

