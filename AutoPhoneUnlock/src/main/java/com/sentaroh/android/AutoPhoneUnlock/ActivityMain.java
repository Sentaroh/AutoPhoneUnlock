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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.ArrayList;

import com.sentaroh.android.AutoPhoneUnlock.ISchedulerCallback;
import com.sentaroh.android.AutoPhoneUnlock.ISchedulerClient;
import com.sentaroh.android.AutoPhoneUnlock.Log.LogFileListDialogFragment;
import com.sentaroh.android.Utilities.LocalMountPoint;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;
import com.sentaroh.android.Utilities.StringUtil;
import com.sentaroh.android.Utilities.ThemeUtil;
import com.sentaroh.android.Utilities.ContextButton.ContextButtonUtil;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;
import com.sentaroh.android.Utilities.Dialog.MessageDialogFragment;
import com.sentaroh.android.Utilities.Dialog.PasswordInputDialogFragment;
import com.sentaroh.android.Utilities.Widget.CustomTabContentView;
import com.sentaroh.android.Utilities.Widget.CustomViewPager;
import com.sentaroh.android.Utilities.Widget.CustomViewPagerAdapter;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;
import android.widget.TabHost.TabSpec;
import android.widget.Toast;

@SuppressLint("NewApi")
public class ActivityMain extends AppCompatActivity {

	private static boolean DEBUG_ENABLE=true;

	private GlobalParameters mGp=null;
	private boolean mIsApplicationTerminated=false;
	
	private String mApplicationVersion="";
	
	private static int mRestartStatus=0;
	
	private EnvironmentParms mEnvParms=null;
	
	private CommonUtilities mUtil=null;

	private CommonDialog commonDlg=null;
	
	private TabHost mMainTabHost=null;
	
	private Context mContext=null;
	private Activity mActivity=null;
	
	private FragmentManager mFragmentManager=null;
	
	private ISchedulerCallback mSvcClientCallback=null;
	private ServiceConnection mSvcConnScheduler=null;
	private ISchedulerClient mSvcServer=null;

	private boolean mApplicationRunFirstTime=false;
	
	private ActionBar mActionBar=null;
	
	private Handler mUiHandler=null;
	
	private final static int RINGTONE_PICKER_REQUEST_ID_SCREEN_LOCKED=20;
	private final static int RINGTONE_PICKER_REQUEST_ID_SCREEN_UNLOCKED=30;
	private final static int PLACE_PICKER_REQUEST_ID=40;

	@Override  
	final protected void onSaveInstanceState(Bundle outState) {  
		super.onSaveInstanceState(outState);
		if (DEBUG_ENABLE) mUtil.addDebugMsg(1,"I","onSaveInstanceState entered");
	};  
	  
	@Override  
	final protected void onRestoreInstanceState(Bundle savedState) {  
		super.onRestoreInstanceState(savedState);
		if (DEBUG_ENABLE) mUtil.addDebugMsg(1,"I","onRestoreInstanceState entered");
		mRestartStatus=2;
	};
	
//	private void getOverflowMenu() {
//		http://stackoverflow.com/questions/9739498/android-action-bar-not-showing-overflow	
//	     try {
//	        ViewConfiguration config = ViewConfiguration.get(this);
//	        Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
//	        if(menuKeyField != null) {
//	            menuKeyField.setAccessible(true);
//	            menuKeyField.setBoolean(config, false);
//	        }
//	    } catch (Exception e) {
//	        e.printStackTrace();
//	    }
//	};

    @SuppressLint("NewApi")
	@Override
    final public void onCreate(Bundle savedInstanceState) {
//    	StrictMode.enableDefaults();
//        if (Build.VERSION.SDK_INT<=10) 
//        	requestWindowFeature(Window.FEATURE_NO_TITLE); 
        
        mContext=this;
        mActivity=this;
        mEnvParms=new EnvironmentParms();
		mEnvParms.loadSettingParms(mContext);
		mEnvParms.loadControlOption(mContext);
        mFragmentManager=getSupportFragmentManager();
        mGp=(GlobalParameters) this.getApplication();
        mGp.envParms=mEnvParms;
        mGp.setLogParms(mEnvParms);
		if (mEnvParms.settingUseLightTheme) mGp.applicationTheme=R.style.MainLight;
		else mGp.applicationTheme=R.style.Main;
		setTheme(mGp.applicationTheme);

        super.onCreate(savedInstanceState);

		mGp.themeColorList=ThemeUtil.getThemeColorList(mActivity);

        mApplicationRunFirstTime=initSettingParms();
        
        setContentView(R.layout.activity_main);
        
		mActionBar = getSupportActionBar();
//		mActionBar.setDisplayShowHomeEnabled(false);
		mActionBar.setDisplayHomeAsUpEnabled(false);
		mActionBar.setHomeButtonEnabled(false);
		
		mUiHandler=new Handler();
		
        mRestartStatus=0;
        mApplicationVersion=setApplVersionName();
        if (mEnvParms.settingDebugLevel==0) DEBUG_ENABLE=false;
        else DEBUG_ENABLE=true;
        mUtil=new CommonUtilities(mContext.getApplicationContext(), "Main", mEnvParms, mGp);
        
        if (DEBUG_ENABLE) mUtil.addDebugMsg(1,"I","onCreate entered");
        
        commonDlg=new CommonDialog(mContext, getSupportFragmentManager());
        
        mUtil.addDebugMsg(1,"I","initSettingParms "+
				"localRootDir="+mEnvParms.localRootDir+
				", settingDebugLevel="+mEnvParms.settingDebugLevel+
				", settingLogMsgDir="+mEnvParms.settingLogMsgDir+
				", settingLogOption="+mEnvParms.settingLogOption+
				", settingExitClean="+mEnvParms.settingExitClean);
		mUtil.addDebugMsg(1, "I", "Android SDK="+Build.VERSION.SDK_INT);
        createMainTabView();
        mUtil.startScheduler();
        if (mEnvParms.settingDeviceAdmin) switchDeviceAdminStatus(mEnvParms.settingDeviceAdmin);
        
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			mEnvParms.isBluetoothLeSupported=false;
			mUtil.addDebugMsg(1, "W", "Bluetooth low energy is Not supprted");
		} else {
			mEnvParms.isBluetoothLeSupported=true;
			
//			LocationManager mLocationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);
//			if(!mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) &&
//					!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))   {
//				mUtil.addDebugMsg(1, "I", "Location is disabled");
//		    } 

		}
   };

	@Override
	final public void onStart() {
		super.onStart();
		if (DEBUG_ENABLE) mUtil.addDebugMsg(1,"I","onStart entered");
	};

	@Override
	final public void onRestart() {
		super.onStart();
		if (DEBUG_ENABLE) mUtil.addDebugMsg(1,"I","onRestart entered");
	};
	
	@Override
	final public void onResume() {
		super.onResume();
		if (DEBUG_ENABLE) mUtil.addDebugMsg(1,"I","onResume entered, restartStatus="+mRestartStatus);

		if (mRestartStatus!=1) {
			NotifyEvent ntfy = new NotifyEvent(this);
			ntfy.setListener(new NotifyEventListener() {
				@Override
				public void positiveResponse(Context c, Object[] o) {
					if (mEnvParms.trustedDeviceKeyguardLockPassword==null) {
						mEnvParms.trustedDeviceKeyguardLockPassword="";
						mEnvParms.clearDpmPassword(mContext);
						commonDlg.showCommonDialog(false, "W",
								mContext.getString(R.string.msgs_trust_device_screen_lock_pswd_delete_title),
								mContext.getString(R.string.msgs_trust_device_screen_lock_pswd_invalid_msg),
								null);
					}
					if (mRestartStatus==0) {
						mUtil.addLogMsg("I",String.format(
								getString(R.string.msgs_main_started), mApplicationVersion));
					} else if (mRestartStatus==2) {
						mUtil.addLogMsg("I",getString(R.string.msgs_main_restarted));
						commonDlg.showCommonDialog(false, "W",
								getString(R.string.msgs_main_restarted), "", null);
						restoreTaskData();
						deleteTaskData();
					}
					
					setControllerStatus();
					setTabTrustView();
					setTabScreenView();
					setTabControlView();
					
					if (mRestartStatus==0) checkLocationSetting(loadTrustItemList());
					
//					refreshOptionMenu();

					mRestartStatus=1;
//					BtLeUtil.startAdvertize(mContext, mUtil, new BtLeCommonArea());
				}
				@Override
				public void negativeResponse(Context c, Object[] o) {}
			});
			bindSchedulerService(ntfy);
		} else {
			if (mAdapterTrustList.isShowCheckBox()) setTrustContextButtonSelectMode(mAdapterTrustList);
			else setTrustContextButtonNormalMode(mAdapterTrustList);
			deleteTaskData();
			checkLocationSetting(loadTrustItemList());
		}
	};
	
	@Override
	final public void onPause() {
		super.onPause();
		if (DEBUG_ENABLE) mUtil.addDebugMsg(1,"I","onPause entered");
		
		if (!mIsApplicationTerminated) saveTaskData(); 
	};

	@Override
	final public void onStop() {
		super.onStop();
		if (DEBUG_ENABLE) mUtil.addDebugMsg(1,"I","onStop entered");
	};

	@Override
	final public void onDestroy() {
		super.onDestroy();
		if (DEBUG_ENABLE) mUtil.addDebugMsg(1,"I","onDestroy entered");
		
        // Application process is follow
		
		if (mIsApplicationTerminated) {
			unbindScheduler();
			mIsApplicationTerminated=false;
			deleteTaskData();
			
//			mGp.clearParms();
//			mGp=null;
//			mMainTabHost=null;
//			mContext=null;
//			mActivity=null;
//			mSvcClientCallback=null;
//			mSvcConnScheduler=null;
//			mSvcServer=null;

			if (mEnvParms.settingExitClean) {
//				System.gc();
				android.os.Process.killProcess(android.os.Process.myPid());
			} else {
//				mEnvParms=null;
				System.gc();
			}
		} else {
			unbindScheduler();
		}
	};
	
	@Override
	final public void onConfigurationChanged(final Configuration newConfig) {
	    // Ignore orientation change to keep activity from restarting
	    super.onConfigurationChanged(newConfig);
	    mUtil.addDebugMsg(1,"I","onConfigurationChanged Entered");
	    
	    setTrustItemListView();
	    
	    refreshOptionMenu();
	};
	
	@Override
	final public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_top, menu);
		return true;
	};
	
	@Override
	final public boolean onPrepareOptionsMenu(Menu menu) {
	    if (mEnvParms.settingDebugLevel>=1) {
	    	mUtil.addDebugMsg(1,"I","onPrepareOptionsMenu Entered");
//	    	Thread.dumpStack();
	    }
		menu.findItem(R.id.menu_top_toggle_controller).setVisible(true);
		if (mEnvParms.settingEnableScheduler) {
			menu.findItem(R.id.menu_top_toggle_controller).setTitle(R.string.msgs_menu_toggle_control_stop);
			menu.findItem(R.id.menu_top_toggle_controller).setIcon(R.drawable.main_stop_icon_64);
		} else {
			menu.findItem(R.id.menu_top_toggle_controller).setTitle(R.string.msgs_menu_toggle_control_start);
			menu.findItem(R.id.menu_top_toggle_controller).setIcon(R.drawable.main_icon_64);
		}
		menu.findItem(R.id.menu_top_log_manage_log).setVisible(true);
		menu.findItem(R.id.menu_top_browse_logfile).setVisible(true);
		menu.findItem(R.id.menu_top_settings).setVisible(true);
		menu.findItem(R.id.menu_top_about).setVisible(true);
		menu.findItem(R.id.menu_top_uninstall).setVisible(true);
		menu.findItem(R.id.menu_top_backup_settings).setVisible(true);
		menu.findItem(R.id.menu_top_restore_settings).setVisible(true);
		menu.findItem(R.id.menu_top_restart_scheduler).setVisible(true);
		if (BluetoothAdapter.getDefaultAdapter()!=null && BluetoothAdapter.getDefaultAdapter().isEnabled()) {
			menu.findItem(R.id.menu_top_scan_ble).setVisible(mEnvParms.isBluetoothLeSupported);
		} else {
			menu.findItem(R.id.menu_top_scan_ble).setVisible(false);
		}
		if (LocalMountPoint.isExternalStorageAvailable()) {
			if (!mUtil.isLogFileExists()) {
				menu.findItem(R.id.menu_top_browse_logfile).setVisible(false);
			}
		} else {
			menu.findItem(R.id.menu_top_browse_logfile).setVisible(false);
		}
		if (mEditModeEnabled) {
			menu.findItem(R.id.menu_top_toggle_controller).setVisible(false);
			menu.findItem(R.id.menu_top_log_manage_log).setVisible(false);
			menu.findItem(R.id.menu_top_browse_logfile).setVisible(false);
			menu.findItem(R.id.menu_top_settings).setVisible(false);
			menu.findItem(R.id.menu_top_about).setVisible(true);
			menu.findItem(R.id.menu_top_uninstall).setVisible(false);
			menu.findItem(R.id.menu_top_backup_settings).setVisible(false);
			menu.findItem(R.id.menu_top_restore_settings).setVisible(false);
			menu.findItem(R.id.menu_top_scan_ble).setVisible(false);
			menu.findItem(R.id.menu_top_restart_scheduler).setVisible(false);
		}
		super.onPrepareOptionsMenu(menu);
        return true;
	};
	
	@SuppressLint("SdCardPath")
	@Override
	final public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				processHomeButtonPress();
				return true;
			case R.id.menu_top_toggle_controller:
				confirmController();
				return true;			
			case R.id.menu_top_settings:
				invokeSettingsActivity();
				return true;			
			case R.id.menu_top_backup_settings:
				backupSettings();
				return true;			
			case R.id.menu_top_restore_settings:
				restoreSettings();
				return true;			
			case R.id.menu_top_browse_logfile:
				invokeLogFileBrowser();
				return true;			
			case R.id.menu_top_scan_ble:
				BtLeDeviceScanDlg rsc=new BtLeDeviceScanDlg(this, mGp, mEnvParms, mUtil);
				rsc.scanDialog(null, "","");
				return true;			
			case R.id.menu_top_log_manage_log:
				invokeLogManagement();
				return true;			
			case R.id.menu_top_restart_scheduler:
				restartScheduler();
				return true;			
			case R.id.menu_top_about:
				aboutApp();
				return true;			
			case R.id.menu_top_uninstall:
				uninstallApplication();
				return true;			
		}
		return false;
	};

	private void restartScheduler() {
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				mUtil.restartScheduler();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
		});
		commonDlg.showCommonDialog(true, "W", 
				mContext.getString(R.string.msgs_menu_restart_scheduler), 
				mContext.getString(R.string.msgs_menu_restart_scheduler_desc), ntfy);
	};
	
	private void backupSettings() {
		File lf=new File( 
				LocalMountPoint.getExternalStorageDir()+"/"+APPLICATION_TAG+"/settings.txt");
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				CommonUtilities.saveSettingsParmsToFile(mContext, 
						LocalMountPoint.getExternalStorageDir()+"/"+APPLICATION_TAG, "settings.txt");
				commonDlg.showCommonDialog(false, "W", 
						mContext.getString(R.string.msgs_main_backup_success), "", null);
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
				
			}
		});
		
		if (!lf.exists()) {
			ntfy.notifyToListener(true, null);
		} else {
			commonDlg.showCommonDialog(true, "W", 
					mContext.getString(R.string.msgs_main_backup_confirm_override_file), "", ntfy);
		}
	};
	
	private void restoreSettings() {
		File lf=new File( 
				LocalMountPoint.getExternalStorageDir()+"/"+APPLICATION_TAG+"/settings.txt");
		if (lf.exists()) {
			long sd=CommonUtilities.getSettingsParmSaveDate(mContext, 
					LocalMountPoint.getExternalStorageDir()+"/"+APPLICATION_TAG, "settings.txt");
			String str_sd=StringUtil.convDateTimeTo_YearMonthDayHourMin(sd);
			String conf_msg=String.format(mContext.getString(R.string.msgs_main_restore_confirm_msg), str_sd);
			
			NotifyEvent ntfy=new NotifyEvent(mContext);
			ntfy.setListener(new NotifyEventListener(){
				@Override
				public void positiveResponse(Context c, Object[] o) {
					CommonUtilities.loadSettingsParmFromFile(mContext, 
							LocalMountPoint.getExternalStorageDir()+"/"+APPLICATION_TAG, "settings.txt");
					applySettingParms();
					Handler hndl=new Handler();
					hndl.postDelayed(new Runnable(){
						@Override
						public void run() {
//							mTrustItemList=CommonUtilities.loadTrustedDeviceTable(mContext, mEnvParms);
							setControllerStatus();
							setTabTrustView();
							setTabScreenView();
							setTabControlView();
							setTrustViewEditMode(false);
							refreshOptionMenu();
						}
					},200);
					commonDlg.showCommonDialog(false, "W", 
							mContext.getString(R.string.msgs_main_restore_success), "", null);
				}
				@Override
				public void negativeResponse(Context c, Object[] o) {
				}
			});
			commonDlg.showCommonDialog(true, "W", conf_msg, "", ntfy);
		} else {
			commonDlg.showCommonDialog(false, "W", 
					mContext.getString(R.string.msgs_main_restore_file_not_found), "", null);
		}
	};
	
	private void invokeLogManagement() {
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				boolean enabled=(Boolean)o[0];
				mUtil.setSettingsLogOption(enabled);
				applySettingParms();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {}
		});
		mUtil.resetLogReceiver();
		LogFileListDialogFragment lfmf=LogFileListDialogFragment.newInstance(true,
				mContext.getString(R.string.msgs_log_file_list_title));
		lfmf.showDialog(mFragmentManager, lfmf, mGp, ntfy);
	};
	
	@SuppressLint("NewApi")
	private void confirmController() {
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				if (mEnvParms.settingEnableScheduler) {
					getPrefsMgr().edit().putBoolean(SETTING_ENABLE_SCHEDULER_KEY,false).commit();
					CommonUtilities.restartScheduler(mContext); 
					mEnvParms.settingEnableScheduler=false;
				} else {
					getPrefsMgr().edit().putBoolean(SETTING_ENABLE_SCHEDULER_KEY,true).commit();
					CommonUtilities.restartScheduler(mContext);
					mEnvParms.settingEnableScheduler=true;
				}
				setControllerStatus();
				refreshOptionMenu();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
		});
		if (mEnvParms.settingEnableScheduler) {
			commonDlg.showCommonDialog(true, "W", 
					mContext.getString(R.string.msgs_menu_toggle_confirm_control_stop), 
					mContext.getString(R.string.msgs_menu_toggle_confirm_control_stop_desc), ntfy);
		} else {
			commonDlg.showCommonDialog(true, "W", 
					mContext.getString(R.string.msgs_menu_toggle_confirm_control_start), "", ntfy);
		}
	};
	
	private void processHomeButtonPress() {
		mAdapterTrustList.setShowCheckBox(false);
		mAdapterTrustList.setAllItemSelected(false);
		mAdapterTrustList.notifyDataSetChanged();
		setTrustContextButtonNormalMode(mAdapterTrustList);
		setActionBarNormalMode();
	};
	
	final private void uninstallApplication() {
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				switchDeviceAdminStatus(false);
				getPrefsMgr().edit().putBoolean(getString(R.string.settings_main_device_admin),false).commit();
				
				if (!mEnvParms.trustedDeviceKeyguardLockPassword.equals("") && mUtil.isDevicePolicyManagerActive()) {
					CommonUtilities.removePassword(mContext);
				}
				
				Uri uri=Uri.fromParts("package",getPackageName(),null);
				Intent intent=new Intent(Intent.ACTION_DELETE,uri);
				startActivity(intent);
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {}
		});
		if (mEnvParms.settingDeviceAdmin) {
			commonDlg.showCommonDialog(true, "W",getString(R.string.msgs_menu_uninstall_subtitle), 
					getString(R.string.msgs_menu_uninstall_message), ntfy);
		} else ntfy.notifyToListener(true, null);
	};
	
	@SuppressLint("NewApi")
	final private void refreshOptionMenu() {
		if (Build.VERSION.SDK_INT>=11) {
			mActivity.invalidateOptionsMenu();
//			Thread.dumpStack();
		}
	};
	
	private CustomViewPagerAdapter mAboutViewPagerAdapter;
	private CustomViewPager mAboutViewPager;
	private TabHost mAboutTabHost ;
	private TabWidget mAboutTabWidget ;
	@SuppressWarnings("deprecation")
	@SuppressLint({ "NewApi", "InflateParams" })
	final private void aboutApp() {
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
	    dialog.setContentView(R.layout.about_dialog);

		final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.about_dialog_title_view);
		final TextView title = (TextView) dialog.findViewById(R.id.about_dialog_title);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		title.setTextColor(mGp.themeColorList.text_color_dialog_title);
		title.setText(getString(R.string.msgs_about_dlg_title)+" Ver "+getApplVersionName());
		
        // get our tabHost from the xml
        mAboutTabHost = (TabHost)dialog.findViewById(R.id.about_tab_host);
        mAboutTabHost.setup();
        
        mAboutTabWidget = (TabWidget)dialog.findViewById(android.R.id.tabs);
		 
		if (Build.VERSION.SDK_INT>=11) {
		    mAboutTabWidget.setStripEnabled(false); 
		    mAboutTabWidget.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);  
		}

		CustomTabContentView tabViewProf = new CustomTabContentView(this,getString(R.string.msgs_about_dlg_func_btn));
		mAboutTabHost.addTab(mAboutTabHost.newTabSpec("func").setIndicator(tabViewProf).setContent(android.R.id.tabcontent));
		
		CustomTabContentView tabViewHist = new CustomTabContentView(this,getString(R.string.msgs_about_dlg_change_btn));
		mAboutTabHost.addTab(mAboutTabHost.newTabSpec("change").setIndicator(tabViewHist).setContent(android.R.id.tabcontent));

        LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout ll_func=(LinearLayout)vi.inflate(R.layout.about_dialog_func,null);
        LinearLayout ll_change=(LinearLayout)vi.inflate(R.layout.about_dialog_change,null);

		final WebView func_view=(WebView)ll_func.findViewById(R.id.about_dialog_function);
		func_view.loadUrl("file:///android_asset/"+getString(R.string.msgs_about_dlg_func_html));
		func_view.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		func_view.getSettings().setBuiltInZoomControls(true);
		
		final WebView change_view=
				(WebView)ll_change.findViewById(R.id.about_dialog_change_history);
		change_view.loadUrl("file:///android_asset/"+getString(R.string.msgs_about_dlg_change_html));
		change_view.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		change_view.getSettings().setBuiltInZoomControls(true);
		
		mAboutViewPagerAdapter=new CustomViewPagerAdapter(this, 
	    		new WebView[]{func_view, change_view});
		mAboutViewPager=(CustomViewPager)dialog.findViewById(R.id.about_view_pager);
