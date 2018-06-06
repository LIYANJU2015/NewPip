package org.playtube.plus.fragments;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;

import org.playtube.plus.R;
import org.playtube.plus.fragments.list.feed.FeedFragment;
import org.playtube.plus.App;
import org.playtube.plus.BaseFragment;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.kiosk.KioskList;
import org.playtube.plus.fragments.list.channel.ChannelFragment;
import org.playtube.plus.fragments.list.kiosk.KioskFragment;
import org.playtube.plus.fragments.local.bookmark.BookmarkFragment;
import org.playtube.plus.fragments.subscription.SubscriptionFragment;
import org.playtube.plus.report.ErrorActivity;
import org.playtube.plus.report.UserAction;
import org.playtube.plus.util.KioskTranslator;
import org.playtube.plus.util.NavigationHelper;
import org.playtube.plus.util.ServiceHelper;

public class MainFragment extends BaseFragment implements BottomNavigationView.OnNavigationItemSelectedListener {

    public int currentServiceId = -1;
    private ViewPager viewPager;

    /*//////////////////////////////////////////////////////////////////////////
    // Constants
    //////////////////////////////////////////////////////////////////////////*/

    private static final int FALLBACK_SERVICE_ID = ServiceList.YouTube.getServiceId();
    private static final String FALLBACK_CHANNEL_URL = "https://www.youtube.com/channel/UC-9-kyTW8ZkZNDHQJ6FgpwQ";
    private static final String FALLBACK_CHANNEL_NAME = "Music";
    private static final String FALLBACK_KIOSK_ID = "Trending";
    private static final int KIOSK_MENU_OFFSET = 2000;

    private BottomNavigationView mBNavigation;

