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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothHealth;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Configuration;
import android.location.Location;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.sentaroh.android.AutoPhoneUnlock.R;
import com.sentaroh.android.Utilities.Base64Compat;
import com.sentaroh.android.Utilities.EncryptUtil;
import com.sentaroh.android.Utilities.LocalMountPoint;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.EncryptUtil.CipherParms;

@SuppressWarnings("deprecation")
public class EnvironmentParms implements Serializable {
	private static final long serialVersionUID = SERIALIZABLE_NUMBER;
	
	public String deviceManufacturer="";
	public String deviceModel="";
	
//	Settings.System
	public int airplane_mode_on=0;
	public boolean isAirplaneModeOn() {
		return airplane_mode_on==1?true:false;
	}
//	Telephony
	public int telephonyStatus=0;
	public final int TELEPHONY_CALL_STATE_IDLE=TelephonyManager.CALL_STATE_IDLE;
	public final int TELEPHONY_CALL_STATE_OFFHOOK=TelephonyManager.CALL_STATE_OFFHOOK;
	public final int TELEPHONY_CALL_STATE_RINGING=TelephonyManager.CALL_STATE_RINGING;
	final public boolean isTelephonyCallStateIdle() {
		return telephonyStatus==TelephonyManager.CALL_STATE_IDLE?true:false;
	}
	final public boolean isTelephonyCallStateOffhook() {
		return telephonyStatus==TelephonyManager.CALL_STATE_OFFHOOK?true:false;
	}
	final public boolean isTelephonyCallStateRinging() {
		return telephonyStatus==TelephonyManager.CALL_STATE_RINGING?true:false;
	}
//	Sensor	    	
	public boolean proximitySensorAvailable=false;
	public boolean proximitySensorActive=false;
	public boolean proximitySensorEventTemporaryIgnore=false;
	
	public int    proximitySensorValue=-1;
	final public boolean isProximitySensorDetected() {
		return proximitySensorValue==0 ? true:false;
	}
//		Battery	    	
	public int    batteryLevel=-1;
	public String batteryPowerSource="";
	public String batteryChargeStatusString="";
	public int batteryChargeStatusInt=0;
	public final int BATTERY_CHARGE_STATUS_INT_FULL=3;
	public final int BATTERY_CHARGE_STATUS_INT_CHARGING=1;
	public final int BATTERY_CHARGE_STATUS_INT_DISCHARGING=2;
	public boolean isBatteryCharging() {
		return batteryPowerSource.equals(CURRENT_POWER_SOURCE_AC)?true:false;
	}
//		WiFi	    	
	public String  wifiSsidName="";
	public String  wifiSsidAddr="";
//	public final static String WIFI_DIRECT_SSID="*WIFI-DIRECT";
	public boolean wifiIsConnected=false;
	public boolean wifiIsActive=false;
	final public boolean isWifiActive() {return wifiIsActive;}
	final public void setWifiActive(boolean p) {wifiIsActive=p;}
	final public boolean isWifiConnected() {
		return wifiIsConnected;
	}
//		Bluetooth	    	
	public boolean bluetoothIsActive=false;
	public boolean bluetoothIsAvailable=false;
	public boolean isBluetoothLeSupported=false;
	public String bluetoothLastEventDeviceName="", bluetoothLastEventDeviceAddr="";
	public int bluetoothLastEventDeviceType=TrustItem.TYPE_BLUETOOTH_CLASSIC_DEVICE;
	private ArrayList<TrustItem> bluetoothConnectedDeviceList=new ArrayList<TrustItem>();
	final public void addBluetoothConnectedDevice(Context c, int type, String name, String addr,
			boolean immed_alert, boolean conn_mode) {
		addBluetoothConnectedDevice(c, bluetoothConnectedDeviceList, type, name, addr, immed_alert, conn_mode);
	};

	final public void addBluetoothConnectedDevice(Context c, ArrayList<TrustItem> list, 
			int type, String t_name, String t_addr, boolean immed_alert, boolean conn_mode) {
		final String name=t_name==null?UNKNOWN_LE_DEVICE_NAME:t_name;
		final String addr=t_addr==null?UNKNOWN_LE_DEVICE_ADDR:t_addr;

		TrustItem c_bcd=getBluetoothConnectedDevice(type,name, addr);
		if (c_bcd==null) {
			TrustItem bcd=new TrustItem();
			bcd.trustItemType=type;
			bcd.trustDeviceName=name;
			bcd.trustDeviceAddr=addr;
			bcd.hasImmedAlert=immed_alert;
			bcd.isBtLeDeviceConnectMode=conn_mode;
			list.add(bcd);
			TrustItem t_item=getTrustItem(type,name,addr);
			if (t_item!=null) {
				t_item.setConnected(true);
				t_item.hasImmedAlert=immed_alert;
				t_item.bleDeviceErrorMsg="";
			}
		}
		
    	if (Build.VERSION.SDK_INT<=10) 
    		putSavedBluetoothConnectedDevice(c,getBluetoothConnectedDeviceList());
	};

	final public TrustItem getBluetoothConnectedDevice(int type, String name, String addr) {
		return getBluetoothConnectedDevice(bluetoothConnectedDeviceList, type, name, addr);
	};
	
	final public TrustItem getBluetoothConnectedDevice(ArrayList<TrustItem> list,
			int type, String t_name, String t_addr) {
		final String name=t_name==null?UNKNOWN_LE_DEVICE_NAME:t_name;
		final String addr=t_addr==null?UNKNOWN_LE_DEVICE_ADDR:t_addr;

		for(int i=0;i<list.size();i++) {
			TrustItem bcd=list.get(i);
			if (bcd.trustItemType==type && bcd.trustDeviceName.equals(name)) {
				if (!addr.equals("")) {
					if (bcd.trustDeviceAddr.equalsIgnoreCase(addr)) {
						return bcd;
					}
				} else {
					return bcd;
				}
			}
		}
		return null;
	};
	
