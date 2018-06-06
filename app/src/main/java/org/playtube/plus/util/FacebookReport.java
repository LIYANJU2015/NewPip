package org.playtube.plus.util;

import android.os.Bundle;

import com.facebook.appevents.AppEventsLogger;

import org.playtube.plus.App;

/**
 * Created by liyanju on 2018/4/9.
 */

public class FacebookReport {

    public static void logSentSuperOpen() {
        AppEventsLogger logger = AppEventsLogger.newLogger(App.sContext);
        logger.logEvent("logSuperOpen");
    }

    public static void logSentRating(String str) {
        AppEventsLogger logger = AppEventsLogger.newLogger(App.sContext);
        Bundle bundle = new Bundle();
        bundle.putString("rating", str);
        logger.logEvent("logRating", bundle);
    }

    public static void logSentBgOpen() {
        AppEventsLogger logger = AppEventsLogger.newLogger(App.sContext);
        logger.logEvent("logBackgroudPlayer open");
    }

    public static void logSentMainPageShow(String service) {
        AppEventsLogger logger = AppEventsLogger.newLogger(App.sContext);
        Bundle bundle = new Bundle();
        bundle.putString("service", service);
        bundle.putString("bgPlayer", App.isBgPlay() ? "true" : "false");
        bundle.putString("isSuper", App.isSuper() ? "true" : "false");
        logger.logEvent("logMainPageShow", bundle);
    }

    public static void logSentSearchPageShow(String service) {
        AppEventsLogger logger = AppEventsLogger.newLogger(App.sContext);
        Bundle bundle = new Bundle();
        bundle.putString("service", service);
        bundle.putString("backgPlayer", App.isBgPlay() ? "true" : "false");
        bundle.putString("isfater", App.isSuper() ? "true" : "false");
        logger.logEvent("logSearchPageShow", bundle);
    }

    public static void logSentPopupPageShow() {
        AppEventsLogger logger = AppEventsLogger.newLogger(App.sContext);
        Bundle bundle = new Bundle();
        bundle.putString("backgPlayer", App.isBgPlay() ? "true" : "false");
        bundle.putString("isfaster", App.isSuper() ? "true" : "false");
        logger.logEvent("logPopupPageShow", bundle);
    }

    public static void logSentBackgroudPlayerPageShow() {
        AppEventsLogger logger = AppEventsLogger.newLogger(App.sContext);
        logger.logEvent("logBackgroudPlayerShow");
    }

    public static void logSentDownloadPageShow() {
        AppEventsLogger logger = AppEventsLogger.newLogger(App.sContext);
        logger.logEvent("logDownloadPageShow");
    }

    public static void logSentStartDownload(String title) {
        AppEventsLogger logger = AppEventsLogger.newLogger(App.sContext);
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        logger.logEvent("logStartDownload", bundle);
    }

    public static void logSentDownloadFinish(String title) {
        AppEventsLogger logger = AppEventsLogger.newLogger(App.sContext);
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        logger.logEvent("logDownloadFinish", bundle);
    }

    public static void logSentFBRegionOpen(String region) {
        AppEventsLogger logger = AppEventsLogger.newLogger(App.sContext);
        Bundle bundle = new Bundle();
        bundle.putString("area", region);
        logger.logEvent("logSentFBRegionOpen",bundle);
    }

    public static void logSentUserInfo(String simCode, String phoneCode) {
        AppEventsLogger logger = AppEventsLogger.newLogger(App.sContext);
        Bundle bundle = new Bundle();
        bundle.putString("sim_ct", simCode);
        bundle.putString("phone_ct", phoneCode);
        bundle.putString("phone_type", android.os.Build.MODEL);
        logger.logEvent("logSentUserInfo",bundle);
    }


    public static void logSentReferrer(String Referrer) {
        AppEventsLogger logger = AppEventsLogger.newLogger(App.sContext);
        Bundle bundle = new Bundle();
        bundle.putString("referrer", Referrer);
        logger.logEvent("logSentReferrer",bundle);
    }

    public static void logSentOpenSuper(String source) {
        AppEventsLogger logger = AppEventsLogger.newLogger(App.sContext);
        Bundle bundle = new Bundle();
        bundle.putString("from", source);
        logger.logEvent("logSentOpenApp",bundle);
    }
}
