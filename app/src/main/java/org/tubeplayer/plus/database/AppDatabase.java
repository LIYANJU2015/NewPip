package org.tubeplayer.plus.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;

import org.tubeplayer.plus.database.history.dao.SearchHistoryDAO;
import org.tubeplayer.plus.database.history.model.SearchHistoryEntry;
import org.tubeplayer.plus.database.history.model.StreamHistoryEntity;
import org.tubeplayer.plus.database.playlist.dao.PlaylistRemoteDAO;
import org.tubeplayer.plus.database.playlist.dao.PlaylistStreamDAO;
import org.tubeplayer.plus.database.playlist.model.PlaylistEntity;
import org.tubeplayer.plus.database.playlist.model.PlaylistRemoteEntity;
import org.tubeplayer.plus.database.playlist.model.PlaylistStreamEntity;
import org.tubeplayer.plus.database.stream.dao.StreamDAO;
import org.tubeplayer.plus.database.stream.dao.StreamStateDAO;
import org.tubeplayer.plus.database.stream.model.StreamEntity;
import org.tubeplayer.plus.database.stream.model.StreamStateEntity;
import org.tubeplayer.plus.database.subscription.SubscriptionDAO;
import org.tubeplayer.plus.database.subscription.SubscriptionEntity;
import org.tubeplayer.plus.database.history.dao.StreamHistoryDAO;
import org.tubeplayer.plus.database.playlist.dao.PlaylistDAO;

@TypeConverters({Converters.class})
@Database(
        entities = {
                SubscriptionEntity.class, SearchHistoryEntry.class,
                StreamEntity.class, StreamHistoryEntity.class, StreamStateEntity.class,
                PlaylistEntity.class, PlaylistStreamEntity.class, PlaylistRemoteEntity.class
        },
        version = Migrations.DB_VER_12_0,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "playtube.db";

    public abstract SubscriptionDAO subscriptionDAO();

    public abstract SearchHistoryDAO searchHistoryDAO();

    public abstract StreamDAO streamDAO();

    public abstract StreamHistoryDAO streamHistoryDAO();

    public abstract StreamStateDAO streamStateDAO();

    public abstract PlaylistDAO playlistDAO();

    public abstract PlaylistStreamDAO playlistStreamDAO();

    public abstract PlaylistRemoteDAO playlistRemoteDAO();
}
