package org.tubeplayer.plus.database.history.dao;

import org.tubeplayer.plus.database.BasicDAO;

public interface HistoryDAO<T> extends BasicDAO<T> {
    T getLatestEntry();
}
