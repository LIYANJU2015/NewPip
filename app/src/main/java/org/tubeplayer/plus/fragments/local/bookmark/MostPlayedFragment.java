package org.tubeplayer.plus.fragments.local.bookmark;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.ads.Ad;

import org.tubeplayer.plus.App;
import org.tubeplayer.plus.R;
import org.tubeplayer.plus.database.stream.StreamStatisticsEntry;
import org.tubeplayer.plus.util.Constants;
import org.tubeplayer.plus.util.FBAdUtils;

import java.util.Collections;
import java.util.List;

public final class MostPlayedFragment extends StatisticsPlaylistFragment {
    @Override
    protected String getName() {
        return getString(R.string.title_most_played);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (App.isSuper() || App.isBgPlay()) {
            FBAdUtils.get().interstitialLoad(Constants.INERSTITIAL_HIGH_AD, new FBAdUtils.FBInterstitialAdListener() {
                @Override
                public void onInterstitialDismissed(Ad ad) {
                    super.onInterstitialDismissed(ad);
                    FBAdUtils.get().destoryInterstitial();
                }
            });
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (App.isSuper() || App.isBgPlay()) {
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
    }

    @Override
    protected List<StreamStatisticsEntry> processResult(List<StreamStatisticsEntry> results)  {
        Collections.sort(results, (left, right) ->
                ((Long) right.watchCount).compareTo(left.watchCount));
        return results;
    }

}
