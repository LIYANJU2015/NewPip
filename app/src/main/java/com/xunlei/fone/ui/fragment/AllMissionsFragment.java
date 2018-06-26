package com.xunlei.fone.ui.fragment;

import com.xunlei.fone.get.DownloadManager;
import com.xunlei.fone.service.DownloadManagerService;

public class AllMissionsFragment extends MissionsFragment {

    @Override
    protected DownloadManager setupDownloadManager(DownloadManagerService.DMBinder binder) {
        return binder.getDownloadManager();
    }
}
