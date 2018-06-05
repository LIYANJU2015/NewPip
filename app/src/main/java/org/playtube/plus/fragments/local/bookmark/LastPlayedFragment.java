package org.playtube.plus.fragments.local.bookmark;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.ads.Ad;

import org.playtube.plus.database.stream.StreamStatisticsEntry;
import org.playtube.plus.util.Constants;
import org.playtube.plus.util.FBAdUtils;

import java.util.Collections;
import java.util.List;

public final class LastPlayedFragment extends StatisticsPlaylistFragment {
    @Override
    protected String getName() {
        return getString(org.playtube.plus.R.string.title_last_played);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FBAdUtils.get().interstitialLoad(Constants.INERSTITIAL_HIGH_AD, new FBAdUtils.FBInterstitialAdListener(){
            @Override
            public void onInterstitialDismissed(Ad ad) {
                super.onInterstitialDismissed(ad);
                FBAdUtils.get().destoryInterstitial();
            }
        });
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            if (FBAdUtils.get().isInterstitialLoaded()) {
                FBAdUtils.get().showInterstitial();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            FBAdUtils.get().destoryInterstitial();
        }
    }

    @Override
    protected List<StreamStatisticsEntry> processResult(List<StreamStatisticsEntry> results)  {
        Collections.sort(results, (left, right) ->
                right.latestAccessDate.compareTo(left.latestAccessDate));
        return results;
    }
}
