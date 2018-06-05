package org.playtube.plus.database.stream.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import org.playtube.plus.database.stream.model.StreamStateEntity;
import org.playtube.plus.database.BasicDAO;

import java.util.List;

import io.reactivex.Flowable;

@Dao
public abstract class StreamStateDAO implements BasicDAO<StreamStateEntity> {
    @Override
    @Query("SELECT * FROM " + StreamStateEntity.STREAM_STATE_TABLE)
    public abstract Flowable<List<StreamStateEntity>> getAll();

    @Override
    @Query("DELETE FROM " + StreamStateEntity.STREAM_STATE_TABLE)
    public abstract int deleteAll();

    @Override
    public Flowable<List<StreamStateEntity>> listByService(int serviceId) {
        throw new UnsupportedOperationException();
    }

    @Query("SELECT * FROM " + StreamStateEntity.STREAM_STATE_TABLE + " WHERE " + StreamStateEntity.JOIN_STREAM_ID + " = :streamId")
    public abstract Flowable<List<StreamStateEntity>> getState(final long streamId);

    @Query("DELETE FROM " + StreamStateEntity.STREAM_STATE_TABLE + " WHERE " + StreamStateEntity.JOIN_STREAM_ID + " = :streamId")
    public abstract int deleteState(final long streamId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract void silentInsertInternal(final StreamStateEntity streamState);

    @Transaction
    public long upsert(StreamStateEntity stream) {
        silentInsertInternal(stream);
        return update(stream);
    }
}
