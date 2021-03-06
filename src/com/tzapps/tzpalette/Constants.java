package com.tzapps.tzpalette;

import com.tzapps.tzpalette.data.PaletteDataComparator.Sorter;

public final class Constants
{
    /** The key name for passing palette data id between activities */
    public static final String PALETTE_DATA_ID = "com.tzapps.tzpalette.PaletteDataId";
    /** The key name for passing palette data isAddNew flag between activities */
    public static final String PALETTE_DATA_ADDNEW = "com.tzapps.tzpalette.PaletteDataAddNew";
    /** The key name for passing paletter data sorter name between activities */
    public static final String PALETTE_DATA_SORTER_NAME = "com.tzapps.tzpalette.PaletteDataSorterName";
    
    /** The folder home of tzpalette app */
    public static final String FOLDER_HOME = "tzpalette";
    /** The sub export directory under the home folder */ 
    public static final String SUBFOLDER_EXPORT = "export";
    /** The prefix for file in tzpalette app */
    public static final String TZPALETTE_FILE_PREFIX = "MyPalette_";
    /** The sub temp directory under the home folder */
    public static final String SUBFOLDER_TEMP = "temp";
    /** The temporary file name in the temp folder */
    public static final String TZPALETTE_TEMP_FILE_NAME = "MyPalette";
    /** The temporary file name for palette card in the temp folder */
    public static final String TZPALETTE_TEMP_SHARE_FILE_NAME_CARD = "card.jpg";
    /** The temporary file name for color list in the temp folder */
    public static final String TZPALETTE_TEMP_SHARE_FILE_NAME_COLORS = "colors.jpg";
    
    /** The maximum picture size in color analysis process. */
    public static final int PICTURE_ANALYSIS_MAX_SIZE = 512;
    /** The maximum thumb size stored in local db. */
    public static final int THUMB_MAX_SIZE = 1024;
    /** The maximum thumb_small size stored in local db. */
    public static final int THUMB_SMALL_MAX_SIZE = 256;
    /** The maximum color number for each palette. */
    public static final int COLOR_SLOT_MAX_SIZE = 8;
    /** The width of exported palette card */
    public static final int PALETTE_CARD_EXPORT_WIDTH = 600;
    
    /** The default palette data sorter */
    public static final Sorter PALETTE_DATA_SORTER_DEFAULT = Sorter.Title;
    
    /** The default color analysis accuracy. */
    public static final String ANALYSIS_ACCURACY_DEFAULT = "NORMAL";
    /** The default color analysis type */
    public static final String ANALYSIS_COLOR_TYPE_DEFAULT = "RGB";
    /** The default thumb quality */
    public static final String CACHE_THUMB_QUALITY_DEFAULT = "MEDIUM";
    
    /** The palette card off screen read limit number */
    public static final int PALETTE_CARD_PAGE_OFFSCREEN_LIMIT = 5;
    
    /** The color info similar color deviation */
    public static final int COLOR_INFO_SIMILAR_DEVIATION = 20;
    /** The color info more action base web url */
    public static final String COLRO_INFO_MORE_BASE_URL = "http://www.colorhexa.com/";
}


