package org.playtube.plus.info_list.holder;

import android.view.ViewGroup;
import android.widget.TextView;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;
import org.playtube.plus.info_list.InfoItemBuilder;
import org.playtube.plus.util.ImageDisplayConstants;
import org.playtube.plus.util.Localization;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChannelMiniInfoItemHolder extends InfoItemHolder {
    public final CircleImageView itemThumbnailView;
    public final TextView itemTitleView;
    public final TextView itemAdditionalDetailView;

    ChannelMiniInfoItemHolder(InfoItemBuilder infoItemBuilder, int layoutId, ViewGroup parent) {
        super(infoItemBuilder, layoutId, parent);

        itemThumbnailView = itemView.findViewById(org.playtube.plus.R.id.itemThumbnailView);
        itemTitleView = itemView.findViewById(org.playtube.plus.R.id.itemTitleView);
        itemAdditionalDetailView = itemView.findViewById(org.playtube.plus.R.id.itemAdditionalDetails);
    }

    public ChannelMiniInfoItemHolder(InfoItemBuilder infoItemBuilder, ViewGroup parent) {
        this(infoItemBuilder, org.playtube.plus.R.layout.list_channel_mini_item, parent);
    }

    @Override
    public void updateFromItem(final InfoItem infoItem) {
        if (!(infoItem instanceof ChannelInfoItem)) return;
        final ChannelInfoItem item = (ChannelInfoItem) infoItem;

        itemTitleView.setText(item.getName());
        itemAdditionalDetailView.setText(getDetailLine(item));

        itemBuilder.getImageLoader()
                .displayImage(item.getThumbnailUrl(),
                        itemThumbnailView,
                        ImageDisplayConstants.DISPLAY_THUMBNAIL_OPTIONS);

        itemView.setOnClickListener(view -> {
            if (itemBuilder.getOnChannelSelectedListener() != null) {
                itemBuilder.getOnChannelSelectedListener().selected(item);
            }
        });
    }

    protected String getDetailLine(final ChannelInfoItem item) {
        String details = "";
        if (item.getSubscriberCount() >= 0) {
            details += Localization.shortSubscriberCount(itemBuilder.getContext(),
                    item.getSubscriberCount());
        }
        return details;
    }
}
