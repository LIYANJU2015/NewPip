package org.playtube.plus.history;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.jakewharton.rxbinding2.view.RxView;

import org.playtube.plus.util.ServiceHelper;
import org.playtube.plus.util.Utils;
import org.playtube.plus.settings.SettingsActivity;

import io.reactivex.android.schedulers.AndroidSchedulers;

public class HistoryActivity extends AppCompatActivity {

    private static final String TAG = "HistoryActivity";
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(org.playtube.plus.R.layout.activity_history);

        Toolbar toolbar = findViewById(org.playtube.plus.R.id.toolbar);
        int color = ServiceHelper.getSelectedServiceId(this) == 0 ? ContextCompat.getColor(this, org.playtube.plus.R.color.light_youtube_primary_color)
                : ContextCompat.getColor(this, org.playtube.plus.R.color.light_soundcloud_primary_color);
        Utils.compat(this, color);
        toolbar.setBackgroundColor(color);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(org.playtube.plus.R.string.title_activity_history);
        }
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(org.playtube.plus.R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = findViewById(org.playtube.plus.R.id.tabs);
        tabLayout.setBackgroundColor(color);
        tabLayout.setupWithViewPager(mViewPager);

        final FloatingActionButton fab = findViewById(org.playtube.plus.R.id.fab);
        RxView.clicks(fab)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(ignored -> {
                    int currentItem = mViewPager.getCurrentItem();
                    HistoryFragment fragment = (HistoryFragment) mSectionsPagerAdapter
                            .instantiateItem(mViewPager, currentItem);
                    fragment.onHistoryCleared();
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(org.playtube.plus.R.menu.menu_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case org.playtube.plus.R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment;
            switch (position) {
                case 0:
                    fragment = SearchHistoryFragment.newInstance();
                    break;
                case 1:
                    fragment = WatchHistoryFragment.newInstance();
                    break;
                default:
                    throw new IllegalArgumentException("position: " + position);
            }
            return fragment;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(org.playtube.plus.R.string.title_history_search);
                case 1:
                    return getString(org.playtube.plus.R.string.title_history_view);
            }
            throw new IllegalArgumentException("position: " + position);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 2;
        }
    }
}