//	    mMainViewPager.setBackgroundColor(mThemeColorList.window_color_background);
		mAboutViewPager.setAdapter(mAboutViewPagerAdapter);
		mAboutViewPager.setOnPageChangeListener(new AboutPageChangeListener()); 

		mAboutTabHost.setOnTabChangedListener(new AboutOnTabChange());
		
		final Button btnOk = (Button) dialog.findViewById(R.id.about_dialog_btn_ok);

		CommonDialog.setDlgBoxSizeLimit(dialog,true);

		// OKボタンの指定
		btnOk.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btnOk.performClick();
			}
		});

		dialog.show();
	};
	
	
	private class AboutOnTabChange implements OnTabChangeListener {
		@Override
		public void onTabChanged(String tabId){
			mUtil.addDebugMsg(2,"I","onTabchanged entered. tab="+tabId);
			mAboutViewPager.setCurrentItem(mAboutTabHost.getCurrentTab());
		};
	};
	
	private class AboutPageChangeListener implements ViewPager.OnPageChangeListener {  
	    @Override  
	    public void onPageSelected(int position) {
//	    	util.addDebugLogMsg(2,"I","onPageSelected entered, pos="+position);
	        mAboutTabWidget.setCurrentTab(position);
	        mAboutTabHost.setCurrentTab(position);
	    }  
	  
	    @Override  
	    public void onPageScrollStateChanged(int state) {  
//	    	util.addDebugLogMsg(2,"I","onPageScrollStateChanged entered, state="+state);
	    }  
	  
	    @Override  
	    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//	    	util.addDebugLogMsg(2,"I","onPageScrolled entered, pos="+position);
	    }  
	};
	
	final private String getApplVersionName() {
		try {
		    String packegeName = getPackageName();
		    PackageInfo packageInfo = getPackageManager().getPackageInfo(packegeName, PackageManager.GET_META_DATA);
		    return packageInfo.versionName;
		} catch (NameNotFoundException e) {
			return "";
		}
	};
	
	private void reloadMainScreen() {
		setContentView(R.layout.activity_main);
		
		mGp.themeColorList=ThemeUtil.getThemeColorList(mActivity);

		int tab_pos=mMainTabHost.getCurrentTab();
		createMainTabView();
		mMainTabHost.setCurrentTab(tab_pos);
		
		setControllerStatus();
		setTabTrustView();
		setTabScreenView();
		setTabControlView();
		refreshOptionMenu();
	};

	private LinearLayout mControlView, mScreenView, mTrustView;

	private TabWidget mMainTabWidget;
	private CustomViewPagerAdapter mMainViewPagerAdapter;
	private CustomViewPager mMainViewPager;
	private TextView mMainMsgScheduler;
	@SuppressWarnings("deprecation")
	@SuppressLint("InflateParams")
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	final private void createMainTabView() {
		mMainTabHost=(TabHost)findViewById(android.R.id.tabhost);
		//getTabHost();
		mMainTabHost.setup();

		mMainTabWidget = (TabWidget) findViewById(android.R.id.tabs);
		 
		if (Build.VERSION.SDK_INT>=11) {
		    mMainTabWidget.setStripEnabled(false);  
		    mMainTabWidget.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);  
		}

		View childview1 = new CustomTabContentView(this,getString(R.string.msgs_main_tab_trust));
		TabSpec tabSpec=mMainTabHost.newTabSpec("Trust").setIndicator(childview1).setContent(android.R.id.tabcontent);
		mMainTabHost.addTab(tabSpec);

		View childview2 = new CustomTabContentView(this,getString(R.string.msgs_main_tab_screen));
		tabSpec=mMainTabHost.newTabSpec("Screen").setIndicator(childview2).setContent(android.R.id.tabcontent);
		mMainTabHost.addTab(tabSpec);

		View childview3 = new CustomTabContentView(this,getString(R.string.msgs_main_tab_control));
		tabSpec=mMainTabHost.newTabSpec("Control").setIndicator(childview3).setContent(android.R.id.tabcontent);
		mMainTabHost.addTab(tabSpec);

		LinearLayout ll_main=(LinearLayout)findViewById(R.id.main_view);
		ll_main.setBackgroundColor(mGp.themeColorList.window_background_color_content);
		mMainMsgScheduler=(TextView)findViewById(R.id.main_view_msg_scheduler);
		mMainMsgScheduler.setVisibility(TextView.GONE);
		
        LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mControlView=(LinearLayout)vi.inflate(R.layout.main_control_view,null);
		mControlView.setBackgroundColor(mGp.themeColorList.window_background_color_content);
		mScreenView=(LinearLayout)vi.inflate(R.layout.main_screen_view,null);
		mScreenView.setBackgroundColor(mGp.themeColorList.window_background_color_content);
		mTrustView=(LinearLayout)vi.inflate(R.layout.main_trust_view,null);
		mTrustView.setBackgroundColor(mGp.themeColorList.window_background_color_content);


//		if (isFirstStart) tabHost.setCurrentTab(0);
		mMainTabHost.setOnTabChangedListener(new OnTabChangeListener(){
			@Override
			public void onTabChanged(String tabId){
				if (DEBUG_ENABLE) 
					mUtil.addDebugMsg(1,"I","onTabchanged entered. tab="+tabId);
				mMainViewPager.setCurrentItem(mMainTabHost.getCurrentTab());
				
				if (mAdapterTrustList!=null) {
					mAdapterTrustList.setShowCheckBox(false);
					mAdapterTrustList.setAllItemSelected(false);
					mAdapterTrustList.notifyDataSetChanged();
					setTrustContextButtonNormalMode(mAdapterTrustList);
					setActionBarNormalMode();
				}
			};
		});
	    mMainViewPagerAdapter=new CustomViewPagerAdapter(this, 
	    		new View[]{mTrustView, mScreenView, mControlView});
	    mMainViewPager=(CustomViewPager)findViewById(R.id.main_view_pager);
