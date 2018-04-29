package org.schabi.newpipe.download;

import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewTreeObserver;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.schabi.newpipe.App;
import org.schabi.newpipe.R;
import org.schabi.newpipe.settings.SettingsActivity;
import org.schabi.newpipe.util.FacebookReport;
import org.schabi.newpipe.util.ServiceHelper;
import org.schabi.newpipe.util.ThemeHelper;
import org.schabi.newpipe.util.Utils;

import us.shandian.giga.service.DownloadManagerService;
import us.shandian.giga.ui.fragment.AllMissionsFragment;
import us.shandian.giga.ui.fragment.MissionsFragment;
import us.shandian.giga.util.Utility;

public class DownloadActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Service
        Intent i = new Intent();
        i.setClass(this, DownloadManagerService.class);
        startService(i);

        ThemeHelper.setTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloader);

        Toolbar toolbar = findViewById(R.id.toolbar);
        int color = ServiceHelper.getSelectedServiceId(this) == 0 ? ContextCompat.getColor(this, R.color.light_youtube_primary_color)
                : ContextCompat.getColor(this, R.color.light_soundcloud_primary_color);
        Utils.compat(this, color);
        toolbar.setBackgroundColor(color);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.downloads_title);
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

        showRating();
    }

    private void showRating() {
        if (isFinishing()) {
            return;
        }

        if (App.sPreferences.getBoolean("isShowRating", false)) {
            return;
        }

        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .canceledOnTouchOutside(true)
                .content(R.string.rating_download_tips)
                .positiveText(R.string.five_star)
                .cancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        App.sPreferences.edit().putBoolean("isShowRating", true).apply();
                    }
                })
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                        Utility.goToGP(App.sContext);
                        FacebookReport.logSentRating("five rating");
                        App.sPreferences.edit().putBoolean("isShowRating", true).apply();
                    }
                })
                .negativeText(R.string.cancel)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                        FacebookReport.logSentRating("not five");
                    }
                })
                .title(R.string.rating)
                .build();
        dialog.show();
    }

    private void updateFragments() {

        MissionsFragment fragment = new AllMissionsFragment();
        getFragmentManager().beginTransaction()
                .replace(R.id.frame, fragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.download_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                return true;
            }
            case R.id.action_settings: {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
