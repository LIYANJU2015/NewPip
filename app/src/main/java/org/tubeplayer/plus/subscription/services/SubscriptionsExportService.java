/*
 * Copyright 2018 Mauricio Colli <mauriciocolli@outlook.com>
 * SubscriptionsExportService.java is part of NewPipe
 *
 * License: GPL-3.0+
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.tubeplayer.plus.subscription.services;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import org.tubeplayer.plus.MainActivity;
import org.tubeplayer.plus.database.subscription.SubscriptionEntity;
import org.tubeplayer.plus.subscription.ImportExportJsonHelper;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.schabi.newpipe.extractor.subscription.SubscriptionItem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class SubscriptionsExportService extends BaseImportExportService {
    public static final String KEY_FILE_PATH = "key_file_path";

    /**
     * A {@link LocalBroadcastManager local broadcast} will be made with this action when the export is successfully completed.
     */
    public static final String EXPORT_COMPLETE_ACTION = "SubscriptionsExportService.EXPORT_COMPLETE";

    private Subscription subscription;
    private File outFile;
    private FileOutputStream outputStream;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || subscription != null) return START_NOT_STICKY;

        final String path = intent.getStringExtra(KEY_FILE_PATH);
        if (TextUtils.isEmpty(path)) {
            stopAndReportError(new IllegalStateException("Exporting to a file, but the path is empty or null"), "Exporting subscriptions");
            return START_NOT_STICKY;
        }

        try {
            outputStream = new FileOutputStream(outFile = new File(path));
        } catch (FileNotFoundException e) {
            handleError(e);
            return START_NOT_STICKY;
        }

        startExport();

        return START_NOT_STICKY;
    }

    @Override
    protected int getNotificationId() {
        return 4567;
    }

    @Override
    public int getTitle() {
        return org.tubeplayer.plus.R.string.export_ongoing;
    }

    @Override
    protected void disposeAll() {
        super.disposeAll();
        if (subscription != null) subscription.cancel();
    }

    private void startExport() {
        showToast(org.tubeplayer.plus.R.string.export_ongoing);

        subscriptionService.subscriptionTable()
                .getAll()
                .take(1)
                .map(subscriptionEntities -> {
                    final List<SubscriptionItem> result = new ArrayList<>(subscriptionEntities.size());
                    for (SubscriptionEntity entity : subscriptionEntities) {
                        result.add(new SubscriptionItem(entity.getServiceId(), entity.getUrl(), entity.getName()));
                    }
                    return result;
                })
                .map(exportToFile())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getSubscriber());
    }

    private Subscriber<File> getSubscriber() {
        return new Subscriber<File>() {
            @Override
            public void onSubscribe(Subscription s) {
                subscription = s;
                s.request(1);
            }

            @Override
            public void onNext(File file) {
                if (MainActivity.DEBUG) Log.d(TAG, "startExport() success: file = " + file);
            }

            @Override
            public void onError(Throwable error) {
                Log.e(TAG, "onError() called with: error = [" + error + "]", error);
                handleError(error);
            }

            @Override
            public void onComplete() {
                LocalBroadcastManager.getInstance(SubscriptionsExportService.this).sendBroadcast(new Intent(EXPORT_COMPLETE_ACTION));
                showToast(org.tubeplayer.plus.R.string.export_complete_toast);
                stopService();
            }
        };
    }

    private Function<List<SubscriptionItem>, File> exportToFile() {
        return subscriptionItems -> {
            ImportExportJsonHelper.writeTo(subscriptionItems, outputStream, eventListener);
            return outFile;
        };
    }

    protected void handleError(Throwable error) {
        super.handleError(org.tubeplayer.plus.R.string.subscriptions_export_unsuccessful, error);
    }
}