//	    mMainViewPager.setBackgroundColor(mThemeColorList.window_color_background);
	    mMainViewPager.setAdapter(mMainViewPagerAdapter);
	    mMainViewPager.setOnPageChangeListener(new MainPageChangeListener()); 
		if (mRestartStatus==0) {
			mMainTabHost.setCurrentTab(0);
			mMainViewPager.setCurrentItem(0);
		}
	};
	
	private void setControllerStatus() {
		if (mEnvParms.settingEnableScheduler) {
			mMainMsgScheduler.setVisibility(TextView.GONE);
		} else {
			mMainMsgScheduler.setVisibility(TextView.VISIBLE);
			mMainMsgScheduler.setTextColor(mGp.themeColorList.text_color_error);
			mMainMsgScheduler.setText(mContext.getString(R.string.msgs_main_view_scheduler_disabled));
		}
	};
	
	private class MainPageChangeListener implements ViewPager.OnPageChangeListener {  
	    @Override  
	    public void onPageSelected(int position) {
//	    	util.addDebugLogMsg(2,"I","onPageSelected entered, pos="+position);
	        mMainTabWidget.setCurrentTab(position);
	        mMainTabHost.setCurrentTab(position);
	    }  
	  
	    @Override  
	    public void onPageScrollStateChanged(int state) {  
//	    	util.addDebugLogMsg(2,"I","onPageScrollStateChanged entered, state="+state);
	    }  
	  
	    @Override  
	    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//	    	util.addDebugLogMsg(2,"I","onPageScrolled entered, pos="+position);
	    }  
	};  

	final private String setApplVersionName() {
		String ver="";
	    String packegeName = getPackageName();
	    PackageInfo packageInfo;
		try {
			packageInfo = getPackageManager().getPackageInfo(packegeName, PackageManager.GET_META_DATA);
		    ver=packageInfo.versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return ver;
	};

	final private SharedPreferences getPrefsMgr() {
        return CommonUtilities.getPrefMgr(mContext);
    };

    final private boolean initSettingParms() {
		boolean initialized=false;
		if (getPrefsMgr().getString(getString(R.string.settings_main_log_level),"-1").equals("-1")) {
			//first time
			PackageInfo packageInfo;
			String ddl="0";
			try {
				packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
				int flags = packageInfo.applicationInfo.flags;
				if ((flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) ddl="2";

			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}           
			getPrefsMgr().edit().putBoolean(SETTING_ENABLE_SCHEDULER_KEY,true).commit();
			getPrefsMgr().edit().putString(getString(R.string.settings_main_log_level),ddl).commit();
			getPrefsMgr().edit().putBoolean(getString(R.string.settings_main_device_admin),true).commit();
			
			getPrefsMgr().edit().putString(mContext.getString(R.string.settings_main_log_dir),
					Environment.getExternalStorageDirectory().toString()+
					"/"+APPLICATION_TAG+"/").commit();
			
			initialized=true;
		}
		return initialized;
	};

	final private void applySettingParms() {
		boolean p_admin=mEnvParms.settingDeviceAdmin;
//		String p_intvl=mEnvParms.settingSleepOption;
//		boolean p_theme=mEnvParms.settingUseLightTheme;
		boolean p_theme=false;
		boolean p_use_trust_dev=mEnvParms.settingForceUseTrustDevice;
		if (mGp.applicationTheme==R.style.MainLight) p_theme=true;

		
		mEnvParms.loadSettingParms(mContext);
		if (mEnvParms.settingLogMsgDir.equals("")) {
			mEnvParms.settingLogMsgDir=Environment.getExternalStorageDirectory().toString()+
					"/"+APPLICATION_TAG+"/";
			getPrefsMgr().edit().putString(mContext.getString(R.string.settings_main_log_dir),
					mEnvParms.settingLogMsgDir).commit();
		} else {
    		if (!mEnvParms.settingLogMsgDir.endsWith("/")) {
    			mEnvParms.settingLogMsgDir+="/";
    			getPrefsMgr().edit().putString(mContext.getString(R.string.settings_main_log_dir),
    					mEnvParms.settingLogMsgDir).commit();
    		}	
		}
		
        if (mEnvParms.settingDebugLevel==0) DEBUG_ENABLE=false;
        else DEBUG_ENABLE=true;
        
		if (isBooleanDifferent(p_admin,mEnvParms.settingDeviceAdmin)) {
			switchDeviceAdminStatus(mEnvParms.settingDeviceAdmin);
			if (!mEnvParms.settingDeviceAdmin) {
				mUiHandler.post(new Runnable(){
					@Override
					public void run() {
						setTabTrustView();
						setTabScreenView();
						setTabControlView();
					}
				});
			}
		}
		
		if (isBooleanDifferent(p_theme,mEnvParms.settingUseLightTheme)) {
			if (Build.VERSION.SDK_INT>=21) {
				if (mEnvParms.settingUseLightTheme) mGp.applicationTheme=R.style.MainLight;
				else mGp.applicationTheme=R.style.Main;
				setTheme(mGp.applicationTheme);
				reloadMainScreen();
			} else {
				mEnvParms.settingUseLightTheme=p_theme;
				NotifyEvent ntfy=new NotifyEvent(mContext);
				ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						finish();
						Intent in=new Intent(mContext,ActivityMain.class);
						startActivity(in);
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
					}
				});
				commonDlg.showCommonDialog(true, "W", 
						mContext.getString(R.string.msgs_main_theme_changed_msg), "", ntfy);
			}
		}
		
		if (isBooleanDifferent(p_use_trust_dev, mEnvParms.settingForceUseTrustDevice)) {
			if (mEnvParms.settingForceUseTrustDevice) {
				NotifyEvent ntfy=new NotifyEvent(mContext);
				ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
//						mTrustItemList=CommonUtilities.loadTrustedDeviceTable(mContext, mEnvParms);
						setTabTrustView();
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
						mEnvParms.settingForceUseTrustDevice=false;
						mEnvParms.setSettingParmsForceUseTrustDevice(mContext,false);
//						mUtil.restartScheduler();
						mUtil.resetScheduler();
						setTabTrustView();
					}
				});
				commonDlg.showCommonDialog(true, "W", 
						mContext.getString(R.string.settings_main_force_use_trust_device_confirm_title), 
						mContext.getString(R.string.settings_main_force_use_trust_device_confirm_msg),
						ntfy);
			} else {
				setTabTrustView();
			}
		}
		
		mGp.setLogParms(mEnvParms);
		
		mUtil.addDebugMsg(1,"I","initSettingParms ");
		mUtil.addDebugMsg(1,"I","  localRootDir="+mEnvParms.localRootDir);
		mUtil.addDebugMsg(1,"I","  settingDebugLevel="+mEnvParms.settingDebugLevel);
		mUtil.addDebugMsg(1,"I","  settingLogMsgDir="+mEnvParms.settingLogMsgDir);
		mUtil.addDebugMsg(1,"I","  settingLogOption="+mEnvParms.settingLogOption);
		mUtil.addDebugMsg(1,"I","  settingWakeLockAlways="+mEnvParms.settingWakeLockOption);
		mUtil.addDebugMsg(1,"I","  settingWakeLockProximitySensor="+mEnvParms.settingWakeLockProximitySensor);
		mUtil.addDebugMsg(1,"I","  settingExitClean="+mEnvParms.settingExitClean);
		
		mUtil.resetScheduler();
		
		mUtil.resetLogReceiver();
		
	};

	private boolean mSwipeEnabled=true;
	final private void setTabSwipeEnabled(boolean enabled) {
		if (enabled) {
			mMainViewPager.setSwipeEnabled(true);
//			mMainTabWidget.setEnabled(true);
			mMainTabWidget.getChildTabViewAt(0).setVisibility(View.VISIBLE);
			mMainTabWidget.getChildTabViewAt(1).setVisibility(View.VISIBLE);
			mMainTabWidget.getChildTabViewAt(2).setVisibility(View.VISIBLE);

			mSwipeEnabled=true;
		} else {
			mMainViewPager.setSwipeEnabled(false);
//			mMainTabWidget.setEnabled(false);
			mSwipeEnabled=false;
			if (mMainTabHost.getCurrentTab()==0) {
				mMainTabWidget.getChildTabViewAt(1).setVisibility(View.GONE);
				mMainTabWidget.getChildTabViewAt(2).setVisibility(View.GONE);
			} else if (mMainTabHost.getCurrentTab()==1) {
				mMainTabWidget.getChildTabViewAt(0).setVisibility(View.GONE);
				mMainTabWidget.getChildTabViewAt(2).setVisibility(View.GONE);
			} else if (mMainTabHost.getCurrentTab()==2) {
				mMainTabWidget.getChildTabViewAt(0).setVisibility(View.GONE);
				mMainTabWidget.getChildTabViewAt(1).setVisibility(View.GONE);
			}
		}
	}
	
	final private void setTabControlView() {
		final Button btn_save=(Button)mControlView.findViewById(R.id.main_control_view_save_btn);
		final Button btn_cancel=(Button)mControlView.findViewById(R.id.main_control_view_cancel_btn);
		final TextView tv_msg=(TextView)mControlView.findViewById(R.id.main_control_view_msg);
		tv_msg.setText("");

		btn_save.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				saveControlConfigData(mControlView);
//				CommonUtilities.restartScheduler(mContext);
				CommonUtilities.restartScheduler(mContext);
				setControlViewEditMode(false);
				checkControlOptionInconsistency();
			}
		});
		btn_cancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				setControlViewContents(mControlView);
				setControlViewEditMode(false);
			}
		});
		setControlViewContents(mControlView);
		setControlViewEditMode(mEditModeEnabled);
	};

	private void saveControlConfigData(LinearLayout main_view) {
		final CheckedTextView ct_wifi_off_timeout=(CheckedTextView)main_view.findViewById(R.id.control_wifi_view_wifi_on);
//		final RadioGroup rg_wifi_off_timeout=(RadioGroup)main_view.findViewById(R.id.control_wifi_view_wifi_on_rg);
		final RadioButton rb_wifi_off_timeout_always=(RadioButton)main_view.findViewById(R.id.control_wifi_view_wifi_on_rg_always);
		final RadioButton rb_wifi_off_timeout_battery=(RadioButton)main_view.findViewById(R.id.control_wifi_view_wifi_on_rg_battery);
		
		final RadioButton rb_wifi_off_timeout_val1=(RadioButton)main_view.findViewById(R.id.control_wifi_view_wifi_on_timeout_rg_value1);
		final RadioButton rb_wifi_off_timeout_val2=(RadioButton)main_view.findViewById(R.id.control_wifi_view_wifi_on_timeout_rg_value2);
		final RadioButton rb_wifi_off_timeout_val3=(RadioButton)main_view.findViewById(R.id.control_wifi_view_wifi_on_timeout_rg_value3);
		
		final CheckedTextView ct_wifi_screen_unlock=(CheckedTextView)main_view.findViewById(R.id.control_wifi_view_wifi_screen_unlock);
//		final RadioGroup rg_wifi_screen_unlock=(RadioGroup)main_view.findViewById(R.id.control_wifi_view_wifi_screen_unlock_rg);
		final RadioButton rb_wifi_screen_unlock_always=(RadioButton)main_view.findViewById(R.id.control_wifi_view_wifi_screen_unlock_rg_always);
		final RadioButton rb_wifi_screen_unlock_charging=(RadioButton)main_view.findViewById(R.id.control_wifi_view_wifi_screen_unlock_rg_charging);

		final CheckedTextView ct_bt_off_timeout=(CheckedTextView)main_view.findViewById(R.id.control_bt_view_bt_on);
//		final RadioGroup rg_bt_off_timeout=(RadioGroup)main_view.findViewById(R.id.control_bt_view_bt_on_rg);
		final RadioButton rb_bt_off_timeout_always=(RadioButton)main_view.findViewById(R.id.control_bt_view_bt_on_rg_always);
		final RadioButton rb_bt_off_timeout_battery=(RadioButton)main_view.findViewById(R.id.control_bt_view_bt_on_rg_battery);
		final CheckedTextView ct_bt_screen_unlock=(CheckedTextView)main_view.findViewById(R.id.control_bt_view_bt_screen_unlock);
//		final RadioGroup rg_bt_screen_unlock=(RadioGroup)main_view.findViewById(R.id.control_bt_view_bt_screen_unlock_rg);
		final RadioButton rb_bt_screen_unlock_always=(RadioButton)main_view.findViewById(R.id.control_bt_view_bt_screen_unlock_rg_always);
		final RadioButton rb_bt_screen_unlock_battery=(RadioButton)main_view.findViewById(R.id.control_bt_view_bt_screen_unlock_rg_charging);

		final RadioButton rb_bt_off_timeout_val1=(RadioButton)main_view.findViewById(R.id.control_bt_view_bt_on_timeout_rg_value1);
		final RadioButton rb_bt_off_timeout_val2=(RadioButton)main_view.findViewById(R.id.control_bt_view_bt_on_timeout_rg_value2);
		final RadioButton rb_bt_off_timeout_val3=(RadioButton)main_view.findViewById(R.id.control_bt_view_bt_on_timeout_rg_value3);

		
//		final LinearLayout ll_wifi_screen_locked=(LinearLayout)main_view.findViewById(R.id.control_wifi_view_screen_locked_view);
		final CheckedTextView ct_wifi_screen_locked=(CheckedTextView)main_view.findViewById(R.id.control_wifi_view_screen_locked);
//		final RadioGroup rg_wifi_screen_locked=(RadioGroup)main_view.findViewById(R.id.control_wifi_view_screen_locked_rg);	
		final RadioButton rb_wifi_screen_locked_always=(RadioButton)main_view.findViewById(R.id.control_wifi_view_screen_locked_rg_always);
		final RadioButton rb_wifi_screen_locked_battery=(RadioButton)main_view.findViewById(R.id.control_wifi_view_screen_locked_rg_battery);
//		final RadioGroup rg_wifi_screen_locked_timeout=(RadioGroup)main_view.findViewById(R.id.control_wifi_view_screen_locked_timeout_rg);	
		final RadioButton rb_wifi_screen_locked_timeout_val1=(RadioButton)main_view.findViewById(R.id.control_wifi_view_screen_locked_timeout_rg_value1);
		final RadioButton rb_wifi_screen_locked_timeout_val2=(RadioButton)main_view.findViewById(R.id.control_wifi_view_screen_locked_timeout_rg_value2);
		final RadioButton rb_wifi_screen_locked_timeout_val3=(RadioButton)main_view.findViewById(R.id.control_wifi_view_screen_locked_timeout_rg_value3);
		
//		final LinearLayout ll_bt_screen_locked=(LinearLayout)main_view.findViewById(R.id.control_bt_view_screen_locked_view);
		final CheckedTextView ct_bt_screen_locked=(CheckedTextView)main_view.findViewById(R.id.control_bt_view_screen_locked);
//		final RadioGroup rg_bt_screen_locked=(RadioGroup)main_view.findViewById(R.id.control_bt_view_screen_locked_rg);	
		final RadioButton rb_bt_screen_locked_always=(RadioButton)main_view.findViewById(R.id.control_bt_view_screen_locked_rg_always);
		final RadioButton rb_bt_screen_locked_battery=(RadioButton)main_view.findViewById(R.id.control_bt_view_screen_locked_rg_battery);
//		final RadioGroup rg_bt_screen_locked_timeout=(RadioGroup)main_view.findViewById(R.id.control_bt_view_screen_locked_timeout_rg);	
		final RadioButton rb_bt_screen_locked_timeout_val1=(RadioButton)main_view.findViewById(R.id.control_bt_view_screen_locked_timeout_rg_value1);
		final RadioButton rb_bt_screen_locked_timeout_val2=(RadioButton)main_view.findViewById(R.id.control_bt_view_screen_locked_timeout_rg_value2);
		final RadioButton rb_bt_screen_locked_timeout_val3=(RadioButton)main_view.findViewById(R.id.control_bt_view_screen_locked_timeout_rg_value3);
		
		
		final TextView tv_msg=(TextView)main_view.findViewById(R.id.main_control_view_msg);
		tv_msg.setText("");

		String wifi_on_timeout_value="1", bt_on_timeout_value="1";

		String wifi_on_when_screen_unlock="", wifi_off_when_connect_timeout="", 
				wifi_off_when_screen_locked="", wifi_off_when_screen_locked_timeout="";
		if (ct_wifi_screen_unlock.isChecked()) {
			if (rb_wifi_screen_unlock_always.isChecked()) {
				wifi_on_when_screen_unlock=WIFI_ON_WHEN_SCREEN_UNLOCKED_ALWAYS;
			} else if (rb_wifi_screen_unlock_charging.isChecked()) {
				wifi_on_when_screen_unlock=WIFI_ON_WHEN_SCREEN_UNLOCKED_CHARGING;
			}
		} else wifi_on_when_screen_unlock=WIFI_ON_WHEN_SCREEN_UNLOCKED_DISABLED;
		if (ct_wifi_off_timeout.isChecked()) {
			if (rb_wifi_off_timeout_always.isChecked()) {
				wifi_off_when_connect_timeout=WIFI_OFF_WHEN_CONNECT_TIMEOUT_ALWAYS;
			} else if (rb_wifi_off_timeout_battery.isChecked()) {
				wifi_off_when_connect_timeout=WIFI_OFF_WHEN_CONNECT_TIMEOUT_BATTERY;
			}
			if (rb_wifi_off_timeout_val1.isChecked()) wifi_on_timeout_value="1";
			else if (rb_wifi_off_timeout_val2.isChecked()) wifi_on_timeout_value="2";
			else if (rb_wifi_off_timeout_val3.isChecked()) wifi_on_timeout_value="3";
		} else wifi_off_when_connect_timeout=WIFI_OFF_WHEN_CONNECT_TIMEOUT_DISABLED;

		if (ct_wifi_screen_locked.isChecked()) {
			if (rb_wifi_screen_locked_always.isChecked()) {
				wifi_off_when_screen_locked=WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_ALWAYS;
			} else if (rb_wifi_screen_locked_battery.isChecked()) {
				wifi_off_when_screen_locked=WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_BATTERY;
			}
			if (rb_wifi_screen_locked_timeout_val1.isChecked()) wifi_off_when_screen_locked_timeout="1";
			else if (rb_wifi_screen_locked_timeout_val2.isChecked()) wifi_off_when_screen_locked_timeout="2";
			else if (rb_wifi_screen_locked_timeout_val3.isChecked()) wifi_off_when_screen_locked_timeout="3";
		} else wifi_off_when_screen_locked=WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_DISABLED;

		String bt_on_when_screen_unlock="", bt_off_when_connect_timeout="",
				bt_off_when_screen_locked="", bt_off_when_screen_locked_timeout="";
		if (ct_bt_screen_unlock.isChecked()) {
			if (rb_bt_screen_unlock_always.isChecked()) {
				bt_on_when_screen_unlock=BLUETOOTH_ON_WHEN_SCREEN_UNLOCKED_ALWAYS;
			} else if (rb_bt_screen_unlock_battery.isChecked()) {
				bt_on_when_screen_unlock=BLUETOOTH_ON_WHEN_SCREEN_UNLOCKED_CHARGING;
			}
		} else bt_on_when_screen_unlock=BLUETOOTH_ON_WHEN_SCREEN_UNLOCKED_DISABLED;
		if (ct_bt_off_timeout.isChecked()) {
			if (rb_bt_off_timeout_always.isChecked()) {
				bt_off_when_connect_timeout=BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_ALWAYS;
			} else if (rb_bt_off_timeout_battery.isChecked()) {
				bt_off_when_connect_timeout=BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_BATTERY;
			}
			if (rb_bt_off_timeout_val1.isChecked()) bt_on_timeout_value="1";
			else if (rb_bt_off_timeout_val2.isChecked()) bt_on_timeout_value="2";
			else if (rb_bt_off_timeout_val3.isChecked()) bt_on_timeout_value="3";
		} else bt_off_when_connect_timeout=BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_DISABLED;
		
		if (ct_bt_screen_locked.isChecked()) {
			if (rb_bt_screen_locked_always.isChecked()) {
				bt_off_when_screen_locked=BLUETOOTH_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_ALWAYS;
			} else if (rb_bt_screen_locked_battery.isChecked()) {
				bt_off_when_screen_locked=BLUETOOTH_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_BATTERY;
			}
			if (rb_bt_screen_locked_timeout_val1.isChecked()) bt_off_when_screen_locked_timeout="1";
			else if (rb_bt_screen_locked_timeout_val2.isChecked()) bt_off_when_screen_locked_timeout="2";
			else if (rb_bt_screen_locked_timeout_val3.isChecked()) bt_off_when_screen_locked_timeout="3";
		} else bt_off_when_screen_locked=BLUETOOTH_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_DISABLED;
		
		
		mUtil.getPrefMgr().edit()
		.putString(WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_KEY,wifi_off_when_screen_locked)
		.putString(WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_VALUE_KEY,wifi_off_when_screen_locked_timeout)
		.putString(WIFI_ON_WHEN_SCREEN_UNLOCKED_KEY,wifi_on_when_screen_unlock)
		.putString(WIFI_OFF_WHEN_CONNECT_TIMEOUT_KEY,wifi_off_when_connect_timeout)
		.putString(BLUETOOTH_ON_WHEN_SCREEN_UNLOCKED_KEY,bt_on_when_screen_unlock)
		.putString(BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_KEY,bt_off_when_connect_timeout)
		.putString(WIFI_OFF_WHEN_CONNECT_TIMEOUT_VALUE_KEY, wifi_on_timeout_value)
		.putString(BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_VALUE_KEY, bt_on_timeout_value)
		.putString(BLUETOOTH_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_KEY,bt_off_when_screen_locked)
		.putString(BLUETOOTH_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_VALUE_KEY,bt_off_when_screen_locked_timeout)

		.commit();
	};
	
	private void setControlViewCheckedTextViewListener(final CheckedTextView ctv) {
//		final Button btn_save=(Button)mControlView.findViewById(R.id.main_control_view_save_btn);
//		final Button btn_cancel=(Button)mControlView.findViewById(R.id.main_control_view_cancel_btn);
		ctv.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				ctv.setChecked(!ctv.isChecked());
				setControlViewEditMode(true);
			}
		});
	};

	private void setScreenViewCheckedTextViewListener(final CheckedTextView ctv) {
//		final Button btn_save=(Button)mScreenView.findViewById(R.id.main_screen_view_save_btn);
//		final Button btn_cancel=(Button)mScreenView.findViewById(R.id.main_screen_view_cancel_btn);
		ctv.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				ctv.setChecked(!ctv.isChecked());
				setScreenViewEditMode(true);
			}
		});
	};

	
	private void setControlViewContents(LinearLayout main_view) {
//		final Button btn_save=(Button)mControlView.findViewById(R.id.main_control_view_save_btn);
//		final Button btn_cancel=(Button)mControlView.findViewById(R.id.main_control_view_cancel_btn);
		final CheckedTextView ct_bt_off_timeout=(CheckedTextView)main_view.findViewById(R.id.control_bt_view_bt_on);
		final CheckedTextView ct_bt_screen_unlock=(CheckedTextView)main_view.findViewById(R.id.control_bt_view_bt_screen_unlock);

		final TextView tv_msg=(TextView)main_view.findViewById(R.id.main_control_view_msg);
		tv_msg.setVisibility(TextView.GONE);

		setControlViewContentsWiFi(main_view);
		setControlViewContentsBt(main_view);
		
		if (BluetoothAdapter.getDefaultAdapter()==null) {
			ct_bt_screen_unlock.setEnabled(false);
			ct_bt_off_timeout.setEnabled(false);
			final TextView tv_bt_title=(TextView)main_view.findViewById(R.id.control_bt_view_bt_title);
			tv_bt_title.setEnabled(false);
			tv_msg.setVisibility(TextView.VISIBLE);
			tv_msg.setText(mContext.getString(R.string.msgs_control_bluetooth_not_available));
		}

	};

	private void setControlViewContentsBt(LinearLayout main_view) {
		final CheckedTextView ct_bt_off_timeout=(CheckedTextView)main_view.findViewById(R.id.control_bt_view_bt_on);
		final LinearLayout ll_bt_off_timeout=(LinearLayout)main_view.findViewById(R.id.control_bt_view_bt_on_view);
		final RadioGroup rg_bt_off_timeout=(RadioGroup)main_view.findViewById(R.id.control_bt_view_bt_on_rg);
		final RadioButton rb_bt_off_timeout_always=(RadioButton)main_view.findViewById(R.id.control_bt_view_bt_on_rg_always);
		final RadioButton rb_bt_off_timeout_battery=(RadioButton)main_view.findViewById(R.id.control_bt_view_bt_on_rg_battery);
		final CheckedTextView ct_bt_screen_unlock=(CheckedTextView)main_view.findViewById(R.id.control_bt_view_bt_screen_unlock);
		final RadioGroup rg_bt_screen_unlock=(RadioGroup)main_view.findViewById(R.id.control_bt_view_bt_screen_unlock_rg);
		final RadioButton rb_bt_screen_unlock_always=(RadioButton)main_view.findViewById(R.id.control_bt_view_bt_screen_unlock_rg_always);
		final RadioButton rb_bt_screen_unlock_battery=(RadioButton)main_view.findViewById(R.id.control_bt_view_bt_screen_unlock_rg_charging);

		final RadioGroup rg_bt_off_timeout_val=(RadioGroup)main_view.findViewById(R.id.control_bt_view_bt_on_timeout_rg);
		final RadioButton rb_bt_off_timeout_val1=(RadioButton)main_view.findViewById(R.id.control_bt_view_bt_on_timeout_rg_value1);
		final RadioButton rb_bt_off_timeout_val2=(RadioButton)main_view.findViewById(R.id.control_bt_view_bt_on_timeout_rg_value2);
		final RadioButton rb_bt_off_timeout_val3=(RadioButton)main_view.findViewById(R.id.control_bt_view_bt_on_timeout_rg_value3);

		final LinearLayout ll_bt_screen_locked=(LinearLayout)main_view.findViewById(R.id.control_bt_view_screen_locked_view);
		final CheckedTextView ct_bt_screen_locked=(CheckedTextView)main_view.findViewById(R.id.control_bt_view_screen_locked);
		final RadioGroup rg_bt_screen_locked=(RadioGroup)main_view.findViewById(R.id.control_bt_view_screen_locked_rg);	
		final RadioButton rb_bt_screen_locked_always=(RadioButton)main_view.findViewById(R.id.control_bt_view_screen_locked_rg_always);
		final RadioButton rb_bt_screen_locked_battery=(RadioButton)main_view.findViewById(R.id.control_bt_view_screen_locked_rg_battery);
		final RadioGroup rg_bt_screen_locked_timeout=(RadioGroup)main_view.findViewById(R.id.control_bt_view_screen_locked_timeout_rg);	
		final RadioButton rb_bt_screen_locked_timeout_val1=(RadioButton)main_view.findViewById(R.id.control_bt_view_screen_locked_timeout_rg_value1);
		final RadioButton rb_bt_screen_locked_timeout_val2=(RadioButton)main_view.findViewById(R.id.control_bt_view_screen_locked_timeout_rg_value2);
		final RadioButton rb_bt_screen_locked_timeout_val3=(RadioButton)main_view.findViewById(R.id.control_bt_view_screen_locked_timeout_rg_value3);

		rg_bt_screen_unlock.setOnCheckedChangeListener(null);
		ct_bt_screen_unlock.setOnClickListener(null);
		rg_bt_off_timeout.setOnCheckedChangeListener(null);
		rg_bt_off_timeout_val.setOnCheckedChangeListener(null);
		ct_bt_off_timeout.setOnClickListener(null);
		rg_bt_screen_locked.setOnCheckedChangeListener(null);
		rg_bt_screen_locked_timeout.setOnCheckedChangeListener(null);
		ct_bt_screen_locked.setOnClickListener(null);

		setControlViewCheckedTextViewListener(ct_bt_screen_locked);
		setControlViewCheckedTextViewListener(ct_bt_off_timeout);
		setControlViewCheckedTextViewListener(ct_bt_screen_unlock);

		final TextView tv_msg=(TextView)main_view.findViewById(R.id.main_control_view_msg);
		tv_msg.setVisibility(TextView.GONE);

		String bt_off_when_screen_locked=mUtil.getPrefMgr().getString(BLUETOOTH_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_KEY,BLUETOOTH_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_DISABLED);
		String bt_off_when_screen_locked_timeout_val=mUtil.getPrefMgr().getString(BLUETOOTH_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_VALUE_KEY,"1");
		String bt_on_when_screen_unlock=mUtil.getPrefMgr().getString(BLUETOOTH_ON_WHEN_SCREEN_UNLOCKED_KEY,BLUETOOTH_ON_WHEN_SCREEN_UNLOCKED_DISABLED);
		String bt_off_when_connect_timeout=mUtil.getPrefMgr().getString(BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_KEY,BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_DISABLED);
		String bt_connect_timeout_val=mUtil.getPrefMgr().getString(BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_VALUE_KEY,"1");

		rg_bt_screen_unlock.setVisibility(RadioGroup.VISIBLE);
		if (bt_on_when_screen_unlock.equals(BLUETOOTH_ON_WHEN_SCREEN_UNLOCKED_DISABLED)) {
			ct_bt_screen_unlock.setChecked(false);
			rg_bt_screen_unlock.setVisibility(RadioGroup.GONE);
		} else if (bt_on_when_screen_unlock.equals(BLUETOOTH_ON_WHEN_SCREEN_UNLOCKED_ALWAYS)) {
			ct_bt_screen_unlock.setChecked(true);
			rb_bt_screen_unlock_always.setChecked(true);
		} else if (bt_on_when_screen_unlock.equals(BLUETOOTH_ON_WHEN_SCREEN_UNLOCKED_CHARGING)) {
			ct_bt_screen_unlock.setChecked(true);
			rb_bt_screen_unlock_battery.setChecked(true);
		}
		rg_bt_screen_unlock.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				setControlViewEditMode(true);
			}
		});
		ct_bt_screen_unlock.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				boolean isChecked=!ct_bt_screen_unlock.isChecked();
				ct_bt_screen_unlock.setChecked(isChecked);
				if (isChecked) rg_bt_screen_unlock.setVisibility(RadioGroup.VISIBLE);
				else rg_bt_screen_unlock.setVisibility(RadioGroup.GONE);
				setControlViewEditMode(true);
			}
		});

		rg_bt_off_timeout.setVisibility(CheckedTextView.VISIBLE);
		if (bt_off_when_connect_timeout.equals(BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_DISABLED)) {
			ct_bt_off_timeout.setChecked(false);
			ll_bt_off_timeout.setVisibility(CheckedTextView.GONE);
		} else {
			if (bt_off_when_connect_timeout.equals(BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_ALWAYS)) {
				ct_bt_off_timeout.setChecked(true);
				rb_bt_off_timeout_always.setChecked(true);
			} else if (bt_off_when_connect_timeout.equals(BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_BATTERY)) {
				ct_bt_off_timeout.setChecked(true);
				rb_bt_off_timeout_battery.setChecked(true);
			}
			if (bt_connect_timeout_val.equals("1")) rb_bt_off_timeout_val1.setChecked(true);
			else if (bt_connect_timeout_val.equals("2")) rb_bt_off_timeout_val2.setChecked(true);
			else if (bt_connect_timeout_val.equals("3")) rb_bt_off_timeout_val3.setChecked(true);
			else rb_bt_off_timeout_val1.setChecked(true);
			ll_bt_off_timeout.setVisibility(CheckedTextView.VISIBLE);
		}
		rg_bt_off_timeout.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				setControlViewEditMode(true);
			}
		});
		rg_bt_off_timeout_val.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				setControlViewEditMode(true);
			}
		});
		ct_bt_off_timeout.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				boolean isChecked=!ct_bt_off_timeout.isChecked();
				ct_bt_off_timeout.setChecked(isChecked);
				if (isChecked) ll_bt_off_timeout.setVisibility(CheckedTextView.VISIBLE);
				else ll_bt_off_timeout.setVisibility(CheckedTextView.GONE);
				setControlViewEditMode(true);
			}
		});

		rg_bt_screen_locked_timeout.setVisibility(RadioGroup.VISIBLE);
		if (bt_off_when_screen_locked.equals(WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_DISABLED)) {
			ct_bt_screen_locked.setChecked(false);
			ll_bt_screen_locked.setVisibility(RadioGroup.GONE);
		} else {
			if (bt_off_when_screen_locked.equals(WIFI_OFF_WHEN_CONNECT_TIMEOUT_ALWAYS)) {
				ct_bt_screen_locked.setChecked(true);
				rb_bt_screen_locked_always.setChecked(true);
			} else if (bt_off_when_screen_locked.equals(WIFI_OFF_WHEN_CONNECT_TIMEOUT_BATTERY)) {
				ct_bt_screen_locked.setChecked(true);
				rb_bt_screen_locked_battery.setChecked(true);
			}
			ll_bt_screen_locked.setVisibility(RadioGroup.VISIBLE);
			if (bt_off_when_screen_locked_timeout_val.equals("1")) rb_bt_screen_locked_timeout_val1.setChecked(true);
			else if (bt_off_when_screen_locked_timeout_val.equals("2")) rb_bt_screen_locked_timeout_val2.setChecked(true);
			else if (bt_off_when_screen_locked_timeout_val.equals("3")) rb_bt_screen_locked_timeout_val3.setChecked(true);
			else rb_bt_screen_locked_timeout_val1.setChecked(true);
		}
		rg_bt_screen_locked.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				setControlViewEditMode(true);
			}
		});
		rg_bt_screen_locked_timeout.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				setControlViewEditMode(true);
			}
		});
		ct_bt_screen_locked.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				boolean isChecked=!ct_bt_screen_locked.isChecked();
				ct_bt_screen_locked.setChecked(isChecked);
				if (isChecked) ll_bt_screen_locked.setVisibility(RadioGroup.VISIBLE);
				else ll_bt_screen_locked.setVisibility(RadioGroup.GONE);
				setControlViewEditMode(true);
			}
		});
	};

	private void setControlViewContentsWiFi(LinearLayout main_view) {
		final CheckedTextView ct_wifi_off_timeout=(CheckedTextView)main_view.findViewById(R.id.control_wifi_view_wifi_on);
		final LinearLayout ll_wifi_off_timeout=(LinearLayout)main_view.findViewById(R.id.control_wifi_view_wifi_on_view);
		final RadioGroup rg_wifi_off_timeout=(RadioGroup)main_view.findViewById(R.id.control_wifi_view_wifi_on_rg);
		final RadioButton rb_wifi_off_timeout_always=(RadioButton)main_view.findViewById(R.id.control_wifi_view_wifi_on_rg_always);
		final RadioButton rb_wifi_off_timeout_battery=(RadioButton)main_view.findViewById(R.id.control_wifi_view_wifi_on_rg_battery);
		final CheckedTextView ct_wifi_screen_unlock=(CheckedTextView)main_view.findViewById(R.id.control_wifi_view_wifi_screen_unlock);
		final RadioGroup rg_wifi_screen_unlock=(RadioGroup)main_view.findViewById(R.id.control_wifi_view_wifi_screen_unlock_rg);
		final RadioButton rb_wifi_screen_unlock_always=(RadioButton)main_view.findViewById(R.id.control_wifi_view_wifi_screen_unlock_rg_always);
		final RadioButton rb_wifi_screen_unlock_charging=(RadioButton)main_view.findViewById(R.id.control_wifi_view_wifi_screen_unlock_rg_charging);

		final RadioGroup rg_wifi_off_timeout_val=(RadioGroup)main_view.findViewById(R.id.control_wifi_view_wifi_on_timeout_rg);	
		final RadioButton rb_wifi_off_timeout_val1=(RadioButton)main_view.findViewById(R.id.control_wifi_view_wifi_on_timeout_rg_value1);
		final RadioButton rb_wifi_off_timeout_val2=(RadioButton)main_view.findViewById(R.id.control_wifi_view_wifi_on_timeout_rg_value2);
		final RadioButton rb_wifi_off_timeout_val3=(RadioButton)main_view.findViewById(R.id.control_wifi_view_wifi_on_timeout_rg_value3);

		final LinearLayout ll_wifi_screen_locked=(LinearLayout)main_view.findViewById(R.id.control_wifi_view_screen_locked_view);
		final CheckedTextView ct_wifi_screen_locked=(CheckedTextView)main_view.findViewById(R.id.control_wifi_view_screen_locked);
		final RadioGroup rg_wifi_screen_locked=(RadioGroup)main_view.findViewById(R.id.control_wifi_view_screen_locked_rg);	
		final RadioButton rb_wifi_screen_locked_always=(RadioButton)main_view.findViewById(R.id.control_wifi_view_screen_locked_rg_always);
		final RadioButton rb_wifi_screen_locked_battery=(RadioButton)main_view.findViewById(R.id.control_wifi_view_screen_locked_rg_battery);
		final RadioGroup rg_wifi_screen_locked_timeout=(RadioGroup)main_view.findViewById(R.id.control_wifi_view_screen_locked_timeout_rg);	
		final RadioButton rb_wifi_screen_locked_timeout_val1=(RadioButton)main_view.findViewById(R.id.control_wifi_view_screen_locked_timeout_rg_value1);
		final RadioButton rb_wifi_screen_locked_timeout_val2=(RadioButton)main_view.findViewById(R.id.control_wifi_view_screen_locked_timeout_rg_value2);
		final RadioButton rb_wifi_screen_locked_timeout_val3=(RadioButton)main_view.findViewById(R.id.control_wifi_view_screen_locked_timeout_rg_value3);

		rg_wifi_screen_unlock.setOnCheckedChangeListener(null);
		ct_wifi_screen_unlock.setOnClickListener(null);
		rg_wifi_off_timeout.setOnCheckedChangeListener(null);
		rg_wifi_off_timeout_val.setOnCheckedChangeListener(null);
		ct_wifi_off_timeout.setOnClickListener(null);
		rg_wifi_screen_locked.setOnCheckedChangeListener(null);
		rg_wifi_screen_locked_timeout.setOnCheckedChangeListener(null);
		ct_wifi_screen_locked.setOnClickListener(null);

		setControlViewCheckedTextViewListener(ct_wifi_screen_locked);
		setControlViewCheckedTextViewListener(ct_wifi_off_timeout);
		setControlViewCheckedTextViewListener(ct_wifi_screen_unlock);

		final TextView tv_msg=(TextView)main_view.findViewById(R.id.main_control_view_msg);
		tv_msg.setVisibility(TextView.GONE);

		String wifi_off_when_screen_locked=mUtil.getPrefMgr().getString(WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_KEY,WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_DISABLED);
		String wifi_off_when_screen_locked_timeout_val=mUtil.getPrefMgr().getString(WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_VALUE_KEY,"1");
		String wifi_on_when_screen_unlock=mUtil.getPrefMgr().getString(WIFI_ON_WHEN_SCREEN_UNLOCKED_KEY,WIFI_ON_WHEN_SCREEN_UNLOCKED_DISABLED);
		String wifi_off_when_connect_timeout=mUtil.getPrefMgr().getString(WIFI_OFF_WHEN_CONNECT_TIMEOUT_KEY,WIFI_OFF_WHEN_CONNECT_TIMEOUT_DISABLED);
		String wifi_connect_timeout_val=mUtil.getPrefMgr().getString(WIFI_OFF_WHEN_CONNECT_TIMEOUT_VALUE_KEY,"1");
		
		rg_wifi_screen_unlock.setVisibility(RadioGroup.VISIBLE);
		if (wifi_on_when_screen_unlock.equals(WIFI_ON_WHEN_SCREEN_UNLOCKED_DISABLED)) {
			ct_wifi_screen_unlock.setChecked(false);
			rg_wifi_screen_unlock.setVisibility(RadioGroup.GONE);
		} else if (wifi_on_when_screen_unlock.equals(WIFI_ON_WHEN_SCREEN_UNLOCKED_ALWAYS)) {
			ct_wifi_screen_unlock.setChecked(true);
			rb_wifi_screen_unlock_always.setChecked(true);
		} else if (wifi_on_when_screen_unlock.equals(WIFI_ON_WHEN_SCREEN_UNLOCKED_CHARGING)) {
			ct_wifi_screen_unlock.setChecked(true);
			rb_wifi_screen_unlock_charging.setChecked(true);
		}
		rg_wifi_screen_unlock.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				setControlViewEditMode(true);
			}
		});
		ct_wifi_screen_unlock.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				boolean isChecked=!ct_wifi_screen_unlock.isChecked();
				ct_wifi_screen_unlock.setChecked(isChecked);
				if (isChecked) rg_wifi_screen_unlock.setVisibility(RadioGroup.VISIBLE);
				else rg_wifi_screen_unlock.setVisibility(RadioGroup.GONE);
				setControlViewEditMode(true);
			}
		});
		
		rg_wifi_off_timeout.setVisibility(RadioGroup.VISIBLE);
		if (wifi_off_when_connect_timeout.equals(WIFI_OFF_WHEN_CONNECT_TIMEOUT_DISABLED)) {
			ct_wifi_off_timeout.setChecked(false);
			ll_wifi_off_timeout.setVisibility(RadioGroup.GONE);
		} else {
			if (wifi_off_when_connect_timeout.equals(WIFI_OFF_WHEN_CONNECT_TIMEOUT_ALWAYS)) {
				ct_wifi_off_timeout.setChecked(true);
				rb_wifi_off_timeout_always.setChecked(true);
			} else if (wifi_off_when_connect_timeout.equals(WIFI_OFF_WHEN_CONNECT_TIMEOUT_BATTERY)) {
				ct_wifi_off_timeout.setChecked(true);
				rb_wifi_off_timeout_battery.setChecked(true);
			}
			ll_wifi_off_timeout.setVisibility(RadioGroup.VISIBLE);
			if (wifi_connect_timeout_val.equals("1")) rb_wifi_off_timeout_val1.setChecked(true);
			else if (wifi_connect_timeout_val.equals("2")) rb_wifi_off_timeout_val2.setChecked(true);
			else if (wifi_connect_timeout_val.equals("3")) rb_wifi_off_timeout_val3.setChecked(true);
			else rb_wifi_off_timeout_val1.setChecked(true);
		}
		rg_wifi_off_timeout.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				setControlViewEditMode(true);
			}
		});
		rg_wifi_off_timeout_val.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				setControlViewEditMode(true);
			}
		});
		ct_wifi_off_timeout.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				boolean isChecked=!ct_wifi_off_timeout.isChecked();
				ct_wifi_off_timeout.setChecked(isChecked);
				if (isChecked) ll_wifi_off_timeout.setVisibility(RadioGroup.VISIBLE);
				else ll_wifi_off_timeout.setVisibility(RadioGroup.GONE);
				setControlViewEditMode(true);
			}
		});

		rg_wifi_screen_locked_timeout.setVisibility(RadioGroup.VISIBLE);
		if (wifi_off_when_screen_locked.equals(WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_DISABLED)) {
			ct_wifi_screen_locked.setChecked(false);
			ll_wifi_screen_locked.setVisibility(RadioGroup.GONE);
		} else {
			if (wifi_off_when_screen_locked.equals(WIFI_OFF_WHEN_CONNECT_TIMEOUT_ALWAYS)) {
				ct_wifi_screen_locked.setChecked(true);
				rb_wifi_screen_locked_always.setChecked(true);
			} else if (wifi_off_when_screen_locked.equals(WIFI_OFF_WHEN_CONNECT_TIMEOUT_BATTERY)) {
				ct_wifi_screen_locked.setChecked(true);
				rb_wifi_screen_locked_battery.setChecked(true);
			}
			ll_wifi_screen_locked.setVisibility(RadioGroup.VISIBLE);
			if (wifi_off_when_screen_locked_timeout_val.equals("1")) rb_wifi_screen_locked_timeout_val1.setChecked(true);
			else if (wifi_off_when_screen_locked_timeout_val.equals("2")) rb_wifi_screen_locked_timeout_val2.setChecked(true);
			else if (wifi_off_when_screen_locked_timeout_val.equals("3")) rb_wifi_screen_locked_timeout_val3.setChecked(true);
			else rb_wifi_screen_locked_timeout_val1.setChecked(true);
		}
		rg_wifi_screen_locked.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				setControlViewEditMode(true);
			}
		});
		rg_wifi_screen_locked_timeout.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				setControlViewEditMode(true);
			}
		});
		ct_wifi_screen_locked.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				boolean isChecked=!ct_wifi_screen_locked.isChecked();
				ct_wifi_screen_locked.setChecked(isChecked);
				if (isChecked) ll_wifi_screen_locked.setVisibility(RadioGroup.VISIBLE);
				else ll_wifi_screen_locked.setVisibility(RadioGroup.GONE);
				setControlViewEditMode(true);
			}
		});

	};

	private boolean mEditModeEnabled=false;
	private void setControlViewEditMode(boolean enabled) {
		final Button btn_save=(Button)mControlView.findViewById(R.id.main_control_view_save_btn);
		final Button btn_cancel=(Button)mControlView.findViewById(R.id.main_control_view_cancel_btn);
		if (enabled) {
			btn_save.setVisibility(Button.VISIBLE);
			btn_cancel.setVisibility(Button.VISIBLE);
			setTabSwipeEnabled(false);
			mEditModeEnabled=true;
		} else {
			btn_save.setVisibility(Button.GONE);
			btn_cancel.setVisibility(Button.GONE);
			setTabSwipeEnabled(true);
			mEditModeEnabled=false;
		}
	};
	
	private void setScreenViewEditMode(boolean enabled) {
		final Button btn_save=(Button)mScreenView.findViewById(R.id.main_screen_view_save_btn);
		final Button btn_cancel=(Button)mScreenView.findViewById(R.id.main_screen_view_cancel_btn);
		if (enabled) {
			btn_save.setVisibility(Button.VISIBLE);
			btn_cancel.setVisibility(Button.VISIBLE);
			setTabSwipeEnabled(false);
			mEditModeEnabled=true;
		} else {
			btn_save.setVisibility(Button.GONE);
			btn_cancel.setVisibility(Button.GONE);
			setTabSwipeEnabled(true);
			mEditModeEnabled=false;
		}
	};

	final private void setTabScreenView() {
		final Button btn_save=(Button)mScreenView.findViewById(R.id.main_screen_view_save_btn);
		final Button btn_cancel=(Button)mScreenView.findViewById(R.id.main_screen_view_cancel_btn);
		final TextView tv_msg=(TextView)mScreenView.findViewById(R.id.main_screen_view_msg);
		tv_msg.setText("");

		btn_save.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				saveScreenConfigData(mScreenView);
				setScreenViewContents(mScreenView);
				CommonUtilities.restartScheduler(mContext);
				setScreenViewEditMode(false);
			}
		});
		btn_cancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				setScreenViewContents(mScreenView);
				setScreenViewEditMode(false);
			}
		});
		setScreenViewContents(mScreenView);
		setScreenViewEditMode(mEditModeEnabled);
	};

	private void saveScreenConfigData(LinearLayout main_view) {
		final CheckedTextView ct_proximity_disabled=(CheckedTextView)main_view.findViewById(R.id.control_screen_view_proximity_diable_when_multiple_sensor_event);
		final CheckedTextView ct_proximity_undetect=(CheckedTextView)main_view.findViewById(R.id.control_screen_view_proximity_undetect);
		final CheckedTextView ct_proximity_detect=(CheckedTextView)main_view.findViewById(R.id.control_screen_view_proximity_detect);
		final CheckedTextView ct_proximity_ignore_landscape=(CheckedTextView)main_view.findViewById(R.id.control_screen_view_proximity_detect_ignore_landscape);
		
		final RadioButton rb_proximity_timeout_val0=(RadioButton)main_view.findViewById(R.id.control_screen_view_proximity_detect_timeout_rg_value0);
		final RadioButton rb_proximity_timeout_val1=(RadioButton)main_view.findViewById(R.id.control_screen_view_proximity_detect_timeout_rg_value1);
		final RadioButton rb_proximity_timeout_val2=(RadioButton)main_view.findViewById(R.id.control_screen_view_proximity_detect_timeout_rg_value2);
		final RadioButton rb_proximity_timeout_val3=(RadioButton)main_view.findViewById(R.id.control_screen_view_proximity_detect_timeout_rg_value3);

		final CheckedTextView ct_notify_screen_locked_vibrate=(CheckedTextView)main_view.findViewById(R.id.control_screen_view_notify_when_screen_locked_vibrate);
		final RadioButton rb_locked_vibrate_val1=(RadioButton)main_view.findViewById(R.id.control_screen_view_notify_when_screen_locked_vibrate_rg_value1);
		final RadioButton rb_locked_vibrate_val2=(RadioButton)main_view.findViewById(R.id.control_screen_view_notify_when_screen_locked_vibrate_rg_value2);
		final RadioButton rb_locked_vibrate_val3=(RadioButton)main_view.findViewById(R.id.control_screen_view_notify_when_screen_locked_vibrate_rg_value3);

		final CheckedTextView ct_notify_screen_locked_notification=(CheckedTextView)main_view.findViewById(R.id.control_screen_view_notify_when_screen_locked_notification);
		final CheckedTextView ct_notify_screen_unlocked_vibrate=(CheckedTextView)main_view.findViewById(R.id.control_screen_view_notify_when_screen_unlocked_vibrate);
		final RadioButton rb_unlocked_vibrate_val1=(RadioButton)main_view.findViewById(R.id.control_screen_view_notify_when_screen_unlocked_vibrate_rg_value1);
		final RadioButton rb_unlocked_vibrate_val2=(RadioButton)main_view.findViewById(R.id.control_screen_view_notify_when_screen_unlocked_vibrate_rg_value2);
		final RadioButton rb_unlocked_vibrate_val3=(RadioButton)main_view.findViewById(R.id.control_screen_view_notify_when_screen_unlocked_vibrate_rg_value3);

		final CheckedTextView ct_notify_screen_unlocked_notification=(CheckedTextView)main_view.findViewById(R.id.control_screen_view_notify_when_screen_unlocked_notification);

		final CheckedTextView tv_notify_screen_locked_notification_title=(CheckedTextView)main_view.findViewById(R.id.control_screen_view_notify_when_screen_locked_notification_view_title);
		final CheckedTextView tv_notify_screen_unlocked_notification_title=(CheckedTextView)main_view.findViewById(R.id.control_screen_view_notify_when_screen_unlocked_notification_view_title);
		
		final TextView tv_msg=(TextView)main_view.findViewById(R.id.main_screen_view_msg);
		tv_msg.setText("");

		String proximity_screen_on="", proximity_screen_off="", proximity_disabled="";
		String proximity_detected_timeout_value="1";
		
		if (ct_proximity_disabled.isChecked()) proximity_disabled=PROXIMITY_DISABLED_WWHEN_MULTIPLE_EVENT_ENABLED;
		else proximity_disabled=PROXIMITY_DISABLED_WWHEN_MULTIPLE_EVENT_DISABLED;
		
		if (ct_proximity_undetect.isChecked()) proximity_screen_on=PROXIMITY_UNDETECTED_ENABLED;
		else proximity_screen_on=PROXIMITY_UNDETECTED_DISABLED;
		if (ct_proximity_detect.isChecked()) {
			if (ct_proximity_ignore_landscape.isChecked()) proximity_screen_off=PROXIMITY_DETECTED_IGNORE_LANDSCAPE;
			else proximity_screen_off=PROXIMITY_DETECTED_ALWAYS;
		} else proximity_screen_off=PROXIMITY_DETECTED_DISABLED;

		if (rb_proximity_timeout_val0.isChecked()) proximity_detected_timeout_value="0";
		else if (rb_proximity_timeout_val1.isChecked()) proximity_detected_timeout_value="1";
		else if (rb_proximity_timeout_val2.isChecked()) proximity_detected_timeout_value="2";
		else if (rb_proximity_timeout_val3.isChecked()) proximity_detected_timeout_value="3";

		String locked_vibrate_value="", unlocked_vibrate_value="";
		if (rb_locked_vibrate_val1.isChecked()) locked_vibrate_value=NOTIFY_WHEN_SCREEN_LOCKED_VIBRATE_PATTERN_VALUE1;
		else if (rb_locked_vibrate_val2.isChecked()) locked_vibrate_value=NOTIFY_WHEN_SCREEN_LOCKED_VIBRATE_PATTERN_VALUE2;
		else if (rb_locked_vibrate_val3.isChecked()) locked_vibrate_value=NOTIFY_WHEN_SCREEN_LOCKED_VIBRATE_PATTERN_VALUE3;

		if (rb_unlocked_vibrate_val1.isChecked()) unlocked_vibrate_value=NOTIFY_WHEN_SCREEN_UNLOCKED_VIBRATE_PATTERN_VALUE1;
		else if (rb_unlocked_vibrate_val2.isChecked()) unlocked_vibrate_value=NOTIFY_WHEN_SCREEN_UNLOCKED_VIBRATE_PATTERN_VALUE2;
		else if (rb_unlocked_vibrate_val3.isChecked()) unlocked_vibrate_value=NOTIFY_WHEN_SCREEN_UNLOCKED_VIBRATE_PATTERN_VALUE3;

		mUtil.getPrefMgr().edit().putString(PROXIMITY_UNDETECTED_KEY,proximity_screen_on)
		.putString(PROXIMITY_DISABLED_WWHEN_MULTIPLE_EVENT_KEY,proximity_disabled)
		.putString(PROXIMITY_DETECTED_KEY,proximity_screen_off)
		.putBoolean(NOTIFY_WHEN_SCREEN_LOCKED_VIBRATE_KEY, ct_notify_screen_locked_vibrate.isChecked())
		.putString(NOTIFY_WHEN_SCREEN_LOCKED_VIBRATE_PATTERN_KEY, locked_vibrate_value)
		.putBoolean(NOTIFY_WHEN_SCREEN_LOCKED_NOTIFICATION_KEY, ct_notify_screen_locked_notification.isChecked())
		.putBoolean(NOTIFY_WHEN_SCREEN_UNLOCKED_VIBRATE_KEY, ct_notify_screen_unlocked_vibrate.isChecked())
		.putString(NOTIFY_WHEN_SCREEN_UNLOCKED_VIBRATE_PATTERN_KEY, unlocked_vibrate_value)
		.putBoolean(NOTIFY_WHEN_SCREEN_UNLOCKED_NOTIFICATION_KEY, ct_notify_screen_unlocked_notification.isChecked())
		.putString(PROXIMITY_DETECTED_TIMEOUT_VALUE_KEY, proximity_detected_timeout_value)
		.putString(NOTIFY_WHEN_SCREEN_LOCKED_NOTIFICATION_TITLE_KEY,tv_notify_screen_locked_notification_title.getText().toString())
		.putString(NOTIFY_WHEN_SCREEN_LOCKED_NOTIFICATION_PATH_KEY,(String)tv_notify_screen_locked_notification_title.getTag())
		.putString(NOTIFY_WHEN_SCREEN_UNLOCKED_NOTIFICATION_TITLE_KEY,tv_notify_screen_unlocked_notification_title.getText().toString())
		.putString(NOTIFY_WHEN_SCREEN_UNLOCKED_NOTIFICATION_PATH_KEY,(String)tv_notify_screen_unlocked_notification_title.getTag())
		.commit();
	};
	
	private NotifyEvent mNotifyRingtonePickerEnded;
	private void setScreenViewContents(LinearLayout main_view) {
		final Uri def_uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		final Ringtone rt=RingtoneManager.getRingtone(mContext, def_uri);
		final String def_notification_title=rt.getTitle(mContext);
		final String def_notification_path=def_uri.getPath();

//		final Button btn_save=(Button)main_view.findViewById(R.id.main_screen_view_save_btn);
//		final Button btn_cancel=(Button)main_view.findViewById(R.id.main_screen_view_cancel_btn);
		final TextView tv_proximity_title=(TextView)main_view.findViewById(R.id.control_screen_view_proximity_title);
		final CheckedTextView ct_proximity_disabled=(CheckedTextView)main_view.findViewById(R.id.control_screen_view_proximity_diable_when_multiple_sensor_event);
		final CheckedTextView ct_proximity_undetect=(CheckedTextView)main_view.findViewById(R.id.control_screen_view_proximity_undetect);
		final CheckedTextView ct_proximity_detect=(CheckedTextView)main_view.findViewById(R.id.control_screen_view_proximity_detect);
		final LinearLayout ll_proximity_detect=(LinearLayout)main_view.findViewById(R.id.control_screen_view_proximity_detect_option_view);
		final CheckedTextView ct_proximity_ignore_landscape=(CheckedTextView)main_view.findViewById(R.id.control_screen_view_proximity_detect_ignore_landscape);
		
		final RadioGroup rg_proximity_timeout_val=(RadioGroup)main_view.findViewById(R.id.control_screen_view_proximity_detect_timeout_rg);
		final RadioButton rb_proximity_timeout_val0=(RadioButton)main_view.findViewById(R.id.control_screen_view_proximity_detect_timeout_rg_value0);
		final RadioButton rb_proximity_timeout_val1=(RadioButton)main_view.findViewById(R.id.control_screen_view_proximity_detect_timeout_rg_value1);
		final RadioButton rb_proximity_timeout_val2=(RadioButton)main_view.findViewById(R.id.control_screen_view_proximity_detect_timeout_rg_value2);
		final RadioButton rb_proximity_timeout_val3=(RadioButton)main_view.findViewById(R.id.control_screen_view_proximity_detect_timeout_rg_value3);
		
		final CheckedTextView ct_notify_screen_locked_vibrate=(CheckedTextView)main_view.findViewById(R.id.control_screen_view_notify_when_screen_locked_vibrate);
		final RadioGroup rg_locked_vibrate=(RadioGroup)main_view.findViewById(R.id.control_screen_view_notify_when_screen_locked_vibrate_rg);
		final RadioButton rb_locked_vibrate_val1=(RadioButton)main_view.findViewById(R.id.control_screen_view_notify_when_screen_locked_vibrate_rg_value1);
		final RadioButton rb_locked_vibrate_val2=(RadioButton)main_view.findViewById(R.id.control_screen_view_notify_when_screen_locked_vibrate_rg_value2);
		final RadioButton rb_locked_vibrate_val3=(RadioButton)main_view.findViewById(R.id.control_screen_view_notify_when_screen_locked_vibrate_rg_value3);

		final CheckedTextView ct_notify_screen_locked_notification_sound=(CheckedTextView)main_view.findViewById(R.id.control_screen_view_notify_when_screen_locked_notification);
		final CheckedTextView tv_notify_screen_locked_notification_title=(CheckedTextView)main_view.findViewById(R.id.control_screen_view_notify_when_screen_locked_notification_view_title);
		final Button btn_notify_screen_locked_notification_select_btn=(Button)main_view.findViewById(R.id.control_screen_view_notify_when_screen_locked_notification_view_btn_select);
		final CheckedTextView ct_notify_screen_unlocked_vibrate=(CheckedTextView)main_view.findViewById(R.id.control_screen_view_notify_when_screen_unlocked_vibrate);
		final RadioGroup rg_unlocked_vibrate=(RadioGroup)main_view.findViewById(R.id.control_screen_view_notify_when_screen_unlocked_vibrate_rg);
		final RadioButton rb_unlocked_vibrate_val1=(RadioButton)main_view.findViewById(R.id.control_screen_view_notify_when_screen_unlocked_vibrate_rg_value1);
		final RadioButton rb_unlocked_vibrate_val2=(RadioButton)main_view.findViewById(R.id.control_screen_view_notify_when_screen_unlocked_vibrate_rg_value2);
		final RadioButton rb_unlocked_vibrate_val3=(RadioButton)main_view.findViewById(R.id.control_screen_view_notify_when_screen_unlocked_vibrate_rg_value3);

		final CheckedTextView ct_notify_screen_unlocked_notification_sound=(CheckedTextView)main_view.findViewById(R.id.control_screen_view_notify_when_screen_unlocked_notification);
		final CheckedTextView tv_notify_screen_unlocked_notification_title=(CheckedTextView)main_view.findViewById(R.id.control_screen_view_notify_when_screen_unlocked_notification_view_title);
		final Button btn_notify_screen_unlocked_notification_select_btn=(Button)main_view.findViewById(R.id.control_screen_view_notify_when_screen_unlocked_notification_view_btn_select);
		
		final TextView tv_msg=(TextView)main_view.findViewById(R.id.main_screen_view_msg);
		
		setScreenViewCheckedTextViewListener(ct_proximity_disabled);
		setScreenViewCheckedTextViewListener(ct_proximity_undetect);
		setScreenViewCheckedTextViewListener(ct_proximity_detect);
		setScreenViewCheckedTextViewListener(ct_proximity_ignore_landscape);
//		setScreenViewCheckedTextViewListener(ct_notify_screen_locked_vibrate);
		setScreenViewCheckedTextViewListener(ct_notify_screen_locked_notification_sound);
//		setScreenViewCheckedTextViewListener(ct_notify_screen_unlocked_vibrate);
		setScreenViewCheckedTextViewListener(ct_notify_screen_unlocked_notification_sound);
		
		if (mUtil.isProximitySensorAvailable()==null) {
			tv_proximity_title.setEnabled(false);
			ct_proximity_disabled.setEnabled(false);
			ct_proximity_undetect.setEnabled(false);
			ct_proximity_detect.setEnabled(false);
			ct_proximity_ignore_landscape.setEnabled(false);
			ll_proximity_detect.setVisibility(LinearLayout.GONE);
			tv_msg.setText(mContext.getString(R.string.msgs_control_screen_proximity_sensor_not_available));
			tv_msg.setVisibility(TextView.VISIBLE);
		} else {
			tv_msg.setText("");
			tv_msg.setVisibility(TextView.GONE);
		}
		
		String proximity_disabled=mUtil.getPrefMgr().getString(PROXIMITY_DISABLED_WWHEN_MULTIPLE_EVENT_KEY,PROXIMITY_DISABLED_WWHEN_MULTIPLE_EVENT_DISABLED);
		String proximity_screen_on=mUtil.getPrefMgr().getString(PROXIMITY_UNDETECTED_KEY,PROXIMITY_UNDETECTED_DISABLED);
		String proximity_screen_off=mUtil.getPrefMgr().getString(PROXIMITY_DETECTED_KEY,PROXIMITY_DETECTED_DISABLED);
		String proximity_timeout_val=mUtil.getPrefMgr().getString(PROXIMITY_DETECTED_TIMEOUT_VALUE_KEY, "2");
//		String use_trusted_device_list=mUtil.getPrefMgr().getString(USE_TRUSTED_DEVICE_LIST_KEY, USE_TRUSTED_DEVICE_LIST_DISABLED);

		if (proximity_disabled.equals(PROXIMITY_DISABLED_WWHEN_MULTIPLE_EVENT_DISABLED)) {
			ct_proximity_disabled.setChecked(false);
		} else if (proximity_disabled.equals(PROXIMITY_DISABLED_WWHEN_MULTIPLE_EVENT_ENABLED)) {
			ct_proximity_disabled.setChecked(true);
		}
		
		if (proximity_screen_on.equals(PROXIMITY_UNDETECTED_DISABLED)) {
			ct_proximity_undetect.setChecked(false);
		} else if (proximity_screen_on.equals(PROXIMITY_UNDETECTED_ENABLED)) {
			ct_proximity_undetect.setChecked(true);
		}

		ct_proximity_ignore_landscape.setVisibility(CheckedTextView.VISIBLE);
		if (proximity_screen_off.equals(PROXIMITY_DETECTED_DISABLED)) {
			ct_proximity_detect.setChecked(false);
			ll_proximity_detect.setVisibility(LinearLayout.GONE);
		} else {
			if (proximity_screen_off.equals(PROXIMITY_DETECTED_ALWAYS)) {
				ct_proximity_detect.setChecked(true);
			} else if (proximity_screen_off.equals(PROXIMITY_DETECTED_IGNORE_LANDSCAPE)) {
				ct_proximity_detect.setChecked(true);
				ct_proximity_ignore_landscape.setChecked(true);
			}
			if (!CommonUtilities.isDevicePolicyManagerActive(mContext)) {
				ct_proximity_detect.setEnabled(false);
				ll_proximity_detect.setVisibility(LinearLayout.GONE);
			} else {
				ct_proximity_detect.setEnabled(true);
				ll_proximity_detect.setVisibility(LinearLayout.VISIBLE);
			}
		}
		if (proximity_timeout_val.equals("0")) rb_proximity_timeout_val0.setChecked(true);
		else if (proximity_timeout_val.equals("1")) rb_proximity_timeout_val1.setChecked(true);
		else if (proximity_timeout_val.equals("2")) rb_proximity_timeout_val2.setChecked(true);
		else if (proximity_timeout_val.equals("3")) rb_proximity_timeout_val3.setChecked(true);
		else rb_proximity_timeout_val1.setChecked(true);
		rg_proximity_timeout_val.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				setScreenViewEditMode(true);
			}
		});
		ct_proximity_detect.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				CheckedTextView ctv=(CheckedTextView)v;
				boolean isChecked=!ctv.isChecked();
				ctv.setChecked(isChecked);
				if (isChecked) ll_proximity_detect.setVisibility(CheckedTextView.VISIBLE);
				else ll_proximity_detect.setVisibility(CheckedTextView.GONE);
				setScreenViewEditMode(true);
			}
		});

		ct_notify_screen_locked_vibrate.setChecked(
				mUtil.getPrefMgr().getBoolean(NOTIFY_WHEN_SCREEN_LOCKED_VIBRATE_KEY, false));
		String saved_locked_pattern=mUtil.getPrefMgr().getString(NOTIFY_WHEN_SCREEN_LOCKED_VIBRATE_PATTERN_KEY, NOTIFY_WHEN_SCREEN_LOCKED_VIBRATE_PATTERN_VALUE1);
		if (saved_locked_pattern.equals(NOTIFY_WHEN_SCREEN_LOCKED_VIBRATE_PATTERN_VALUE1)) rb_locked_vibrate_val1.setChecked(true);
		else if (saved_locked_pattern.equals(NOTIFY_WHEN_SCREEN_LOCKED_VIBRATE_PATTERN_VALUE2)) rb_locked_vibrate_val2.setChecked(true);
		else if (saved_locked_pattern.equals(NOTIFY_WHEN_SCREEN_LOCKED_VIBRATE_PATTERN_VALUE3)) rb_locked_vibrate_val3.setChecked(true);

		rb_locked_vibrate_val1.setEnabled(ct_notify_screen_locked_vibrate.isChecked());
		rb_locked_vibrate_val2.setEnabled(ct_notify_screen_locked_vibrate.isChecked());
		rb_locked_vibrate_val3.setEnabled(ct_notify_screen_locked_vibrate.isChecked());
		
		ct_notify_screen_locked_vibrate.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				CheckedTextView ctv=(CheckedTextView)v;
				boolean isChecked=!ctv.isChecked();
				ctv.setChecked(isChecked);
				rb_locked_vibrate_val1.setEnabled(isChecked);
				rb_locked_vibrate_val2.setEnabled(isChecked);
				rb_locked_vibrate_val3.setEnabled(isChecked);
				setScreenViewEditMode(true);
			}
		});
		rg_locked_vibrate.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				setScreenViewEditMode(true);
			}
		});

		ct_notify_screen_locked_notification_sound.setChecked(
				mUtil.getPrefMgr().getBoolean(NOTIFY_WHEN_SCREEN_LOCKED_NOTIFICATION_KEY, false));
		String saved_notification_locked_path=mUtil.getPrefMgr().getString(NOTIFY_WHEN_SCREEN_LOCKED_NOTIFICATION_PATH_KEY, def_notification_path);
		String saved_notification_locked_title=mUtil.getPrefMgr().getString(NOTIFY_WHEN_SCREEN_LOCKED_NOTIFICATION_TITLE_KEY, def_notification_title);
		String notification_locked_title=saved_notification_locked_title;
		if (saved_notification_locked_path.equals(def_notification_path)) 
			notification_locked_title=mContext.getString(R.string.msgs_control_screen_notify_notification_def_title);
		tv_notify_screen_locked_notification_title.setText(notification_locked_title);
		tv_notify_screen_locked_notification_title.setTag(saved_notification_locked_path);
		
		ct_notify_screen_unlocked_vibrate.setChecked(
				mUtil.getPrefMgr().getBoolean(NOTIFY_WHEN_SCREEN_UNLOCKED_VIBRATE_KEY, false));
		String saved_unlocked_pattern=mUtil.getPrefMgr().getString(NOTIFY_WHEN_SCREEN_UNLOCKED_VIBRATE_PATTERN_KEY, NOTIFY_WHEN_SCREEN_UNLOCKED_VIBRATE_PATTERN_VALUE1);
		if (saved_unlocked_pattern.equals(NOTIFY_WHEN_SCREEN_UNLOCKED_VIBRATE_PATTERN_VALUE1)) rb_unlocked_vibrate_val1.setChecked(true);
		else if (saved_unlocked_pattern.equals(NOTIFY_WHEN_SCREEN_UNLOCKED_VIBRATE_PATTERN_VALUE2)) rb_unlocked_vibrate_val2.setChecked(true);
		else if (saved_unlocked_pattern.equals(NOTIFY_WHEN_SCREEN_UNLOCKED_VIBRATE_PATTERN_VALUE3)) rb_unlocked_vibrate_val3.setChecked(true);

		rb_unlocked_vibrate_val1.setEnabled(ct_notify_screen_unlocked_vibrate.isChecked());
		rb_unlocked_vibrate_val2.setEnabled(ct_notify_screen_unlocked_vibrate.isChecked());
		rb_unlocked_vibrate_val3.setEnabled(ct_notify_screen_unlocked_vibrate.isChecked());
		ct_notify_screen_unlocked_vibrate.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				CheckedTextView ctv=(CheckedTextView)v;
				boolean isChecked=!ctv.isChecked();
				ctv.setChecked(isChecked);
				rb_unlocked_vibrate_val1.setEnabled(isChecked);
				rb_unlocked_vibrate_val2.setEnabled(isChecked);
				rb_unlocked_vibrate_val3.setEnabled(isChecked);
				setScreenViewEditMode(true);
			}
		});
		rg_unlocked_vibrate.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				setScreenViewEditMode(true);
			}
		});

		ct_notify_screen_unlocked_notification_sound.setChecked(
				mUtil.getPrefMgr().getBoolean(NOTIFY_WHEN_SCREEN_UNLOCKED_NOTIFICATION_KEY, false));
		String saved_notification_unlocked_path=mUtil.getPrefMgr().getString(NOTIFY_WHEN_SCREEN_UNLOCKED_NOTIFICATION_PATH_KEY, def_notification_path);
		String saved_notification_unlocked_title=mUtil.getPrefMgr().getString(NOTIFY_WHEN_SCREEN_UNLOCKED_NOTIFICATION_TITLE_KEY, def_notification_title);
		String notification_unlocked_title=saved_notification_unlocked_title;
		if (saved_notification_unlocked_path.equals(def_notification_path)) 
			notification_unlocked_title=mContext.getString(R.string.msgs_control_screen_notify_notification_def_title);
		tv_notify_screen_unlocked_notification_title.setText(notification_unlocked_title);
		tv_notify_screen_unlocked_notification_title.setTag(saved_notification_unlocked_path);

		if (ct_notify_screen_locked_notification_sound.isChecked()) btn_notify_screen_locked_notification_select_btn.setEnabled(true);
		else btn_notify_screen_locked_notification_select_btn.setEnabled(false);
		ct_notify_screen_locked_notification_sound.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				CheckedTextView ctv=(CheckedTextView)v;
				boolean isChecked=!ctv.isChecked();
				ctv.setChecked(isChecked);
				setScreenViewEditMode(true);
				if (isChecked) btn_notify_screen_locked_notification_select_btn.setEnabled(true);
				else btn_notify_screen_locked_notification_select_btn.setEnabled(false);
			}
		});

		if (ct_notify_screen_unlocked_notification_sound.isChecked()) btn_notify_screen_unlocked_notification_select_btn.setEnabled(true);
		else btn_notify_screen_unlocked_notification_select_btn.setEnabled(false);
		ct_notify_screen_unlocked_notification_sound.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				CheckedTextView ctv=(CheckedTextView)v;
				boolean isChecked=!ctv.isChecked();
				ctv.setChecked(isChecked);
				setScreenViewEditMode(true);
				if (isChecked) btn_notify_screen_unlocked_notification_select_btn.setEnabled(true);
				else btn_notify_screen_unlocked_notification_select_btn.setEnabled(false);
			}
		});

		mNotifyRingtonePickerEnded=new NotifyEvent(mContext);
		mNotifyRingtonePickerEnded.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				int id=(Integer)o[0];
				String r_title=(String)o[1];
				String r_path=(String)o[2];
				String title="";
				if (r_path.equals(def_notification_path)) title=mContext.getString(R.string.msgs_control_screen_notify_notification_def_title);
				else title=r_title;
				if (id==RINGTONE_PICKER_REQUEST_ID_SCREEN_LOCKED) {
					if (r_path.equals("")) {
						ct_notify_screen_locked_notification_sound.setChecked(false);
					} else {
						tv_notify_screen_locked_notification_title.setText(title);
						tv_notify_screen_locked_notification_title.setTag(r_path);
					}
				} else if (id==RINGTONE_PICKER_REQUEST_ID_SCREEN_UNLOCKED) {
					if (r_path.equals("")) {
						ct_notify_screen_unlocked_notification_sound.setChecked(false);
					} else {
						tv_notify_screen_unlocked_notification_title.setText(title);
						tv_notify_screen_unlocked_notification_title.setTag(r_path);
					}
				}
				setScreenViewEditMode(true);
			}

			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
		});
		
		btn_notify_screen_locked_notification_select_btn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
			    final Intent ringtone = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
			    ringtone.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
			    ringtone.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
			    ringtone.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
			            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
			    startActivityForResult(ringtone, RINGTONE_PICKER_REQUEST_ID_SCREEN_LOCKED);
			}
		});

		btn_notify_screen_unlocked_notification_select_btn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
			    final Intent ringtone = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
			    ringtone.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
			    ringtone.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
			    ringtone.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
			            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
			    startActivityForResult(ringtone, RINGTONE_PICKER_REQUEST_ID_SCREEN_UNLOCKED);
			}
		});

	};

	private void setTrustTabMsg() {
		final TextView tv_msg=(TextView)mTrustView.findViewById(R.id.main_trust_view_msg);
//		final TextView tv_device_list_title=(TextView)mTrustView.findViewById(R.id.main_trust_view_trust_device_title);
		final LinearLayout ll_trust_dev_list=(LinearLayout)mTrustView.findViewById(R.id.main_trust_view_trust_device_list_view);
		final LinearLayout ll_delay_time=(LinearLayout)mTrustView.findViewById(R.id.main_trust_view_option_view);
		
		if (Build.VERSION.SDK_INT==24 ||
				(Build.VERSION.SDK_INT==17 && !mEnvParms.settingForceUseTrustDevice)) {
			tv_msg.setVisibility(TextView.VISIBLE);
			ll_trust_dev_list.setVisibility(LinearLayout.GONE);
			ll_delay_time.setVisibility(LinearLayout.GONE);
			if (Build.VERSION.SDK_INT==17) tv_msg.setText(mContext.getString(R.string.msgs_trust_device_sdk_17_not_support));	
			else if (Build.VERSION.SDK_INT==24) tv_msg.setText(mContext.getString(R.string.msgs_trust_device_sdk_24_not_support));
		} else {
			if (mUtil.isDevicePolicyManagerActive()) {
				if (mAdapterTrustList.getCount()==0) {
					tv_msg.setVisibility(TextView.VISIBLE);
					ll_trust_dev_list.setVisibility(LinearLayout.VISIBLE);
//					tv_device_list_title.setVisibility(TextView.GONE);
					ll_delay_time.setVisibility(LinearLayout.GONE);
					tv_msg.setText(mContext.getString(R.string.msgs_trust_device_trust_dev_registered));
				} else {
					ll_trust_dev_list.setVisibility(LinearLayout.VISIBLE);
//					tv_device_list_title.setVisibility(TextView.VISIBLE);
					ll_delay_time.setVisibility(LinearLayout.VISIBLE);
					tv_msg.setVisibility(TextView.GONE);
				}
			} else {
				tv_msg.setVisibility(TextView.VISIBLE);
				ll_trust_dev_list.setVisibility(LinearLayout.GONE);
				ll_delay_time.setVisibility(LinearLayout.GONE);
				tv_msg.setText(mContext.getString(R.string.msgs_trust_device_device_policy_manager_not_activated));	
			}
		}
	};

	
	private void setTrustViewEditMode(boolean enabled) {
		final LinearLayout ll_trust_dev_list=(LinearLayout)mTrustView.findViewById(R.id.main_trust_view_trust_device_list_view);
		final LinearLayout ll_delay_time=(LinearLayout)mTrustView.findViewById(R.id.main_trust_view_option_view);
		LayoutParams lp=(LayoutParams) ll_delay_time.getLayoutParams();
    	final LinearLayout ll_delay_time_btn=(LinearLayout)mTrustView.findViewById(R.id.main_trust_view_delay_time_btn_view);
    	final TextView tv_spacer=(TextView)mTrustView.findViewById(R.id.main_trust_view_trust_device_delay_time_spacer);
		if (enabled) {
			lp.height=LayoutParams.MATCH_PARENT;
			ll_delay_time.setLayoutParams(lp);
			tv_spacer.setVisibility(TextView.VISIBLE);
			ll_delay_time_btn.setVisibility(LinearLayout.VISIBLE);
			ll_delay_time.setVisibility(LinearLayout.VISIBLE);
			ll_trust_dev_list.setVisibility(LinearLayout.GONE);
			setTabSwipeEnabled(false);
			mEditModeEnabled=true;
		} else {
			lp.height=LayoutParams.WRAP_CONTENT;
			ll_delay_time.setLayoutParams(lp);
			tv_spacer.setVisibility(TextView.GONE);
			ll_delay_time_btn.setVisibility(LinearLayout.GONE);
			ll_delay_time.setVisibility(LinearLayout.VISIBLE);
			ll_trust_dev_list.setVisibility(LinearLayout.VISIBLE);
			setTabSwipeEnabled(true);
			mEditModeEnabled=false;
		}
	};

	private String mTempScreenLockPassword="";
	private void setTrustDelayTimeView() {
//		final LinearLayout ll_trust_dev_list=(LinearLayout)mTrustView.findViewById(R.id.main_trust_view_trust_device_list_view);
//		final LinearLayout ll_delay_time=(LinearLayout)mTrustView.findViewById(R.id.main_trust_view_delay_time_view);
		final CheckedTextView ctv_immed_lock=(CheckedTextView)mTrustView.findViewById(R.id.main_trust_view_immediate_lock_when_trusted_device_disconn);
		
    	final CheckedTextView ctv_use_delay_time=(CheckedTextView)mTrustView.findViewById(R.id.main_trust_view_trust_device_use_delay_time);
    	final RadioGroup rg_delay_time=(RadioGroup)mTrustView.findViewById(R.id.main_trust_view_trust_device_rg_delay_time);
    	final RadioButton rb_delay_time_val2=(RadioButton)mTrustView.findViewById(R.id.main_trust_view_trust_device_rb_delay_time_value2);
    	final RadioButton rb_delay_time_val3=(RadioButton)mTrustView.findViewById(R.id.main_trust_view_trust_device_rb_delay_time_value3);
    	final RadioButton rb_delay_time_val4=(RadioButton)mTrustView.findViewById(R.id.main_trust_view_trust_device_rb_delay_time_value4);
    	final LinearLayout ll_delay_time_btn=(LinearLayout)mTrustView.findViewById(R.id.main_trust_view_delay_time_btn_view);
//    	final Button btn_delay_time_save=(Button)mTrustView.findViewById(R.id.main_trust_view_delay_time_btn_save);
//    	final Button btn_delay_time_cancel=(Button)mTrustView.findViewById(R.id.main_trust_view_delay_time_btn_cancel);
    	final LinearLayout ll_reset_password=(LinearLayout)mTrustView.findViewById(R.id.main_trust_view_trust_device_screen_lock_use_pswd_reset_view);
    	final CheckedTextView ctv_use_reset_password=(CheckedTextView)mTrustView.findViewById(R.id.main_trust_view_trust_device_screen_lock_use_pswd_reset);
    	final Button btn_change_password=(Button)mTrustView.findViewById(R.id.main_trust_view_trust_device_screen_lock_pswd_change_btn);
    	ctv_use_delay_time.setOnClickListener(null);
    	rg_delay_time.setOnCheckedChangeListener(null);

    	final TextView tv_spacer=(TextView)mTrustView.findViewById(R.id.main_trust_view_trust_device_delay_time_spacer);
    	tv_spacer.setVisibility(TextView.GONE);

    	String trusted_dev_delay_time_value=mUtil.getPrefMgr().getString(TRUST_DEVICE_DELAY_TIME_VLAUE_KEY, TRUST_DEVICE_DELAY_TIME_NOT_USED);
    	
    	if (trusted_dev_delay_time_value.equals(TRUST_DEVICE_DELAY_TIME_NOT_USED)) {
    		ctv_use_delay_time.setChecked(false);
//    		rg_delay_time.setVisibility(RadioGroup.GONE);
    		rb_delay_time_val2.setEnabled(false);
    		rb_delay_time_val3.setEnabled(false);
    		rb_delay_time_val4.setEnabled(false);
    	} else {
//    		rg_delay_time.setVisibility(RadioGroup.VISIBLE);
    		rb_delay_time_val2.setEnabled(true);
    		rb_delay_time_val3.setEnabled(true);
    		rb_delay_time_val4.setEnabled(true);
    		ctv_use_delay_time.setChecked(true);
    		if (trusted_dev_delay_time_value.equals(TRUST_DEVICE_DELAY_TIME_VALUE2)) rb_delay_time_val2.setChecked(true);
    		else if (trusted_dev_delay_time_value.equals(TRUST_DEVICE_DELAY_TIME_VALUE3)) rb_delay_time_val3.setChecked(true);
    		else if (trusted_dev_delay_time_value.equals(TRUST_DEVICE_DELAY_TIME_VALUE4)) rb_delay_time_val4.setChecked(true);
    	}
    	
    	String trust_dev_immed_lock=
    			mUtil.getPrefMgr().getString(TRUST_DEVICE_IMMEDIATE_LOCK_WHEN_TRUSTED_DEVICE_DISCONN_KEY,
    					TRUST_DEVICE_IMMEDIATE_LOCK_WHEN_TRUSTED_DEVICE_DISCONN_NOT_USED);
    	if (mUtil.isDevicePolicyManagerActive()) {
    		ll_reset_password.setVisibility(CheckedTextView.VISIBLE);
        	mTempScreenLockPassword=mEnvParms.trustedDeviceKeyguardLockPassword;
//        	Log.v("","pswd="+mEnvParms.trustedDeviceKeyguardLockPassword);
        	if (mEnvParms.trustedDeviceKeyguardLockPassword==null || 
        			mEnvParms.trustedDeviceKeyguardLockPassword.equals("")) {
        		ctv_use_reset_password.setChecked(false);
        		btn_change_password.setVisibility(Button.GONE);
        	} else {
        		ctv_use_reset_password.setChecked(true);
        		btn_change_password.setVisibility(Button.VISIBLE);
        	}
        	
        	ctv_use_reset_password.setOnClickListener(new OnClickListener(){
    			@Override
    			public void onClick(View v) {
    				boolean isChecked=!ctv_use_reset_password.isChecked();
    				ctv_use_reset_password.setChecked(isChecked);
    				if (isChecked) {
    					btn_change_password.setVisibility(Button.VISIBLE);
    					if (mEnvParms.trustedDeviceKeyguardLockPassword.equals("")) {
    						NotifyEvent ntfy_reset_pswd=new NotifyEvent(mContext);
    						ntfy_reset_pswd.setListener(new NotifyEventListener(){
								@Override
								public void positiveResponse(Context c, Object[] o) {
		    						btn_change_password.performClick();
								}
								@Override
								public void negativeResponse(Context c, Object[] o) {
									ctv_use_reset_password.setChecked(false);
								}
    						});
    						commonDlg.showCommonDialog(true, "W", 
    								mContext.getString(R.string.msgs_trust_device_screen_lock_pswd_warning_title),
    								mContext.getString(R.string.msgs_trust_device_screen_lock_pswd_warning_msg),
    								ntfy_reset_pswd);
    					}
    				} else {
    					btn_change_password.setVisibility(Button.GONE);
    				}
    				setTrustViewEditMode(true);
    			}
    		});
        	
        	btn_change_password.setOnClickListener(new OnClickListener(){
    			@Override
    			public void onClick(View v) {
    				setTrustViewEditMode(true);
    				NotifyEvent ntfy_pswd=new NotifyEvent(mContext);
    				ntfy_pswd.setListener(new NotifyEventListener(){
    					@Override
    					public void positiveResponse(Context c, Object[] o) {
    						mTempScreenLockPassword=(String)o[0];
    					}
    					@Override
    					public void negativeResponse(Context c, Object[] o) {
    						if (mTempScreenLockPassword.equals("")) {
    							ctv_use_reset_password.setChecked(false);
    						}
    					}
    				});
    				PasswordInputDialogFragment pidf=PasswordInputDialogFragment.newInstance(
    						mContext.getString(R.string.msgs_trust_device_screen_lock_password_dlg_title), 
    						MINUMUM_PASSWORD_LENGTH);
    				pidf.showDialog(mFragmentManager, pidf, ntfy_pswd);
    			}
        	});

    	} else {
    		ll_reset_password.setVisibility(CheckedTextView.GONE);
    	}

    	ll_delay_time_btn.setVisibility(LinearLayout.GONE);
		ctv_use_delay_time.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				boolean isChecked=!ctv_use_delay_time.isChecked();
				ctv_use_delay_time.setChecked(isChecked);
				if (isChecked) {
//					rg_delay_time.setVisibility(RadioGroup.VISIBLE);
		    		rb_delay_time_val2.setEnabled(true);
		    		rb_delay_time_val3.setEnabled(true);
		    		rb_delay_time_val4.setEnabled(true);
				} else {
//					rg_delay_time.setVisibility(RadioGroup.GONE);
		    		rb_delay_time_val2.setEnabled(false);
		    		rb_delay_time_val3.setEnabled(false);
		    		rb_delay_time_val4.setEnabled(false);
				}
				setTrustViewEditMode(true);
			}
		});
		
		if (trust_dev_immed_lock.equals(TRUST_DEVICE_IMMEDIATE_LOCK_WHEN_TRUSTED_DEVICE_DISCONN_NOT_USED)) {
			ctv_immed_lock.setChecked(false);
			ctv_use_delay_time.setEnabled(true);
			if (ctv_use_delay_time.isChecked()) {
	    		rb_delay_time_val2.setEnabled(true);
	    		rb_delay_time_val3.setEnabled(true);
	    		rb_delay_time_val4.setEnabled(true);
			} else {
	    		rb_delay_time_val2.setEnabled(false);
	    		rb_delay_time_val3.setEnabled(false);
	    		rb_delay_time_val4.setEnabled(false);
			}
		} else {
			ctv_use_delay_time.setEnabled(false);
			ctv_use_delay_time.setChecked(false);
			ctv_immed_lock.setChecked(true);
    		rb_delay_time_val2.setEnabled(false);
    		rb_delay_time_val3.setEnabled(false);
    		rb_delay_time_val4.setEnabled(false);
		}

		ctv_immed_lock.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				boolean isChecked=!ctv_immed_lock.isChecked();
				ctv_immed_lock.setChecked(isChecked);
				if (!isChecked) {
					ctv_use_delay_time.setEnabled(true);
					if (ctv_use_delay_time.isChecked()) {
			    		rb_delay_time_val2.setEnabled(true);
			    		rb_delay_time_val3.setEnabled(true);
			    		rb_delay_time_val4.setEnabled(true);
					} else {
			    		rb_delay_time_val2.setEnabled(false);
			    		rb_delay_time_val3.setEnabled(false);
			    		rb_delay_time_val4.setEnabled(false);
					}
				} else {
					ctv_use_delay_time.setEnabled(false);
		    		rb_delay_time_val2.setEnabled(false);
		    		rb_delay_time_val3.setEnabled(false);
		    		rb_delay_time_val4.setEnabled(false);
				}
				setTrustViewEditMode(true);
			}
		});
    	rg_delay_time.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				setTrustViewEditMode(true);
			}
    	});
    	checkControlOptionInconsistency();
    	
	};

	private void checkControlOptionInconsistency() {
    	final CheckedTextView ctv_use_delay_time=(CheckedTextView)mTrustView.findViewById(R.id.main_trust_view_trust_device_use_delay_time);
//    	final RadioGroup rg_delay_time=(RadioGroup)mTrustView.findViewById(R.id.main_trust_view_trust_device_rg_delay_time);
//    	final RadioButton rb_delay_time_val1=(RadioButton)mTrustView.findViewById(R.id.main_trust_view_trust_device_rb_delay_time_value1);
    	final RadioButton rb_delay_time_val2=(RadioButton)mTrustView.findViewById(R.id.main_trust_view_trust_device_rb_delay_time_value2);
    	final RadioButton rb_delay_time_val3=(RadioButton)mTrustView.findViewById(R.id.main_trust_view_trust_device_rb_delay_time_value3);
    	final RadioButton rb_delay_time_val4=(RadioButton)mTrustView.findViewById(R.id.main_trust_view_trust_device_rb_delay_time_value4);
//    	final LinearLayout ll_delay_time_btn=(LinearLayout)mTrustView.findViewById(R.id.main_trust_view_delay_time_btn_view);
    	
    	if (mAdapterTrustList.getCount()==0) return;
    	
    	boolean bt_reged=false, wifi_reged=false;
    	for(TrustItem tdli:mAdapterTrustList.getAllItem()) {
    		if (tdli.trustItemType==TrustItem.TYPE_BLUETOOTH_CLASSIC_DEVICE) bt_reged=true;
    		else if (tdli.trustItemType==TrustItem.TYPE_WIFI_AP) wifi_reged=true;
    	}
    	
    	int reconnect_time=Integer.parseInt(TRUST_DEVICE_DELAY_TIME_VALUE2)-1;
		if (rb_delay_time_val2.isChecked()) reconnect_time=Integer.parseInt(TRUST_DEVICE_DELAY_TIME_VALUE2)-1; 
		else if (rb_delay_time_val3.isChecked()) reconnect_time=Integer.parseInt(TRUST_DEVICE_DELAY_TIME_VALUE3)-1;
		else if (rb_delay_time_val4.isChecked()) reconnect_time=Integer.parseInt(TRUST_DEVICE_DELAY_TIME_VALUE4)-1;
		
//		Log.v("","recon="+reconnect_time);
		
		String warn_msg="";
		boolean t_bt_opt1=false, t_bt_opt2=false;

		String bt_off_when_screen_locked=mUtil.getPrefMgr().getString(BLUETOOTH_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_KEY,BLUETOOTH_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_DISABLED);
		String bt_off_when_connect_timeout=mUtil.getPrefMgr().getString(BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_KEY,BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_DISABLED);
		int bt_connect_timeout_val=Integer.parseInt(mUtil.getPrefMgr().getString(BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_VALUE_KEY,"1"));

		if (bt_reged) {
			
			if (!bt_off_when_screen_locked.equals(BLUETOOTH_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_DISABLED)) {
				//Screenロック時に時間指定で信頼できる装置がオフにされる警告
				warn_msg+=mContext.getString(R.string.msgs_control_inconsistencey_check_blutooth_off_screen_locked)+"\n";
				t_bt_opt1=true;
			}
			
			if (ctv_use_delay_time.isChecked() && 
					!bt_off_when_connect_timeout.equals(BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_DISABLED)) {
				if (reconnect_time>bt_connect_timeout_val) {
					//再接続が許容される時間内に信頼できる装置がオフにされる警告
					warn_msg+="\n"+mContext.getString(R.string.msgs_control_inconsistencey_check_blutooth_off_connect_timeout)+"\n";
					t_bt_opt2=true;
				}
			}
			
		}

		boolean t_wifi_opt1=false, t_wifi_opt2=false;
		String wifi_off_when_screen_locked=mUtil.getPrefMgr().getString(WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_KEY,WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_DISABLED);
		String wifi_off_when_connect_timeout=mUtil.getPrefMgr().getString(WIFI_OFF_WHEN_CONNECT_TIMEOUT_KEY,WIFI_OFF_WHEN_CONNECT_TIMEOUT_DISABLED);
		int wifi_connect_timeout_val=Integer.parseInt(mUtil.getPrefMgr().getString(WIFI_OFF_WHEN_CONNECT_TIMEOUT_VALUE_KEY,"1"));
		if (wifi_reged) {
			if (!wifi_off_when_screen_locked.equals(WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_DISABLED)) {
				//Screenロック時に時間指定で信頼できる装置がオフにされる警告
				warn_msg+="\n"+mContext.getString(R.string.msgs_control_inconsistencey_check_wifi_off_screen_locked)+"\n";
				t_wifi_opt1=true;
			}
			
			if (ctv_use_delay_time.isChecked() &&
					!wifi_off_when_connect_timeout.equals(WIFI_OFF_WHEN_CONNECT_TIMEOUT_DISABLED)) {
				if (reconnect_time>wifi_connect_timeout_val) {
					//再接続が許容される時間内に信頼できる装置がオフにされる警告
					warn_msg+="\n"+mContext.getString(R.string.msgs_control_inconsistencey_check_wifi_off_connect_timeout)+"\n";
					t_wifi_opt2=true;
				}
			}
		}
		if (!warn_msg.equals("")) {
			final boolean bt_opt1=t_bt_opt1, bt_opt2=t_bt_opt2;
			final boolean wifi_opt1=t_wifi_opt1, wifi_opt2=t_wifi_opt2;
			final String reconnect_time_value=String.valueOf(reconnect_time);
			NotifyEvent ntfy=new NotifyEvent(mContext);
			ntfy.setListener(new NotifyEventListener(){
				@Override
				public void positiveResponse(Context c, Object[] o) {
					if (bt_opt1) {
						mUtil.getPrefMgr().edit().putString(BLUETOOTH_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_KEY,BLUETOOTH_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_DISABLED).commit();						
					}
					if (bt_opt2) {
						mUtil.getPrefMgr().edit().putString(BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_VALUE_KEY,reconnect_time_value).commit();						
					}
					if (wifi_opt1) {
						mUtil.getPrefMgr().edit().putString(WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_KEY,WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_DISABLED).commit();						
					}
					if (wifi_opt2) {
						mUtil.getPrefMgr().edit().putString(WIFI_OFF_WHEN_CONNECT_TIMEOUT_VALUE_KEY,reconnect_time_value).commit();						
					}
					setControlViewContents(mControlView);
					CommonUtilities.restartScheduler(mContext);
				}
				@Override
				public void negativeResponse(Context c, Object[] o) {
				}
			});
			commonDlg.showCommonDialog(true, "W", 
					mContext.getString(R.string.msgs_control_inconsistencey_check_title), warn_msg, ntfy);
		}
	}
	
	private void setTrustItemListView() {
		NotifyEvent ntfy_cb_listener=new NotifyEvent(mContext);
    	ntfy_cb_listener.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				if (mAdapterTrustList.isShowCheckBox()) {
					setTrustContextButtonSelectMode(mAdapterTrustList);
				}
			};

			@Override
			public void negativeResponse(Context c, Object[] o) {}
    	});

		final NotifyEvent ntfy_alarm=new NotifyEvent(mContext);
		ntfy_alarm.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				String item_name=(String)o[0];
				String dev_name=(String)o[1];
				String dev_addr=(String)o[2];
				confirmSendAlarm(mActivity, mUtil, item_name, dev_name, dev_addr);
			};

			@Override
			public void negativeResponse(Context c, Object[] o) {}
    	});

		final NotifyEvent ntfy_enable=new NotifyEvent(mContext);
		ntfy_enable.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				int pos=(Integer)o[0];
				final TrustItem tli=mAdapterTrustList.getItem(pos);
				tli.setSelected(true);
				NotifyEvent ntfy=new NotifyEvent(mContext);
				ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						mAdapterTrustList.setShowCheckBox(false);
						mAdapterTrustList.setAllItemSelected(false);
						mAdapterTrustList.notifyDataSetChanged();
						saveTrustDeviceList();
						setTrustContextButtonNormalMode(mAdapterTrustList);
