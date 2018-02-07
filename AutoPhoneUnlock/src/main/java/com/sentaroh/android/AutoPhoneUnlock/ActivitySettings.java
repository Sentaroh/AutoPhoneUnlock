package com.sentaroh.android.AutoPhoneUnlock;

/*
The MIT License (MIT)
Copyright (c) 2011-2013 Sentaroh

Permission is hereby granted, free of charge, to any person obtaining a copy of 
this software and associated documentation files (the "Software"), to deal 
in the Software without restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to 
the following conditions:

The above copyright notice and this permission notice shall be included in all copies or 
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE 
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

*/

import static com.sentaroh.android.AutoPhoneUnlock.CommonConstants.*;

import java.util.List;

import com.sentaroh.android.AutoPhoneUnlock.R;
import com.sentaroh.android.Utilities.LocalMountPoint;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

@SuppressLint("NewApi")
@SuppressWarnings("deprecation")
public class ActivitySettings extends PreferenceActivity{
	private static Context mContext=null;
	private static PreferenceActivity mPrefAct=null;
	private static PreferenceFragment mPrefFrag=null;
	
	private static EnvironmentParms mEnvParms=null;
	private static CommonUtilities mUtilMain=null;
	
	private GlobalParameters mGp=null;
    
	private static void initEnvParms(Context c, boolean force) {
        if (mEnvParms==null) {
        	mContext=c;
        	mEnvParms=new EnvironmentParms();
        	mEnvParms.loadSettingParms(c);
            GlobalParameters gp=new GlobalParameters();
            gp.setLogParms(mEnvParms);
        	mUtilMain=new CommonUtilities(c.getApplicationContext(), "SettingsActivity",mEnvParms, gp);
        }
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	if (mGp==null) mGp=(GlobalParameters) getApplication();
    	if (mGp.themeColorList!=null && mGp.themeColorList.theme_is_light) setTheme(mGp.applicationTheme);
    	if (mGp.themeColorList==null) {
    		setResult(Activity.RESULT_CANCELED);
    		finish();
    	} else {
    		setResult(Activity.RESULT_OK);
    	}

        super.onCreate(savedInstanceState);
		mPrefAct=this;
		mContext=this;//getApplicationContext();
        initEnvParms(this,false);
        mUtilMain.addDebugMsg(1,"I","onCreate entered");
        if (Build.VERSION.SDK_INT>=11) return;
        
		PreferenceManager pm=getPreferenceManager();
		pm.setSharedPreferencesName(DEFAULT_PREFS_FILENAME);
		pm.setSharedPreferencesMode(Context.MODE_PRIVATE|Context.MODE_MULTI_PROCESS);
		addPreferencesFromResource(R.xml.main_settings);

		SharedPreferences shared_pref=pm.getSharedPreferences();
		
		pref_proximity=mPrefAct.findPreference(mContext.getString(R.string.settings_main_scheduler_sleep_wake_lock_proximity_sensor));

    	if (!LocalMountPoint.isExternalStorageAvailable()) {
    		mPrefAct.findPreference(getString(R.string.settings_main_log_dir).toString())
    			.setEnabled(false);
    	}
		
    	if (Build.VERSION.SDK_INT<11) {
    		findPreference(getString(R.string.settings_main_use_light_theme).toString())
			.setEnabled(false);
    	}

    	if (Build.VERSION.SDK_INT!=17) {
    		findPreference(getString(R.string.settings_main_force_use_trust_device).toString())
			.setEnabled(false);
    	}

    	
		boolean admin=isDeviceAdminActive(mContext);
		shared_pref.edit().putBoolean(
				getString(R.string.settings_main_device_admin), admin).commit();

		initSettingValueBeforeHc(shared_pref,getString(R.string.settings_main_device_admin));
		initSettingValueBeforeHc(shared_pref,getString(R.string.settings_main_scheduler_sleep_wake_lock_option));
		initSettingValueBeforeHc(shared_pref,getString(R.string.settings_main_scheduler_sleep_wake_lock_proximity_sensor));
		initSettingValueBeforeHc(shared_pref,getString(R.string.settings_main_scheduler_rssi_criteria));
		initSettingValueBeforeHc(shared_pref,getString(R.string.settings_main_use_light_theme));
		initSettingValueBeforeHc(shared_pref,getString(R.string.settings_main_force_use_trust_device));
		initSettingValueBeforeHc(shared_pref,getString(R.string.settings_main_log_dir));
		initSettingValueBeforeHc(shared_pref,getString(R.string.settings_main_log_option));
		initSettingValueBeforeHc(shared_pref,getString(R.string.settings_main_log_file_max_count));
		initSettingValueBeforeHc(shared_pref,getString(R.string.settings_main_log_level));
		initSettingValueBeforeHc(shared_pref,getString(R.string.settings_main_exit_clean));
		
	};

