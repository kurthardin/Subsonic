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
import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.DownloadServiceImpl;
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

	private final ServiceConnection mConnection = new ServiceConnection() {
		@SuppressWarnings("unchecked")
		public void onServiceConnected(ComponentName className, IBinder service) {
			mDownloadService = ((SimpleServiceBinder<DownloadServiceImpl>) service).getService();
			configureNowPlayingView();
		}

		public void onServiceDisconnected(ComponentName className) {
			mDownloadService = null;
			if (mNowPlayingView != null) {
				mNowPlayingView.onCurrentSongChanged(null, null);
			}
		}
	};

	private DownloadService mDownloadService;
	private NowPlayingView mNowPlayingView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent serviceIntent = new Intent(this, DownloadServiceImpl.class);
		bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onResume() {
		super.onResume();
		configureNowPlayingView();
	}

	@Override
	public void onPause() {
		if (mDownloadService != null) {
			mDownloadService.setNowPlayingListener(null);
        }
		super.onPause();
	}
	
	@Override
	public void onDestroy() {
		unbindService(mConnection);
		mDownloadService = null;
		super.onDestroy();
	}
	
	public void configureNowPlayingView() {
		if (mNowPlayingView == null) {
			mNowPlayingView = (NowPlayingView) findViewById(R.id.now_playing_view);
		}
		if (mDownloadService != null && mNowPlayingView != null) {
			mDownloadService.setNowPlayingListener(mNowPlayingView);
			DownloadFile currentFile = mDownloadService.getCurrentPlaying();
			mNowPlayingView.onCurrentSongChanged(mDownloadService, currentFile == null ? null : currentFile.getSong());
			mNowPlayingView.onPlaybackStateChanged(mDownloadService, mDownloadService.getPlayerState());
		}
	}

}

