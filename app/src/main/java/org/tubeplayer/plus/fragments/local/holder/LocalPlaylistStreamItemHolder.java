package org.tubeplayer.plus.fragments.local.holder;

import android.support.v4.content.ContextCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.tubeplayer.plus.fragments.local.LocalItemBuilder;
import org.tubeplayer.plus.database.LocalItem;
import org.tubeplayer.plus.database.playlist.PlaylistStreamEntry;
import org.schabi.newpipe.extractor.NewPipe;
import org.tubeplayer.plus.util.ImageDisplayConstants;
import org.tubeplayer.plus.util.Localization;

import java.text.DateFormat;

public class LocalPlaylistStreamItemHolder extends LocalItemHolder {

    public final ImageView itemThumbnailView;
    public final TextView itemVideoTitleView;
    public final TextView itemAdditionalDetailsView;
    public final TextView itemDurationView;
    public final View itemHandleView;

    LocalPlaylistStreamItemHolder(LocalItemBuilder infoItemBuilder, int layoutId, ViewGroup parent) {
        super(infoItemBuilder, layoutId, parent);

        itemThumbnailView = itemView.findViewById(org.tubeplayer.plus.R.id.itemThumbnailView);
        itemVideoTitleView = itemView.findViewById(org.tubeplayer.plus.R.id.itemVideoTitleView);
        itemAdditionalDetailsView = itemView.findViewById(org.tubeplayer.plus.R.id.itemAdditionalDetails);
        itemDurationView = itemView.findViewById(org.tubeplayer.plus.R.id.itemDurationView);
        itemHandleView = itemView.findViewById(org.tubeplayer.plus.R.id.itemHandle);
    }

    public LocalPlaylistStreamItemHolder(LocalItemBuilder infoItemBuilder, ViewGroup parent) {
        this(infoItemBuilder, org.tubeplayer.plus.R.layout.list_stream_playlist_item, parent);
    }

    @Override
    public void updateFromItem(final LocalItem localItem, final DateFormat dateFormat) {
        if (!(localItem instanceof PlaylistStreamEntry)) return;
        final PlaylistStreamEntry item = (PlaylistStreamEntry) localItem;

        itemVideoTitleView.setText(item.title);
        itemAdditionalDetailsView.setText(Localization.concatenateStrings(item.uploader,
                NewPipe.getNameOfService(item.serviceId)));

        if (item.duration > 0) {
            itemDurationView.setText(Localization.getDurationString(item.duration));
            itemDurationView.setBackgroundColor(ContextCompat.getColor(itemBuilder.getContext(),
                    org.tubeplayer.plus.R.color.duration_background_color));
            itemDurationView.setVisibility(View.VISIBLE);
        } else {
            itemDurationView.setVisibility(View.GONE);
        }

        // Default thumbnail is shown on error, while loading and if the url is empty
        itemBuilder.displayImage(item.thumbnailUrl, itemThumbnailView,
                ImageDisplayConstants.DISPLAY_THUMBNAIL_OPTIONS);

        itemView.setOnClickListener(view -> {
            if (itemBuilder.getOnItemSelectedListener() != null) {
                itemBuilder.getOnItemSelectedListener().selected(item);
            }
        });

        itemView.setLongClickable(true);
        itemView.setOnLongClickListener(view -> {
            if (itemBuilder.getOnItemSelectedListener() != null) {
                itemBuilder.getOnItemSelectedListener().held(item);
            }
            return true;
        });

        itemThumbnailView.setOnTouchListener(getOnTouchListener(item));
        itemHandleView.setOnTouchListener(getOnTouchListener(item));
    }

    private View.OnTouchListener getOnTouchListener(final PlaylistStreamEntry item) {
        return (view, motionEvent) -> {
            view.performClick();
            if (itemBuilder != null && itemBuilder.getOnItemSelectedListener() != null &&
                    motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
                itemBuilder.getOnItemSelectedListener().drag(item,
                        LocalPlaylistStreamItemHolder.this);
            }
            return false;
        };
    }
}