	private static void initSettingValueBeforeHc(SharedPreferences shared_pref, String key_string) {
		initSettingValue(mPrefAct.findPreference(key_string),shared_pref,key_string);
	};

	private static void initSettingValueAfterHc(SharedPreferences shared_pref, String key_string) {
		initSettingValue(mPrefFrag.findPreference(key_string),shared_pref,key_string);
	};
	
	private static boolean isDeviceAdminActive(Context c) {
        DevicePolicyManager dpm =(DevicePolicyManager)c.getSystemService(
				Context.DEVICE_POLICY_SERVICE);
        ComponentName darcn = new ComponentName(c,DevAdmReceiver.class);
        return dpm.isAdminActive(darcn);
	};

	private static void initSettingValue(Preference pref_key, 
			SharedPreferences shared_prefs, String key_string) {
		if (!checkSchedulerSettings(pref_key,shared_prefs, key_string,mContext)) 
    	if (!checkLogSettings(pref_key,shared_prefs, key_string,mContext))
		if (!checkMiscSettings(pref_key,shared_prefs, key_string,mContext))			
			checkOtherSettings(pref_key,shared_prefs, key_string,mContext);
	};
		
 
    @Override
    public void onStart(){
        super.onStart();
        mUtilMain.addDebugMsg(1,"I","onStart entered");
    };
 
    @Override
    public void onResume(){
        super.onResume();
        mUtilMain.addDebugMsg(1,"I","onResume entered");
		setTitle(R.string.settings_main_title);
        if (Build.VERSION.SDK_INT<=10) {
    	    mPrefAct.getPreferenceScreen().getSharedPreferences()
    			.registerOnSharedPreferenceChangeListener(listenerBeforeHc);  
        } else {
//    	    mPrefFrag.getPreferenceScreen().getSharedPreferences()
//    			.registerOnSharedPreferenceChangeListener(listenerAfterHc);  
        }
    };
 
    @Override
    public void onBuildHeaders(List<Header> target) {
    	initEnvParms(this,false);
    	mUtilMain.addDebugMsg(1,"I","onBuildHeaders entered");
        loadHeadersFromResource(R.xml.settings_frag, target);
    };

//    @Override
//    public boolean isMultiPane () {
//    	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"isMultiPane entered");
//        return true;
//    };

    @Override
    public boolean onIsMultiPane () {
    	initEnvParms(this,false);
    	mUtilMain.addDebugMsg(1,"I","onIsMultiPane entered");
        return true;
    };

	@Override  
	protected void onPause() {  
	    super.onPause();  
	    mUtilMain.addDebugMsg(1,"I","onPause entered");
        if (Build.VERSION.SDK_INT<=10) {
    	    mPrefAct.getPreferenceScreen().getSharedPreferences()
    			.unregisterOnSharedPreferenceChangeListener(listenerBeforeHc);  
        } else {
//    	    mPrefFrag.getPreferenceScreen().getSharedPreferences()
//    			.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);  
        }
	};

	@Override
	final public void onStop() {
		super.onStop();
		mUtilMain.addDebugMsg(1,"I","onStop entered");
	};

	@Override
	final public void onDestroy() {
		super.onDestroy();
		mUtilMain.addDebugMsg(1,"I","onDestroy entered");
	};

