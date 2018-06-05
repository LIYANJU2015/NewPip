package org.playtube.plus.facebook;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.playtube.plus.App;
import org.playtube.plus.BuildConfig;
import org.playtube.plus.util.FacebookReport;
import org.playtube.plus.util.ReferVersions;

/**
 * Created by liyanju on 2018/6/5.
 */

public class FacebookReferrerHandler {

    public static void onHandler(Context context, Intent intent) {
        String referrer = intent.getStringExtra("referrer");
        if (referrer == null) {
            return;
        }

        boolean result = App.sPreferences.getBoolean("handle_referrer", false);
        if (result) {
            return;
        }
        App.sPreferences.edit().putBoolean("handle_referrer", true).apply();

        if (BuildConfig.DEBUG) {
            Log.e("Installrr:::::", referrer);
        } else {
            if (!App.sPreferences.getBoolean("canRefer", true)) {
                Log.e("Referrer", "canRefer false ");
                return;
            }
        }

        FacebookReport.logSentReferrer(referrer);

        if (ReferVersions.SuperVersionHandler.isReferrerOpen(referrer)) {
            if (BuildConfig.DEBUG) {
                Log.v("super", "isfasterOpen true");
            }
            FacebookReport.logSentOpenSuper("open for admob");
            ReferVersions.SuperVersionHandler.setSuper();
        } else {
            ReferVersions.SuperVersionHandler.countryIfShow(context);
        }

        FacebookReport.logSentUserInfo(ReferVersions.SuperVersionHandler.getSimCountry(context),
                ReferVersions.SuperVersionHandler.getPhoneCountry(context));
    }
}
