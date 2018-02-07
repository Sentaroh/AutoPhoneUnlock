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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.sentaroh.android.AutoPhoneUnlock.ISchedulerCallback;
import com.sentaroh.android.AutoPhoneUnlock.ISchedulerClient;
import com.sentaroh.android.AutoPhoneUnlock.R;
import com.sentaroh.android.AutoPhoneUnlock.ScanRecord.ScanRecordCompat;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;
import com.sentaroh.android.Utilities.SerializeUtil;
import com.sentaroh.android.Utilities.ThreadCtrl;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.PowerManager;
import android.os.Vibrator;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public final class SchedulerService extends Service {

	static private BroadcastReceiver mBatteryStatusReceiver=null;
	static private BroadcastReceiver mWifiReceiver=null;
	static private BroadcastReceiver mSleepReceiver=null;
	static private BroadcastReceiver mBluetoothReceiver=null;
	static private MiscellaneousReceiver mMiscellaneousReceiver=null;

	static private Context mContext;
	
	static private CommonUtilities mUtil=null;
	
	static private EnvironmentParms mEnvParms=null;
	static private TaskManagerParms mTaskMgrParms=null;

    private static Sensor mSensorProximity=null;

	static private ProximitySensorReceiver mReceiverProximity=null;
	static private SensorManager mSensorManager=null;

	static private WakeLock mWakelockForSleep=null;
	
	static private AudioManager mAudioManager;
	
	static private Service mSvcInstance=null;
	
	static private WifiManager mWifiMgr=null;
	
	static private Handler mUiHandler=null;

	static private GlobalParameters mGp=null;
	
	static private ThreadCtrl mTcBtLeScanTimer=null;
	
	static private boolean mIsBluetoothLeSupported=false;
	
	static private boolean mIgnoreDeviceDiscoonectedHostAction=false;
	
	@Override
	public void onConfigurationChanged(Configuration newconfig) {
		mUtil.addDebugMsg(1,"I","onConfigurationChanged entered,"+
				" current Orientation="+mEnvParms.currentOrientation+
				", new Orientation="+newconfig.orientation);
		if (mEnvParms.currentOrientation!=newconfig.orientation) {
			mEnvParms.currentOrientation=newconfig.orientation;
		}
	};
	
	@SuppressLint("NewApi")
	@Override
    public void onCreate() {
//    	StrictMode.enableDefaults();
		mContext=getApplicationContext();
		mSvcInstance=this;
		mGp=new GlobalParameters();
		mSensorManager=(SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);
		mWifiMgr=(WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
		mUiHandler=new Handler();

		mEnvParms=new EnvironmentParms();
		mTaskMgrParms=new TaskManagerParms();

		mTaskMgrParms.svcMsgs.loadString(mContext);
		mEnvParms.loadSettingParms(mContext);

		mGp.setLogParms(mEnvParms);
        mUtil=new CommonUtilities(mContext, "Scheduler", mEnvParms, mGp);
		mWakelockForSleep=((PowerManager)mContext.getSystemService(Context.POWER_SERVICE))
    			.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
    					| PowerManager.ON_AFTER_RELEASE, "AutoPhoneUnlock-Sensor");

		mAudioManager=(AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
		
		mUtil.addDebugMsg(1,"I","onCreate entered");
		
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			mIsBluetoothLeSupported=false;
			mUtil.addDebugMsg(1, "W", "Bluetooth low energy is Not supprted");
		} else {
			mIsBluetoothLeSupported=true;
		}

		LocationManager lm = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);
		if(!lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) &&
				!lm.isProviderEnabled(LocationManager.GPS_PROVIDER))   {
			mUtil.addDebugMsg(1, "W", "Location setting is disabled");
	    } 

		listInitSettingsParm();
		mEnvParms.loadControlOption(mContext);
		listControlOption();

		mTcBluetoothConnect=new ThreadCtrl();
		mTcWifiConnect=new ThreadCtrl();
		mTcProximityDetected=new ThreadCtrl();
		mTcProximityUndetected=new ThreadCtrl();
		mTcWifiDeviceReconnectTimer=new ThreadCtrl();
		mTcBluetoothDeviceReconnectTimer=new ThreadCtrl();
		mTcWifiOffTimer=new ThreadCtrl();
		mTcBluetoothOffTimer=new ThreadCtrl();
		mTcBtLeScanTimer=new ThreadCtrl();
		mTcBtLeScanTimer.setDisabled();
		
		mUtil.addLogMsg("I",mTaskMgrParms.svcMsgs.msgs_svc_started, " ", String.valueOf(android.os.Process.myPid()));

	    TaskManager.initTaskMgrParms(mEnvParms,mTaskMgrParms, mContext, mUtil);
		TaskManager.initNotification(mTaskMgrParms, mEnvParms);
		
        mTaskMgrParms.locationUtil=new LocationUtilities(mTaskMgrParms,mEnvParms,mUtil);

    	mEnvParms.currentOrientation=mSvcInstance.getResources().getConfiguration().orientation;

		mEnvParms.screenIsLocked=mUtil.isKeyguardEffective();
		mEnvParms.screenIsOn=CommonUtilities.isScreenOn(mContext);

 		mSensorProximity=mUtil.isProximitySensorAvailable();
 		if (mSensorProximity!=null) mEnvParms.proximitySensorAvailable=true;
 		
 		mEnvParms.currentRingerMode=mAudioManager.getRingerMode();
 		
 		TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
		mEnvParms.telephonyStatus=tm.getCallState();
 		
 		mEnvParms.airplane_mode_on=getAirplaneModeOn();

 		mEnvParms.setKeyGuardStatusLocked();
 		mEnvParms.setKgEnabled(mContext);
 		isValidDpmPasswordLength();
 		
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
		    	if (BluetoothAdapter.getDefaultAdapter()!=null) {
		 			mEnvParms.bluetoothIsAvailable=true;
		 	 		if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
		 	 			mEnvParms.bluetoothIsActive=true;
//		 	 			connectBluetoothLeDevice();
		 	 			if (mEnvParms.isBluetoothConnected()) {
			 	 			TrustItem tdli=mEnvParms.getBluetoothConnectedDeviceList().get(mEnvParms.getBluetoothConnectedDeviceList().size()-1);
			 	 			mEnvParms.bluetoothLastEventDeviceAddr=tdli.trustDeviceAddr;
			 	 			mEnvParms.bluetoothLastEventDeviceName=tdli.trustDeviceName;
		 	 			}
		 	 		} else {
		 	    		mEnvParms.bluetoothIsActive=false;
		 	    		mEnvParms.clearBluetoothConnectedDeviceList(c);
		 	 		}
		    	} else {
		    		mEnvParms.bluetoothIsAvailable=false;
		    		mEnvParms.bluetoothIsActive=false;
		    	}
				if (mWifiMgr!=null) {
					mEnvParms.wifiIsActive=mWifiMgr.isWifiEnabled();
					String wssid=CommonUtilities.getWifiSsidName(mWifiMgr);
					String tmac=mWifiMgr.getConnectionInfo().getBSSID();
					mEnvParms.wifiSsidName=wssid;
					mEnvParms.wifiSsidAddr=tmac;
					if (!wssid.equals("")) mEnvParms.wifiIsConnected=true;
				}

				mUtil.addLogMsg("I","EnvironmentParameters initialized");
				mUtil.addLogMsg("I","    Airplane mode on="+mEnvParms.airplane_mode_on);
				mUtil.addLogMsg("I","    Ringer mode="+mEnvParms.currentRingerMode);
				mUtil.addLogMsg("I","    Telephony status="+mEnvParms.telephonyStatus);
				mUtil.addLogMsg("I","    Bluetooth active="+mEnvParms.bluetoothIsActive);
				ArrayList<TrustItem>bdl=mEnvParms.getBluetoothConnectedDeviceList();
				if (bdl.size()>0) {
					for(int i=0;i<bdl.size();i++) {
						mUtil.addLogMsg("I","    Bluetooth device name="+bdl.get(i).trustDeviceName+", addr="+bdl.get(i).trustDeviceAddr);
					}
				} else {
					mUtil.addLogMsg("I","    Bluetooth device not connected");
				}
				mUtil.addLogMsg("I","    Wifi active="+mEnvParms.wifiIsActive);
				mUtil.addLogMsg("I","    Wifi SSID="+mEnvParms.wifiSsidName);
				mUtil.addLogMsg("I","    Screen locked="+mEnvParms.screenIsLocked);
				mUtil.addLogMsg("I","    Proxiity sensor available="+mEnvParms.proximitySensorAvailable);

				startBasicEventReceiver(mContext);

				if (mEnvParms.settingEnableScheduler) {
					setHeartBeat(mContext);
		 	   		TaskManager.showNotification(mTaskMgrParms, mEnvParms, mUtil);
		 			mSvcInstance.startForeground(R.string.app_name,mTaskMgrParms.mainNotification);
				}

				initialExecuteSchedulerTask(mContext);
				
				startBtLeIntervalScanTimer();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
		});
		
		mEnvParms.buildBluetoothConnectedDeviceList(mContext,ntfy,mUtil);
		
    };


    
	@Override
    public int onStartCommand(Intent in, int flags, int startId) {
		acqSvcWakeLock();
		String t_act="";
    	if (in!=null && in.getAction()!=null) t_act=in.getAction();
    	else t_act="";
    	final String action=t_act;
		if (mEnvParms.settingDebugLevel>=2 && !action.equals(BROADCAST_SERVICE_HEARTBEAT))
			mUtil.addDebugMsg(2,"I","onStartCommand entered, action=",action,", flag=",String.valueOf(flags));
		processIntent(in, action);
		relSvcWakeLock();
    	return START_STICKY; //START_STICKY;
    };

    @SuppressLint("NewApi")
	final private void processIntent(Intent in, String action) {
		if (action.equals(BROADCAST_RELOAD_DEVICE_ADMIN)) {
			mEnvParms.settingDeviceAdmin=
					CommonUtilities.getPrefMgr(mContext).getBoolean(mContext.getString(R.string.settings_main_device_admin),false);
		} else if (action.equals(BROADCAST_DISABLE_KEYGUARD)) {
			mEnvParms.setKeyGuardStatusUnlocked();
			mEnvParms.setKgDisabled(mContext);
			mUtil.addDebugMsg(1,"I","disableKeyguard issued");
		} else if (action.equals(BROADCAST_ENABLE_KEYGUARD)) {
			mEnvParms.setKeyGuardStatusLocked();
			mEnvParms.setKgEnabled(mContext);
			mUtil.addDebugMsg(1,"I","enableKeyguard issued");
		} else if (action.equals(BROADCAST_START_ACTIVITY_MAIN)) {
			Intent in_b=new Intent(mContext.getApplicationContext(),ActivityMain.class);
			in_b.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mContext.startActivity(in_b);
		} else if (action.equals("android.media.VOLUME_CHANGED_ACTION")) {
    		cancelPlayBackDefaultAlarm();
    		cancelVibrateDefaultPattern();
		} else if (action.equals(BROADCAST_SERVICE_HEARTBEAT)) {
			cancelHeartBeat(mContext);
			setHeartBeat(mContext);
    		resourceCleanup(mTaskMgrParms, mEnvParms, mUtil);
		} else if (action.equals(Intent.ACTION_SHUTDOWN)) {
			mEnvParms.setKeyGuardStatusLocked();
			mEnvParms.setKgEnabled(mContext);
			mUtil.addDebugMsg(1,"I","Shutdown received. enableKeyguard issued");
		} else if (action.equals(Intent.ACTION_LOCALE_CHANGED)) {
			mTaskMgrParms.svcMsgs.loadString(mContext);
			if (mEnvParms.batteryChargeStatusInt==mEnvParms.BATTERY_CHARGE_STATUS_INT_CHARGING){
				mEnvParms.batteryChargeStatusString=mTaskMgrParms.svcMsgs.msgs_widget_battery_status_charge_charging;
			} else if (mEnvParms.batteryChargeStatusInt==mEnvParms.BATTERY_CHARGE_STATUS_INT_DISCHARGING){
				mEnvParms.batteryChargeStatusString=mTaskMgrParms.svcMsgs.msgs_widget_battery_status_charge_discharging;
			} else if (mEnvParms.batteryChargeStatusInt==mEnvParms.BATTERY_CHARGE_STATUS_INT_FULL){
				mEnvParms.batteryChargeStatusString=mTaskMgrParms.svcMsgs.msgs_widget_battery_status_charge_full;
			}
			if (mEnvParms.settingEnableScheduler) 
				TaskManager.showNotification(mTaskMgrParms, mEnvParms, mUtil);
		} else if (action.equals(BROADCAST_RESET_SCHEDULER)) {
			mEnvParms.loadSettingParms(mContext);
			mEnvParms.proximitySensorEventTemporaryIgnore=false;
			TaskManager.cancelErrorNotification(mTaskMgrParms);
			mGp.setLogParms(mEnvParms);
			mEnvParms.loadControlOption(mContext);
			listInitSettingsParm();
			listControlOption();
 			reloadTrustItemList();
	 		if (mEnvParms.settingEnableScheduler) {
	 			startBtLeIntervalScanTimer();
	 			scanBtLeDevice(true);
	 			if (mEnvParms.isWifiConnected()) isTrustedWifiAccessPointConnected(BUILTIN_EVENT_WIFI_CONNECTED);
	 			if (mEnvParms.isBluetoothConnected()) isTrustedBluetoothDeviceConnected(BUILTIN_EVENT_BLUETOOTH_CONNECTED);
	 			resetKeyGuard(true);
	 			mSvcInstance.startForeground(R.string.app_name,mTaskMgrParms.mainNotification);
		   		TaskManager.showNotification(mTaskMgrParms, mEnvParms, mUtil);
	 		} else {
	 			stopBtLeIntervalScanTimer();
	 			mSvcInstance.stopForeground(true);
	 			setKeyguardEnabled(mTaskMgrParms, mEnvParms, mUtil);
	 		}
		} else if (action.equals(BROADCAST_RELOAD_TRUST_DEVICE_LIST)) {
			reloadTrustItemList();
			if (mEnvParms.settingEnableScheduler) {
	 			isTrustedWifiAccessPointConnected(BUILTIN_EVENT_WIFI_CONNECTED);
	 			isTrustedBluetoothDeviceConnected(BUILTIN_EVENT_BLUETOOTH_CONNECTED);
	 			scanBtLeDevice(false);
	 			resetKeyGuard(false);
			}
		} else if (action.equals(BROADCAST_START_SCHEDULER)) {
			if (mEnvParms.settingEnableScheduler) {
				startBtLeIntervalScanTimer();
				setHeartBeat(mContext);
	 	   		TaskManager.showNotification(mTaskMgrParms, mEnvParms, mUtil);
	 			mSvcInstance.startForeground(R.string.app_name,mTaskMgrParms.mainNotification);
			}
//		} else if (action.equals(BROADCAST_STOP_SCHEDULER)) {
//			stopSelf();
    	} else if (action.equals(BROADCAST_RESTART_SCHEDULER)) {
//	 		if (!mEnvParms.settingEnableScheduler) mSvcInstance.stopForeground(true);
    		restartScheduler();
    	} else if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
    	} else if (action.equals(BROADCAST_TOGGLE_SILENT)) {
    		toggleSilent();
    	} else if (action.equals(BROADCAST_LOCK_SCREEN)) {
    		forceLockScreen();
		} 
		checkTerminateService();
		
    };
    
    static final private void reloadTrustItemList() {
		ArrayList<TrustItem> new_t_list=CommonUtilities.loadTrustedDeviceTable(mContext, mEnvParms);
 		if (mEnvParms.settingEnableScheduler) {
 			ArrayList<TrustItem> del_conn_list=new ArrayList<TrustItem>();
 			if (new_t_list.size()==0) {
 				for(int i=0;i<mEnvParms.trustItemList.size();i++) 
 					del_conn_list.add(mEnvParms.trustItemList.get(i)); 
 			} else {
	 			for(TrustItem n_item:new_t_list) {
	 				if (n_item.trustItemType==TrustItem.TYPE_BLUETOOTH_CLASSIC_DEVICE ||
	 						n_item.trustItemType==TrustItem.TYPE_BLUETOOTH_LE_DEVICE) {
		 				for(TrustItem o_item:mEnvParms.trustItemList) {
		 					if (n_item.trustDeviceName.equals(o_item.trustDeviceName) &&
	 							o_item.trustDeviceAddr.equals(n_item.trustDeviceAddr)) {
								if (o_item.isEnabled() && !n_item.isEnabled()) {
									//disabled old entry
									if (n_item.trustItemType==TrustItem.TYPE_BLUETOOTH_CLASSIC_DEVICE) {
										n_item.setConnected(false);
									} else {
										del_conn_list.add(o_item);
									}
								} else if (!o_item.isEnabled() && n_item.isEnabled()) {
									//connect old entry
								} else if (o_item.isEnabled() && n_item.isEnabled()) {
//									Log.v("","old type="+o_item.trustItemType+", name="+o_item.trustDeviceName+", conn="+o_item.isConnected());
//									Log.v("","new type="+n_item.trustItemType+", name="+n_item.trustDeviceName+", conn="+n_item.isConnected());
									n_item.setConnected(o_item.isConnected());
									n_item.hasImmedAlert=o_item.hasImmedAlert;
									//check to connect mode
									if ((n_item.isBtLeDeviceConnectMode && !o_item.isBtLeDeviceConnectMode) ||
											(!n_item.isBtLeDeviceConnectMode && o_item.isBtLeDeviceConnectMode)) {
										del_conn_list.add(o_item);
									} else if ((n_item.bleDeviceLinkLossActionToTag!=o_item.bleDeviceLinkLossActionToTag) 
											|| (n_item.bleDeviceLinkLossActionToHost!=o_item.bleDeviceLinkLossActionToHost) ||
											(n_item.bleDeviceNotifyButtonAction!=o_item.bleDeviceNotifyButtonAction)
											) {
										del_conn_list.add(o_item);
									}
 								}
								break;
		 					}
		 				}
//		 				if (!found) {
//		 					//removed by trust device list
//		 					del_conn_list.add(n_item);
//		 				}
	 				}
	 			}
 			}
 			final boolean immed_lock=mEnvParms.trustedDeviceImmediateLockWhenDisconnected;
 			mEnvParms.trustedDeviceImmediateLockWhenDisconnected=false;
 			setIgnoreDeviceDisconnectHostAction(2000);
 			mEnvParms.trustItemList=new_t_list;
 			if (del_conn_list.size()>0) {
 				for(int i=0;i<del_conn_list.size();i++) {
 						bluetoothReceiverDeviceDisconnected(-1, del_conn_list.get(i).trustItemType,
 								del_conn_list.get(i).trustDeviceName, del_conn_list.get(i).trustDeviceAddr);
 				}
 			}
 			mUiHandler.postDelayed(new Runnable(){
				@Override
				public void run() {
					mEnvParms.trustedDeviceImmediateLockWhenDisconnected=immed_lock;
				}
 			}, 1000*2);
 		} else {
			mEnvParms.trustItemList=new_t_list;
 		}

    };
    
    static final private boolean isIgnoreDeviceDisconnectHostAction() {
    	return mIgnoreDeviceDiscoonectedHostAction;
    }
    static final private void setIgnoreDeviceDisconnectHostAction(long duration) {
    	mIgnoreDeviceDiscoonectedHostAction=true;
		mUiHandler.postDelayed(new Runnable(){
			@Override
			public void run() {
//				Log.v("","resumed");
				mIgnoreDeviceDiscoonectedHostAction=false;
			}
		}, duration);
    };
    
    static final private void forceLockScreen() {
		final WakeLock wl=obtainTaskWakeLock();
//    	Runnable th=new Runnable(){
//    		@Override
//    		public void run(){
//        	}
//    	};
//    	TaskManager.executeTaskByHighPriority(mEnvParms, mTaskMgrParms, mUtil, th);
		if (mEnvParms.settingDeviceAdmin) {
			//SCREEN_OFFでのICON処理をバイパスするためにmEnvParms.enableKeyguard=trueを行わない。
			if (isValidDpmPasswordLength()) {
	    		if (mEnvParms.isKeyGuardStatusUnlocked()) mEnvParms.setKeyGuardStatusManualUnlockRequired();
	    		mEnvParms.setKgEnabled(mContext);
	    		mUtil.screenLockNow();
			}
		} else {
			Toast.makeText(mContext, 
					mContext.getString(R.string.msgs_widget_battery_button_not_functional), Toast.LENGTH_SHORT).show();
		}
		if (wl.isHeld()) wl.release();
    };
    
    static final private void toggleSilent() {
		final WakeLock wl=obtainTaskWakeLock();
		final Handler hndl=new Handler();
    	Runnable th=new Runnable(){
    		@SuppressWarnings("deprecation")
			@Override
    		public void run(){
    			if (mAudioManager.getRingerMode()==AudioManager.RINGER_MODE_NORMAL) {
    				mAudioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
    				mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
    				mAudioManager.setStreamMute(AudioManager.STREAM_RING, true);
    				mAudioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
    				mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
    				mAudioManager.setStreamMute(AudioManager.STREAM_ALARM, true);
    				if (Build.VERSION.SDK_INT==21) {
    					hndl.postDelayed(new Runnable(){
    						@Override
    						public void run() {
    	    					mAudioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
    						}
    					}, 200);
    				}
    				TaskManager.showNotification(mTaskMgrParms, mEnvParms, mUtil);
    			} else {
    				mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
    				mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
    				mAudioManager.setStreamMute(AudioManager.STREAM_RING, false);
    				mAudioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, false);
    				mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
    				mAudioManager.setStreamMute(AudioManager.STREAM_ALARM, false);
    	    		TaskManager.showNotification(mTaskMgrParms, mEnvParms, mUtil);
    			}  
        		if (wl.isHeld()) wl.acquire();
    		}
    	};
    	TaskManager.executeTaskByHighPriority(mEnvParms, mTaskMgrParms, mUtil, th);
    };
    
    final private void checkTerminateService() {
    	if (!mEnvParms.settingEnableScheduler)  {
    		stopSelf();
//    	} else {
//        	if (mEnvParms.bluetoothOffConnectionTimeout.equals(BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_DISABLED) &&
//        			mEnvParms.bluetoothOnScreenUnlocked.equals(BLUETOOTH_ON_WHEN_SCREEN_UNLOCK_DISABLED) &&
//        			mEnvParms.wifiOffConnectionTimeout.equals(WIFI_OFF_WHEN_CONNECT_TIMEOUT_DISABLED) &&
//        			mEnvParms.wifiOnScreenUnlocked.equals(WIFI_ON_WHEN_SCREEN_UNLOCK_DISABLED) &&
//        			mEnvParms.proximityDetected.equals(PROXIMITY_DETECTED_DISABLED) &&
//        			mEnvParms.proximityUndetected.equals(PROXIMITY_UNDETECTED_DISABLED) &&
//        			mTaskMgrParms.trustDeviceList.size()==0) {
//        		stopSelf();
//        	}
    	}
    };
    
    static final private boolean isValidDpmPasswordLength() {
    	boolean result=true;
 		if (Build.VERSION.SDK_INT>=23) {
 			if (mEnvParms.trustedDeviceKeyguardLockPassword!=null &&
 					!mEnvParms.trustedDeviceKeyguardLockPassword.equals("") &&
 					mEnvParms.trustedDeviceKeyguardLockPassword.length()<MINUMUM_PASSWORD_LENGTH) {
 	    		Intent in_b=
 	    				new Intent(mContext.getApplicationContext(),ActivityMessageDlg.class);
// 	    		in_b.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
 	    		in_b.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|
 	    				Intent.FLAG_ACTIVITY_MULTIPLE_TASK|
 	    				Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
 	    		in_b.putExtra("MSG_TITLE", 
 	    				mContext.getString(R.string.msgs_trust_device_screen_lock_pswd_less_than_min_length_title));

 	    		in_b.putExtra("MSG_TEXT", 
 	    				mContext.getString(R.string.msgs_trust_device_screen_lock_pswd_less_than_min_length_msg));
 	    		mContext.startActivity(in_b);
 	    		result=false;
 			}
 		}
 		return result;
    }
    
	static final private synchronized void resourceCleanup(final TaskManagerParms taskMgrParms,
    		final EnvironmentParms envParms, final CommonUtilities util) {
		mUtil.flushLog();
		final long c_time=System.currentTimeMillis();
    	if (taskMgrParms.resourceCleanupTime<=c_time) {
    		util.addDebugMsg(1, "I", "Resource cleanup started");
    		taskMgrParms.resourceCleanupTime=c_time+envParms.settingResourceCleanupIntervalTime;
			synchronized(mTaskMgrParms.normalPriorityTaskThreadPool) {
	    		int n_act=mTaskMgrParms.normalPriorityTaskThreadPool.getActiveCount();
	    		if (n_act==0) {
					TaskManager.buildNormalPriorityTaskThreadPool(mEnvParms, mTaskMgrParms, mUtil);
	    		} else {
	    			util.addDebugMsg(2,"I", "Normal priority task thread pool cleanup is bypassed.");
	    		}
			}
			synchronized(mTaskMgrParms.highPriorityTaskThreadPool) {
	    		int h_act=mTaskMgrParms.highPriorityTaskThreadPool.getActiveCount();
	    		if (h_act==0) {
					TaskManager.buildHighPriorityTaskThreadPool(mEnvParms, mTaskMgrParms, mUtil);
	    		} else {
	    			util.addDebugMsg(2,"I", "High priority task thread pool cleanup is bypassed.");
	    		}
			}
//    		System.gc();
    		util.addLogMsg("I", "Resource cleanup ended.");
    	}
    };

    @SuppressWarnings("deprecation")
	final static public int getAirplaneModeOn() {
    	int result=0;
       	result=Settings.System.getInt(mContext.getContentResolver(),
   	           Settings.System.AIRPLANE_MODE_ON, 0);
    	return result;
    }
    
    private static void initialExecuteSchedulerTask(Context c){
   		mUtil.addDebugMsg(1,"I","Scheduler initial execution was started");
		stopProximitySensorReceiver();
		startProximitySensorReceiver();

     	if (BluetoothAdapter.getDefaultAdapter()!=null) {
 			if (mEnvParms.isBluetoothConnected()) {
 				scheduleTask(BUILTIN_EVENT_BLUETOOTH_CONNECTED);
 				scanBtLeDevice(true);
 			} else if (mEnvParms.bluetoothIsActive) {
 				scheduleTask(BUILTIN_EVENT_BLUETOOTH_ON);
 			}
 		}
 		if (mEnvParms.isWifiConnected()) scheduleTask(BUILTIN_EVENT_WIFI_CONNECTED);	
 		else if (mEnvParms.wifiIsActive) scheduleTask(BUILTIN_EVENT_WIFI_ON);
    };

	@Override
    public IBinder onBind(Intent in) {
		String action="";
		if (in!=null && in.getAction()!=null) action=in.getAction();
		mUtil.addDebugMsg(1,"I","onBind entered, action=",action);
		return mSvcSchedulerClient;
    };

	@Override
	public boolean onUnbind(Intent in) {
		mUtil.addDebugMsg(1,"I","onUnBind entered, action=",in.getAction());
		return true;
	};

    @Override
    public void onDestroy() {
    	mUtil.addDebugMsg(1,"I","onDestroy enterd");
    	mUtil.addLogMsg("I",mTaskMgrParms.svcMsgs.msgs_svc_termination);
    	stopBasicEventReceiver(mContext);
    	stopProximitySensorReceiver();
        
        cancelHeartBeat(mContext);

        stopBtLeIntervalScanTimer(); 
        
    	TaskManager.cancelNotification(mTaskMgrParms);

    	TaskManager.removeNormalPriorityTaskThreadPool(mEnvParms,mTaskMgrParms,mUtil);

    	if (!mEnvParms.trustedDeviceKeyguardLockPassword.equals("")) {
    		mEnvParms.setKgEnabled(mContext);
    	}
    	
    	mUtil.resetLogReceiver();
    	
//        util=null;

		new Handler().postDelayed(new Runnable(){
			@Override
			public void run() {
				android.os.Process.killProcess(android.os.Process.myPid());
			}
			
		}, 100);

//        if (mEnvParms.settingExitClean) {
//			System.gc();
//			new Handler().postDelayed(new Runnable(){
//				@Override
//				public void run() {
//					android.os.Process.killProcess(android.os.Process.myPid());
//				}
//				
//			}, 100);
//        }
    };
    
    private static void startBtLeIntervalScanTimer() {
    	if (!mTcBtLeScanTimer.isEnabled() && mEnvParms.settingEnableScheduler && mIsBluetoothLeSupported) {
    		mTcBtLeScanTimer.setEnabled();
        	Thread th=new Thread() {
        		@Override
        		public void run() {
        	    	while(mTcBtLeScanTimer.isEnabled() && mEnvParms.settingEnableScheduler) {
        	    		synchronized(mTcBtLeScanTimer) {
        	    			try {
        	    				if (mEnvParms.screenIsOn) {
        	    					mTcBtLeScanTimer.wait(mEnvParms.settingBtLeScanIntervalTimeScreenUnlocked);
        	    				} else {
        	    					mTcBtLeScanTimer.wait(mEnvParms.settingBtLeScanIntervalTimeScreenLocked);
        	    				}
        					} catch (InterruptedException e) {
        						String ste=getSteString(e.getStackTrace());
        						mUtil.addDebugMsg(2,"I",e.getMessage()+"\n"+ste);
        					}
        	    		}
        	    		if (mTcBtLeScanTimer.isEnabled()) {
        	    			if (mIsBluetoothLeSupported) scanBtLeDevice(false);
        	    		}
        	    	}
        	    	mTcBtLeScanTimer.setDisabled();
        		}
        	};
        	th.setName("BtLeScanTimer");
        	th.start();
    	}
    };

    private static void stopBtLeIntervalScanTimer() {
    	synchronized(mTcBtLeScanTimer) {
    		mTcBtLeScanTimer.setDisabled();
    		mTcBtLeScanTimer.notifyAll();
    	}
    };
    
    @SuppressLint("NewApi")
	private static LeScanCallback mBtLeScanCallbackApi18=null;
	private static ScanCallback mScanCallbackApi21=null;
    private static String getSteString(StackTraceElement[] st) {
    	String st_msg="",sep="";
    	for (int i=0;i<st.length;i++) {
    		st_msg+=sep+" at "+st[i].getClassName()+"."+
    				st[i].getMethodName()+"("+st[i].getFileName()+
    				":"+st[i].getLineNumber()+")";
    		sep="\n";
    	}
		return st_msg;
    };
    