	static Preference pref_proximity=null;

	private SharedPreferences.OnSharedPreferenceChangeListener listenerBeforeHc =   
		    new SharedPreferences.OnSharedPreferenceChangeListener() {  
		    public void onSharedPreferenceChanged(SharedPreferences shared_pref, 
		    		String key_string) {
		    	Preference pref_key=mPrefAct.findPreference(key_string);
		    	
		    	if (!checkSchedulerSettings(pref_key,shared_pref, key_string,mContext)) 
	    		if (!checkLogSettings(pref_key,shared_pref, key_string,mContext))
   				if (!checkMiscSettings(pref_key,shared_pref, key_string,mContext))   					
		    		checkOtherSettings(pref_key,shared_pref, key_string,mContext);
		    }
	};
	
	private static SharedPreferences.OnSharedPreferenceChangeListener listenerAfterHc =   
		    new SharedPreferences.OnSharedPreferenceChangeListener() {  
		    public void onSharedPreferenceChanged(SharedPreferences shared_pref, 
		    		String key_string) {
		    	Preference pref=mPrefFrag.findPreference(key_string);
		    	
		    	if (!checkSchedulerSettings(pref,shared_pref, key_string,mContext)) 
	    		if (!checkLogSettings(pref,shared_pref, key_string,mContext))
				if (!checkMiscSettings(pref,shared_pref, key_string,mContext))		    				
  					checkOtherSettings(pref,shared_pref, key_string,mContext);
		    }
	};


	private static boolean checkSchedulerSettings(final Preference pref_key, 
			final SharedPreferences shared_pref, final String key_string, final Context c) {
		boolean isChecked = false;
    	if (key_string.equals(c.getString(R.string.settings_main_device_admin))) {
    		isChecked=true;
    	} else if (key_string.equals(c.getString(R.string.settings_main_scheduler_sleep_wake_lock_option))) {
    		isChecked=true;
    		String wl_option=shared_pref.getString(c.getString(R.string.settings_main_scheduler_sleep_wake_lock_option), "");
    		if (wl_option.equals("")) {
    			shared_pref.edit().putString(c.getString(R.string.settings_main_scheduler_sleep_wake_lock_option), WAKE_LOCK_OPTION_SYSTEM);
    			wl_option=WAKE_LOCK_OPTION_SYSTEM;
    		}
    		String[] wl_label= c.getResources().getStringArray(R.array.settings_main_scheduler_sleep_wake_lock_option_list_entries);
    		int pos=Integer.parseInt(wl_option);
    		if (pos<wl_label.length) pref_key.setSummary(wl_label[pos]);
    		else pref_key.setSummary(wl_label[0]);

    		if (shared_pref.getString(c.getString(R.string.settings_main_scheduler_sleep_wake_lock_option), WAKE_LOCK_OPTION_SYSTEM).equals(WAKE_LOCK_OPTION_DISCREATE)) {
    			pref_proximity.setEnabled(mUtilMain.isProximitySensorAvailable()!=null);
    		} else {
    			pref_proximity.setEnabled(false);
    		}
    	} else if (key_string.equals(c.getString(R.string.settings_main_scheduler_rssi_criteria))) {
    		isChecked=true;
    		String wl_option=shared_pref.getString(c.getString(R.string.settings_main_scheduler_rssi_criteria), "");
    		if (wl_option.equals("")) {
    			shared_pref.edit().putString(c.getString(R.string.settings_main_scheduler_rssi_criteria), "0");
    			wl_option="0";
    		}
    		String[] wl_label= c.getResources().getStringArray(R.array.settings_main_scheduler_rssi_criteria_list_entries);
    		if (wl_option.equals("0")) pref_key.setSummary(wl_label[0]);
    		else if (wl_option.equals("-60")) pref_key.setSummary(wl_label[1]);
    		else if (wl_option.equals("-80")) pref_key.setSummary(wl_label[2]);
    		else if (wl_option.equals("-90")) pref_key.setSummary(wl_label[3]);
    	} else if (key_string.equals(c.getString(R.string.settings_main_scheduler_sleep_wake_lock_proximity_sensor))) {
    		isChecked=true;
    	} else if (key_string.equals(c.getString(R.string.settings_main_use_light_theme))) {
    		isChecked=true;
    	} else if (key_string.equals(c.getString(R.string.settings_main_force_use_trust_device))) {
    		isChecked=true;
//    		boolean use_trust_dev=shared_pref.getBoolean(c.getString(R.string.settings_main_force_use_trust_device), false);
    	}
    	return isChecked;
	};
	
