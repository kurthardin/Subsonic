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
import github.daneren2005.dsub.service.DownloadServiceImpl;
import github.daneren2005.dsub.util.NowPlayingHelper;
import github.daneren2005.dsub.util.SimpleServiceBinder;
import github.daneren2005.dsub.view.NowPlayingView;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

/**
 * @author Kurt Hardin
 */
public abstract class SubsonicTabActivity extends SubsonicActivity {
	
	protected DownloadServiceImpl mDownloadService;

	private final ServiceConnection mConnection = new ServiceConnection() {
		@SuppressWarnings("unchecked")
		public void onServiceConnected(ComponentName className, IBinder service) {
			mDownloadService = ((SimpleServiceBinder<DownloadServiceImpl>) service).getService();
			NowPlayingHelper.onResume(mDownloadService, getNowPlayingView());
		}

		public void onServiceDisconnected(ComponentName className) {
			mDownloadService = null;
			if (getNowPlayingView() != null) {
				getNowPlayingView().onCurrentSongChanged(null, null);
			}
		}
	};
	
	private NowPlayingView mNowPlayingView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// bind to our database using our Service Connection
		Intent serviceIntent = new Intent(this, DownloadServiceImpl.class);
		bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onResume() {
		super.onResume();
		NowPlayingHelper.onResume(mDownloadService, getNowPlayingView());
	}

	@Override
	public void onPause() {
		super.onPause();
		NowPlayingHelper.onPause(mDownloadService);
	}
	
	@Override
	public void onDestroy() {
		unbindService(mConnection);
		mDownloadService = null;
		super.onDestroy();
	}
	
	private NowPlayingView getNowPlayingView() {
		if (mNowPlayingView == null) {
			mNowPlayingView = (NowPlayingView) findViewById(R.id.now_playing_view);
		}
		return mNowPlayingView;
	}

}

