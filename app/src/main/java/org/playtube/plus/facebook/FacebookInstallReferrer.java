package org.playtube.plus.facebook;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by liyanju on 2018/6/5.
 */

public class FacebookInstallReferrer extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            FacebookReferrerHandler.onHandler(context, intent);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
