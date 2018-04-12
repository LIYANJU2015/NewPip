package org.schabi.newpipe.fragments.list;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.ads.AdChoicesView;
import com.facebook.ads.NativeAd;

import org.schabi.newpipe.R;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.ListInfo;
import org.schabi.newpipe.extractor.kiosk.KioskInfo;
import org.schabi.newpipe.util.Constants;
import org.schabi.newpipe.util.FBAdUtils;
import org.schabi.newpipe.views.AdViewWrapperAdapter;

import java.util.Queue;

import icepick.State;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public abstract class BaseListInfoFragment<I extends ListInfo>
        extends BaseListFragment<I, ListExtractor.InfoItemsPage> {

    @State
    protected int serviceId = Constants.NO_SERVICE_ID;
    @State
    protected String name;
    @State
    protected String url;

    protected I currentInfo;
    protected String currentNextPageUrl;
    protected Disposable currentWorker;

    @Override
    protected void initViews(View rootView, Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);
        setTitle(name);
        showListFooter(hasMoreItems());
    }

    @Override
    public void onPause() {
        super.onPause();
        if (currentWorker != null) currentWorker.dispose();
    }

    private AdViewWrapperAdapter adViewWrapperAdapter;

    @Override
    public RecyclerView.Adapter onGetAdapter() {
        adViewWrapperAdapter = new AdViewWrapperAdapter(infoListAdapter);
        infoListAdapter.setParentAdapter(adViewWrapperAdapter);
        return adViewWrapperAdapter;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check if it was loading when the fragment was stopped/paused,
        if (wasLoading.getAndSet(false)) {
            if (hasMoreItems() && infoListAdapter.getItemsList().size() > 0) {
                loadMoreItems();
            } else {
                doInitialLoadLogic();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (currentWorker != null) currentWorker.dispose();
        currentWorker = null;
    }

    /*//////////////////////////////////////////////////////////////////////////
    // State Saving
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void writeTo(Queue<Object> objectsToSave) {
        super.writeTo(objectsToSave);
        objectsToSave.add(currentInfo);
        objectsToSave.add(currentNextPageUrl);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void readFrom(@NonNull Queue<Object> savedObjects) throws Exception {
        super.readFrom(savedObjects);
        currentInfo = (I) savedObjects.poll();
        currentNextPageUrl = (String) savedObjects.poll();
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Load and handle
    //////////////////////////////////////////////////////////////////////////*/

    protected void doInitialLoadLogic() {
        if (DEBUG) Log.d(TAG, "doInitialLoadLogic() called");
        if (currentInfo == null) {
            startLoading(false);
        } else handleResult(currentInfo);
    }

    /**
     * Implement the logic to load the info from the network.<br/>
     * You can use the default implementations from {@link org.schabi.newpipe.util.ExtractorHelper}.
     *
     * @param forceLoad allow or disallow the result to come from the cache
     */
    protected abstract Single<I> loadResult(boolean forceLoad);

    @Override
    public void startLoading(boolean forceLoad) {
        super.startLoading(forceLoad);

        showListFooter(false);
        currentInfo = null;
        if (currentWorker != null) currentWorker.dispose();
        currentWorker = loadResult(forceLoad)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((@NonNull I result) -> {
                    isLoading.set(false);
                    currentInfo = result;
                    currentNextPageUrl = result.getNextPageUrl();
                    handleResult(result);
                }, (@NonNull Throwable throwable) -> onError(throwable));
    }

    /**
     * Implement the logic to load more items<br/>
     * You can use the default implementations from {@link org.schabi.newpipe.util.ExtractorHelper}
     */
    protected abstract Single<ListExtractor.InfoItemsPage> loadMoreItemsLogic();

    protected void loadMoreItems() {
        isLoading.set(true);

        if (currentWorker != null) currentWorker.dispose();
        currentWorker = loadMoreItemsLogic()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((@io.reactivex.annotations.NonNull ListExtractor.InfoItemsPage InfoItemsPage) -> {
                    isLoading.set(false);
                    handleNextItems(InfoItemsPage);
                }, (@io.reactivex.annotations.NonNull Throwable throwable) -> {
                    isLoading.set(false);
                    onError(throwable);
                });
    }

    @Override
    public void handleNextItems(ListExtractor.InfoItemsPage result) {
        super.handleNextItems(result);
        currentNextPageUrl = result.getNextPageUrl();

        NativeAd nativeAd = FBAdUtils.nextNativieAd();
        if (nativeAd == null || !nativeAd.isAdLoaded()) {
            nativeAd = FBAdUtils.getNativeAd();
        }
        if (nativeAd != null && nativeAd.isAdLoaded() && result.getItems().size() > 3) {
            int offsetStart = adViewWrapperAdapter.getItemCount();
            adViewWrapperAdapter.addAdView(offsetStart + 2, new AdViewWrapperAdapter.
                    AdViewItem(FBAdUtils.setUpItemNativeAdView(activity, nativeAd, onSmallItem()), offsetStart + 2));
            infoListAdapter.addInfoItemList2(result.getItems());
        } else {
            infoListAdapter.addInfoItemList(result.getItems());
        }

        showListFooter(hasMoreItems());
    }

    @Override
    protected boolean hasMoreItems() {
        return !TextUtils.isEmpty(currentNextPageUrl);
    }

    public boolean onSmallItem() {
        return false;
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Contract
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void handleResult(@NonNull I result) {
        super.handleResult(result);
        if (result instanceof KioskInfo && serviceId == 1) {
            if (!result.getUrl().startsWith("http")) {
                url = result.getName();
            } else {
                url = result.getUrl();
            }
        } else {
            url = result.getUrl();
        }
        name = result.getName();
        setTitle(name);

        if (infoListAdapter.getItemsList() != null && infoListAdapter.getItemsList().size() == 0) {
            if (result.getRelatedItems().size() > 0) {
                NativeAd nativeAd = FBAdUtils.nextNativieAd();
                if (nativeAd == null || !nativeAd.isAdLoaded()) {
                    nativeAd = FBAdUtils.getNativeAd();
                }
                if (nativeAd != null && nativeAd.isAdLoaded() && result.getRelatedItems().size() > 3) {
                    int offsetStart = adViewWrapperAdapter.getItemCount();
                    adViewWrapperAdapter.addAdView(offsetStart + 2, new AdViewWrapperAdapter.
                            AdViewItem(FBAdUtils.setUpItemNativeAdView(activity, nativeAd, onSmallItem()), offsetStart + 2));
                    infoListAdapter.addInfoItemList2(result.getRelatedItems());
                } else {
                    infoListAdapter.addInfoItemList(result.getRelatedItems());
                }
                showListFooter(hasMoreItems());
            } else {
                infoListAdapter.clearStreamItemList();
                showEmptyState();
            }
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Utils
    //////////////////////////////////////////////////////////////////////////*/

    protected void setInitialData(int serviceId, String url, String name) {
        this.serviceId = serviceId;
        this.url = url;
        this.name = !TextUtils.isEmpty(name) ? name : "";
    }
}
