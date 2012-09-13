package github.daneren2005.dsub.util.compat;

import github.daneren2005.dsub.domain.MusicDirectory.Entry;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

public class RemoteControlClientBase extends RemoteControlClientHelper {

    protected RemoteControlClientBase(Context context) {
		super(context);
	}

	private static final String TAG = RemoteControlClientBase.class.getSimpleName();

	@Override
	public void register(ComponentName mediaButtonReceiverComponent) {
		Log.w(TAG, "RemoteControlClient requires Android API level 14 or higher.");
	}

	@Override
	public void unregister() {
		Log.w(TAG, "RemoteControlClient requires Android API level 14 or higher.");
	}

	@Override
	public void setPlaybackState(int state) {
		Log.w(TAG, "RemoteControlClient requires Android API level 14 or higher.");
	}

	@Override
	public void updateMetadata(Entry currentSong) {
		Log.w(TAG, "RemoteControlClient requires Android API level 14 or higher.");
	}

}
