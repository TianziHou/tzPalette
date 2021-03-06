package com.tzapps.tzpalette.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.tzapps.common.utils.BitmapUtils;
import com.tzapps.common.utils.ColorUtils;
import com.tzapps.tzpalette.Constants;
import com.tzapps.tzpalette.R;
import com.tzapps.tzpalette.algorithm.ClusterCenter;
import com.tzapps.tzpalette.algorithm.ClusterPoint;
import com.tzapps.tzpalette.algorithm.KMeansProcessor;
import com.tzapps.tzpalette.db.PaletteDataSource;
import com.tzapps.tzpalette.debug.MyDebug;
import com.tzapps.tzpalette.ui.SettingsFragment;

public class PaletteDataHelper
{
    private static final String TAG = "PaletteDataHelper";
    
    private static PaletteDataHelper instance;
    
    private Context mContext;
    private PaletteDataSource mDataSource;
    
    private PaletteDataHelper(Context context)
    {
        mContext = context;
        mDataSource = PaletteDataSource.getInstance(context);
    };
    
    public static PaletteDataHelper getInstance(Context context)
    {
        if (instance == null)
            instance = new PaletteDataHelper(context.getApplicationContext());
        
        return instance;
    }
    
    public void openDb(boolean writable)
    {
        mDataSource.open(writable);
    }
    
    public void closeDb()
    {
        mDataSource.close();
    }
    
    public void delete(PaletteData data)
    {
        mDataSource.delete(data);
    }
    
    public void delete(long id)
    {
        mDataSource.delete(id);
    }
    
    public void deleteAll()
    {
        mDataSource.deleteAll();
    }
    
    public long add(PaletteData data)
    {
        long id = -1;
        
        id = mDataSource.add(data, getThumbQuality());
        
        return id;
    }
    
    public Bitmap getThumb(long id)
    {
        Bitmap bitmap = null;
        
        bitmap = mDataSource.getThumb(id);
        
        return bitmap;
    }
    
    public Bitmap getThumbSmall(long id)
    {
        Bitmap bitmap = null;
        
        bitmap = mDataSource.getThumbSmall(id);
        
        return bitmap;
    }
    
    public PaletteData get(long id)
    {
        PaletteData data = null;
        
        data = mDataSource.get(id);
        
        return data;
    }
    
    public void update(PaletteData data, boolean updateThumb)
    {
        mDataSource.update(data, updateThumb, getThumbQuality());
    }
    
    /**
     * Get the palette data count stored in database
     * 
     * @return the count of palette data
     */
    public int getDataCount()
    {
        int count = 0;
        count = mDataSource.count();
        return count;
    }
    
    public List<PaletteData> getAllData()
    {
        List<PaletteData> dataList = null;
        dataList = mDataSource.getAllPaletteData();
        return dataList;
    }
    
    private int getInt(int resId)
    {
        return mContext.getResources().getInteger(resId);
    }
    
    /** 
     * Get thumb quality settings from shared perference and 
     * convert it to int values
     */
    private int getThumbQuality()
    {
        int quality;
        
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        String qualityStr = sp.getString(SettingsFragment.KEY_PREF_CACHE_THUMB_QUALITY, 
                                         Constants.CACHE_THUMB_QUALITY_DEFAULT);
        
        if (qualityStr.equalsIgnoreCase("HIGH"))
            quality = 100;
        else if (qualityStr.equalsIgnoreCase("MEDIUM"))
            quality = 85;
        else if (qualityStr.equalsIgnoreCase("LOW"))
            quality = 70;
        else
            quality = 85;
        
        return quality;
    }

    /**
     * Analysis the thumb in palette data to pick up its main colors
     * automatically.
     * 
     * @param data          the PaletteData data to analysis
     * @param reset         flag to indicate if remove the existing colors
     */
    public void analysis(PaletteData data, boolean reset)
    {
        int numOfColors, deviation;
        PaletteDataType dataType; 
        
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        
        numOfColors = sp.getInt(SettingsFragment.KEY_PREF_COLOR_NUMBER, getInt(R.integer.pref_setColorNumber_default));
        String accuracy = sp.getString(SettingsFragment.KEY_PREF_ANALYSIS_ACCURACY, Constants.ANALYSIS_ACCURACY_DEFAULT);
        
        if (accuracy.equalsIgnoreCase("HIGH"))
            deviation = 0;
        else if (accuracy.equalsIgnoreCase("NORMAL"))
            deviation = 5;
        else if (accuracy.equalsIgnoreCase("LOW"))
            deviation = 10;
        else
            deviation = 5;
        
        String colorType = sp.getString(SettingsFragment.KEY_PREF_COLOR_TYPE, Constants.ANALYSIS_COLOR_TYPE_DEFAULT);
        dataType = PaletteDataType.fromString(colorType);
        
        boolean enableKpp = sp.getBoolean(SettingsFragment.KEY_PREF_ENABLE_KPP, true);
        
        if (MyDebug.LOG)
            Log.d(TAG, "analysis: numOfColors=" + numOfColors + " deviation=" + deviation + " dataType=" + dataType + " enableKpp=" + enableKpp);
        
        analysis(data, reset, numOfColors, deviation, dataType, enableKpp);
    }
    
