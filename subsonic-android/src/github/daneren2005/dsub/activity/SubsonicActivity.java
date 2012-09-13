package github.daneren2005.dsub.activity;

import java.io.File;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.interfaces.Exitable;
import github.daneren2005.dsub.interfaces.Restartable;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.DownloadServiceImpl;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.ImageLoader;
import github.daneren2005.dsub.util.MainOptionsMenuHelper;
import github.daneren2005.dsub.util.ModalBackgroundTask;
import github.daneren2005.dsub.util.SelectServerHelper;
import github.daneren2005.dsub.util.Util;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.View;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

public class SubsonicActivity extends SherlockActivity implements Exitable, Restartable {

    private static final String TAG = SubsonicActivity.class.getSimpleName();

    private static ImageLoader IMAGE_LOADER;
    private static boolean infoDialogDisplayed;

    private boolean destroyed;
    private String theme;

    @Override
    protected void onCreate(Bundle bundle) {
        setUncaughtExceptionHandler();
        applyTheme();
        super.onCreate(bundle);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        startService(new Intent(this, DownloadServiceImpl.class));
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onPostCreate(Bundle bundle) {
        super.onPostCreate(bundle);
        
        loadSettings();
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        setProgressVisible(false);

// TODO: Remove drawer
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
        
//        updateButtonVisibility();

        // Remember the current theme.
        theme = Util.getTheme(this);

        showInfoDialog();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Restart activity if theme has changed.
        if (theme != null && !theme.equals(Util.getTheme(this))) {
            restart();
        }

        loadSettings();
        Util.registerMediaButtonEventReceiver(this);
        
        MainOptionsMenuHelper.registerForServerContextMenu(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MainOptionsMenuHelper.onCreateOptionsMenu(getSupportMenuInflater(), menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return MainOptionsMenuHelper.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return MainOptionsMenuHelper.onOptionsItemSelected(this, item);
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        SelectServerHelper.onCreateContextMenu(this, menu, view, menuInfo);
    }
    
    @Override
    public boolean onContextItemSelected(android.view.MenuItem menuItem) {
        if (SelectServerHelper.onContextItemSelected(this, menuItem)) {
        	return true;
        }
    	return super.onContextItemSelected(menuItem);
    }

    @Override
    public void finish() {
        super.finish();
        Util.disablePendingTransition(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyed = true;
        getImageLoader().clear();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	DownloadService downloadService = Util.getDownloadService(this);
        boolean isVolumeDown = keyCode == KeyEvent.KEYCODE_VOLUME_DOWN;
        boolean isVolumeUp = keyCode == KeyEvent.KEYCODE_VOLUME_UP;
        boolean isVolumeAdjust = isVolumeDown || isVolumeUp;
        boolean isJukebox = downloadService != null && downloadService.isJukeboxEnabled();

        if (isVolumeAdjust && isJukebox) {
            downloadService.adjustJukeboxVolume(isVolumeUp);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void loadSettings() {
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);
        SharedPreferences prefs = Util.getPreferences(this);
        if (!prefs.contains(Constants.PREFERENCES_KEY_CACHE_LOCATION)) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(Constants.PREFERENCES_KEY_CACHE_LOCATION, FileUtil.getDefaultMusicDirectory().getPath());
            editor.commit();
        }
    }

    private void showInfoDialog() {
        if (!infoDialogDisplayed) {
            infoDialogDisplayed = true;
            if (Util.getRestUrl(this, null).contains("demo.subsonic.org")) {
                Util.info(this, R.string.main_welcome_title, R.string.main_welcome_text);
            }
        }
    }

    private void applyTheme() {
        String theme = Util.getTheme(this);
//        if ("dark".equals(theme)) {
//            setTheme(R.style.Theme_Dark);
//        } else 
        if ("light".equals(theme)) {
            setTheme(R.style.Theme_Sherlock_Light);
        }
    }

    public void restart() {
//    	Intent intent = new Intent(getIntent());
//    	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//    	Util.startActivityWithoutTransition(this, intent);
    	returnHome(false);
    }

    public void exit() {
    	returnHome(true);
    }
    
    private void returnHome(boolean shouldExit) {
    	Intent homeIntent = new Intent(this, MainActivity.class);
    	if (shouldExit) {
    		homeIntent.putExtra(Constants.INTENT_EXTRA_NAME_EXIT, true);
    	}
    	homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	Util.startActivityWithoutTransition(this, homeIntent);
    }
    
    public boolean isDestroyed() {
        return destroyed;
    }

    public void setProgressVisible(boolean visible) {
    	setSupportProgressBarIndeterminateVisibility(visible);
    	MainOptionsMenuHelper.setRefreshVisible(!visible);
    	if (!visible) {
        	updateProgress(null);
    	}
    }

    public void updateProgress(String message) {
    	getSupportActionBar().setSubtitle(message);
    }

    protected void warnIfNetworkOrStorageUnavailable() {
        if (!Util.isExternalStoragePresent()) {
            Util.toast(this, R.string.select_album_no_sdcard);
        } else if (!Util.isOffline(this) && !Util.isNetworkConnected(this)) {
            Util.toast(this, R.string.select_album_no_network);
        }
    }

    protected synchronized ImageLoader getImageLoader() {
        if (IMAGE_LOADER == null) {
            IMAGE_LOADER = new ImageLoader(this);
        }
        return IMAGE_LOADER;
    }

    protected void downloadRecursively(final String id, final boolean save, final boolean append, final boolean autoplay, final boolean shuffle) {
        ModalBackgroundTask<List<MusicDirectory.Entry>> task = new ModalBackgroundTask<List<MusicDirectory.Entry>>(this, false) {

            private static final int MAX_SONGS = 500;

            @Override
            protected List<MusicDirectory.Entry> doInBackground() throws Throwable {
                MusicService musicService = MusicServiceFactory.getMusicService(SubsonicActivity.this);
                MusicDirectory root = musicService.getMusicDirectory(id, false, SubsonicActivity.this, this);
                List<MusicDirectory.Entry> songs = new LinkedList<MusicDirectory.Entry>();
                getSongsRecursively(root, songs);
                return songs;
            }

            private void getSongsRecursively(MusicDirectory parent, List<MusicDirectory.Entry> songs) throws Exception {
                if (songs.size() > MAX_SONGS) {
                    return;
                }

                for (MusicDirectory.Entry song : parent.getChildren(false, true)) {
                    if (!song.isVideo()) {
                        songs.add(song);
                    }
                }
                for (MusicDirectory.Entry dir : parent.getChildren(true, false)) {
                    MusicService musicService = MusicServiceFactory.getMusicService(SubsonicActivity.this);
                    getSongsRecursively(musicService.getMusicDirectory(dir.getId(), false, SubsonicActivity.this, this), songs);
                }
            }

            @Override
            protected void done(List<MusicDirectory.Entry> songs) {
                DownloadService downloadService = Util.getDownloadService(SubsonicActivity.this);
                if (!songs.isEmpty() && downloadService != null) {
                    if (!append) {
                        downloadService.clear();
                    }
                    warnIfNetworkOrStorageUnavailable();
                    downloadService.download(songs, save, autoplay, false, shuffle);
//                    showTabActivity(DownloadActivity.class); // TODO: Show 'now playing'
                }
            }
        };

        task.execute();
    }

    private void setUncaughtExceptionHandler() {
        Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
        if (!(handler instanceof SubsonicUncaughtExceptionHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(new SubsonicUncaughtExceptionHandler(this));
        }
    }

    /**
     * Logs the stack trace of uncaught exceptions to a file on the SD card.
     */
    private static class SubsonicUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

        private final Thread.UncaughtExceptionHandler defaultHandler;
        private final Context context;

        private SubsonicUncaughtExceptionHandler(Context context) {
            this.context = context;
            defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        }

        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            File file = null;
            PrintWriter printWriter = null;
            try {

                PackageInfo packageInfo = context.getPackageManager().getPackageInfo("github.daneren2005.dsub", 0);
                file = new File(Environment.getExternalStorageDirectory(), "subsonic-stacktrace.txt");
                printWriter = new PrintWriter(file);
                printWriter.println("Android API level: " + Build.VERSION.SDK_INT);
                printWriter.println("Subsonic version name: " + packageInfo.versionName);
                printWriter.println("Subsonic version code: " + packageInfo.versionCode);
                printWriter.println();
                throwable.printStackTrace(printWriter);
                Log.i(TAG, "Stack trace written to " + file);
            } catch (Throwable x) {
                Log.e(TAG, "Failed to write stack trace to " + file, x);
            } finally {
                Util.close(printWriter);
                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, throwable);
                }

            }
        }
    }
    
// TODO: Remove drawer
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
    
}