//	final static private String getCallerMethodName() {
//		StackTraceElement[] ste=Thread.currentThread().getStackTrace();
//		String name = ste[4].getMethodName()+", "+ste[5].getMethodName()+", "+ste[6].getMethodName();
//		return name+"()";
//	};

    @SuppressLint("NewApi")
	private static void cancelBtLeScan() {
    	if (mTcBtLeScan!=null) {
    		synchronized(mTcBtLeScan) {
    			mTcBtLeScan.setDisabled();
        		mTcBtLeScan.notify();
    		}
    	}
    };

    private static ArrayList<TrustItem>mBtLeDeviceScanCheckList=new ArrayList<TrustItem>();
    private static ThreadCtrl mTcBtLeScan=null;

    private static void scanBtLeDevice(boolean adapter_on) {
    	if (mIsBluetoothLeSupported) {
        	if (Build.VERSION.SDK_INT==18 || Build.VERSION.SDK_INT==19) scanBtLeDeviceForApi18(adapter_on);
        	else if (Build.VERSION.SDK_INT>=21) scanBtLeDeviceForApi21(adapter_on);
    		else {
    			mUtil.addDebugMsg(2,"I","scanBtLeDevice request ignored, Unsupported SDK");
    		}
    	} else {
    		mUtil.addDebugMsg(2,"I","scanBtLeDevice request ignored, Bluetooth LE not supported");
    	}
    };

    private static Runnable mScanTimerApi21=null;
    private static long mScanStartTime=0;
    
	@SuppressLint("NewApi")
	private static void scanBtLeDeviceForApi21(final boolean adapter_on) {
		if (mEnvParms.settingDebugLevel>=2){
			mUtil.addDebugMsg(2,"I","scanBtLeDeviceForApi21 entered");
//			mUtil.addDebugMsg(2,"I",getSteString(Thread.currentThread().getStackTrace()));
		}
		mBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();//bm.getAdapter();
		if (mScanCallbackApi21==null) {
    		mTcBtLeScan=new ThreadCtrl();
    		mTcBtLeScan.setDisabled();
	    	mScanCallbackApi21=new ScanCallback(){
	    		@Override
	    		public void onBatchScanResults (List<ScanResult> results) {}
	    		@Override
	    		public void onScanFailed (int errorCode) {
	    			if (mEnvParms.settingDebugLevel>=2) 
	    				mUtil.addDebugMsg(2,"W","onScanFailed code="+errorCode);
	    			cancelBtLeScan();
    				if (mBluetoothAdapter.getBluetoothLeScanner()!=null)
    					mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallbackApi21);
	    		}
	    		@Override
	    		public void onScanResult (int callbackType, ScanResult result) {
	    			BluetoothDevice device=result.getDevice();
	    			if (mEnvParms.settingDebugLevel>=3) 
	    				mUtil.addDebugMsg(3,"I","onScanResult name="+device.getName()+", addr="+device.getAddress());
	    			checkBtLeDeviceConnected(device, result.getRssi(), result.getScanRecord().getBytes());
	    		}
	        };
    	}
    	boolean le_exists=false;
    	for(TrustItem tdli:mEnvParms.trustItemList) {
    		if(tdli.trustItemType==TrustItem.TYPE_BLUETOOTH_LE_DEVICE && tdli.isEnabled()) le_exists=true;
//    		Log.v("","name="+tdli.trustItemName+", ena="+tdli.isEnabled()+", le="+le_exists);
    	}
    	if (le_exists) {
    		if (mTcBtLeScan.isEnabled()) {
    			mUtil.addDebugMsg(2,"I","scanBtLeDeviceForApi21 aborted, scan already started");
    			return;
//    			cancelBLeScan();
//    			SystemClock.sleep(100);
    		}
    		buildBtLeDeviceScanCheckList();
    		ScanSettings.Builder ssb=new ScanSettings.Builder();
    		ssb.setReportDelay(0);
    		ssb.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
    		List<ScanFilter> sf=new ArrayList<ScanFilter>();
    		if (mBluetoothAdapter.getBluetoothLeScanner()!=null && mBtLeDeviceScanCheckList.size()>0) {
    			mBluetoothAdapter.getBluetoothLeScanner().startScan(sf,ssb.build(),mScanCallbackApi21);
	    		mTcBtLeScan.setEnabled();
	    		TaskManager.callBackToActivity(mTaskMgrParms, mEnvParms, mUtil, CB_BTLE_SCAN_STARTED, null);
				mScanTimerApi21=new Runnable(){
        			@Override
    				public void run() {
        				putThreadDebugMsg(2,"I","scanBtLeDeviceForApi21 LE scan started");
        				
        				mScanStartTime=System.currentTimeMillis();
	    				int retry_cnt=0;
	    				long scan_bt=System.currentTimeMillis();
	    				int wt=mEnvParms.settingBtLeScanTimeForAndroid5;
	    				if (adapter_on) wt+=mEnvParms.settingBtLeScanTimeForAdapterOn;
	    				while(mTcBtLeScan.isEnabled()){
            				synchronized(mTcBtLeScan) {
        	    				try {
        	    					mTcBtLeScan.wait(wt);
        	    					if (!mTcBtLeScan.isEnabled())
        	    						putThreadDebugMsg(2,"I","scanBtLeDeviceForApi21 LE scan stop received");
        						} catch (InterruptedException e) {
        							e.printStackTrace();
        						}
            				}
            				wt=1000*3;
							//Check disconnect
							if (checkBtLeDeviceDisconnected(true)) {
								if (retry_cnt>=mEnvParms.settingBtLeScanRetryCouny) {
		            				if (mBluetoothAdapter.getBluetoothLeScanner()!=null)
		            					mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallbackApi21);
									checkBtLeDeviceDisconnected(false);
									mTcBtLeScan.setDisabled();
		        					if (mEnvParms.settingDebugLevel>=2) {
		        						putThreadDebugMsg(2,"I","scanBtLeDeviceForApi21 LE scan was stopped. " +
		        								"Elapsed time="+(System.currentTimeMillis()-scan_bt));
		        					}
		        					TaskManager.callBackToActivity(mTaskMgrParms, mEnvParms, mUtil, CB_BTLE_SCAN_ENDED, null);
								} else {
			    					retry_cnt++;
			    					if (mEnvParms.settingDebugLevel>=2)
			    						putThreadDebugMsg(2,"I","scanBtLeDeviceForApi21 LE was retried, retry count="+retry_cnt);
								}
							} else {
	            				if (mBluetoothAdapter.getBluetoothLeScanner()!=null)
	            					mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallbackApi21);
								mTcBtLeScan.setDisabled();
	        					if (mEnvParms.settingDebugLevel>=2) {
	        						putThreadDebugMsg(2,"I","scanBtLeDeviceForApi21 LE scan was stopped. " +
	        								"Elapsed time="+(System.currentTimeMillis()-scan_bt));
	        					}
	        					TaskManager.callBackToActivity(mTaskMgrParms, mEnvParms, mUtil, CB_BTLE_SCAN_ENDED, null);
							}
	    				}
        			}
				};
    			TaskManager.executeTaskByNormalPriority(mEnvParms, mTaskMgrParms, mUtil, mScanTimerApi21);
    		} else {
    			if (mBtLeDeviceScanCheckList.size()>0) mUtil.addDebugMsg(2,"I","scanBtLeDeviceForApi21 aborted, LE scanner not available");
    			else mUtil.addDebugMsg(2,"I","scanBtLeDeviceForApi21 aborted, Advtising LE device does not exist");
    		}
    	} else {
    		mUtil.addDebugMsg(2,"I","scanBtLeDeviceForApi21 aborted, LE device does not exists");
    	}
    };

    private static void checkBtLeDeviceConnected(BluetoothDevice device, int rssi, byte[] scan_record) {
		final String name=device.getName()==null?UNKNOWN_LE_DEVICE_NAME:device.getName();
		final String addr=device.getAddress()==null?UNKNOWN_LE_DEVICE_ADDR:device.getAddress();;

		TrustItem check_list_item=mEnvParms.getBluetoothConnectedDevice(mBtLeDeviceScanCheckList, 
				TrustItem.TYPE_BLUETOOTH_LE_DEVICE, name, addr);
//		Log.v("","temp_tdli="+temp_tdli);

		if (check_list_item!=null) {
			ScanRecordCompat sr=ScanRecordCompat.parseFromBytes(scan_record);
			List<ParcelUuid> sul=sr.getServiceUuids();
			boolean immed_alert=false;
			if (sul!=null) {
				for(ParcelUuid pu:sul) {
					if (pu.getUuid().equals(BtLeUtil.UUID_SVC_ALERT)) immed_alert=true;
//					Log.v("","uu="+pu.getUuid().toString());
				}
			}
			if (mEnvParms.settingBtLeMinRssi==0 || rssi>=mEnvParms.settingBtLeMinRssi) {
				check_list_item.setConnected(true);
				check_list_item.hasImmedAlert=immed_alert;
				check_list_item.searchFindCount++;
    			TrustItem conn_list_item=mEnvParms.getBluetoothConnectedDevice(TrustItem.TYPE_BLUETOOTH_LE_DEVICE, name,addr);
    			if (conn_list_item==null){
    				mUtil.addDebugMsg(1,"I","checkBtLeDeviceFound device connected, dev=",name+", addr="+addr+
    						", ImmedAlert="+immed_alert+", connect mode="+check_list_item.isBtLeDeviceConnectMode);
    				if (check_list_item.isBtLeDeviceConnectMode) {
    					mEnvParms.addBluetoothConnectedDevice(mContext, 
    							TrustItem.TYPE_BLUETOOTH_LE_DEVICE, name, addr, immed_alert, true);
    					final TrustItem c_list=
    							mEnvParms.getBluetoothConnectedDevice(TrustItem.TYPE_BLUETOOTH_LE_DEVICE, name, addr);
    					c_list.bleDeviceLinkLossActionToHost=check_list_item.bleDeviceLinkLossActionToHost;
    					c_list.bleDeviceLinkLossActionToTag=check_list_item.bleDeviceLinkLossActionToTag;
    					c_list.bleDeviceNotifyButtonAction=check_list_item.bleDeviceNotifyButtonAction;

    					NotifyEvent ntfy=new NotifyEvent(mContext);
    					ntfy.setListener(new NotifyEventListener(){
							@Override
							public void positiveResponse(Context c, Object[] o) {
							}
							@Override
							public void negativeResponse(Context c, Object[] o) {
								c_list.bluetoothLeGatt=null;
								final String name=(String) (o[0]==null?UNKNOWN_LE_DEVICE_NAME:o[0]);
								final String addr=(String) (o[1]==null?UNKNOWN_LE_DEVICE_ADDR:o[1]);
								mEnvParms.removeBluetoothConnectedDevice(mContext, 
										TrustItem.TYPE_BLUETOOTH_LE_DEVICE, name, addr);
								TrustItem t_list=mEnvParms.getTrustItem(TrustItem.TYPE_BLUETOOTH_LE_DEVICE, name,addr);
								if (t_list!=null && t_list.isEnabled()) {
									t_list.bleDeviceErrorMsg=c_list.bleDeviceErrorMsg;
								}
							}
    					});
    					setBtLeDeviceConnListener(c_list);
    					c_list.bluetoothLeGatt=
    							BtLeUtil.connectBtLeDevice(mContext, mUtil, ntfy, c_list);
    				} else {
    					mEnvParms.addBluetoothConnectedDevice(mContext, 
    							TrustItem.TYPE_BLUETOOTH_LE_DEVICE, name, addr, immed_alert, false);
    					mEnvParms.bluetoothLastEventDeviceName=name;
    					mEnvParms.bluetoothLastEventDeviceAddr=addr;
    					mEnvParms.bluetoothLastEventDeviceType=TrustItem.TYPE_BLUETOOTH_LE_DEVICE;
    					scheduleTask(BUILTIN_EVENT_BLUETOOTH_CONNECTED);
    				}
    			}
    			if (check_list_item.searchFindCount>=2) {
    				if ((System.currentTimeMillis()-mScanStartTime)>mEnvParms.settingBtLeScanMinimumTime) 
    					cancelBtLeScan();
    			}
			} else {
				mUtil.addDebugMsg(1,"I","checkBtLeDeviceFound ignored, RSSI is less than criteria. RSSI="+rssi);
			}
//		} else {
//			TrustedListItem n_tdli=new TrustedListItem();
//			n_tdli.trustedDeviceName=name;
//			n_tdli.trustedDeviceAddr=addr;
//			n_tdli.trustedItemType=TrustedListItem.TYPE_BLUETOOTH_LE_DEVICE;
//			n_tdli.hasImmedAlert=immed_alert;
//			n_tdli.setEnabled(false);
//			mBtLeDeviceScanCheckList.add(n_tdli);
		}
    };
    
    private static void setBtLeDeviceConnListener(final TrustItem c_list) {
		c_list.notifyGattCallback=new NotifyEvent(mContext);
		c_list.notifyGattCallback.setListener(new NotifyEventListener(){
			@SuppressLint("NewApi")
			@Override
			public void positiveResponse(Context c, Object[] o) {
				String type=(String)o[0];
				final BluetoothGatt gatt=(BluetoothGatt)o[1];
				NotifyEvent ntfy_linkloss=new NotifyEvent(mContext);
				ntfy_linkloss.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						BluetoothGattCharacteristic bc=null;
					    bc=BtLeUtil.characteristic(mUtil, gatt, 
					    		BtLeUtil.UUID_LINK_LOSS, BtLeUtil.UUID_CHAR_ALERT_LEVEL);
					    if (bc!=null) {
						    byte level =(byte) ((byte)c_list.bleDeviceLinkLossActionToTag & 0xFF);
						    bc.setValue(new byte[] { level });
						    gatt.writeCharacteristic(bc);
					    }
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {}
				});
				if (type.equals(TrustItem.NOTIFY_GATT_CALLBACK_TYPE_CONNECTION_STATE_CHANGED)) {
					
				} else if (type.equals(TrustItem.NOTIFY_GATT_CALLBACK_TYPE_SERVICE_DISCOVERED)) {
				    BluetoothGattCharacteristic bc=null;
				    bc=BtLeUtil.characteristic(mUtil, gatt, 
				    				BtLeUtil.UUID_SVC_BATTERY, BtLeUtil.UUID_CHAR_BATTERY_LEVEL);
				    if (bc!=null) gatt.readCharacteristic(bc);
					if (c_list.bleDeviceNotifyButtonAction!=TrustItem.BLE_DEVICE_ACTION_NONE) {
					    bc=BtLeUtil.characteristic(mUtil, gatt, 
			    				BtLeUtil.UUID_SVC_BATTERY, BtLeUtil.UUID_CHAR_BATTERY_POWER_STATE);
					    gatt.setCharacteristicNotification(bc,true);
					}
				} else if (type.equals(TrustItem.NOTIFY_GATT_CALLBACK_TYPE_CHAR_CHANGED)) {
					BluetoothGattCharacteristic bc=(BluetoothGattCharacteristic)o[2];
					if (bc.getUuid().equals(BtLeUtil.UUID_CHAR_BATTERY_POWER_STATE)) {
						int value=bc.getValue()[0];
						if (value==1) {
//							mUtil.addDebugMsg(2, "I", "Bluetooth LE device Notify="+value);
							if (c_list.bleDeviceNotifyButtonAction==TrustItem.BLE_DEVICE_ACTION_VIBRATION) {
								vibrateByPattern("3");
							} else if (c_list.bleDeviceNotifyButtonAction==TrustItem.BLE_DEVICE_ACTION_ALARM) {
								if (mTcPlayBackAlarm!=null) {
									if (mTcPlayBackAlarm.isEnabled()) cancelPlayBackDefaultAlarm();
									else playBackDefaultAlarm(30);
								} else {
									playBackDefaultAlarm(30);
								}
							}
						}
					}
				} else if (type.equals(TrustItem.NOTIFY_GATT_CALLBACK_TYPE_CHAR_READ)) {
					BluetoothGattCharacteristic bc=(BluetoothGattCharacteristic)o[2];
					if (bc.getUuid().equals(BtLeUtil.UUID_CHAR_BATTERY_LEVEL)) {
						if (bc.getValue()!=null) c_list.bleDeviceBatteryLevel=bc.getValue()[0];
						mUtil.addDebugMsg(2, "I", "Bluetooth LE device Battery level="+c_list.bleDeviceBatteryLevel);
						TrustItem t_list=mEnvParms.getTrustItem(c_list.trustItemType, 
								c_list.trustDeviceName, c_list.trustDeviceAddr);
						if (t_list!=null) {
							t_list.bleDeviceBatteryLevel=bc.getValue()[0];
							TaskManager.callBackToActivity(mTaskMgrParms, mEnvParms, mUtil, CB_NETWORK_CHANGED, null);
						}
					}
					ntfy_linkloss.notifyToListener(true, null);
				} else if (type.equals(TrustItem.NOTIFY_GATT_CALLBACK_TYPE_CHAR_WRITE)) {
//					BluetoothGattCharacteristic bc=(BluetoothGattCharacteristic)o[2];
				}
			}
			@Override
			public void negativeResponse(Context c,Object[] o) {}
		});

    }
    
    private static boolean checkBtLeDeviceDisconnected(boolean check_only) {
    	boolean result=false;
		if (mBtLeDeviceScanCheckList.size()>0) {
			for(TrustItem check_list_item:mBtLeDeviceScanCheckList) {
				if (check_list_item.isEnabled() && !check_list_item.isConnected() && 
						!check_list_item.isBtLeDeviceConnectMode) {
					TrustItem c_tdli=mEnvParms.getBluetoothConnectedDevice(
							TrustItem.TYPE_BLUETOOTH_LE_DEVICE, check_list_item.trustDeviceName,
							check_list_item.trustDeviceAddr);
					if (c_tdli!=null) {
						if (!check_only) {
							//Disconnected
							mEnvParms.removeBluetoothConnectedDevice(mContext,  
									check_list_item.trustItemType, check_list_item.trustDeviceName, check_list_item.trustDeviceAddr);
							putThreadDebugMsg(1,"I","checkBtLeDeviceNotfound device disconnected, dev="+check_list_item.trustDeviceName+", addr="+check_list_item.trustDeviceAddr);
							mEnvParms.bluetoothLastEventDeviceName=check_list_item.trustDeviceName;
							mEnvParms.bluetoothLastEventDeviceAddr=check_list_item.trustDeviceAddr;
							mEnvParms.bluetoothLastEventDeviceType=check_list_item.trustItemType;
							scheduleTask(BUILTIN_EVENT_BLUETOOTH_DISCONNECTED);
							result=true;
						} else {
							result=true;
							break;
						}
					}
				}
			}
		}
		return result;
    };
    
    private static void buildBtLeDeviceScanCheckList(){
		mBtLeDeviceScanCheckList.clear();
		for(TrustItem tdli:mEnvParms.trustItemList) {
			if (tdli.trustItemType==TrustItem.TYPE_BLUETOOTH_LE_DEVICE && tdli.isEnabled()) {
				if (tdli.isBtLeDeviceConnectMode) {
					TrustItem c_tli=mEnvParms.getBluetoothConnectedDevice(TrustItem.TYPE_BLUETOOTH_LE_DEVICE,
							tdli.trustDeviceName, tdli.trustDeviceAddr);
					if (c_tli==null) {
						TrustItem n_tdli=new TrustItem();
						n_tdli.trustDeviceName=tdli.trustDeviceName;
						n_tdli.trustDeviceAddr=tdli.trustDeviceAddr;
						n_tdli.trustItemType=tdli.trustItemType;
						n_tdli.isBtLeDeviceConnectMode=tdli.isBtLeDeviceConnectMode;
						n_tdli.bleDeviceLinkLossActionToHost=tdli.bleDeviceLinkLossActionToHost;
						n_tdli.bleDeviceLinkLossActionToTag=tdli.bleDeviceLinkLossActionToTag;
						n_tdli.bleDeviceNotifyButtonAction=tdli.bleDeviceNotifyButtonAction;
						n_tdli.setEnabled(true);
						mBtLeDeviceScanCheckList.add(n_tdli);
						mUtil.addDebugMsg(3,"I","Scan list added, device="+tdli.trustDeviceName+", addr="+tdli.trustDeviceAddr);
					}
				} else {
					TrustItem n_tdli=new TrustItem();
					n_tdli.trustDeviceName=tdli.trustDeviceName;
					n_tdli.trustDeviceAddr=tdli.trustDeviceAddr;
					n_tdli.trustItemType=tdli.trustItemType;
					n_tdli.isBtLeDeviceConnectMode=tdli.isBtLeDeviceConnectMode;
					n_tdli.setEnabled(true);
					mBtLeDeviceScanCheckList.add(n_tdli);
					mUtil.addDebugMsg(3,"I","Scan list added, device="+tdli.trustDeviceName+", addr="+tdli.trustDeviceAddr);
				}
			}
		}
    };
    
    private static Runnable mScanTimerApi18=null;
    private static BluetoothAdapter mBluetoothAdapter=null;
    @SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	private static void scanBtLeDeviceForApi18(final boolean adapter_on) {
		if (mEnvParms.settingDebugLevel>=2){
			mUtil.addDebugMsg(2,"I","scanBtLeDeviceForApi18 entered");
//			mUtil.addDebugMsg(2,"I",getSteString(Thread.currentThread().getStackTrace()));
		}
		mBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();//bm.getAdapter();
    	if (mBtLeScanCallbackApi18==null) {
    		mTcBtLeScan=new ThreadCtrl();
    		mTcBtLeScan.setDisabled();
	    	mBtLeScanCallbackApi18=new LeScanCallback(){
	    		@Override
	    		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
	    			if (mEnvParms.settingDebugLevel>=3) 
	    				mUtil.addDebugMsg(3,"I","onLeScan name="+device.getName()+", addr="+device.getAddress());
	    			checkBtLeDeviceConnected(device,rssi,scanRecord);
	    		}
	        };
    	}