    /**
     * Analysis the thumb in palette data to pick up its main colors
     * automatically.
     * 
     * @param data          the PaletteData data to analysis
     * @param reset         flag to indicate if remove the existing colors
     * @param numOfColors   number of colors to pick up
     * @param deviation     deviation to control how precise the analysis it is
     * @param dataType      the color type (RGB or HSV) when do the color analysis
     * @param enableKpp     flag to indicate if enable the kpp process
     */
    public void analysis(PaletteData data, boolean reset, int numOfColors, int deviation, PaletteDataType dataType, boolean enableKpp)
    {
        if (MyDebug.LOG)
            Log.d(TAG, "palette data analysis()");
        
        Bitmap bitmap = getThumb(data.getId());
        
        /* try to get the bitmap from cached thumb directly, and if 
         * it doesn't exist then get it from its original url
         */
        if (bitmap == null)
            bitmap = BitmapUtils.getBitmapFromUri(mContext, Uri.parse(data.getImageUrl()), Constants.THUMB_MAX_SIZE);
        
        assert(bitmap != null);
        
        if (bitmap == null)
        {
            if (MyDebug.LOG)
                Log.e(TAG, "cannot load a picture from palette data!");
            return;
        }
        
        if (reset)
            data.clearColors();
        
        // initialization the pixel data
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        int maxWidth = Constants.PICTURE_ANALYSIS_MAX_SIZE;
        int maxHeight = Constants.PICTURE_ANALYSIS_MAX_SIZE;
        
        // scale the bitmap to limit its size, so the k-mean processor could have a reasonable process time
        if (width > maxWidth || height > maxHeight )
        {
            bitmap = BitmapUtils.resizeBitmapToFitFrame(bitmap, maxWidth, maxHeight);
            width = bitmap.getWidth();
            height = bitmap.getHeight();
        }
        
        int[] inPixels = new int[width*height];
        bitmap.getPixels(inPixels, 0, width, 0, 0, width, height);
        
        ClusterPoint[] points = new ClusterPoint[inPixels.length];
        for (int i = 0; i < inPixels.length; i++)
            points[i] = new ClusterPoint(convertColor(inPixels[i], dataType));
        
        KMeansProcessor proc = new KMeansProcessor(numOfColors, deviation, /*maxRound*/99, false);   
        proc.processKMean(points);
        
        
        List<Integer> colors = new ArrayList<Integer>();
        
        for (ClusterCenter center : proc.getClusterCenters())
        {
            /*
             * So we use the nearest point values rather than the
             * cluster center directly, in case that some times
             * there will be some weird non-existing colors caught
             * after the analysis...
             */
            int[] values = center.getNearestPoint().getValues();
            
            int color = 0;
            
            switch(dataType)
            {
                case ColorToRGB:
                    color = ColorUtils.rgbToColor(values);
                    break;
                
                case ColorToHSV:
                    color = ColorUtils.hsvToColor(values);
                    break;
                    
                case ColorToHSL:
                    color = ColorUtils.hslToColor(values);
                    break;
                    
                case ColorToLAB:
                    color = ColorUtils.labToColor(values);
                    break;
            }
            
            colors.add(color);
        }
        
        //Sort color before add them into PaletteData
        Collections.sort(colors, ColorUtils.colorSorter);
        
        data.addColors(colors, /*reset*/true);
    }
    
    /**
     * Convert the color value into Cluster point values
     */
    private int[] convertColor(int color, PaletteDataType type)
    {
        switch(type)
        {
            case ColorToRGB:
                return ColorUtils.colorToRGB(color);
                
            case ColorToHSV:
                return ColorUtils.colorToHSV(color);
                
            case ColorToHSL:
                return ColorUtils.colorToHSL(color);
                
            case ColorToLAB:
                return ColorUtils.colorToLAB(color);
               
            default:
                int[] values = new int[1];
                values[0] = color;
                return values;
        }
    }
    
}
