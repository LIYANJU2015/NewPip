package org.schabi.newpipe.views;

import android.support.v4.util.SparseArrayCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.zhy.adapter.recyclerview.base.ViewHolder;


/**
 * Created by liyanju on 2017/5/25.
 */

public class AdViewWrapperAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private RecyclerView.Adapter mInnerAdapter;

    private SparseArrayCompat<AdViewItem> mADViews = new SparseArrayCompat<>();

    public AdViewWrapperAdapter(RecyclerView.Adapter adapter) {
        mInnerAdapter = adapter;
    }

    public void addAdView(int viewType, AdViewItem adViewItem) {
        mADViews.put(viewType, adViewItem);
    }

    public void clearAdView() {
        mADViews.clear();
    }

    @Override
    public void registerAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
        super.registerAdapterDataObserver(observer);
        mInnerAdapter.registerAdapterDataObserver(observer);
    }

    @Override
    public void unregisterAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
        super.unregisterAdapterDataObserver(observer);
        mInnerAdapter.unregisterAdapterDataObserver(observer);
    }

    public boolean isAddAdView() {
        return mADViews.size() > 0;
    }

    public int getAddAdViewCount() {
        return mADViews.size();
    }

    @Override
    public int getItemViewType(int position) {
        int viewType = findAdViewTypeByPostion(position);
        if (viewType != -1) {
            return viewType;
        }

        int count = getAdViewCountBeforeByPostion(position);

        try {
            return mInnerAdapter.getItemViewType(position - count);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getAdViewCountBeforeByPostion(int postion) {
        int count = 0;
        for (int i = 0; i < mADViews.size(); i++) {
            AdViewItem adViewItem = mADViews.valueAt(i);
            if (adViewItem != null && adViewItem.postion < postion) {
                count++;
            }
        }
        return count;
    }

    private int findAdViewTypeByPostion(int postion) {
        for (int i = 0; i < mADViews.size(); i++) {
            AdViewItem adViewItem = mADViews.valueAt(i);
            if (adViewItem != null && adViewItem.postion == postion) {
                return mADViews.keyAt(i);
            }
        }
        return -1;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mADViews.get(viewType) != null) {
            return ViewHolder.createViewHolder(parent.getContext(), mADViews.get(viewType).adView);

        }
        return mInnerAdapter.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (findAdViewTypeByPostion(position) == -1) {
            int count = getAdViewCountBeforeByPostion(position);
            try {
                mInnerAdapter.onBindViewHolder(holder, position - count);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int getItemCount() {
        return mInnerAdapter.getItemCount() + mADViews.size();
    }

    @Override
    public long getItemId(int position) {
        return mInnerAdapter.getItemId(position);
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        mInnerAdapter.onViewRecycled(holder);
        super.onViewRecycled(holder);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mInnerAdapter.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        mInnerAdapter.onViewAttachedToWindow(holder);
    }

    public static class AdViewItem {

        public AdViewItem(View adView, int postion) {
            this.adView = adView;
            this.postion = postion;
        }

        public View adView;

        public int postion;
    }
}