//    	cancelLeScan(); 
		if (mTcBtLeScan.isEnabled()) {
			mUtil.addDebugMsg(2,"I","scanBtLeDeviceForApi18 aborted, scan already started");
    		return;
		}
		buildBtLeDeviceScanCheckList();
    	if (mBtLeDeviceScanCheckList.size()>0) {
			boolean scan_result=mBluetoothAdapter.startLeScan(mBtLeScanCallbackApi18);
			if (scan_result) {
	    		mTcBtLeScan.setEnabled();
	    		TaskManager.callBackToActivity(mTaskMgrParms, mEnvParms, mUtil, CB_BTLE_SCAN_STARTED, null);
				mScanTimerApi18=new Runnable() {
	    			@Override
					public void run() {
	    				putThreadDebugMsg(2,"I","scanBtLeDeviceForApi18 LE scan started");
	    				
	    				mScanStartTime=System.currentTimeMillis();
	    				int retry_cnt=0;
	    				long scan_bt=System.currentTimeMillis();
	    				int wt=mEnvParms.settingBtLeScanTimeForAndroid4;
	    				if (adapter_on) wt+=mEnvParms.settingBtLeScanTimeForAdapterOn;
	    				while(mTcBtLeScan.isEnabled()){
		    				synchronized(mTcBtLeScan) {
			    				try {
			    					mTcBtLeScan.wait(wt);
			    					if (!mTcBtLeScan.isEnabled())
			    						putThreadDebugMsg(2,"I","scanBtLeDeviceForApi18 LE scan stop received");
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
            				}
		    				wt=1000*3;
							//Check disconnect
							if (checkBtLeDeviceDisconnected(true)) {
								if (retry_cnt>=mEnvParms.settingBtLeScanRetryCouny) {
									mBluetoothAdapter.stopLeScan(mBtLeScanCallbackApi18);
									checkBtLeDeviceDisconnected(false);
									mTcBtLeScan.setDisabled();
		        					if (mEnvParms.settingDebugLevel>=2) {
		        						putThreadDebugMsg(2,"I","scanBtLeDeviceForApi18 LE scan was stopped. " +
		        								"Elapsed time="+(System.currentTimeMillis()-scan_bt));
		        					}
		        					TaskManager.callBackToActivity(mTaskMgrParms, mEnvParms, mUtil, CB_BTLE_SCAN_ENDED, null);
								} else {
			    					retry_cnt++;
			    					if (mEnvParms.settingDebugLevel>=2)
			    						putThreadDebugMsg(2,"I","scanBtLeDeviceForApi18 LE scan was retried, retry count="+retry_cnt);
								}
							} else {
								mBluetoothAdapter.stopLeScan(mBtLeScanCallbackApi18);
								mTcBtLeScan.setDisabled();
	        					if (mEnvParms.settingDebugLevel>=2) {
	        						putThreadDebugMsg(2,"I","scanBtLeDeviceForApi18 LE scan was stopped. " +
	        								"Elapsed time="+(System.currentTimeMillis()-scan_bt));
	        					}
	        					TaskManager.callBackToActivity(mTaskMgrParms, mEnvParms, mUtil, CB_BTLE_SCAN_ENDED, null);
							}
	    				}
	    			}
				};
	    		TaskManager.executeTaskByNormalPriority(mEnvParms, mTaskMgrParms, mUtil, mScanTimerApi18);
			} else {
				mUtil.addDebugMsg(2,"I","scanBtLeDeviceForApi18 aborted, startLeScan failed");
				mBluetoothAdapter.stopLeScan(mBtLeScanCallbackApi18);
				cancelBtLeScan();
			}
    	} else {
    		mUtil.addDebugMsg(2,"I","scanBtLeDeviceForApi18 aborted, Advtising LE device does not exists");
    	}
    };

    final static private ISchedulerClient.Stub mSvcSchedulerClient = 
			new ISchedulerClient.Stub() {
		final public void setCallBack(final ISchedulerCallback callback)
				throws RemoteException {
			mUtil.addDebugMsg(2,"I","setCallBack entered");
			mTaskMgrParms.callBackList.register(callback);
		};
		
		final public void removeCallBack(ISchedulerCallback callback)
				throws RemoteException {
			mUtil.addDebugMsg(2,"I","removeCallBack entered");
			mTaskMgrParms.callBackList.unregister(callback);
		};
		
		public byte[] getBluetoothConnectedDeviceList() throws RemoteException {
			int no=mEnvParms.getBluetoothConnectedDeviceList().size();
			if (no==0) return null;
			else {
				ByteArrayOutputStream bos = new ByteArrayOutputStream(1024*100); 
				byte[] buf=null; 
			    try { 
			    	ObjectOutput out = new ObjectOutputStream(bos);
			    	SerializeUtil.writeArrayList(out, mEnvParms.getBluetoothConnectedDeviceList());
//			    	out.writeObject(prof_list);
				    out.flush();
				    out.close();;
				    buf= bos.toByteArray(); 
			    } catch(IOException e) { 
			    	mUtil.addDebugMsg(2,"I","serialize bluetooth connected device list error"); 
					String ste=getSteString(e.getStackTrace());
					mUtil.addDebugMsg(2,"I",e.getMessage()+"\n"+ste);
				}
			    return buf;
			}
		};

		public byte[] getTrustItemList() throws RemoteException {
			int no=mEnvParms.trustItemList.size();
			if (no==0) return null;
			else {
				ByteArrayOutputStream bos = new ByteArrayOutputStream(1024*100); 
				byte[] buf=null; 
			    try { 
			    	ObjectOutput out = new ObjectOutputStream(bos);
//			    	SerializeUtil.writeArrayList(out, mEnvParms.trustItemList);
			    	CommonUtilities.serilizeTrustItemList(out,mEnvParms.trustItemList);
//			    	out.writeObject(prof_list);
				    out.flush();
				    out.close();;
				    buf= bos.toByteArray(); 
			    } catch(IOException e) { 
			    	mUtil.addDebugMsg(2,"I","serialize trust item list error"); 
					String ste=getSteString(e.getStackTrace());
					mUtil.addDebugMsg(2,"I",e.getMessage()+"\n"+ste);
				}
			    return buf;
			}
		};

		final public void reScanBtLeDevice() {
			scanBtLeDevice(false);
		};
		

    };

	final private void restartScheduler() {
		mSvcInstance.stopForeground(true);
//		TaskManager.cancelNotification(mTaskMgrParms);
		Handler restartHandler=new Handler();
		restartHandler.postDelayed(new Runnable(){
			@Override
			public void run() {
				mUtil.startScheduler();
				System.gc();
				android.os.Process.killProcess(android.os.Process.myPid());
			}
		}, 100);
	};

