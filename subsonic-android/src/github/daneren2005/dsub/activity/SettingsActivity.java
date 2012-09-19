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
import github.daneren2005.dsub.provider.SearchSuggestionProvider1;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.DownloadServiceImpl;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.ErrorDialog;
import github.daneren2005.dsub.util.FileUtil;
import github.daneren2005.dsub.util.ModalBackgroundTask;
import github.daneren2005.dsub.util.Util;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.SearchRecentSuggestions;
import android.util.Log;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

public class SettingsActivity extends SherlockPreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = SettingsActivity.class.getSimpleName();
    private static final int MAX_SERVERS = 3;
    
    private PreferenceCategory serverCategory;
	private List<Preference> serverPreferenceScreens = new ArrayList<Preference>(MAX_SERVERS);
    private final Map<String, ServerSettings> serverSettings = new LinkedHashMap<String, ServerSettings>();
    private boolean testingConnection;
    private ListPreference theme;
    private ListPreference maxBitrateWifi;
    private ListPreference maxBitrateMobile;
	private ListPreference networkTimeout;
    private EditTextPreference cacheSize;
    private EditTextPreference cacheLocation;
    private ListPreference preloadCount;
	private EditTextPreference randomSize;

    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Util.setDefaultPreferenceValues(this);
        
        addPreferencesFromResource(R.xml.settings);
        
        serverCategory = (PreferenceCategory) findPreference("serverCategory");
        for (int i = 1; i <= MAX_SERVERS; i++) {
        	final int serverId = i;
        	final PreferenceScreen serverPref = (PreferenceScreen) findPreference("server" + serverId);
        	serverPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					if (preference.getTitle().toString().equals(getString(R.string.settings_server_add))) {
						addServer();
					}
					return false;
				}
			});
            findPreference("testConnection" + serverId).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    testConnection(serverId);
                    return false;
                }
            });
            findPreference("removeServer" + serverId).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    removeServer(serverId);
                    serverPref.getDialog().dismiss();
                    return true;
                }
            });
			serverPreferenceScreens.add(serverPref);
		}

        theme = (ListPreference) findPreference(Constants.PREFERENCES_KEY_THEME);
        maxBitrateWifi = (ListPreference) findPreference(Constants.PREFERENCES_KEY_MAX_BITRATE_WIFI);
        maxBitrateMobile = (ListPreference) findPreference(Constants.PREFERENCES_KEY_MAX_BITRATE_MOBILE);
		networkTimeout = (ListPreference) findPreference(Constants.PREFERENCES_KEY_NETWORK_TIMEOUT);
        cacheSize = (EditTextPreference) findPreference(Constants.PREFERENCES_KEY_CACHE_SIZE);
        cacheLocation = (EditTextPreference) findPreference(Constants.PREFERENCES_KEY_CACHE_LOCATION);
        preloadCount = (ListPreference) findPreference(Constants.PREFERENCES_KEY_PRELOAD_COUNT);
		randomSize = (EditTextPreference) findPreference(Constants.PREFERENCES_KEY_RANDOM_SIZE);

        findPreference("clearSearchHistory").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SearchRecentSuggestions suggestions = new SearchRecentSuggestions(SettingsActivity.this, SearchSuggestionProvider1.AUTHORITY, SearchSuggestionProvider1.MODE);
                suggestions.clearHistory();
                Util.toast(SettingsActivity.this, R.string.settings_search_history_cleared);
                return false;
            }
        });
        
        SharedPreferences prefs = Util.getPreferences(this);
        
		int numServers = prefs.getInt(Constants.PREFERENCES_KEY_SERVER_COUNT, 0);
        for (int i = 1; i <= numServers; i++) {
            String instance = String.valueOf(i);
            serverSettings.put(instance, new ServerSettings(instance));
        }
        if (numServers < MAX_SERVERS) {
        	Preference serverScreen = findPreference("server" + String.valueOf(numServers + 1));
        	serverScreen.setTitle(R.string.settings_server_add);
        	for (int i = numServers + 2; i <= MAX_SERVERS; i++) {
        		String instance = String.valueOf(i);
        		Preference unusedServerPref = findPreference("server" + instance);
        		serverCategory.removePreference(unusedServerPref);
        	}
        }
		
        prefs.registerOnSharedPreferenceChangeListener(this);

        update();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        SharedPreferences prefs = Util.getPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, "Preference changed: " + key);
        update();

        if (Constants.PREFERENCES_KEY_HIDE_MEDIA.equals(key)) {
            setHideMedia(sharedPreferences.getBoolean(key, false));
        }
        else if (Constants.PREFERENCES_KEY_MEDIA_BUTTONS.equals(key)) {
            setMediaButtonsEnabled(sharedPreferences.getBoolean(key, true));
        }
        else if (Constants.PREFERENCES_KEY_CACHE_LOCATION.equals(key)) {
            setCacheLocation(sharedPreferences.getString(key, ""));
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
	    	case android.R.id.home:
	        	finish();
	        	return true;
	
	    	default:
	    		return false;
		}
    }
    
    @Override
    public void finish() {
    	super.finish();
    	Util.disablePendingTransition(this);
    }
    
    private void addServer() {
    	SharedPreferences prefs = Util.getPreferences(SettingsActivity.this);
    	int numServers = prefs.getInt(Constants.PREFERENCES_KEY_SERVER_COUNT, 0) + 1;
    	
    	String instance = String.valueOf(numServers);
    	serverSettings.put(instance, new ServerSettings(instance));

    	if (numServers < MAX_SERVERS) {
    		Preference newAddServerPref = serverPreferenceScreens.get(numServers); // serverPreferenceScreens uses 0 based indexing.
    		newAddServerPref.setTitle(R.string.settings_server_add);
    		serverCategory.addPreference(newAddServerPref);
    	}
    	onContentChanged();
    	
    	prefs.edit().putInt(Constants.PREFERENCES_KEY_SERVER_COUNT, numServers).commit();
    }
    
    private void removeServer(int serverId) {
    	SharedPreferences prefs = Util.getPreferences(SettingsActivity.this);
    	int numServers = prefs.getInt(Constants.PREFERENCES_KEY_SERVER_COUNT, 0);
    	if (numServers > 0) {
    		if (serverId < numServers) {
    			for (int i = serverId + 1; i <= numServers; i++) {
    				String serverName = prefs.getString("serverName" + i, getString(R.string.settings_server_new_name));
    				String serverUrl = prefs.getString("serverUrl" + i, "http://");
    				String username = prefs.getString("username" + i, null);
    				String password = prefs.getString("password" + i, null);
    				
    				String prevServerKey = String.valueOf(i - 1);
    				
    				ServerSettings server = serverSettings.get(prevServerKey);
    				server.setName(serverName);
    				server.setUrl(serverUrl);
    				server.setUsername(username);
    				server.setPassword(password);
				}
    		}
    		
    		if (numServers < MAX_SERVERS) {
    			Preference lastServerPref = serverPreferenceScreens.get(numServers);
    			serverCategory.removePreference(lastServerPref);
    		}

    		ServerSettings removedServer = serverSettings.remove(String.valueOf(numServers));
    		removedServer.reset();
    		onContentChanged();
    		
    		prefs.edit()
    		.putInt(Constants.PREFERENCES_KEY_SERVER_COUNT, numServers - 1)
    		.commit();
    		
    		int activeServer = Util.getActiveServer(this);
    		if (activeServer == serverId) {
    			Util.setActiveServer(this, 0);
    		} else if (activeServer > serverId) {
    			Util.setActiveServer(this, activeServer - 1);
    		}
    	}
    }

    private void update() {
        if (testingConnection) {
            return;
        }

        theme.setSummary(theme.getEntry());
        maxBitrateWifi.setSummary(maxBitrateWifi.getEntry());
        maxBitrateMobile.setSummary(maxBitrateMobile.getEntry());
		networkTimeout.setSummary(networkTimeout.getEntry());
        cacheSize.setSummary(cacheSize.getText());
        cacheLocation.setSummary(cacheLocation.getText());
        preloadCount.setSummary(preloadCount.getEntry());
		randomSize.setSummary(randomSize.getText());
        for (ServerSettings ss : serverSettings.values()) {
            ss.update();
        }
    }

    private void setHideMedia(boolean hide) {
        File nomediaDir = new File(FileUtil.getSubsonicDirectory(), ".nomedia");
        if (hide && !nomediaDir.exists()) {
            if (!nomediaDir.mkdir()) {
                Log.w(TAG, "Failed to create " + nomediaDir);
            }
        } else if (nomediaDir.exists()) {
            if (!nomediaDir.delete()) {
                Log.w(TAG, "Failed to delete " + nomediaDir);
            }
        }
        Util.toast(this, R.string.settings_hide_media_toast, false);
    }

    private void setMediaButtonsEnabled(boolean enabled) {
        if (enabled) {
            Util.registerMediaButtonEventReceiver(this);
        } else {
            Util.unregisterMediaButtonEventReceiver(this);
        }
    }

    private void setCacheLocation(String path) {
        File dir = new File(path);
        if (!FileUtil.ensureDirectoryExistsAndIsReadWritable(dir)) {
            Util.toast(this, R.string.settings_cache_location_error, false);

            // Reset it to the default.
            String defaultPath = FileUtil.getDefaultMusicDirectory().getPath();
            if (!defaultPath.equals(path)) {
                SharedPreferences prefs = Util.getPreferences(this);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(Constants.PREFERENCES_KEY_CACHE_LOCATION, defaultPath);
                editor.commit();
                cacheLocation.setSummary(defaultPath);
                cacheLocation.setText(defaultPath);
            }

            // Clear download queue.
            DownloadService downloadService = DownloadServiceImpl.getInstance();
            downloadService.clear();
        }
    }

    private void testConnection(final int instance) {
        ModalBackgroundTask<Boolean> task = new ModalBackgroundTask<Boolean>(this, false) {
            private int previousInstance;

            @Override
            protected Boolean doInBackground() throws Throwable {
                updateProgress(R.string.settings_testing_connection);

                previousInstance = Util.getActiveServer(SettingsActivity.this);
                testingConnection = true;
                Util.setActiveServer(SettingsActivity.this, instance);
                try {
                    MusicService musicService = MusicServiceFactory.getMusicService(SettingsActivity.this);
                    musicService.ping(SettingsActivity.this, this);
                    return musicService.isLicenseValid(SettingsActivity.this, null);
                } finally {
                    Util.setActiveServer(SettingsActivity.this, previousInstance);
                    testingConnection = false;
                }
            }

            @Override
            protected void done(Boolean licenseValid) {
                if (licenseValid) {
                    Util.toast(SettingsActivity.this, R.string.settings_testing_ok);
                } else {
                    Util.toast(SettingsActivity.this, R.string.settings_testing_unlicensed);
                }
            }

            @Override
            protected void cancel() {
                super.cancel();
                Util.setActiveServer(SettingsActivity.this, previousInstance);
            }

            @Override
            protected void error(Throwable error) {
                Log.w(TAG, error.toString(), error);
                new ErrorDialog(SettingsActivity.this, getResources().getString(R.string.settings_connection_failure) +
                        " " + getErrorMessage(error), false);
            }
        };
        task.execute();
    }

    private class ServerSettings {
    	
        private EditTextPreference mName;
        private EditTextPreference mUrl;
        private EditTextPreference mUsername;
        private EditTextPreference mPassword;
        private PreferenceScreen mScreen;

        private ServerSettings(String instance) {

            mScreen = (PreferenceScreen) findPreference("server" + instance);
            mName = (EditTextPreference) findPreference(Constants.PREFERENCES_KEY_SERVER_NAME + instance);
            mUrl = (EditTextPreference) findPreference(Constants.PREFERENCES_KEY_SERVER_URL + instance);
            mUsername = (EditTextPreference) findPreference(Constants.PREFERENCES_KEY_USERNAME + instance);
            mPassword = (EditTextPreference) findPreference("password" + instance);

            mUrl.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                    try {
                        String url = (String) value;
                        new URL(url);
                        if (!url.equals(url.trim()) || url.contains("@")) {
                            throw new Exception();
                        }
                    } catch (Exception x) {
                        new ErrorDialog(SettingsActivity.this, R.string.settings_invalid_url, false);
                        return false;
                    }
                    return true;
                }
            });

            mUsername.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                    String username = (String) value;
                    if (username == null || !username.equals(username.trim())) {
                        new ErrorDialog(SettingsActivity.this, R.string.settings_invalid_username, false);
                        return false;
                    }
                    return true;
                }
            });
        }
        
        private void setName(String name) {
        	mName.setText(name);
        }
        
        private void setUrl(String url) {
        	mUrl.setText(url);
        }
        
        private void setUsername(String username) {
        	mUsername.setText(username);
        }
        
        private void setPassword(String password) {
        	mPassword.setText(password);
        }
        
        private void setTitle(String title) {
        	mScreen.setTitle(title);
        }
        
        private void setSummary(String summary) {
        	mScreen.setSummary(summary);
        }
        
        private void update() {
            mName.setSummary(mName.getText());
            mUrl.setSummary(mUrl.getText());
            mUsername.setSummary(mUsername.getText());
            mScreen.setSummary(mUrl.getText());
            mScreen.setTitle(mName.getText());
        }
        
        private void reset() {
    		mName.setText(getString(R.string.settings_server_new_name));
    		mUrl.setText("http://");
    		mUsername.setText(null);
    		mPassword.setText(null);
    		mScreen.setTitle(R.string.settings_server_add);
    		mScreen.setSummary(null);
        }
    }
}