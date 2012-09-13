package github.daneren2005.dsub.util.compat;

import github.daneren2005.dsub.domain.MusicDirectory;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;

public abstract class RemoteControlClientHelper {
	
	protected final Context mContext;
	
	public static RemoteControlClientHelper createInstance(Context context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return new RemoteControlClientBase(context);
		} else {
			return new RemoteControlClientICS(context);
		}
	}
	
	protected RemoteControlClientHelper(Context context) {
		mContext = context;
	}
	
	public abstract void register(final ComponentName mediaButtonReceiverComponent);
	public abstract void unregister();
	public abstract void setPlaybackState(final int state);
	public abstract void updateMetadata(final MusicDirectory.Entry currentSong);
	
}
