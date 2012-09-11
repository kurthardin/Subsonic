package github.daneren2005.dsub.util;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.interfaces.Restartable;
import github.daneren2005.dsub.service.DownloadService;
import android.content.Context;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;

public abstract class SelectServerHelper {
	
    private static final int MENU_ITEM_SERVER_1 = 101;
    private static final int MENU_ITEM_SERVER_2 = 102;
    private static final int MENU_ITEM_SERVER_3 = 103;
    private static final int MENU_ITEM_OFFLINE = 104;

    public static void onCreateContextMenu(Context context, ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
    	if (view.equals(MainOptionsMenuHelper.getServerContextMenuPresenter())) {
    		MenuItem menuItem1 = menu.add(R.id.select_server_context_menu, MENU_ITEM_SERVER_1, MENU_ITEM_SERVER_1, Util.getServerName(context, 1));
    		MenuItem menuItem2 = menu.add(R.id.select_server_context_menu, MENU_ITEM_SERVER_2, MENU_ITEM_SERVER_2, Util.getServerName(context, 2));
    		MenuItem menuItem3 = menu.add(R.id.select_server_context_menu, MENU_ITEM_SERVER_3, MENU_ITEM_SERVER_3, Util.getServerName(context, 3));
    		MenuItem menuItem4 = menu.add(R.id.select_server_context_menu, MENU_ITEM_OFFLINE, MENU_ITEM_OFFLINE, Util.getServerName(context, 0));
    		menu.setGroupCheckable(R.id.select_server_context_menu, true, true);
    		menu.setHeaderTitle(R.string.main_select_server);

    		switch (Util.getActiveServer(context)) {
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

    public static <T extends Context & Restartable> boolean onContextItemSelected(T context, MenuItem menuItem) {
    	if (menuItem.getGroupId() == R.id.select_server_context_menu) {
    		switch (menuItem.getItemId()) {
    		case MENU_ITEM_OFFLINE:
    			setActiveServer(context, 0);
    			break;
    		case MENU_ITEM_SERVER_1:
    			setActiveServer(context, 1);
    			break;
    		case MENU_ITEM_SERVER_2:
    			setActiveServer(context, 2);
    			break;
    		case MENU_ITEM_SERVER_3:
    			setActiveServer(context, 3);
    			break;
    		default:
    			return false;
    		}

    		context.restart();
    		return true;
    	} else {
    		return false;
        }
    }

    private static void setActiveServer(Context context, int instance) {
        if (Util.getActiveServer(context) != instance) {
            DownloadService service = Util.getDownloadService(context);
            if (service != null) {
                service.clearIncomplete();
            }
            Util.setActiveServer(context, instance);
        }
    }

}