    /*//////////////////////////////////////////////////////////////////////////
    // Fragment's LifeCycle
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        currentServiceId = ServiceHelper.getSelectedServiceId(activity);
        return inflater.inflate(org.playtube.plus.R.layout.fragment_main, container, false);
    }

    @Override
    protected void initViews(View rootView, Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);

        mBNavigation = rootView.findViewById(R.id.main_navigation);
        mBNavigation.setOnNavigationItemSelectedListener(this);
        viewPager = rootView.findViewById(R.id.pager);

        /*  Nested fragment, use child fragment here to maintain backstack in view pager. */
        PagerAdapter adapter = new PagerAdapter(getChildFragmentManager());
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(adapter.getCount());
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){
            @Override
            public void onPageSelected(int position) {
                int menuId = mBNavigation.getMenu().getItem(position).getItemId();
                mBNavigation.setSelectedItemId(menuId);
            }
        });

        if (isSubscriptionsPageOnlySelected()) {
            mBNavigation.inflateMenu(R.menu.navigation_two);
        } else {
            mBNavigation.inflateMenu(R.menu.navigation_three);
        }

        int[][] states = new int[][]{
                new int[]{-android.R.attr.state_checked},
                new int[]{android.R.attr.state_checked}
        };
        int[] colors;
        if (ServiceHelper.getSelectedServiceId(activity) == 0) {
            colors = new int[]{ getResources().getColor(R.color.color_606060),
                    getResources().getColor(R.color.color_ee1919)
            };
        } else {
            colors = new int[]{ getResources().getColor(R.color.color_606060),
                    getResources().getColor(R.color.color_f9800)
            };
        }

        ColorStateList csl = new ColorStateList(states, colors);
        mBNavigation.setItemIconTintList(csl);
        mBNavigation.setItemTextColor(csl);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewPager.clearOnPageChangeListeners();
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Menu
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (DEBUG) Log.d(TAG, "onCreateOptionsMenu() called with: menu = [" + menu + "], inflater = [" + inflater + "]");
        inflater.inflate(R.menu.main_fragment_menu, menu);
        SubMenu kioskMenu = menu.addSubMenu(Menu.NONE, Menu.NONE, 200, getString(org.playtube.plus.R.string.kiosk));
        if (App.isSuper()) {
            menu.add(Menu.NONE, Menu.NONE, 200, org.playtube.plus.R.string.download)
                    .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            NavigationHelper.openDownloads(activity);
                            return false;
                        }
                    });
        }
        try {
            createKioskMenu(kioskMenu, inflater);
        } catch (Exception e) {
            ErrorActivity.reportError(activity, e,
                    activity.getClass(),
                    null,
                    ErrorActivity.ErrorInfo.make(UserAction.UI_ERROR,
                            "none", "", org.playtube.plus.R.string.app_ui_crash));
        }

        ActionBar supportActionBar = activity.getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case org.playtube.plus.R.id.action_search:
                NavigationHelper.openSearchFragment(getFragmentManager(), ServiceHelper.getSelectedServiceId(activity), "");
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Tabs
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        viewPager.setCurrentItem(item.getOrder());
        return true;
    }

    private class PagerAdapter extends FragmentPagerAdapter {
        PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return isSubscriptionsPageOnlySelected() ? new SubscriptionFragment() : getMainPageFragment();
                case 2:
                    if(PreferenceManager.getDefaultSharedPreferences(getActivity())
                            .getString(getString(org.playtube.plus.R.string.main_page_content_key), getString(org.playtube.plus.R.string.blank_page_key))
                            .equals(getString(org.playtube.plus.R.string.subscription_page_key))) {
                        return new BookmarkFragment();
                    } else {
                        return new SubscriptionFragment();
                    }
                case 1:
                    return new BookmarkFragment();
                default:
                    return new BlankFragment();
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            //return getString(this.tabTitles[position]);
            return "";
        }

        @Override
        public int getCount() {
            return isSubscriptionsPageOnlySelected() ? 2 : 3;
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Main page content
    //////////////////////////////////////////////////////////////////////////*/

    private boolean isSubscriptionsPageOnlySelected() {
        return PreferenceManager.getDefaultSharedPreferences(activity)
                .getString(getString(org.playtube.plus.R.string.main_page_content_key), getString(org.playtube.plus.R.string.blank_page_key))
                .equals(getString(org.playtube.plus.R.string.subscription_page_key));
    }

    private Fragment getMainPageFragment() {
        if (getActivity() == null) return new BlankFragment();

        try {
            SharedPreferences preferences =
                    PreferenceManager.getDefaultSharedPreferences(getActivity());
            final String setMainPage = preferences.getString(getString(org.playtube.plus.R.string.main_page_content_key),
                    getString(org.playtube.plus.R.string.main_page_selectd_kiosk_id));
            if (setMainPage.equals(getString(org.playtube.plus.R.string.blank_page_key))) {
                return new BlankFragment();
            } else if (setMainPage.equals(getString(org.playtube.plus.R.string.kiosk_page_key))) {
                int serviceId = preferences.getInt(getString(org.playtube.plus.R.string.main_page_selected_service),
                        FALLBACK_SERVICE_ID);
                String kioskId = preferences.getString(getString(org.playtube.plus.R.string.main_page_selectd_kiosk_id),
                        FALLBACK_KIOSK_ID);
                KioskFragment fragment = KioskFragment.getInstance(serviceId, kioskId);
                fragment.useAsFrontPage(true);
                return fragment;
            } else if (setMainPage.equals(getString(org.playtube.plus.R.string.feed_page_key))) {
                FeedFragment fragment = new FeedFragment();
                fragment.useAsFrontPage(true);
                return fragment;
            } else if (setMainPage.equals(getString(org.playtube.plus.R.string.channel_page_key))) {
                int serviceId = preferences.getInt(getString(org.playtube.plus.R.string.main_page_selected_service),
                        FALLBACK_SERVICE_ID);
                String url = preferences.getString(getString(org.playtube.plus.R.string.main_page_selected_channel_url),
                        FALLBACK_CHANNEL_URL);
                String name = preferences.getString(getString(org.playtube.plus.R.string.main_page_selected_channel_name),
                        FALLBACK_CHANNEL_NAME);
                ChannelFragment fragment = ChannelFragment.getInstance(serviceId, url, name);
                fragment.useAsFrontPage(true);
                return fragment;
            } else {
                return new BlankFragment();
            }

        } catch (Exception e) {
            ErrorActivity.reportError(activity, e,
                    activity.getClass(),
                    null,
                    ErrorActivity.ErrorInfo.make(UserAction.UI_ERROR,
                            "none", "", org.playtube.plus.R.string.app_ui_crash));
            return new BlankFragment();
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Select Kiosk
    //////////////////////////////////////////////////////////////////////////*/

    private void createKioskMenu(Menu menu, MenuInflater menuInflater)
            throws Exception {
        StreamingService service = NewPipe.getService(currentServiceId);
        KioskList kl = service.getKioskList();
        int i = 0;
        for (final String ks : kl.getAvailableKiosks()) {
            menu.add(0, KIOSK_MENU_OFFSET + i, Menu.NONE,
                    KioskTranslator.getTranslatedKioskName(ks, getContext()))
                    .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            try {
                                NavigationHelper.openKioskFragment(getFragmentManager(), currentServiceId, ks);
                            } catch (Exception e) {
                                ErrorActivity.reportError(activity, e,
                                        activity.getClass(),
                                        null,
                                        ErrorActivity.ErrorInfo.make(UserAction.UI_ERROR,
                                                "none", "", org.playtube.plus.R.string.app_ui_crash));
                            }
                            return true;
                        }
                    });
            i++;
        }
    }
}
