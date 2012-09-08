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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.Artist;
import github.daneren2005.dsub.domain.Indexes;
import github.daneren2005.dsub.domain.MusicFolder;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.ArtistAdapter;
import github.daneren2005.dsub.util.BackgroundTask;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.TabActivityBackgroundTask;
import github.daneren2005.dsub.util.Util;

import java.util.ArrayList;
import java.util.List;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class SelectArtistActivity extends SubsonicTabActivity 
implements AdapterView.OnItemClickListener, ActionBar.OnNavigationListener {

    private ListView artistList;
    private List<MusicFolder> musicFolders;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_artist);
        
        artistList = (ListView) findViewById(R.id.select_artist_list);
        artistList.setOnItemClickListener(this);
        
        musicFolders = null;
        loadMusicFolders();
        
        registerForContextMenu(artistList);
        
        refresh();
    }

    protected void refresh() {
        setTitle(Util.isOffline(this) ? R.string.music_library_label_offline : R.string.music_library_label);
    	loadArtists(true);
    }

    private void loadMusicFolders() {
        BackgroundTask<List<MusicFolder>> task = new TabActivityBackgroundTask<List<MusicFolder>>(this) {
            @Override
            protected List<MusicFolder> doInBackground() throws Throwable {
                boolean refresh = getIntent().getBooleanExtra(Constants.INTENT_EXTRA_NAME_REFRESH, false);
                MusicService musicService = MusicServiceFactory.getMusicService(SelectArtistActivity.this);
                if (!Util.isOffline(SelectArtistActivity.this)) {
                    return musicService.getMusicFolders(refresh, SelectArtistActivity.this, this);
                } else {
                	return null;
                }
            }

            @Override
            protected void done(List<MusicFolder> result) {
            	musicFolders = result;
            	if (musicFolders != null) {
            		Context context = getSupportActionBar().getThemedContext();
            		List<CharSequence> musicFolderNames = new ArrayList<CharSequence>(musicFolders.size() + 1);
            		musicFolderNames.add(getString(R.string.select_artist_all_folders));
            		for (MusicFolder folder: musicFolders) {
            			musicFolderNames.add(folder.getName());
            		}
                    ArrayAdapter<CharSequence> list = new ArrayAdapter<CharSequence>(context, R.layout.sherlock_spinner_item, musicFolderNames);
                    list.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);

                    getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
                    getSupportActionBar().setListNavigationCallbacks(list, SelectArtistActivity.this);

                    String musicFolderId = Util.getSelectedMusicFolderId(SelectArtistActivity.this);
                    int dropDownId = musicFolderId == null ? 0 : Integer.valueOf(musicFolderId) + 1;
                    getSupportActionBar().setSelectedNavigationItem(dropDownId);
            	}
                
            }
        };
        task.execute();
    }
    
    private void loadArtists(final boolean refresh) {
        BackgroundTask<Indexes> task = new TabActivityBackgroundTask<Indexes>(this) {
            @Override
            protected Indexes doInBackground() throws Throwable {
                MusicService musicService = MusicServiceFactory.getMusicService(SelectArtistActivity.this);
                String musicFolderId = Util.getSelectedMusicFolderId(SelectArtistActivity.this);
                return musicService.getIndexes(musicFolderId, refresh, SelectArtistActivity.this, this);
            }

            @Override
            protected void done(Indexes result) {
                List<Artist> artists = new ArrayList<Artist>(result.getShortcuts().size() + result.getArtists().size());
                artists.addAll(result.getShortcuts());
                artists.addAll(result.getArtists());
                artistList.setAdapter(new ArtistAdapter(SelectArtistActivity.this, artists));
            }
        };
        task.execute();
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
    	MusicFolder selectedFolder = itemId == 0 ? null : musicFolders.get((int) itemId - 1);
        String musicFolderId = selectedFolder == null ? null : selectedFolder.getId();
        Util.setSelectedMusicFolderId(this, musicFolderId);
        refresh();
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    	Artist artist = (Artist) parent.getItemAtPosition(position);
    	Intent intent = new Intent(this, SelectAlbumActivity.class);
    	intent.putExtra(Constants.INTENT_EXTRA_NAME_ID, artist.getId());
    	intent.putExtra(Constants.INTENT_EXTRA_NAME_NAME, artist.getName());
    	startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.select_artist_options, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        
	        case R.id.action_refresh:
	        	refresh();
	        	return true;
	        	
	        case R.id.action_shuffle:
	        	Intent intent = new Intent(SelectArtistActivity.this, DownloadActivity.class);
                intent.putExtra(Constants.INTENT_EXTRA_NAME_SHUFFLE, true);
                showTabActivity(DownloadActivity.class, intent);
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        	AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

        	if (artistList.getItemAtPosition(info.position) instanceof Artist) {
        		android.view.MenuInflater inflater = getMenuInflater();
        		inflater.inflate(R.menu.select_artist_context, menu);
        	}
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem menuItem) {
    	  AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();

    	  Artist artist = (Artist) artistList.getItemAtPosition(info.position);

    	  if (artist != null) {
    		  switch (menuItem.getItemId()) {
    		  case R.id.artist_menu_play_now:
    			  downloadRecursively(artist.getId(), false, false, true, false);
    			  break;
    		  case R.id.artist_menu_play_shuffled:
    			  downloadRecursively(artist.getId(), false, false, true, true);
    			  break;
    		  case R.id.artist_menu_play_last:
    			  downloadRecursively(artist.getId(), false, true, false, false);
    			  break;
    		  case R.id.artist_menu_pin:
    			  downloadRecursively(artist.getId(), true, true, false, false);
    			  break;
    		  default:
    			  return super.onContextItemSelected(menuItem);
    		  }
    	  }

        return true;
    }
}