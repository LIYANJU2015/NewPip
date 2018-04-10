package org.schabi.newpipe.util;

import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.applinks.AppLinkData;

import org.schabi.newpipe.App;
import org.schabi.newpipe.BuildConfig;
import org.schabi.newpipe.R;

import java.util.Locale;

/**
 * Created by liyanju on 2018/4/3.
 */

public class ReferVersions {


    public static void setSuper() {
        SuperVersionHandler.setSuper();
        SuperVersionHandler.setBGPlayer();
    }

    public static void initSuper() {
        SuperVersionHandler.initSpecial();
    }

    public static boolean isSuper() {
        return SuperVersionHandler.isSpecial();
    }

    public static void fetchDeferredAppLinkData(Context context) {
        AppLinkDataHandler.fetchDeferredAppLinkData(context);
    }

    public static MultipleReferrerReceiverHandler createInstallReferrerReceiverHandler() {
        return new MultipleReferrerReceiverHandler();
    }

    public static class SuperVersionHandler {

        private static volatile boolean isSpecial = false;

        private static volatile boolean isBGPlayer = false;

        public static void setSuper() {
            isSpecial = true;
            App.sPreferences.edit().putBoolean(Constants.KEY_SPECIAL, true).apply();
        }

        public static void setBGPlayer() {
            isBGPlayer = true;
            App.sPreferences.edit().putBoolean(Constants.KEY_BG_PLAYER, true).apply();
        }

