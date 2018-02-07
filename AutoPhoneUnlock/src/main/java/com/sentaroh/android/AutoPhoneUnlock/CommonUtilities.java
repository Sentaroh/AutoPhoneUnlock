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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.PowerManager;
import com.sentaroh.android.AutoPhoneUnlock.R;
import com.sentaroh.android.AutoPhoneUnlock.Log.LogUtil;
import com.sentaroh.android.Utilities.Base64Compat;

public final class CommonUtilities {
	private boolean DEBUG_ENABLE=true;

//	private SimpleDateFormat sdfTimeHHMM = new SimpleDateFormat("HH:mm");
	
	private Context mContext=null;

   	private EnvironmentParms envParms=null;
	
   	private LogUtil mLog=null;
   	
	public CommonUtilities(Context c, String li, EnvironmentParms ep, GlobalParameters gp) {
		mContext=c;// ContextはApplicationContext
		mLog=new LogUtil(c, li, gp);
		envParms=ep;
        if (envParms.settingDebugLevel==0) DEBUG_ENABLE=false;
        else DEBUG_ENABLE=true;
	}

	final public SharedPreferences getPrefMgr() {
    	return getPrefMgr(mContext);
    }

	@SuppressWarnings("deprecation")
	@SuppressLint("InlinedApi")
	final static public SharedPreferences getPrefMgr(Context c) {
    	return c.getSharedPreferences(DEFAULT_PREFS_FILENAME, Context.MODE_PRIVATE|Context.MODE_MULTI_PROCESS);
    }

	final public void setLogId(String li) {
		mLog.setLogId(li);
	};
	
	final public void resetLogReceiver() {
		mLog.resetLogReceiver();
	};

	final public void flushLog() {
		mLog.flushLog();
	};

	final public void rotateLogFile() {
		mLog.rotateLogFile();
	};

	final public void startScheduler() {
		if (DEBUG_ENABLE) addDebugMsg(2, "I", "startScheduler entered");
		startScheduler(mContext);
	};

	final static public void startScheduler(Context c) {
		Intent intent = new Intent(c, SchedulerService.class);
		intent.setAction(BROADCAST_START_SCHEDULER);
		c.startService(intent);
	};
	
