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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

public class MainActivity extends SubsonicTabActivity 
implements Exitable, Restartable {

	private static final String TAG = MainActivity.class.getSimpleName();
    
    private static boolean infoDialogDisplayed;
	
    private MainActivityPagerAdapter mPagerAdapter;
    private ViewPager mViewPager;
    private SubsonicTabFragment prevPageFragment;
    
    public static Intent createIntent(Context context) {
    	Intent intent = new Intent(context, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return intent;
    }
    
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
    	mPagerAdapter.notifyDataSetChanged();
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
        	String action = intent.getAction();
    		if (intent.getBooleanExtra(Constants.INTENT_EXTRA_NAME_EXIT, false)) {
    			exit();
    		} else if (Constants.INTENT_ACTION_START_DOWNLOAD_ACTIVITY.equals(action))  {
    				startActivity(new Intent(this, DownloadActivity.class));
    		}
    	}
    }

    @Override
    protected void onPostCreate(Bundle bundle) {
        super.onPostCreate(bundle);
        
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(false);

        showInfoDialog();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
        Util.setDefaultPreferenceValues(this);
    }

    @Override
    public void exit() {
        stopService(new Intent(this, DownloadServiceImpl.class));
        finish();
    }

    private void showInfoDialog() {
        if (!infoDialogDisplayed) {
            infoDialogDisplayed = true;
            if (Util.getRestUrl(this, null).contains("demo.subsonic.org")) {
                Util.info(this, R.string.main_welcome_title, R.string.main_welcome_text);
            }
        }
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
    	public int getItemPosition(Object object) {
    		int position;
    		if (object instanceof SelectArtistFragment) {
        		if (Util.isOffline(MainActivity.this)) {
        			position = 0;
        		} else {
        			position = 3;
        		}
        	} else {
        		position = POSITION_NONE;
        	}
        	return position;
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