package org.tubeplayer.plus.settings;

import android.os.Bundle;

import org.tubeplayer.plus.App;
import org.tubeplayer.plus.BuildConfig;

public class MainSettingsFragment extends BasePreferenceFragment {
    public static final boolean DEBUG = !BuildConfig.BUILD_TYPE.equals("release");

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        if (App.isSuper()) {
            addPreferencesFromResource(org.tubeplayer.plus.R.xml.main_settings);
        } else {
            addPreferencesFromResource(org.tubeplayer.plus.R.xml.main_settings2);
        }
    }
}
