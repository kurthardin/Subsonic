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
import github.daneren2005.dsub.activity.DownloadActivity;
import github.daneren2005.dsub.activity.MainActivity;
import github.daneren2005.dsub.activity.SelectAlbumActivity;
import github.daneren2005.dsub.domain.Artist;
import github.daneren2005.dsub.domain.Indexes;
import github.daneren2005.dsub.domain.MusicFolder;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.ArtistAdapter;
import github.daneren2005.dsub.util.BackgroundTask;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.TabFragmentBackgroundTask;
import github.daneren2005.dsub.util.Util;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.actionbarsherlock.internal.widget.IcsAdapterView;
import com.actionbarsherlock.internal.widget.IcsAdapterView.OnItemSelectedListener;
import com.actionbarsherlock.internal.widget.IcsSpinner;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class SelectArtistFragment extends SubsonicTabFragment 
implements OnItemSelectedListener {

    private List<MusicFolder> musicFolders;
    private View mMusicFolderHeader;
    private IcsSpinner mMusicFolderSpinner;
    
    private boolean mShouldRefresh;
    
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.select_artist, container, false);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
    	registerForContextMenu(getListView());
    	
    	mMusicFolderHeader = getView().findViewById(R.id.music_folder_header);
    	
    	mMusicFolderSpinner = (IcsSpinner) getView().findViewById(R.id.music_folder_spinner);
        musicFolders = null;
        loadMusicFolders();
        
    	super.onActivityCreated(savedInstanceState);
    }
    
    public void refresh() {
    	mMusicFolderHeader.setVisibility(Util.isOffline(getActivity()) ? View.GONE : View.VISIBLE);
    	loadArtists(true);
    }
    
    public void shouldRefresh(boolean refresh) {
    	mShouldRefresh = refresh;
    }

    private void loadMusicFolders() {
        BackgroundTask<List<MusicFolder>> task = new TabFragmentBackgroundTask<List<MusicFolder>>(SelectArtistFragment.this) {
            @Override
            protected List<MusicFolder> doInBackground() throws Throwable {
                MusicService musicService = MusicServiceFactory.getMusicService(getActivity());
                if (!Util.isOffline(getActivity())) {
                    return musicService.getMusicFolders(mShouldRefresh, getActivity(), this);
                } else {
                	return null;
                }
            }

            @Override
            protected void done(List<MusicFolder> result) {
            	musicFolders = result;
            	if (musicFolders != null) {
            		MainActivity activity = getMainActivity();
            		List<CharSequence> musicFolderNames = new ArrayList<CharSequence>(musicFolders.size() + 1);
            		musicFolderNames.add(getString(R.string.select_artist_all_folders));
            		for (MusicFolder folder: musicFolders) {
            			musicFolderNames.add(folder.getName());
            		}
                    ArrayAdapter<CharSequence> list = new ArrayAdapter<CharSequence>(activity, R.layout.sherlock_spinner_item, musicFolderNames);
                    list.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
                    mMusicFolderSpinner.setAdapter(list);
                    mMusicFolderSpinner.setOnItemSelectedListener(SelectArtistFragment.this);

                    String musicFolderId = Util.getSelectedMusicFolderId(activity);
                    int dropDownId = musicFolderId == null ? 0 : Integer.valueOf(musicFolderId) + 1;
                    mMusicFolderSpinner.setSelection(dropDownId);
            	}
                
            }
        };
        task.execute();
    }
    
    private void loadArtists(final boolean refresh) {
        BackgroundTask<Indexes> task = new TabFragmentBackgroundTask<Indexes>(SelectArtistFragment.this) {
            @Override
            protected Indexes doInBackground() throws Throwable {
                MusicService musicService = MusicServiceFactory.getMusicService(getActivity());
                String musicFolderId = Util.getSelectedMusicFolderId(getActivity());
                return musicService.getIndexes(musicFolderId, refresh, getActivity(), this);
            }

            @Override
            protected void done(Indexes result) {
                updateProgress(getString(R.string.select_artist_empty));
                List<Artist> artists = new ArrayList<Artist>(result.getShortcuts().size() + result.getArtists().size());
                artists.addAll(result.getShortcuts());
                artists.addAll(result.getArtists());
                setListAdapter(new ArtistAdapter(getActivity(), artists));
            }
        };
        task.execute();
    }

    @Override
    public void onItemSelected(IcsAdapterView<?> parent, View view, int itemPosition, long itemId) {
    	MusicFolder selectedFolder = itemId == 0 ? null : musicFolders.get((int) itemId - 1);
        String musicFolderId = selectedFolder == null ? null : selectedFolder.getId();
        Util.setSelectedMusicFolderId(getActivity(), musicFolderId);
        refresh();
    }

	@Override
	public void onNothingSelected(IcsAdapterView<?> parent) {
		// Do nothing...
	}

    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
    	Artist artist = (Artist) list.getItemAtPosition(position);
    	Intent intent = new Intent(getActivity(), SelectAlbumActivity.class);
    	intent.putExtra(Constants.INTENT_EXTRA_NAME_ID, artist.getId());
    	intent.putExtra(Constants.INTENT_EXTRA_NAME_NAME, artist.getName());
    	startActivity(intent);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.select_artist_options, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
	        case R.id.action_refresh:
	        	refresh();
	        	return true;
	        	
	        case R.id.action_shuffle:
	        	Intent intent = new Intent(getActivity(), DownloadActivity.class);
                intent.putExtra(Constants.INTENT_EXTRA_NAME_SHUFFLE, true);
                startActivity(intent);
                return true;
                
            default:
            	return super.onOptionsItemSelected(item);
        } 
    }
 
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        	AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

        	if (getListView().getItemAtPosition(info.position) instanceof Artist) {
        		android.view.MenuInflater inflater = getActivity().getMenuInflater();
        		inflater.inflate(R.menu.select_artist_context, menu);
        	}
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem menuItem) {
    	if (menuItem.getGroupId() == R.id.select_artist_context_menu) {  
    		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();

    		Artist artist = (Artist) getListView().getItemAtPosition(info.position);

    		if (artist != null) {
    			MainActivity activity = getMainActivity();
    			switch (menuItem.getItemId()) {
    			case R.id.artist_menu_play_now:
    				activity.downloadRecursively(artist.getId(), false, false, true, false);
    				break;
    			case R.id.artist_menu_play_shuffled:
    				activity.downloadRecursively(artist.getId(), false, false, true, true);
    				break;
    			case R.id.artist_menu_play_last:
    				activity.downloadRecursively(artist.getId(), false, true, false, false);
    				break;
    			case R.id.artist_menu_pin:
    				activity.downloadRecursively(artist.getId(), true, true, false, false);
    				break;
    			default:
    				return super.onContextItemSelected(menuItem);
    			}
    		}

    		return true;
    	} else {
    		return super.onContextItemSelected(menuItem);
    	}
    }
}