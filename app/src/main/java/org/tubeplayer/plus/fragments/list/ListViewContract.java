package org.tubeplayer.plus.fragments.list;

import org.tubeplayer.plus.fragments.ViewContract;

public interface ListViewContract<I, N> extends ViewContract<I> {
    void showListFooter(boolean show);

    void handleNextItems(N result);
}
