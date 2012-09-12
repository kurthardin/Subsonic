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
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.EntryAdapter;
import github.daneren2005.dsub.util.Pair;
import github.daneren2005.dsub.util.TabFragmentBackgroundTask;
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
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.WrapperListAdapter;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class SelectAlbumFragment extends SubsonicTabFragment {

    private static final String TAG = SelectAlbumFragment.class.getSimpleName();

    private View footer;
    private Button selectButton;
    private Button playNowButton;
	private Button playShuffledButton;
    private Button playLastButton;
    private Button pinButton;
    private Button unpinButton;
    private Button deleteButton;
    private Button moreButton;
    private boolean licenseValid;
    
    private AlbumListType mAlbumListType;
    private int mAlbumListDefaultFetchSize;
    private int mAlbumListDefaultFetchOffset;
    
    private int mAlbumListFetchSize;
    private int mAlbumListFetchOffset;
    
    private boolean mResetScrollPosition;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
        	mAlbumListType = AlbumListType.values()[args.getInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE)];
        	if (mAlbumListType == null) {
        		throw new IllegalArgumentException("Must specify AlbumListType for SelectAlbumFragment in its arguments Bundle with putInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE, AlbumListType.TYPE.ordinal()");
        	}
        	mAlbumListDefaultFetchSize = args.getInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, 20);
        	mAlbumListDefaultFetchOffset = args.getInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET, 0);
        }
        
    }
    
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	return inflater.inflate(R.layout.select_album, container, false);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        setHasOptionsMenu(true);

    	ListView entryListView = getListView();

    	footer = getActivity().getLayoutInflater().inflate(R.layout.select_album_footer, entryListView, false);
    	entryListView.addFooterView(footer);
    	entryListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        registerForContextMenu(entryListView);
        
        View view = getView();
        selectButton = (Button) view.findViewById(R.id.select_album_select);
        selectButton.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View view) {
        		selectAllOrNone();
        	}
        });
        playNowButton = (Button) view.findViewById(R.id.select_album_play_now);
        playNowButton.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View view) {
        		download(false, false, true, false, false);
        		selectAll(false, false);
        	}
        });
        playShuffledButton = (Button) view.findViewById(R.id.select_album_play_shuffled);
        playShuffledButton.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View view) {
        		download(false, false, true, false, true);
        		selectAll(false, false);
        	}
        });
        playLastButton = (Button) view.findViewById(R.id.select_album_play_last);
        playLastButton.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View view) {
        		download(true, false, false, false, false);
        		selectAll(false, false);
        	}
        });
        pinButton = (Button) view.findViewById(R.id.select_album_pin);
        pinButton.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View view) {
        		download(true, true, false, false, false);
        		selectAll(false, false);
        	}
        });
        unpinButton = (Button) view.findViewById(R.id.select_album_unpin);
        unpinButton.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View view) {
        		unpin();
        		selectAll(false, false);
        	}
        });
        deleteButton = (Button) view.findViewById(R.id.select_album_delete);
        deleteButton.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View view) {
        		delete();
        		selectAll(false, false);
        	}
        });
        moreButton = (Button) footer.findViewById(R.id.select_album_more);

    	super.onActivityCreated(savedInstanceState);
    }
    
    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
        if (position >= 0) {
            MusicDirectory.Entry entry = (MusicDirectory.Entry) list.getItemAtPosition(position);
            if (entry.isDirectory()) {
                Intent intent = new Intent(getActivity(), SelectAlbumActivity.class);
                intent.putExtra(Constants.INTENT_EXTRA_NAME_ID, entry.getId());
                intent.putExtra(Constants.INTENT_EXTRA_NAME_NAME, entry.getTitle());
                startActivity(intent);
            } else if (entry.isVideo()) {
                playVideo(entry);
            } else {
                enableButtons();
            }
        }
    }

    public void refresh() {
        enableButtons();
        
    	mAlbumListFetchSize = mAlbumListDefaultFetchSize;
    	mAlbumListFetchOffset = mAlbumListDefaultFetchOffset;
        
        getAlbumList(mAlbumListType.getName(), mAlbumListFetchSize, mAlbumListFetchOffset);
    }

    private void playAll(final boolean shuffle) {
    	//TODO: Fix play all
//        boolean hasSubFolders = false;
//        for (int i = 0; i < mEntryListView.getCount(); i++) {
//            MusicDirectory.Entry entry = (MusicDirectory.Entry) mEntryListView.getItemAtPosition(i);
//            if (entry != null && entry.isDirectory()) {
//                hasSubFolders = true;
//                break;
//            }
//        }
//
//        if (hasSubFolders && mMusicDirectoryFetchId != null) {
//            getMainActivity().downloadRecursively(mMusicDirectoryFetchId, false, false, true, shuffle);
//        } else {
//            selectAll(true, false);
//            download(false, false, true, false, shuffle);
//            selectAll(false, false);
//        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.select_album_options, menu);
        boolean isAlbumList = mAlbumListType != null;//getIntent().hasExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE);
        menu.findItem(R.id.action_play_all).setVisible(isAlbumList || getListView().getCount() == 0 ? false : true);
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
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) menuInfo;

        MusicDirectory.Entry entry = (MusicDirectory.Entry) getListView().getItemAtPosition(info.position);

        if (entry.isDirectory()) {
            android.view.MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.select_album_context, menu);
        } else {
            android.view.MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.select_song_context, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem menuItem) {
    	if (menuItem.getGroupId() == R.id.select_album_context_menu ||
    			menuItem.getGroupId() == R.id.select_song_context_menu) {
    		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
    		MusicDirectory.Entry entry = (MusicDirectory.Entry) getListView().getItemAtPosition(info.position);
    		List<MusicDirectory.Entry> songs = new ArrayList<MusicDirectory.Entry>(10);
    		songs.add(entry);
    		switch (menuItem.getItemId()) {
    		case R.id.album_menu_play_now:
    			getMainActivity().downloadRecursively(entry.getId(), false, false, true, false);
    			break;
    		case R.id.album_menu_play_shuffled:
    			getMainActivity().downloadRecursively(entry.getId(), false, false, true, true);
    			break;
    		case R.id.album_menu_play_last:
    			getMainActivity().downloadRecursively(entry.getId(), false, true, false, false);
    			break;
    		case R.id.album_menu_pin:
    			getMainActivity().downloadRecursively(entry.getId(), true, true, false, false);
    			break;
    		case R.id.song_menu_play_now:
    			Util.getDownloadService(getActivity()).download(songs, false, true, true, false);
    			break;
    		case R.id.song_menu_play_next:
    			Util.getDownloadService(getActivity()).download(songs, false, false, true, false);
    			break;
    		case R.id.song_menu_play_last:
    			Util.getDownloadService(getActivity()).download(songs, false, false, false, false);
    			break;
    		default:
    			return super.onContextItemSelected(menuItem);
    		}
    		return true;
    	} else {
    		return super.onContextItemSelected(menuItem);
    	}
    }
    
    public AlbumListType getAlbumListType() {
    	return mAlbumListType;
    }

    private void getAlbumList(final String albumListFetchType, final int albumListFetchSize, final int albumListFetchOffset) {
        new LoadTask() {
            @Override
            protected MusicDirectory load(MusicService service) throws Exception {
                return service.getAlbumList(albumListFetchType, albumListFetchSize, albumListFetchOffset, getActivity(), this);
            }

            @Override
            protected void done(Pair<MusicDirectory, Boolean> result) {
                if (!result.getFirst().getChildren().isEmpty()) {
                    moreButton.setVisibility(View.VISIBLE);

                    moreButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                        	mAlbumListFetchOffset = getListView().getCount();
                        	mAlbumListFetchSize = mAlbumListFetchOffset;
                        	getAlbumList(mAlbumListType.getName(), mAlbumListFetchSize, mAlbumListFetchOffset);
                        }
                    });
                }
                super.done(result);
            }
        }.execute();
    }

    private void selectAllOrNone() {
    	ListView entryListView = getListView();
        boolean someUnselected = false;
        int count = entryListView.getCount();
        for (int i = 0; i < count; i++) {
            if (!entryListView.isItemChecked(i) && entryListView.getItemAtPosition(i) instanceof MusicDirectory.Entry) {
                someUnselected = true;
                break;
            }
        }
        selectAll(someUnselected, true);
    }

    private void selectAll(boolean selected, boolean toast) {
    	ListView entryListView = getListView();
        int count = entryListView.getCount();
        int selectedCount = 0;
        for (int i = 0; i < count; i++) {
            MusicDirectory.Entry entry = (MusicDirectory.Entry) entryListView.getItemAtPosition(i);
            if (entry != null && !entry.isDirectory() && !entry.isVideo()) {
                entryListView.setItemChecked(i, selected);
                selectedCount++;
            }
        }

        // Display toast: N tracks selected / N tracks unselected
        if (toast) {
            int toastResId = selected ? R.string.select_album_n_selected
                                      : R.string.select_album_n_unselected;
            Util.toast(getActivity(), getString(toastResId, selectedCount));
        }

        enableButtons();
    }

    private void enableButtons() {
        if (Util.getDownloadService(getActivity()) == null) {
            return;
        }

        List<MusicDirectory.Entry> selection = getSelectedSongs();
        boolean enabled = !selection.isEmpty();
        boolean unpinEnabled = false;
        boolean deleteEnabled = false;

        for (MusicDirectory.Entry song : selection) {
            DownloadFile downloadFile = Util.getDownloadService(getActivity()).forSong(song);
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
        pinButton.setEnabled(enabled && !Util.isOffline(getActivity()));
        unpinButton.setEnabled(unpinEnabled);
        deleteButton.setEnabled(deleteEnabled);
    }

    private List<MusicDirectory.Entry> getSelectedSongs() {
    	ListView entryListView = getListView();
        List<MusicDirectory.Entry> songs = new ArrayList<MusicDirectory.Entry>(10);
        int count = entryListView.getCount();
        for (int i = 0; i < count; i++) {
            if (entryListView.isItemChecked(i)) {
                songs.add((MusicDirectory.Entry) entryListView.getItemAtPosition(i));
            }
        }
        return songs;
    }

    private void download(final boolean append, final boolean save, final boolean autoplay, final boolean playNext, final boolean shuffle) {
        if (Util.getDownloadService(getActivity()) == null) {
            return;
        }

        final List<MusicDirectory.Entry> songs = getSelectedSongs();
        Runnable onValid = new Runnable() {
            @Override
            public void run() {
                if (!append) {
                	Util.getDownloadService(getActivity()).clear();
                }

                getMainActivity().warnIfNetworkOrStorageUnavailable();
                Util.getDownloadService(getActivity()).download(songs, save, autoplay, playNext, shuffle);
                if (autoplay) {
//                    showTabActivity(DownloadActivity.class); // TODO Show 'now playing'
                } else if (save) {
                    Util.toast(getActivity(),
                               getResources().getQuantityString(R.plurals.select_album_n_songs_downloading, songs.size(), songs.size()));
                } else if (append) {
                    Util.toast(getActivity(),
                               getResources().getQuantityString(R.plurals.select_album_n_songs_added, songs.size(), songs.size()));
                }
            }
        };

        checkLicenseAndTrialPeriod(onValid);
    }

    private void delete() {
        if (Util.getDownloadService(getActivity()) != null) {
        	Util.getDownloadService(getActivity()).delete(getSelectedSongs());
        }
    }

    private void unpin() {
        if (Util.getDownloadService(getActivity()) != null) {
        	Util.getDownloadService(getActivity()).unpin(getSelectedSongs());
        }
    }

    private void playVideo(MusicDirectory.Entry entry) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(MusicServiceFactory.getMusicService(getActivity()).getVideoUrl(getActivity(), entry.getId())));
        startActivity(intent);
    }

    private void checkLicenseAndTrialPeriod(Runnable onValid) {
        if (licenseValid) {
            onValid.run();
            return;
        }

        int trialDaysLeft = Util.getRemainingTrialDays(getActivity());
        Log.i(TAG, trialDaysLeft + " trial days left.");

        if (trialDaysLeft == 0) {
            showDonationDialog(trialDaysLeft, null);
        } else if (trialDaysLeft < Constants.FREE_TRIAL_DAYS / 2) {
            showDonationDialog(trialDaysLeft, onValid);
        } else {
            Util.toast(getActivity(), getResources().getString(R.string.select_album_not_licensed, trialDaysLeft));
            onValid.run();
        }
    }

    private void showDonationDialog(int trialDaysLeft, final Runnable onValid) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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

    private abstract class LoadTask extends TabFragmentBackgroundTask<Pair<MusicDirectory, Boolean>> {

        public LoadTask() {
            super(SelectAlbumFragment.this);
        }

        protected abstract MusicDirectory load(MusicService service) throws Exception;

        @Override
        protected Pair<MusicDirectory, Boolean> doInBackground() throws Throwable {
            MusicService musicService = MusicServiceFactory.getMusicService(getActivity());
            MusicDirectory dir = load(musicService);
            boolean valid = musicService.isLicenseValid(getActivity(), this);
            return new Pair<MusicDirectory, Boolean>(dir, valid);
        }

        @Override
        protected void done(Pair<MusicDirectory, Boolean> result) {
            updateProgress(getString(R.string.select_album_empty));
            List<MusicDirectory.Entry> entries = result.getFirst().getChildren();
            
            final EntryAdapter entryListAdapter;
            if (getListAdapter() == null) {
            	entryListAdapter = new EntryAdapter(getActivity(), getMainActivity().getImageLoader(), entries, true);
            	setListAdapter(entryListAdapter);
            } else {
            	ListAdapter listAdapter = getListAdapter();
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
            		getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							entryListAdapter.notifyDataSetChanged();
						}
					});
            	}
            }
			
            int songCount = 0;
            for (int i = 0; i < entryListAdapter.getCount(); i++) {
            	MusicDirectory.Entry entry = entryListAdapter.getItem(i);
                if (!entry.isDirectory()) {
                    songCount++;
                }
            }

			if (mResetScrollPosition) {
				setSelection(0);
				mResetScrollPosition = false;
			}

            if (songCount > 0) {
            	if (selectButton.getVisibility() == View.GONE) {
            		selectButton.setVisibility(View.VISIBLE);
            		playNowButton.setVisibility(View.VISIBLE);
            		playShuffledButton.setVisibility(View.VISIBLE);
            		playLastButton.setVisibility(View.VISIBLE);
            		pinButton.setVisibility(View.VISIBLE);
            		unpinButton.setVisibility(View.VISIBLE);
            		deleteButton.setVisibility(View.VISIBLE);
            	}
            } else if (selectButton.getVisibility() == View.VISIBLE) {
                selectButton.setVisibility(View.GONE);
                playNowButton.setVisibility(View.GONE);
				playShuffledButton.setVisibility(View.GONE);
                playLastButton.setVisibility(View.GONE);
				pinButton.setVisibility(View.GONE);
				unpinButton.setVisibility(View.GONE);
				deleteButton.setVisibility(View.GONE);
            }

            getMainActivity().invalidateOptionsMenu();
            
            licenseValid = result.getSecond();

// TODO: Handle AUTOPLAY and SHUFFLE
//            boolean playAll = getIntent().getBooleanExtra(Constants.INTENT_EXTRA_NAME_AUTOPLAY, false);
//            if (playAll && songCount > 0) {
//                playAll(getIntent().getBooleanExtra(Constants.INTENT_EXTRA_NAME_SHUFFLE, false));
//            }
        }
    }
    
    public enum AlbumListType {
    	FREQUENT("frequent"), 
    	HIGHEST("highest"), 
    	NEWEST("newest"), 
    	RANDOM("random"), 
    	RECENT("recent");
    	
    	private final String mName;
    	
    	private AlbumListType(String name) {
    		mName = name;
    	}
    	
    	public String getName() {
    		return mName;
    	}
    }
}
