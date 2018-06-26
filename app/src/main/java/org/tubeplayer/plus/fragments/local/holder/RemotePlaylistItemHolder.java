package org.tubeplayer.plus.fragments.local.holder;

import android.view.ViewGroup;

import org.tubeplayer.plus.database.LocalItem;
import org.tubeplayer.plus.database.playlist.model.PlaylistRemoteEntity;
import org.tubeplayer.plus.fragments.local.LocalItemBuilder;
import org.tubeplayer.plus.util.ImageDisplayConstants;
import org.schabi.newpipe.extractor.NewPipe;
import org.tubeplayer.plus.util.Localization;

import java.text.DateFormat;

public class RemotePlaylistItemHolder extends PlaylistItemHolder {
    public RemotePlaylistItemHolder(LocalItemBuilder infoItemBuilder, ViewGroup parent) {
        super(infoItemBuilder, parent);
    }

    @Override
    public void updateFromItem(final LocalItem localItem, final DateFormat dateFormat) {
        if (!(localItem instanceof PlaylistRemoteEntity)) return;
        final PlaylistRemoteEntity item = (PlaylistRemoteEntity) localItem;

        itemTitleView.setText(item.getName());
        itemStreamCountView.setText(String.valueOf(item.getStreamCount()));
        itemUploaderView.setText(Localization.concatenateStrings(item.getUploader(),
                NewPipe.getNameOfService(item.getServiceId())));

        itemBuilder.displayImage(item.getThumbnailUrl(), itemThumbnailView,
                ImageDisplayConstants.DISPLAY_PLAYLIST_OPTIONS);

        super.updateFromItem(localItem, dateFormat);
    }
}
