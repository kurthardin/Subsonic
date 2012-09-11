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
import github.daneren2005.dsub.activity.MainActivity;
import github.daneren2005.dsub.interfaces.Refreshable;
import github.daneren2005.dsub.util.Util;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;

/**
 * @author Sindre Mehus
 */
public abstract class SubsonicTabFragment extends SherlockListFragment implements Refreshable {
	
	private boolean mShouldSelect;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
    	if (shouldSelect()) {
    		select();
    	}
	}
	
	public void startActivity(Class<? extends Activity> newActivity) {
		Util.startActivityWithoutTransition(getActivity(), newActivity);
	}
	
	@Override
	public void startActivity(Intent intent) {
		Util.startActivityWithoutTransition(getActivity(), intent);
	}
	
	public final void onPageSelected() {
		if (getActivity() == null) {
			mShouldSelect = true;
		} else {
			select();
		}
	}
	
	public final boolean shouldSelect() {
		return mShouldSelect;
	}
	
	protected void select() {
		mShouldSelect = false;
		doSelect();
	}
	
    protected abstract void doSelect();
	
    public abstract void refresh();
    
    protected MainActivity getMainActivity() {
    	return (MainActivity) getActivity();
    }
    
    public void setProgressVisible(boolean visible) {
        View view = getView().findViewById(R.id.tab_progress_bar);
        if (view != null) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        getMainActivity().setProgressVisible(visible);
    }
    
    public void updateProgress(String message) {
    	View view = getView();
    	if (view != null) {
    		TextView textView = (TextView) view.findViewById(R.id.tab_progress_message);
    		if (textView != null) {
    			textView.setText(message);
    		}
    	}
    }
	
}