//						for(int i=0;i<mAdapterTrustList.getCount();i++)
//							Log.v("","le="+mAdapterTrustList.getItem(i).isEnabled()+", type="+mAdapterTrustList.getItem(i).trustItemName);
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
						mAdapterTrustList.setAllItemSelected(false);
						mAdapterTrustList.notifyDataSetChanged();
					}
				});
				if (tli.isEnabled()) confirmInactivateItem(mAdapterTrustList,ntfy);
				else confirmActivateItem(mAdapterTrustList,ntfy);
			};

			@Override
			public void negativeResponse(Context c, Object[] o) {}
    	});

    	mTrustListView=(ListView)mTrustView.findViewById(R.id.main_trust_view_trust_device_list);
    	ArrayList<TrustItem>tl=loadTrustItemList();
    	mAdapterTrustList=new TrustItemListAdapter(this, R.layout.trust_dev_list_view_item, 
    			tl, ntfy_cb_listener, null, ntfy_alarm, ntfy_enable);
		mTrustListView.setAdapter(mAdapterTrustList);
		
		NotifyEvent ntfy_edit=new NotifyEvent(mContext);
		ntfy_edit.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				int pos=(Integer)o[0];
				NotifyEvent ntfy=new NotifyEvent(c);
				ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						saveTrustDeviceList();
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
					}
				});
				TrustItemEditDlg ted=new TrustItemEditDlg(mActivity, c, mGp, mEnvParms, mUtil, 
						mFragmentManager, mAdapterTrustList);
				ted.showDlg(ntfy, mAdapterTrustList, mAdapterTrustList.getItem(pos));
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {}
		});
		mAdapterTrustList.setNotifyEditClickListener(ntfy_edit);
		
    	mTrustListView.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
				if (mAdapterTrustList.getItem(0).trustDeviceName==null) return;
				if (mAdapterTrustList.isShowCheckBox()) {
					mAdapterTrustList.getItem(pos).setSelected(!mAdapterTrustList.getItem(pos).isSelected());
					mAdapterTrustList.notifyDataSetChanged();
					setTrustContextButtonSelectMode(mAdapterTrustList);
//		        	if (mAdapterTrustList.isAnyItemSelected()) {
//		        		setTrustContextButtonSelectMode(mAdapterTrustList);
//		        	} else {
//		        		setTrustContextButtonNormalMode(mAdapterTrustList);
//		        	}
				} else {
//					editTrustItem(setTrustTabSaveButtonListener(),
//							mAdapterTrustList, mAdapterTrustList.getItem(pos));
					ntfy_enable.notifyToListener(true, new Object[]{pos});
				}
			}
    	});
    	
    	mTrustListView.setOnItemLongClickListener(new OnItemLongClickListener(){
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
				if (mAdapterTrustList.isEmptyAdapter()) return true;
				if (!mAdapterTrustList.getItem(pos).isSelected()) {
					if (mAdapterTrustList.isAnyItemSelected()) {
						int down_sel_pos=-1, up_sel_pos=-1;
						int tot_cnt=mAdapterTrustList.getCount();
						if (pos+1<=tot_cnt) {
							for(int i=pos+1;i<tot_cnt;i++) {
								if (mAdapterTrustList.getItem(i).isSelected()) {
									up_sel_pos=i;
									break;
								}
							}
						}
						if (pos>0) {
							for(int i=pos;i>=0;i--) {
								if (mAdapterTrustList.getItem(i).isSelected()) {
									down_sel_pos=i;
									break;
								}
							}
						}
//						Log.v("","up="+up_sel_pos+", down="+down_sel_pos);
						if (up_sel_pos!=-1 && down_sel_pos==-1) {
							for (int i=pos;i<up_sel_pos;i++) 
								mAdapterTrustList.getItem(i).setSelected(true);
						} else if (up_sel_pos!=-1 && down_sel_pos!=-1) {
							for (int i=down_sel_pos+1;i<up_sel_pos;i++) 
								mAdapterTrustList.getItem(i).setSelected(true);
						} else if (up_sel_pos==-1 && down_sel_pos!=-1) {
							for (int i=down_sel_pos+1;i<=pos;i++) 
								mAdapterTrustList.getItem(i).setSelected(true);
						}
						mAdapterTrustList.notifyDataSetChanged();
					} else {
						mAdapterTrustList.setShowCheckBox(true);
						mAdapterTrustList.getItem(pos).setSelected(true);
						mAdapterTrustList.notifyDataSetChanged();
					}
					setTrustContextButtonSelectMode(mAdapterTrustList);
				}
				return true;
			}
    	});

	};
	
	private void confirmSendAlarm(final Activity activity, final CommonUtilities util, 
			String item_name, final String dev_name, final String addr) {

    	NotifyEvent ntfy=new NotifyEvent(null);
    	ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				NotifyEvent ntfy=new NotifyEvent(mContext);
				ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						mUiHandler.postDelayed(new Runnable(){
							@Override
							public void run() {
								try {
									mSvcServer.reScanBtLeDevice();
								} catch (RemoteException e) {
									e.printStackTrace();
								}
							}
						}, 100);
					}
					@Override
					public void negativeResponse(Context c,final  Object[] o) {
						if (o!=null) {
//							final String cb_type=(String)o[0];
							final int status=(Integer)o[2];
							if (status!=0) {
								mUiHandler.post(new Runnable(){
									@Override
									public void run() {
										commonDlg.showCommonDialog(false, "E", 
												"SendAlarm error Name="+dev_name+", addr="+addr+", status="+status, "", null);
										try {
											mSvcServer.reScanBtLeDevice();
										} catch (RemoteException e) {
											e.printStackTrace();
										}
									}
								});
							}
						}
					}
				});
				BtLeUtil.sendAlertByAddr(activity, util, ntfy, addr, BtLeUtil.ALERT_TYPE_ALARM);
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {}
    	});
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(true, "W",
        		String.format(mContext.getString(R.string.msgs_trust_device_send_alarm_confirm_msg),
        			item_name+"("+dev_name+" "+addr+")"), "");
        cdf.showDialog(getSupportFragmentManager(),cdf,ntfy);

	};
	
	private TrustItemListAdapter mAdapterTrustList=null;
	private ListView mTrustListView=null;
	private void setTabTrustView() {
		final CheckedTextView ctv_immed_lock=(CheckedTextView)mTrustView.findViewById(R.id.main_trust_view_immediate_lock_when_trusted_device_disconn);
//		final LinearLayout ll_trust_dev_list=(LinearLayout)mTrustView.findViewById(R.id.main_trust_view_trust_device_list_view);
//		final LinearLayout ll_delay_time=(LinearLayout)mTrustView.findViewById(R.id.main_trust_view_delay_time_view);
    	final CheckedTextView ctv_use_delay_time=(CheckedTextView)mTrustView.findViewById(R.id.main_trust_view_trust_device_use_delay_time);
//    	final RadioGroup rg_delay_time=(RadioGroup)mTrustView.findViewById(R.id.main_trust_view_trust_device_rg_delay_time);
    	final RadioButton rb_delay_time_val2=(RadioButton)mTrustView.findViewById(R.id.main_trust_view_trust_device_rb_delay_time_value2);
    	final RadioButton rb_delay_time_val3=(RadioButton)mTrustView.findViewById(R.id.main_trust_view_trust_device_rb_delay_time_value3);
    	final RadioButton rb_delay_time_val4=(RadioButton)mTrustView.findViewById(R.id.main_trust_view_trust_device_rb_delay_time_value4);

    	final CheckedTextView ctv_use_reset_password=(CheckedTextView)mTrustView.findViewById(R.id.main_trust_view_trust_device_screen_lock_use_pswd_reset);
//    	final Button btn_change_password=(Button)mTrustView.findViewById(R.id.main_trust_view_trust_device_screen_lock_password_change_btn);

//    	final LinearLayout ll_delay_time_btn=(LinearLayout)mTrustView.findViewById(R.id.main_trust_view_delay_time_btn_view);
    	final Button btn_delay_time_save=(Button)mTrustView.findViewById(R.id.main_trust_view_delay_time_btn_save);
    	final Button btn_delay_time_cancel=(Button)mTrustView.findViewById(R.id.main_trust_view_delay_time_btn_cancel);

    	setTrustItemListView();
    	
		setTrustTabMsg();
		
		updateTrustDeviceConnectStatus();
		
    	setTrustContextButtonListener();
    	setTrustContextButtonNormalMode(mAdapterTrustList);
    	
    	setTrustDelayTimeView();
    	
    	btn_delay_time_save.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				String trust_dev_immed_lock_value="0";
//				String prev_pswd=mEnvParms.trustedDeviceKeyguardLockPassword;
				if (ctv_immed_lock.isChecked()) trust_dev_immed_lock_value=TRUST_DEVICE_IMMEDIATE_LOCK_WHEN_TRUSTED_DEVICE_DISCONN_USED;
				mUtil.getPrefMgr().edit().putString(TRUST_DEVICE_IMMEDIATE_LOCK_WHEN_TRUSTED_DEVICE_DISCONN_KEY, trust_dev_immed_lock_value).commit();
				
				String trusted_dev_delay_time_value=TRUST_DEVICE_DELAY_TIME_NOT_USED;
				if (ctv_use_delay_time.isChecked()) {
					if (rb_delay_time_val2.isChecked()) trusted_dev_delay_time_value=TRUST_DEVICE_DELAY_TIME_VALUE2;
					else if (rb_delay_time_val3.isChecked()) trusted_dev_delay_time_value=TRUST_DEVICE_DELAY_TIME_VALUE3;
					else if (rb_delay_time_val4.isChecked()) trusted_dev_delay_time_value=TRUST_DEVICE_DELAY_TIME_VALUE4;
				} 
				mUtil.getPrefMgr().edit().putString(TRUST_DEVICE_DELAY_TIME_VLAUE_KEY, trusted_dev_delay_time_value).commit();
				
				if (ctv_use_reset_password.isChecked()) {
//					Log.v("","tp="+mTempScreenLockPassword);
					if (mTempScreenLockPassword.equals("")) {
						mEnvParms.clearDpmPassword(mContext);
					} else {
						if (!mTempScreenLockPassword.equals(mEnvParms.trustedDeviceKeyguardLockPassword)) {
							mEnvParms.putDpmPassword(mContext, mTempScreenLockPassword);
							mEnvParms.trustedDeviceKeyguardLockPassword=mTempScreenLockPassword;
						}
					}
				} else {
					if (!mEnvParms.trustedDeviceKeyguardLockPassword.equals("")) {
						mEnvParms.clearDpmPassword(mContext);
						mEnvParms.trustedDeviceKeyguardLockPassword="";
						if (mUtil.isDevicePolicyManagerActive()) {
							CommonUtilities.removePassword(mContext);
							String msg=mContext.getString(R.string.msgs_trust_device_screen_lock_pswd_delete_msg);
							if (Build.VERSION.SDK_INT==21) msg+=" "+mContext.getString(R.string.msgs_trust_device_screen_lock_pswd_restart_msg);
				        	commonDlg.showCommonDialog(false, "W", 
				        			mContext.getString(R.string.msgs_trust_device_screen_lock_pswd_delete_title), msg, null);
						}
					}
				}
				
				CommonUtilities.resetScheduler(mContext);
				setTrustViewEditMode(false);
				setTrustDelayTimeView();
				setTrustTabMsg();
				
//				if (!prev_pswd.equals(mEnvParms.trustedDeviceKeyguardLockPassword) && 
//						!mEnvParms.trustedDeviceKeyguardLockPassword.equals("") && 
//						Build.VERSION.SDK_INT==21) {
//					commonDlg.showCommonDialog(false, "W", 
//							mContext.getString(R.string.msgs_trust_device_screen_lock_pswd_delete_title), 
//							mContext.getString(R.string.msgs_trust_device_screen_lock_pswd_restart_msg), null);
//				}
			}
        });

    	btn_delay_time_cancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				setTrustDelayTimeView();
				setTrustViewEditMode(false);
				setTrustTabMsg();
			}
        });

