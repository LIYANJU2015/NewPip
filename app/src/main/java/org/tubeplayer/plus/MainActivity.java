/*
 * Created by Christian Schabesberger on 02.08.16.
 * <p>
 * Copyright (C) Christian Schabesberger 2016 <chris.schabesberger@mailbox.org>
 * DownloadActivity.java is part of NewPipe.
 * <p>
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tubeplayer.plus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import org.tubeplayer.plus.fragments.MainFragment;
import org.tubeplayer.plus.util.FBAdUtils;
import org.tubeplayer.plus.util.FacebookReport;
import org.tubeplayer.plus.util.StateSaver;
import org.schabi.newpipe.extractor.StreamingService;
import org.tubeplayer.plus.fragments.BackPressable;
import org.tubeplayer.plus.fragments.detail.VideoDetailFragment;
import org.tubeplayer.plus.fragments.list.search.SearchFragment;
import org.tubeplayer.plus.util.Constants;
import org.tubeplayer.plus.util.NavigationHelper;
import org.tubeplayer.plus.util.ServiceHelper;
import org.tubeplayer.plus.util.Utils;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static final boolean DEBUG = !BuildConfig.BUILD_TYPE.equals("release");

    /*//////////////////////////////////////////////////////////////////////////
    // Activity's LifeCycle
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG) Log.d(TAG, "onCreate() called with: savedInstanceState = [" + savedInstanceState + "]");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Utils.compat(this, ContextCompat.getColor(this, R.color.color_cccccc));

        if (getSupportFragmentManager() != null && getSupportFragmentManager().getBackStackEntryCount() == 0) {
            initFragments();
        }

