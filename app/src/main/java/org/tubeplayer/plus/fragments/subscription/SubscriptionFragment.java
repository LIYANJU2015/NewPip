package org.tubeplayer.plus.fragments.subscription;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nononsenseapps.filepicker.Utils;

import org.tubeplayer.plus.database.subscription.SubscriptionEntity;
import org.tubeplayer.plus.fragments.BaseStateFragment;
import org.tubeplayer.plus.info_list.InfoListAdapter;
import org.tubeplayer.plus.report.UserAction;
import org.tubeplayer.plus.subscription.SubscriptionService;
import org.tubeplayer.plus.subscription.services.SubscriptionsImportService;
import org.tubeplayer.plus.util.AnimationUtils;
import org.tubeplayer.plus.util.FilePickerActivityHelper;
import org.tubeplayer.plus.util.MyLinearLayoutManager;
import org.tubeplayer.plus.util.NavigationHelper;
import org.tubeplayer.plus.util.OnClickGesture;
import org.tubeplayer.plus.views.CollapsibleView;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.subscription.SubscriptionExtractor;
import org.tubeplayer.plus.subscription.services.SubscriptionsExportService;
import org.tubeplayer.plus.util.ServiceHelper;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import icepick.State;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static org.tubeplayer.plus.util.AnimationUtils.animateView;

public class SubscriptionFragment extends BaseStateFragment<List<SubscriptionEntity>> {
    private static final int REQUEST_EXPORT_CODE = 666;
    private static final int REQUEST_IMPORT_CODE = 667;

    private RecyclerView itemsList;
    @State
    protected Parcelable itemsListState;
    private InfoListAdapter infoListAdapter;

    private View headerRootLayout;
    private View whatsNewItemListHeader;
    private View importExportListHeader;

    @State
    protected Parcelable importExportOptionsState;
    private CollapsibleView importExportOptions;

    private CompositeDisposable disposables = new CompositeDisposable();
    private SubscriptionService subscriptionService;

    ///////////////////////////////////////////////////////////////////////////
    // Fragment LifeCycle
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
//        if (activity != null && isVisibleToUser) {
//            setTitle(activity.getString(org.playtube.plus.R.string.tab_subscriptions));
//        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        infoListAdapter = new InfoListAdapter(activity);
        subscriptionService = SubscriptionService.getInstance(activity);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(org.tubeplayer.plus.R.layout.fragment_subscription, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        setupBroadcastReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();
        itemsListState = itemsList.getLayoutManager().onSaveInstanceState();
        importExportOptionsState = importExportOptions.onSaveInstanceState();

        if (subscriptionBroadcastReceiver != null && activity != null) {
            LocalBroadcastManager.getInstance(activity).unregisterReceiver(subscriptionBroadcastReceiver);
        }
    }

    @Override
    public void onDestroyView() {
        if (disposables != null) disposables.clear();

        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (disposables != null) disposables.dispose();
        disposables = null;
        subscriptionService = null;

        super.onDestroy();
    }

    /*/////////////////////////////////////////////////////////////////////////
    // Menu
    /////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        ActionBar supportActionBar = activity.getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayShowTitleEnabled(true);
//            setTitle(getString(org.playtube.plus.R.string.tab_subscriptions));
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Subscriptions import/export
    //////////////////////////////////////////////////////////////////////////*/

    private BroadcastReceiver subscriptionBroadcastReceiver;

