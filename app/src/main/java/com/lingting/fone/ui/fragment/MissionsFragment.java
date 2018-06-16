package com.lingting.fone.ui.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.facebook.ads.NativeAd;

import org.playtube.plus.App;
import org.playtube.plus.R;
import org.playtube.plus.util.Constants;
import org.playtube.plus.util.FBAdUtils;

import com.lingting.fone.get.DownloadManager;
import com.lingting.fone.service.DownloadManagerService;
import com.lingting.fone.ui.adapter.MissionAdapter;
import com.rating.RatingActivity;

public abstract class MissionsFragment extends Fragment {
    private DownloadManager mManager;
    private DownloadManagerService.DMBinder mBinder;

    private SharedPreferences mPrefs;
    private boolean mLinear;
    private MenuItem mSwitch;

    private RecyclerView mList;
    private MissionAdapter mAdapter;
    private GridLayoutManager mGridManager;
    private LinearLayoutManager mLinearManager;
    private Context mActivity;

    public static boolean sIsPlay = false;

    @Override
    public void onResume() {
        super.onResume();
        if (sIsPlay) {
            sIsPlay = false;
            RatingActivity.launch(App.sContext, "", App.sContext.getString(R.string.download_rating));
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mBinder = (DownloadManagerService.DMBinder) binder;
            mManager = setupDownloadManager(mBinder);
            updateList();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // What to do?
        }


    };

    private FrameLayout mAdFramelayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.missions, container, false);
        mAdFramelayout = v.findViewById(R.id.download_ad_frame);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mLinear = mPrefs.getBoolean("linear", false);

        // Bind the service
        Intent i = new Intent();
        i.setClass(getActivity(), DownloadManagerService.class);
        getActivity().bindService(i, mConnection, Context.BIND_AUTO_CREATE);

        // Views
        mList = v.findViewById(R.id.mission_recycler);

        // Init
        mGridManager = new GridLayoutManager(getActivity(), 2);
        mLinearManager = new LinearLayoutManager(getActivity());
        mList.setLayoutManager(mGridManager);

        setHasOptionsMenu(true);

        NativeAd nativeAd = FBAdUtils.get().nextNativieAd();
        if (nativeAd == null || !nativeAd.isAdLoaded()) {
            nativeAd = FBAdUtils.get().getNativeAd();
        }
        if (nativeAd != null && nativeAd.isAdLoaded()) {
            mAdFramelayout.removeAllViews();
            mAdFramelayout.addView(FBAdUtils.get().setUpItemNativeAdView(getActivity(), nativeAd));
        }

        return v;
    }

    /**
     * Added in API level 23.
     */
    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);

        // Bug: in api< 23 this is never called
        // so mActivity=null
        // so app crashes with nullpointer exception
        mActivity = activity;
    }

    /**
     * deprecated in API level 23,
     * but must remain to allow compatibility with api<23
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mActivity = activity;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unbindService(mConnection);

        FBAdUtils.get().loadAd(Constants.NATIVE_AD);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);

		/*switch (item.getItemId()) {
            case R.id.switch_mode:
				mLinear = !mLinear;
				updateList();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}*/
    }

    public void notifyChange() {
        mAdapter.notifyDataSetChanged();
    }

    private void updateList() {
        mAdapter = new MissionAdapter(mActivity, mBinder, mManager, mLinear);

        if (mLinear) {
            mList.setLayoutManager(mLinearManager);
        } else {
            mList.setLayoutManager(mGridManager);
        }

        mList.setAdapter(mAdapter);

        if (mSwitch != null) {
            mSwitch.setIcon(mLinear ? R.drawable.grid : R.drawable.list);
        }

        mPrefs.edit().putBoolean("linear", mLinear).commit();
    }

    protected abstract DownloadManager setupDownloadManager(DownloadManagerService.DMBinder binder);
}
