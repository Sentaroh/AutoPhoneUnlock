package com.sentaroh.android.AutoPhoneUnlock;

import com.sentaroh.android.AutoPhoneUnlock.ISchedulerCallback;

interface ISchedulerClient{
	
	void setCallBack(ISchedulerCallback callback);
	void removeCallBack(ISchedulerCallback callback);
	
	byte[] getBluetoothConnectedDeviceList();
	
	byte[] getTrustItemList();
	
	void reScanBtLeDevice();
}