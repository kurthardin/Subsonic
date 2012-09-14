package github.daneren2005.dsub.util;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.service.OfflineException;
import github.daneren2005.dsub.service.ServerTooOldException;
import android.app.Activity;
import android.widget.ImageButton;

public class StarringHelper {
	
    public static void toggleStarredInBackground(final Activity activity, final MusicDirectory.Entry entry, final ImageButton button) {
        
    	final boolean starred = !entry.isStarred();
    	
    	button.setImageResource(starred ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
    	entry.setStarred(starred);
    	
        //        Util.toast(SubsonicTabActivity.this, getResources().getString(R.string.starring_content, entry.getTitle()));
        new SilentBackgroundTask<Void>(activity) {
            @Override
            protected Void doInBackground() throws Throwable {
                MusicService musicService = MusicServiceFactory.getMusicService(activity);
				musicService.setStarred(entry.getId(), starred, activity, null);
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
            		msg = activity.getResources().getString(R.string.starring_content_error, entry.getTitle()) + " " + getErrorMessage(error);
            	}
            	
        		Util.toast(activity, msg, false);
            }
        }.execute();
    }

}