	@SuppressLint("NewApi")
	final public void removeBluetoothConnectedDevice(Context c, int type, String t_name, String t_addr) {
		final String name=t_name==null?UNKNOWN_LE_DEVICE_NAME:t_name;
		final String addr=t_addr==null?UNKNOWN_LE_DEVICE_ADDR:t_addr;
		ArrayList<TrustItem>dl=new ArrayList<TrustItem>();
		for(int i=0;i<bluetoothConnectedDeviceList.size();i++) {
			TrustItem bcd=bluetoothConnectedDeviceList.get(i);
			if (bcd.trustItemType==type && bcd.trustDeviceName.equals(name)) {
				if (addr.equals("")) dl.add(bcd);
				else {
					if (bcd.trustDeviceAddr.equalsIgnoreCase(addr)) {
						dl.add(bcd);
					}
				}
			}
		}
//		Log.v("","remove name="+name+", addr="+addr);
		for(int i=0;i<dl.size();i++) {
//			Log.v("","remove item name="+dl.get(i).btName+", addr="+dl.get(i).btAddr);
			bluetoothConnectedDeviceList.remove(dl.get(i));
			TrustItem t_item=getTrustItem(dl.get(i).trustItemType, dl.get(i).trustDeviceName, dl.get(i).trustDeviceAddr);
			if (t_item!=null) t_item.setConnected(false);
		}
		if (Build.VERSION.SDK_INT<=10) 
			putSavedBluetoothConnectedDevice(c,getBluetoothConnectedDeviceList());
	};
	final public ArrayList<TrustItem> getBluetoothConnectedDeviceList() {
		return bluetoothConnectedDeviceList;
	};
	final public void setBluetoothConnectedDeviceList(ArrayList<TrustItem> bdcl) {
		bluetoothConnectedDeviceList=bdcl;
	};
	@SuppressLint("NewApi")
	final public void clearBluetoothConnectedDeviceList(Context c) {
		synchronized(bluetoothConnectedDeviceList) {
			bluetoothConnectedDeviceList=new ArrayList<TrustItem>();
			for(TrustItem t_item:trustItemList) t_item.setConnected(false);
			
			if (Build.VERSION.SDK_INT<=10) 
				putSavedBluetoothConnectedDevice(c, getBluetoothConnectedDeviceList());
		}
	};
	final public boolean isBluetoothActive() {return bluetoothIsActive;}
	final public boolean isBluetoothConnected() {
		return bluetoothConnectedDeviceList.size()>0 ? true:false;
	}
	
