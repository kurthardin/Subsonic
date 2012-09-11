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
import github.daneren2005.dsub.fragment.SelectAlbumFragment;
import github.daneren2005.dsub.fragment.SelectArtistFragment;
import github.daneren2005.dsub.fragment.SelectPlaylistFragment;
import github.daneren2005.dsub.fragment.SubsonicTabFragment;
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

import java.io.File;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class MainActivity extends SherlockFragmentActivity implements Exitable, Restartable {

	private static final String TAG = MainActivity.class.getSimpleName();
    private static ImageLoader IMAGE_LOADER;
    
    private MainActivityPagerAdapter mPagerAdapter;
    private ViewPager mViewPager;

    private boolean destroyed;

    private String theme;

    private static boolean infoDialogDisplayed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setUncaughtExceptionHandler();
        applyTheme();
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.main);
        
        startService(new Intent(this, DownloadServiceImpl.class));
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
     
        mPagerAdapter = new MainActivityPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
        	@Override
        	public void onPageSelected(int position) {
        		notifyFragmentOnPageSelected(position);
        	}
        });
        int prevItem = 2; // TODO: Remember previous tab
        if (prevItem == mViewPager.getCurrentItem()) {
        	notifyFragmentOnPageSelected(prevItem);
        } else {
        	mViewPager.setCurrentItem(prevItem);
        }
        
        handleExtras(getIntent());
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
    	handleExtras(intent);
    	notifyFragmentOnPageSelected(mViewPager.getCurrentItem());
    }
    
    private void notifyFragmentOnPageSelected(int position) {
		SubsonicTabFragment fragment = (SubsonicTabFragment) mPagerAdapter.instantiateItem(mViewPager, position);
		fragment.onPageSelected();
    }
    
    private void handleExtras(Intent intent) {
    	if (intent != null) {
    		if (intent.getBooleanExtra(Constants.INTENT_EXTRA_NAME_EXIT, false)) {
    			exit();
    		}
    	}
    }

    @Override
    protected void onPostCreate(Bundle bundle) {
        super.onPostCreate(bundle);
        
        loadSettings();
        setProgressVisible(false);
        showInfoDialog();

        // Remember the current theme.
        theme = Util.getTheme(this);

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
    protected void onDestroy() {
        super.onDestroy();
        destroyed = true;
        getImageLoader().clear();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean isVolumeDown = keyCode == KeyEvent.KEYCODE_VOLUME_DOWN;
        boolean isVolumeUp = keyCode == KeyEvent.KEYCODE_VOLUME_UP;
        boolean isVolumeAdjust = isVolumeDown || isVolumeUp;
        boolean isJukebox = Util.getDownloadService(this) != null && Util.getDownloadService(this).isJukeboxEnabled();

        if (isVolumeAdjust && isJukebox) {
            Util.getDownloadService(this).adjustJukeboxVolume(isVolumeUp);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void finish() {
        super.finish();
        Util.disablePendingTransition(this);
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
    	Intent intent = new Intent(getIntent());
    	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	startActivity(intent);
    }

    public void exit() {
        stopService(new Intent(this, DownloadServiceImpl.class));
        finish();
    }
    
    public boolean isDestroyed() {
        return destroyed;
    }

    public void setProgressVisible(boolean visible) {
    	setSupportProgressBarIndeterminateVisibility(visible);
    	MainOptionsMenuHelper.setRefreshVisible(!visible);
    	invalidateOptionsMenu();
    	if (!visible) {
        	updateProgress(null);
    	}
    }

    public void updateProgress(String message) {
//    	getSupportActionBar().setSubtitle(message);
    	Log.w(TAG, "MainActivity.updateProgress() does nothing...");
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
                MusicService musicService = MusicServiceFactory.getMusicService(MainActivity.this);
                MusicDirectory root = musicService.getMusicDirectory(id, false, MainActivity.this, this);
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
                    MusicService musicService = MusicServiceFactory.getMusicService(MainActivity.this);
                    getSongsRecursively(musicService.getMusicDirectory(dir.getId(), false, MainActivity.this, this), songs);
                }
            }

            @Override
            protected void done(List<MusicDirectory.Entry> songs) {
                DownloadService downloadService = Util.getDownloadService(MainActivity.this);
                if (!songs.isEmpty() && downloadService != null) {
                    if (!append) {
                        downloadService.clear();
                    }
                    warnIfNetworkOrStorageUnavailable();
                    downloadService.download(songs, save, autoplay, false, shuffle);
//                    showTabActivity(DownloadActivity.class); // TODO: Switch to 'now playing'
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
    
    public class MainActivityPagerAdapter extends FragmentPagerAdapter {
    	public MainActivityPagerAdapter(FragmentManager fm) {
    		super(fm);
    	}

    	@Override
    	public Fragment getItem(int i) {
    		Fragment fragment;
    		Bundle args = new Bundle();
    		switch(i) {
    			case 0:
    				fragment = new SelectAlbumFragment();
    				args.putString(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE, "newest");
    				break;
    				
    			case 1:
    				fragment = new SelectAlbumFragment();
    				args.putString(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE, "recent");
    				break;
    			
    			case 2:
    				fragment = new SelectArtistFragment();
    				break;
    				
    			case 3:
    				fragment = new SelectAlbumFragment();
    				args.putString(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE, "highest");
    				break;
    				
    			case 4:
    				fragment = new SelectAlbumFragment();
    				args.putString(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE, "frequent");
    				break;
    				
    			case 5:
    				fragment = new SelectAlbumFragment();
    				args.putString(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE, "random");
    				break;
    			
    			case 6:
    				fragment = new SelectPlaylistFragment();
    				break;
    			
    			default:
    				fragment = null;
    		}
    		
//    		TODO: Put other arguments?
    		fragment.setArguments(args);
    		
    		return fragment;
    	}

    	@Override
    	public int getCount() {
    		return 7;
    	}

    	@Override
    	public CharSequence getPageTitle(int position) {
    		switch (position) {
    			case 0:
    				return "New";//getString(R.string.main_albums_newest);

    			case 1:
    				return "Recent";//getString(R.string.main_albums_recent);

    			case 2:
    				return "Artists";//getString(R.string.button_bar_browse);
    			
    			case 3:
    				return getString(R.string.main_albums_highest);
    				
    			case 4:
    				return getString(R.string.main_albums_frequent);
    				
    			case 5:
    				return getString(R.string.main_albums_random);
    				
    			case 6:
    				return getString(R.string.button_bar_playlists);
    				
    			default:
    				return null;
    			
    		}
    	}
    }
}

// TODO: Previous implementation
//public class MainActivity extends SubsonicTabActivity {
//
//    private static final int MENU_GROUP_SERVER = 10;
//    private static final int MENU_ITEM_SERVER_1 = 101;
//    private static final int MENU_ITEM_SERVER_2 = 102;
//    private static final int MENU_ITEM_SERVER_3 = 103;
//    private static final int MENU_ITEM_OFFLINE = 104;
//
//    private String theme;
//
//    private static boolean infoDialogDisplayed;
//
//    /**
//     * Called when the activity is first created.
//     */
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        if (getIntent().hasExtra(Constants.INTENT_EXTRA_NAME_EXIT)) {
//            exit();
//        }
//        setContentView(R.layout.main);
//
//        refresh();
//    }
//    
//    protected void refresh() {
//    	loadSettings();
//        
//        View buttons = LayoutInflater.from(this).inflate(R.layout.main_buttons, null);
//
//        final View serverButton = buttons.findViewById(R.id.main_select_server);
//        final TextView serverTextView = (TextView) serverButton.findViewById(R.id.main_select_server_2);
//
//        final View albumsTitle = buttons.findViewById(R.id.main_albums);
//        final View albumsNewestButton = buttons.findViewById(R.id.main_albums_newest);
//        final View albumsRandomButton = buttons.findViewById(R.id.main_albums_random);
//        final View albumsHighestButton = buttons.findViewById(R.id.main_albums_highest);
//        final View albumsRecentButton = buttons.findViewById(R.id.main_albums_recent);
//        final View albumsFrequentButton = buttons.findViewById(R.id.main_albums_frequent);
//
//        final View dummyView = findViewById(R.id.main_dummy);
//
//        int instance = Util.getActiveServer(this);
//        String name = Util.getServerName(this, instance);
//        serverTextView.setText(name);
//
//        ListView list = (ListView) findViewById(R.id.main_list);
//
//        MergeAdapter adapter = new MergeAdapter();
//        adapter.addViews(Arrays.asList(serverButton), true);
//        if (!Util.isOffline(this)) {
//            adapter.addView(albumsTitle, false);
//            adapter.addViews(Arrays.asList(albumsNewestButton, albumsRandomButton, albumsHighestButton, albumsRecentButton, albumsFrequentButton), true);
//        }
//        list.setAdapter(adapter);
//        registerForContextMenu(dummyView);
//
//        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                if (view == serverButton) {
//                    dummyView.showContextMenu();
//                } else if (view == albumsNewestButton) {
//                    showAlbumList("newest");
//                } else if (view == albumsRandomButton) {
//                    showAlbumList("random");
//                } else if (view == albumsHighestButton) {
//                    showAlbumList("highest");
//                } else if (view == albumsRecentButton) {
//                    showAlbumList("recent");
//                } else if (view == albumsFrequentButton) {
//                    showAlbumList("frequent");
//                }
//            }
//        });
//
//        // Title: Subsonic
//        setTitle(R.string.common_appname);
//
////        // Button 1: shuffle
////        ImageButton actionShuffleButton = (ImageButton)findViewById(R.id.action_button_1);
////        actionShuffleButton.setImageResource(R.drawable.action_shuffle);
////        actionShuffleButton.setOnClickListener(new View.OnClickListener() {
////            @Override
////            public void onClick(View view) {
////                Intent intent = new Intent(MainActivity.this, DownloadActivity.class);
////                intent.putExtra(Constants.INTENT_EXTRA_NAME_SHUFFLE, true);
////                Util.startActivityWithoutTransition(MainActivity.this, intent);
////            }
////        });
////
////        // Button 2: search
////        ImageButton actionSearchButton = (ImageButton)findViewById(R.id.action_button_2);
////        actionSearchButton.setImageResource(R.drawable.action_search);
////        actionSearchButton.setOnClickListener(new View.OnClickListener() {
////            @Override
////            public void onClick(View view) {
////            	Intent intent = new Intent(MainActivity.this, SearchActivity.class);
////            	intent.putExtra(Constants.INTENT_EXTRA_REQUEST_SEARCH, true);
////                Util.startActivityWithoutTransition(MainActivity.this, intent);
////            }
////        });
////		
////		// Button 3: Help
////        ImageButton actionHelpButton = (ImageButton)findViewById(R.id.action_button_3);
////        actionHelpButton.setOnClickListener(new View.OnClickListener() {
////            @Override
////            public void onClick(View view) {
////                startActivity(new Intent(MainActivity.this, HelpActivity.class));
////            }
////        });
////		
////		// Button 4: Settings
////        ImageButton actionSettingsButton = (ImageButton)findViewById(R.id.action_button_4);
////        actionSettingsButton.setOnClickListener(new View.OnClickListener() {
////            @Override
////            public void onClick(View view) {
////            	startActivity(new Intent(MainActivity.this, SettingsActivity.class));
////				
////				/*LayoutInflater inflater = (LayoutInflater)MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
////				PopupWindow pw = new PopupWindow(inflater.inflate(R.layout.overflow_menu, null, false), 100, 100, true);
////				pw.showAsDropDown(findViewById(R.id.action_button_4));*/
////				
////				/*PopupWindow window = new PopupWindow(findViewById(R.layout.overflow_menu));
////				window.showAsDropDown(findViewById(R.id.action_button_2));*/
////            }
////        });
//
//        // Remember the current theme.
//        theme = Util.getTheme(this);
//
//        showInfoDialog();
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
//    @Override
//    protected void onResume() {
//        super.onResume();
//
//        // Restart activity if theme has changed.
//        if (theme != null && !theme.equals(Util.getTheme(this))) {
//            restart();
//        }
//    }
//
//    @Override
//    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
//        super.onCreateContextMenu(menu, view, menuInfo);
//
//        MenuItem menuItem1 = menu.add(MENU_GROUP_SERVER, MENU_ITEM_SERVER_1, MENU_ITEM_SERVER_1, Util.getServerName(this, 1));
//        MenuItem menuItem2 = menu.add(MENU_GROUP_SERVER, MENU_ITEM_SERVER_2, MENU_ITEM_SERVER_2, Util.getServerName(this, 2));
//        MenuItem menuItem3 = menu.add(MENU_GROUP_SERVER, MENU_ITEM_SERVER_3, MENU_ITEM_SERVER_3, Util.getServerName(this, 3));
//        MenuItem menuItem4 = menu.add(MENU_GROUP_SERVER, MENU_ITEM_OFFLINE, MENU_ITEM_OFFLINE, Util.getServerName(this, 0));
//        menu.setGroupCheckable(MENU_GROUP_SERVER, true, true);
//        menu.setHeaderTitle(R.string.main_select_server);
//
//        switch (Util.getActiveServer(this)) {
//            case 0:
//                menuItem4.setChecked(true);
//                break;
//            case 1:
//                menuItem1.setChecked(true);
//                break;
//            case 2:
//                menuItem2.setChecked(true);
//                break;
//            case 3:
//                menuItem3.setChecked(true);
//                break;
//        }
//    }
//
//    @Override
//    public boolean onContextItemSelected(MenuItem menuItem) {
//        switch (menuItem.getItemId()) {
//            case MENU_ITEM_OFFLINE:
//                setActiveServer(0);
//                break;
//            case MENU_ITEM_SERVER_1:
//                setActiveServer(1);
//                break;
//            case MENU_ITEM_SERVER_2:
//                setActiveServer(2);
//                break;
//            case MENU_ITEM_SERVER_3:
//                setActiveServer(3);
//                break;
//            default:
//                return super.onContextItemSelected(menuItem);
//        }
//
//        // Restart activity
//        restart();
//        return true;
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
//    private void restart() {
//        Intent intent = new Intent(this, MainActivity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        Util.startActivityWithoutTransition(this, intent);
//    }
//
//    private void exit() {
//        stopService(new Intent(this, DownloadServiceImpl.class));
//        finish();
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
//    private void showAlbumList(String type) {		
//        Intent intent = new Intent(this, SelectAlbumActivity.class);
//        intent.putExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE, type);
//        intent.putExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, 20);
//        intent.putExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET, 0);
//		Util.startActivityWithoutTransition(this, intent);
//	}
//}