//    	addTrustedPlace(null);
	};

	private void setActionBarSelectMode(int sel_cnt, int total_cnt) {
		ActionBar actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
        String sel_txt=""+sel_cnt+"/"+total_cnt;
        actionBar.setTitle(sel_txt);
        
        mEditModeEnabled=true;
	};

	private void setActionBarNormalMode() {
		ActionBar actionBar = getSupportActionBar();
		actionBar.setTitle(R.string.app_name);
		actionBar.setHomeButtonEnabled(false);
		actionBar.setDisplayHomeAsUpEnabled(false);
		
		mEditModeEnabled=false;
	};

	private void saveTrustDeviceList() {
		CommonUtilities.saveTrustedDeviceTable(mContext, mAdapterTrustList.getAllItem());
		CommonUtilities.reloadTrustDeviceList(mContext);
//		updateTrustDeviceConnectStatus();
		setTrustTabMsg();
		checkLocationSetting(mAdapterTrustList.getAllItem());
	};
	
	private void checkLocationSetting(ArrayList<TrustItem>tl) {
		if (Build.VERSION.SDK_INT>=23) {
			if (tl!=null && tl.size()>0) {
				for(int i=0;i<tl.size();i++) {
					TrustItem ti=tl.get(i);
					Log.v("","name="+ti.trustDeviceName+", addr="+ti.trustDeviceAddr+", type="+ti.trustItemType+", enabled="+ti.isEnabled());
					if (ti.trustItemType==TrustItem.TYPE_BLUETOOTH_LE_DEVICE && ti.isEnabled()) {
						LocationManager mLocationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);
						if(!mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) &&
								!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))   {
							commonDlg.showCommonDialog(false, "W", 
									mContext.getString(R.string.msgs_main_bt_le_can_not_scan_location_setting_diabled_title), 
									mContext.getString(R.string.msgs_main_bt_le_can_not_scan_location_setting_diabled_msg), null); 
					    } 
						break;
					}
				}
			}
		}
	};
	
	private NotifyEvent setTrustTabSaveButtonListener() {
    	final NotifyEvent ntfy_save_btn=new NotifyEvent(mContext);
    	ntfy_save_btn.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				mAdapterTrustList.setShowCheckBox(false);
				mAdapterTrustList.setAllItemSelected(false);
				mAdapterTrustList.notifyDataSetChanged();
				setTrustContextButtonNormalMode(mAdapterTrustList);
				setActionBarNormalMode();
				saveTrustDeviceList();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
    	});
    	return ntfy_save_btn;
	};
	
	private void setTrustContextButtonListener() {
		LinearLayout ll_prof=(LinearLayout) mTrustView.findViewById(R.id.main_trust_view_context_view);
		ImageButton ib_refresh=(ImageButton)ll_prof.findViewById(R.id.context_button_refresh);
		ImageButton ib_add=(ImageButton)ll_prof.findViewById(R.id.context_button_add);
		ImageButton ib_activate=(ImageButton)ll_prof.findViewById(R.id.context_button_activate);
		ImageButton ib_inactivate=(ImageButton)ll_prof.findViewById(R.id.context_button_inactivate);
        ImageButton ib_delete=(ImageButton)ll_prof.findViewById(R.id.context_button_delete);
        ImageButton ib_select_all=(ImageButton)ll_prof.findViewById(R.id.context_button_select_all);
        ImageButton ib_unselect_all=(ImageButton)ll_prof.findViewById(R.id.context_button_unselect_all);

    	final NotifyEvent ntfy_save_btn=setTrustTabSaveButtonListener();

    	ib_refresh.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				try {
					mSvcServer.reScanBtLeDevice();
					Toast toast=Toast.makeText(mContext, mContext.getString(R.string.msgs_trust_device_refresh_started), Toast.LENGTH_SHORT);
					toast.setDuration(1500);
					if ((mLastShowTime+1500)<System.currentTimeMillis()) {
						toast.show();
						mLastShowTime=System.currentTimeMillis();
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, ib_refresh, 
        		mContext.getString(R.string.msgs_trust_device_label_refresh));

    	ib_add.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				TrustItemAddDlg ad=new TrustItemAddDlg(mActivity, mContext, mGp, mEnvParms, mUtil, 
						mFragmentManager, mAdapterTrustList);
				ad.showDlg(ntfy_save_btn);
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, ib_add, 
        		mContext.getString(R.string.msgs_trust_device_label_add));
        
        ib_activate.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				confirmActivateItem(mAdapterTrustList, ntfy_save_btn);
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, ib_activate, 
        		mContext.getString(R.string.msgs_trust_device_label_activate));

        ib_inactivate.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				confirmInactivateItem(mAdapterTrustList, ntfy_save_btn);
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, ib_inactivate, 
        		mContext.getString(R.string.msgs_trust_device_label_inactivate));

        ib_delete.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				confirmDeleteItem(mAdapterTrustList, ntfy_save_btn);
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, ib_delete, 
        		mContext.getString(R.string.msgs_trust_device_label_delete));
        
        ib_select_all.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				mAdapterTrustList.setAllItemSelected(true);
				mAdapterTrustList.setShowCheckBox(true);
				mAdapterTrustList.notifyDataSetChanged();
				setTrustContextButtonSelectMode(mAdapterTrustList);
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, ib_select_all, 
        		mContext.getString(R.string.msgs_trust_device_label_select_all));

        ib_unselect_all.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				mAdapterTrustList.setAllItemSelected(false);
