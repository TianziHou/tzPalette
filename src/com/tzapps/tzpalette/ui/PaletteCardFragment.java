package com.tzapps.tzpalette.ui;

import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.tzapps.common.ui.BaseFragment;
import com.tzapps.common.utils.BitmapUtils;
import com.tzapps.common.utils.MediaHelper;
import com.tzapps.tzpalette.Constants;
import com.tzapps.tzpalette.R;
import com.tzapps.tzpalette.data.PaletteData;
import com.tzapps.tzpalette.data.PaletteDataHelper;
import com.tzapps.tzpalette.ui.view.ColorRow;

public class PaletteCardFragment extends BaseFragment
{
    private static final String TAG = "PaletteCardFragment";
    
    private TextView mTitle;
    private ImageView mThumb;
    private ColorRow mColorRow;
    private PaletteData mData;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.palette_card_view, container, false);
        
        mTitle = (TextView)view.findViewById(R.id.palette_card_title);
        mThumb = (ImageView)view.findViewById(R.id.palette_card_thumb);
        mColorRow = (ColorRow)view.findViewById(R.id.palette_card_colors);
        
        refresh();
        
        return view;
    }
    
    public void setData(PaletteData data)
    {
        mData = data;
        refresh();
    }
    
    public PaletteData getData()
    {
        return mData;
    }
    
    private void refresh()
    {
        if (mData == null)
            return;
        
        if (mTitle != null)
            mTitle.setText(mData.getTitle());
        
        if (mColorRow != null)
            mColorRow.setColors(mData.getColors());
        
        if (mThumb != null)
            updateThumb();
    }
    
    private void updateThumb()
    {
        String imageUrl = mData.getImageUrl();
        
        assert (imageUrl != null);
        
        Bitmap bitmap = PaletteDataHelper.getInstance(getActivity()).getThumb(mData.getId());
        
        /* try to get the cached image first, if it doesn't exist then
         * to fetch its original image instead 
         */
        if (bitmap == null)
        {
            Uri imageUri  = Uri.parse(imageUrl);

            /*
             * FIXME: it cannot handle with the imageUri correctly if its
             * content is "content://com.google.android.gallery3d.provider/picasa"
             * i.e. the picture from picasa app, it refers to the uri permission
             * grant logic, and might be worse in previous android version (3.0, 4.0, 
             * 4.1). I will need to test it and evaluate this bug on other lower 
             * android version.
             * 
             * A useful reference: http://dimitar.me/how-to-get-picasa-images-using-the-image-picker-on-android-devices-running-any-os-version/
             */
        
            bitmap = BitmapUtils.getBitmapFromUri(getActivity(), imageUri, Constants.THUMB_MAX_SIZE);
        
            if (bitmap != null)
            {
                int orientation = MediaHelper.getPictureOrientation(getActivity(), imageUri);
                
                if (orientation != ExifInterface.ORIENTATION_NORMAL)
                    bitmap = BitmapUtils.getRotatedBitmap(bitmap, orientation);
            }
        }
        
        //Add a tag here to save the palette data id on the thumb
        //then in onClick() methed when the thumb is clicked, we
        //could open the palette edit view
        mThumb.setTag(mData.getId());
        
        mThumb.setImageBitmap(bitmap);
    }
    
}