    private void setupBroadcastReceiver() {
        if (activity == null) return;

        if (subscriptionBroadcastReceiver != null) {
            LocalBroadcastManager.getInstance(activity).unregisterReceiver(subscriptionBroadcastReceiver);
        }

        final IntentFilter filters = new IntentFilter();
        filters.addAction(SubscriptionsExportService.EXPORT_COMPLETE_ACTION);
        filters.addAction(SubscriptionsImportService.IMPORT_COMPLETE_ACTION);
        subscriptionBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (importExportOptions != null) importExportOptions.collapse();
            }
        };

        LocalBroadcastManager.getInstance(activity).registerReceiver(subscriptionBroadcastReceiver, filters);
    }

    private View addItemView(final String title, @DrawableRes final int icon, ViewGroup container) {
        final View itemRoot = View.inflate(getContext(), org.tubeplayer.plus.R.layout.subscription_import_export_item, null);
        final TextView titleView = itemRoot.findViewById(android.R.id.text1);
        final ImageView iconView = itemRoot.findViewById(android.R.id.icon1);

        titleView.setText(title);
        iconView.setImageResource(icon);

        container.addView(itemRoot);
        return itemRoot;
    }

    private void setupImportFromItems(final ViewGroup listHolder) {
        final View previousBackupItem = addItemView(getString(org.tubeplayer.plus.R.string.previous_export), org.tubeplayer.plus.R.drawable.ic_backup_black_24dp, listHolder);
        previousBackupItem.setOnClickListener(item -> onImportPreviousSelected());

        final String[] services = getResources().getStringArray(org.tubeplayer.plus.R.array.service_list);
        for (String serviceName : services) {
            try {
                final StreamingService service = NewPipe.getService(serviceName);

                final SubscriptionExtractor subscriptionExtractor = service.getSubscriptionExtractor();
                if (subscriptionExtractor == null) continue;

                final List<SubscriptionExtractor.ContentSource> supportedSources = subscriptionExtractor.getSupportedSources();
                if (supportedSources.isEmpty()) continue;

                final View itemView = addItemView(serviceName, ServiceHelper.getIcon(service.getServiceId()), listHolder);
                final ImageView iconView = itemView.findViewById(android.R.id.icon1);
//                iconView.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);

                itemView.setOnClickListener(selectedItem -> onImportFromServiceSelected(service.getServiceId()));
            } catch (ExtractionException e) {
                throw new RuntimeException("Services array contains an entry that it's not a valid service name (" + serviceName + ")", e);
            }
        }
    }

    private void setupExportToItems(final ViewGroup listHolder) {
        final View previousBackupItem = addItemView(getString(org.tubeplayer.plus.R.string.file), org.tubeplayer.plus.R.drawable.ic_save_black_24dp, listHolder);
        previousBackupItem.setOnClickListener(item -> onExportSelected());
    }

    private void onImportFromServiceSelected(int serviceId) {
        if (getParentFragment() == null) return;
        NavigationHelper.openSubscriptionsImportFragment(getParentFragment().getFragmentManager(), serviceId);
    }

    private void onImportPreviousSelected() {
        startActivityForResult(FilePickerActivityHelper.chooseSingleFile(activity), REQUEST_IMPORT_CODE);
    }

    private void onExportSelected() {
        final String date = new SimpleDateFormat("yyyyMMddHHmm", Locale.ENGLISH).format(new Date());
        final String exportName = "newpipe_subscriptions_" + date + ".json";
        final File exportFile = new File(Environment.getExternalStorageDirectory(), exportName);

        startActivityForResult(FilePickerActivityHelper.chooseFileToSave(activity, exportFile.getAbsolutePath()), REQUEST_EXPORT_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && data.getData() != null && resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_EXPORT_CODE) {
                final File exportFile = Utils.getFileForUri(data.getData());
                if (!exportFile.getParentFile().canWrite() || !exportFile.getParentFile().canRead()) {
                    Toast.makeText(activity, org.tubeplayer.plus.R.string.invalid_directory, Toast.LENGTH_SHORT).show();
                } else {
                    activity.startService(new Intent(activity, SubscriptionsExportService.class)
                            .putExtra(SubscriptionsExportService.KEY_FILE_PATH, exportFile.getAbsolutePath()));
                }
            } else if (requestCode == REQUEST_IMPORT_CODE) {
                final String path = Utils.getFileForUri(data.getData()).getAbsolutePath();
                ImportConfirmationDialog.show(this, new Intent(activity, SubscriptionsImportService.class)
                        .putExtra(SubscriptionsImportService.KEY_MODE, SubscriptionsImportService.PREVIOUS_EXPORT_MODE)
                        .putExtra(SubscriptionsImportService.KEY_VALUE, path));
            }
        }
    }
    /*/////////////////////////////////////////////////////////////////////////
    // Fragment Views
    /////////////////////////////////////////////////////////////////////////*/

    @Override
    protected void initViews(View rootView, Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);

        infoListAdapter = new InfoListAdapter(getActivity());
        itemsList = rootView.findViewById(org.tubeplayer.plus.R.id.items_list);
        itemsList.setLayoutManager(new MyLinearLayoutManager(activity));

        infoListAdapter.setHeader(headerRootLayout = activity.getLayoutInflater().inflate(org.tubeplayer.plus.R.layout.subscription_header, itemsList, false));
        whatsNewItemListHeader = headerRootLayout.findViewById(org.tubeplayer.plus.R.id.whats_new);
        importExportListHeader = headerRootLayout.findViewById(org.tubeplayer.plus.R.id.import_export);
        importExportOptions = headerRootLayout.findViewById(org.tubeplayer.plus.R.id.import_export_options);

        infoListAdapter.useMiniItemVariants(true);
        itemsList.setAdapter(infoListAdapter);

        setupImportFromItems(headerRootLayout.findViewById(org.tubeplayer.plus.R.id.import_from_options));
        setupExportToItems(headerRootLayout.findViewById(org.tubeplayer.plus.R.id.export_to_options));

        if (importExportOptionsState != null) {
            importExportOptions.onRestoreInstanceState(importExportOptionsState);
            importExportOptionsState = null;
        }

        importExportOptions.addListener(getExpandIconSyncListener(headerRootLayout.findViewById(org.tubeplayer.plus.R.id.import_export_expand_icon)));
        importExportOptions.ready();
    }

    private CollapsibleView.StateListener getExpandIconSyncListener(final ImageView iconView) {
        return newState -> AnimationUtils.animateRotation(iconView, 250, newState == CollapsibleView.COLLAPSED ? 0 : 180);
    }

    @Override
    protected void initListeners() {
        super.initListeners();

        infoListAdapter.setOnChannelSelectedListener(new OnClickGesture<ChannelInfoItem>() {
            @Override
            public void selected(ChannelInfoItem selectedItem) {
                // Requires the parent fragment to find holder for fragment replacement
                NavigationHelper.openChannelFragment(getParentFragment().getFragmentManager(),
                        selectedItem.getServiceId(), selectedItem.getUrl(), selectedItem.getName());
            }
        });

        //noinspection ConstantConditions
        whatsNewItemListHeader.setOnClickListener(v ->
                NavigationHelper.openWhatsNewFragment(getParentFragment().getFragmentManager()));
        importExportListHeader.setOnClickListener(v -> importExportOptions.switchState());
    }

    private void resetFragment() {
        if (disposables != null) disposables.clear();
        if (infoListAdapter != null) infoListAdapter.clearStreamItemList();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Subscriptions Loader
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void startLoading(boolean forceLoad) {
        super.startLoading(forceLoad);
        resetFragment();

        subscriptionService.getSubscription().toObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getSubscriptionObserver());
    }

    private Observer<List<SubscriptionEntity>> getSubscriptionObserver() {
        return new Observer<List<SubscriptionEntity>>() {
            @Override
            public void onSubscribe(Disposable d) {
                showLoading();
                disposables.add(d);
            }

            @Override
            public void onNext(List<SubscriptionEntity> subscriptions) {
                handleResult(subscriptions);
            }

            @Override
            public void onError(Throwable exception) {
                SubscriptionFragment.this.onError(exception);
            }

            @Override
            public void onComplete() {
            }
        };
    }

    @Override
    public void handleResult(@NonNull List<SubscriptionEntity> result) {
        super.handleResult(result);

        infoListAdapter.clearStreamItemList();

        if (result.isEmpty()) {
            whatsNewItemListHeader.setVisibility(View.GONE);
            showEmptyState();
        } else {
            infoListAdapter.addInfoItemList(getSubscriptionItems(result));
            if (itemsListState != null) {
                itemsList.getLayoutManager().onRestoreInstanceState(itemsListState);
                itemsListState = null;
            }
            whatsNewItemListHeader.setVisibility(View.VISIBLE);
            hideLoading();
        }
    }


    private List<InfoItem> getSubscriptionItems(List<SubscriptionEntity> subscriptions) {
        List<InfoItem> items = new ArrayList<>();
        for (final SubscriptionEntity subscription : subscriptions) items.add(subscription.toChannelInfoItem());

        Collections.sort(items,
                (InfoItem o1, InfoItem o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
        return items;
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Contract
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void showLoading() {
        super.showLoading();
        AnimationUtils.animateView(itemsList, false, 100);
    }

    @Override
    public void hideLoading() {
        super.hideLoading();
        AnimationUtils.animateView(itemsList, true, 200);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment Error Handling
    ///////////////////////////////////////////////////////////////////////////

    @Override
    protected boolean onError(Throwable exception) {
        resetFragment();
        if (super.onError(exception)) return true;

        onUnrecoverableError(exception, UserAction.SOMETHING_ELSE, "none", "Subscriptions", org.tubeplayer.plus.R.string.general_error);
        return true;
    }
}