//				mAdapterTrustList.setShowCheckBox(false);
				mAdapterTrustList.notifyDataSetChanged();
				setTrustContextButtonSelectMode(mAdapterTrustList);
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, ib_unselect_all, 
        		mContext.getString(R.string.msgs_trust_device_label_unselect_all));

	};
	
	private void setTrustContextButtonSelectMode(TrustItemListAdapter lfm_adapter) {
    	int sel_cnt=lfm_adapter.getItemSelectedCount();
    	setActionBarSelectMode(sel_cnt,lfm_adapter.getCount());
    	
		LinearLayout ll_prof=(LinearLayout) mTrustView.findViewById(R.id.main_trust_view_context_view);
		LinearLayout ll_activate=(LinearLayout)ll_prof.findViewById(R.id.context_button_activate_view);
		LinearLayout ll_inactivate=(LinearLayout)ll_prof.findViewById(R.id.context_button_inactivate_view);
		LinearLayout ll_refresh=(LinearLayout)ll_prof.findViewById(R.id.context_button_refresh_view);
		LinearLayout ll_add=(LinearLayout)ll_prof.findViewById(R.id.context_button_add_view);
		LinearLayout ll_delete=(LinearLayout)ll_prof.findViewById(R.id.context_button_delete_view);
		LinearLayout ll_select_all=(LinearLayout)ll_prof.findViewById(R.id.context_button_select_all_view);
		LinearLayout ll_unselect_all=(LinearLayout)ll_prof.findViewById(R.id.context_button_unselect_all_view);

		boolean enabled=false;
//		int sel=0;
		boolean disabled=false;
		boolean deletable_log_selected=false;
		for(int i=0;i<lfm_adapter.getCount();i++) {
			if (lfm_adapter.getItem(i).isSelected() && !lfm_adapter.getItem(i).isEnabled()) {
				enabled=true;
			}
			if (lfm_adapter.getItem(i).isSelected() && lfm_adapter.getItem(i).isEnabled()) {
				disabled=true;
			}
			if (lfm_adapter.getItem(i).isSelected()) {
				deletable_log_selected=true;
//				sel++;
			}
		}
		if (enabled) {
			if (lfm_adapter.isAnyItemSelected()) {
				ll_activate.setVisibility(LinearLayout.VISIBLE);
			} else {
				ll_activate.setVisibility(LinearLayout.GONE);
			}
		} else {
			ll_activate.setVisibility(LinearLayout.GONE);
		}

		if (disabled) {
			if (lfm_adapter.isAnyItemSelected()) {
				ll_inactivate.setVisibility(LinearLayout.VISIBLE);
			} else {
				ll_inactivate.setVisibility(LinearLayout.GONE);
			}
		} else {
			ll_inactivate.setVisibility(LinearLayout.GONE);
		}

		ll_add.setVisibility(LinearLayout.GONE);
		ll_refresh.setVisibility(LinearLayout.GONE);
		
		if (deletable_log_selected) ll_delete.setVisibility(LinearLayout.VISIBLE);
		else ll_delete.setVisibility(LinearLayout.GONE);
		
        ll_select_all.setVisibility(LinearLayout.VISIBLE);
        
        if (lfm_adapter.isAnyItemSelected()) {
        	ll_unselect_all.setVisibility(LinearLayout.VISIBLE);
        } else {
        	ll_unselect_all.setVisibility(LinearLayout.GONE);
        }
	};

	private void setTrustContextButtonNormalMode(TrustItemListAdapter lfm_adapter) {
    	setActionBarNormalMode();

		LinearLayout ll_prof=(LinearLayout) mTrustView.findViewById(R.id.main_trust_view_context_view);
		LinearLayout ll_activate=(LinearLayout)ll_prof.findViewById(R.id.context_button_activate_view);
		LinearLayout ll_inactivate=(LinearLayout)ll_prof.findViewById(R.id.context_button_inactivate_view);
		LinearLayout ll_refresh=(LinearLayout)ll_prof.findViewById(R.id.context_button_refresh_view);
		ImageButton ib_refresh=(ImageButton)ll_prof.findViewById(R.id.context_button_refresh);
		LinearLayout ll_add=(LinearLayout)ll_prof.findViewById(R.id.context_button_add_view);
		LinearLayout ll_delete=(LinearLayout)ll_prof.findViewById(R.id.context_button_delete_view);
		LinearLayout ll_select_all=(LinearLayout)ll_prof.findViewById(R.id.context_button_select_all_view);
		LinearLayout ll_unselect_all=(LinearLayout)ll_prof.findViewById(R.id.context_button_unselect_all_view);

		if (Build.VERSION.SDK_INT==24 ||
				(Build.VERSION.SDK_INT==17 && !mEnvParms.settingForceUseTrustDevice)) {
			ll_add.setVisibility(LinearLayout.GONE);
			ll_refresh.setVisibility(LinearLayout.GONE);
		} else {
			boolean le_exist=false;
			for(TrustItem ti:lfm_adapter.getAllItem()) 
				if (ti.trustItemType==TrustItem.TYPE_BLUETOOTH_LE_DEVICE && ti.isEnabled()) le_exist=true;
			if (le_exist && BluetoothAdapter.getDefaultAdapter()!=null && 
					BluetoothAdapter.getDefaultAdapter().isEnabled()) {
				ll_refresh.setVisibility(LinearLayout.VISIBLE);
//				Log.v("","started="+mBtLeDeviceScanStarted)
				if (isBtLeDeviceScanStarted()) {
					ib_refresh.setEnabled(false);
					ib_refresh.setImageResource(R.drawable.context_button_refresh_disabled);
				} else {
					ib_refresh.setEnabled(true);
					ib_refresh.setImageResource(R.drawable.context_button_refresh);
				}
			} else ll_refresh.setVisibility(LinearLayout.GONE);
			ll_add.setVisibility(LinearLayout.VISIBLE);
		}
		
		ll_activate.setVisibility(LinearLayout.GONE);
		ll_inactivate.setVisibility(LinearLayout.GONE);
		
		ll_delete.setVisibility(LinearLayout.GONE);
        
    	if (lfm_adapter.isEmptyAdapter()) {
            ll_select_all.setVisibility(LinearLayout.GONE);
            ll_unselect_all.setVisibility(LinearLayout.GONE);
    	} else {
            ll_select_all.setVisibility(LinearLayout.VISIBLE);
            ll_unselect_all.setVisibility(LinearLayout.GONE);
    	}
	};

