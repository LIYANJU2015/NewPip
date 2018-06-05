package org.schabi.newpipe.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;

import org.schabi.newpipe.App;
import org.schabi.newpipe.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by liyanju on 2018/4/8.
 */

public class Utils {

    private static final int INVALID_VAL = -1;
    private static final int COLOR_DEFAULT = Color.parseColor("#20000000");

    private static Handler sHandler = new Handler(Looper.getMainLooper());

    public static View getToolbarMoreView(Context context, Toolbar toolbar) {
        int count = toolbar.getChildCount();
        for (int i = 0; i < count; i++) {
            View view = toolbar.getChildAt(i);
            if (view instanceof LinearLayoutCompat) {
                count = ((LinearLayoutCompat) view).getChildCount();
                for (int j = 0; j < count; j++) {
                    view = ((LinearLayoutCompat) view).getChildAt(j);
                    if (view instanceof ImageView) {
                        return view;
                    }

                }
                return null;
            }
        }
        return null;
    }

    public static void runUIThead(Runnable runnable) {
        sHandler.post(runnable);
    }

    public static void runUIThreadDelay(Runnable runnable, long delay) {
        sHandler.postDelayed(runnable, delay);
    }

    public static DisplayMetrics getMetrics(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        return metrics;
    }

    public static void checkAndRequestPermissions(Activity activity) {
        try {
            ArrayList<String> permissionList = new ArrayList<>();
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            checkAndRequestPermissions(activity, permissionList);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static void checkAndRequestPermissions(Activity activity, ArrayList<String> permissionList) {
        ArrayList<String> list = new ArrayList<>(permissionList);
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            String permission = it.next();
            //检查权限是否已经申请
            int hasPermission = ContextCompat.checkSelfPermission(activity, permission);
            if (hasPermission == PackageManager.PERMISSION_GRANTED) {
                it.remove();
            }
        }

        if (list.size() == 0) {
            return;
        }
        String[] permissions = list.toArray(new String[0]);
        //正式请求权限
        ActivityCompat.requestPermissions(activity, permissions, 101);
    }

    public static void setStatusColor(Activity activity) {
        int color = ServiceHelper.getSelectedServiceId(activity) == 0 ? ContextCompat.getColor(activity, R.color.light_youtube_primary_color)
                : ContextCompat.getColor(activity, R.color.light_soundcloud_primary_color);
        Utils.compat(activity, color);
    }

    public static boolean isRoot(){
        boolean bool = false;

        try{
            if ((!new File("/system/bin/su").exists())
                    && (!new File("/system/xbin/su").exists())){
                bool = false;
            } else {
                bool = true;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return bool;
    }

    public static boolean isScreenOn() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 20) {
                // I'm counting
                // STATE_DOZE, STATE_OFF, STATE_DOZE_SUSPENDED
                // all as "OFF"
                DisplayManager dm = (DisplayManager) App.sContext.getSystemService(Context.DISPLAY_SERVICE);
                Display[] displays = dm.getDisplays();
                for (Display display : displays) {
                    if (display.getState() == Display.STATE_ON
                            || display.getState() == Display.STATE_UNKNOWN) {
                        return true;
                    }
                }
                return false;
            }

            // If you use less than API20:
            PowerManager powerManager = (PowerManager) App.sContext.getSystemService(Context.POWER_SERVICE);
            if (powerManager.isScreenOn()) {
                return true;
            }
            return false;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return true;
    }


    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static void compat(Activity activity, int statusColor) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (statusColor != INVALID_VAL) {
                    activity.getWindow().setStatusBarColor(statusColor);
                }
                return;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