	private static boolean checkMiscSettings(Preference pref_key, 
			SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = false;
    	if (key_string.equals(c.getString(R.string.settings_main_exit_clean))) {
    		isChecked=true;
    	}
    	return isChecked;
	};

	private static boolean checkLogSettings(Preference pref_key, 
			SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = false;
    	if (key_string.equals(c.getString(R.string.settings_main_log_dir))) {
    		isChecked=true;
    		pref_key.setSummary(shared_pref.getString(key_string,""));
//						Environment.getExternalStorageDirectory().toString()+
//						"/"+APPLICATION_TAG+"/"));
    	} else if (key_string.equals(c.getString(R.string.settings_main_log_option))) {
    		isChecked=true;
    	} else if (key_string.equals(c.getString(R.string.settings_main_log_file_max_count))) {
    		isChecked=true;
    		pref_key.setSummary(c.getString(R.string.settings_main_log_file_max_count_summary)+
    				shared_pref.getString(key_string,"10"));
    	} 

    	return isChecked;
	};

	private static boolean checkOtherSettings(Preference pref_key, 
			SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = true;
//		Log.v("","key="+key_string);
		pref_key.setSummary(
    			c.getString(R.string.settings_main_default_current_setting)+
	    		shared_pref.getString(key_string, "0"));    	
    	return isChecked;
	};

 
    public static class SettingsSceduler extends PreferenceFragment {
    	private static CommonUtilities mUtilScheduler=null;
        @Override
        public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);
            mPrefFrag=this;
            mContext=this.getActivity().getApplicationContext();
            initEnvParms(mContext,false);
            GlobalParameters gp=new GlobalParameters();
            gp.setLogParms(mEnvParms);
            mUtilScheduler=new CommonUtilities(mContext.getApplicationContext(), "SettingsScheduler",mEnvParms, gp);
        	mUtilScheduler.addDebugMsg(1,"I","onCreate entered");
            
    		PreferenceManager pm=getPreferenceManager();
    		pm.setSharedPreferencesName(DEFAULT_PREFS_FILENAME);
    		pm.setSharedPreferencesMode(Context.MODE_PRIVATE|Context.MODE_MULTI_PROCESS);

    		addPreferencesFromResource(R.xml.settings_frag_scheduler);

//    		getActivity().setTitle("TaskAutomation設定");
    		
    		SharedPreferences shared_pref=pm.getSharedPreferences();
    		
			pref_proximity=mPrefFrag.findPreference(mContext.getString(R.string.settings_main_scheduler_sleep_wake_lock_proximity_sensor));

        	if (!LocalMountPoint.isExternalStorageAvailable()) {
        		findPreference(getString(R.string.settings_main_log_dir).toString())
        			.setEnabled(false);
        	}
    		
        	if (Build.VERSION.SDK_INT!=17) {
        		findPreference(getString(R.string.settings_main_force_use_trust_device).toString())
    			.setEnabled(false);
        	}

