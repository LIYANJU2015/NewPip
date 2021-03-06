package org.tubeplayer.plus.fragments.local.bookmark;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.tubeplayer.plus.NewPipeDatabase;
import org.tubeplayer.plus.R;
import org.tubeplayer.plus.database.playlist.PlaylistLocalItem;
import org.tubeplayer.plus.database.playlist.PlaylistMetadataEntry;
import org.tubeplayer.plus.database.playlist.model.PlaylistRemoteEntity;
import org.tubeplayer.plus.fragments.local.RemotePlaylistManager;
import org.tubeplayer.plus.report.UserAction;
import org.tubeplayer.plus.util.NavigationHelper;
import org.tubeplayer.plus.util.OnClickGesture;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.tubeplayer.plus.database.AppDatabase;
import org.tubeplayer.plus.database.LocalItem;
import org.tubeplayer.plus.fragments.local.LocalPlaylistManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import icepick.State;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public final class BookmarkFragment
        extends BaseLocalListFragment<List<PlaylistLocalItem>, Void> {

    private View lastPlayedButton;
    private View mostPlayedButton;

    @State
    protected Parcelable itemsListState;

    private Subscription databaseSubscription;
    private CompositeDisposable disposables = new CompositeDisposable();
    private LocalPlaylistManager localPlaylistManager;
    private RemotePlaylistManager remotePlaylistManager;

    ///////////////////////////////////////////////////////////////////////////
    // Fragment LifeCycle - Creation
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final AppDatabase database = NewPipeDatabase.getInstance(getContext());
        localPlaylistManager = new LocalPlaylistManager(database);
        remotePlaylistManager = new RemotePlaylistManager(database);
        disposables = new CompositeDisposable();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        if (activity != null && activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setDisplayShowTitleEnabled(true);
            //activity.setTitle(R.string.tab_subscriptions);
        }

        return inflater.inflate(R.layout.fragment_bookmarks, container, false);
    }


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
//        if (activity != null && isVisibleToUser) {
//            setTitle(activity.getString(R.string.tab_bookmarks));
//        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment LifeCycle - Views
    ///////////////////////////////////////////////////////////////////////////

    @Override
    protected void initViews(View rootView, Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);
    }

    @Override
    protected View getListHeader() {
        final View headerRootLayout = activity.getLayoutInflater()
                .inflate(org.tubeplayer.plus.R.layout.bookmark_header, itemsList, false);
        lastPlayedButton = headerRootLayout.findViewById(org.tubeplayer.plus.R.id.lastPlayed);
        mostPlayedButton = headerRootLayout.findViewById(org.tubeplayer.plus.R.id.mostPlayed);
        return headerRootLayout;
    }

    @Override
    protected void initListeners() {
        super.initListeners();

        itemListAdapter.setSelectedListener(new OnClickGesture<LocalItem>() {
            @Override
            public void selected(LocalItem selectedItem) {
                // Requires the parent fragment to find holder for fragment replacement
                if (getParentFragment() == null) return;
                final FragmentManager fragmentManager = getParentFragment().getFragmentManager();

                if (selectedItem instanceof PlaylistMetadataEntry) {
                    final PlaylistMetadataEntry entry = ((PlaylistMetadataEntry) selectedItem);
                    NavigationHelper.openLocalPlaylistFragment(fragmentManager, entry.uid,
                            entry.name);

                } else if (selectedItem instanceof PlaylistRemoteEntity) {
                    final PlaylistRemoteEntity entry = ((PlaylistRemoteEntity) selectedItem);
                    NavigationHelper.openPlaylistFragment(fragmentManager, entry.getServiceId(),
                            entry.getUrl(), entry.getName());
                }
            }

            @Override
            public void held(LocalItem selectedItem) {
                if (selectedItem instanceof PlaylistMetadataEntry) {
                    showLocalDeleteDialog((PlaylistMetadataEntry) selectedItem);

                } else if (selectedItem instanceof PlaylistRemoteEntity) {
                    showRemoteDeleteDialog((PlaylistRemoteEntity) selectedItem);
                }
            }
        });

        lastPlayedButton.setOnClickListener(view -> {
            if (getParentFragment() != null) {
                NavigationHelper.openLastPlayedFragment(getParentFragment().getFragmentManager());
            }
        });

        mostPlayedButton.setOnClickListener(view -> {
            if (getParentFragment() != null) {
                NavigationHelper.openMostPlayedFragment(getParentFragment().getFragmentManager());
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment LifeCycle - Loading
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void startLoading(boolean forceLoad) {
        super.startLoading(forceLoad);

        Flowable.combineLatest(
                localPlaylistManager.getPlaylists(),
                remotePlaylistManager.getPlaylists(),
                BookmarkFragment::merge
        ).onBackpressureLatest()
         .observeOn(AndroidSchedulers.mainThread())
         .subscribe(getPlaylistsSubscriber());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment LifeCycle - Destruction
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onPause() {
        super.onPause();
        itemsListState = itemsList.getLayoutManager().onSaveInstanceState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mostPlayedButton != null) mostPlayedButton.setOnClickListener(null);
        if (lastPlayedButton != null) lastPlayedButton.setOnClickListener(null);

        if (disposables != null) disposables.clear();
        if (databaseSubscription != null) databaseSubscription.cancel();

        databaseSubscription = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (disposables != null) disposables.dispose();

        disposables = null;
        localPlaylistManager = null;
        remotePlaylistManager = null;
        itemsListState = null;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Subscriptions Loader
    ///////////////////////////////////////////////////////////////////////////

    private Subscriber<List<PlaylistLocalItem>> getPlaylistsSubscriber() {
        return new Subscriber<List<PlaylistLocalItem>>() {
            @Override
            public void onSubscribe(Subscription s) {
                showLoading();
                if (databaseSubscription != null) databaseSubscription.cancel();
                databaseSubscription = s;
                databaseSubscription.request(1);
            }

            @Override
            public void onNext(List<PlaylistLocalItem> subscriptions) {
                handleResult(subscriptions);
                if (databaseSubscription != null) databaseSubscription.request(1);
            }

            @Override
            public void onError(Throwable exception) {
                BookmarkFragment.this.onError(exception);
            }

            @Override
            public void onComplete() {
            }
        };
    }

    @Override
    public void handleResult(@NonNull List<PlaylistLocalItem> result) {
        super.handleResult(result);

        itemListAdapter.clearStreamItemList();

        if (result.isEmpty()) {
            showEmptyState();
            return;
        }

        itemListAdapter.addItems(result);
        if (itemsListState != null) {
            itemsList.getLayoutManager().onRestoreInstanceState(itemsListState);
            itemsListState = null;
        }
        hideLoading();
    }
    ///////////////////////////////////////////////////////////////////////////
    // Fragment Error Handling
    ///////////////////////////////////////////////////////////////////////////

    @Override
    protected boolean onError(Throwable exception) {
        if (super.onError(exception)) return true;

        onUnrecoverableError(exception, UserAction.SOMETHING_ELSE,
                "none", "Bookmark", org.tubeplayer.plus.R.string.general_error);
        return true;
    }

    @Override
    protected void resetFragment() {
        super.resetFragment();
        if (disposables != null) disposables.clear();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Utils
    ///////////////////////////////////////////////////////////////////////////

    private void showLocalDeleteDialog(final PlaylistMetadataEntry item) {
        showDeleteDialog(item.name, localPlaylistManager.deletePlaylist(item.uid));
    }

    private void showRemoteDeleteDialog(final PlaylistRemoteEntity item) {
        showDeleteDialog(item.getName(), remotePlaylistManager.deletePlaylist(item.getUid()));
    }

    private void showDeleteDialog(final String name, final Single<Integer> deleteReactor) {
        if (activity == null || disposables == null) return;

        new AlertDialog.Builder(activity)
                .setTitle(name)
                .setMessage(org.tubeplayer.plus.R.string.delete_playlist_prompt)
                .setCancelable(true)
                .setPositiveButton(org.tubeplayer.plus.R.string.delete, (dialog, i) ->
                        disposables.add(deleteReactor
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(ignored -> {/*Do nothing on success*/}, this::onError))
                )
                .setNegativeButton(org.tubeplayer.plus.R.string.cancel, null)
                .show();
    }

    private static List<PlaylistLocalItem> merge(final List<PlaylistMetadataEntry> localPlaylists,
                                                 final List<PlaylistRemoteEntity> remotePlaylists) {
        List<PlaylistLocalItem> items = new ArrayList<>(
                localPlaylists.size() + remotePlaylists.size());
        items.addAll(localPlaylists);
        items.addAll(remotePlaylists);

        Collections.sort(items, (left, right) ->
                left.getOrderingName().compareToIgnoreCase(right.getOrderingName()));

        return items;
    }
}

