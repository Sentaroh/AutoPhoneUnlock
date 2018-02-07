package com.sentaroh.android.AutoPhoneUnlock;

import static com.sentaroh.android.AutoPhoneUnlock.CommonConstants.UNKNOWN_LE_DEVICE_ADDR;
import static com.sentaroh.android.AutoPhoneUnlock.CommonConstants.UNKNOWN_LE_DEVICE_NAME;

import java.util.UUID;

import com.sentaroh.android.Utilities.NotifyEvent;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Handler;

public class BtLeUtil {
	public final static int ALERT_TYPE_NONE=0;
	public final static int ALERT_TYPE_VIBRATION=1;
	public final static int ALERT_TYPE_ALARM=2;
	@SuppressLint("NewApi")
	static public BluetoothGatt sendAlertByAddr(final Context context, final CommonUtilities util, 
			final NotifyEvent p_ntfy, final String addr, final int alert_type) {
		final Handler hndl=new Handler();
		BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(addr);
		if (device!=null) {
			BluetoothGatt c_gatt = device.connectGatt(context, false, new BluetoothGattCallback(){
				@Override
				public void onConnectionStateChange (BluetoothGatt gatt, int status, int newState) {
					final String name=gatt.getDevice().getName()==null?UNKNOWN_LE_DEVICE_NAME:gatt.getDevice().getName();
					final String addr=gatt.getDevice().getAddress()==null?UNKNOWN_LE_DEVICE_ADDR:gatt.getDevice().getAddress();;
					util.addDebugMsg(2, "I", "onConnectionStateChange Name="+name+", addr="+addr+", status=" + status+", newState="+newState);
		            if (status == BluetoothGatt.GATT_SUCCESS && newState==BluetoothGatt.STATE_CONNECTED) {
		                gatt.discoverServices();
		            } else {
		            	gatt.disconnect();
		            	gatt.close();
		            	if (p_ntfy!=null) p_ntfy.notifyToListener(false, 
		            			new Object[] {TrustItem.NOTIFY_GATT_CALLBACK_TYPE_CONNECTION_STATE_CHANGED,
								gatt, status, newState});
		            }
				}
				@Override
				public void onServicesDiscovered (final BluetoothGatt gatt, int status) {
					final String name=gatt.getDevice().getName()==null?UNKNOWN_LE_DEVICE_NAME:gatt.getDevice().getName();
					final String addr=gatt.getDevice().getAddress()==null?UNKNOWN_LE_DEVICE_ADDR:gatt.getDevice().getAddress();;
					util.addDebugMsg(2, "I", "onServicesDiscovered Name="+name+", addr="+addr+", status=" + status);
					if (!sendImmediateAlarm(context, util, gatt, alert_type)) {
					    gatt.disconnect();
					    gatt.close();
					    if (p_ntfy!=null) p_ntfy.notifyToListener(false, 
					    		new Object[] {TrustItem.NOTIFY_GATT_CALLBACK_TYPE_SERVICE_DISCOVERED,
								gatt, status});
					}
				}
				
//		        @Override
//		        // Result of a characteristic read operation
//		        public void onCharacteristicRead(BluetoothGatt gatt,
//		                BluetoothGattCharacteristic characteristic,
//		                int status) {
//		        	util.addDebugMsg(2, "I", "onCharacteristicRead status=" + status);
//		            if (UUID_CHAR_BATTERY_LEVEL.equals(characteristic.getUuid())) {
//		            	util.addDebugMsg(2, "I", "Battery level: " + characteristic.getValue()[0]);
//		            }
//
//		        }
//
		        @Override
		        // Result of a characteristic write operation
		        public void onCharacteristicWrite(final BluetoothGatt gatt,
		                BluetoothGattCharacteristic characteristic, final int status) {
					final String name=gatt.getDevice().getName()==null?UNKNOWN_LE_DEVICE_NAME:gatt.getDevice().getName();
					final String addr=gatt.getDevice().getAddress()==null?UNKNOWN_LE_DEVICE_ADDR:gatt.getDevice().getAddress();;
					util.addDebugMsg(2, "I", "onCharacteristicWrite Name="+name+", addr="+addr+", status=" + status);
				    hndl.postDelayed(new Runnable(){
						@Override
						public void run() {
						    gatt.disconnect();
						    gatt.close();
						    if (p_ntfy!=null) p_ntfy.notifyToListener(true, 
						    		new Object[] {TrustItem.NOTIFY_GATT_CALLBACK_TYPE_CHAR_WRITE,
									gatt, status});
						}
				    },100);
		        }

			});
			return c_gatt;
		} else {
			util.addDebugMsg(2, "I", "Address NOT found :" + addr);
		}
		return null;
	};

