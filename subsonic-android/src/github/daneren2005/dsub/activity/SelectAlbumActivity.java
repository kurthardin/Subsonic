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

 Copyright 2009 (C) Sindre Mehus
 */
package github.daneren2005.dsub.activity;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.fragment.SelectAlbumFragment;
import github.daneren2005.dsub.util.Constants;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

public class SelectAlbumActivity extends SubsonicTabActivity { //implements Refreshable {

    private static final String TAG = SelectAlbumActivity.class.getSimpleName();

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_album_activity);
        
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment selectAlbumFragment = new SelectAlbumFragment();
        
        Bundle args = new Bundle();
        args.putString(Constants.INTENT_EXTRA_NAME_ID, getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_ID));
        args.putString(Constants.INTENT_EXTRA_NAME_NAME, getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_NAME));
        args.putString(Constants.INTENT_EXTRA_NAME_PLAYLIST_ID, getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_ID));
        args.putString(Constants.INTENT_EXTRA_NAME_PLAYLIST_NAME, getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_NAME));
        args.putInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE, getIntent().getIntExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE, -1));
        args.putInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, getIntent().getIntExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, 0));
        args.putInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET, getIntent().getIntExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET, 0));
        selectAlbumFragment.setArguments(args);
        
        fragmentTransaction.add(R.id.select_album_fragment_container, selectAlbumFragment);
        fragmentTransaction.commit();
    }
}
