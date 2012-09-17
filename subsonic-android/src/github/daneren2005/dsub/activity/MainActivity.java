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
import github.daneren2005.dsub.fragment.SelectAlbumFragment;
import github.daneren2005.dsub.fragment.SelectAlbumFragment.AlbumListType;
import github.daneren2005.dsub.fragment.SelectArtistFragment;
import github.daneren2005.dsub.fragment.SelectPlaylistFragment;
import github.daneren2005.dsub.fragment.SubsonicTabFragment;
import github.daneren2005.dsub.interfaces.Exitable;
import github.daneren2005.dsub.interfaces.Restartable;
import github.daneren2005.dsub.service.DownloadServiceImpl;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.Util;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

public class MainActivity extends SubsonicTabActivity 
implements Exitable, Restartable {

	private static final String TAG = MainActivity.class.getSimpleName();
	
    private MainActivityPagerAdapter mPagerAdapter;
    private ViewPager mViewPager;
    
    private SubsonicTabFragment prevPageFragment;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
     
        mPagerAdapter = new MainActivityPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
        	@Override
        	public void onPageSelected(int position) {
        		notifyFragmentOnPageSelected(position);
        	}
        });
        
        if (savedInstanceState == null) {
        	int position = Util.isOffline(this) ? 0 : 2;
        	if (position == mViewPager.getCurrentItem()) {
        		notifyFragmentOnPageSelected(position);
        	} else {
        		mViewPager.setCurrentItem(position);
        	}
        }
        
        handleExtras(getIntent());
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
    	handleExtras(intent);
    	notifyFragmentOnPageSelected(mViewPager.getCurrentItem());
    }
    
    private void notifyFragmentOnPageSelected(int position) {
    	if (prevPageFragment != null) {
    		prevPageFragment.onPageDeselected();
    	}
		prevPageFragment = (SubsonicTabFragment) mPagerAdapter.instantiateItem(mViewPager, position);
		prevPageFragment.onPageSelected();
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
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(false);
    }

    @Override
    public void exit() {
        stopService(new Intent(this, DownloadServiceImpl.class));
        finish();
    }
    
    public class MainActivityPagerAdapter extends FragmentPagerAdapter {
    	
    	private final String [] titles = new String [] {
    			"New", 
    			"Recent", 
    			"Artists", 
    			getString(R.string.main_albums_highest),
    			getString(R.string.main_albums_frequent),
    			getString(R.string.main_albums_random),
    			getString(R.string.button_bar_playlists)
    	};
    	
    	private final String offlineTitle = "Artists";
    	
    	public MainActivityPagerAdapter(FragmentManager fm) {
    		super(fm);
    	}

    	@Override
    	public Fragment getItem(int i) {
    		Fragment fragment;
    		Bundle args = new Bundle();
    		if (Util.isOffline(MainActivity.this)) {
    			return new SelectArtistFragment();
    		} else {
    			switch(i) {
					case 0:
						fragment = new SelectAlbumFragment();
						args.putInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE, AlbumListType.NEWEST.ordinal());
						break;

					case 1:
						fragment = new SelectAlbumFragment();
						args.putInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE, AlbumListType.RECENT.ordinal());
						break;

					case 2:
						fragment = new SelectArtistFragment();
						break;

					case 3:
						fragment = new SelectAlbumFragment();
						args.putInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE, AlbumListType.HIGHEST.ordinal());
						break;

					case 4:
						fragment = new SelectAlbumFragment();
						args.putInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE, AlbumListType.FREQUENT.ordinal());
						break;

					case 5:
						fragment = new SelectAlbumFragment();
						args.putInt(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE, AlbumListType.RANDOM.ordinal());
						break;

					case 6:
						fragment = new SelectPlaylistFragment();
						break;

					default:
						fragment = null;
    			}
    		}
    		
    		if (fragment != null) {
    			fragment.setArguments(args);
    		}
    		
    		return fragment;
    	}

    	@Override
    	public int getCount() {
    		return Util.isOffline(MainActivity.this) ? 1 : 7;
    	}

    	@Override
    	public CharSequence getPageTitle(int position) {
    		return Util.isOffline(MainActivity.this) ? offlineTitle : titles[position];
    	}
    }
}

// Previous implementation
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