	@SuppressLint("NewApi")
	private static boolean sendImmediateAlarm(
			final Context context, final CommonUtilities util,
			final BluetoothGatt gatt, int alert_type) {
		boolean result=false;
		byte level =(byte) ((byte)alert_type&0xFF);
	    BluetoothGattCharacteristic c =
	    		characteristic(util, gatt, UUID_SVC_ALERT, UUID_CHAR_ALERT_LEVEL);
	    if (c!=null) {
		    c.setValue(new byte[] { level });
		    gatt.writeCharacteristic(c);
		    result=true;
	    }
	    util.addDebugMsg(2, "I", "sendImmediateAlarm exit, result=" + result);
//	    Thread.dumpStack();
	    return result;
	};

	@SuppressLint("NewApi")
	public static boolean readBatteryLevel(
			final Context context, final CommonUtilities util, 
			final BluetoothGatt gatt) {
		boolean result=false;
		
	    BluetoothGattCharacteristic c =
	    		characteristic(util, gatt, UUID_SVC_BATTERY, UUID_CHAR_BATTERY_LEVEL);
	    if (c!=null) {
		    gatt.readCharacteristic(c);
		    result=true;
	    }
	    util.addDebugMsg(2, "I", "readBatteryLevel exit, result=" + result);
	    return result;
	};