        public static String getPhoneCountry(Context context) {
            String country = "";
            try {
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (telephonyManager.getPhoneType()
                        != TelephonyManager.PHONE_TYPE_CDMA) {
                    country = telephonyManager.getNetworkCountryIso();
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return country;
        }

        public static String getCountry2(Context context) {
            String country = "";
            try {
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                String simCountry = telephonyManager.getSimCountryIso();
                if (simCountry != null && simCountry.length() == 2) {
                    country = simCountry.toUpperCase(Locale.ENGLISH);
                } else if (telephonyManager.getPhoneType()
                        != TelephonyManager.PHONE_TYPE_CDMA) {
                    country = telephonyManager.getNetworkCountryIso();
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return country;
        }

        public static String getSimCountry(Context context) {
            String country = "";
            try {
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                String simCountry = telephonyManager.getSimCountryIso();
                if (simCountry != null && simCountry.length() == 2) {
                    country = simCountry.toUpperCase(Locale.ENGLISH);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return country;
        }

//        public static String getCountry(Context context) {
//            String country = "";
//            try {
//                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
//                String simCountry = telephonyManager.getSimCountryIso();
//                if (simCountry != null && simCountry.length() == 2) {
//                    country = simCountry.toUpperCase(Locale.ENGLISH);
//                    if (TextUtils.isEmpty(country)) {
//                        country = Locale.getDefault().getCountry();
//                    }
//                } else if (telephonyManager.getPhoneType()
//                        != TelephonyManager.PHONE_TYPE_CDMA) {
//                    country = telephonyManager.getNetworkCountryIso();
//                    if (TextUtils.isEmpty(country)) {
//                        country = Locale.getDefault().getCountry();
//                    }
//                } else {
//                    country = Locale.getDefault().getCountry();
//                    if (!TextUtils.isEmpty(country)) {
//                        country = country.toUpperCase(Locale.ENGLISH);
//                    }
//                }
//            } catch (Throwable e) {
//                e.printStackTrace();
//            }
//            return country;
//        }

        public static void initSpecial() {
            isSpecial = App.sPreferences.getBoolean(Constants.KEY_SPECIAL, false);
            isBGPlayer = App.sPreferences.getBoolean(Constants.KEY_BG_PLAYER, false);
        }

        public static boolean isSpecial() {
            return isSpecial;
        }

        public static boolean isIsBGPlayer() {
            return isBGPlayer;
        }

        public static boolean isReferrerOpen(String referrer) {
            if (referrer.startsWith("campaigntype=")
                    && referrer.contains("campaignid=")) {
                return true;
            } else {
                return false;
            }
        }

        private static boolean countryIfShow2(String country) {
            if ("ph".equals(country.toLowerCase())) {
                FacebookReport.logSentFBRegionOpen("ph");
                return true;
            }

            if ("it".equals(country.toLowerCase())) {
                FacebookReport.logSentFBRegionOpen("it");
                return true;
            }

            if ("de".equals(country.toLowerCase())) {
                FacebookReport.logSentFBRegionOpen("de");
                return true;
            }

            if ("mx".equals(country.toLowerCase())) {
                FacebookReport.logSentFBRegionOpen("mx");
                return true;
            }

            if ("id".equals(country.toLowerCase())) {
                FacebookReport.logSentFBRegionOpen("id");
                return true;
            }

            if ("gb".equals(country.toLowerCase())) {
                FacebookReport.logSentFBRegionOpen("gb");
                return true;
            }

            if ("fr".equals(country.toLowerCase())) {
                FacebookReport.logSentFBRegionOpen("fr");
                return true;
            }

            return false;
        }

        private static boolean countryIfShow(String country) {
            if ("id".equals(country.toLowerCase())) {
                FacebookReport.logSentFBRegionOpen("id");
                return true;
            }

            if ("br".equals(country.toLowerCase())) {
                FacebookReport.logSentFBRegionOpen("br");
                return true;
            }

            if ("in".equals(country.toLowerCase())) {
                FacebookReport.logSentFBRegionOpen("in");
                return true;
            }

            if ("sa".equals(country.toLowerCase())) {
                FacebookReport.logSentFBRegionOpen("sa");
                return true;
            }


            if ("th".equals(country.toLowerCase())) {
                FacebookReport.logSentFBRegionOpen("th");
                return true;
            }

            return false;
        }


        public static void countryIfShow(Context context) {
            String country4 = getPhoneCountry(context);
            String country = getCountry2(context);
            String country3 = getSimCountry(context);

            if (TextUtils.isEmpty(country)) {
                return;
            }

            if (!TextUtils.isEmpty(country4)
                    && !TextUtils.isEmpty(country3)
                    && !country4.equals(country3) && Utils.isRoot()) {
                return;
            }

            if (countryIfShow(country)) {
                setSuper();
            } else if (countryIfShow2(country)) {
                setBGPlayer();
            }

            return;
        }
    }

    public static class AppLinkDataHandler {

        public static void fetchDeferredAppLinkData(Context context) {
            int count = App.sPreferences.getInt("fetchcount", 0);
            if (count < 2) {
                count++;
                App.sPreferences.getInt("fetchcount", count);
                AppLinkData.fetchDeferredAppLinkData(context, context.getString(R.string.facebook_app_id),
                        new AppLinkData.CompletionHandler() {
                            @Override
                            public void onDeferredAppLinkDataFetched(AppLinkData appLinkData) {
                                Log.v("xx", " onDeferredAppLinkDataFetched>>>>");
                                if (appLinkData != null && appLinkData.getTargetUri() != null) {
                                    Log.v("xx", " onDeferredAppLinkDataFetched111>>>>");
                                    String deepLinkStr = appLinkData.getTargetUri().toString();
                                    FacebookReport.logSentFBDeepLink(deepLinkStr);
                                    if (App.DEEPLINK.equals(deepLinkStr)) {
                                        FacebookReport.logSentOpenSuper("facebook");
                                        App.setSuper();
                                    }
                                }
                                App.sPreferences.edit().putInt("fetchcount", 2).apply();
                            }
                        });
            }
        }
    }

    public static class MultipleReferrerReceiverHandler {

        public void onHandleIntent(Context context, Intent intent) {
            String referrer = intent.getStringExtra("referrer");
            if (referrer == null) {
                return;
            }

            boolean result = App.sPreferences.getBoolean("sent_referrer", false);
            if (result) {
                return;
            }
            App.sPreferences.edit().putBoolean("sent_referrer", true).apply();

            if (BuildConfig.DEBUG) {
                Log.e("InstallReferrer:::::", referrer);
            }

            FacebookReport.logSentReferrer(referrer);

            if (SuperVersionHandler.isReferrerOpen(referrer)) {
                if (BuildConfig.DEBUG) {
                    Log.v("super", "issuperOpen true");
                }
                FacebookReport.logSentOpenSuper("google_admob");
                setSuper();
            } else {
                SuperVersionHandler.countryIfShow(context);
            }

            FacebookReport.logSentUserInfo(SuperVersionHandler.getSimCountry(context),
                    SuperVersionHandler.getPhoneCountry(context));
        }
    }
}
