package org.tubeplayer.plus.fragments.local.holder;

import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.tubeplayer.plus.fragments.local.LocalItemBuilder;
import org.tubeplayer.plus.database.LocalItem;

import java.text.DateFormat;

public abstract class PlaylistItemHolder extends LocalItemHolder {
    public final ImageView itemThumbnailView;
    public final TextView itemStreamCountView;
    public final TextView itemTitleView;
    public final TextView itemUploaderView;

    public PlaylistItemHolder(LocalItemBuilder infoItemBuilder,
                              int layoutId, ViewGroup parent) {
        super(infoItemBuilder, layoutId, parent);

        itemThumbnailView = itemView.findViewById(org.tubeplayer.plus.R.id.itemThumbnailView);
        itemTitleView = itemView.findViewById(org.tubeplayer.plus.R.id.itemTitleView);
        itemStreamCountView = itemView.findViewById(org.tubeplayer.plus.R.id.itemStreamCountView);
        itemUploaderView = itemView.findViewById(org.tubeplayer.plus.R.id.itemUploaderView);
    }

    public PlaylistItemHolder(LocalItemBuilder infoItemBuilder, ViewGroup parent) {
        this(infoItemBuilder, org.tubeplayer.plus.R.layout.list_playlist_mini_item, parent);
    }

    @Override
    public void updateFromItem(final LocalItem localItem, final DateFormat dateFormat) {
        itemView.setOnClickListener(view -> {
            if (itemBuilder.getOnItemSelectedListener() != null) {
                itemBuilder.getOnItemSelectedListener().selected(localItem);
            }
        });

        itemView.setLongClickable(true);
        itemView.setOnLongClickListener(view -> {
            if (itemBuilder.getOnItemSelectedListener() != null) {
                itemBuilder.getOnItemSelectedListener().held(localItem);
            }
            return true;
        });
    }
}
