/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2010 (C) Sindre Mehus
 */
package github.daneren2005.dsub.util;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import github.daneren2005.dsub.domain.MusicDirectory;

/**
 * @author Sindre Mehus
 */
public class EntryAdapter extends ArrayAdapter<MusicDirectory.Entry> {

    private final Context mContext;
    private final ImageLoader imageLoader;
    private final boolean checkable;

    public EntryAdapter(Context context, ImageLoader imageLoader, List<MusicDirectory.Entry> entries, boolean checkable) {
        super(context, android.R.layout.simple_list_item_1, entries);
        mContext = context;
        this.imageLoader = imageLoader;
        this.checkable = checkable;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MusicDirectory.Entry entry = getItem(position);

        if (entry.isDirectory()) {
            AlbumView view;
  // TODO: Reuse AlbumView objects once cover art loading is working.
//            if (convertView != null && convertView instanceof AlbumView) {
//                view = (AlbumView) convertView;
//            } else {
                view = new AlbumView(mContext);
//            }
            view.setAlbum(entry, imageLoader);
            return view;

        } else {
            SongView view;
            if (convertView != null && convertView instanceof SongView) {
                view = (SongView) convertView;
            } else {
                view = new SongView(mContext);
            }
            view.setSong(entry, checkable);
            return view;
        }
    }
}
