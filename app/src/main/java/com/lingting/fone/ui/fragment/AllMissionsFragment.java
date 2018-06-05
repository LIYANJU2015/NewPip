package com.lingting.fone.ui.fragment;

import com.lingting.fone.get.DownloadManager;
import com.lingting.fone.service.DownloadManagerService;

public class AllMissionsFragment extends MissionsFragment {

    @Override
    protected DownloadManager setupDownloadManager(DownloadManagerService.DMBinder binder) {
        return binder.getDownloadManager();
    }
}
