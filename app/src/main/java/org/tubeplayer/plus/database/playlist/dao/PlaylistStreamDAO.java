package org.tubeplayer.plus.database.playlist.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import org.tubeplayer.plus.database.playlist.PlaylistMetadataEntry;
import org.tubeplayer.plus.database.playlist.model.PlaylistStreamEntity;
import org.tubeplayer.plus.database.stream.model.StreamEntity;
import org.tubeplayer.plus.database.BasicDAO;
import org.tubeplayer.plus.database.playlist.PlaylistStreamEntry;

import java.util.List;

import io.reactivex.Flowable;

import static org.tubeplayer.plus.database.playlist.model.PlaylistEntity.*;

@Dao
public abstract class PlaylistStreamDAO implements BasicDAO<PlaylistStreamEntity> {
    @Override
    @Query("SELECT * FROM " + PlaylistStreamEntity.PLAYLIST_STREAM_JOIN_TABLE)
    public abstract Flowable<List<PlaylistStreamEntity>> getAll();

    @Override
    @Query("DELETE FROM " + PlaylistStreamEntity.PLAYLIST_STREAM_JOIN_TABLE)
    public abstract int deleteAll();

    @Override
    public Flowable<List<PlaylistStreamEntity>> listByService(int serviceId) {
        throw new UnsupportedOperationException();
    }

    @Query("DELETE FROM " + PlaylistStreamEntity.PLAYLIST_STREAM_JOIN_TABLE +
            " WHERE " + PlaylistStreamEntity.JOIN_PLAYLIST_ID + " = :playlistId")
    public abstract void deleteBatch(final long playlistId);

    @Query("SELECT COALESCE(MAX(" + PlaylistStreamEntity.JOIN_INDEX + "), -1)" +
            " FROM " + PlaylistStreamEntity.PLAYLIST_STREAM_JOIN_TABLE +
            " WHERE " + PlaylistStreamEntity.JOIN_PLAYLIST_ID + " = :playlistId")
    public abstract Flowable<Integer> getMaximumIndexOf(final long playlistId);

    @Transaction
    @Query("SELECT * FROM " + StreamEntity.STREAM_TABLE + " INNER JOIN " +
            // get ids of streams of the given playlist
            "(SELECT " + PlaylistStreamEntity.JOIN_STREAM_ID + "," + PlaylistStreamEntity.JOIN_INDEX +
            " FROM " + PlaylistStreamEntity.PLAYLIST_STREAM_JOIN_TABLE +
            " WHERE " + PlaylistStreamEntity.JOIN_PLAYLIST_ID + " = :playlistId)" +

            // then merge with the stream metadata
            " ON " + StreamEntity.STREAM_ID + " = " + PlaylistStreamEntity.JOIN_STREAM_ID +
            " ORDER BY " + PlaylistStreamEntity.JOIN_INDEX + " ASC")
    public abstract Flowable<List<PlaylistStreamEntry>> getOrderedStreamsOf(long playlistId);

    @Transaction
    @Query("SELECT " + PLAYLIST_ID + ", " + PLAYLIST_NAME + ", " +
            PLAYLIST_THUMBNAIL_URL + ", " +
            "COALESCE(COUNT(" + PlaylistStreamEntity.JOIN_PLAYLIST_ID + "), 0) AS " + PlaylistMetadataEntry.PLAYLIST_STREAM_COUNT +

            " FROM " + PLAYLIST_TABLE +
            " LEFT JOIN " + PlaylistStreamEntity.PLAYLIST_STREAM_JOIN_TABLE +
            " ON " + PLAYLIST_ID + " = " + PlaylistStreamEntity.JOIN_PLAYLIST_ID +
            " GROUP BY " + PlaylistStreamEntity.JOIN_PLAYLIST_ID +
            " ORDER BY " + PLAYLIST_NAME + " COLLATE NOCASE ASC")
    public abstract Flowable<List<PlaylistMetadataEntry>> getPlaylistMetadata();
}
