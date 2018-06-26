package org.playtube.plus.fragments.list.kiosk;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import org.playtube.plus.fragments.list.BaseListInfoFragment;
import org.playtube.plus.report.UserAction;
import org.playtube.plus.util.AnimationUtils;
import org.playtube.plus.util.ExtractorHelper;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.UrlIdHandler;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.kiosk.KioskInfo;
import org.playtube.plus.util.KioskTranslator;

import icepick.State;
import io.reactivex.Single;

import static org.playtube.plus.util.AnimationUtils.animateView;

/**
 * Created by Christian Schabesberger on 23.09.17.
 *
 * Copyright (C) Christian Schabesberger 2017 <chris.schabesberger@mailbox.org>
 * KioskFragment.java is part of NewPipe.
 *
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */

public class KioskFragment extends BaseListInfoFragment<KioskInfo> {

    @State
    protected String kioskId = "";
    protected String kioskTranslatedName;

    /*//////////////////////////////////////////////////////////////////////////
    // Views
    //////////////////////////////////////////////////////////////////////////*/

    public static KioskFragment getInstance(int serviceId)
            throws ExtractionException {
        return getInstance(serviceId, NewPipe.getService(serviceId)
                .getKioskList()
                .getDefaultKioskId());
    }

    public static KioskFragment getInstance(int serviceId, String kioskId)
            throws ExtractionException {
        KioskFragment instance = new KioskFragment();
        StreamingService service = NewPipe.getService(serviceId);
        UrlIdHandler kioskTypeUrlIdHandler = service.getKioskList()
                .getUrlIdHandlerByType(kioskId);
        instance.setInitialData(serviceId,
                kioskTypeUrlIdHandler.getUrl(kioskId),
                kioskId);
        instance.kioskId = kioskId;
        return instance;
    }

    /*//////////////////////////////////////////////////////////////////////////
    // LifeCycle
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        kioskTranslatedName = KioskTranslator.getTranslatedKioskName(kioskId, activity);
        name = kioskTranslatedName;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
//        if(useAsFrontPage && isVisibleToUser && activity != null) {
//            try {
////                setTitle(kioskTranslatedName);
//            } catch (Exception e) {
//                onUnrecoverableError(e, UserAction.UI_ERROR,
//                        "none",
//                        "none", org.playtube.plus.R.string.app_ui_crash);
//            }
//        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(org.playtube.plus.R.layout.fragment_kiosk, container, false);
        return view;
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Menu
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        ActionBar supportActionBar = activity.getSupportActionBar();
        if (supportActionBar != null && useAsFrontPage) {
            supportActionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Load and handle
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public Single<KioskInfo> loadResult(boolean forceReload) {
        String contentCountry = PreferenceManager
                .getDefaultSharedPreferences(activity)
                .getString(getString(org.playtube.plus.R.string.content_country_key),
                        getString(org.playtube.plus.R.string.default_country_value));
        return ExtractorHelper.getKioskInfo(serviceId, url, contentCountry, forceReload);
    }

    @Override
    public Single<ListExtractor.InfoItemsPage> loadMoreItemsLogic() {
        String contentCountry = PreferenceManager
                .getDefaultSharedPreferences(activity)
                .getString(getString(org.playtube.plus.R.string.content_country_key),
                        getString(org.playtube.plus.R.string.default_country_value));
        return ExtractorHelper.getMoreKioskItems(serviceId, url, currentNextPageUrl, contentCountry);
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
    public void handleResult(@NonNull final KioskInfo result) {
        super.handleResult(result);

        name = kioskTranslatedName;
//        setTitle(kioskTranslatedName);

        if (!result.getErrors().isEmpty()) {
            showSnackBarError(result.getErrors(),
                    UserAction.REQUESTED_KIOSK,
                    NewPipe.getNameOfService(result.getServiceId()), result.getUrl(), 0);
        }
    }

    @Override
    public void handleNextItems(ListExtractor.InfoItemsPage result) {
        super.handleNextItems(result);

        if (!result.getErrors().isEmpty()) {
            showSnackBarError(result.getErrors(),
                    UserAction.REQUESTED_PLAYLIST, NewPipe.getNameOfService(serviceId)
                    , "Get next page of: " + url, 0);
        }
    }
}
