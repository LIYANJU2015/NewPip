package org.tubeplayer.plus.playlist;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by Christian Schabesberger on 01.08.16.
 * <p>
 * Copyright (C) Christian Schabesberger 2016 <chris.schabesberger@mailbox.org>
 * StreamInfoItemHolder.java is part of NewPipe.
 * <p>
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */

public class PlayQueueItemHolder extends RecyclerView.ViewHolder {

    public final TextView itemVideoTitleView, itemDurationView, itemAdditionalDetailsView;
    public final ImageView itemSelected, itemThumbnailView, itemHandle;

    public final View itemRoot;

    public PlayQueueItemHolder(View v) {
        super(v);
        itemRoot = v.findViewById(org.tubeplayer.plus.R.id.itemRoot);
        itemVideoTitleView = v.findViewById(org.tubeplayer.plus.R.id.itemVideoTitleView);
        itemDurationView = v.findViewById(org.tubeplayer.plus.R.id.itemDurationView);
        itemAdditionalDetailsView = v.findViewById(org.tubeplayer.plus.R.id.itemAdditionalDetails);
        itemSelected = v.findViewById(org.tubeplayer.plus.R.id.itemSelected);
        itemThumbnailView = v.findViewById(org.tubeplayer.plus.R.id.itemThumbnailView);
        itemHandle = v.findViewById(org.tubeplayer.plus.R.id.itemHandle);
    }
}