    		boolean admin=isDeviceAdminActive(mContext);
    		shared_pref.edit().putBoolean(
    				getString(R.string.settings_main_device_admin), admin).commit();

    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_main_device_admin));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_main_scheduler_sleep_wake_lock_option));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_main_scheduler_sleep_wake_lock_proximity_sensor));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_main_scheduler_rssi_criteria));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_main_use_light_theme));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_main_force_use_trust_device));
    		
   		};
        
        @Override
        public void onStart() {
        	super.onStart();
        	mUtilScheduler.addDebugMsg(1,"I","onStart entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.registerOnSharedPreferenceChangeListener(listenerAfterHc);
//    		getActivity().setTitle(R.string.settings_main_title);
        };
        @Override
        public void onStop() {
        	super.onStop();
        	mUtilScheduler.addDebugMsg(1,"I","onStop entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);  
        };
    };
    
    public static class SettingsLog extends PreferenceFragment {
    	private static CommonUtilities mUtilLog=null;
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mPrefFrag=this;
            mContext=this.getActivity().getApplicationContext();
            initEnvParms(mContext,false);
            GlobalParameters gp=new GlobalParameters();
            gp.setLogParms(mEnvParms);
            mUtilLog=new CommonUtilities(mContext.getApplicationContext(), "SettingsLog",mEnvParms, gp);
            mUtilLog.addDebugMsg(1,"I","onCreate entered");
            
    		PreferenceManager pm=getPreferenceManager();
    		pm.setSharedPreferencesName(DEFAULT_PREFS_FILENAME);
    		pm.setSharedPreferencesMode(Context.MODE_PRIVATE|Context.MODE_MULTI_PROCESS);

            addPreferencesFromResource(R.xml.settings_frag_log);

    		SharedPreferences shared_pref=pm.getSharedPreferences();

        	if (!LocalMountPoint.isExternalStorageAvailable()) {
        		findPreference(getString(R.string.settings_main_log_dir).toString())
        			.setEnabled(false);
        	}
    		
    		boolean admin=isDeviceAdminActive(mContext);
    		shared_pref.edit().putBoolean(
    				getString(R.string.settings_main_device_admin), admin).commit();

    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_main_log_dir));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_main_log_option));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_main_log_file_max_count));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_main_log_level));
        };
        
        @Override
        public void onStart() {
        	super.onStart();
        	mUtilLog.addDebugMsg(1,"I","onStart entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.registerOnSharedPreferenceChangeListener(listenerAfterHc);
//    	    getActivity().setTitle(R.string.settings_main_title);
        };
        @Override
        public void onStop() {
        	super.onStop();
        	mUtilLog.addDebugMsg(1,"I","onStop entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);  
        };

    };

    public static class SettingsMisc extends PreferenceFragment {
    	private static CommonUtilities mUtilMisc=null;
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mPrefFrag=this;
            mContext=this.getActivity().getApplicationContext();
            initEnvParms(mContext,false);
            GlobalParameters gp=new GlobalParameters();
            gp.setLogParms(mEnvParms);
            mUtilMisc=new CommonUtilities(mContext.getApplicationContext(), "SettingsMisc",mEnvParms, gp);
            mUtilMisc.addDebugMsg(1,"I","onCreate entered");

    		PreferenceManager pm=getPreferenceManager();
    		pm.setSharedPreferencesName(DEFAULT_PREFS_FILENAME);
    		pm.setSharedPreferencesMode(Context.MODE_PRIVATE|Context.MODE_MULTI_PROCESS);

            addPreferencesFromResource(R.xml.settings_frag_misc);

            mPrefFrag=this;
            mContext=this.getActivity().getApplicationContext();

    		SharedPreferences shared_pref=pm.getSharedPreferences();

        	if (!LocalMountPoint.isExternalStorageAvailable()) {
        		findPreference(getString(R.string.settings_main_log_dir).toString())
        			.setEnabled(false);
        	}
    		
    		boolean admin=isDeviceAdminActive(mContext);
    		shared_pref.edit().putBoolean(
    				getString(R.string.settings_main_device_admin), admin).commit();

    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_main_exit_clean));
        };
        
        @Override
        public void onStart() {
        	super.onStart();
        	mUtilMisc.addDebugMsg(1,"I","onStart entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.registerOnSharedPreferenceChangeListener(listenerAfterHc);
//    	    getActivity().setTitle(R.string.settings_main_title);
        };
        @Override
        public void onStop() {
        	super.onStop();
        	mUtilMisc.addDebugMsg(1,"I","onStop entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);  
        };
    };

}