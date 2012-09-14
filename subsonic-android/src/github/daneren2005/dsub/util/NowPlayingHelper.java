package github.daneren2005.dsub.util;

import github.daneren2005.dsub.service.DownloadFile;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.view.NowPlayingView;

public abstract class NowPlayingHelper {
	
	public static void onResume(DownloadService service, NowPlayingView nowPlayingView) {
		if (service != null && nowPlayingView != null) {
			service.setNowPlayingListener(nowPlayingView);
			DownloadFile currentFile = service.getCurrentPlaying();
			nowPlayingView.onCurrentSongChanged(service, currentFile == null ? null : currentFile.getSong());
			nowPlayingView.onPlaybackStateChanged(service, service.getPlayerState());
		}
	}
	
	public static void onPause(DownloadService service) {
        if (service != null) {
        	service.setNowPlayingListener(null);
        }
	}

}
