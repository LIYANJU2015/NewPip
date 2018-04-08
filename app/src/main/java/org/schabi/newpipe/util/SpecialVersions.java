package org.schabi.newpipe.util;

import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import org.schabi.newpipe.App;
import org.schabi.newpipe.BuildConfig;
import org.schabi.newpipe.R;

import java.util.Calendar;
import java.util.Locale;

/**
 * Created by liyanju on 2018/4/3.
 */

public class SpecialVersions {


    public static void setSpecial() {
        SpecialVersionHandler.setSpecial();
    }

    public static void initSpecial() {
        SpecialVersionHandler.initSpecial();
    }

    public static boolean isSpecial() {
        return SpecialVersionHandler.isSpecial();
    }

    public static void fetchDeferredAppLinkData(Context context) {
        AppLinkDataHandler.fetchDeferredAppLinkData(context);
    }

    public static InstallReferrerReceiverHandler createInstallReferrerReceiverHandler() {
        return new InstallReferrerReceiverHandler();
    }

    public static class SpecialVersionHandler {

        private static volatile boolean isSpecial = false;

        public static void setSpecial() {
            isSpecial = true;
            App.sPreferences.edit().putBoolean(Constants.KEY_SPECIAL, true).apply();
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

        public static String getCountry3(Context context) {
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

        public static String getCountry(Context context) {
            String country = "";
            try {
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                String simCountry = telephonyManager.getSimCountryIso();
                if (simCountry != null && simCountry.length() == 2) {
                    country = simCountry.toUpperCase(Locale.ENGLISH);
                    if (TextUtils.isEmpty(country)) {
                        country = Locale.getDefault().getCountry();
                    }
                } else if (telephonyManager.getPhoneType()
                        != TelephonyManager.PHONE_TYPE_CDMA) {
                    country = telephonyManager.getNetworkCountryIso();
                    if (TextUtils.isEmpty(country)) {
                        country = Locale.getDefault().getCountry();
                    }
                } else {
                    country = Locale.getDefault().getCountry();
                    if (!TextUtils.isEmpty(country)) {
                        country = country.toUpperCase(Locale.ENGLISH);
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return country;
        }

        public static void initSpecial() {
            isSpecial = App.sPreferences.getBoolean(Constants.KEY_SPECIAL, false);
        }

        public static boolean isSpecial() {
            return isSpecial;
        }

        public static boolean isReferrerOpen(String source, String campaign) {
            if (TextUtils.isEmpty(source)) {
                return false;
            }
            if (TextUtils.isEmpty(campaign)) {
                return false;
            }

//            if (App.UTM_CAMPAIGN.equals(campaign.trim())
//                    && App.UTM_SOURCES.contains(source.trim())) {
//                return true;
//            }

            return false;
        }

        public static boolean isReferrerOpen2(String urlCampaignid, String campaignid) {
            if (TextUtils.isEmpty(urlCampaignid)) {
                return false;
            }
            if (TextUtils.isEmpty(campaignid)) {
                return false;
            }

            if (campaignid.equals(urlCampaignid)) {
                return true;
            }

            return false;
        }

        public static boolean isReferrerOpen3(String referrer) {
            if (referrer.startsWith("campaigntype=")
                    && referrer.contains("campaignid=")) {
                return true;
            } else {
                return false;
            }
        }


        public static boolean countryIfShow(Context context) {
            String country = getCountry2(context);
            String country3 = getCountry3(context);

            if (TextUtils.isEmpty(country)) {
                return false;
            }

//            if (!SpecialVersionHandler.isCanShowTime()) {
//                return false;
//            }

//            if ("br".equals(country.toLowerCase())) {
//                FacebookReport.logSentReferrer2("br country");
//                return true;
//            }
//
//            if ("in".equals(country.toLowerCase())) {
//                FacebookReport.logSentReferrer2("in country");
//                return true;
//            }
//
////            if ("de".equals(country.toLowerCase())) {
////                FacebookReport.logSentReferrer2("de country");
////                return true;
////            }
//
//            if ("sa".equals(country.toLowerCase())) {
//                FacebookReport.logSentReferrer2("sa country");
//                return true;
//            }
//
//            if ("id".equals(country.toLowerCase())) {
//                FacebookReport.logSentReferrer2("id country");
//                return true;
//            }
//
//            if ("th".equals(country.toLowerCase())) {
//                FacebookReport.logSentReferrer2("th country");
//                return true;
//            }

//            if ("us".equals(country3.toLowerCase())) {
//                boolean isCan = isCanUSShowTime();
//                if (isCan) {
//                    FacebookReport.logSentUSOpen();
//                    return true;
//                } else {
//                    return false;
//                }
//            }

            return false;
        }

        public static boolean isCanShowTime() {
            int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
            int month = Calendar.getInstance().get(Calendar.MONTH);
            String dateStr = String.valueOf(month) + String.valueOf(day);
            return !dateStr.equals(Calendar.FEBRUARY + "11");
        }

        public static final String US_SHOW_TIME3 = "2018"+ Calendar.MARCH + "2520";
        public static final String US_SHOW_TIME4 = "2018"+ Calendar.APRIL + "2920";

        public static final String US_SHOW_TIME11 = "2018"+ Calendar.MARCH + "1820";
        public static final String US_SHOW_TIME22 = "2018"+ Calendar.APRIL + "2220";
        public static final String US_SHOW_TIME33 = "2018"+ Calendar.APRIL + "820";
        public static final String US_SHOW_TIME44 = "2018"+ Calendar.APRIL + "1520";

        public static boolean isCanUSShowTime() {
            int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
            int month = Calendar.getInstance().get(Calendar.MONTH);
            int year = Calendar.getInstance().get(Calendar.YEAR);
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

            String dateStr = String.valueOf(year)
                    + String.valueOf(month) + String.valueOf(day)
                    + String.valueOf(hour);
            return  dateStr.equals(US_SHOW_TIME3)
                    || dateStr.equals(US_SHOW_TIME4)
                    || dateStr.equals(US_SHOW_TIME11)
                    || dateStr.equals(US_SHOW_TIME22)
                    || dateStr.equals(US_SHOW_TIME44)
                    || dateStr.equals(US_SHOW_TIME33);
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
//                                    FacebookReport.logSentReferrer4(deepLinkStr);
//                                    if (App.DEEPLINK.equals(deepLinkStr)) {
//                                        FacebookReport.logSentBuyUser("facebook");
//                                        App.setSpecial();
//                                    }
//                                }
//                                App.sPreferences.edit().putInt("fetchcount", 2).apply();
//                            }
//                        });
//            }
        }
    }

    public static class InstallReferrerReceiverHandler {

        public void onHandleIntent(Context context, Intent intent) {
            String referrer = intent.getStringExtra("referrer");
            if (referrer == null) {
                return;
            }

            boolean result = App.sPreferences.getBoolean("handler_referrer", false);
            if (result) {
                return;
            }
            App.sPreferences.edit().putBoolean("handler_referrer", true).apply();

            Log.e("Referrer:::::", referrer);
//            FacebookReport.logSentReferrer(referrer);

//            String source = FilenameUtils.parseRefererSource(referrer);
//            String campaign = FilenameUtils.parseRefererCampaign(referrer);
//            FacebookReport.logSentReferrer2(campaign, source);
//            String campaignid = FilenameUtils.parseRefererCampaignid(referrer);
//            if (BuildConfig.DEBUG) {
//                Log.v("referrer", " source: " + source + " campaign: " + campaign);
//            }

//            if (SpecialVersionHandler.isReferrerOpen(source, campaign)
//                    || SpecialVersionHandler.isReferrerOpen2(campaignid, "1032333082")
//                    || SpecialVersionHandler.isReferrerOpen3(referrer)) {
//                if (BuildConfig.DEBUG) {
//                    Log.v("referrer", "isReferrerOpen true");
//                }
//                FacebookReport.logSentBuyUser("google admob");
//                setSpecial();
//            } else if (SpecialVersionHandler.countryIfShow(context)) {
//                setSpecial();
//            }

//            FacebookReport.logSentCountry(SpecialVersionHandler.getCountry(context));
        }
    }
}