    public static final int BluetoothProfile_PAN = 5;
	private BluetoothHeadset mBluetoothProfHeadset=null;
	private BluetoothA2dp mBluetoothProfA2DP=null;
	private BluetoothHealth mBluetoothProfHealth=null;
	private BluetoothGatt mBluetoothProfGATT=null;
	private BluetoothProfile mBluetoothProfPAN=null;
	private boolean buildBluetoothDeviceListCompleted=false;
	@SuppressLint("NewApi")
	public void buildBluetoothConnectedDeviceList(final Context c, final NotifyEvent p_ntfy, 
			final CommonUtilities util) {
		if (Build.VERSION.SDK_INT<=10) {
			setBluetoothConnectedDeviceList(loadSavedBluetoothConnectedDevice(c));
			p_ntfy.notifyToListener(true, null);
			return;
		};

		buildBluetoothDeviceListCompleted=false;
    	final Handler hndl=new Handler();
		BluetoothProfile.ServiceListener mProfileListener = new BluetoothProfile.ServiceListener() {
			@SuppressWarnings("unchecked")
			@SuppressLint("NewApi")
			public void onServiceConnected(int profile, BluetoothProfile proxy) {
				List<BluetoothDevice> devices=null;
			    if (profile == BluetoothProfile.HEADSET) {
					util.addDebugMsg(1,"I","Bluetooth profile connected, type=HEADSET");
			        mBluetoothProfHeadset = (BluetoothHeadset) proxy;
			    	devices = mBluetoothProfHeadset.getConnectedDevices();
			    	addConnectedBluetoothDeviceByList(c, "HEADSET", devices, util);
			    } else if (profile == BluetoothProfile.A2DP) {
			    	util.addDebugMsg(1,"I","Bluetooth profile connected, type=A2DP");
			        mBluetoothProfA2DP = (BluetoothA2dp) proxy;
			    	devices = mBluetoothProfA2DP.getConnectedDevices();
			    	addConnectedBluetoothDeviceByList(c, "A2DP", devices, util);
			    } else if (profile == BluetoothProfile.HEALTH) {
			    	util.addDebugMsg(1,"I","Bluetooth profile connected, type=HEALTH");
			        mBluetoothProfHealth = (BluetoothHealth) proxy;
			    	devices = mBluetoothProfHealth.getConnectedDevices();
			    	addConnectedBluetoothDeviceByList(c, "HEALTH", devices, util);
			    } else if (profile == BluetoothProfile.GATT) {
			    	util.addDebugMsg(1,"I","Bluetooth profile connected, type=GATT");
			        mBluetoothProfGATT = (BluetoothGatt) proxy;
			    	devices = mBluetoothProfGATT.getConnectedDevices();
			    	addConnectedBluetoothDeviceByList(c, "GATT", devices, util);
			    } else if (profile == BluetoothProfile_PAN) {
			    	util.addDebugMsg(1,"I","Bluetooth profile connected, type=PAN");
			    	mBluetoothProfPAN=proxy;
		  	    	Method[] m=mBluetoothProfPAN.getClass().getMethods();
		  	    	Method pan_method_getConnectedDevices = null;
		  	    	for (int i=0;i<m.length;i++) {
//		  	    		if (m[i].getName().equals("connect")) { 
//				    	} else if (m[i].getName().equals("disconnect")) {
//				    	} else if (m[i].getName().equals("getConnectionState")) {
//				    	} else if (m[i].getName().equals("isTetheringOn")) {
//				    	} else if (m[i].getName().equals("setBluetoothTethering")) {
				    	if (m[i].getName().equals("getConnectedDevices")) {
				    		pan_method_getConnectedDevices=m[i];
				    	}
		  	    	}
					try {
						if (pan_method_getConnectedDevices!=null)
							devices = (List<BluetoothDevice>) pan_method_getConnectedDevices.invoke(mBluetoothProfPAN);
						addConnectedBluetoothDeviceByList(c, "PAN", devices, util);
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}                        
			    } else {
					util.addDebugMsg(1,"I","Bluetooth profile connected, type="+profile);
			    }
				if (mBluetoothProfHeadset!=null && mBluetoothProfA2DP!=null 
						&& mBluetoothProfHealth!=null
//						&& mBluetoothProfGATT!=null
						&& !buildBluetoothDeviceListCompleted) {
					//mBluetoothProfGATT=null;
					//mBluetoothProfPAN=null;
					boolean init_comp=false;
			    	if (Build.VERSION.SDK_INT>=16) {
			        	if (mBluetoothProfPAN!=null)init_comp=true; 
			    	} else init_comp=true;
			    	if (init_comp) {
			    		buildBluetoothDeviceListCompleted=true;
			        	hndl.postDelayed(new Runnable(){
			    			@Override
			    			public void run() {
					    		BluetoothAdapter bt_adapter = BluetoothAdapter.getDefaultAdapter();
						    	bt_adapter.closeProfileProxy(BluetoothProfile.HEADSET, mBluetoothProfHeadset);
						    	bt_adapter.closeProfileProxy(BluetoothProfile.A2DP, mBluetoothProfA2DP);
						    	bt_adapter.closeProfileProxy(BluetoothProfile.HEALTH, mBluetoothProfHealth);
//						    	bt_adapter.closeProfileProxy(BluetoothProfile.GATT, mBluetoothProfGATT);
						    	if (Build.VERSION.SDK_INT>=16) {
						        	bt_adapter.closeProfileProxy(BluetoothProfile_PAN, mBluetoothProfPAN);
						    	}
			    			}
			        	}, 1000);
				    	p_ntfy.notifyToListener(true, null);
			    	}
				}
			}
			public void onServiceDisconnected(int profile) {
				util.addDebugMsg(1,"I","Bluetooth profile disconnected, type="+profile);
//			    if (profile == BluetoothProfile.HEADSET) {
//			        mBluetoothProfHeadset = null;
//			    } else if (profile == BluetoothProfile.A2DP) {
//			        mBluetoothProfA2DP = null;
//			    } else if (profile == BluetoothProfile.HEALTH) {
//			        mBluetoothProfHealth = null;
//			    } else if (profile == BluetoothProfile.GATT) {
//			        mBluetoothProfGATT = null;
//			    } else if (profile == BluetoothProfile_PAN) {
//			    	mBluetoothProfPAN=null;
//			    }
//			    if (mBluetoothProfHeadset==null &&
//			    		mBluetoothProfA2DP==null &&
//			    		mBluetoothProfHealth==null &&
//			    		mBluetoothProfGATT==null &&
//			    		mBluetoothProfPAN==null) {
//			    	p_ntfy.notifyToListener(true, null);
//			    }
			}
		};

    	final BluetoothAdapter bt_adapter = BluetoothAdapter.getDefaultAdapter();
    	if (bt_adapter==null || !bt_adapter.isEnabled()) p_ntfy.notifyToListener(true, null);
    	else {
        	bt_adapter.getProfileProxy(c, mProfileListener, BluetoothProfile.HEADSET);
        	bt_adapter.getProfileProxy(c, mProfileListener, BluetoothProfile.A2DP);
        	bt_adapter.getProfileProxy(c, mProfileListener, BluetoothProfile.HEALTH);
//         	bt_adapter.getProfileProxy(c, mProfileListener, BluetoothProfile.GATT);
        	if (Build.VERSION.SDK_INT>=16) {
            	bt_adapter.getProfileProxy(c, mProfileListener, BluetoothProfile_PAN);
        	}
        	Handler hndl_to=new Handler();
        	hndl_to.postDelayed(new Runnable(){
				@Override
				public void run() {
					if (!buildBluetoothDeviceListCompleted) {
						util.addDebugMsg(1,"I","buildBluetoothConnectedDeviceList timeout occured");
						buildBluetoothDeviceListCompleted=true;
						p_ntfy.notifyToListener(true, null);
			    		BluetoothAdapter bt_adapter = BluetoothAdapter.getDefaultAdapter();
				    	bt_adapter.closeProfileProxy(BluetoothProfile.HEADSET, mBluetoothProfHeadset);
				    	bt_adapter.closeProfileProxy(BluetoothProfile.A2DP, mBluetoothProfA2DP);
				    	bt_adapter.closeProfileProxy(BluetoothProfile.HEALTH, mBluetoothProfHealth);
//				    	bt_adapter.closeProfileProxy(BluetoothProfile.GATT, mBluetoothProfGATT);
				    	if (Build.VERSION.SDK_INT>=16) {
				        	bt_adapter.closeProfileProxy(BluetoothProfile_PAN, mBluetoothProfPAN);
				    	}
					}
				}
        	}, 2000);
        	
    	}
    };
    
    @SuppressLint("NewApi")
	private void addConnectedBluetoothDeviceByList(Context c, String id, List<BluetoothDevice> bdl,
			final CommonUtilities util) {
    	if (bdl!=null) {
    		if (!buildBluetoothDeviceListCompleted) {
        		for(int i=0;i<bdl.size();i++) {
        			int type=TrustItem.TYPE_BLUETOOTH_CLASSIC_DEVICE;
        			if (Build.VERSION.SDK_INT>=18) {
            			if (bdl.get(i).getType()==BluetoothDevice.DEVICE_TYPE_LE) type=TrustItem.TYPE_BLUETOOTH_LE_DEVICE; 
        			}
        			if (getBluetoothConnectedDevice(type, bdl.get(i).getName(), bdl.get(i).getAddress())!=null) {
            			util.addDebugMsg(2,"I","addConnectedBluetoothDeviceByList device already added, "+id+", Name="+bdl.get(i).getName()+", addr="+bdl.get(i).getAddress());
        			} else {
        				util.addDebugMsg(2,"I","addConnectedBluetoothDeviceByList device added, "+id+", Name="+bdl.get(i).getName()+", addr="+bdl.get(i).getAddress());
            			addBluetoothConnectedDevice(c, type, bdl.get(i).getName(), bdl.get(i).getAddress(), false, true);
        			}
        		}
    		} else {
    			util.addDebugMsg(1,"I","addConnectedBluetoothDeviceByList ignored, profile="+id);
    		}
    	}
    };
    