    public static final UUID UUID_SVC_ALERT = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_LINK_LOSS = UUID.fromString("00001803-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_SVC_BATTERY = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHAR_BATTERY_LEVEL = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHAR_BATTERY_POWER_STATE = UUID.fromString("00002a1b-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHAR_ALERT_LEVEL = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");

    @SuppressLint("NewApi")
	static public BluetoothGattCharacteristic characteristic(CommonUtilities util, 
			BluetoothGatt gatt, UUID sid, UUID cid) {
        BluetoothGattService s = gatt.getService(sid);
        if (s == null) {
            util.addDebugMsg(2, "I", "BLE Service NOT found :" + sid.toString());
            return null;
        }
        BluetoothGattCharacteristic c = s.getCharacteristic(cid);
        if (c == null) {
        	util.addDebugMsg(2, "I", "BLE Characteristic NOT found :" + cid.toString());
            return null;
        }
        return c;
    }


	@SuppressLint("NewApi")
	static public BluetoothGatt connectBtLeDevice(final Context context, final CommonUtilities util, 
			final NotifyEvent p_ntfy, final TrustItem conn_item) {
//		final Handler hndl=new Handler();
		BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(conn_item.trustDeviceAddr);
		if (device!=null) {
			BluetoothGatt gatt = device.connectGatt(context, false, new BluetoothGattCallback(){
				@Override
				public void onConnectionStateChange (BluetoothGatt gatt, int status, int newState) {
					final String name=gatt.getDevice().getName()==null?UNKNOWN_LE_DEVICE_NAME:gatt.getDevice().getName();
					final String addr=gatt.getDevice().getAddress()==null?UNKNOWN_LE_DEVICE_ADDR:gatt.getDevice().getAddress();;
					util.addDebugMsg(2, "I", "onConnectionStateChange Name="+name+", addr="+addr+", status=" + status+", newState="+newState);
					if (status==19 || status==129 || status==133) {
		            	gatt.disconnect();
		            	gatt.close();
		            	conn_item.bleDeviceErrorMsg="Connection closed, Status="+status+", newState="+newState;
		            	if (p_ntfy!=null) p_ntfy.notifyToListener(false, 
		            			new Object[]{gatt.getDevice().getName(), gatt.getDevice().getAddress()});
					} else {
			            if (status == BluetoothGatt.GATT_SUCCESS && newState==BluetoothGatt.STATE_CONNECTED) {
							if (conn_item.notifyGattCallback!=null) 
								conn_item.notifyGattCallback.notifyToListener(true, 
										new Object[] {TrustItem.NOTIFY_GATT_CALLBACK_TYPE_CONNECTION_STATE_CHANGED,
										gatt, status, newState});
			                gatt.discoverServices();
//			            	sendImmediateAlarm(context, util, gatt, 2);
			            } else {
			            	gatt.disconnect();
			            	gatt.close();
			            	if (p_ntfy!=null) p_ntfy.notifyToListener(false, 
			            			new Object[]{gatt.getDevice().getName(), gatt.getDevice().getAddress()});
			            }
					}
				}
				@Override
				public void onServicesDiscovered (final BluetoothGatt gatt, int status) {
					final String name=gatt.getDevice().getName()==null?UNKNOWN_LE_DEVICE_NAME:gatt.getDevice().getName();
					final String addr=gatt.getDevice().getAddress()==null?UNKNOWN_LE_DEVICE_ADDR:gatt.getDevice().getAddress();;
					util.addDebugMsg(2, "I", "onServicesDiscovered Name="+name+", addr="+addr+", status=" + status);
					if (conn_item.notifyGattCallback!=null) 
						conn_item.notifyGattCallback.notifyToListener(true, 
								new Object[] {TrustItem.NOTIFY_GATT_CALLBACK_TYPE_SERVICE_DISCOVERED,
								gatt, status});
				}
				
				@Override
				public void onCharacteristicChanged(BluetoothGatt gatt, 
						BluetoothGattCharacteristic characteristic) {
//					util.addDebugMsg(2, "I", "onCharacteristicChanged entered");
					if (conn_item.notifyGattCallback!=null) 
						conn_item.notifyGattCallback.notifyToListener(true, 
								new Object[] {TrustItem.NOTIFY_GATT_CALLBACK_TYPE_CHAR_CHANGED,
								gatt, characteristic});
				}
				
		        @Override
		        // Result of a characteristic read operation
		        public void onCharacteristicRead(BluetoothGatt gatt,
		                BluetoothGattCharacteristic characteristic,
		                int status) {
					final String name=gatt.getDevice().getName()==null?UNKNOWN_LE_DEVICE_NAME:gatt.getDevice().getName();
					final String addr=gatt.getDevice().getAddress()==null?UNKNOWN_LE_DEVICE_ADDR:gatt.getDevice().getAddress();;
					util.addDebugMsg(2, "I", "onCharacteristicRead Name="+name+", addr="+addr+", status=" + status);
					if (conn_item.notifyGattCallback!=null) 
						conn_item.notifyGattCallback.notifyToListener(true, 
								new Object[] {TrustItem.NOTIFY_GATT_CALLBACK_TYPE_CHAR_READ,
								gatt, characteristic, status});
		        }

		        @Override
		        // Result of a characteristic read operation
		        public void onCharacteristicWrite(final BluetoothGatt gatt,
		                BluetoothGattCharacteristic characteristic,
		                int status) {
					final String name=gatt.getDevice().getName()==null?UNKNOWN_LE_DEVICE_NAME:gatt.getDevice().getName();
					final String addr=gatt.getDevice().getAddress()==null?UNKNOWN_LE_DEVICE_ADDR:gatt.getDevice().getAddress();;
					util.addDebugMsg(2, "I", "onCharacteristicWrite Name="+name+", addr="+addr+", status=" + status);
					if (conn_item.notifyGattCallback!=null) 
						conn_item.notifyGattCallback.notifyToListener(true, 
								new Object[] {TrustItem.NOTIFY_GATT_CALLBACK_TYPE_CHAR_WRITE,
								gatt, characteristic, status});
		        }

			});
			return gatt;
		} else {
			util.addDebugMsg(2, "I", "Address NOT found :" + conn_item.trustDeviceAddr);
		}
		return null;
	};
	
	private static final String SERVICE_UUID_YOU_CAN_CHANGE = "0000180a-0000-1000-8000-00805f9b34fb";
    private static final String CHAR_UUID_YOU_CAN_CHANGE = "00002a29-0000-1000-8000-00805f9b34fb";

	@SuppressLint("NewApi")
	public static boolean startAdvertize(final Context context, 
			final CommonUtilities util, final BtLeCommonArea btle_ca) {
		btle_ca.btleAdvtizer=BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
		if (btle_ca.btleAdvtizer==null) return false;
		
		BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		btle_ca.btleGattServer=manager.openGattServer(context, new BluetoothGattServerCallback(){
		    public void onCharacteristicReadRequest(android.bluetooth.BluetoothDevice device, int requestId, 
		        int offset, BluetoothGattCharacteristic characteristic) {
				final String name=device.getName()==null?UNKNOWN_LE_DEVICE_NAME:device.getName();
				final String addr=device.getAddress()==null?UNKNOWN_LE_DEVICE_ADDR:device.getAddress();;
				util.addDebugMsg(2, "I", "onCharacteristicReadRequest Name="+name+", addr="+addr+
						", requestId=" + requestId+", offset="+offset);

		        //セントラルに任意の文字を返信する
		        characteristic.setValue("something you want to send");
		        btle_ca.btleGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
		        characteristic.getValue());
		    }

		    //セントラル（クライアント）からWriteRequestが来ると呼ばれる
		    public void onCharacteristicWriteRequest(android.bluetooth.BluetoothDevice device, int requestId, 
		        BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, 
		        int offset, byte[] value) {
				final String name=device.getName()==null?UNKNOWN_LE_DEVICE_NAME:device.getName();
				final String addr=device.getAddress()==null?UNKNOWN_LE_DEVICE_ADDR:device.getAddress();;
				util.addDebugMsg(2, "I", "onCharacteristicWriteRequest Name="+name+", addr="+addr+
						", requestId=" + requestId+", offset="+offset+", preparedWrite="+preparedWrite+
						"responseNeeded="+responseNeeded);

		        //セントラルにnullを返信する
		    	btle_ca.btleGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
		    }
		});
		
        //serviceUUIDを設定
        BluetoothGattService service = new BluetoothGattService(
                UUID.fromString(SERVICE_UUID_YOU_CAN_CHANGE),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        //characteristicUUIDを設定
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                UUID.fromString(CHAR_UUID_YOU_CAN_CHANGE),
                BluetoothGattCharacteristic.PROPERTY_READ |
                        BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ |
                        BluetoothGattCharacteristic.PERMISSION_WRITE);

        //characteristicUUIDをserviceUUIDにのせる
        service.addCharacteristic(characteristic);

        //serviceUUIDをサーバーにのせる
        btle_ca.btleGattServer.addService(service);

		AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
		dataBuilder.setIncludeTxPowerLevel(false);
		dataBuilder.setIncludeDeviceName(true);
		AdvertiseData mAdvertiseData = dataBuilder.build();

		AdvertiseSettings.Builder settingBuilder = new AdvertiseSettings.Builder();
		settingBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
		settingBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
		settingBuilder.setConnectable(true);
		AdvertiseSettings mAdvertiseSettings = settingBuilder.build();

		btle_ca.btleAdvertiseCallback = new AdvertiseCallback() {
			public void onStartFailure(int errorCode) {
				super.onStartFailure(errorCode);
				util.addDebugMsg(1, "E", "onStartFailure entered, errorCode="+errorCode);
			}

			public void onStartSuccess(AdvertiseSettings settingsInEffect) {
				super.onStartSuccess(settingsInEffect);
				util.addDebugMsg(1, "I", "onStartSuccess entered, settingsInEffect="+settingsInEffect.toString());
			}
		};
		
		btle_ca.btleAdvtizer.startAdvertising(mAdvertiseSettings, mAdvertiseData, btle_ca.btleAdvertiseCallback);
		
		return true;
	};

	@SuppressLint("NewApi")
	public static boolean stopAdvertize(final Context context, 
			final CommonUtilities util, final BtLeCommonArea btle_ca) {
		if (btle_ca.btleAdvtizer==null) return false;
		//サーバーを閉じる
        if (btle_ca.btleGattServer != null) {
        	btle_ca.btleGattServer.clearServices();
        	btle_ca.btleGattServer.close();
        	btle_ca.btleGattServer = null;
        }

        //アドバタイズを停止
    	btle_ca.btleAdvtizer.stopAdvertising(btle_ca.btleAdvertiseCallback);
    	btle_ca.btleAdvtizer = null;
    	
    	return true;
	};

}

class BtLeCommonArea {
	BluetoothLeAdvertiser btleAdvtizer=null;
	BluetoothGattServer btleGattServer=null;
	AdvertiseCallback btleAdvertiseCallback=null;
}

