package org.playtube.plus.util;

import android.graphics.Bitmap;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

public class ImageDisplayConstants {
    private static final int BITMAP_FADE_IN_DURATION_MILLIS = 250;

    /**
     * Base display options
     */
    private static final DisplayImageOptions BASE_DISPLAY_IMAGE_OPTIONS =
            new DisplayImageOptions.Builder()
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
                    .resetViewBeforeLoading(true)
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .imageScaleType(ImageScaleType.EXACTLY)
                    .displayer(new FadeInBitmapDisplayer(BITMAP_FADE_IN_DURATION_MILLIS))
                    .build();

    /*//////////////////////////////////////////////////////////////////////////
    // DisplayImageOptions default configurations
    //////////////////////////////////////////////////////////////////////////*/

    public static final DisplayImageOptions DISPLAY_AVATAR_OPTIONS =
            new DisplayImageOptions.Builder()
                    .cloneFrom(BASE_DISPLAY_IMAGE_OPTIONS)
                    .showImageForEmptyUri(org.playtube.plus.R.drawable.buddy)
                    .showImageOnFail(org.playtube.plus.R.drawable.buddy)
                    .build();

    public static final DisplayImageOptions DISPLAY_THUMBNAIL_OPTIONS =
            new DisplayImageOptions.Builder()
                    .cloneFrom(BASE_DISPLAY_IMAGE_OPTIONS)
                    .showImageOnLoading(org.playtube.plus.R.drawable.item_thumbnail)
                    .showImageForEmptyUri(org.playtube.plus.R.drawable.item_thumbnail)
                    .showImageOnFail(org.playtube.plus.R.drawable.item_thumbnail)
                    .build();

    public static final DisplayImageOptions DISPLAY_BANNER_OPTIONS =
            new DisplayImageOptions.Builder()
                    .cloneFrom(BASE_DISPLAY_IMAGE_OPTIONS)
                    .showImageForEmptyUri(org.playtube.plus.R.drawable.channel_banner)
                    .showImageOnFail(org.playtube.plus.R.drawable.channel_banner)
                    .build();

    public static final DisplayImageOptions DISPLAY_PLAYLIST_OPTIONS =
            new DisplayImageOptions.Builder()
                    .cloneFrom(BASE_DISPLAY_IMAGE_OPTIONS)
                    .showImageOnLoading(org.playtube.plus.R.drawable.dummy_thumbnail_playlist)
                    .showImageForEmptyUri(org.playtube.plus.R.drawable.dummy_thumbnail_playlist)
                    .showImageOnFail(org.playtube.plus.R.drawable.dummy_thumbnail_playlist)
                    .build();
}
