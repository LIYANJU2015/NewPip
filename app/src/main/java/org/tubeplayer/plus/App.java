package org.tubeplayer.plus;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.nostra13.universalimageloader.cache.memory.impl.LRULimitedMemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;
import com.tencent.bugly.crashreport.CrashReport;

import org.tubeplayer.plus.util.ExtractorHelper;
import org.tubeplayer.plus.util.FBAdUtils;
import org.tubeplayer.plus.util.ReferVersions;
import org.tubeplayer.plus.util.StateSaver;
import org.schabi.newpipe.extractor.Downloader;
import org.schabi.newpipe.extractor.NewPipe;
import org.tubeplayer.plus.settings.SettingsActivity;
import org.tubeplayer.plus.util.Constants;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

import io.reactivex.annotations.NonNull;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.exceptions.OnErrorNotImplementedException;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;

/*
 * Copyright (C) Hans-Christoph Steiner 2016 <hans@eds.org>
 * App.java is part of NewPipe.
 *
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */

public class App extends Application {
    protected static final String TAG = App.class.toString();
    private RefWatcher refWatcher;

    public static SharedPreferences sPreferences;

    public static Context sContext;

    public static boolean sIsCoolLaunch = false;


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(base);
    }

    public static boolean isSuper() {
        return ReferVersions.isSuper();
    }

    public static boolean isBgPlay() {
        return ReferVersions.SuperVersionHandler.isIsBGPlayer();
    }

    public static void setSuper() {
        ReferVersions.setSuper();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }

        sIsCoolLaunch = true;

        refWatcher = installLeakCanary();
        sContext = this;
        sPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        FBAdUtils.init(this);
        FBAdUtils.get().loadFBAds(Constants.NATIVE_AD);

        ReferVersions.initSuper();
        CrashReport.initCrashReport(this);

        // Initialize settings first because others inits can use its values
        SettingsActivity.initSettings(this);

        NewPipe.init(getDownloader());
        StateSaver.init(this);
        initNotificationChannel();

        // Initialize image loader
        ImageLoader.getInstance().init(getImageLoaderConfigurations(10, 50));

        configureRxJavaErrorHandler();

        ReferVersions.fetchDeferredAppLinkData(this);

        if (!sPreferences.getBoolean("shortcut", false)) {
            sPreferences.edit().putBoolean("shortcut", true).apply();
            addShortcut(sContext, SplashActivity.class, getString(R.string.app_name), R.mipmap.ic_launcher);
        }
    }

    public static void addShortcut(Context context, Class clazz, String appName, int ic_launcher) {
        Intent shortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");

        Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
        shortcutIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        shortcutIntent.putExtra("tName", appName);
        shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, appName);
        shortcutIntent.setClassName(context, clazz.getName());
        //        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // 快捷名称
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, context.getResources().getString(R.string.app_name));
        // 快捷图标是否允许重复
        shortcut.putExtra("duplicate", false);
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        // 快捷图标
        Intent.ShortcutIconResource iconRes = Intent.ShortcutIconResource.fromContext(context, ic_launcher);
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconRes);
        // 发送广播
        context.sendBroadcast(shortcut);
    }

    protected Downloader getDownloader() {
        return org.tubeplayer.plus.Downloader.init(null);
    }

    private void configureRxJavaErrorHandler() {
        // https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0#error-handling
        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            @Override
            public void accept(@NonNull Throwable throwable) throws Exception {
                Log.e(TAG, "RxJavaPlugins.ErrorHandler called with -> : " +
                        "throwable = [" + throwable.getClass().getName() + "]");

                if (throwable instanceof UndeliverableException) {
                    // As UndeliverableException is a wrapper, get the cause of it to get the "real" exception
                    throwable = throwable.getCause();
                }

                final List<Throwable> errors;
                if (throwable instanceof CompositeException) {
                    errors = ((CompositeException) throwable).getExceptions();
                } else {
                    errors = Collections.singletonList(throwable);
                }

                Log.e(TAG, "RxJavaPlugin: Undeliverable Exception received: ", throwable);

//                for (final Throwable error : errors) {
//                    if (isThrowableIgnored(error)) return;
//                    if (isThrowableCritical(error)) {
//                        reportException(error);
//                        return;
//                    }
//                }
//
//                // Out-of-lifecycle exceptions should only be reported if a debug user wishes so,
//                // When exception is not reported, log it
//                if (isDisposedRxExceptionsReported()) {
//                    reportException(throwable);
//                } else {
//                    Log.e(TAG, "RxJavaPlugin: Undeliverable Exception received: ", throwable);
//                }
            }

            private boolean isThrowableIgnored(@NonNull final Throwable throwable) {
                // Don't crash the application over a simple network problem
                return ExtractorHelper.hasAssignableCauseThrowable(throwable,
                        IOException.class, SocketException.class, // network api cancellation
                        InterruptedException.class, InterruptedIOException.class); // blocking code disposed
            }

            private boolean isThrowableCritical(@NonNull final Throwable throwable) {
                // Though these exceptions cannot be ignored
                return ExtractorHelper.hasAssignableCauseThrowable(throwable,
                        NullPointerException.class, IllegalArgumentException.class, // bug in app
                        OnErrorNotImplementedException.class, MissingBackpressureException.class,
                        IllegalStateException.class); // bug in operator
            }

            private void reportException(@NonNull final Throwable throwable) {
                // Throw uncaught exception that will trigger the report system
                Thread.currentThread().getUncaughtExceptionHandler()
                        .uncaughtException(Thread.currentThread(), throwable);
            }
        });
    }

    private ImageLoaderConfiguration getImageLoaderConfigurations(final int memoryCacheSizeMb,
                                                                  final int diskCacheSizeMb) {
        return new ImageLoaderConfiguration.Builder(this)
                .memoryCache(new LRULimitedMemoryCache(memoryCacheSizeMb * 1024 * 1024))
                .diskCacheSize(diskCacheSizeMb * 1024 * 1024)
                .imageDownloader(new ImageDownloader(getApplicationContext()))
                .build();
    }

    public void initNotificationChannel() {
        if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            return;
        }

        final String id = getString(R.string.notification_channel_id);
        final CharSequence name = getString(R.string.notification_channel_name);
        final String description = getString(R.string.notification_channel_description);

        // Keep this below DEFAULT to avoid making noise on every notification update
        final int importance = NotificationManager.IMPORTANCE_LOW;

        NotificationChannel mChannel = new NotificationChannel(id, name, importance);
        mChannel.setDescription(description);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.createNotificationChannel(mChannel);
    }

    @Nullable
    public static RefWatcher getRefWatcher(Context context) {
        final App application = (App) context.getApplicationContext();
        return application.refWatcher;
    }

    protected RefWatcher installLeakCanary() {
        return RefWatcher.DISABLED;
    }

    protected boolean isDisposedRxExceptionsReported() {
        return false;
    }
}