	@SuppressWarnings("unused")
	final static public void removePassword(Context c) {
        DevicePolicyManager dpm = 
        		(DevicePolicyManager)c.getSystemService(Context.DEVICE_POLICY_SERVICE);
    	dpm.resetPassword("", DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
    	ComponentName darcn = new ComponentName(c, DevAdmReceiver.class);
    	boolean rc=false;
    	if (Build.VERSION.SDK_INT>=23) {
    		int mpl=dpm.getPasswordMinimumLength(darcn);
        	dpm.setPasswordMinimumLength(darcn, 0);
        	rc=dpm.resetPassword("", DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
        	dpm.setPasswordMinimumLength(darcn, mpl);
    	} else {
    		rc=dpm.resetPassword("", DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
    	}
//    	Log.v("","rc="+rc);
	};
	

	final public void resetScheduler() {
		if (DEBUG_ENABLE) addDebugMsg(2, "I", "resetScheduler entered");
		resetScheduler(mContext);
	};

	final static public void resetScheduler(Context c) {
		Intent intent = new Intent(BROADCAST_RESET_SCHEDULER);
		c.sendBroadcast(intent);
	};

	final public void reloadTrustDeviceList() {
		if (DEBUG_ENABLE) addDebugMsg(2, "I", "reloadTrustDeviceList entered");
		reloadTrustDeviceList(mContext);
	};

	final static public void reloadTrustDeviceList(Context c) {
		Intent intent = new Intent(BROADCAST_RELOAD_TRUST_DEVICE_LIST);
		c.sendBroadcast(intent);
	};

//	final public void stopScheduler() {
//		if (DEBUG_ENABLE) addDebugMsg(2, "I", "stopScheduler entered");
//		stopScheduler(mContext);
//	};

//	final static public void stopScheduler(Context c) {
//		Intent intent = new Intent(BROADCAST_STOP_SCHEDULER);
//		c.sendBroadcast(intent);
//	};

	final public void restartScheduler() {
		if (DEBUG_ENABLE) addDebugMsg(2, "I", "restartScheduler entered");
		restartScheduler(mContext);
	};

	final static public void restartScheduler(Context c) {
		Intent intent = new Intent(BROADCAST_RESTART_SCHEDULER);
		c.sendBroadcast(intent);
//		Thread.dumpStack();
	};
	
	final public static String getWifiSsidName(WifiManager wm) {
		String wssid="";
		if (wm.isWifiEnabled()) {
			String tssid=wm.getConnectionInfo().getSSID();
			if (tssid==null || tssid.equals("<unknown ssid>")) wssid="";
			else wssid=tssid.replaceAll("\"", "");
			if (wssid.equals("0x")) wssid="";
		}
		return wssid;
	};

    final public static boolean isNetworkConnected(Context context){
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if( ni != null ){
            return cm.getActiveNetworkInfo().isConnected();
        }
        return false;
    };

//	final public void clearSavedWifiSsidName() {
//		setSavedWifiSsidName(mContext,"");
//	};
//	final public String getSavedWifiSsidName() {
//		return getPrefMgr(mContext).getString(PREFS_WIFI_CONNECTED_DEVICE_NAME_KEY, "");
//	};
//	final static public void clearSavedWifiSsidName(Context c) {
//		setSavedWifiSsidName(c,"");
//	};
//	final public void setSavedWifiSsidName(String dev) {
//    	getPrefMgr(mContext).edit().putString(PREFS_WIFI_CONNECTED_DEVICE_NAME_KEY, dev).commit();
//	};
//	final static public void setSavedWifiSsidName(Context c, String dev) {
//    	getPrefMgr(c).edit().putString(PREFS_WIFI_CONNECTED_DEVICE_NAME_KEY, dev).commit();
//	};
//
//	final public void clearSavedWifiSsidAddr() {
//		setSavedWifiSsidAddr(mContext,"");
//	};
//	final public String getSavedWifiSsidAddr() {
//		return getPrefMgr(mContext).getString(PREFS_WIFI_CONNECTED_DEVICE_ADDR_KEY, "");
//	};
//	final static public void clearSavedWifiSsidAddr(Context c) {
//		setSavedWifiSsidAddr(c,"");
//	};
//	final public void setSavedWifiSsidAddr(String dev) {
//    	getPrefMgr(mContext).edit().putString(PREFS_WIFI_CONNECTED_DEVICE_ADDR_KEY, dev).commit();
//	};
//	final static public void setSavedWifiSsidAddr(Context c, String dev) {
//    	getPrefMgr(c).edit().putString(PREFS_WIFI_CONNECTED_DEVICE_ADDR_KEY, dev).commit();
//	};

	
	final public Sensor isProximitySensorAvailable() {
    	Sensor result=null;
        SensorManager sm = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensors = sm.getSensorList(Sensor.TYPE_PROXIMITY);
        for(Sensor sensor: sensors) {
           	if (DEBUG_ENABLE) addDebugMsg(2,"I", "Proximity sensor list size="+sensors.size()+
           			", type="+sensor.getType()+", vendor="+sensor.getVendor()+
        			", ver="+sensor.getVersion());
        	if (sensor.getType()==Sensor.TYPE_PROXIMITY) {
	            result=sensor;
//	            break;
            }
        }
        if (result!=null) {
        	if (DEBUG_ENABLE) addDebugMsg(2,"I", "Proximity sensor is available, name="+result.getName()+", vendor="+result.getVendor()+", version="+result.getVersion());
        } else {
        	if (DEBUG_ENABLE) addDebugMsg(2,"I", "Proximity sensor is not available");
        }
        return result;
    };

    final public Sensor isAccelerometerSensorAvailable() {
    	Sensor result=null;
	    final SensorManager sm = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensors_list = sm.getSensorList(Sensor.TYPE_ACCELEROMETER);
        for(Sensor sensor: sensors_list) {
           	if (DEBUG_ENABLE) addDebugMsg(2,"I", "Accelerometer sensor list size="+sensors_list.size()+
           			", type="+sensor.getType()+", vendor="+sensor.getVendor()+
        			", ver="+sensor.getVersion());
        	if (sensor.getType()==Sensor.TYPE_ACCELEROMETER) {
        		if (DEBUG_ENABLE) addDebugMsg(2, "I", "Accelerometer sensor is available, name="+sensor.getName()+", vendor="+sensor.getVendor()+", version="+sensor.getVersion());
        		result=sensor;
            }
        }
        return result;
	};

    final public Sensor isLightSensorAvailable() {
    	Sensor result=null;
	    final SensorManager sm = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensors_list = sm.getSensorList(Sensor.TYPE_LIGHT);
        for(Sensor sensor: sensors_list) {
           	if (DEBUG_ENABLE) addDebugMsg(2,"I", "Light sensor list size="+sensors_list.size()+
           			", type="+sensor.getType()+", vendor="+sensor.getVendor()+
        			", ver="+sensor.getVersion());
        	if (sensor.getType()==Sensor.TYPE_LIGHT) {
        		if (DEBUG_ENABLE) addDebugMsg(2, "I", "Light sensor is available, name="+sensor.getName()+", vendor="+sensor.getVendor()+", version="+sensor.getVersion());
        		result=sensor;
            }
        }
        return result;
	};
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	final static public boolean isScreenOn(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
	    if (Build.VERSION.SDK_INT >= 20) {
	        return pm.isInteractive();
	    } else {
	        return pm.isScreenOn();
	    }
	}

	final public Sensor isMagneticFieldSensorAvailable() {
    	Sensor result=null;
	    final SensorManager sm = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensor_list = sm.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
        for(Sensor sensor: sensor_list) {
           	if (DEBUG_ENABLE) addDebugMsg(2,"I", "Magnetic-field sensor list size="+sensor_list.size()+
           			", type="+sensor.getType()+", vendor="+sensor.getVendor()+
        			", ver="+sensor.getVersion());
        	if (sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD) {
        		if (DEBUG_ENABLE) addDebugMsg(2, "I", "Magnetic-field sensor is available, name="+sensor.getName()+", vendor="+sensor.getVendor()+", version="+sensor.getVersion());
        		result=sensor;
            }
        }
        return result;
	};

	final public boolean screenLockNow() {
		boolean result=false;
        DevicePolicyManager dpm = 
        		(DevicePolicyManager)mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName darcn = new ComponentName(mContext, DevAdmReceiver.class);
        if (dpm.isAdminActive(darcn)) {
        	dpm.lockNow();
        	result=true;
        } else result=false;
        return result;
	};
	
	@SuppressWarnings("deprecation")
	final public boolean isTelephonyAvailable() {
		boolean result=false;
      	ConnectivityManager cm = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] ni_array=cm.getAllNetworkInfo();
        if( ni_array != null ){
         	for (int i=0;i<ni_array.length;i++) {
         		if (ni_array[i].getType()==ConnectivityManager.TYPE_MOBILE||
         				ni_array[i].getType()==ConnectivityManager.TYPE_MOBILE_DUN||
         				ni_array[i].getType()==ConnectivityManager.TYPE_MOBILE_HIPRI||
         				ni_array[i].getType()==ConnectivityManager.TYPE_MOBILE_MMS||
         				ni_array[i].getType()==ConnectivityManager.TYPE_MOBILE_SUPL) {
         			result=true;
         			break;
         		}
        	}
        }
        return result;
	};
	
	final public boolean isDevicePolicyManagerActive() {
    	boolean result=isDevicePolicyManagerActive(mContext);
    	if (DEBUG_ENABLE) addDebugMsg(2, "I", "isDevicePolicyManagerActive result="+result);
    	return result;
	};

	final public static boolean isDevicePolicyManagerActive(Context c) {
        DevicePolicyManager dpm = 
        		(DevicePolicyManager)c.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName darcn = new ComponentName(c, DevAdmReceiver.class);
    	boolean result=dpm.isAdminActive(darcn);
    	return result;
	};

	final public boolean isKeyguardEffective() {
    	boolean result=isKeyguardEffective(mContext);
    	if (DEBUG_ENABLE) addDebugMsg(2, "I", "isKeyguardEffective result="+result);
    	return result;
    };

	@SuppressLint("NewApi")
	static final public boolean isKeyguardEffective(Context c) {
        KeyguardManager keyguardMgr=(KeyguardManager)c.getSystemService(Context.KEYGUARD_SERVICE);
    	boolean result=false;
		if (Build.VERSION.SDK_INT>=16) {
			result=keyguardMgr.isKeyguardLocked();			
		} else {
			result=keyguardMgr.inKeyguardRestrictedInputMode();
		}

    	return result;
    };

    final public static ArrayList<TrustItem> loadTrustedDeviceTable(Context c, EnvironmentParms ep) {
    	ArrayList<TrustItem> tl=new ArrayList<TrustItem>();
    	if (Build.VERSION.SDK_INT==17 && !ep.settingForceUseTrustDevice) return tl; 
    	SharedPreferences prefs=getPrefMgr(c);
    	String tl_data=prefs.getString(TRUSTED_DEVICE_TABLE_KEY, "");
    	if (tl_data!=null && !tl_data.equals("")) {
    		String[] tl_data_array=tl_data.split("\n");
    		for(int i=0;i<tl_data_array.length;i++) {
    			String[] tl_item_array=tl_data_array[i].split("\t");
    			TrustItem tl_item=new TrustItem();
    			if (tl_item_array.length>=3 && tl_item_array[0]!=null && 
    					(tl_item_array[0].equals("0") || tl_item_array[0].equals("1") 
    							|| tl_item_array[0].equals("2")) ) {
        			tl_item.trustItemType=Integer.parseInt(tl_item_array[0]);
        			if (tl_item_array[1]!=null) tl_item.trustDeviceName=tl_item_array[1];
        			if (tl_item_array[2]!=null) tl_item.trustDeviceAddr=tl_item_array[2];
        			if (tl_item_array.length>=4 && tl_item_array[3]!=null) {
        				if (tl_item_array[3].equals("0")) tl_item.setEnabled(false);
        				else tl_item.setEnabled(true);
        			}
        			if (tl_item_array.length>=5 && tl_item_array[4]!=null) {
        				tl_item.trustItemName=tl_item_array[4];
        			}
        			if (tl_item_array.length>=6 && tl_item_array[5]!=null) {
        				if (tl_item_array[5].equals("0")) tl_item.isBtLeDeviceConnectMode=false;
        				else tl_item.isBtLeDeviceConnectMode=true;
        			}
        			if (tl_item_array.length>=7 && tl_item_array[6]!=null) {
        				tl_item.bleDeviceLinkLossActionToTag=Integer.parseInt(tl_item_array[6]);
        			}
        			if (tl_item_array.length>=8 && tl_item_array[7]!=null) {
        				tl_item.bleDeviceLinkLossActionToHost=Integer.parseInt(tl_item_array[7]);
        			}
        			if (tl_item_array.length>=9 && tl_item_array[8]!=null) {
        				tl_item.bleDeviceNotifyButtonAction=Integer.parseInt(tl_item_array[8]);
        			}
        			tl.add(tl_item);
//        			Log.v(APPLICATION_TAG,"Load trust list, type="+tl_item.trustItemType+
//        					", name="+tl_item.trustDeviceName+", addr="+tl_item.trustDeviceAddr+
//        					", LinkLossTag="+tl.get(i).bleDeviceLinkLossActionToTag+
//        					", LinkLossHost="+tl.get(i).bleDeviceLinkLossActionToHost+
//        					", Button="+tl.get(i).bleDeviceNotifyButtonAction);
    			}
    		}
    		TrustItemListAdapter.sort(tl);
    	}
    	return tl;
    };

    final public static void saveTrustedDeviceTable(Context c, ArrayList<TrustItem> tl) {
    	SharedPreferences prefs=getPrefMgr(c);
    	String tl_data="";
    	String sep="";
    	for(int i=0;i<tl.size();i++) {
    		String act="0", dt="0";
    		if (tl.get(i).isEnabled()) act="1";
    		if (tl.get(i).isBtLeDeviceConnectMode) dt="1";
    		String tl_item=tl.get(i).trustItemType+"\t"+tl.get(i).trustDeviceName+
    				"\t"+tl.get(i).trustDeviceAddr+"\t"+act+"\t"+tl.get(i).trustItemName+"\t"+
    				dt+"\t"
    				+tl.get(i).bleDeviceLinkLossActionToTag+"\t"
    				+tl.get(i).bleDeviceLinkLossActionToHost+"\t"
    				+tl.get(i).bleDeviceNotifyButtonAction
    				;
    		tl_data+=sep+tl_item;
    		sep="\n";
//			Log.v(APPLICATION_TAG,"Save trust list, type="+tl.get(i).trustItemType+
//					", name="+tl.get(i).trustDeviceName+", addr="+tl.get(i).trustDeviceAddr+
//					", LinkLossTag="+tl.get(i).bleDeviceLinkLossActionToTag+
//					", LinkLossHost="+tl.get(i).bleDeviceLinkLossActionToHost+
//					", Button="+tl.get(i).bleDeviceNotifyButtonAction);
    	}
    	prefs.edit().putString(TRUSTED_DEVICE_TABLE_KEY, tl_data).commit();
    };

    final public void deleteLogFile() {
    	mLog.deleteLogFile();
	};

	final public void addLogMsg(String cat, String... msg) {
		mLog.addLogMsg(cat, msg);
	};
	final public void addDebugMsg(int lvl, String cat, String... msg) {
		mLog.addDebugMsg(lvl, cat, msg);
	};

	final public boolean isLogFileExists() {
		boolean result = false;
		result=mLog.isLogFileExists();
		if (envParms.settingDebugLevel>=3) addDebugMsg(3,"I","Log file exists="+result);
		return result;
	};

	final public boolean getSettingsLogOption() {
		boolean result = false;
		result=getPrefMgr().getBoolean(mContext.getString(R.string.settings_main_log_option), false);
		if (envParms.settingDebugLevel>=2) addDebugMsg(2,"I","LogOption="+result);
		return result;
	};

	final public boolean setSettingsLogOption(boolean enabled) {
		boolean result = false;
		getPrefMgr().edit().putBoolean(mContext.getString(R.string.settings_main_log_option), enabled).commit();
		if (envParms.settingDebugLevel>=2) addDebugMsg(2,"I","setLLogOption="+result);
		return result;
	};

	final public String getLogFilePath() {
		return mLog.getLogFilePath();
	};
	
	private static void saveSettingsParmsToFileString(Context c, String group, PrintWriter pw, String dflt,
			String key) {
		SharedPreferences prefs = getPrefMgr(c);
		String k_type, k_val;

		k_val=prefs.getString(key, dflt);
		String enc = Base64Compat.encodeToString(
				k_val.getBytes(), 
				Base64Compat.NO_WRAP);
		k_type=SETTING_PARMS_SAVE_STRING;
		String k_str=group+"\t"+
				SETTING_PARMS_SAVE_KEY+"\t"+key+"\t"+k_type+"\t"+enc;

		pw.println(k_str);
	};
	
	@SuppressWarnings("unused")
	static private void saveSettingsParmsToFileInt(Context c, String group, PrintWriter pw, int dflt,
			String key) {
		SharedPreferences prefs = getPrefMgr(c);
		String k_type;
		int k_val;

		k_val=prefs.getInt(key, dflt);
		k_type=SETTING_PARMS_SAVE_INT;
		String k_str=group+"\t"+
				SETTING_PARMS_SAVE_KEY+"\t"+key+"\t"+k_type+"\t"+k_val;
		pw.println(k_str);
	};

	@SuppressWarnings("unused")
	static private void saveSettingsParmsToFileLong(Context c, String group, PrintWriter pw, long dflt,
			String key) {
		SharedPreferences prefs = getPrefMgr(c);
		String k_type;
		long k_val;

		k_val=prefs.getLong(key, dflt);
		k_type=SETTING_PARMS_SAVE_LONG;
		String k_str=group+"\t"+
				SETTING_PARMS_SAVE_KEY+"\t"+key+"\t"+k_type+"\t"+k_val;
		pw.println(k_str);
	};
	
	static private void saveSettingsParmsToFileBoolean(Context c, String group, PrintWriter pw, boolean dflt,
			String key) {
		SharedPreferences prefs = getPrefMgr(c);
		String k_type;
		boolean k_val;

		k_val=prefs.getBoolean(key, dflt);
		k_type=SETTING_PARMS_SAVE_BOOLEAN;
		String k_str=group+"\t"+
				SETTING_PARMS_SAVE_KEY+"\t"+key+"\t"+k_type+"\t"+k_val;
		pw.println(k_str);
	};
	
	static public ArrayList<PreferenceParmListIItem> loadSettingsParmFromFile(Context c, String dir, String fn) {
		ArrayList<PreferenceParmListIItem>ispl=new ArrayList<PreferenceParmListIItem>();
		File lf=new File(dir+"/"+fn);
		if (lf.exists()) {
			BufferedReader br;
			try {
				Editor prefs = getPrefMgr(c).edit();
				br = new BufferedReader(new FileReader(lf),8192);
				String pl;
				while ((pl = br.readLine()) != null) {
					String tmp_ps=pl.substring(7,pl.length());
					String[] tmp_pl=tmp_ps.split("\t");// {"type","name","active",options...};
					if (tmp_pl[1]!=null && tmp_pl.length>=5 && tmp_pl[1].equals(SETTING_PARMS_SAVE_KEY)) {
//						String[] val = new String[]{parm[2],parm[3],parm[4]};
						PreferenceParmListIItem ppli=new PreferenceParmListIItem();
						if (tmp_pl[2]!=null) ppli.parms_key=tmp_pl[2];
						if (tmp_pl[3]!=null) ppli.parms_type=tmp_pl[3];
						if (tmp_pl[4]!=null) {
							if (ppli.parms_type.equals(SETTING_PARMS_SAVE_STRING)) {
								byte[] enc_array=Base64Compat.decode(tmp_pl[4], Base64Compat.NO_WRAP);
								ppli.parms_value=new String(enc_array);
							} else {
								ppli.parms_value=tmp_pl[4];
							}
						}
						if (!ppli.parms_key.equals("") && !ppli.parms_type.equals("")) {
//							Log.v("","key="+tmp_pl[2]+", value="+ppli.parms_value+", type="+tmp_pl[3]);
//							Log.v("","key="+ppli.parms_key+", value="+ppli.parms_value+", type="+ppli.parms_type);
							ispl.add(ppli);
							if (ppli.parms_type.equals(SETTING_PARMS_SAVE_STRING)) {
								prefs.putString(ppli.parms_key, ppli.parms_value).commit();
							} else if (ppli.parms_type.equals(SETTING_PARMS_SAVE_LONG)) {
								prefs.putLong(ppli.parms_key, Long.parseLong(ppli.parms_value)).commit();
							} else if (ppli.parms_type.equals(SETTING_PARMS_SAVE_INT)) {
								prefs.putInt(ppli.parms_key, Integer.parseInt(ppli.parms_value)).commit();
							} else if (ppli.parms_type.equals(SETTING_PARMS_SAVE_BOOLEAN)) {
								if (ppli.parms_value.equals("true")) prefs.putBoolean(ppli.parms_key, true).commit();
								else prefs.putBoolean(ppli.parms_key, false).commit();
							}
						}
					}
				}
				br.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			ispl=null;
		}
		return ispl;
	};

	static public long getSettingsParmSaveDate(Context c, String dir, String fn) {
		File lf=new File(dir+"/"+fn);
		long result=0;
		if (lf.exists()) {
			result=lf.lastModified();
		} else {
			result=-1;
		}
		return result;
	};
	
	public static void saveSettingsParmsToFile(Context c, String dir, String fn) {
		File df=new File(dir);
		if (!df.exists()) df.mkdirs();
		
		File lf=new File(dir+"/"+fn);
		try {
			PrintWriter pw=new PrintWriter(lf);
			
			saveSettingsParmsToFileBoolean(c, "DEFAULT", pw, true, SETTING_ENABLE_SCHEDULER_KEY);
			saveSettingsParmsToFileString(c, "DEFAULT", pw, "", BLUETOOTH_CONNECTED_DEVICE_LIST_KEY);
			saveSettingsParmsToFileString(c, "DEFAULT", pw, "", TRUSTED_DEVICE_TABLE_KEY);
			saveSettingsParmsToFileBoolean(c, "DEFAULT", pw, true, c.getString(R.string.settings_main_device_admin));
			saveSettingsParmsToFileString(c, "DEFAULT", pw, "0", c.getString(R.string.settings_main_scheduler_sleep_wake_lock_option));
			saveSettingsParmsToFileBoolean(c, "DEFAULT", pw, false, c.getString(R.string.settings_main_scheduler_sleep_wake_lock_proximity_sensor));
			saveSettingsParmsToFileBoolean(c, "DEFAULT", pw, false, c.getString(R.string.settings_main_use_light_theme));
			saveSettingsParmsToFileBoolean(c, "DEFAULT", pw, false, c.getString(R.string.settings_main_log_option));
			saveSettingsParmsToFileString(c, "DEFAULT", pw, "10", c.getString(R.string.settings_main_log_file_max_count));
			saveSettingsParmsToFileString(c, "DEFAULT", pw, "0", c.getString(R.string.settings_main_log_level));
			saveSettingsParmsToFileBoolean(c, "DEFAULT", pw, false, c.getString(R.string.settings_main_exit_clean));
			
			saveSettingsParmsToFileString(c, "DEFAULT", pw, "0", WIFI_ON_WHEN_SCREEN_UNLOCKED_KEY);
			saveSettingsParmsToFileString(c, "DEFAULT", pw, "0", WIFI_OFF_WHEN_CONNECT_TIMEOUT_KEY);
			saveSettingsParmsToFileString(c, "DEFAULT", pw, "0", WIFI_OFF_WHEN_CONNECT_TIMEOUT_VALUE_KEY);
			saveSettingsParmsToFileString(c, "DEFAULT", pw, "0", WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_KEY);
			saveSettingsParmsToFileString(c, "DEFAULT", pw, "0", WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_VALUE_KEY);
			
			saveSettingsParmsToFileString(c, "DEFAULT", pw, "0", BLUETOOTH_ON_WHEN_SCREEN_UNLOCKED_KEY);
			saveSettingsParmsToFileString(c, "DEFAULT", pw, "0", BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_KEY);
			saveSettingsParmsToFileString(c, "DEFAULT", pw, "0", BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_VALUE_KEY);
			saveSettingsParmsToFileString(c, "DEFAULT", pw, "0", BLUETOOTH_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_KEY);
			saveSettingsParmsToFileString(c, "DEFAULT", pw, "0", BLUETOOTH_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_VALUE_KEY);
			
			saveSettingsParmsToFileString(c, "DEFAULT", pw, "0", PROXIMITY_DISABLED_WWHEN_MULTIPLE_EVENT_KEY);
			saveSettingsParmsToFileString(c, "DEFAULT", pw, "0", PROXIMITY_UNDETECTED_KEY);
			saveSettingsParmsToFileString(c, "DEFAULT", pw, "0", PROXIMITY_DETECTED_KEY);
			saveSettingsParmsToFileString(c, "DEFAULT", pw, "0", PROXIMITY_DETECTED_TIMEOUT_VALUE_KEY);
			saveSettingsParmsToFileBoolean(c, "DEFAULT", pw, false, NOTIFY_WHEN_SCREEN_LOCKED_VIBRATE_KEY);
			saveSettingsParmsToFileString(c, "DEFAULT", pw, NOTIFY_WHEN_SCREEN_LOCKED_VIBRATE_PATTERN_VALUE1,
					NOTIFY_WHEN_SCREEN_LOCKED_VIBRATE_PATTERN_KEY);
			saveSettingsParmsToFileBoolean(c, "DEFAULT", pw, false, NOTIFY_WHEN_SCREEN_LOCKED_NOTIFICATION_KEY);
			saveSettingsParmsToFileString(c, "DEFAULT", pw, "0", NOTIFY_WHEN_SCREEN_LOCKED_NOTIFICATION_TITLE_KEY);
			saveSettingsParmsToFileString(c, "DEFAULT", pw, "0", NOTIFY_WHEN_SCREEN_LOCKED_NOTIFICATION_PATH_KEY);
			
			saveSettingsParmsToFileBoolean(c, "DEFAULT", pw, false, NOTIFY_WHEN_SCREEN_UNLOCKED_VIBRATE_KEY);
			saveSettingsParmsToFileString(c, "DEFAULT", pw, NOTIFY_WHEN_SCREEN_UNLOCKED_VIBRATE_PATTERN_VALUE1,
					NOTIFY_WHEN_SCREEN_UNLOCKED_VIBRATE_PATTERN_KEY);
			saveSettingsParmsToFileBoolean(c, "DEFAULT", pw, false, NOTIFY_WHEN_SCREEN_UNLOCKED_NOTIFICATION_KEY);
			saveSettingsParmsToFileString(c, "DEFAULT", pw, "0", NOTIFY_WHEN_SCREEN_UNLOCKED_NOTIFICATION_TITLE_KEY);
			saveSettingsParmsToFileString(c, "DEFAULT", pw, "0", NOTIFY_WHEN_SCREEN_UNLOCKED_NOTIFICATION_PATH_KEY);

			saveSettingsParmsToFileString(c, "DEFAULT", pw, "0", TRUST_DEVICE_DELAY_TIME_VLAUE_KEY);

			saveSettingsParmsToFileString(c, "DEFAULT", pw, "0", TRUST_DEVICE_IMMEDIATE_LOCK_WHEN_TRUSTED_DEVICE_DISCONN_KEY);

			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	};

	public static void serilizeTrustItemList(ObjectOutput out, ArrayList<TrustItem>tl) {
		int lsz=-1;
		if (tl!=null) {
			if (tl.size()!=0) lsz=tl.size();
			else lsz=0;
		}
		try {
			out.writeInt(lsz);
			if (lsz>0) {
				for (int i=0;i<lsz;i++) {
					tl.get(i).writeExternal(out);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	};
	
	public static ArrayList<TrustItem> deSerilizeTrustItemList(ObjectInputStream ois) {
		ArrayList<TrustItem>t_list=new ArrayList<TrustItem>();
		try {
			int lsz = ois.readInt();
			if (lsz!=-1) {
				for (int i=0;i<lsz;i++) {
					TrustItem tai=new TrustItem();
					tai.readExternal(ois);
					t_list.add(tai);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return t_list;
	};

}

class PreferenceParmListIItem {
	public String parms_key="";
	public String parms_type="";
	public String parms_value="";
}
