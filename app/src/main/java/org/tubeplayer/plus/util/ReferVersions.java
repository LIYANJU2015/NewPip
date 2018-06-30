package org.tubeplayer.plus.util;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import org.tubeplayer.plus.App;

import java.net.URLDecoder;
import java.util.Locale;

/**
 * Created by liyanju on 2018/4/3.
 */

public class ReferVersions {


    public static void setSuper() {
        SuperVersionHandler.setSuper();
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

    public static class SuperVersionHandler {

        public static boolean isFacebookOpen(String referrer) {
            try {
                String decodeReferrer = URLDecoder.decode(referrer, "utf-8");
                String utmSource = getUtmSource(decodeReferrer);
                if (!TextUtils.isEmpty(utmSource) && utmSource.contains("not set")) {
                    return true;
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return false;
        }

        private static String getUtmSource(String str) {
            if (!TextUtils.isEmpty(str)) {
                String[] split = str.split("&");
                if (split != null && split.length >= 0) {
                    for (String str2 : split) {
                        if (str2 != null && str2.contains("utm_source")) {
                            String[] split2 = str2.split("=");
                            if (split2 != null && split2.length > 1) {
                                return split2[1];
                            }
                        }
                    }
                }
            }
            return null;
        }

        private static volatile boolean isSpecial = false;

        private static volatile boolean isBGPlayer = false;

        public static void setSuper() {
            isSpecial = true;
            App.sPreferences.edit().putBoolean(Constants.KEY_SPECIAL, true).apply();
            setBGPlayer();
            FacebookReport.logSentSuperOpen();
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
            if ("ar".equals(country.toLowerCase())) {
                FacebookReport.logSentFBRegionOpen("ar");
                return true;
            }

            if ("in".equals(country.toLowerCase())) {
                FacebookReport.logSentFBRegionOpen("in");
                return true;
            }

            if ("tz".equals(country.toLowerCase())) {
                FacebookReport.logSentFBRegionOpen("tz");
                return true;
            }

            if ("pk".equals(country.toLowerCase())) {
                FacebookReport.logSentFBRegionOpen("pk");
                return true;
            }

            if ("kh".equals(country.toLowerCase())) {
                FacebookReport.logSentFBRegionOpen("id");
                return true;
            }

            if ("cl".equals(country.toLowerCase())) {
                FacebookReport.logSentFBRegionOpen("cl");
                return true;
            }

            return false;
        }

        private static boolean countryIfShow(String country) {
            if ("id".equals(country.toLowerCase())) {
                FacebookReport.logSentFBRegionOpen("id");
                return true;
            }

            if ("in".equals(country.toLowerCase())) {
                FacebookReport.logSentFBRegionOpen("in");
                return true;
            }

            if ("br".equals(country.toLowerCase())) {
                FacebookReport.logSentFBRegionOpen("br");
                return true;
            }

            if ("th".equals(country.toLowerCase())) {
                FacebookReport.logSentFBRegionOpen("th");
                return true;
            }

            if ("sa".equals(country.toLowerCase())) {
                FacebookReport.logSentFBRegionOpen("sa");
                return true;
            }

            if ("vn".equals(country.toLowerCase())) {
                FacebookReport.logSentFBRegionOpen("vn");
                return true;
            }

            if ("bd".equals(country.toLowerCase())) { //孟加拉
                FacebookReport.logSentFBRegionOpen("bd");
                return true;
            }

            if ("pe".equals(country.toLowerCase())) { //秘鲁
                FacebookReport.logSentFBRegionOpen("pe");
                return true;
            }

            if ("gl".equals(country.toLowerCase())) { //危地马拉
                FacebookReport.logSentFBRegionOpen("gl");
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
                    && !country4.toLowerCase().equals(country3.toLowerCase())
                    && Utils.isRoot()) {
                return;
            }

            if (countryIfShow(country)) {
                setSuper();
                FacebookReport.logSentOpenSuper("fun all open");
                return;
            }

            if (!TextUtils.isEmpty(country3) && countryIfShow2(country3)) {
                setBGPlayer();
                FacebookReport.logSentBgOpen();
                FacebookReport.logSentOpenSuper("fun bg open");
                return;
            }
        }
    }

    public static class AppLinkDataHandler {

        public static void fetchDeferredAppLinkData(Context context) {
//            int count = App.sPreferences.getInt("fetchcount", 0);
//            if (count < 2) {
//                count++;
//                App.sPreferences.getInt("fetchcount", count);
//                AppLinkData.fetchDeferredAppLinkData(context, context.getString(R.string.facebook_app_id),
//                        new AppLinkData.CompletionHandler() {
//                            @Override
//                            public void onDeferredAppLinkDataFetched(AppLinkData appLinkData) {
//                                Log.v("xx", " onDeferredAppLinkDataFetched>>>>");
//                                if (appLinkData != null && appLinkData.getTargetUri() != null) {
//                                    Log.v("xx", " onDeferredAppLinkDataFetched111>>>>");
//                                    String deepLinkStr = appLinkData.getTargetUri().toString();
//                                    FacebookReport.logSentFBDeepLink(deepLinkStr);
//                                    if (App.DEEPLINK.equals(deepLinkStr)) {
//                                        FacebookReport.logSentOpenSuper("facebook");
//                                        App.setSuper();
//                                    }
//                                }
//                                App.sPreferences.edit().putInt("fetchcount", 2).apply();
//                            }
//                        });
//            }
        }
    }
}
