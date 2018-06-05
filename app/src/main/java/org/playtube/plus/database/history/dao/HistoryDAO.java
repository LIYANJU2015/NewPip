package org.playtube.plus.database.history.dao;

import org.playtube.plus.database.BasicDAO;

public interface HistoryDAO<T> extends BasicDAO<T> {
    T getLatestEntry();
}