//        int color = ServiceHelper.getSelectedServiceId(this) == 0 ? ContextCompat.getColor(this, R.color.light_youtube_primary_color)
//                : ContextCompat.getColor(this, R.color.light_soundcloud_primary_color);
//        findViewById(R.id.toolbar).setBackgroundColor(color);
//        findViewById(R.id.toolbar_search_edit_text).setBackgroundColor(color);

        setSupportActionBar(findViewById(R.id.toolbar));

        FacebookReport.logSentMainPageShow();

        if (App.sPreferences.getBoolean("canRefer", true)) {
            Utils.runUIThreadDelay(new Runnable() {
                @Override
                public void run() {
                    App.sPreferences.edit().putBoolean("canRefer", false).apply();
                }
            }, 1000);
        }

        Utils.checkAndRequestPermissions(MainActivity.this);
        FBAdUtils.get().showAdDialog(this, Constants.NATIVE_AD_HIGHT);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isChangingConfigurations()) {
            StateSaver.clearStateFiles();
        }

        FBAdUtils.get().loadFBAds(Constants.NATIVE_AD);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean(Constants.KEY_THEME_CHANGE, false)) {
            if (DEBUG) Log.d(TAG, "Theme has changed, recreating activity...");
            sharedPreferences.edit().putBoolean(Constants.KEY_THEME_CHANGE, false).apply();
            // https://stackoverflow.com/questions/10844112/runtimeexception-performing-pause-of-activity-that-is-not-resumed
            // Briefly, let the activity resume properly posting the recreate call to end of the message queue
            new Handler(Looper.getMainLooper()).post(MainActivity.this::recreate);
        }

        if (sharedPreferences.getBoolean(Constants.KEY_MAIN_PAGE_CHANGE, false)) {
            if (DEBUG) Log.d(TAG, "main page has changed, recreating main fragment...");
            sharedPreferences.edit().putBoolean(Constants.KEY_MAIN_PAGE_CHANGE, false).apply();
            NavigationHelper.openMainActivity(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (DEBUG) Log.d(TAG, "onNewIntent() called with: intent = [" + intent + "]");
        if (intent != null) {
            // Return if launched from a launcher (e.g. Nova Launcher, Pixel Launcher ...)
            // to not destroy the already created backstack
            String action = intent.getAction();
            if ((action != null && action.equals(Intent.ACTION_MAIN)) && intent.hasCategory(Intent.CATEGORY_LAUNCHER)) return;
        }

        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    public void onBackPressed() {
        if (DEBUG) Log.d(TAG, "onBackPressed() called");

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
        // If current fragment implements BackPressable (i.e. can/wanna handle back press) delegate the back press to it
        if (fragment instanceof BackPressable) {
            if (((BackPressable) fragment).onBackPressed()) return;
        }


        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            moveTaskToBack(true);
            FBAdUtils.get().loadFBAds(Constants.NATIVE_AD);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Implement the following diagram behavior for the up button:
     * <pre>
     *              +---------------+
     *              |  Main Screen  +----+
     *              +-------+-------+    |
     *                      |            |
     *                      ▲ Up         | Search Button
     *                      |            |
     *                 +----+-----+      |
     *    +------------+  Search  |◄-----+
     *    |            +----+-----+
     *    |   Open          |
     *    |  something      ▲ Up
     *    |                 |
     *    |    +------------+-------------+
     *    |    |                          |
     *    |    |  Video    <->  Channel   |
     *    +---►|  Channel  <->  Playlist  |
     *         |  Video    <->  ....      |
     *         |                          |
     *         +--------------------------+
     * </pre>
     */
    private void onHomeButtonPressed() {
        // If search fragment wasn't found in the backstack...
        if (!NavigationHelper.tryGotoSearchFragment(getSupportFragmentManager())) {
            // ...go to the main fragment
            NavigationHelper.gotoMainFragment(getSupportFragmentManager());
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Menu
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (DEBUG) Log.d(TAG, "onCreateOptionsMenu() called with: menu = [" + menu + "]");
        super.onCreateOptionsMenu(menu);

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
        if (!(fragment instanceof VideoDetailFragment)) {
            findViewById(R.id.toolbar).findViewById(R.id.toolbar_spinner).setVisibility(View.GONE);
        }

        if (!(fragment instanceof SearchFragment)) {
            findViewById(R.id.toolbar).findViewById(R.id.toolbar_search_container).setVisibility(View.GONE);

            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.main_menu, menu);
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        updateDrawerNavigation();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (DEBUG) Log.d(TAG, "onOptionsItemSelected() called with: item = [" + item + "]");
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                onHomeButtonPressed();
                return true;
            case R.id.action_settings:
                NavigationHelper.openSettings(this);
                return true;
            case R.id.action_show_downloads:
                return NavigationHelper.openDownloads(this);
            case R.id.action_about:
                NavigationHelper.openSettings(this);
                return true;
            case R.id.action_history:
                NavigationHelper.openHistory(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Init
    //////////////////////////////////////////////////////////////////////////*/

    private void initFragments() {
        if (DEBUG) Log.d(TAG, "initFragments() called");
        StateSaver.clearStateFiles();
        if (getIntent() != null && getIntent().hasExtra(Constants.KEY_LINK_TYPE)) {
            handleIntent(getIntent());
        } else NavigationHelper.gotoMainFragment(getSupportFragmentManager());
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Utils
    //////////////////////////////////////////////////////////////////////////*/

    private void updateDrawerNavigation() {
        if (getSupportActionBar() == null) return;

        final Toolbar toolbar = findViewById(R.id.toolbar);

        final Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
        if (fragment instanceof MainFragment) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        } else {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> onHomeButtonPressed());
        }
    }

    private void handleIntent(Intent intent) {
        if (DEBUG) Log.d(TAG, "handleIntent() called with: intent = [" + intent + "]");

        if (intent.hasExtra(Constants.KEY_LINK_TYPE)) {
            String url = intent.getStringExtra(Constants.KEY_URL);
            int serviceId = intent.getIntExtra(Constants.KEY_SERVICE_ID, 0);
            String title = intent.getStringExtra(Constants.KEY_TITLE);
            switch (((StreamingService.LinkType) intent.getSerializableExtra(Constants.KEY_LINK_TYPE))) {
                case STREAM:
                    boolean autoPlay = intent.getBooleanExtra(VideoDetailFragment.AUTO_PLAY, false);
                    NavigationHelper.openVideoDetailFragment(getSupportFragmentManager(), serviceId, url, title, autoPlay);
                    break;
                case CHANNEL:
                    NavigationHelper.openChannelFragment(getSupportFragmentManager(), serviceId, url, title);
                    break;
                case PLAYLIST:
                    NavigationHelper.openPlaylistFragment(getSupportFragmentManager(), serviceId, url, title);
                    break;
            }
        } else if (intent.hasExtra(Constants.KEY_OPEN_SEARCH)) {
            String searchQuery = intent.getStringExtra(Constants.KEY_QUERY);
            if (searchQuery == null) searchQuery = "";
            int serviceId = intent.getIntExtra(Constants.KEY_SERVICE_ID, 0);
            NavigationHelper.openSearchFragment(getSupportFragmentManager(), serviceId, searchQuery);
        } else {
            NavigationHelper.gotoMainFragment(getSupportFragmentManager());
        }
    }
}
