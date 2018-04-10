package org.schabi.newpipe.util;

import android.os.Bundle;

import com.facebook.appevents.AppEventsLogger;

import org.schabi.newpipe.App;

/**
 * Created by liyanju on 2018/4/9.
 */

public class FacebookReport {

    public static void logSentMainPageShow(String service) {
        AppEventsLogger logger = AppEventsLogger.newLogger(App.sContext);
        Bundle bundle = new Bundle();
        bundle.putString("service", service);
        bundle.putString("bgPlayer", App.isBgPlay() ? "true" : "false");
        bundle.putString("isSuper", App.isSuper() ? "true" : "false");
        logger.logEvent("MainPageShow", bundle);
    }

    public static void logSentSearchPageShow(String service) {
        AppEventsLogger logger = AppEventsLogger.newLogger(App.sContext);
        Bundle bundle = new Bundle();
        bundle.putString("service", service);
        bundle.putString("bgPlayer", App.isBgPlay() ? "true" : "false");
        bundle.putString("isSuper", App.isSuper() ? "true" : "false");
        logger.logEvent("SearchPageShow", bundle);
    }

    public static void logSentPopupPageShow() {
        AppEventsLogger logger = AppEventsLogger.newLogger(App.sContext);
        logger.logEvent("PopupPageShow");
    }

    public static void logSentBackgroudPlayerPageShow() {
        AppEventsLogger logger = AppEventsLogger.newLogger(App.sContext);
        logger.logEvent("BackgroudPlayerShow");
    }

    public static void logSentDownloadPageShow() {
        AppEventsLogger logger = AppEventsLogger.newLogger(App.sContext);
        logger.logEvent("DownloadPageShow");
    }

    public static void logSentStartDownload(String title) {
        AppEventsLogger logger = AppEventsLogger.newLogger(App.sContext);
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        logger.logEvent("StartDownload", bundle);
    }

    public static void logSentDownloadFinish(String title) {
        AppEventsLogger logger = AppEventsLogger.newLogger(App.sContext);
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        logger.logEvent("DownloadFinish", bundle);
    }

    public static void logSentFBRegionOpen(String region) {
        AppEventsLogger logger = AppEventsLogger.newLogger(App.sContext);
        Bundle bundle = new Bundle();
        bundle.putString("region", region);
        logger.logEvent("SentFBRegionOpen",bundle);
    }

    public static void logSentUserInfo(String simCode, String phoneCode) {
        AppEventsLogger logger = AppEventsLogger.newLogger(App.sContext);
        Bundle bundle = new Bundle();
        bundle.putString("sim_country", simCode);
        bundle.putString("phone_country", phoneCode);
        bundle.putString("phone_type", android.os.Build.MODEL);
        logger.logEvent("sentUserInfo",bundle);
    }

    public static void logSentFBDeepLink(String deepLink) {
        AppEventsLogger logger = AppEventsLogger.newLogger(App.sContext);
        Bundle bundle = new Bundle();
        bundle.putString("deepLink", deepLink);
        logger.logEvent("SentFBDeepLink",bundle);
    }

    public static void logSentReferrer(String Referrer) {
        AppEventsLogger logger = AppEventsLogger.newLogger(App.sContext);
        Bundle bundle = new Bundle();
        bundle.putString("referrer", Referrer);
        logger.logEvent("SentReferrer",bundle);
    }

    public static void logSentOpenSuper(String source) {
        AppEventsLogger logger = AppEventsLogger.newLogger(App.sContext);
        Bundle bundle = new Bundle();
        bundle.putString("from_source", source);
        logger.logEvent("SentOpenSuper",bundle);
    }
}