//	final static private boolean lockScreen() {
//		boolean result=false;
//        DevicePolicyManager dpm = 
//        		(DevicePolicyManager)mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
//        ComponentName darcn = new ComponentName(mContext, DevAdmReceiver.class);
//        if (dpm.isAdminActive(darcn)) {
//        	dpm.lockNow();
//        	result=true;
//        } else result=false;
//        return result;
//	};

	@SuppressLint("InlinedApi")
	final static private void startBasicEventReceiver(Context c) {
		if (mEnvParms.settingDebugLevel>=1) mUtil.addDebugMsg(1,"I","startBasicEventReceiver entered");
		
		mBatteryStatusReceiver=new BatteryStatusReceiver();
		mWifiReceiver=new WifiReceiver();
		mSleepReceiver=new SleepReceiver();
		mBluetoothReceiver=new BluetoothReceiver();
		mMiscellaneousReceiver=new MiscellaneousReceiver();
		mReceiverProximity=new ProximitySensorReceiver();

  		IntentFilter intent = new IntentFilter();

  		intent.addAction(Intent.ACTION_BATTERY_CHANGED);
  		c.registerReceiver(mBatteryStatusReceiver, intent);
  		
  		intent = new IntentFilter();
  		intent.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
  		intent.addAction(WifiManager.RSSI_CHANGED_ACTION);
  		intent.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
		intent.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
		intent.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		intent.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
		
		if (Build.VERSION.SDK_INT>=14) {
//			intent.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
//			intent.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
//			intent.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
//			intent.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
//			intent.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
		}
		c.registerReceiver(mWifiReceiver, intent);
        
        intent = new IntentFilter();
        
        intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intent.addAction(BluetoothDevice.ACTION_FOUND);
        intent.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        c.registerReceiver(mBluetoothReceiver, intent);
        
        intent = new IntentFilter();
        intent.addAction(Intent.ACTION_SCREEN_OFF);
        intent.addAction(Intent.ACTION_SCREEN_ON);
        intent.addAction(Intent.ACTION_USER_PRESENT);
        c.registerReceiver(mSleepReceiver, intent);
        
        IntentFilter i_flt = new IntentFilter();
        i_flt.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
        i_flt.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        i_flt.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        c.registerReceiver(mMiscellaneousReceiver, i_flt);
        
        startPhoneStateListener();
        
    };

    final static private void stopBasicEventReceiver(Context c) {
    	if (mBatteryStatusReceiver!=null) {
        	c.unregisterReceiver(mBatteryStatusReceiver);
    		mBatteryStatusReceiver=null;
    	}
    	if (mWifiReceiver!=null) {
        	c.unregisterReceiver(mWifiReceiver);
        	mWifiReceiver=null;
    	}
    	if (mBluetoothReceiver!=null) {
        	c.unregisterReceiver(mBluetoothReceiver);
        	mBluetoothReceiver=null;
    	}
    	if (mSleepReceiver!=null) {
        	c.unregisterReceiver(mSleepReceiver);
        	mSleepReceiver=null;
    	}
    	if (mMiscellaneousReceiver!=null) {
        	c.unregisterReceiver(mMiscellaneousReceiver);
        	mMiscellaneousReceiver=null;
    	}
    };
    
    final static private void waitBluetoothDeviceConnectedIfRequired() {
		if (mEnvParms.bluetoothOffConnectionTimeout.equals(BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_ALWAYS)) {
			waitUntilBluetoothConnected();
		} else {
			if (mEnvParms.batteryChargeStatusInt==mEnvParms.BATTERY_CHARGE_STATUS_INT_DISCHARGING && 
					mEnvParms.bluetoothOffConnectionTimeout.equals(BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_BATTERY)) {
				waitUntilBluetoothConnected();
			}
		}
    };

    final static private void waitWifiDeviceConnectIfRequired() {
		if (mEnvParms.wifiOffConnectionTimeout.equals(WIFI_OFF_WHEN_CONNECT_TIMEOUT_ALWAYS)) {
			waitUntilWifiConnected();
		} else {
			if (mEnvParms.batteryChargeStatusInt==mEnvParms.BATTERY_CHARGE_STATUS_INT_DISCHARGING &&
					mEnvParms.wifiOffConnectionTimeout.equals(WIFI_OFF_WHEN_CONNECT_TIMEOUT_BATTERY)) {
				waitUntilWifiConnected();
			}
		}
    };

    static private void processBluetoothOn(String event) {
		cancelBtLeScan();
		scanBtLeDevice(true);
//		mUiHandler.postDelayed(new Runnable(){
//			@Override
//			public void run() {
//				scanBtLeDevice(true);
//			}
//		}, 1000*3);
    	cancelTaskBluetoothWait();
    	isTrustedBluetoothDeviceConnected(event);
    	resetKeyGuard(true);
    	waitBluetoothDeviceConnectedIfRequired();
		TaskManager.callBackToActivity(mTaskMgrParms, mEnvParms, mUtil, CB_NETWORK_CHANGED, null);
    };
    
    static private void processWifiOn(String event) {
    	cancelTaskWifiWait();
    	isTrustedWifiAccessPointConnected(event);
    	resetKeyGuard(true);
    	waitWifiDeviceConnectIfRequired();
		TaskManager.callBackToActivity(mTaskMgrParms, mEnvParms, mUtil, CB_NETWORK_CHANGED, null);
    };
    
    static private void processBluetoothOff(String event) {
    	cancelBtLeScan();
		cancelTaskBluetoothWait();
		isTrustedBluetoothDeviceConnected(event);
		resetKeyGuard(true);
		TaskManager.callBackToActivity(mTaskMgrParms, mEnvParms, mUtil, CB_NETWORK_CHANGED, null);
    };
    
    static private void processWifiOff(String event) {
		cancelTaskWifiWait();
		isTrustedWifiAccessPointConnected(event);
		resetKeyGuard(true);
		TaskManager.callBackToActivity(mTaskMgrParms, mEnvParms, mUtil, CB_NETWORK_CHANGED, null);
    };

    static private void processBluetoothDisconnected(String event) {
    	if (mEnvParms.bluetoothLastEventDeviceType==TrustItem.TYPE_BLUETOOTH_LE_DEVICE) {
        	TrustItem tli=mEnvParms.getTrustItem(mEnvParms.bluetoothLastEventDeviceType, 
        			mEnvParms.bluetoothLastEventDeviceName, mEnvParms.bluetoothLastEventDeviceAddr);
        	if (tli!=null && !mIgnoreDeviceDiscoonectedHostAction && tli.isBtLeDeviceConnectMode) {
//        		Log.v("","name="+mEnvParms.bluetoothLastEventDeviceName+", n="+tli.bleDeviceLinkLossActionToHost);
        		if (tli.bleDeviceLinkLossActionToHost!=TrustItem.BLE_DEVICE_ACTION_NONE) {
        			if (tli.bleDeviceLinkLossActionToHost==TrustItem.BLE_DEVICE_ACTION_VIBRATION) vibrateDefaultPattern();
        			else if (tli.bleDeviceLinkLossActionToHost==TrustItem.BLE_DEVICE_ACTION_ALARM) playBackDefaultAlarm(30);
        		}
        	}
    	}
    	int act_cnt=0;
    	for(TrustItem tdli:mEnvParms.getBluetoothConnectedDeviceList()) {
    		if (tdli.isConnected() && tdli.isEnabled()) act_cnt++;
    	}
    	if (isDeviceTrusted(mEnvParms.bluetoothLastEventDeviceType, mEnvParms.bluetoothLastEventDeviceName, mEnvParms.bluetoothLastEventDeviceAddr)
    			&& act_cnt==0) {
    		cancelTaskBluetoothReconnectWait();
    		cancelTaskBluetoothWait();
        	waitUntilBluetoothDeviceReconnect(event);
        	waitBluetoothDeviceConnectedIfRequired();
    	} else {
    		cancelTaskBluetoothWait();
    		waitBluetoothDeviceConnectedIfRequired();
    	}
    	TaskManager.callBackToActivity(mTaskMgrParms, mEnvParms, mUtil, CB_NETWORK_CHANGED, null);
    };

    static private void processWifiDisconnected(String event) {
    	if (isDeviceTrusted(TrustItem.TYPE_WIFI_AP, mEnvParms.wifiSsidName, mEnvParms.wifiSsidAddr)) {
    		cancelTaskWifiReconnectWait();
    		cancelTaskWifiWait();
        	waitUntilWifiAccessPointReconnect(event);
        	waitWifiDeviceConnectIfRequired();
    	} else {
    		cancelTaskWifiWait();
    		waitWifiDeviceConnectIfRequired();
    	}
    	TaskManager.callBackToActivity(mTaskMgrParms, mEnvParms, mUtil, CB_NETWORK_CHANGED, null);
    };

    static private void processBluetoothConnected(String event) {
    	if (isDeviceTrusted(mEnvParms.bluetoothLastEventDeviceType, mEnvParms.bluetoothLastEventDeviceName, mEnvParms.bluetoothLastEventDeviceAddr)) {
        	cancelTaskBluetoothReconnectWait();
    		cancelTaskBluetoothWait();
        	isTrustedBluetoothDeviceConnected(event);
        	resetKeyGuard(true);
    	} else {
    		cancelTaskBluetoothWait();
    	}
    	TaskManager.callBackToActivity(mTaskMgrParms, mEnvParms, mUtil, CB_NETWORK_CHANGED, null);
    };

    static private void processWifiConnected(String event) {
    	if (isDeviceTrusted(TrustItem.TYPE_WIFI_AP, mEnvParms.wifiSsidName, mEnvParms.wifiSsidAddr)) {
    		cancelTaskWifiReconnectWait();
    		cancelTaskWifiWait();
    		isTrustedWifiAccessPointConnected(event);
    		resetKeyGuard(true);
    	} else {
    		cancelTaskWifiWait();
    	}
    	TaskManager.callBackToActivity(mTaskMgrParms, mEnvParms, mUtil, CB_NETWORK_CHANGED, null);
    };

    static private void processPhoneStateIdle(String event) {
    	
    };

    static private void processPhoneStateRinging(String event) {
		cancelTaskProximityDetected();
		cancelTaskProximityUndetected();
    };

    static private void processPhoneStateOffHook(String event) {
		cancelTaskProximityDetected();
		cancelTaskProximityUndetected();
    };
    
    static private void processProximityDetected(String event) {
		checkProximityDetected();
    };

    static private void processProximityUndetected(String event) {
		checkProximityUndetected();
    };

    static private void processScreenLocked(String event) {
    	cancelPlayBackDefaultAlarm();
    	cancelVibrateDefaultPattern();
		cancelTaskProximityDetected();
		cancelTaskProximityUndetected();
		checkScreenLocked();
		notifyToUser(event);
    };

    static private void processScreenUnlocked(String event) {
    	cancelPlayBackDefaultAlarm();
		checkScreenUnlocked();
		notifyToUser(event);
    };

    static private void processScreenOff(String event) {
    	cancelPlayBackDefaultAlarm();
		cancelTaskProximityDetected();
		cancelTaskProximityUndetected();
		notifyToUser(event);
    };

    static private void processPowerSourceChangedToAc(String event) {
		waitBluetoothDeviceConnectedIfRequired();
		waitWifiDeviceConnectIfRequired();
    };

    static private void processPowerSourceChangedToBattery(String event) {
		waitBluetoothDeviceConnectedIfRequired();
		waitWifiDeviceConnectIfRequired();
    };

    final static private void scheduleTask(final String event) {
    	if (mEnvParms.settingDebugLevel>=1) mUtil.addDebugMsg(1,"I","scheduleTask entered",", Event="+event);
    	if (mEnvParms.settingEnableScheduler) {
        	if (event.equals(BUILTIN_EVENT_BLUETOOTH_ON)) {
        		processBluetoothOn(event);
        	} else if (event.equals(BUILTIN_EVENT_BLUETOOTH_OFF)) {
        		processBluetoothOff(event);
        	} else if (event.equals(BUILTIN_EVENT_BLUETOOTH_CONNECTED)) {
        		processBluetoothConnected(event);
        	} else if (event.equals(BUILTIN_EVENT_BLUETOOTH_DISCONNECTED)) {
        		processBluetoothDisconnected(event);
        	} else if (event.equals(BUILTIN_EVENT_WIFI_ON)) {
        		processWifiOn(event);
        	} else if (event.equals(BUILTIN_EVENT_WIFI_OFF)) {
        		processWifiOff(event);
        	} else if (event.equals(BUILTIN_EVENT_WIFI_CONNECTED)) {
        		processWifiConnected(event);
        	} else if (event.equals(BUILTIN_EVENT_WIFI_DISCONNECTED)) {
        		processWifiDisconnected(event);
        	} else if (event.equals(BUILTIN_EVENT_PHONE_CALL_STATE_IDLE)) {
        		processPhoneStateIdle(event);
        	} else if (event.equals(BUILTIN_EVENT_PHONE_CALL_STATE_RINGING)) {
        		processPhoneStateRinging(event);
        	} else if (event.equals(BUILTIN_EVENT_PHONE_CALL_STATE_OFF_HOOK)) {
        		processPhoneStateOffHook(event);
        	} else if (event.equals(BUILTIN_EVENT_PROXIMITY_DETECTED)) {
        		processProximityDetected(event);
        	} else if (event.equals(BUILTIN_EVENT_PROXIMITY_UNDETECTED)) {
        		processProximityUndetected(event);
        	} else if (event.equals(BUILTIN_EVENT_SCREEN_UNLOCKED)) {
        		processScreenUnlocked(event);
        	} else if (event.equals(BUILTIN_EVENT_SCREEN_LOCKED)) {
        		processScreenLocked(event);
        	} else if (event.equals(BUILTIN_EVENT_SCREEN_OFF)) {
        		processScreenOff(event);
        	} else if (event.equals(BUILTIN_EVENT_POWER_SOURCE_CHANGED_AC)) {
        		processPowerSourceChangedToAc(event);
        	} else if (event.equals(BUILTIN_EVENT_POWER_SOURCE_CHANGED_BATTERY)) {
        		processPowerSourceChangedToBattery(event);
        	}
    	} else {
    		mUtil.addDebugMsg(1,"I","scheduleTask ignored, because scheduler is disabled");
    	}
    };
    
    final private static void notifyToUser(String event) {
    	if (!mEnvParms.isTelephonyCallStateIdle()) return;
    	if ((event.equals(BUILTIN_EVENT_SCREEN_LOCKED) || event.equals(BUILTIN_EVENT_SCREEN_OFF))) {
    		if (mEnvParms.notifyScreenLockedNotification) playBackNotification(1);
    		if (mEnvParms.notifyScreenLockedVibrate) vibrateByPattern(mEnvParms.notifyScreenLockedVibratePattern);
    	} else if ((event.equals(BUILTIN_EVENT_SCREEN_UNLOCKED))) {
    		if (mEnvParms.notifyScreenUnlockedNotification) playBackNotification(2);
    		if (mEnvParms.notifyScreenUnlockedVibrate) vibrateByPattern(mEnvParms.notifyScreenUnlockedVibratePattern);
    	}

    };
    
    final private static void vibrateByPattern(final String pattern) {
    	Runnable th=new Runnable(){
    		@Override
    		public void run(){
    	    	Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
    	    	if (pattern.equals("1")) vibrator.vibrate(new long[]{0,200},-1);
    	    	else if (pattern.equals("2")) vibrator.vibrate(new long[]{0,150,40,150},-1);
    	    	else vibrator.vibrate(new long[]{0,150,40,150,40,150},-1);
    		}
    	};
    	TaskManager.executeTaskByNormalPriority(mEnvParms, mTaskMgrParms, mUtil, th);
    };
    
    final private static void cancelVibrateDefaultPattern() {
    	if (mTcVibrateDefaultPattern!=null) {
    		synchronized(mTcVibrateDefaultPattern) {
    			mTcVibrateDefaultPattern.setDisabled();
    			mTcVibrateDefaultPattern.notify(); 
    		}
    	}
    };
    
    private static ThreadCtrl mTcVibrateDefaultPattern=null;
    final private static void vibrateDefaultPattern() {
    	if (mTcVibrateDefaultPattern==null) mTcVibrateDefaultPattern=new ThreadCtrl();
    	cancelVibrateDefaultPattern();
    	Runnable th=new Runnable(){
    		@Override
    		public void run(){
    	    	Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
    	    	vibrator.vibrate(new long[]{0,200,500},1);
    	    	synchronized(mTcVibrateDefaultPattern) {
    	    		try {
						mTcVibrateDefaultPattern.wait(10*1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
    	    		vibrator.cancel();
    	    	}
    		}
    	};
    	TaskManager.executeTaskByNormalPriority(mEnvParms, mTaskMgrParms, mUtil, th);
    };

    private static WakeLock obtainTaskWakeLock() {
		return obtainTaskWakeLock(false);
    };

    private static WakeLock obtainTaskWakeLock(boolean acq_force) {
		WakeLock wl=((PowerManager)mContext.getSystemService(Context.POWER_SERVICE))
    			.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
    					| PowerManager.ON_AFTER_RELEASE, "AutoPhoneUnlock-Misc");
		if (TaskManager.isAcqWakeLockRequired(mEnvParms) || acq_force) wl.acquire();
		return wl;
    };

    private static ThreadCtrl mTcPlayBackNotification=null;
    final private static void playBackNotification(final int id) {
    	if (mTcPlayBackNotification==null) mTcPlayBackNotification=new ThreadCtrl();
		final WakeLock wl=obtainTaskWakeLock();
    	Runnable th=new Runnable(){
    		@Override
    		public void run(){
    			Ringtone player=null;
				Uri rt_uri=null;
    			if (id==1) {//Locked notification
    				if (mEnvParms.notifyScreenLockedNotificationPath.startsWith("/system/")) rt_uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    				else rt_uri=Uri.parse("content://media"+mEnvParms.notifyScreenLockedNotificationPath);
    			} else if (id==2){//Unlocked notification
    				if (mEnvParms.notifyScreenUnlockedNotificationPath.startsWith("/system/")) rt_uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    				else rt_uri=Uri.parse("content://media"+mEnvParms.notifyScreenUnlockedNotificationPath);
    			}
				player=RingtoneManager.getRingtone(mContext, rt_uri);
				if (player!=null) {
					player.play();
				}
				if (wl.isHeld()) wl.acquire();
    		}
    	};
    	TaskManager.executeTaskByHighPriority(mEnvParms, mTaskMgrParms, mUtil, th);
    };

    final private static void cancelPlayBackDefaultAlarm() {
    	if (mTcPlayBackAlarm!=null) {
    		synchronized(mTcPlayBackAlarm) {
    			mTcPlayBackAlarm.setDisabled();
    			mTcPlayBackAlarm.notify(); 
    		}
    	}
    };
    
    private static ThreadCtrl mTcPlayBackAlarm=null;
    final private static void playBackDefaultAlarm(final int duration) {
    	if (mTcPlayBackAlarm==null) mTcPlayBackAlarm=new ThreadCtrl();
    	cancelPlayBackDefaultAlarm();
		final WakeLock wl=obtainTaskWakeLock();
    	Runnable th=new Runnable(){
    		@Override
    		public void run(){
    			mUtil.addDebugMsg(2, "I", "Alarm playback started");
    			mTcPlayBackAlarm.setEnabled();
    			Ringtone player=null;
				Uri rt_uri=null;
				rt_uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
				player=RingtoneManager.getRingtone(mContext, rt_uri);
				if (player!=null) {
					player.play();
					synchronized(mTcPlayBackAlarm) {
						try {
							mTcPlayBackAlarm.wait(duration*1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					player.stop();
				}
				if (wl.isHeld()) wl.acquire();
				mUtil.addDebugMsg(2, "I", "Alarm playback ended");
    		}
    	};
    	TaskManager.executeTaskByHighPriority(mEnvParms, mTaskMgrParms, mUtil, th);
    };
    
    final static private void checkScreenUnlocked() {
    	if (mEnvParms.settingDebugLevel>=1) mUtil.addDebugMsg(1,"I","checkScreenUnlocked entered, ",
				"mEnvParms.bluetoothOnScreenUnlocked="+mEnvParms.bluetoothOnScreenUnlocked,
				", mEnvParms.wifiOnScreenUnlocked="+mEnvParms.wifiOnScreenUnlocked);
    	cancelTaskProximityDetected();
//    	cancelTaskProximityUndetected();
    	cancelTaskWaitWifiOff();
    	if (!mEnvParms.bluetoothOnScreenUnlocked.equals(BLUETOOTH_ON_WHEN_SCREEN_UNLOCKED_DISABLED)) {
    		if (mEnvParms.bluetoothOnScreenUnlocked.equals(BLUETOOTH_ON_WHEN_SCREEN_UNLOCKED_ALWAYS)) {
    			if (BluetoothAdapter.getDefaultAdapter()!=null) 
    				BluetoothAdapter.getDefaultAdapter().enable();
    		} else if (mEnvParms.bluetoothOnScreenUnlocked.equals(BLUETOOTH_ON_WHEN_SCREEN_UNLOCKED_CHARGING)) {
    			if (mEnvParms.isBatteryCharging()) {
    				if (BluetoothAdapter.getDefaultAdapter()!=null) 
    					BluetoothAdapter.getDefaultAdapter().enable();
    			}
    		}
    	} 
    	if (!mEnvParms.wifiOnScreenUnlocked.equals(WIFI_ON_WHEN_SCREEN_UNLOCKED_DISABLED)) {
    		if (mEnvParms.wifiOnScreenUnlocked.equals(WIFI_ON_WHEN_SCREEN_UNLOCKED_ALWAYS)) {
    			mWifiMgr.setWifiEnabled(true);
    		} else if (mEnvParms.wifiOnScreenUnlocked.equals(WIFI_ON_WHEN_SCREEN_UNLOCKED_CHARGING)) {
    			if (mEnvParms.isBatteryCharging()) mWifiMgr.setWifiEnabled(true);
    		}
    	}
    	TaskManager.showNotification(mTaskMgrParms, mEnvParms, mUtil);
    };

    final static private void checkScreenLocked() {
    	if (mEnvParms.settingDebugLevel>=1) mUtil.addDebugMsg(1,"I","checkScreenLocked entered, ",
				"mEnvParms.bluetoothOffScreenLocked="+mEnvParms.bluetoothOffScreenLocked,
				", mEnvParms.wifiOffScreenLocked="+mEnvParms.wifiOffScreenLocked);
    	cancelTaskProximityUndetected();
    	cancelTaskProximityDetected();
    	if (!mEnvParms.bluetoothOffScreenLocked.equals(BLUETOOTH_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_DISABLED)) {
    		if (mEnvParms.bluetoothOffScreenLocked.equals(BLUETOOTH_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_ALWAYS)) {
    			setBluetoothOffTimer();
    		} else if (mEnvParms.bluetoothOffScreenLocked.equals(BLUETOOTH_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_BATTERY)) {
    			if (!mEnvParms.isBatteryCharging()) {
    				setBluetoothOffTimer();
    			}
    		}
    	} 
    	if (!mEnvParms.wifiOffScreenLocked.equals(WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_DISABLED)) {
    		if (mEnvParms.wifiOffScreenLocked.equals(WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_ALWAYS)) {
    			setWifiOffTimer();
    		} else if (mEnvParms.wifiOnScreenUnlocked.equals(WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_BATTERY)) {
    			if (!mEnvParms.isBatteryCharging()) {
    				setWifiOffTimer();
    			}
    		}
    	}
    	TaskManager.showNotification(mTaskMgrParms, mEnvParms, mUtil);
    };
    
    private static ThreadCtrl mTcBluetoothOffTimer=null;
    static private void setBluetoothOffTimer() {
    	if (BluetoothAdapter.getDefaultAdapter()!=null && BluetoothAdapter.getDefaultAdapter().isEnabled()) {
    		enableThreadCtrl(mTcBluetoothOffTimer);
    		final WakeLock wl=obtainTaskWakeLock();
    		mUtil.addDebugMsg(1,"I","setBluetoothOffTimer　started("+mEnvParms.wifiOffScreenLockedTimeoutValue/1000+"Sec)");
    		Runnable th=new Runnable(){
        		@Override
        		public void run(){
        			waitThreadCtrl(mTcBluetoothOffTimer, mEnvParms.wifiOffScreenLockedTimeoutValue);
        			if (mTcBluetoothOffTimer.isEnabled()) {
        				putThreadDebugMsg(1,"I","setBluetoothOffTimer expired");
        				if (BluetoothAdapter.getDefaultAdapter()!=null) 
        					BluetoothAdapter.getDefaultAdapter().disable();
        			} else {
        				putThreadDebugMsg(1,"I","setBluetoothOffTimer cancelled by other task");
        			}
        			if (wl.isHeld()) wl.release();
        		}
    		};
    		TaskManager.executeTaskByNormalPriority(mEnvParms, mTaskMgrParms, mUtil, th);
    	} else {
    		mUtil.addDebugMsg(1,"I","setBluetoothOffTimer　ignored, Bluetooth already off or Bluetooth does not exist");
    	}
    };

    private static ThreadCtrl mTcWifiOffTimer=null;
    static private void setWifiOffTimer() {
    	if (mWifiMgr.isWifiEnabled()) {
    		enableThreadCtrl(mTcWifiOffTimer);
    		final WakeLock wl=obtainTaskWakeLock();
    		mUtil.addDebugMsg(1,"I","setWifiOffTimer　started("+mEnvParms.wifiOffScreenLockedTimeoutValue/1000+"Sec)");
    		Runnable th=new Runnable(){
        		@Override
        		public void run(){
        			waitThreadCtrl(mTcWifiOffTimer, mEnvParms.wifiOffScreenLockedTimeoutValue);
        			if (mTcWifiOffTimer.isEnabled()) {
        				putThreadDebugMsg(1,"I","setWifiOffTimer timer expired");
        				mWifiMgr.setWifiEnabled(false);
        			} else {
        				putThreadDebugMsg(1,"I","setWifiOffTimer timer cancelled by other task");
        			}
        			if (wl.isHeld()) wl.release();
        		}
    		};
    		TaskManager.executeTaskByNormalPriority(mEnvParms, mTaskMgrParms, mUtil, th);
    	} else {
    		mUtil.addDebugMsg(1,"I","setWifiOffTimer　ignored, WiFi already off");
    	}
    };

    final static private void cancelTaskProximityDetected() {
    	cancelThreadCtrl(mTcProximityDetected);
    };

    final static private void cancelTaskWaitWifiOff() {
    	cancelThreadCtrl(mTcWifiOffTimer);
    };
    
    final static private void waitThreadCtrl(ThreadCtrl tc, long incr) {
    	synchronized(tc) {
    		try {
				tc.wait(incr);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
    }
    
    final static private void cancelThreadCtrl(ThreadCtrl tc) {
    	synchronized(tc) {
    		if (tc.isEnabled()) {
        		tc.setDisabled();
        		tc.notifyAll();
    		}
    	}
    };
    
    final static private void enableThreadCtrl(ThreadCtrl tc) {
    	synchronized(tc) {
    		if (!tc.isEnabled()) {
        		tc.setEnabled();
    		}
    	}
    };

    final static private void cancelTaskProximityUndetected() {
    	cancelThreadCtrl(mTcProximityUndetected);
    };
    
    private static void putThreadDebugMsg(int lvl, String cat, String msg) {
    	mUtil.addDebugMsg(lvl, cat, Thread.currentThread().getName()," ",msg);
    };
    
    private static ThreadCtrl mTcProximityDetected=null;
    final static private void checkProximityDetected() {
    	if (!mEnvParms.proximityDetected.equals(PROXIMITY_DETECTED_DISABLED)) {
    		if ((mEnvParms.proximityDetected.equals(PROXIMITY_DETECTED_IGNORE_LANDSCAPE) && 
    				mEnvParms.isOrientationLanscape()) || !mEnvParms.isTelephonyCallStateIdle()) {
    			mUtil.addDebugMsg(1,"I","Proximity detection ignored, landscape="+mEnvParms.isOrientationLanscape()+
    					", TelephonyStateIdle="+mEnvParms.isTelephonyCallStateIdle());
    			//Ignore
    		} else {
    			cancelTaskProximityUndetected();
    			cancelTaskProximityDetected();
    			if (mEnvParms.screenIsLocked && !mEnvParms.screenIsOn) {
        			mUtil.addDebugMsg(1,"I","Proximity detection ignored, already locked");
    			} else {
    				enableThreadCtrl(mTcProximityDetected);
    	    		final WakeLock wl=obtainTaskWakeLock();
        			mUtil.addDebugMsg(1,"I","Proximity(Detected)　timer started("+mEnvParms.proximityScreenLockTimeValue/1000+"Sec)");
        			Runnable th=new Runnable(){
        	    		@Override
        	    		public void run() {
        	    			int to=0, inc=100;
        	    			boolean sw_exit=false;
        	    			while(mTcProximityDetected.isEnabled() && !sw_exit) {
        	    				waitThreadCtrl(mTcProximityDetected, inc);
//        	    				SystemClock.sleep(inc);
        	    				if (!mEnvParms.isProximitySensorDetected()) {
        	    					sw_exit=true;
        	    					putThreadDebugMsg(1,"I","Proximity(Detected) timer cancelled, because proximity not detected");
        	    				} else {
        	    					if (!mEnvParms.isTelephonyCallStateIdle()) {
            	    					sw_exit=true;
            	    					putThreadDebugMsg(1,"I","Proximity(Detected) timer cancelled, because telephony state not idle");
        	    					} else {
            	    					to+=inc;
            	    					if (to>=mEnvParms.proximityScreenLockTimeValue) {
            	    						mUtil.screenLockNow();
            	    						sw_exit=true;
            	    						putThreadDebugMsg(1,"I","Proximity(Detected) timer expired, screen now loked");
            	    					}
        	    					}
        	    				}
        	    			}
        	    			if (!mTcProximityDetected.isEnabled() && !sw_exit) {
        	    				putThreadDebugMsg(1,"I","Proximity(Detected) timer cancelled by other task");
        	    			}
        	    			if (wl.isHeld()) wl.release();
        	    		}
        			};
        			TaskManager.executeTaskByNormalPriority(mEnvParms, mTaskMgrParms, mUtil, th);
    			}
    		}
    	} else {
    		mUtil.addDebugMsg(1,"I","Proximity detection ignored, Proximity detect disabled");    		
    	}
    };
    
    private static ThreadCtrl mTcProximityUndetected=null;
    final static private void checkProximityUndetected() {
    	if (mEnvParms.proximityUndetected.equals(PROXIMITY_UNDETECTED_ENABLED)) {
    		cancelTaskProximityDetected();
    		cancelTaskProximityUndetected();
    		if (mEnvParms.screenIsLocked) {
    			enableThreadCtrl(mTcProximityUndetected);
    			Runnable th=new Runnable(){
    	    		@Override
    	    		public void run() {
    	    			setScreenOn(mTcProximityUndetected);
    	    			if (!mTcProximityUndetected.isEnabled())
    	    				mUtil.addDebugMsg(1,"I","Proximity undetection cancelled by other task");
    	    		}
    			};
    			TaskManager.executeTaskByNormalPriority(mEnvParms, mTaskMgrParms, mUtil, th);
    		} else {
        		mUtil.addDebugMsg(1,"I","Proximity undetection ignored, Already unlocked");
    		}
    	} else {
    		mUtil.addDebugMsg(1,"I","Proximity undetection ignored, Proximity detect disabled");
    	}
    };
    
	@SuppressWarnings("deprecation")
	final static public void setScreenOn(ThreadCtrl tc) {
   		WakeLock wakelock= 
   	    		((PowerManager)mTaskMgrParms.context.getSystemService(Context.POWER_SERVICE))
   	    			.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
   	    				| PowerManager.ACQUIRE_CAUSES_WAKEUP
//   	   	    				| PowerManager.ON_AFTER_RELEASE
   	    				, "AutoPhoneUnlock-ScreenOn");
   		try {
   			mUtil.addDebugMsg(1,"I","Screen on started");
   			wakelock.acquire();
   			int to=10*1000;
   			int ct=0, inc=50;
   			while(ct<=to && tc.isEnabled()) {
   				waitThreadCtrl(tc, inc);
   				ct+=inc;
   			}
   			mUtil.addDebugMsg(1,"I","Screen on ended");
   		} finally {
   			if (wakelock.isHeld()) wakelock.release();
   		}
	};

	final static private void cancelTaskBluetoothWait() {
		cancelThreadCtrl(mTcBluetoothConnect);
	};
	
	final static private void cancelTaskBluetoothReconnectWait() {
		cancelThreadCtrl(mTcBluetoothDeviceReconnectTimer);
	};
	
    private static ThreadCtrl mTcBluetoothConnect=null;
    final static private void waitUntilBluetoothConnected() {
    	if (mEnvParms.isBluetoothConnected()) {
//    		cancelTaskBluetoothWait();
    		return;
    	}
    	enableThreadCtrl(mTcBluetoothConnect);
		final WakeLock wl=obtainTaskWakeLock();
    	Runnable th=new Runnable(){
    		@Override
    		public void run() {
    			putThreadDebugMsg(1,"I","Bluetooth connect timer started("+mEnvParms.bluetoothConnectionTimeoutValue/1000+"Sec)");
    			int to=0, inc=100;
    			boolean sw_exit=false;
    			while(mTcBluetoothConnect.isEnabled() && !sw_exit) {
    				waitThreadCtrl(mTcBluetoothConnect, inc);
    				if (mEnvParms.isBluetoothConnected()) {
    					sw_exit=true;
    					putThreadDebugMsg(1,"I","Bluetooth connect timer cancelled, because Bluetooth is connected");
    				} else {
    					to+=inc;
    					if (to>=(mEnvParms.wifiConnectionTimeoutValue+0)) {
    						BluetoothAdapter.getDefaultAdapter().disable();
    						sw_exit=true;
    						putThreadDebugMsg(1,"I","Bluetooth connect timer expired, turn off bluetooth adapter");
    					}
    				}
    			}
    			if (!mTcBluetoothConnect.isEnabled() && !sw_exit) 
    				putThreadDebugMsg(1,"I","Bluetooth connect timer cancelled by other task");
    			if (wl.isHeld()) wl.release();
    		};
    	};
    	TaskManager.executeTaskByNormalPriority(mEnvParms, mTaskMgrParms, mUtil, th);
    };
    
    private static ThreadCtrl mTcWifiConnect=null;
    final static private void waitUntilWifiConnected() { 
    	if (mEnvParms.isWifiConnected()) {
//    		cancelTaskWifiWait();
    		return;
    	}
    	enableThreadCtrl(mTcWifiConnect);
		final WakeLock wl=obtainTaskWakeLock();
    	Runnable th=new Runnable(){
    		@Override
    		public void run() {
    			putThreadDebugMsg(1,"I","WiFi connect timer started("+mEnvParms.wifiConnectionTimeoutValue/1000+"Sec)");
    			int to=0, inc=100;
    			boolean sw_exit=false;
    			while(mTcWifiConnect.isEnabled() && !sw_exit) {
    				waitThreadCtrl(mTcWifiConnect, inc);
    				if (mEnvParms.isWifiConnected()) {
    					sw_exit=true;
    					putThreadDebugMsg(1,"I","WiFi connect timer cancelled by WiFi is connected");
    				} else {
    					to+=inc;
    					if (to>=(mEnvParms.wifiConnectionTimeoutValue+0)) {
    						mWifiMgr.setWifiEnabled(false);
    						sw_exit=true;
    						putThreadDebugMsg(1,"I","WiFi connect timer expired, turn off WiFi");
    					}
    				}
    			}
    			if (!mTcWifiConnect.isEnabled() && !sw_exit) putThreadDebugMsg(1,"I","Wifi connect timer cancelled by other task");
    			if (wl.isHeld()) wl.release();
    		};
    	};
    	TaskManager.executeTaskByNormalPriority(mEnvParms, mTaskMgrParms, mUtil, th);
    };
    
    final static private void cancelTaskWifiWait() {
    	cancelThreadCtrl(mTcWifiConnect);
    };

    final static private void cancelTaskWifiReconnectWait() {
    	cancelThreadCtrl(mTcWifiDeviceReconnectTimer);
    };

    static private ThreadCtrl mTcBluetoothDeviceReconnectTimer=null;
    final static private void waitUntilBluetoothDeviceReconnect(final String event) {
    	waitUntillDeviceReconnect(event, TrustItem.TYPE_BLUETOOTH_CLASSIC_DEVICE, mTcBluetoothDeviceReconnectTimer);
    };

    final static private void waitUntillDeviceReconnect(final String event, final int type, final ThreadCtrl tc) {
    	if (mEnvParms.trustedDeviceReconnectWaitTime>0 && !mEnvParms.trustedDeviceImmediateLockWhenDisconnected) {
    		final WakeLock wl=obtainTaskWakeLock();
    		enableThreadCtrl(tc);
        	Runnable th=new Runnable(){
        		@Override
        		public void run() {
    				synchronized(tc) {
    					putThreadDebugMsg(1,"I","Trusted device reconnect timer started, type="+type+
    							", wifiSsidName="+mEnvParms.wifiSsidName+
    							", bluetoothLastEventDeviceName="+mEnvParms.bluetoothLastEventDeviceName+
    							", time="+mEnvParms.trustedDeviceReconnectWaitTime/1000+"Sec");
    					waitThreadCtrl(tc,mEnvParms.trustedDeviceReconnectWaitTime);
    					if (tc.isEnabled()) {
    						putThreadDebugMsg(1,"I","Trusted device reconnect timer timeout occured, type="+type);
    						if (type==TrustItem.TYPE_BLUETOOTH_CLASSIC_DEVICE) {
        						isTrustedBluetoothDeviceConnected(event);
        						resetKeyGuard(true);
//        						waitBluetoothDeviceConnectedIfRequired();
    						} else if (type==TrustItem.TYPE_WIFI_AP) {
        						isTrustedWifiAccessPointConnected(event);
        						resetKeyGuard(true);
//        			    		waitWifiDeviceConnectIfRequired();
    						}
    					} else putThreadDebugMsg(1,"I","Trusted device reconnection timer was canceled, type="+type);
    				}
        			if (wl.isHeld()) wl.release();
        		};
        	};
        	TaskManager.executeTaskByNormalPriority(mEnvParms, mTaskMgrParms, mUtil, th);
    	} else {
			if (type==TrustItem.TYPE_BLUETOOTH_CLASSIC_DEVICE) {
				isTrustedBluetoothDeviceConnected(event);
				resetKeyGuard(true);
//				waitBluetoothDeviceConnectedIfRequired();
			} else if (type==TrustItem.TYPE_WIFI_AP) {
				isTrustedWifiAccessPointConnected(event);
				resetKeyGuard(true);
//	    		waitWifiDeviceConnectIfRequired();
			}
    	}
    };

    static private ThreadCtrl mTcWifiDeviceReconnectTimer=null;
    final static private void waitUntilWifiAccessPointReconnect(final String event) {
    	waitUntillDeviceReconnect(event, TrustItem.TYPE_WIFI_AP, mTcWifiDeviceReconnectTimer);
    };

    static private boolean isTrustedWifiAccessPointConnected(String event) {
		boolean trusted=false;
    	if (mEnvParms.trustItemList.size()>0 && mEnvParms.wifiIsConnected) {
    		trusted=isDeviceTrusted(TrustItem.TYPE_WIFI_AP, mEnvParms.wifiSsidName, mEnvParms.wifiSsidAddr);
    	}
    	if (mEnvParms.settingDebugLevel>=1) 
    		mUtil.addDebugMsg(1,"I","Wifi Trusted device was connected, result="+trusted+", device name="+mEnvParms.wifiSsidName+", addr="+mEnvParms.wifiSsidAddr);
    	mEnvParms.wifiTrusted=trusted;
    	return trusted;
    };

    static private void resetKeyGuard(boolean locked) {
    	if (mEnvParms.settingDebugLevel>=1) 
    		mUtil.addDebugMsg(1,"I","resetKeyguard bluetoothTrusted="+mEnvParms.bluetoothTrusted+", wifiTrusted="+mEnvParms.wifiTrusted);
		if (mEnvParms.bluetoothTrusted || mEnvParms.wifiTrusted ) {
			setKeyguardDisabled(mTaskMgrParms, mEnvParms, mUtil);
		} else {
			boolean prev_kgs_unlocked=mEnvParms.isKeyGuardStatusUnlocked();
			setKeyguardEnabled(mTaskMgrParms, mEnvParms, mUtil);
			if (mEnvParms.trustedDeviceImmediateLockWhenDisconnected ) {
				if (prev_kgs_unlocked && locked) {
					mUtil.screenLockNow();
//					Thread.dumpStack();
				}
			}
		}
    };
    
    static private boolean isTrustedBluetoothDeviceConnected(String event) {
		String name="";
		String addr="";
    	boolean trusted=false;
		if (mEnvParms.isBluetoothConnected()) {
			ArrayList<TrustItem>bdl=mEnvParms.getBluetoothConnectedDeviceList();
			for(TrustItem tdli:bdl) {
				name=tdli.trustDeviceName;
				addr=tdli.trustDeviceAddr;
				if (isDeviceTrusted(tdli.trustItemType, name, addr)) {
					trusted=true;
					break;
				}
			}
		}
    	if (mEnvParms.settingDebugLevel>=1) 
    		mUtil.addDebugMsg(1,"I","Bluetooth Trusted device was connected, result="+trusted+", device name="+name+", addr="+addr);
    	mEnvParms.bluetoothTrusted=trusted;
    	return trusted;
    };

    static private boolean isDeviceTrusted(int type, String name, String addr) {
    	boolean trusted=false;
    	TrustItem tdli=mEnvParms.getTrustItem(type, name, addr);
    	if (tdli!=null && tdli.isEnabled()) trusted=true;
    	if (mEnvParms.settingDebugLevel>=2) 
    		mUtil.addDebugMsg(2,"I","isDeviceTrusted, result="+trusted+", type="+type+", name="+name+", addr="+addr);
		return trusted;
    };
    
	final static public boolean setKeyguardDisabled(TaskManagerParms tmp, 
			EnvironmentParms ep, final CommonUtilities util) {
		boolean result=false;
		if (!ep.screenIsLocked) {
			ep.setKgDisabled(mContext);
			mEnvParms.setKeyGuardStatusUnlocked();
			ep.enableKeyguard=false;
			ep.pendingRequestForEnableKeyguard=false;
			util.addDebugMsg(1, "I", "disableKeyguard issued immediately");
			result=true;
		} else {
			util.addDebugMsg(1, "I", "disableKeyguard will be issued during USER_PRESENT processed");
			if (!mEnvParms.isKeyGuardStatusUnlocked()) mEnvParms.setKeyGuardStatusManualUnlockRequired();
			ep.enableKeyguard=false;
			ep.pendingRequestForEnableKeyguard=true;
			result=true;
		}
		TaskManager.showNotification(tmp, ep, util);
//		if (tmp.enableKeyguard) {
//			tmp.enableKeyguard=false;
//			if (!ep.screenIsLocked) {
//				tmp.keyguardLock.disableKeyguard();
//				tmp.pendingRequestForEnableKeyguard=false;
//				util.addDebugMsg(1, "I", "disableKeyguard issued immediately");
//				result=true;
//			} else {
//				util.addDebugMsg(1, "I", "disableKeyguard will be issued during USER_PRESENT processed");
//				tmp.pendingRequestForEnableKeyguard=true;
//				result=true;
//			}
//			TaskManager.showNotification(tmp, ep, util);
//		} else {
//			util.addDebugMsg(1, "I", "disableKeyguard request ignored, because keyguard is already disabled");
//		}
		return result;
	};

	final static public boolean setKeyguardEnabled(TaskManagerParms tmp, 
			EnvironmentParms ep, final CommonUtilities util) {
		boolean result=false;
		
		if (isValidDpmPasswordLength()) {
			ep.enableKeyguard=true;
			if (ep.screenIsLocked && ep.screenIsOn) {
				mEnvParms.setKeyGuardStatusLocked();
				ep.pendingRequestForEnableKeyguard=true;
				util.addDebugMsg(1, "I", "reenableKeyguard will be issued during SCREEN_OFF processed");
				result=true;
			} else {
				ep.setKgEnabled(mContext);
				mEnvParms.setKeyGuardStatusLocked();
				if (ep.screenIsLocked) util.screenLockNow();
				ep.pendingRequestForEnableKeyguard=false;
				util.addDebugMsg(1, "I", "reenableKeyguard issued immediately");
				result=true;
			}
			TaskManager.showNotification(tmp, ep, util);
		}
		
		return result;
	};

    
    final static private void startPhoneStateListener() {
    	final TelephonyManager tm = (TelephonyManager)mContext.getSystemService(TELEPHONY_SERVICE);
    	tm.listen(new PhoneStateListener() {
    	    @Override
    	    public void onCallStateChanged(int state, String number) {
    	        switch(state) {
    	        case TelephonyManager.CALL_STATE_RINGING:
    	        	mEnvParms.telephonyStatus=TelephonyManager.CALL_STATE_RINGING;
//    	        	addTaskScheduleQueueBuiltin(mTaskMgrParms,mBuiltinEventTaskList,BUILTIN_EVENT_PHONE_CALL_STATE_RINGING);
    	        	scheduleTask(BUILTIN_EVENT_PHONE_CALL_STATE_RINGING);
    	            break;
    	 
    	        case TelephonyManager.CALL_STATE_OFFHOOK:
    	        	mEnvParms.telephonyStatus=TelephonyManager.CALL_STATE_OFFHOOK;
//    	        	addTaskScheduleQueueBuiltin(mTaskMgrParms,mBuiltinEventTaskList,BUILTIN_EVENT_PHONE_CALL_STATE_OFF_HOOK);
    	        	scheduleTask(BUILTIN_EVENT_PHONE_CALL_STATE_OFF_HOOK);
    	            break;
    	 
    	        case TelephonyManager.CALL_STATE_IDLE:
    	        	mEnvParms.telephonyStatus=TelephonyManager.CALL_STATE_IDLE;
//    	        	addTaskScheduleQueueBuiltin(mTaskMgrParms,mBuiltinEventTaskList,BUILTIN_EVENT_PHONE_CALL_STATE_IDLE);
    	        	scheduleTask(BUILTIN_EVENT_PHONE_CALL_STATE_IDLE);
    	            break;
    	    };
    	    }
    	}, PhoneStateListener.LISTEN_CALL_STATE);
    };

    final static private class MiscellaneousReceiver extends BroadcastReceiver {
		@SuppressLint("Wakelock")
		@Override
		final public void onReceive(Context c, Intent in) {
    		final WakeLock wl=obtainTaskWakeLock();
			String action=in.getAction();
			mUtil.addDebugMsg(2,"I", "MiscellaneousReceiver entered, action=",action);
			if(action.equals(AudioManager.RINGER_MODE_CHANGED_ACTION)){
				mUtil.addDebugMsg(1,"I", "RingerMode from="+mEnvParms.currentRingerMode+", new="+mAudioManager.getRingerMode());
				mEnvParms.currentRingerMode=mAudioManager.getRingerMode();
				TaskManager.showNotification(mTaskMgrParms, mEnvParms, mUtil);
			} else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
				int nv=getAirplaneModeOn();
				if (nv!=mEnvParms.airplane_mode_on) {
					mEnvParms.airplane_mode_on=nv;
//					if (nv==0) addTaskScheduleQueueBuiltin(mTaskMgrParms,BUILTIN_EVENT_AIRPLANE_MODE_OFF);
//					else addTaskScheduleQueueBuiltin(mTaskMgrParms,BUILTIN_EVENT_AIRPLANE_MODE_ON);
					if (nv==0) scheduleTask(BUILTIN_EVENT_AIRPLANE_MODE_OFF);
					else scheduleTask(BUILTIN_EVENT_AIRPLANE_MODE_ON);
				}
			} else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
		        ConnectivityManager cm =
		                (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
		        NetworkInfo ani = cm.getActiveNetworkInfo();
	            if (ani != null) {
	                if (ani.getType() == ConnectivityManager.TYPE_WIFI) {
		            	String ssid=mWifiMgr.getConnectionInfo().getSSID();
		            	String bssid=mWifiMgr.getConnectionInfo().getBSSID();
		            	mUtil.addDebugMsg(1,"I", "ConnectivityManager WiFi connected, "+
		            			"SSID="+ssid+", BSSID="+bssid);
	                }
	            }
			}
			if (wl.isHeld()) wl.release();
		}
    };

    static private class  BatteryStatusReceiver  extends BroadcastReceiver {
		@Override
		final public void onReceive(Context c, Intent in) {
			mLastBatteryStatusSt=in.getIntExtra("status", 0);
			mLastBatteryStatusBl = in.getIntExtra("level", 0);
			mLastBatteryStatusBs = in.getIntExtra("scale", 0);
			analyzeBatteryStatusValue(mLastBatteryStatusSt,mLastBatteryStatusBl,mLastBatteryStatusBs);
		}
    };

    static private int mLastBatteryStatusSt,mLastBatteryStatusBl,mLastBatteryStatusBs;
    static final private String parseBatteryChargeStatus(int st) {
		String n_bcs=mTaskMgrParms.svcMsgs.msgs_widget_battery_status_charge_discharging;
		if (st==BatteryManager.BATTERY_PLUGGED_AC||st==BatteryManager.BATTERY_STATUS_CHARGING){
			n_bcs=mTaskMgrParms.svcMsgs.msgs_widget_battery_status_charge_charging;
			mEnvParms.batteryChargeStatusInt=mEnvParms.BATTERY_CHARGE_STATUS_INT_CHARGING;
		} else if (st==BatteryManager.BATTERY_STATUS_DISCHARGING||st==BatteryManager.BATTERY_STATUS_NOT_CHARGING){
			n_bcs=mTaskMgrParms.svcMsgs.msgs_widget_battery_status_charge_discharging;
			mEnvParms.batteryChargeStatusInt=mEnvParms.BATTERY_CHARGE_STATUS_INT_DISCHARGING;
		} else if (st==BatteryManager.BATTERY_STATUS_FULL){
			n_bcs=mTaskMgrParms.svcMsgs.msgs_widget_battery_status_charge_full;
			mEnvParms.batteryChargeStatusInt=mEnvParms.BATTERY_CHARGE_STATUS_INT_FULL;
		}
		return n_bcs;
    };
    
    static final private void analyzeBatteryStatusValue(int st,int bl, int bs) {
		String n_ps="";
		int n_bl=0;
		if (bs==0) n_bl=bl;
		else n_bl=(bl*100)/bs;
		if (st==BatteryManager.BATTERY_PLUGGED_AC||
				st==BatteryManager.BATTERY_PLUGGED_USB ||
				st==BatteryManager.BATTERY_STATUS_FULL )
//				st==BatteryManager.BATTERY_STATUS_NOT_CHARGING) 
				n_ps=CURRENT_POWER_SOURCE_AC;
			else n_ps=CURRENT_POWER_SOURCE_BATTERY;
		if (n_ps==CURRENT_POWER_SOURCE_AC) {
			if (mEnvParms.batteryPowerSource.equals(n_ps)) {
				//充電中が継続なのでなにもしない
			} else {
				//放電から充電に切りかわったのでなにもしない
			}
		} else {
			if (mEnvParms.batteryPowerSource.equals(n_ps)) {
				if (mEnvParms.batteryLevel==-1) {
					//起動直後ため何もしない
				} else {
				}
			} else {
				//放電に切りかわった
			}
		}
		String n_bcs=parseBatteryChargeStatus(st);
//		mUtil.addDebugMsg(1,"I","Battery receiver bl=",String.valueOf(bl));
		if (mEnvParms.batteryLevel==-1) {
			if (mEnvParms.settingDebugLevel>=1)
				mUtil.addDebugMsg(1,"I","Initial battery status, level=",String.valueOf(n_bl),
					", source=",n_ps,", charge=",n_bcs);
			mEnvParms.batteryChargeStatusString=n_bcs;
			mEnvParms.batteryLevel=n_bl;
			mEnvParms.batteryPowerSource=n_ps;
			checkBatteryNotification(true, false, true);

			TaskManager.showNotification(mTaskMgrParms, mEnvParms, mUtil);
		} else if (!n_ps.equals(mEnvParms.batteryPowerSource) || (n_bl!=mEnvParms.batteryLevel) ||
				(n_bcs!=mEnvParms.batteryChargeStatusString)) {
//			if (envParms.settingDebugLevel>=1)
			mUtil.addLogMsg("I","Battery status changed,",
					" Level=(", String.valueOf(mEnvParms.batteryLevel), ",", String.valueOf(n_bl),")",
					", Power=(",mEnvParms.batteryPowerSource, ",", n_ps,")", 
					", Charge=(", mEnvParms.batteryChargeStatusString, ",", n_bcs,")");
			boolean change_ps=false, change_bcs=false, change_bl=false;
			if (!n_ps.equals(mEnvParms.batteryPowerSource)) change_ps=true;
			if (n_bl!=mEnvParms.batteryLevel) change_bl=true;
			if (n_bcs!=mEnvParms.batteryChargeStatusString) change_bcs=true; 
			mEnvParms.batteryChargeStatusString=n_bcs;
			mEnvParms.batteryLevel=n_bl;
			mEnvParms.batteryPowerSource=n_ps;
			checkBatteryNotification(change_bcs, change_ps, change_bl);

			TaskManager.showNotification(mTaskMgrParms, mEnvParms, mUtil);
		}
    };
    
    final static private void checkBatteryNotification(boolean change_bcs, 
    		boolean change_ps, boolean change_bl) {
		if (change_bcs &&
				mEnvParms.batteryChargeStatusString.equals(mTaskMgrParms.svcMsgs.msgs_widget_battery_status_charge_full)) {
//			addTaskScheduleQueueBuiltin(mTaskMgrParms,BUILTIN_EVENT_BATTERY_FULLY_CHARGED);
			scheduleTask(BUILTIN_EVENT_BATTERY_FULLY_CHARGED);
		}
		if (change_ps) {
			if (mEnvParms.batteryPowerSource.equals(CURRENT_POWER_SOURCE_AC)) {
//				addTaskScheduleQueueBuiltin(mTaskMgrParms,mBuiltinEventTaskList,BUILTIN_EVENT_POWER_SOURCE_CHANGED_AC);
				scheduleTask(BUILTIN_EVENT_POWER_SOURCE_CHANGED_AC);
			} else {
//				addTaskScheduleQueueBuiltin(mTaskMgrParms,mBuiltinEventTaskList,BUILTIN_EVENT_POWER_SOURCE_CHANGED_BATTERY);
				scheduleTask(BUILTIN_EVENT_POWER_SOURCE_CHANGED_BATTERY);
			}
		}
		if (change_bl) {
			scheduleTask(BUILTIN_EVENT_BATTERY_LEVEL_CHANGED);
		}
    	
    };
    
    final static private class WifiReceiver  extends BroadcastReceiver {
		@SuppressLint({ "InlinedApi", "NewApi" })
		@Override
		final public void onReceive(Context c, Intent in) {
			String tssid=CommonUtilities.getWifiSsidName(mWifiMgr);
			String tmac=mWifiMgr.getConnectionInfo().getBSSID();
			String ss=mWifiMgr.getConnectionInfo().getSupplicantState().toString();
//			String action=in.getAction();
//			NetworkInfo ni=in.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
//			if (ni!=null) {
//				if (mEnvParms.wifiSsidName.equals("") && ni.getState().equals(NetworkInfo.State.CONNECTED)){
//					tssid=EnvironmentParms.WIFI_DIRECT_SSID;
//					ss="COMPLETED";
//				} else if (mEnvParms.wifiSsidName.equals(EnvironmentParms.WIFI_DIRECT_SSID) && ni.getState().equals(NetworkInfo.State.DISCONNECTED)){
//					tssid="";
//					ss="DISCONNECTED";
//				}
//			}
			
	        ConnectivityManager cm =
	                (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
	        NetworkInfo ani = cm.getActiveNetworkInfo();
	        boolean cm_connect=false;
            if (ani != null
                && ani.getType() == ConnectivityManager.TYPE_WIFI) {
            	cm_connect=true;
            }
			
			boolean new_wifi_enabled=mWifiMgr.isWifiEnabled();
			mUtil.addDebugMsg(2,"I","WIFI receiver " +"Action="+in.getAction()+
					", SupplicantState="+ss+
					", mEnvParms.wifiIsActive="+mEnvParms.wifiIsActive+
					", new_wifi_enabled="+new_wifi_enabled+
					", mEnvParms.wifiSsidName="+mEnvParms.wifiSsidName+
					", cm_connect="+cm_connect+
					", tssid="+tssid+", SSID addr="+tmac);
			if (!new_wifi_enabled && mEnvParms.wifiIsActive ) {
				mUtil.addDebugMsg(1,"I","WIFI receiver, WIFI Off");
				mEnvParms.wifiSsidName="";
				mEnvParms.wifiSsidAddr="";
				mEnvParms.wifiIsActive=false;
				mEnvParms.wifiIsConnected=false;
				scheduleTask(BUILTIN_EVENT_WIFI_OFF);
			} else {
				if (new_wifi_enabled && !mEnvParms.wifiIsActive) {
					mUtil.addDebugMsg(1,"I","WIFI receiver, WIFI On");
					mEnvParms.wifiSsidName="";
					mEnvParms.wifiSsidAddr="";
					mEnvParms.wifiIsActive=true;
					mEnvParms.wifiIsConnected=false;
					scheduleTask(BUILTIN_EVENT_WIFI_ON);
				} else {
					if (ss.equals("COMPLETED")  
//							|| ss.equals("ASSOCIATING") 
//							|| ss.equals("ASSOCIATED") 
							) {
						if (!tssid.equals("") && tmac!=null) {
							if (!mEnvParms.wifiIsConnected ||
									(!mEnvParms.wifiSsidName.equals(tssid)||!mEnvParms.wifiSsidAddr.equals(tmac))) {
								mUtil.addDebugMsg(1,"I","WIFI receiver, Connected WIFI Access point ssid=",tssid,", mac addr="+tmac);
								mEnvParms.wifiIsConnected=true;
								mEnvParms.wifiSsidName=tssid;
								mEnvParms.wifiSsidAddr=tmac;
								mEnvParms.wifiIsActive=true; //2013/09/04
								scheduleTask(BUILTIN_EVENT_WIFI_CONNECTED);
							}
						}
					} else if (ss.equals("INACTIVE") ||
							ss.equals("DISCONNECTED") ||
							ss.equals("UNINITIALIZED") ||
							ss.equals("INTERFACE_DISABLED")
//							|| ss.equals("SCANNING")
							) {
						if (mEnvParms.wifiIsConnected) {
							mEnvParms.wifiIsConnected=false;
							mUtil.addDebugMsg(1,"I","WIFI receiver, Disconnected WIFI Access point ssid=", mEnvParms.wifiSsidName
									,", mac addr="+tmac);
							mEnvParms.wifiIsActive=true;
							scheduleTask(BUILTIN_EVENT_WIFI_DISCONNECTED);							
						}
					}
				}
			}
		}
    };

    final static private class BluetoothReceiver  extends BroadcastReceiver {
		@SuppressLint("NewApi")
		@Override
		final public void onReceive(Context c, Intent in) {
			String action = in.getAction();
			mUtil.addDebugMsg(2,"I","Bluetooth receiver entered, action=",action);
			int bs=BluetoothAdapter.getDefaultAdapter().getState();
			if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
				if (bs==BluetoothAdapter.STATE_OFF) {
					mEnvParms.bluetoothIsActive=false;
					mEnvParms.bluetoothLastEventDeviceName="";
					mEnvParms.bluetoothLastEventDeviceAddr="";
					mEnvParms.bluetoothLastEventDeviceType=-1;
					
					for(TrustItem tli:mEnvParms.getBluetoothConnectedDeviceList()) {
						tli.setConnected(false);
						if (tli.bluetoothLeGatt!=null) {
							tli.bluetoothLeGatt.disconnect();
							tli.bluetoothLeGatt.close();
							tli.bluetoothLeGatt=null;
						}
					}
					
					mEnvParms.clearBluetoothConnectedDeviceList(c);
					scheduleTask(BUILTIN_EVENT_BLUETOOTH_OFF);
				} else if (bs==BluetoothAdapter.STATE_ON) {
					mEnvParms.bluetoothIsActive=true;
					mEnvParms.bluetoothLastEventDeviceName="";
					mEnvParms.bluetoothLastEventDeviceAddr="";
					mEnvParms.bluetoothLastEventDeviceType=-1;
					mEnvParms.clearBluetoothConnectedDeviceList(c);
					scheduleTask(BUILTIN_EVENT_BLUETOOTH_ON);
				}
			} else {
				BluetoothDevice device = in.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				int type=TrustItem.TYPE_BLUETOOTH_CLASSIC_DEVICE;
				if (Build.VERSION.SDK_INT>=18) {
					if (device.getType()==BluetoothDevice.DEVICE_TYPE_LE) type=TrustItem.TYPE_BLUETOOTH_LE_DEVICE;
				}
				if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
					bluetoothReceiverDeviceConnected(bs, type, device.getName(), device.getAddress());
				} else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
					bluetoothReceiverDeviceDisconnected(bs, type, device.getName(), device.getAddress());
				} else if (action.equals(BluetoothDevice.ACTION_FOUND)) {
					BluetoothDevice found_device = in.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					mUtil.addDebugMsg(1,"I","Bluetooth action found, dev=",found_device.getName()+", addr="+found_device.getAddress());
				}
			}
		}	
    };

    final static private void bluetoothReceiverDeviceConnected(int bs, int type, String name, String addr) {
		mUtil.addDebugMsg(1,"I","Bluetooth connected, state="+bs+", dev=", name+", addr="+addr);
		mEnvParms.bluetoothLastEventDeviceName=name;
		mEnvParms.bluetoothLastEventDeviceAddr=addr;
		mEnvParms.bluetoothLastEventDeviceType=type;
		mEnvParms.addBluetoothConnectedDevice(mContext, type, name, addr,false, true);
		scheduleTask(BUILTIN_EVENT_BLUETOOTH_CONNECTED);
    };

    @SuppressLint("NewApi")
	final static private void bluetoothReceiverDeviceDisconnected(int bs, int type, String name, String addr) {
		mUtil.addDebugMsg(1,"I","Bluetooth disconnected, state="+bs+", dev=", name+", addr="+addr);
		if (bs==BluetoothAdapter.STATE_TURNING_OFF && !isIgnoreDeviceDisconnectHostAction()) 
			setIgnoreDeviceDisconnectHostAction(2000);
 		mEnvParms.bluetoothLastEventDeviceName=name;
		mEnvParms.bluetoothLastEventDeviceAddr=addr;
		mEnvParms.bluetoothLastEventDeviceType=type;
		TrustItem tli=mEnvParms.getBluetoothConnectedDevice(type, name, addr);
		if (tli!=null && tli.bluetoothLeGatt!=null) {
			if (tli.bluetoothLeGatt!=null) tli.bluetoothLeGatt.disconnect();
			if (tli.bluetoothLeGatt!=null) tli.bluetoothLeGatt.close();
			tli.bluetoothLeGatt=null;
		}
		mEnvParms.removeBluetoothConnectedDevice(mContext, type, name, addr);
		scheduleTask(BUILTIN_EVENT_BLUETOOTH_DISCONNECTED);
    };
    
    final static private class SleepReceiver  extends BroadcastReceiver {
		@SuppressLint({ "Wakelock", "NewApi"})
		@Override 
		final public void onReceive(Context c, Intent in) {
			String action = in.getAction();
			boolean c_key_guard_secure=true;
			if (mEnvParms.settingDebugLevel>=1) {
				if (Build.VERSION.SDK_INT>=16) {
			        KeyguardManager km=(KeyguardManager)c.getSystemService(Context.KEYGUARD_SERVICE);
					mUtil.addDebugMsg(1,"I","Sleep receiver entered, action=",action,
							", enableKeyguard="+mEnvParms.enableKeyguard,
							", pendingRequestForEnableKeyguard="+mEnvParms.pendingRequestForEnableKeyguard,
							", isKeyguardEffective()="+mUtil.isKeyguardEffective()+
//							", isDeviceLocked()="+km.isDeviceLocked()+
							", isKeyguardSecure()="+km.isKeyguardSecure()+
							", isScreenOn()="+CommonUtilities.isScreenOn(c)+
							", screenIsOn="+mEnvParms.screenIsOn+
							", screenIsLocked="+mEnvParms.screenIsLocked+
							", proximitySensorValue="+mEnvParms.proximitySensorValue);
					c_key_guard_secure=km.isKeyguardSecure();
				} else {
					mUtil.addDebugMsg(1,"I","Sleep receiver entered, action=",action,
							", enableKeyguard="+mEnvParms.enableKeyguard,
							", pendingRequestForEnableKeyguard="+mEnvParms.pendingRequestForEnableKeyguard,
							", isKeyguardEffective()="+mUtil.isKeyguardEffective()+
							", isScreenOn()="+CommonUtilities.isScreenOn(c)+
							", screenIsOn="+mEnvParms.screenIsOn+
							", screenIsLocked="+mEnvParms.screenIsLocked+
							", proximitySensorValue="+mEnvParms.proximitySensorValue);
				}
			}
			scanBtLeDevice(false);
			boolean prev_screenIsOn=mEnvParms.screenIsOn;
			if(action.equals(Intent.ACTION_SCREEN_ON)) {
				boolean kge=mUtil.isKeyguardEffective();
				mEnvParms.screenIsOn=true;
				if (!kge) {
					relWakeLockForSleep();
//			 		stopProximitySensorReceiver();
//			 		startProximitySensorReceiver();
			 		if (mEnvParms.screenIsLocked) {
						mEnvParms.screenIsLocked=false;
			 			scheduleTask(BUILTIN_EVENT_SCREEN_UNLOCKED);
//			 			TaskManager.buildNotification(mTaskMgrParms, mEnvParms);
//			 			TaskManager.showNotification(mTaskMgrParms, mEnvParms, mUtil);
//			 			if (Build.VERSION.SDK_INT>=21) {
//			 			}
			 		}
			 		if (mEnvParms.proximitySensorActive && mEnvParms.proximitySensorValue==0) {
			 			mUiHandler.postDelayed(new Runnable(){
							@Override
							public void run() {
					 			scheduleTask(BUILTIN_EVENT_PROXIMITY_DETECTED);
							}
			 			}, 200);
			 		}
				} else {
			 		if (mEnvParms.proximitySensorActive && mEnvParms.proximitySensorValue==0) {
			 			scheduleTask(BUILTIN_EVENT_PROXIMITY_DETECTED);
			 		}
				}
			} else if(action.equals(Intent.ACTION_SCREEN_OFF)) {
				setIgnoreProximitySensorValue();
//				if (mTaskMgrParms.pendingRequestForEnableKeyguard && mTaskMgrParms.enableKeyguard) {
				if (!mEnvParms.previousKeyguard && c_key_guard_secure) {
					mUtil.addDebugMsg(1,"I","reenableKeyguard issued during screen off because keyguard option changed");
		    		if (mEnvParms.isKeyGuardStatusUnlocked()) mEnvParms.setKeyGuardStatusManualUnlockRequired();
		    		mEnvParms.setKgEnabled(mContext);
				}
				mEnvParms.previousKeyguard=c_key_guard_secure;
//				mEnvParms.setKgEnabled(mContext);
				if (mEnvParms.enableKeyguard) {
					mUtil.addDebugMsg(1,"I","reenableKeyguard issued during screen off");
					if (isValidDpmPasswordLength()) {
						mEnvParms.setKeyGuardStatusLocked();
						mEnvParms.setKgEnabled(mContext);
						mEnvParms.pendingRequestForEnableKeyguard=false;
						TaskManager.showNotification(mTaskMgrParms, mEnvParms, mUtil);
					}
				}
				mEnvParms.screenIsOn=false;
				if (!mEnvParms.screenIsLocked) {
					mEnvParms.screenIsLocked=true;
					acqWakeLockForSleep();
//			 		stopProximitySensorReceiver();
//			 		startProximitySensorReceiver();
			 		scheduleTask(BUILTIN_EVENT_SCREEN_LOCKED);
//		 			TaskManager.buildNotification(mTaskMgrParms, mEnvParms);
//		 			TaskManager.showNotification(mTaskMgrParms, mEnvParms, mUtil);
//		 			if (Build.VERSION.SDK_INT>=21) {
//		 			}
				} else {
					if (prev_screenIsOn) scheduleTask(BUILTIN_EVENT_SCREEN_OFF);
				}
			} else if(action.equals(Intent.ACTION_USER_PRESENT)) {
//				if (mTaskMgrParms.pendingRequestForEnableKeyguard && !mEnvParms.enableKeyguard) {
				if (!mEnvParms.enableKeyguard) {
					//無効化する前にパスワードやパターンを入れてクリアする必要がある
					mUtil.addDebugMsg(1,"I","disableKeyguard issued during user present");
					mEnvParms.setKgDisabled(mContext);
					mEnvParms.setKeyGuardStatusUnlocked();
					mEnvParms.pendingRequestForEnableKeyguard=false;
				} else {
					mEnvParms.setKeyGuardStatusLocked();
				}
				mEnvParms.screenIsOn=true;
				mEnvParms.screenIsLocked=false;
				relWakeLockForSleep();
//		 		stopProximitySensorReceiver();
//		 		startProximitySensorReceiver();
		 		scheduleTask(BUILTIN_EVENT_SCREEN_UNLOCKED);
//	 			TaskManager.buildNotification(mTaskMgrParms, mEnvParms);
	 			TaskManager.showNotification(mTaskMgrParms, mEnvParms, mUtil);
//	 			if (Build.VERSION.SDK_INT>=21) {
//	 			}
			}
		}	
    };

    private static boolean isIgnoreProximitySensorValue() {
    	return mIgnoreProximitySensorValue;
    };
    private static boolean mIgnoreProximitySensorValue=false;
    private static void setIgnoreProximitySensorValue() {
//    	mIgnoreProximitySensorValue=true;
//    	mUiHandler.postDelayed(new Runnable(){
//			@Override
//			public void run() {
//				mIgnoreProximitySensorValue=false;
//			}
//    	}, 200);
    };
    
    static private ArrayList<Long>mProximityTimeList=new ArrayList<Long>();
    static private int mNoOfProximityEventOccured=5;
    static private int mResetTimeForProximityTimeList=10;
    @SuppressWarnings("unused")
	static private int mResetTimeAfterProximityDisabled=60*10;
	@SuppressLint("Wakelock")
	final static private class ProximitySensorReceiver implements SensorEventListener {
    	@Override
    	final public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    	@Override
    	final public void onSensorChanged(SensorEvent event) {
			WakeLock wl=((PowerManager)mContext.getSystemService(Context.POWER_SERVICE))
					.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AutoPhoneUnlock-Proximity");
			wl.acquire();
			
    		int nv=Integer.valueOf((int) event.values[0]);
        	if (mEnvParms.settingDebugLevel>=1) mUtil.addDebugMsg(1,"I","Proximity sensor current=",
    				String.valueOf(mEnvParms.proximitySensorValue),", new=",String.valueOf(nv));
        	if (mEnvParms.proximitySensorValue!=nv) {
    			mEnvParms.proximitySensorValue=nv;
    			if (mEnvParms.proximitySensorValue>=1) {
					long last=0;
					if (mProximityTimeList.size()>0) last=mProximityTimeList.get(mProximityTimeList.size()-1);
    				if (!mEnvParms.proximitySensorEventTemporaryIgnore) {
    					
    					long ct=System.currentTimeMillis();
    					long last_intvl=(ct-last);
    					long min_intvl=0;
    					if (last==0 || last_intvl>(1000*mResetTimeForProximityTimeList)) {
    						mProximityTimeList.clear();
    						mProximityTimeList.add(ct);
    					} else {
    						mProximityTimeList.add(ct);
    						if (mProximityTimeList.size()>mNoOfProximityEventOccured) mProximityTimeList.remove(0);
    						if (mProximityTimeList.size()>=mNoOfProximityEventOccured) {
    							min_intvl=ct-mProximityTimeList.get(0);
    							if (min_intvl>0 && min_intvl<(1000*mResetTimeForProximityTimeList)) {
    								//Temporary disabled
    								if (!mEnvParms.proximitySensorEventTemporaryIgnore &&
    										mEnvParms.proximityDisabledWhenMultipleEvent) {
    									mEnvParms.proximitySensorEventTemporaryIgnore=true;
    									TaskManager.showErrorNotification(mTaskMgrParms, mEnvParms, mUtil);
    								}
    							}
    						}
    					}
    					if (mEnvParms.settingDebugLevel>=2) 
    						mUtil.addDebugMsg(2,"I","Proximity sensor counter="+mProximityTimeList.size()+
    								", last_intvl="+last_intvl+", min_intvl="+min_intvl);
    					if (!isIgnoreProximitySensorValue()) scheduleTask(BUILTIN_EVENT_PROXIMITY_UNDETECTED);
    					else {
    						mUtil.addDebugMsg(1,"I","Proximity sensor event ignored, because disable flag active");    						
    					}
    				} else {
    					if (mEnvParms.settingDebugLevel>=1) 
    						mUtil.addDebugMsg(1,"I","Proximity sensor event ignored, because proximity sensor event count exceed criteria");
    				}
    			} else scheduleTask(BUILTIN_EVENT_PROXIMITY_DETECTED);
        	} else {
            	if (mEnvParms.settingDebugLevel>=1) mUtil.addDebugMsg(1,"I","Proximity sensor ignored, because same value");
        	}
			
			wl.release();
    	}
    };

	static private void startProximitySensorReceiver() {
		if (mSensorProximity!=null) { 
			if ((!mEnvParms.proximityDetected.equals(PROXIMITY_DETECTED_DISABLED)) ||
					(!mEnvParms.proximityUndetected.equals(PROXIMITY_UNDETECTED_DISABLED))) {
				mEnvParms.proximitySensorActive=true;
				mSensorManager.registerListener(mReceiverProximity, mSensorProximity, SensorManager.SENSOR_DELAY_UI);
				mUtil.addDebugMsg(1, "I", "Proximity sensor receiver was started.");
			}
		}
    };
    
    static private void stopProximitySensorReceiver() {
		if (mEnvParms.proximitySensorActive) {
			mSensorManager.unregisterListener(mReceiverProximity);
			mEnvParms.proximitySensorActive=false;
			mUtil.addDebugMsg(1, "I", "Proximity sensor receiver was stopped.");
		}
    };

    static private void acqWakeLockForSleep() {
    	if (mEnvParms.settingDebugLevel>=2) mUtil.addDebugMsg(2, "I", "acqWakeLockForSleep Proximity=",String.valueOf(mEnvParms.proximitySensorActive),
    			", Airplane=",String.valueOf(mEnvParms.airplane_mode_on),
    			", Held=",String.valueOf(mWakelockForSleep.isHeld()));
    	if (TaskManager.isAcqWakeLockRequired(mEnvParms)) {
    		if (!mWakelockForSleep.isHeld()) mWakelockForSleep.acquire();
    	}
    	if (mEnvParms.settingDebugLevel>=2) 
    		mUtil.addDebugMsg(2, "I", "acqWakeLockForSleep Result=",String.valueOf(mWakelockForSleep.isHeld()));
    };
    
    static private void relWakeLockForSleep() {
    	if (mWakelockForSleep.isHeld()) mWakelockForSleep.release();
    	if (mEnvParms.settingDebugLevel>=2) 
    		mUtil.addDebugMsg(2, "I", "relWakeLockForSleep released");
    };
    static private void acqSvcWakeLock() {
//    	if (!mWakelockSvcProcess.isHeld()) mWakelockSvcProcess.acquire();
    };
    static private void relSvcWakeLock() {
//    	if (mWakelockSvcProcess.isHeld()) mWakelockSvcProcess.release();
    };
    
    @SuppressLint("NewApi")
	final static private void setHeartBeat(Context context) {
//		Intent iw = new Intent();
    	Intent iw = new Intent(context,SchedulerService.class);
		iw.setAction(BROADCAST_SERVICE_HEARTBEAT);
		long time=System.currentTimeMillis()+mEnvParms.settingHeartBeatIntervalTime;
//		PendingIntent piw = PendingIntent.getBroadcast(context, 0, iw,
//				PendingIntent.FLAG_UPDATE_CURRENT);
		PendingIntent piw = PendingIntent.getService(context, 0, iw,
				PendingIntent.FLAG_UPDATE_CURRENT);
	    AlarmManager amw = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
	    if (Build.VERSION.SDK_INT>=23) amw.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, piw);
	    else amw.set(AlarmManager.RTC_WAKEUP, time, piw);
 
//	    amw.setRepeating(AlarmManager.RTC_WAKEUP, time, mEnvParms.settingHeartBeatIntervalTime,piw);
    };
    
	final static private void cancelHeartBeat(Context context) {
//		Intent iw = new Intent();
    	Intent iw = new Intent(context,SchedulerService.class);
		iw.setAction(BROADCAST_SERVICE_HEARTBEAT);
//		PendingIntent piw = PendingIntent.getBroadcast(context, 0, iw,
//				PendingIntent.FLAG_UPDATE_CURRENT);
		PendingIntent piw = PendingIntent.getService(context, 0, iw,
				PendingIntent.FLAG_UPDATE_CURRENT);
	    AlarmManager amw = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
	    amw.cancel(piw);
    };

    static private void listInitSettingsParm() {
    	mUtil.addDebugMsg(1,"I","initSettingParms "+Build.MANUFACTURER + " - " + Build.MODEL);
    	mUtil.addDebugMsg(1,"I","General parameters");
  		mUtil.addDebugMsg(1,"I","   localRootDir=",mEnvParms.localRootDir);
  		mUtil.addDebugMsg(1,"I","   settingDebugLevel=",String.valueOf(mEnvParms.settingDebugLevel));
  		mUtil.addDebugMsg(1,"I","   settingLogMsgDir=",mEnvParms.settingLogMsgDir);
  		mUtil.addDebugMsg(1,"I","   settingLogOption=",String.valueOf(mEnvParms.settingLogOption));
  		mUtil.addDebugMsg(1,"I","   settingWakeLockAlways=",String.valueOf(mEnvParms.settingWakeLockOption));
  		mUtil.addDebugMsg(1,"I","   settingWakeLockProximitySensor=",String.valueOf(mEnvParms.settingWakeLockProximitySensor));
  		mUtil.addDebugMsg(1,"I","   settingHeartBeatIntervalTime=",String.valueOf(mEnvParms.settingHeartBeatIntervalTime));
  		mUtil.addDebugMsg(1,"I","   settingEnableScheduler=",String.valueOf(mEnvParms.settingEnableScheduler));
  		mUtil.addDebugMsg(1,"I","   settingBluetoothLEMinRssi=",String.valueOf(mEnvParms.settingBtLeMinRssi));
    };
    
    static private void listControlOption() {
    	mUtil.addDebugMsg(1,"I","Control options ");
  		mUtil.addDebugMsg(1,"I","   wifiOffScreenlocked=",mEnvParms.wifiOffScreenLocked);
    	mUtil.addDebugMsg(1,"I","   wifiOffScreenlockedTimeout="+mEnvParms.wifiOffScreenLockedTimeoutValue);
  		mUtil.addDebugMsg(1,"I","   wifiOnScreenUnlocked=",mEnvParms.wifiOnScreenUnlocked);
    	mUtil.addDebugMsg(1,"I","   wifiOffConnectionTimeout=",mEnvParms.wifiOffConnectionTimeout);
    	mUtil.addDebugMsg(1,"I","   wifiOffConnectionTimeoutValue="+mEnvParms.wifiConnectionTimeoutValue);
  		mUtil.addDebugMsg(1,"I","   bluetoothOffScreenlocked=",mEnvParms.bluetoothOffScreenLocked);
    	mUtil.addDebugMsg(1,"I","   bluetoothOffScreenlockedTimeout="+mEnvParms.bluetoothOffScreenLockedTimeoutValue);
    	mUtil.addDebugMsg(1,"I","   bluetoothOnScreenUnlocked=",mEnvParms.bluetoothOnScreenUnlocked);
    	mUtil.addDebugMsg(1,"I","   bluetoothOffConnectionTimeout=",mEnvParms.bluetoothOffConnectionTimeout);
    	mUtil.addDebugMsg(1,"I","   bluetoothOffConnectionTimeoutValue="+mEnvParms.bluetoothConnectionTimeoutValue);
    	mUtil.addDebugMsg(1,"I","   proximityDisabledWhenMultipleEvent="+mEnvParms.proximityDisabledWhenMultipleEvent);
    	mUtil.addDebugMsg(1,"I","   proximityUndetected=",mEnvParms.proximityUndetected);
    	mUtil.addDebugMsg(1,"I","   proximityDetected=",mEnvParms.proximityDetected);
    	mUtil.addDebugMsg(1,"I","   proximityScreenLockTimeValue="+mEnvParms.proximityScreenLockTimeValue);
  		mUtil.addDebugMsg(1,"I","   notifyScreenLockedVibrate=",String.valueOf(mEnvParms.notifyScreenLockedVibrate));
  		mUtil.addDebugMsg(1,"I","   notifyScreenLockedNotification=",String.valueOf(mEnvParms.notifyScreenLockedNotification));
  		mUtil.addDebugMsg(1,"I","   notifyScreenLockedNotificationTitle=",String.valueOf(mEnvParms.notifyScreenLockedNotificationTitle));
  		mUtil.addDebugMsg(1,"I","   notifyScreenLockedNotificationPath=",String.valueOf(mEnvParms.notifyScreenLockedNotificationPath));
  		mUtil.addDebugMsg(1,"I","   notifyScreenUnlockedVibrate=",String.valueOf(mEnvParms.notifyScreenUnlockedVibrate));
  		mUtil.addDebugMsg(1,"I","   notifyScreenUnlockedNotification=",String.valueOf(mEnvParms.notifyScreenUnlockedNotification));
  		mUtil.addDebugMsg(1,"I","   notifyScreenUnlockedNotificationTitle=",String.valueOf(mEnvParms.notifyScreenUnlockedNotificationTitle));
  		mUtil.addDebugMsg(1,"I","   notifyScreenUnlockedNotificationPath=",String.valueOf(mEnvParms.notifyScreenUnlockedNotificationPath));
  		mUtil.addDebugMsg(1,"I","   trustedDeviceReconnectWaitTime=",String.valueOf(mEnvParms.trustedDeviceReconnectWaitTime));
  		mUtil.addDebugMsg(1,"I","   trustedDeviceImmediateLockWhenDisconnected=",String.valueOf(mEnvParms.trustedDeviceImmediateLockWhenDisconnected));
    };
}