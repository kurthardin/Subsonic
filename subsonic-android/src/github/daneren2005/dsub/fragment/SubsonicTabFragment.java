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
package github.daneren2005.dsub.fragment;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.activity.SubsonicTabActivity;
import github.daneren2005.dsub.interfaces.Refreshable;
import github.daneren2005.dsub.util.Util;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListFragment;

/**
 * @author Sindre Mehus
 */
public abstract class SubsonicTabFragment extends SherlockListFragment implements Refreshable {
	
	private boolean mIsSelected;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setRetainInstance(true);
    	setHasOptionsMenu(true);
    }
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		SubsonicTabActivity tabActivity = getTabActivity();
    	tabActivity.setTitle(R.string.common_appname);
		tabActivity.getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		refresh();
	}
	
	public void startActivity(Class<? extends Activity> newActivity) {
		Util.startActivityWithoutTransition(getActivity(), newActivity);
	}
	
	@Override
	public void startActivity(Intent intent) {
		Util.startActivityWithoutTransition(getActivity(), intent);
	}
	
	public final void onPageSelected() {
		mIsSelected = true;
		if (getActivity() != null) {
			refresh();
		}
	}
	
	public final void onPageDeselected() {
		mIsSelected = false;
	}
	
    public abstract void refresh();
    
    protected SubsonicTabActivity getTabActivity() {
    	return (SubsonicTabActivity) getActivity();
    }
    
    public void setProgressVisible(boolean visible) {
    	if (mIsSelected) {
    		View view = getView();
    		if (view != null) {
    			View progressView = view.findViewById(R.id.tab_progress_bar);
    			if (progressView != null) {
    				progressView.setVisibility(visible ? View.VISIBLE : View.GONE);
    			}
    			getTabActivity().setProgressVisible(visible);
    		}
    	}
    }
    
    public void updateProgress(String message) {
    	if (mIsSelected) {
    		View view = getView();
    		if (view != null) {
    			TextView textView = (TextView) view.findViewById(R.id.tab_progress_message);
    			if (textView != null) {
    				textView.setText(message);
    			}
    		}
    	}
    }
	
}

