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
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.interfaces.Refreshable;
import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.EntryAdapter;
import github.daneren2005.dsub.util.Pair;
import github.daneren2005.dsub.util.TabActivityBackgroundTask;
import github.daneren2005.dsub.util.Util;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.WrapperListAdapter;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class SelectAlbumActivity extends SubsonicActivity implements Refreshable {

    private static final String TAG = SelectAlbumActivity.class.getSimpleName();

    private List<MusicDirectory.Entry> entryList;
    
    private ListView mEntryListView;
    private View footer;
    private View emptyView;
    private Button selectButton;
    private Button playNowButton;
	private Button playShuffledButton;
    private Button playLastButton;
    private Button pinButton;
    private Button unpinButton;
    private Button deleteButton;
    private Button moreButton;
    private boolean licenseValid;
    
    private String mMusicDirectoryFetchId;
    private String mMusicDirectoryFetchName;
    private String mPlaylistFetchId;
    private String mPlaylistFetchName;
    private String mAlbumListType;
    private int mAlbumListFetchSize;
    private int mAlbumListFetchOffset;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_album);
        
        entryList = new ArrayList<MusicDirectory.Entry>();
        mEntryListView = (ListView) findViewById(android.R.id.list);

        footer = LayoutInflater.from(this).inflate(R.layout.select_album_footer, mEntryListView, false);
        mEntryListView.addFooterView(footer);
        mEntryListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mEntryListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0) {
                    MusicDirectory.Entry entry = (MusicDirectory.Entry) parent.getItemAtPosition(position);
                    if (entry.isDirectory()) {
                        Intent intent = new Intent(SelectAlbumActivity.this, SelectAlbumActivity.class);
                        intent.putExtra(Constants.INTENT_EXTRA_NAME_ID, entry.getId());
                        intent.putExtra(Constants.INTENT_EXTRA_NAME_NAME, entry.getTitle());
                        Util.startActivityWithoutTransition(SelectAlbumActivity.this, intent);
                    } else if (entry.isVideo()) {
                        playVideo(entry);
                    } else {
                        enableButtons();
                    }
                }
            }
        });

        selectButton = (Button) findViewById(R.id.select_album_select);
        playNowButton = (Button) findViewById(R.id.select_album_play_now);
		playShuffledButton = (Button) findViewById(R.id.select_album_play_shuffled);
        playLastButton = (Button) findViewById(R.id.select_album_play_last);
        pinButton = (Button) findViewById(R.id.select_album_pin);
        unpinButton = (Button) findViewById(R.id.select_album_unpin);
        deleteButton = (Button) findViewById(R.id.select_album_delete);
        moreButton = (Button) footer.findViewById(R.id.select_album_more);
        emptyView = findViewById(android.R.id.empty);

        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectAllOrNone();
            }
        });
        playNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                download(false, false, true, false, false);
                selectAll(false, false);
            }
        });
		playShuffledButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                download(false, false, true, false, true);
                selectAll(false, false);
            }
        });
        playLastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                download(true, false, false, false, false);
                selectAll(false, false);
            }
        });
        pinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                download(true, true, false, false, false);
                selectAll(false, false);
            }
        });
        unpinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                unpin();
                selectAll(false, false);
            }
        });
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                delete();
                selectAll(false, false);
            }
        });

        registerForContextMenu(mEntryListView);

        mMusicDirectoryFetchId = getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_ID);
        mMusicDirectoryFetchName = getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_NAME);
        mPlaylistFetchId = getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_ID);
        mPlaylistFetchName = getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_NAME);
        mAlbumListType = getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE);
        mAlbumListFetchSize = getIntent().getIntExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, 0);
        mAlbumListFetchOffset = getIntent().getIntExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET, 0);
        
        refresh();
        
    }

    public void refresh() {

        enableButtons();

        if (mPlaylistFetchId != null) {
            getPlaylist(mPlaylistFetchId, mPlaylistFetchName);
        } else if (mAlbumListType != null) {
            getAlbumList(mAlbumListType, mAlbumListFetchSize, mAlbumListFetchOffset);
        } else {
            getMusicDirectory(mMusicDirectoryFetchId, mMusicDirectoryFetchName);
        }
		
    }

    private void playAll(final boolean shuffle) {
        boolean hasSubFolders = false;
        for (int i = 0; i < mEntryListView.getCount(); i++) {
            MusicDirectory.Entry entry = (MusicDirectory.Entry) mEntryListView.getItemAtPosition(i);
            if (entry != null && entry.isDirectory()) {
                hasSubFolders = true;
                break;
            }
        }

        if (hasSubFolders && mMusicDirectoryFetchId != null) {
            downloadRecursively(mMusicDirectoryFetchId, false, false, true, shuffle);
        } else {
            selectAll(true, false);
            download(false, false, true, false, shuffle);
            selectAll(false, false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.select_album_options, menu);
        
        boolean isAlbumList = mAlbumListType != null;//getIntent().hasExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE);
        menu.findItem(R.id.action_play_all).setVisible(isAlbumList || mEntryListView.getCount() == 0 ? false : true);
        
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        
	        case R.id.action_refresh:
	        	refresh();
	        	return true;
	        	
	        case R.id.action_play_all:
	        	playAll(false);
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        
        if(menuInfo != null){
        	AdapterView.AdapterContextMenuInfo info =
        			(AdapterView.AdapterContextMenuInfo) menuInfo;

        	MusicDirectory.Entry entry = (MusicDirectory.Entry) mEntryListView.getItemAtPosition(info.position);

        	if (entry.isDirectory()) {
        		android.view.MenuInflater inflater = getMenuInflater();
        		inflater.inflate(R.menu.select_album_context, menu);
        	} else {
        		android.view.MenuInflater inflater = getMenuInflater();
        		inflater.inflate(R.menu.select_song_context, menu);
        	}
        }
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem menuItem) {
        if (menuItem.getGroupId() == R.id.select_album_context_menu ||
        		menuItem.getGroupId() == R.id.select_song_context_menu) {
        	AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
        	MusicDirectory.Entry entry = (MusicDirectory.Entry) mEntryListView.getItemAtPosition(info.position);
        	List<MusicDirectory.Entry> songs = new ArrayList<MusicDirectory.Entry>(10);
        	songs.add((MusicDirectory.Entry) mEntryListView.getItemAtPosition(info.position));
        	switch (menuItem.getItemId()) {
        		case R.id.album_menu_play_now:
        			downloadRecursively(entry.getId(), false, false, true, false);
        			break;
        		case R.id.album_menu_play_shuffled:
        			downloadRecursively(entry.getId(), false, false, true, true);
        			break;
        		case R.id.album_menu_play_last:
        			downloadRecursively(entry.getId(), false, true, false, false);
        			break;
        		case R.id.album_menu_pin:
        			downloadRecursively(entry.getId(), true, true, false, false);
        			break;
        		case R.id.song_menu_play_now:
        			Util.getDownloadService(this).download(songs, false, true, true, false);
        			break;
        		case R.id.song_menu_play_next:
        			Util.getDownloadService(this).download(songs, false, false, true, false);
        			break;
        		case R.id.song_menu_play_last:
        			Util.getDownloadService(this).download(songs, false, false, false, false);
        			break;
        		default:
        			return false;
        	}
        	return true;
        } else {
        	return super.onContextItemSelected(menuItem);
        }
    }
    
    public String getAlbumListType() {
    	return mAlbumListType;
    }

    private void getMusicDirectory(final String id, String name) {
        setTitle(name);

        new LoadTask() {
            @Override
            protected MusicDirectory load(MusicService service) throws Exception {
                boolean refresh = getIntent().getBooleanExtra(Constants.INTENT_EXTRA_NAME_REFRESH, false);
                return service.getMusicDirectory(id, refresh, SelectAlbumActivity.this, this);
            }
        }.execute();
    }

    private void getPlaylist(final String playlistId, final String playlistName) {
        setTitle(playlistName);

        new LoadTask() {
            @Override
            protected MusicDirectory load(MusicService service) throws Exception {
                return service.getPlaylist(playlistId, playlistName, SelectAlbumActivity.this, this);
            }
        }.execute();
    }

    private void getAlbumList(final String albumListFetchType, final int albumListFetchSize, final int albumListFetchOffset) {

        if ("newest".equals(albumListFetchType)) {
            setTitle(R.string.main_albums_newest);
        } else if ("random".equals(albumListFetchType)) {
            setTitle(R.string.main_albums_random);
        } else if ("highest".equals(albumListFetchType)) {
            setTitle(R.string.main_albums_highest);
        } else if ("recent".equals(albumListFetchType)) {
            setTitle(R.string.main_albums_recent);
        } else if ("frequent".equals(albumListFetchType)) {
            setTitle(R.string.main_albums_frequent);
        }

        new LoadTask() {
            @Override
            protected MusicDirectory load(MusicService service) throws Exception {
                return service.getAlbumList(albumListFetchType, albumListFetchSize, albumListFetchOffset, SelectAlbumActivity.this, this);
            }

            @Override
            protected void done(Pair<MusicDirectory, Boolean> result) {
                if (!result.getFirst().getChildren().isEmpty()) {
                    moreButton.setVisibility(View.VISIBLE);

                    moreButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                        	mAlbumListFetchOffset = entryList.size();
                        	mAlbumListFetchSize = mAlbumListFetchOffset;
                            refresh();
                        }
                    });
                }
                super.done(result);
            }
        }.execute();
    }

    private void selectAllOrNone() {
        boolean someUnselected = false;
        int count = mEntryListView.getCount();
        for (int i = 0; i < count; i++) {
            if (!mEntryListView.isItemChecked(i) && mEntryListView.getItemAtPosition(i) instanceof MusicDirectory.Entry) {
                someUnselected = true;
                break;
            }
        }
        selectAll(someUnselected, true);
    }

    private void selectAll(boolean selected, boolean toast) {
        int count = mEntryListView.getCount();
        int selectedCount = 0;
        for (int i = 0; i < count; i++) {
            MusicDirectory.Entry entry = (MusicDirectory.Entry) mEntryListView.getItemAtPosition(i);
            if (entry != null && !entry.isDirectory() && !entry.isVideo()) {
                mEntryListView.setItemChecked(i, selected);
                selectedCount++;
            }
        }

        // Display toast: N tracks selected / N tracks unselected
        if (toast) {
            int toastResId = selected ? R.string.select_album_n_selected
                                      : R.string.select_album_n_unselected;
            Util.toast(this, getString(toastResId, selectedCount));
        }

        enableButtons();
    }

    private void enableButtons() {
        if (Util.getDownloadService(this) == null) {
            return;
        }

        List<MusicDirectory.Entry> selection = getSelectedSongs();
        boolean enabled = !selection.isEmpty();
        boolean unpinEnabled = false;
        boolean deleteEnabled = false;

        for (MusicDirectory.Entry song : selection) {
            DownloadFile downloadFile = Util.getDownloadService(this).forSong(song);
            if (downloadFile.isCompleteFileAvailable()) {
                deleteEnabled = true;
            }
            if (downloadFile.isSaved()) {
                unpinEnabled = true;
            }
        }

        playNowButton.setEnabled(enabled);
		playShuffledButton.setEnabled(enabled);
        playLastButton.setEnabled(enabled);
        pinButton.setEnabled(enabled && !Util.isOffline(this));
        unpinButton.setEnabled(unpinEnabled);
        deleteButton.setEnabled(deleteEnabled);
    }

    private List<MusicDirectory.Entry> getSelectedSongs() {
        List<MusicDirectory.Entry> songs = new ArrayList<MusicDirectory.Entry>(10);
        int count = mEntryListView.getCount();
        for (int i = 0; i < count; i++) {
            if (mEntryListView.isItemChecked(i)) {
                songs.add((MusicDirectory.Entry) mEntryListView.getItemAtPosition(i));
            }
        }
        return songs;
    }

    private void download(final boolean append, final boolean save, final boolean autoplay, final boolean playNext, final boolean shuffle) {
        if (Util.getDownloadService(this) == null) {
            return;
        }

        final List<MusicDirectory.Entry> songs = getSelectedSongs();
        Runnable onValid = new Runnable() {
            @Override
            public void run() {
                if (!append) {
                    Util.getDownloadService(SelectAlbumActivity.this).clear();
                }

                warnIfNetworkOrStorageUnavailable();
                Util.getDownloadService(SelectAlbumActivity.this).download(songs, save, autoplay, playNext, shuffle);
                if (mPlaylistFetchName != null) {
                    Util.getDownloadService(SelectAlbumActivity.this).setSuggestedPlaylistName(mPlaylistFetchName);
                }
                if (autoplay) {
//                    showTabActivity(DownloadActivity.class); // TODO Show 'now playing'
                } else if (save) {
                    Util.toast(SelectAlbumActivity.this,
                               getResources().getQuantityString(R.plurals.select_album_n_songs_downloading, songs.size(), songs.size()));
                } else if (append) {
                    Util.toast(SelectAlbumActivity.this,
                               getResources().getQuantityString(R.plurals.select_album_n_songs_added, songs.size(), songs.size()));
                }
            }
        };

        checkLicenseAndTrialPeriod(onValid);
    }

    private void delete() {
        if (Util.getDownloadService(this) != null) {
            Util.getDownloadService(this).delete(getSelectedSongs());
        }
    }

    private void unpin() {
        if (Util.getDownloadService(this) != null) {
            Util.getDownloadService(this).unpin(getSelectedSongs());
        }
    }

    private void playVideo(MusicDirectory.Entry entry) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(MusicServiceFactory.getMusicService(this).getVideoUrl(this, entry.getId())));
        Util.startActivityWithoutTransition(this, intent);
    }

    private void checkLicenseAndTrialPeriod(Runnable onValid) {
        if (licenseValid) {
            onValid.run();
            return;
        }

        int trialDaysLeft = Util.getRemainingTrialDays(this);
        Log.i(TAG, trialDaysLeft + " trial days left.");

        if (trialDaysLeft == 0) {
            showDonationDialog(trialDaysLeft, null);
        } else if (trialDaysLeft < Constants.FREE_TRIAL_DAYS / 2) {
            showDonationDialog(trialDaysLeft, onValid);
        } else {
            Util.toast(this, getResources().getString(R.string.select_album_not_licensed, trialDaysLeft));
            onValid.run();
        }
    }

    private void showDonationDialog(int trialDaysLeft, final Runnable onValid) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_info);

        if (trialDaysLeft == 0) {
            builder.setTitle(R.string.select_album_donate_dialog_0_trial_days_left);
        } else {
            builder.setTitle(getResources().getQuantityString(R.plurals.select_album_donate_dialog_n_trial_days_left,
                                                              trialDaysLeft, trialDaysLeft));
        }

        builder.setMessage(R.string.select_album_donate_dialog_message);

        builder.setPositiveButton(R.string.select_album_donate_dialog_now,
                                  new DialogInterface.OnClickListener() {
                                      @Override
                                      public void onClick(DialogInterface dialogInterface, int i) {
                                          startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.DONATION_URL)));
                                      }
                                  });

        builder.setNegativeButton(R.string.select_album_donate_dialog_later,
                                  new DialogInterface.OnClickListener() {
                                      @Override
                                      public void onClick(DialogInterface dialogInterface, int i) {
                                          dialogInterface.dismiss();
                                          if (onValid != null) {
                                              onValid.run();
                                          }
                                      }
                                  });

        builder.create().show();
    }

    private abstract class LoadTask extends TabActivityBackgroundTask<Pair<MusicDirectory, Boolean>> {

        public LoadTask() {
            super(SelectAlbumActivity.this);
        }

        protected abstract MusicDirectory load(MusicService service) throws Exception;

        @Override
        protected Pair<MusicDirectory, Boolean> doInBackground() throws Throwable {
            MusicService musicService = MusicServiceFactory.getMusicService(SelectAlbumActivity.this);
            MusicDirectory dir = load(musicService);
            boolean valid = musicService.isLicenseValid(SelectAlbumActivity.this, this);
            return new Pair<MusicDirectory, Boolean>(dir, valid);
        }

        @Override
        protected void done(Pair<MusicDirectory, Boolean> result) {
            List<MusicDirectory.Entry> entries = result.getFirst().getChildren();
            entryList.addAll(entries);
            int songCount = 0;
            for (MusicDirectory.Entry entry : entryList) {
                if (!entry.isDirectory()) {
                    songCount++;
                }
            }

            if (songCount > 0) {
                getImageLoader().loadImage(getSupportActionBar(), entryList.get(0));
                mEntryListView.addFooterView(footer);
                selectButton.setVisibility(View.VISIBLE);
                playNowButton.setVisibility(View.VISIBLE);
				playShuffledButton.setVisibility(View.VISIBLE);
                playLastButton.setVisibility(View.VISIBLE);
				pinButton.setVisibility(View.VISIBLE);
				unpinButton.setVisibility(View.VISIBLE);
				deleteButton.setVisibility(View.VISIBLE);
            }

            emptyView.setVisibility(entryList.isEmpty() ? View.VISIBLE : View.GONE);
            invalidateOptionsMenu();
            
            if (mEntryListView.getAdapter() == null) {
            	mEntryListView.setAdapter(new EntryAdapter(SelectAlbumActivity.this, getImageLoader(), entries, true));
            } else {
            	ListAdapter listAdapter = mEntryListView.getAdapter();
            	final EntryAdapter entryListAdapter;
            	if (listAdapter instanceof WrapperListAdapter) {
            		entryListAdapter = (EntryAdapter) ((WrapperListAdapter) listAdapter).getWrappedAdapter();
            	} else if(listAdapter instanceof EntryAdapter) {
            		entryListAdapter = (EntryAdapter) listAdapter;
            	} else {
            		entryListAdapter = null;
            	}
            	
            	if (entryListAdapter != null) {
            		if (mAlbumListFetchOffset == 0) {
            			entryListAdapter.clear();
            		}
            		for (MusicDirectory.Entry entry : entries) {
            			entryListAdapter.add(entry);
            		}
            		runOnUiThread(new Runnable() {
						@Override
						public void run() {
							entryListAdapter.notifyDataSetChanged();
						}
					});
            	}
            }
            licenseValid = result.getSecond();

            boolean playAll = getIntent().getBooleanExtra(Constants.INTENT_EXTRA_NAME_AUTOPLAY, false);
            if (playAll && songCount > 0) {
                playAll(getIntent().getBooleanExtra(Constants.INTENT_EXTRA_NAME_SHUFFLE, false));
            }
        }
    }
}