// Previous implementation...
///**
// * @author Sindre Mehus
// */
//public abstract class SubsonicTabActivity extends SubsonicActivity implements IDrawerCallbacks {
//
//    private static final String TAG = SubsonicTabActivity.class.getSimpleName();
//    private static ImageLoader IMAGE_LOADER;
//
//    private boolean destroyed;
//
//    private DrawerGarment mDrawer;
//    private Intent mOnDrawerClosedIntent;
//    private boolean isRefreshVisible;
//    private String theme;
//
//    private static boolean infoDialogDisplayed;
//
//    @Override
//    protected void onCreate(Bundle bundle) {
//        setUncaughtExceptionHandler();
//        applyTheme();
//        super.onCreate(bundle);
//        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
//        startService(new Intent(this, DownloadServiceImpl.class));
//        setVolumeControlStream(AudioManager.STREAM_MUSIC);
//    }
//
//    @Override
//    protected void onPostCreate(Bundle bundle) {
//        super.onPostCreate(bundle);
//        
//        loadSettings();
//        
//        ActionBar actionBar = getSupportActionBar();
//        actionBar.setDisplayHomeAsUpEnabled(true);
//        
//        setProgressVisible(false);
//
//        mDrawer = new DrawerGarment(this, R.layout.dashboard);
//        mDrawer.setSlideTarget(DrawerGarment.SLIDE_TARGET_WINDOW);
//        mDrawer.setDrawerCallbacks(this);
//
//        final Spinner drawerServerSpinner = (Spinner) mDrawer.findViewById(R.id.dashboard_server_spinner);
//        drawerServerSpinner.setAdapter(new ServerSpinnerAdapter());
//        drawerServerSpinner.setSelection(Util.getActiveServer(this));
//        drawerServerSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
//
//        	@Override
//        	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//        		setActiveServer((int) id);
//        		// Refresh activity
//        		SubsonicTabActivity.this.refresh();
//        	}
//
//        	@Override
//        	public void onNothingSelected(AdapterView<?> parent) {
//        		// Do nothing
//        	}
//        });
//
//        final View nowPlayingButton = mDrawer.findViewById(R.id.dashboard_now_playing);
//        nowPlayingButton.setOnClickListener(new View.OnClickListener() {
//        	@Override
//        	public void onClick(View view) {
//        		showTabActivity(DownloadActivity.class);
//        	}
//        });
//
//        final View artistsButton = mDrawer.findViewById(R.id.dashboard_artists);
//        artistsButton.setOnClickListener(new View.OnClickListener() {
//        	@Override
//        	public void onClick(View view) {
//        		showTabActivity(SelectArtistActivity.class);
//        	}
//        });
//
//        final View drawerAlbumsFrequentButton = mDrawer.findViewById(R.id.dashboard_albums_frequent);
//        drawerAlbumsFrequentButton.setOnClickListener(new OnClickListener() {
//        	@Override
//        	public void onClick(View v) {
//        		showAlbumList("frequent");
//        	}
//        });
//
//        final View drawerAlbumsHighestButton = mDrawer.findViewById(R.id.dashboard_albums_highest);
//        drawerAlbumsHighestButton.setOnClickListener(new OnClickListener() {
//        	@Override
//        	public void onClick(View v) {
//        		showAlbumList("highest");
//        	}
//        });
//
//        final View drawerAlbumsNewestButton = mDrawer.findViewById(R.id.dashboard_albums_newest);
//        drawerAlbumsNewestButton.setOnClickListener(new OnClickListener() {
//        	@Override
//        	public void onClick(View v) {
//        		showAlbumList("newest");
//        	}
//        });
//
//        final View drawerAlbumsRandomButton = mDrawer.findViewById(R.id.dashboard_albums_random);
//        drawerAlbumsRandomButton.setOnClickListener(new OnClickListener() {
//        	@Override
//        	public void onClick(View v) {
//        		showAlbumList("random");
//        	}
//        });
//
//        final View drawerAlbumsRecentButton = mDrawer.findViewById(R.id.dashboard_albums_recent);
//        drawerAlbumsRecentButton.setOnClickListener(new OnClickListener() {
//        	@Override
//        	public void onClick(View v) {
//        		showAlbumList("recent");
//        	}
//        });
//
//        final View playlistsButton = mDrawer.findViewById(R.id.dashboard_playlists);
//        playlistsButton.setOnClickListener(new View.OnClickListener() {
//        	@Override
//        	public void onClick(View view) {
//        		showTabActivity(SelectPlaylistActivity.class);
//        	}
//        });
//
//        final View helpButton = mDrawer.findViewById(R.id.dashboard_help);
//        helpButton.setOnClickListener(new View.OnClickListener() {
//        	@Override
//        	public void onClick(View view) {
//        		startActivity(HelpActivity.class);
//        	}
//        });
//
//        final View settingsButton = mDrawer.findViewById(R.id.dashboard_settings);
//        settingsButton.setOnClickListener(new View.OnClickListener() {
//        	@Override
//        	public void onClick(View view) {
//        		startActivity(SettingsActivity.class);
//        	}
//        });
//        
////        updateButtonVisibility();
//
//        // Remember the current theme.
//        theme = Util.getTheme(this);
//
//        showInfoDialog();
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//
//        // Restart activity if theme has changed.
//        if (theme != null && !theme.equals(Util.getTheme(this))) {
//            restart();
//        }
//
//        loadSettings();
//        Util.registerMediaButtonEventReceiver(this);
//    }
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater inflater = getSupportMenuInflater();
//        inflater.inflate(R.menu.main, menu);
//        
//        MenuItem refreshItem = menu.findItem(R.id.action_refresh);
//        if (refreshItem != null) {
//        	refreshItem.setVisible(isRefreshVisible);
//        }
//        
//        return super.onCreateOptionsMenu(menu);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//        
//	        case android.R.id.home:
//	        	mDrawer.toggleDrawer();
//	        	return true;
//	        	
//	        case R.id.action_search:
//	        	Intent searchIntent = new Intent(SubsonicTabActivity.this, SearchActivity.class);
//            	searchIntent.putExtra(Constants.INTENT_EXTRA_REQUEST_SEARCH, true);
//                showTabActivity(SearchActivity.class, searchIntent);
//                return true;
//
//        }
//
//        return false;
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        destroyed = true;
//        getImageLoader().clear();
//    }
//
//
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        boolean isVolumeDown = keyCode == KeyEvent.KEYCODE_VOLUME_DOWN;
//        boolean isVolumeUp = keyCode == KeyEvent.KEYCODE_VOLUME_UP;
//        boolean isVolumeAdjust = isVolumeDown || isVolumeUp;
//        boolean isJukebox = getDownloadService() != null && getDownloadService().isJukeboxEnabled();
//
//        if (isVolumeAdjust && isJukebox) {
//            getDownloadService().adjustJukeboxVolume(isVolumeUp);
//            return true;
//        }
//        return super.onKeyDown(keyCode, event);
//    }
//    
//    @Override
//    public void onBackPressed() {
//    	if (mDrawer.isDrawerOpened()) {
//    		mDrawer.closeDrawer();
//    	} else {
//    		super.onBackPressed();
//    	}
//    }
//
//    @Override
//    public void finish() {
//        super.finish();
//        Util.disablePendingTransition(this);
//    }
//
////    @Override
////    public void setTitle(CharSequence title) {
////        super.setTitle(title);
////
////        // Set the font of title in the action bar.
////        TextView text = (TextView) findViewById(R.id.actionbar_title_text);
////        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Storopia.ttf");
////        text.setTypeface(typeface);
////
////        text.setText(title);
////    }
//
//    @Override
//    public void setTitle(int titleId) {
//        setTitle(getString(titleId));
//    }
//
//    private void loadSettings() {
//        PreferenceManager.setDefaultValues(this, R.xml.settings, false);
//        SharedPreferences prefs = Util.getPreferences(this);
//        if (!prefs.contains(Constants.PREFERENCES_KEY_CACHE_LOCATION)) {
//            SharedPreferences.Editor editor = prefs.edit();
//            editor.putString(Constants.PREFERENCES_KEY_CACHE_LOCATION, FileUtil.getDefaultMusicDirectory().getPath());
//            editor.commit();
//        }
//    }
//
//    private void showInfoDialog() {
//        if (!infoDialogDisplayed) {
//            infoDialogDisplayed = true;
//            if (Util.getRestUrl(this, null).contains("demo.subsonic.org")) {
//                Util.info(this, R.string.main_welcome_title, R.string.main_welcome_text);
//            }
//        }
//    }
//
//    private void applyTheme() {
//        String theme = Util.getTheme(this);
////        if ("dark".equals(theme)) {
////            setTheme(R.style.Theme_Dark);
////        } else 
//        if ("light".equals(theme)) {
//            setTheme(R.style.Theme_Sherlock_Light);
//        }
//    }
//
//    private void showAlbumList(String type) {		
//        Intent intent = new Intent(this, SelectAlbumActivity.class);
//        intent.putExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE, type);
//        intent.putExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, 20);
//        intent.putExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET, 0);
//        showTabActivity(SelectAlbumActivity.class, intent);
//	}
//    
//    public void showTabActivity(Class<? extends SubsonicTabActivity> activityClass) {
//        Intent intent = new Intent(this, activityClass);
//        showTabActivity(activityClass, intent);
//    }
//    
//    public void showTabActivity(Class<? extends SubsonicTabActivity> activityClass, Intent intent) {
//    	boolean isIdenticalActivity = (activityClass == this.getClass());
//    	if (this instanceof SelectAlbumActivity && activityClass == SelectAlbumActivity.class) {
//    		SelectAlbumActivity selectAlbumActivity = (SelectAlbumActivity) this;
//    		String oldAlbumListType = selectAlbumActivity.getAlbumListType();
//    		if (oldAlbumListType != null) {
//    			String newAlbumListType = intent.getStringExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE);
//    			isIdenticalActivity &= (oldAlbumListType.compareTo(newAlbumListType) == 0);
//    		} else {
//    			isIdenticalActivity = false;
//    		}
//    	}
//    	
//    	if (isIdenticalActivity) {
//    		refresh();
//        	mDrawer.closeDrawer(true);
//    	} else {
//    		startActivityWithDrawerCheck(intent);
//    	}
//    }
//    
//    public void startActivity(Class<? extends Activity> activityClass) {
//    	startActivityWithDrawerCheck(new Intent(this, activityClass));
//    }
//    
//    @Override
//    public void startActivity(Intent intent) {
//    	startActivityWithDrawerCheck(intent);
//    }
//    
//    private void startActivityWithDrawerCheck(Intent intent) {
//    	
//    	if (mDrawer != null && mDrawer.isDrawerOpened()) {
//			mOnDrawerClosedIntent = intent;
//	    	mDrawer.closeDrawer(true);
//		} else {
//			performStartActivity(intent);
//		}
//    }
//    
//    private void performStartActivity(Intent intent) {
//    	super.startActivity(intent);
//    	mOnDrawerClosedIntent = null;
//    }
//
//    private void setActiveServer(int instance) {
//        if (Util.getActiveServer(this) != instance) {
//            DownloadService service = getDownloadService();
//            if (service != null) {
//                service.clearIncomplete();
//            }
//            Util.setActiveServer(this, instance);
//        }
//    }
//    
//    protected abstract void refresh();
//
//    private void restart() {
//    	Intent intent = new Intent(getIntent());
//    	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//    	startActivity(intent);
//    }
//
//    private void exit() {
//        stopService(new Intent(this, DownloadServiceImpl.class));
//        finish();
//    }
//    
//    public boolean isDestroyed() {
//        return destroyed;
//    }
//
////    private void updateButtonVisibility() {
////        int visibility = Util.isOffline(this) ? View.GONE : View.VISIBLE;
////    }
//
//    public void setProgressVisible(boolean visible) {
//    	setSupportProgressBarIndeterminateVisibility(visible);
//    	isRefreshVisible = !visible;
//    	invalidateOptionsMenu();
//    	if (!visible) {
//        	updateProgress(null);
//    	}
//    }
//
//    public void updateProgress(String message) {
//    	getSupportActionBar().setSubtitle(message);
//    }
//    
//    public void onDrawerOpened() {
//    	// Do nothing...
//    }
//
//    public void onDrawerClosed() {
//    	if (mOnDrawerClosedIntent != null) {
//    		performStartActivity(mOnDrawerClosedIntent);
//    	}
//    }
//
//    public DownloadService getDownloadService() {
//        // If service is not available, request it to start and wait for it.
//        for (int i = 0; i < 5; i++) {
//            DownloadService downloadService = DownloadServiceImpl.getInstance();
//            if (downloadService != null) {
//                return downloadService;
//            }
//            Log.w(TAG, "DownloadService not running. Attempting to start it.");
//            startService(new Intent(this, DownloadServiceImpl.class));
//            Util.sleepQuietly(50L);
//        }
//        return DownloadServiceImpl.getInstance();
//    }
//
//    protected void warnIfNetworkOrStorageUnavailable() {
//        if (!Util.isExternalStoragePresent()) {
//            Util.toast(this, R.string.select_album_no_sdcard);
//        } else if (!Util.isOffline(this) && !Util.isNetworkConnected(this)) {
//            Util.toast(this, R.string.select_album_no_network);
//        }
//    }
//
//    protected synchronized ImageLoader getImageLoader() {
//        if (IMAGE_LOADER == null) {
//            IMAGE_LOADER = new ImageLoader(this);
//        }
//        return IMAGE_LOADER;
//    }
//
//    protected void downloadRecursively(final String id, final boolean save, final boolean append, final boolean autoplay, final boolean shuffle) {
//        ModalBackgroundTask<List<MusicDirectory.Entry>> task = new ModalBackgroundTask<List<MusicDirectory.Entry>>(this, false) {
//
//            private static final int MAX_SONGS = 500;
//
//            @Override
//            protected List<MusicDirectory.Entry> doInBackground() throws Throwable {
//                MusicService musicService = MusicServiceFactory.getMusicService(SubsonicTabActivity.this);
//                MusicDirectory root = musicService.getMusicDirectory(id, false, SubsonicTabActivity.this, this);
//                List<MusicDirectory.Entry> songs = new LinkedList<MusicDirectory.Entry>();
//                getSongsRecursively(root, songs);
//                return songs;
//            }
//
//            private void getSongsRecursively(MusicDirectory parent, List<MusicDirectory.Entry> songs) throws Exception {
//                if (songs.size() > MAX_SONGS) {
//                    return;
//                }
//
//                for (MusicDirectory.Entry song : parent.getChildren(false, true)) {
//                    if (!song.isVideo()) {
//                        songs.add(song);
//                    }
//                }
//                for (MusicDirectory.Entry dir : parent.getChildren(true, false)) {
//                    MusicService musicService = MusicServiceFactory.getMusicService(SubsonicTabActivity.this);
//                    getSongsRecursively(musicService.getMusicDirectory(dir.getId(), false, SubsonicTabActivity.this, this), songs);
//                }
//            }
//
//            @Override
//            protected void done(List<MusicDirectory.Entry> songs) {
//                DownloadService downloadService = getDownloadService();
//                if (!songs.isEmpty() && downloadService != null) {
//                    if (!append) {
//                        downloadService.clear();
//                    }
//                    warnIfNetworkOrStorageUnavailable();
//                    downloadService.download(songs, save, autoplay, false, shuffle);
//                    showTabActivity(DownloadActivity.class);
//                }
//            }
//        };
//
//        task.execute();
//    }
//
//    private void setUncaughtExceptionHandler() {
//        Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
//        if (!(handler instanceof SubsonicUncaughtExceptionHandler)) {
//            Thread.setDefaultUncaughtExceptionHandler(new SubsonicUncaughtExceptionHandler(this));
//        }
//    }
//
//    /**
//     * Logs the stack trace of uncaught exceptions to a file on the SD card.
//     */
//    private static class SubsonicUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
//
//        private final Thread.UncaughtExceptionHandler defaultHandler;
//        private final Context context;
//
//        private SubsonicUncaughtExceptionHandler(Context context) {
//            this.context = context;
//            defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
//        }
//
//        @Override
//        public void uncaughtException(Thread thread, Throwable throwable) {
//            File file = null;
//            PrintWriter printWriter = null;
//            try {
//
//                PackageInfo packageInfo = context.getPackageManager().getPackageInfo("github.daneren2005.dsub", 0);
//                file = new File(Environment.getExternalStorageDirectory(), "subsonic-stacktrace.txt");
//                printWriter = new PrintWriter(file);
//                printWriter.println("Android API level: " + Build.VERSION.SDK_INT);
//                printWriter.println("Subsonic version name: " + packageInfo.versionName);
//                printWriter.println("Subsonic version code: " + packageInfo.versionCode);
//                printWriter.println();
//                throwable.printStackTrace(printWriter);
//                Log.i(TAG, "Stack trace written to " + file);
//            } catch (Throwable x) {
//                Log.e(TAG, "Failed to write stack trace to " + file, x);
//            } finally {
//                Util.close(printWriter);
//                if (defaultHandler != null) {
//                    defaultHandler.uncaughtException(thread, throwable);
//                }
//
//            }
//        }
//    }
//    
//    private class ServerSpinnerAdapter extends BaseAdapter {
//
//		@Override
//		public int getCount() {
//			return 4;
//		}
//
//		@Override
//		public String getItem(int position) {
//			return Util.getServerName(getBaseContext(), position);
//		}
//
//		@Override
//		public long getItemId(int position) {
//			return position;
//		}
//
//		@Override
//		public View getDropDownView(int position, View convertView, ViewGroup parent) {
//			return getView(position, convertView, parent, R.layout.simple_spinner_dropdown_item);
//		}
//
//		@Override
//		public View getView(int position, View convertView, ViewGroup parent) {
//			return getView(position, convertView, parent, R.layout.simple_spinner_item);
//		}
//		
//		public View getView(int position, View convertView, ViewGroup parent, int layoutResId) {
//			TextView v = (TextView) convertView;
//		    if (v == null) {
//		        LayoutInflater vi = (LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//		        v = (TextView) vi.inflate(layoutResId, null);
//		    }
//		    v.setText(getItem(position));
//			return v;
//		}
//    	
//    }
//}

