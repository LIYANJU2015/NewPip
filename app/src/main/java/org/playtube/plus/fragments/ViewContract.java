package org.playtube.plus.fragments;

public interface ViewContract<I> {
    void showLoading();
    void hideLoading();
    void showEmptyState();
    void showError(String message, boolean showRetryButton);

    void handleResult(I result);
}
