package org.schabi.newpipe.util;

import android.os.Bundle;

import com.facebook.appevents.AppEventsLogger;

import org.schabi.newpipe.App;

/**
 * Created by liyanju on 2018/4/9.
 */

public class FacebookReport {

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
