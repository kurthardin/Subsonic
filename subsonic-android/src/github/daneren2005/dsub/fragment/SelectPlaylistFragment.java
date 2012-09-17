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

package github.daneren2005.dsub.fragment;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.activity.SelectAlbumActivity;
import github.daneren2005.dsub.domain.Playlist;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.BackgroundTask;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.TabFragmentBackgroundTask;
import github.daneren2005.dsub.util.PlaylistAdapter;

import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class SelectPlaylistFragment extends SubsonicTabFragment {

    private static final int MENU_ITEM_PLAY_ALL = 1;
	private static final int MENU_ITEM_PLAY_SHUFFLED = 2;

    private boolean mShouldRefresh;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View view = inflater.inflate(R.layout.select_playlist, container, false);
    	return view;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        registerForContextMenu(getListView());
    	super.onActivityCreated(savedInstanceState);
    }
    
    @Override
	public void refresh() {
        load();
	}

    private void load() {
        BackgroundTask<List<Playlist>> task = new TabFragmentBackgroundTask<List<Playlist>>(SelectPlaylistFragment.this) {
            @Override
            protected List<Playlist> doInBackground() throws Throwable {
                MusicService musicService = MusicServiceFactory.getMusicService(getMainActivity());
                return musicService.getPlaylists(mShouldRefresh, getMainActivity(), this);
            }

            @Override
            protected void done(List<Playlist> result) {
                updateProgress(getString(R.string.select_artist_empty));
                setListAdapter(new PlaylistAdapter(getMainActivity(), PlaylistAdapter.PlaylistComparator.sort(result)));
            }
        };
        task.execute();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.select_playlist_options, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        
	        case R.id.action_refresh:
	        	refresh();
	        	return true;
	        
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        menu.add(R.id.playlist_context_menu, MENU_ITEM_PLAY_ALL, MENU_ITEM_PLAY_ALL, R.string.common_play_now);
		menu.add(R.id.playlist_context_menu, MENU_ITEM_PLAY_SHUFFLED, MENU_ITEM_PLAY_SHUFFLED, R.string.common_play_shuffled);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem menuItem) {
    	if (menuItem.getGroupId() == R.id.playlist_context_menu) {
    		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
    		Playlist playlist = (Playlist) getListView().getItemAtPosition(info.position);

    		Intent intent;
    		switch (menuItem.getItemId()) {
    			case MENU_ITEM_PLAY_ALL:
    				intent = new Intent(getActivity(), SelectAlbumActivity.class);
    				intent.putExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_ID, playlist.getId());
    				intent.putExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_NAME, playlist.getName());
    				intent.putExtra(Constants.INTENT_EXTRA_NAME_AUTOPLAY, true);
    				startActivity(intent);
    				break;
    			case MENU_ITEM_PLAY_SHUFFLED:
    				intent = new Intent(getActivity(), SelectAlbumActivity.class);
    				intent.putExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_ID, playlist.getId());
    				intent.putExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_NAME, playlist.getName());
    				intent.putExtra(Constants.INTENT_EXTRA_NAME_AUTOPLAY, true);
    				intent.putExtra(Constants.INTENT_EXTRA_NAME_SHUFFLE, true);
    				startActivity(intent);
    				break;
    			default:
    				return super.onContextItemSelected(menuItem);
    		}
    		return true;
    	} else {
    		return super.onContextItemSelected(menuItem);
    	}
    }

    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
        Playlist playlist = (Playlist) list.getItemAtPosition(position);
        Intent intent = new Intent(getActivity(), SelectAlbumActivity.class);
        intent.putExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_ID, playlist.getId());
        intent.putExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_NAME, playlist.getName());
        startActivity(intent);
    }

}