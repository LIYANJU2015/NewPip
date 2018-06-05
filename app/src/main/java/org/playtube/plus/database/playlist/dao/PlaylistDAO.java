package org.playtube.plus.database.playlist.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;

import org.playtube.plus.database.BasicDAO;
import org.playtube.plus.database.playlist.model.PlaylistEntity;

import java.util.List;

import io.reactivex.Flowable;

@Dao
public abstract class PlaylistDAO implements BasicDAO<PlaylistEntity> {
    @Override
    @Query("SELECT * FROM " + PlaylistEntity.PLAYLIST_TABLE)
    public abstract Flowable<List<PlaylistEntity>> getAll();

    @Override
    @Query("DELETE FROM " + PlaylistEntity.PLAYLIST_TABLE)
    public abstract int deleteAll();

    @Override
    public Flowable<List<PlaylistEntity>> listByService(int serviceId) {
        throw new UnsupportedOperationException();
    }

    @Query("SELECT * FROM " + PlaylistEntity.PLAYLIST_TABLE + " WHERE " + PlaylistEntity.PLAYLIST_ID + " = :playlistId")
    public abstract Flowable<List<PlaylistEntity>> getPlaylist(final long playlistId);

    @Query("DELETE FROM " + PlaylistEntity.PLAYLIST_TABLE + " WHERE " + PlaylistEntity.PLAYLIST_ID + " = :playlistId")
    public abstract int deletePlaylist(final long playlistId);
}