//	private void addTrustedPlace(final NotifyEvent ntfy_save_btn) {
//		PlacePicker.IntentBuilder pp=new PlacePicker.IntentBuilder();
//		try {
//			Intent in=pp.build(mContext);
//			startActivityForResult(in,PLACE_PICKER_REQUEST_ID);
//		} catch (GooglePlayServicesRepairableException
//				| GooglePlayServicesNotAvailableException e) {
//			e.printStackTrace();
//		}
//	};

    private void confirmActivateItem(final TrustItemListAdapter adapter,  final NotifyEvent ntfy_save_btn) {
    	String sel_list="",sep="";
    	for (int i=0;i<adapter.getCount();i++) {
    		TrustItem item=adapter.getItem(i);
    		if (item.isSelected()) {
    			String type="B";
    			if (item.trustItemType==TrustItem.TYPE_WIFI_AP) type="W";
    			if (item.trustDeviceAddr.equals("")) sel_list+=sep+type+" "+item.trustDeviceName;
    			else sel_list+=sep+type+" "+item.trustDeviceName+"("+item.trustDeviceAddr+")";
    			sep="\n";
    		}
    	}
//    	if (sel_list.length()>0) sel_list+="\n";
    	
    	NotifyEvent ntfy=new NotifyEvent(null);
    	ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				for(int i=0;i<adapter.getCount();i++) {
					if (adapter.getItem(i).isSelected()) adapter.getItem(i).setEnabled(true);
				}
				adapter.setAllItemSelected(false);
				adapter.setShowCheckBox(false);
				adapter.notifyDataSetChanged();
				ntfy_save_btn.notifyToListener(true, null);
			}

			@Override
			public void negativeResponse(Context c, Object[] o) {
				ntfy_save_btn.notifyToListener(false, null);
			}
    	});
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(true, "W",
        		mContext.getString(R.string.msgs_trust_device_activate_confirm_msg),
        		sel_list);
        cdf.showDialog(getSupportFragmentManager(),cdf,ntfy);
    };

    private void confirmInactivateItem(final TrustItemListAdapter adapter,  final NotifyEvent ntfy_save_btn) {
    	String sel_list="",sep="";
    	for (int i=0;i<adapter.getCount();i++) {
    		TrustItem item=adapter.getItem(i);
    		if (item.isSelected()) {
    			String type="B";
    			if (item.trustItemType==TrustItem.TYPE_WIFI_AP) type="W";
    			if (item.trustDeviceAddr.equals("")) sel_list+=sep+type+" "+item.trustDeviceName;
    			else sel_list+=sep+type+" "+item.trustDeviceName+"("+item.trustDeviceAddr+")";
    			sep="\n";
    		}
    	}
//    	if (sel_list.length()>0) sel_list+="\n";
    	
    	NotifyEvent ntfy=new NotifyEvent(null);
    	ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				for(int i=0;i<adapter.getCount();i++) {
					if (adapter.getItem(i).isSelected()) adapter.getItem(i).setEnabled(false);
				}
				adapter.setAllItemSelected(false);
				adapter.setShowCheckBox(false);
				adapter.notifyDataSetChanged();
				ntfy_save_btn.notifyToListener(true, null);
			}

			@Override
			public void negativeResponse(Context c, Object[] o) {
				ntfy_save_btn.notifyToListener(false, null);
			}
    	});
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(true, "W",
        		mContext.getString(R.string.msgs_trust_device_inactivate_confirm_msg),
        		sel_list);
        cdf.showDialog(getSupportFragmentManager(),cdf,ntfy);
    };

    private void confirmDeleteItem(final TrustItemListAdapter adapter,  final NotifyEvent ntfy_save_btn) {
    	final ArrayList<TrustItem> del_tl=new ArrayList<TrustItem>();
    	String delete_list="",sep="";
    	for (int i=0;i<adapter.getCount();i++) {
    		TrustItem item=adapter.getItem(i);
    		if (item.isSelected()) {
    			String type="B";
    			if (item.trustItemType==TrustItem.TYPE_WIFI_AP) type="W";
    			if (item.trustDeviceAddr.equals("")) delete_list+=sep+type+" "+item.trustDeviceName;
    			else delete_list+=sep+type+" "+item.trustDeviceName+"("+item.trustDeviceAddr+")";
    			sep="\n";
    			del_tl.add(item);
    		}
    	}
//    	if (delete_list.length()>0) delete_list+="\n";
    	
    	NotifyEvent ntfy=new NotifyEvent(null);
    	ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				for(int i=0;i<del_tl.size();i++) adapter.remove(del_tl.get(i));
				adapter.setAllItemSelected(false);
				adapter.setShowCheckBox(false);
				adapter.notifyDataSetChanged();
				ntfy_save_btn.notifyToListener(true, null);
			}

			@Override
			public void negativeResponse(Context c, Object[] o) {}
    	});
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(true, "W",
        		mContext.getString(R.string.msgs_trust_device_delete_confirm_msg),
        		delete_list);
        cdf.showDialog(getSupportFragmentManager(),cdf,ntfy);
    };
 	
	final static private boolean isBooleanDifferent(boolean p1, boolean p2) {
		boolean result=true;
		if (p1 && p2) result=false;
		else if(!p1 && !p2) result=false;
		return result;
	};

	final private void switchDeviceAdminStatus(boolean activate) {
        DevicePolicyManager dpm =(DevicePolicyManager)getSystemService(
				Context.DEVICE_POLICY_SERVICE);
        final ComponentName darcn = new ComponentName(
        		mContext, DevAdmReceiver.class);
        if (activate && !dpm.isAdminActive(darcn)) {
        	NotifyEvent ntfy=new NotifyEvent(mContext);
        	ntfy.setListener(new NotifyEventListener(){
				@Override
				public void positiveResponse(Context c, Object[] o) {
		        	Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
		        	intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,darcn);
		        	startActivityForResult(intent,10);
				}
				@Override
				public void negativeResponse(Context c, Object[] o) {}
        	});
            if (mApplicationRunFirstTime) {
            	commonDlg.showCommonDialog(false, "I", 
            			mContext.getString(R.string.msgs_main_screen_lock_confirm_title), 
            			mContext.getString(R.string.msgs_main_screen_lock_confirm_msg),ntfy);
            } else ntfy.notifyToListener(true, null);

		} else {
			if (!activate) dpm.removeActiveAdmin(darcn);
		}
	};

	final private void invokeLogFileBrowser() {
		if (DEBUG_ENABLE) mUtil.addDebugMsg(1,"I","Invoke log file browser.");
		mUtil.resetLogReceiver();
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		intent.setDataAndType(Uri.parse("file://"+
				mEnvParms.settingLogMsgDir+mEnvParms.settingLogMsgFilename+".txt"),
				"text/plain");
		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			commonDlg.showCommonDialog(false, "E", 
					mContext.getString(R.string.msgs_main_log_file_browse_app_can_not_found), e.getMessage(), null);
		}
	};
	
	final private void invokeSettingsActivity() {
		if (DEBUG_ENABLE) mUtil.addDebugMsg(1,"I","Invoke Settings.");
		Intent intent = new Intent(this, ActivitySettings.class);
//		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivityForResult(intent,0);
	};

	final protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (DEBUG_ENABLE) mUtil.addDebugMsg(1,"I","Return from External activity. ID="+
				requestCode+", result="+resultCode);
		if (requestCode==0) {
			if (resultCode==Activity.RESULT_OK) applySettingParms();
		} else if (requestCode==10) {
			if (resultCode!=Activity.RESULT_OK) {
				getPrefsMgr().edit().putBoolean(
						getString(R.string.settings_main_device_admin), false).commit();
				mEnvParms.settingDeviceAdmin=false;
			}
			setTabTrustView();
			setTabScreenView();
			setTabControlView();
		} else if ((requestCode==RINGTONE_PICKER_REQUEST_ID_SCREEN_LOCKED) ||
				(requestCode==RINGTONE_PICKER_REQUEST_ID_SCREEN_UNLOCKED)) {
			if (resultCode==Activity.RESULT_OK) {
				Uri uri =null;
				if (data!=null) uri=data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
				if (uri!=null) {
					Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
					if (ringtone!=null) {
				        String title=ringtone.getTitle(this);
				        String path=uri.getPath();
				        mNotifyRingtonePickerEnded.notifyToListener(true, new Object[]{requestCode,title,path});
					} else {
						mNotifyRingtonePickerEnded.notifyToListener(true, new Object[]{requestCode,"",""});
					}
				} else {
					mNotifyRingtonePickerEnded.notifyToListener(true, new Object[]{requestCode,"",""});
				}
			}
		} else if (requestCode==PLACE_PICKER_REQUEST_ID) {
			if (resultCode==Activity.RESULT_OK) {
//				Place place=PlacePicker.getPlace(data, mContext);
//				Log.v("","lat="+place.getLatLng().latitude+", long="+place.getLatLng().longitude);
			}
		}
	};
	
	final public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			confirmTerminateApplication();
			return true;
			// break;
		default:
			return super.onKeyDown(keyCode, event);
			// break;
		}
	};

	private long mLastShowTime=0l;
	final private void confirmTerminateApplication() {
		if (!mSwipeEnabled) {
			Toast toast=Toast.makeText(mContext, mContext.getString(R.string.msgs_dlg_hardkey_back_button), Toast.LENGTH_SHORT);
			toast.setDuration(1500);
			if ((mLastShowTime+1500)<System.currentTimeMillis()) {
				toast.show();
				mLastShowTime=System.currentTimeMillis();
			}
			return;
		} else {
			if (mMainTabHost.getCurrentTab()==0) {
				if (mAdapterTrustList.isShowCheckBox()) {
					processHomeButtonPress();
					return;
				}
			}
		}
		mUtil.addLogMsg("I",getString(R.string.msgs_main_termination));
		mIsApplicationTerminated=true;
		finish();
	};

	final private void bindSchedulerService(final NotifyEvent p_ntfy) {
		if (mSvcServer != null) return;
		mUtil.addDebugMsg(1,"I", "bindScheduler entered");
		
        mSvcConnScheduler = new ServiceConnection(){
        	final public void onServiceConnected(ComponentName name, IBinder service) {
				mUtil.addDebugMsg(1, "I", "Callback onServiceConnected entered");
    			mSvcServer = ISchedulerClient.Stub.asInterface(service);
				setCallbackListener();
    			if (p_ntfy!=null) p_ntfy.notifyToListener(true, null);
    		}
        	final public void onServiceDisconnected(ComponentName name) {
				mUtil.addDebugMsg(1, "I", "Callback onServiceDisconnected entered");
    			mSvcServer = null;
    		}
    	};
		Intent intent = new Intent(mContext, SchedulerService.class);
		intent.setAction("Main");
		bindService(intent, mSvcConnScheduler, BIND_AUTO_CREATE);
	};
	
	final private void unbindScheduler() { 
		mUtil.addDebugMsg(1, "I", "unbindScheduler entered");
		if (mSvcClientCallback!=null) {
			unsetCallbackListener();
			mSvcClientCallback=null;
		}
		unbindService(mSvcConnScheduler);
	};
	
	private boolean mBtLeDeviceScanStarted=false;
	final private void setBtLeDeviceScanStarted(boolean p) {
		mBtLeDeviceScanStarted=p;
//		mUtil.addDebugMsg(1, "I", "setBtLeDeviceScanStarted result="+p);
	};

	final private boolean isBtLeDeviceScanStarted() {
//		mUtil.addDebugMsg(1, "I", "isBtLeDeviceScanStarted result="+mBtLeDeviceScanStarted);
		return mBtLeDeviceScanStarted;
	};

	final private void setCallbackListener() {
		mUtil.addDebugMsg(1, "I", "setCallbackListener entered");
        mSvcClientCallback = new ISchedulerCallback.Stub() {
        	final public void notifyToClient(String cb_type) 
        			throws RemoteException {
        		if (mEnvParms==null  || mGp==null) return;
				if (mEnvParms.settingDebugLevel>=2)
					mUtil.addDebugMsg(2, "I", "Callback received ", "Type=",cb_type);
				if (cb_type.equals(CB_NETWORK_CHANGED)) {
					mUiHandler.post(new Runnable(){
						@Override
						public void run() {
							updateTrustDeviceConnectStatus();
						}
					});
				} else if (cb_type.equals(CB_BTLE_SCAN_STARTED)) {
					mUiHandler.post(new Runnable(){
						@Override
						public void run() {
							setBtLeDeviceScanStarted(true);
							updateTrustDeviceConnectStatus();
							if (mAdapterTrustList!=null) {
								if (!mAdapterTrustList.isShowCheckBox()) 
									setTrustContextButtonNormalMode(mAdapterTrustList);
							}
						}
					});
				} else if (cb_type.equals(CB_BTLE_SCAN_ENDED)) {
					mUiHandler.post(new Runnable(){
						@Override
						public void run() {
							setBtLeDeviceScanStarted(false);
							updateTrustDeviceConnectStatus();
							if (mAdapterTrustList!=null) {
								if (!mAdapterTrustList.isShowCheckBox()) 
									setTrustContextButtonNormalMode(mAdapterTrustList);
							}
						}
					});
				}
			}
        };
		try{
			mSvcServer.setCallBack(mSvcClientCallback);
		} catch (RemoteException e){
			e.printStackTrace();
			mUtil.addLogMsg("E", "setCallbackListener error :"+e.toString());
		}
	};
	
	private ArrayList<TrustItem> loadTrustItemList() {
		byte[] dev_buf=null;
		try {
			dev_buf=mSvcServer.getTrustItemList();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		ObjectInputStream ois=null;
		ArrayList<TrustItem> t_list=new ArrayList<TrustItem>();
		if (dev_buf!=null) {
			try {
				ois = new ObjectInputStream(new ByteArrayInputStream(dev_buf));
				t_list=CommonUtilities.deSerilizeTrustItemList(ois);
			} catch (StreamCorruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (t_list==null) return new ArrayList<TrustItem>(); 
		else return t_list;
	};
	
	private void updateTrustDeviceConnectStatus() {
		
		WifiManager wm=(WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
		String wifi_addr="", wifi_ssid="";
		wifi_ssid=CommonUtilities.getWifiSsidName(wm);
		wifi_addr=wm.getConnectionInfo().getBSSID();
		
		ArrayList<TrustItem>tl=loadTrustItemList();
		
		if (!wifi_ssid.equals("")) {
			for(int j=0;j<tl.size();j++) {
				TrustItem tdi=tl.get(j);
				if (tdi.trustItemType==TrustItem.TYPE_WIFI_AP) {
					if (tdi.trustDeviceName.equals(wifi_ssid)) {
						if (!tdi.trustDeviceAddr.equals("")) {
							if (tdi.trustDeviceAddr.equals(wifi_addr)) 
								tdi.setConnected(true); 
						} else tdi.setConnected(true);
					}
				}
			}
		}
		mAdapterTrustList.setAllItem(tl);
		mAdapterTrustList.notifyDataSetChanged();
		
		if (mAdapterTrustList.isShowCheckBox()) setTrustContextButtonSelectMode(mAdapterTrustList);
		else setTrustContextButtonNormalMode(mAdapterTrustList);
	};
	
	final private void unsetCallbackListener() {
		try{
			if (mSvcServer!=null) mSvcServer.removeCallBack(mSvcClientCallback);
		} catch (RemoteException e){
			e.printStackTrace();
			mUtil.addLogMsg("E", "unsetCallbackListener error :"+e.toString());
		}
	};

	final private void saveTaskData() {
		ActivityMainDataHolder data = new ActivityMainDataHolder();
		try {
		    FileOutputStream fos=openFileOutput(ACTIVITY_TASK_DATA_FILE_NAME, MODE_PRIVATE);
		    ObjectOutputStream oos = new ObjectOutputStream(fos);
		    oos.writeObject(data);
		    oos.close();
		    mUtil.addDebugMsg(1,"I", "Activity data was saved");
		} catch (Exception e) {
			e.printStackTrace();
			mUtil.addDebugMsg(1,"E", "saveActivityData error, "+e.getMessage());
		}
	};
	
	@SuppressWarnings("unused")
	final private void restoreTaskData() {
		try {
		    File lf =new File(getFilesDir()+"/"+ACTIVITY_TASK_DATA_FILE_NAME);
		    FileInputStream fis = new FileInputStream(lf); 
		    ObjectInputStream ois = new ObjectInputStream(fis);
		    ActivityMainDataHolder data = (ActivityMainDataHolder) ois.readObject();
		    ois.close();
		    mUtil.addDebugMsg(1,"I", "Activity data was restored");
		} catch (Exception e) {
			e.printStackTrace();
			mUtil.addDebugMsg(1,"E","restoreActivityData error, "+e.getMessage());
		}
	};

	final private void deleteTaskData() {
	    File lf =new File(getFilesDir()+"/"+ACTIVITY_TASK_DATA_FILE_NAME);
	    if (lf.exists()) {
		    lf.delete();
		    mUtil.addDebugMsg(2,"I", "Activity data was deleted");
	    }
	};

}

class ActivityMainDataHolder implements Serializable  {
	private static final long serialVersionUID = 1L;
};

