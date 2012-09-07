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

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
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

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class SelectArtistActivity extends SubsonicTabActivity implements AdapterView.OnItemClickListener {

    private static final int MENU_GROUP_MUSIC_FOLDER = 10;

    private ListView artistList;
    private View folderButton;
    private TextView folderName;
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
        
        folderButton = findViewById(R.id.select_artist_folder);
    	folderButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				selectFolder();
			}
		});
        folderName = (TextView) folderButton.findViewById(R.id.select_artist_folder_2);
        
        refresh();
    }

    protected void refresh() {

    	folderButton.setVisibility(Util.isOffline(this) ? View.GONE : View.VISIBLE);
    	
        registerForContextMenu(folderButton);
        registerForContextMenu(artistList);

        setTitle(Util.isOffline(this) ? R.string.music_library_label_offline : R.string.music_library_label);
		
    	musicFolders = null;
    	load();
    	
    }

    private void selectFolder() {
        folderButton.showContextMenu();
    }

    private void load() {
        BackgroundTask<Indexes> task = new TabActivityBackgroundTask<Indexes>(this) {
            @Override
            protected Indexes doInBackground() throws Throwable {
                boolean refresh = getIntent().getBooleanExtra(Constants.INTENT_EXTRA_NAME_REFRESH, false);
                MusicService musicService = MusicServiceFactory.getMusicService(SelectArtistActivity.this);
                if (!Util.isOffline(SelectArtistActivity.this)) {
                    musicFolders = musicService.getMusicFolders(refresh, SelectArtistActivity.this, this);
                }
                String musicFolderId = Util.getSelectedMusicFolderId(SelectArtistActivity.this);
                return musicService.getIndexes(musicFolderId, refresh, SelectArtistActivity.this, this);
            }

            @Override
            protected void done(Indexes result) {
                List<Artist> artists = new ArrayList<Artist>(result.getShortcuts().size() + result.getArtists().size());
                artists.addAll(result.getShortcuts());
                artists.addAll(result.getArtists());
                artistList.setAdapter(new ArtistAdapter(SelectArtistActivity.this, artists));

                // Display selected music folder
                if (musicFolders != null) {
                    String musicFolderId = Util.getSelectedMusicFolderId(SelectArtistActivity.this);
                    if (musicFolderId == null) {
                        folderName.setText(R.string.select_artist_all_folders);
                    } else {
                        for (MusicFolder musicFolder : musicFolders) {
                            if (musicFolder.getId().equals(musicFolderId)) {
                                folderName.setText(musicFolder.getName());
                                break;
                            }
                        }
                    }
                }
            }
        };
        task.execute();
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

        if (view ==  folderButton) {
        	String musicFolderId = Util.getSelectedMusicFolderId(this);
        	android.view.MenuItem menuItem = menu.add(MENU_GROUP_MUSIC_FOLDER, -1, 0, R.string.select_artist_all_folders);
            if (musicFolderId == null) {
                menuItem.setChecked(true);
            }
            if (musicFolders != null) {
                for (int i = 0; i < musicFolders.size(); i++) {
                    MusicFolder musicFolder = musicFolders.get(i);
                    menuItem = menu.add(MENU_GROUP_MUSIC_FOLDER, i, i + 1, musicFolder.getName());
                    if (musicFolder.getId().equals(musicFolderId)) {
                        menuItem.setChecked(true);
                    }
                }
            }
            menu.setGroupCheckable(MENU_GROUP_MUSIC_FOLDER, true, true);
        } else {
        	AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

        	if (artistList.getItemAtPosition(info.position) instanceof Artist) {
        		android.view.MenuInflater inflater = getMenuInflater();
        		inflater.inflate(R.menu.select_artist_context, menu);
        	}
        }
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem menuItem) {
      if (menuItem.getGroupId() == MENU_GROUP_MUSIC_FOLDER) {
    	  MusicFolder selectedFolder = menuItem.getItemId() == -1 ? null : musicFolders.get(menuItem.getItemId());
          String musicFolderId = selectedFolder == null ? null : selectedFolder.getId();
          String musicFolderName = selectedFolder == null ? getString(R.string.select_artist_all_folders)
                                                          : selectedFolder.getName();
          Util.setSelectedMusicFolderId(this, musicFolderId);
          folderName.setText(musicFolderName);
          refresh();
      } else {
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
        }

        return true;
    }
}