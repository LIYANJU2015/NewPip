package org.tubeplayer.plus.download;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewTreeObserver;

import com.facebook.ads.Ad;

import org.tubeplayer.plus.R;
import org.tubeplayer.plus.util.FBAdUtils;
import org.tubeplayer.plus.util.FacebookReport;
import org.tubeplayer.plus.util.Utils;
import org.tubeplayer.plus.settings.SettingsActivity;
import org.tubeplayer.plus.util.Constants;
import org.tubeplayer.plus.util.ServiceHelper;

import com.xunlei.fone.service.DownloadManagerService;
import com.xunlei.fone.ui.fragment.AllMissionsFragment;
import com.xunlei.fone.ui.fragment.MissionsFragment;

public class DownloadActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Service
        Intent i = new Intent();
        i.setClass(this, DownloadManagerService.class);
        startService(i);

        super.onCreate(savedInstanceState);
        setContentView(org.tubeplayer.plus.R.layout.activity_downloader);

        Toolbar toolbar = findViewById(org.tubeplayer.plus.R.id.toolbar);
//        int color = ServiceHelper.getSelectedServiceId(this) == 0 ? ContextCompat.getColor(this, org.tubeplayer.plus.R.color.light_youtube_primary_color)
//                : ContextCompat.getColor(this, org.tubeplayer.plus.R.color.light_soundcloud_primary_color);
//        Utils.compat(this, color);
//        toolbar.setBackgroundColor(color);
        Utils.compat(this, ContextCompat.getColor(this, R.color.color_cccccc));
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(org.tubeplayer.plus.R.string.downloads_title);
            actionBar.setDisplayShowTitleEnabled(true);
        }

        // Fragment
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                updateFragments();
                getWindow().getDecorView().getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });

        FacebookReport.logSentDownloadPageShow();

        FBAdUtils.get().interstitialLoad(Constants.INERSTITIAL_HIGH_AD, new FBAdUtils.FBInterstitialAdListener(){
            @Override
            public void onInterstitialDismissed(Ad ad) {
                super.onInterstitialDismissed(ad);
                FBAdUtils.get().destoryInterstitial();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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

    private void updateFragments() {

        MissionsFragment fragment = new AllMissionsFragment();
        getFragmentManager().beginTransaction()
                .replace(org.tubeplayer.plus.R.id.frame, fragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commitAllowingStateLoss();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(org.tubeplayer.plus.R.menu.download_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                return true;
            }
            case org.tubeplayer.plus.R.id.action_settings: {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
