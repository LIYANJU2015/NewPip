package org.playtube.plus.settings;

import android.os.Bundle;

public class VideoAudioSettingsFragment extends BasePreferenceFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(org.playtube.plus.R.xml.video_audio_settings);
    }
}