	final static public void putSavedBluetoothConnectedDevice(Context c, 
			ArrayList<TrustItem> bdcl) {
		if (Build.VERSION.SDK_INT>10) return;
		
		if (bdcl==null || bdcl.size()==0) CommonUtilities.getPrefMgr(c).edit().remove(BLUETOOTH_CONNECTED_DEVICE_LIST_KEY).commit();
		else {
			String data="", sep="", line="";
			for(int i=0;i<bdcl.size();i++) {
				if (bdcl.get(i).trustItemType==TrustItem.TYPE_BLUETOOTH_CLASSIC_DEVICE) {
					line=bdcl.get(i).trustDeviceName+"\t"+bdcl.get(i).trustDeviceAddr;
					data+=sep+line;
					sep="\n";
				}
			}
			CommonUtilities.getPrefMgr(c).edit().putString(BLUETOOTH_CONNECTED_DEVICE_LIST_KEY,data).commit();
//			Log.v("","saved="+data);
		}
	};

	final static private ArrayList<TrustItem> loadSavedBluetoothConnectedDevice(Context c) {
		ArrayList<TrustItem> bdcl=new ArrayList<TrustItem>();
		
		if (Build.VERSION.SDK_INT>10) return bdcl;
		
		String raw_data=CommonUtilities.getPrefMgr(c).getString(BLUETOOTH_CONNECTED_DEVICE_LIST_KEY,"");
//		Log.v("","load="+raw_data);
		if (!raw_data.equals("")) {
			String[] line=raw_data.split("\n");
			if (line!=null && line.length>0) {
				for(int i=0;i<line.length;i++) {
					TrustItem bdli=new TrustItem();
					bdli.trustItemType=TrustItem.TYPE_BLUETOOTH_CLASSIC_DEVICE;
					String[]data=line[i].split("\t");
//					Log.v("","data="+data);
					if (data!=null) {
//						Log.v("","data0="+data[0]);
						if (data.length>=1) bdli.trustDeviceName=data[0];
						if (data.length>=2) bdli.trustDeviceAddr=data[1];
					}
					if (!bdli.trustDeviceName.equals("")) bdcl.add(bdli);
				}
			}
		}
		return bdcl;
	};

//	Trusted network/bluetooth device 
	public ArrayList<TrustItem> trustItemList=new ArrayList<TrustItem>();
	public boolean wifiTrusted=false;
	public boolean bluetoothTrusted=false;
	
	final public TrustItem getTrustItem(int type, String t_name, String t_addr) {
		String name="", addr="";
		if (t_name!=null) name=t_name;
		if (t_addr!=null) addr=t_addr;
		for(TrustItem tdli:trustItemList) {
			if (tdli.trustItemType==type && tdli.trustDeviceName.equals(name)) {
				if (tdli.trustDeviceAddr.equals("")) return tdli;
				if (!addr.equals("")) {
					if (tdli.trustDeviceAddr.equalsIgnoreCase(addr)) {
						return tdli;
					}
				} else {
					return tdli;
				}
			}
		}
		return null;
	};

//		Screen status	    	
	public boolean screenIsLocked=false;
	public boolean screenIsOn=false;

//	Keyguard control	
	private KeyguardManager keyguardManager = null;
	private KeyguardLock keyguardLock = null;
	public void initKgs(Context c) {
 	    keyguardManager = (KeyguardManager)c.getSystemService(Context.KEYGUARD_SERVICE);
		keyguardLock= keyguardManager.newKeyguardLock(APPLICATION_TAG);
	};
	
