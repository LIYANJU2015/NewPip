package org.tubeplayer.plus.settings;

import android.os.Bundle;

public class VideoAudioSettingsFragment extends BasePreferenceFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(org.tubeplayer.plus.R.xml.video_audio_settings);
    }
}
