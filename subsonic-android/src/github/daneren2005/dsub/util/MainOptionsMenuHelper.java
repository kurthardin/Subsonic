package github.daneren2005.dsub.util;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.activity.HelpActivity;
import github.daneren2005.dsub.activity.MainActivity;
import github.daneren2005.dsub.activity.SearchActivity;
import github.daneren2005.dsub.activity.SettingsActivity;
import github.daneren2005.dsub.interfaces.Exitable;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public abstract class MainOptionsMenuHelper {
	
	private static Menu kCurrentMenu;
	
	private static View kServerContextView;
	private static boolean kIsRefreshVisible;
	
	public static void registerForServerContextMenu(Activity activity) {
        if (kServerContextView == null || !kServerContextView.getContext().equals(activity)) {
        	if (kServerContextView != null) {
        		ViewGroup contentView = (ViewGroup) kServerContextView.getParent();
        		contentView.removeView(kServerContextView);
        		activity.unregisterForContextMenu(kServerContextView);
        	}
        	
        	kServerContextView = new View(activity);
        	kServerContextView.setVisibility(View.GONE);
        	activity.registerForContextMenu(kServerContextView);
        	ViewGroup contentView = (ViewGroup) activity.getWindow().getDecorView().findViewById(android.R.id.content);
        	contentView.addView(kServerContextView, 0);
        }
	}
	
    public static boolean onCreateOptionsMenu(MenuInflater inflater, Menu menu) {
        inflater.inflate(R.menu.main, menu);
        
        kCurrentMenu = menu;
        
        return true;
    }
    
    public static boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem refreshItem = menu.findItem(R.id.action_refresh);
        if (refreshItem != null) {
        	refreshItem.setVisible(kIsRefreshVisible);
        }
        return true;
    }
    
    public static <T extends Activity & Exitable> boolean onOptionsItemSelected(T activity, MenuItem item) {
        switch (item.getItemId()) {
        
	        case android.R.id.home:
	        	Intent homeIntent = new Intent(activity, MainActivity.class);
	        	homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        	Util.startActivityWithoutTransition(activity, homeIntent);
	        	break;
	        	
	        case R.id.action_search:
	        	Intent searchIntent = new Intent(activity, SearchActivity.class);
            	searchIntent.putExtra(Constants.INTENT_EXTRA_REQUEST_SEARCH, true);
                Util.startActivityWithoutTransition(activity, searchIntent);
                break;
                
	        case R.id.menu_help:
	        	activity.startActivity(new Intent(activity, HelpActivity.class));
	        	break;
                
	        case R.id.menu_server:
	        	kServerContextView.showContextMenu();
	        	break;
	        	
	        case R.id.menu_settings:
	        	activity.startActivity(new Intent(activity, SettingsActivity.class));
	        	break;
	        	
	        case R.id.menu_exit:
	        	activity.exit();
	        	break;
	        	
	        default:
	        	return false;

        }

        return true;
    }
    
    public static boolean isRefreshVisible() {
    	return kIsRefreshVisible; 
    }
    
    public static void setRefreshVisible(boolean visible) {
    	kIsRefreshVisible = visible;
    	if (kCurrentMenu != null) {
    		MenuItem refreshItem = kCurrentMenu.findItem(R.id.action_refresh);
    		if (refreshItem != null) {
    			refreshItem.setVisible(kIsRefreshVisible);
    		}
    	}
    }
    
    public static View getServerContextMenuPresenter() {
    	return kServerContextView;
    }
	
}
