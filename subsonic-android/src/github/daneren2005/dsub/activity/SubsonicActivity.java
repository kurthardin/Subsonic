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
import github.daneren2005.dsub.service.OfflineException;
import github.daneren2005.dsub.service.ServerTooOldException;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.ImageLoader;
import github.daneren2005.dsub.util.ModalBackgroundTask;
import github.daneren2005.dsub.util.SilentBackgroundTask;
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
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

public class SubsonicActivity extends SherlockFragmentActivity implements Exitable, Restartable {

    private static final String TAG = SubsonicActivity.class.getSimpleName();
	
    private static final int MENU_ITEM_SERVER_1 = 101;
    private static final int MENU_ITEM_SERVER_2 = 102;
    private static final int MENU_ITEM_SERVER_3 = 103;
    private static final int MENU_ITEM_OFFLINE = 104;

    private static ImageLoader IMAGE_LOADER;
    private static boolean infoDialogDisplayed;

    private boolean destroyed;
    private String theme;

	private static Menu mCurrentMenu;
	
	private static View mServerContextView;
	private static boolean mIsRefreshVisible;

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
        
        mServerContextView = new View(this);
    	mServerContextView.setVisibility(View.GONE);
    	registerForContextMenu(mServerContextView);
    	ViewGroup contentView = (ViewGroup) getWindow().getDecorView().findViewById(android.R.id.content);
    	contentView.addView(mServerContextView, 0);
        
        loadSettings();
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        setProgressVisible(false);

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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mCurrentMenu = menu;
    	getSupportMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	MenuItem refreshItem = menu.findItem(R.id.action_refresh);
        if (refreshItem != null) {
        	refreshItem.setVisible(mIsRefreshVisible);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
        
        case android.R.id.home:
        	Intent homeIntent = new Intent(this, MainActivity.class);
        	homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        	Util.startActivityWithoutTransition(this, homeIntent);
        	break;
        	
        case R.id.action_search:
        	Intent searchIntent = new Intent(this, SearchActivity.class);
        	searchIntent.putExtra(Constants.INTENT_EXTRA_REQUEST_SEARCH, true);
            Util.startActivityWithoutTransition(this, searchIntent);
            break;
            
        case R.id.menu_help:
        	startActivity(new Intent(this, HelpActivity.class));
        	break;
            
        case R.id.menu_server:
        	mServerContextView.showContextMenu();
        	break;
        	
        case R.id.menu_settings:
        	startActivity(new Intent(this, SettingsActivity.class));
        	break;
        	
        case R.id.menu_exit:
        	exit();
        	break;
        	
        default:
        	return false;

    }

    return true;
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        if (view.equals(mServerContextView)) {
    		android.view.MenuItem menuItem1 = menu.add(R.id.select_server_context_menu, MENU_ITEM_SERVER_1, MENU_ITEM_SERVER_1, Util.getServerName(this, 1));
    		android.view.MenuItem menuItem2 = menu.add(R.id.select_server_context_menu, MENU_ITEM_SERVER_2, MENU_ITEM_SERVER_2, Util.getServerName(this, 2));
    		android.view.MenuItem menuItem3 = menu.add(R.id.select_server_context_menu, MENU_ITEM_SERVER_3, MENU_ITEM_SERVER_3, Util.getServerName(this, 3));
    		android.view.MenuItem menuItem4 = menu.add(R.id.select_server_context_menu, MENU_ITEM_OFFLINE, MENU_ITEM_OFFLINE, Util.getServerName(this, 0));
    		menu.setGroupCheckable(R.id.select_server_context_menu, true, true);
    		menu.setHeaderTitle(R.string.main_select_server);

    		switch (Util.getActiveServer(this)) {
    			case 0:
    				menuItem4.setChecked(true);
    				break;
    			case 1:
    				menuItem1.setChecked(true);
    				break;
    			case 2:
    				menuItem2.setChecked(true);
    				break;
    			case 3:
    				menuItem3.setChecked(true);
    				break;
    		}
    	}
    }
    
    @Override
    public boolean onContextItemSelected(android.view.MenuItem menuItem) {
    	if (menuItem.getGroupId() == R.id.select_server_context_menu) {
    		switch (menuItem.getItemId()) {
    		case MENU_ITEM_OFFLINE:
    			setActiveServer(0);
    			break;
    		case MENU_ITEM_SERVER_1:
    			setActiveServer(1);
    			break;
    		case MENU_ITEM_SERVER_2:
    			setActiveServer(2);
    			break;
    		case MENU_ITEM_SERVER_3:
    			setActiveServer(3);
    			break;
    		default:
    			return false;
    		}

    		restart();
    		return true;
    	} else {
    		return false;
        }
    }

    private void setActiveServer(int instance) {
        if (Util.getActiveServer(this) != instance) {
            DownloadService service = Util.getDownloadService(this);
            if (service != null) {
                service.clearIncomplete();
            }
            Util.setActiveServer(this, instance);
        }
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
            setTheme(R.style.Theme_DSub_Light);
        }
    }
	
    public void toggleStarredInBackground(final MusicDirectory.Entry entry, final ImageButton button) {
        
    	final boolean starred = !entry.isStarred();
    	
    	button.setImageResource(starred ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
    	entry.setStarred(starred);
    	
        //        Util.toast(SubsonicTabActivity.this, getResources().getString(R.string.starring_content, entry.getTitle()));
        new SilentBackgroundTask<Void>(this) {
            @Override
            protected Void doInBackground() throws Throwable {
                MusicService musicService = MusicServiceFactory.getMusicService(SubsonicActivity.this);
				musicService.setStarred(entry.getId(), starred, SubsonicActivity.this, null);
                return null;
            }
            
            @Override
            protected void done(Void result) {
                //                Util.toast(SubsonicTabActivity.this, getResources().getString(R.string.starring_content_done, entry.getTitle()));
            }
            
            @Override
            protected void error(Throwable error) {
            	button.setImageResource(!starred ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
            	entry.setStarred(!starred);
            	
            	String msg;
            	if (error instanceof OfflineException || error instanceof ServerTooOldException) {
            		msg = getErrorMessage(error);
            	} else {
            		msg = getResources().getString(R.string.starring_content_error, entry.getTitle()) + " " + getErrorMessage(error);
            	}
            	
        		Util.toast(SubsonicActivity.this, msg, false);
            }
        }.execute();
    }

    public void restart() {
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

    	mIsRefreshVisible = !visible;
    	if (mCurrentMenu != null) {
    		MenuItem refreshItem = mCurrentMenu.findItem(R.id.action_refresh);
    		if (refreshItem != null) {
    			refreshItem.setVisible(mIsRefreshVisible);
    		}
    	}
    	
    	if (!visible) {
        	updateProgress(null);
    	}
    }

    public void updateProgress(String message) {
    	getSupportActionBar().setSubtitle(message);
    }

    public void warnIfNetworkOrStorageUnavailable() {
        if (!Util.isExternalStoragePresent()) {
            Util.toast(this, R.string.select_album_no_sdcard);
        } else if (!Util.isOffline(this) && !Util.isNetworkConnected(this)) {
            Util.toast(this, R.string.select_album_no_network);
        }
    }

    public synchronized ImageLoader getImageLoader() {
        if (IMAGE_LOADER == null) {
            IMAGE_LOADER = new ImageLoader(this);
        }
        return IMAGE_LOADER;
    }

    public void downloadRecursively(final String id, final boolean save, final boolean append, final boolean autoplay, final boolean shuffle) {
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
    
}
