package org.playtube.plus.settings;

import android.os.Bundle;

import org.playtube.plus.App;
import org.playtube.plus.BuildConfig;

public class MainSettingsFragment extends BasePreferenceFragment {
    public static final boolean DEBUG = !BuildConfig.BUILD_TYPE.equals("release");

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        if (App.isSuper()) {
            addPreferencesFromResource(org.playtube.plus.R.xml.main_settings);
        } else {
            addPreferencesFromResource(org.playtube.plus.R.xml.main_settings2);
        }
    }
}
