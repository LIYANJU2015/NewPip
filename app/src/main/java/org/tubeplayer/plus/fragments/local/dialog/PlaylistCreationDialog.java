package org.tubeplayer.plus.fragments.local.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.tubeplayer.plus.NewPipeDatabase;
import org.tubeplayer.plus.database.stream.model.StreamEntity;
import org.tubeplayer.plus.fragments.local.LocalPlaylistManager;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;

public final class PlaylistCreationDialog extends PlaylistDialog {
    private static final String TAG = PlaylistCreationDialog.class.getCanonicalName();

    public static PlaylistCreationDialog newInstance(final List<StreamEntity> streams) {
        PlaylistCreationDialog dialog = new PlaylistCreationDialog();
        dialog.setInfo(streams);
        return dialog;
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Dialog
    //////////////////////////////////////////////////////////////////////////*/

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (getStreams() == null) return super.onCreateDialog(savedInstanceState);

        View dialogView = View.inflate(getContext(), org.tubeplayer.plus.R.layout.dialog_playlist_name, null);
        EditText nameInput = dialogView.findViewById(org.tubeplayer.plus.R.id.playlist_name);

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext())
                .setTitle(org.tubeplayer.plus.R.string.create_playlist)
                .setView(dialogView)
                .setCancelable(true)
                .setNegativeButton(org.tubeplayer.plus.R.string.cancel, null)
                .setPositiveButton(org.tubeplayer.plus.R.string.create, (dialogInterface, i) -> {
                    final String name = nameInput.getText().toString();
                    final LocalPlaylistManager playlistManager =
                            new LocalPlaylistManager(NewPipeDatabase.getInstance(getContext()));
                    final Toast successToast = Toast.makeText(getActivity(),
                            org.tubeplayer.plus.R.string.playlist_creation_success,
                            Toast.LENGTH_SHORT);

                    playlistManager.createPlaylist(name, getStreams())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(longs -> successToast.show());
                });

        return dialogBuilder.create();
    }
}