	public boolean setDpmPassword(Context c) {
		boolean result=true;
		if (trustedDeviceKeyguardLockPassword!=null && !trustedDeviceKeyguardLockPassword.equals("")) {
	        DevicePolicyManager dpm = 
	        		(DevicePolicyManager)c.getSystemService(Context.DEVICE_POLICY_SERVICE);
	        ComponentName darcn = new ComponentName(c, DevAdmReceiver.class);
	        if (dpm.isAdminActive(darcn)) {
	        	boolean dpm_rc=false;
	        	if (Build.VERSION.SDK_INT>=23) {
		        	if (trustedDeviceKeyguardLockPassword.length()>=MINUMUM_PASSWORD_LENGTH) {
		        		dpm_rc=dpm.resetPassword(trustedDeviceKeyguardLockPassword, 
		        				DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
		        	} else {
		        		result=false;
		        	}
	        	} else {
	        		dpm_rc=dpm.resetPassword(trustedDeviceKeyguardLockPassword, 
	        				DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
	        	}
	        	Log.v("","dpm_rc="+dpm_rc+", pswd="+trustedDeviceKeyguardLockPassword);
	        } else {
	        	result=false;
	        }
		}
		return result;
	}
	
	public boolean setKgEnabled(Context c) {
//		Thread.dumpStack();
		boolean result=true;
		if (trustedDeviceKeyguardLockPassword==null || trustedDeviceKeyguardLockPassword.equals("")) keyguardLock.reenableKeyguard();
		else {
			result=setDpmPassword(c);
        	keyguardLock.reenableKeyguard();
		}
		return result;
	};
	
	@SuppressWarnings("unused")
	public boolean removeDpmPassword(Context c) {
		boolean result=true;
		if (trustedDeviceKeyguardLockPassword!=null && !trustedDeviceKeyguardLockPassword.equals("")) {
	        DevicePolicyManager dpm = 
	        		(DevicePolicyManager)c.getSystemService(Context.DEVICE_POLICY_SERVICE);
	        ComponentName darcn = new ComponentName(c, DevAdmReceiver.class);
	        if (dpm.isAdminActive(darcn)) {
	        	boolean rc=false;
	        	if (Build.VERSION.SDK_INT>=23) {
	        		int mpl=dpm.getPasswordMinimumLength(darcn);
		        	dpm.setPasswordMinimumLength(darcn, 0);
		        	rc=dpm.resetPassword("", DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
		        	dpm.setPasswordMinimumLength(darcn, mpl);
//		        	Log.v("","mpl="+mpl);
	        	} else {
	        		rc=dpm.resetPassword("", DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
	        	}
//	        	Log.v("","rc="+rc);
	        } else {
	        	result=false;
	        }
		}
		return result;
	}
	
	public boolean setKgDisabled(Context c) {
		boolean result=true;
		if (trustedDeviceKeyguardLockPassword==null || trustedDeviceKeyguardLockPassword.equals("")) {
			keyguardLock.disableKeyguard();
		} else {
			result=removeDpmPassword(c);
        	keyguardLock.disableKeyguard();
		}
		return result;
	};
	
	public boolean pendingRequestForEnableKeyguard=true;
	public boolean enableKeyguard=true;
	public boolean previousKeyguard=true;
	
	public final static int KEYGUARD_STATUS_LOCKED=0;
	public final static int KEYGUARD_STATUS_UNLOCKED=1;
	public final static int KEYGUARD_STATUS_MANUAL_UNLOCK_REQUIRED=2;
	private int keyGuardStatus=0;

	public void setKeyGuardStatusLocked() {
//		Log.v("","setKeyGuardStatusLocked() called");
		keyGuardStatus=KEYGUARD_STATUS_LOCKED;}
	public void setKeyGuardStatusUnlocked() {
//		Log.v("","setKeyGuardStatusUnlocked() called");
		keyGuardStatus=KEYGUARD_STATUS_UNLOCKED;}
	public void setKeyGuardStatusManualUnlockRequired() {
//		Log.v("","setKeyGuardStatusManualUnlockRequired() called");
		keyGuardStatus=KEYGUARD_STATUS_MANUAL_UNLOCK_REQUIRED;}
	
	public boolean isKeyGuardStatusLocked() {return keyGuardStatus==KEYGUARD_STATUS_LOCKED?true:false;}
	public boolean isKeyGuardStatusUnlocked() {return keyGuardStatus==KEYGUARD_STATUS_UNLOCKED?true:false;}
	public boolean isKeyGuardStatusManualUnlockRequired() {return keyGuardStatus==KEYGUARD_STATUS_MANUAL_UNLOCK_REQUIRED?true:false;}
	public int getKeyGuardStatus() {return keyGuardStatus;}

//	Ringer mode	    	
	public int     currentRingerMode=0;
	final public boolean isRingerModeNormal() {
		return currentRingerMode==AudioManager.RINGER_MODE_NORMAL?true:false;
	}
	final public boolean isRingerModeSilent() {
		return currentRingerMode==AudioManager.RINGER_MODE_SILENT?true:false;
	}
	final public boolean isRingerModeVibrate() {
		return currentRingerMode==AudioManager.RINGER_MODE_VIBRATE?true:false;
	}
	
//	Location
	transient public Location currentLocation=null;
	
//	Configuration
	public int currentOrientation=Configuration.ORIENTATION_PORTRAIT;
	
	public boolean isOrientationLanscape() {
		return currentOrientation==Configuration.ORIENTATION_LANDSCAPE ? true : false;
	}
	
//	Settings parameter	    	
	public boolean settingEnableScheduler=true;
	public boolean settingExitClean;
	public int     settingDebugLevel=3;
	public boolean settingDeviceAdmin;
	public boolean settingUseLightTheme=false;
	public int     settingLogMaxFileCount=10;		
	public String  settingLogMsgDir="", settingLogMsgFilename=LOG_FILE_NAME;
	public boolean settingLogOption=false;
	public long    settingHeartBeatIntervalTime=1*60*1000;
	public long	   settingResourceCleanupIntervalTime=30*60*1000;//10*60*1000;
	
	public boolean settingForceUseTrustDevice=false;
	
	public int     settingBtLeScanMinimumTime=1000*2;
	public int     settingBtLeScanTimeForAndroid4=1000*10;
	public int     settingBtLeScanTimeForAndroid5=1000*10;
	public int     settingBtLeScanTimeForAdapterOn=1000*50;
	public int     settingBtLeScanRetryCouny=0;
	public int     settingBtLeMinRssi=0;
	public int     settingBtLeScanIntervalTimeScreenUnlocked=180*1000;
	public int     settingBtLeScanIntervalTimeScreenLocked=300*1000;

	public int     settingBtLeScanDialogIntervalTime=120*1000;

	public String settingWakeLockOption=WAKE_LOCK_OPTION_SYSTEM;
	public boolean settingWakeLockProximitySensor=false;

	public boolean notifyScreenLockedVibrate=false;
	public String notifyScreenLockedVibratePattern=NOTIFY_WHEN_SCREEN_LOCKED_VIBRATE_PATTERN_VALUE2;
	public boolean notifyScreenLockedNotification=false;
	public String notifyScreenLockedNotificationTitle="";
	public String notifyScreenLockedNotificationPath="";
	
	public boolean notifyScreenUnlockedVibrate=false;
	public String notifyScreenUnlockedVibratePattern=NOTIFY_WHEN_SCREEN_UNLOCKED_VIBRATE_PATTERN_VALUE3;
	public boolean notifyScreenUnlockedNotification=false;
	public String notifyScreenUnlockedNotificationTitle="";
	public String notifyScreenUnlockedNotificationPath="";

	public String wifiOffScreenLocked=WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_DISABLED;
	public int wifiOffScreenLockedTimeoutValue=1000*60;
	public String bluetoothOffScreenLocked=BLUETOOTH_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_DISABLED;
	public int bluetoothOffScreenLockedTimeoutValue=1000*60;
	
	public String wifiOnScreenUnlocked=WIFI_ON_WHEN_SCREEN_UNLOCKED_DISABLED;
	public String wifiOffConnectionTimeout=WIFI_OFF_WHEN_CONNECT_TIMEOUT_DISABLED;
	public int wifiConnectionTimeoutValue=1000*60;
	public String bluetoothOnScreenUnlocked=BLUETOOTH_ON_WHEN_SCREEN_UNLOCKED_DISABLED;
	public String bluetoothOffConnectionTimeout=BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_DISABLED;
	public int bluetoothConnectionTimeoutValue=1000*60;
	
	public boolean proximityDisabledWhenMultipleEvent=false;
	
	public String proximityUndetected=PROXIMITY_UNDETECTED_DISABLED;
	public String proximityDetected=PROXIMITY_DETECTED_DISABLED;
	public int proximityScreenLockTimeValue=1000*60;
	
	public int trustedDeviceReconnectWaitTime=1000*0;
	public boolean trustedDeviceImmediateLockWhenDisconnected=true;
	public String  trustedDeviceKeyguardLockPassword="";
	
	public String localRootDir="";
	
	@Override
	final public EnvironmentParms clone() {  
        EnvironmentParms env=new EnvironmentParms();
		byte[] buf=serialize();
		env=deSerialize(buf);
		return env;  
    }
	
	final public void dumpEnvParms(String id) {
		
		Log.v(APPLICATION_TAG, id+" "+"Airplane mode on="+airplane_mode_on);
		Log.v(APPLICATION_TAG, id+" "+"Sensor available : Proximity="+proximitySensorAvailable);
		Log.v(APPLICATION_TAG, id+" "+"Sensor active : Proximity="+proximitySensorActive);
		Log.v(APPLICATION_TAG, id+" "+"Battery : Levelt="+batteryLevel+
				", CurrentPowerSource="+batteryPowerSource+
				", Charge status="+batteryChargeStatusString);
		Log.v(APPLICATION_TAG, id+" "+"WiFi : Active="+wifiIsActive+
				", SSID="+wifiSsidName);
		Log.v(APPLICATION_TAG, id+" "+"Bluetooth : Active="+bluetoothIsActive+
				", Device count="+bluetoothConnectedDeviceList.size());
		Log.v(APPLICATION_TAG, id+" "+"Screen locked="+screenIsLocked+
				", Screen is On="+screenIsOn+
				", Telephony status="+telephonyStatus+
				", Ringer mode="+currentRingerMode);
	};
	
	final public void loadControlOption(Context c) {
		wifiOffScreenLocked=
				CommonUtilities.getPrefMgr(c).getString(WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_KEY, WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_DISABLED);
		String to_wifi_off_locked=
				CommonUtilities.getPrefMgr(c).getString(WIFI_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_VALUE_KEY, "1");
		if (to_wifi_off_locked.equals("2")) wifiOffScreenLockedTimeoutValue=60*1000*5;
		else if (to_wifi_off_locked.equals("3")) wifiOffScreenLockedTimeoutValue=60*1000*10;
		else wifiOffScreenLockedTimeoutValue=60*1000*1;
		wifiOnScreenUnlocked=
				CommonUtilities.getPrefMgr(c).getString(WIFI_ON_WHEN_SCREEN_UNLOCKED_KEY, WIFI_ON_WHEN_SCREEN_UNLOCKED_DISABLED);
		wifiOffConnectionTimeout=
				CommonUtilities.getPrefMgr(c).getString(WIFI_OFF_WHEN_CONNECT_TIMEOUT_KEY, WIFI_OFF_WHEN_CONNECT_TIMEOUT_DISABLED);
		bluetoothOffScreenLocked=
				CommonUtilities.getPrefMgr(c).getString(BLUETOOTH_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_KEY, BLUETOOTH_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_DISABLED);
		String to_bt_off_locked=
				CommonUtilities.getPrefMgr(c).getString(BLUETOOTH_OFF_WHEN_SCREEN_LOCKED_TIMEOUT_VALUE_KEY, "1");
		if (to_bt_off_locked.equals("2")) bluetoothOffScreenLockedTimeoutValue=60*1000*5;
		else if (to_bt_off_locked.equals("3")) bluetoothOffScreenLockedTimeoutValue=60*1000*10;
		else bluetoothOffScreenLockedTimeoutValue=60*1000*1;
		
		bluetoothOnScreenUnlocked=
				CommonUtilities.getPrefMgr(c).getString(BLUETOOTH_ON_WHEN_SCREEN_UNLOCKED_KEY, BLUETOOTH_ON_WHEN_SCREEN_UNLOCKED_DISABLED);
		bluetoothOffConnectionTimeout=
				CommonUtilities.getPrefMgr(c).getString(BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_KEY, BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_DISABLED);
		
		String pd=CommonUtilities.getPrefMgr(c).getString(PROXIMITY_DISABLED_WWHEN_MULTIPLE_EVENT_KEY, PROXIMITY_DISABLED_WWHEN_MULTIPLE_EVENT_DISABLED);
		if (pd.equals(PROXIMITY_DISABLED_WWHEN_MULTIPLE_EVENT_DISABLED)) proximityDisabledWhenMultipleEvent=false;
		else proximityDisabledWhenMultipleEvent=true;
		
		
		proximityUndetected=
				CommonUtilities.getPrefMgr(c).getString(PROXIMITY_UNDETECTED_KEY, PROXIMITY_UNDETECTED_DISABLED);
		proximityDetected=
				CommonUtilities.getPrefMgr(c).getString(PROXIMITY_DETECTED_KEY, PROXIMITY_DETECTED_DISABLED);
		
		String to_wifi_on=
				CommonUtilities.getPrefMgr(c).getString(WIFI_OFF_WHEN_CONNECT_TIMEOUT_VALUE_KEY, "1");
		if (to_wifi_on.equals("2")) wifiConnectionTimeoutValue=60*1000*5;
		else if (to_wifi_on.equals("3")) wifiConnectionTimeoutValue=60*1000*10;
		else wifiConnectionTimeoutValue=60*1000*1;
		String to_bt_on=
				CommonUtilities.getPrefMgr(c).getString(BLUETOOTH_OFF_WHEN_CONNECT_TIMEOUT_VALUE_KEY, "1");
		if (to_bt_on.equals("1")) bluetoothConnectionTimeoutValue=60*1000*1;
		else if (to_bt_on.equals("2")) bluetoothConnectionTimeoutValue=60*1000*5;
		else if (to_bt_on.equals("3")) bluetoothConnectionTimeoutValue=60*1000*10;
		String to_prox_detected=
				CommonUtilities.getPrefMgr(c).getString(PROXIMITY_DETECTED_TIMEOUT_VALUE_KEY, "2");
		if (to_prox_detected.equals("0")) proximityScreenLockTimeValue=5*1000;
		else if (to_prox_detected.equals("1")) proximityScreenLockTimeValue=30*1000;
		else if (to_prox_detected.equals("2")) proximityScreenLockTimeValue=60*1000;
		else if (to_prox_detected.equals("3")) proximityScreenLockTimeValue=180*1000;

		notifyScreenLockedVibrate=
				CommonUtilities.getPrefMgr(c).getBoolean(NOTIFY_WHEN_SCREEN_LOCKED_VIBRATE_KEY,false);
		notifyScreenLockedVibratePattern=
				CommonUtilities.getPrefMgr(c).getString(NOTIFY_WHEN_SCREEN_LOCKED_VIBRATE_PATTERN_KEY,
						NOTIFY_WHEN_SCREEN_LOCKED_VIBRATE_PATTERN_VALUE1);

		notifyScreenLockedNotification=
				CommonUtilities.getPrefMgr(c).getBoolean(NOTIFY_WHEN_SCREEN_LOCKED_NOTIFICATION_KEY,false);
		
		notifyScreenUnlockedVibrate=
				CommonUtilities.getPrefMgr(c).getBoolean(NOTIFY_WHEN_SCREEN_UNLOCKED_VIBRATE_KEY,false);
		notifyScreenUnlockedVibratePattern=
				CommonUtilities.getPrefMgr(c).getString(NOTIFY_WHEN_SCREEN_UNLOCKED_VIBRATE_PATTERN_KEY,
						NOTIFY_WHEN_SCREEN_UNLOCKED_VIBRATE_PATTERN_VALUE1);
		
		notifyScreenUnlockedNotification=
				CommonUtilities.getPrefMgr(c).getBoolean(NOTIFY_WHEN_SCREEN_UNLOCKED_NOTIFICATION_KEY,false);
		
		final Uri def_uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		final Ringtone rt=RingtoneManager.getRingtone(c, def_uri);
		final String def_notification_title=rt.getTitle(c);
		final String def_notification_path=def_uri.getPath();

		notifyScreenLockedNotificationPath=CommonUtilities.getPrefMgr(c).getString(NOTIFY_WHEN_SCREEN_LOCKED_NOTIFICATION_PATH_KEY, def_notification_path);
		notifyScreenLockedNotificationTitle=CommonUtilities.getPrefMgr(c).getString(NOTIFY_WHEN_SCREEN_LOCKED_NOTIFICATION_TITLE_KEY, def_notification_title);

		notifyScreenUnlockedNotificationPath=CommonUtilities.getPrefMgr(c).getString(NOTIFY_WHEN_SCREEN_UNLOCKED_NOTIFICATION_PATH_KEY, def_notification_path);
		notifyScreenUnlockedNotificationTitle=CommonUtilities.getPrefMgr(c).getString(NOTIFY_WHEN_SCREEN_UNLOCKED_NOTIFICATION_TITLE_KEY, def_notification_title);

		String tdt=CommonUtilities.getPrefMgr(c).getString(TRUST_DEVICE_DELAY_TIME_VLAUE_KEY, TRUST_DEVICE_DELAY_TIME_NOT_USED);
		if (tdt.equals(TRUST_DEVICE_DELAY_TIME_NOT_USED)) trustedDeviceReconnectWaitTime=0;
		else {
			if (tdt.equals(TRUST_DEVICE_DELAY_TIME_VALUE2)) trustedDeviceReconnectWaitTime=1000*60;
			else if (tdt.equals(TRUST_DEVICE_DELAY_TIME_VALUE3)) trustedDeviceReconnectWaitTime=1000*60*5;
			else if (tdt.equals(TRUST_DEVICE_DELAY_TIME_VALUE4)) trustedDeviceReconnectWaitTime=1000*60*10;
			else {
				trustedDeviceReconnectWaitTime=1000*60;
				CommonUtilities.getPrefMgr(c).edit()
					.putString(TRUST_DEVICE_DELAY_TIME_VLAUE_KEY, TRUST_DEVICE_DELAY_TIME_VALUE2).commit();
			}
		}
		
		trustedDeviceKeyguardLockPassword=getDpmPassword(c);
		
		String immed_lock_value=
				CommonUtilities.getPrefMgr(c).getString(TRUST_DEVICE_IMMEDIATE_LOCK_WHEN_TRUSTED_DEVICE_DISCONN_KEY,
    					TRUST_DEVICE_IMMEDIATE_LOCK_WHEN_TRUSTED_DEVICE_DISCONN_NOT_USED);
		if (immed_lock_value.equals(TRUST_DEVICE_IMMEDIATE_LOCK_WHEN_TRUSTED_DEVICE_DISCONN_NOT_USED)) trustedDeviceImmediateLockWhenDisconnected=false;
		else trustedDeviceImmediateLockWhenDisconnected=true;
	};
	
	final public void loadSettingParms(Context c) {
		deviceManufacturer=Build.MANUFACTURER;
		deviceModel=Build.MODEL;
		
//		Log.v("","Board="+Build.BOARD+", "+
//				"Brand="+Build.BRAND+", "+
//				"Device="+Build.DEVICE+", "+
//				"Display="+Build.DISPLAY+", "+
//				"HW="+Build.HARDWARE+", "+
//				"Host="+Build.HOST+", "+
//				"ID="+Build.ID+", "+
//				"Product="+Build.PRODUCT+", "+
//				"Radio="+Build.RADIO+", "+
//				"Tags="+Build.TAGS+", "+
//				"Type="+Build.TYPE+", "+
//				"User="+Build.USER+", "
//				);

		localRootDir=LocalMountPoint.getExternalStorageDir();
		settingEnableScheduler=
				CommonUtilities.getPrefMgr(c).getBoolean(SETTING_ENABLE_SCHEDULER_KEY,true);
		settingExitClean=
				CommonUtilities.getPrefMgr(c).getBoolean(c.getString(R.string.settings_main_exit_clean),false);
		settingDebugLevel=Integer.parseInt(
				CommonUtilities.getPrefMgr(c).getString(c.getString(R.string.settings_main_log_level),"0"));
		settingDeviceAdmin=
				CommonUtilities.getPrefMgr(c).getBoolean(c.getString(R.string.settings_main_device_admin),true);
		settingUseLightTheme=
				CommonUtilities.getPrefMgr(c).getBoolean(c.getString(R.string.settings_main_use_light_theme),false);
		
		settingForceUseTrustDevice=
				CommonUtilities.getPrefMgr(c).getBoolean(c.getString(R.string.settings_main_force_use_trust_device),false);
		
		settingWakeLockOption=
				CommonUtilities.getPrefMgr(c).getString(c.getString(R.string.settings_main_scheduler_sleep_wake_lock_option),WAKE_LOCK_OPTION_SYSTEM);
		settingWakeLockProximitySensor=
				CommonUtilities.getPrefMgr(c).getBoolean(c.getString(R.string.settings_main_scheduler_sleep_wake_lock_proximity_sensor),false);

		settingLogMsgDir=
				CommonUtilities.getPrefMgr(c).getString(c.getString(R.string.settings_main_log_dir),"");
		settingLogOption=
				CommonUtilities.getPrefMgr(c).getBoolean(c.getString(R.string.settings_main_log_option),false);
		settingLogMaxFileCount=Integer.valueOf(
				CommonUtilities.getPrefMgr(c).getString(c.getString(R.string.settings_main_log_file_max_count),"10"));
		settingBtLeMinRssi=Integer.parseInt(
				CommonUtilities.getPrefMgr(c).getString(c.getString(R.string.settings_main_scheduler_rssi_criteria),"0"));
	};
	
	final public void clearDpmPassword(Context c) {
		CommonUtilities.getPrefMgr(c).edit().remove(TRUST_DEVICE_SCREEN_LOCK_BY_PASSWORD_RESET_KEY).commit();
	}
	
	private final static String mPrefix=APPLICATION_TAG+Build.SERIAL;
	final public String getDpmPassword(Context c) {
		String raw_data=
				CommonUtilities.getPrefMgr(c).getString(TRUST_DEVICE_SCREEN_LOCK_BY_PASSWORD_RESET_KEY,"");
		String dec_pswd="";
		if (!raw_data.equals("")) {
			String enc_data=raw_data;
			CipherParms cp=EncryptUtil.initEncryptEnv(mPrefix);
			byte[] enc_array=Base64Compat.decode(enc_data, Base64Compat.NO_WRAP);
			String dec_str=EncryptUtil.decrypt(enc_array, cp);
			if (dec_str!=null && dec_str.startsWith("AutoPhoneUnlock")) dec_pswd=dec_str.replace("AutoPhoneUnlock","");
			else dec_pswd=null;
		}
//		Log.v("","dec="+dec_pswd);
		return dec_pswd;
	}

	final public void putDpmPassword(Context c, String pswd) {
		if (pswd==null || (pswd!=null && pswd.equals(""))) clearDpmPassword(c);
		else {
			CipherParms cp=EncryptUtil.initEncryptEnv(mPrefix);
			byte[] enc_array=EncryptUtil.encrypt("AutoPhoneUnlock"+pswd, cp); 
			String enc_str = 
					Base64Compat.encodeToString(enc_array, Base64Compat.NO_WRAP);
			CommonUtilities.getPrefMgr(c).edit().putString(TRUST_DEVICE_SCREEN_LOCK_BY_PASSWORD_RESET_KEY, enc_str).commit();
//			Log.v("","pswd="+pswd+", enc="+enc_str);
		}
	}
	
	final public void setSettingParmsForceUseTrustDevice(Context c, boolean force) {
		CommonUtilities.getPrefMgr(c).edit().putBoolean(c.getString(R.string.settings_main_force_use_trust_device),force).commit();
	}
	
	final static public EnvironmentParms deSerialize(byte[] buf) {
		EnvironmentParms env_parms=null;
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(buf); 
			ObjectInput in = new ObjectInputStream(bis); 
		    env_parms=(EnvironmentParms) in.readObject(); 
		    in.close(); 
		} catch (StreamCorruptedException e) {
			Log.v(APPLICATION_TAG, "EnvironmentParameters deSerialize error", e);
		} catch (IOException e) {
			Log.v(APPLICATION_TAG, "EnvironmentParameters deSerialize error", e);
		} catch (ClassNotFoundException e) {
			Log.v(APPLICATION_TAG, "EnvironmentParameters deSerialize error", e);
		}
		return env_parms;
	};
	final public byte[] serialize() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(100000); 
		byte[] buf=null; 
	    try { 
	    	ObjectOutput out = new ObjectOutputStream(bos); 
		    out.writeObject(this);
		    out.flush(); 
		    buf= bos.toByteArray(); 
	    } catch(IOException e) { 
	    	Log.v(APPLICATION_TAG, "EnvironmentParameters serialize error", e); 
		}
		return buf;
	};
}
