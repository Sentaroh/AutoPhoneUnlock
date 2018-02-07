package com.sentaroh.android.AutoPhoneUnlock;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

public class LocationUtilities implements LocationListener{
	
	private TaskManagerParms mTaskMgrParms=null;
	private EnvironmentParms mEnvParms=null;
	private CommonUtilities mUtil=null;
	private LocationManager mLocMgr=null;
	private Looper mLooper=null;
	private boolean isLocationProviderActive=false;
	
	public LocationUtilities(TaskManagerParms tmp, EnvironmentParms ep, CommonUtilities cu) {
		mTaskMgrParms=tmp;
		mEnvParms=ep;
		mUtil=cu;
		mLocMgr=(LocationManager)tmp.context.getSystemService(Context.LOCATION_SERVICE);
		mLooper=Looper.getMainLooper();
	};

	final public boolean isLocationProviderAvailable() {
		LocationManager locMgr=(LocationManager)mTaskMgrParms.context.getSystemService(Context.LOCATION_SERVICE);
		boolean result=false;
		if (locMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			result=true;
		} else if (locMgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			result=true;
		}
		return result;
	};
	
	final public boolean isGpsLocationProviderAvailable() {
		LocationManager locMgr=(LocationManager)mTaskMgrParms.context.getSystemService(Context.LOCATION_SERVICE);
		boolean result=false;
		if (locMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			result=true;
		}
		return result;
	};

	final public boolean isNetworkLocationProviderAvailable() {
		LocationManager locMgr=(LocationManager)mTaskMgrParms.context.getSystemService(Context.LOCATION_SERVICE);
		boolean result=false;
		if (locMgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			result=true;
		}
		return result;
	};

	final public boolean activateAvailableLocationProvider() {
		boolean result=false;
		mEnvParms.currentLocation=null;
		if (isLocationProviderActive) deactivateLocationProvider();
		if (isGpsLocationProviderAvailable()) {
			mLocMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,0,this,mLooper);
			isLocationProviderActive=true;
			result=true;
		} else if (isNetworkLocationProviderAvailable()) {
			mLocMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0,0,this,mLooper);
			isLocationProviderActive=true;
			result=true;
		}
		return result;
	};

	final public boolean activateGpsLocationProvider() {
		boolean result=false;
		mEnvParms.currentLocation=null;
		if (isLocationProviderActive) deactivateLocationProvider();
		if (isGpsLocationProviderAvailable()) {
			mLocMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,0,this,mLooper);
			isLocationProviderActive=true;
			result=true;
		} 
		return result;
	};

	final public boolean activateNetworkLocationProvider() {
		boolean result=false;
		mEnvParms.currentLocation=null;
		if (isLocationProviderActive) deactivateLocationProvider();
		if (isNetworkLocationProviderAvailable()) {
			mLocMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0,0,this,mLooper);
			Log.v("","network");
			isLocationProviderActive=true;
			result=true;
		}
		return result;
	};

	final public Location getCurrentLocation() {
		if (!isLocationProviderActive) {
			Location loc=new Location("");
			loc.setProvider("");
			loc.setAccuracy(-9999f);
			loc.setLatitude(-9999f);
			loc.setLongitude(-9999f);
			mEnvParms.currentLocation=loc;
		}
		return mEnvParms.currentLocation;
	};
	
	final public Location getLastKnownLocation() {
		Location result=null;
		if (isGpsLocationProviderAvailable()) {
			result=mLocMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		} else  if (isNetworkLocationProviderAvailable()) {
			result=mLocMgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		}
		return result;
	};

	final public Location getLastKnownLocationNetworkProvider() {
		Location result=null;
		if (isNetworkLocationProviderAvailable()) {
			result=mLocMgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		}
		return result;
	};

	final public Location getLastKnownLocationGpsProvider() {
		Location result=null;
		if (isGpsLocationProviderAvailable()) {
			result=mLocMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		}
		return result;
	};

	final public void deactivateLocationProvider() {
		mLocMgr.removeUpdates(this);
		isLocationProviderActive=false;
	};
	
	@Override
	public void onLocationChanged(Location loc) {
		mEnvParms.currentLocation=loc;
		mUtil.addDebugMsg(1, "I", "Location changed Provider="+loc.getProvider()+
				", Altitude="+loc.getAltitude()+
				", Latitude="+loc.getLatitude()+
				", Longitude="+loc.getLongitude());
	};

	@Override
	public void onProviderDisabled(String arg0) {
		Location loc=new Location(arg0);
		loc.setAccuracy(-9999f);
		loc.setLatitude(-9999f);
		loc.setLongitude(-9999f);
		mEnvParms.currentLocation=loc;
	};

	@Override
	public void onProviderEnabled(String provider) {
	};

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
        switch(status){
        case LocationProvider.AVAILABLE:
            mUtil.addDebugMsg(1, "I", "Location provider="+provider+" changed(AVAILABLE)");
            break;
        case LocationProvider.OUT_OF_SERVICE:
        	mUtil.addDebugMsg(1, "I", "Location provider="+provider+" changed(OUT_OF_SERVICE)");
            break;
        case  LocationProvider.TEMPORARILY_UNAVAILABLE:
        	mUtil.addDebugMsg(1, "I", "Location provider="+provider+" changed(TEMPORARILY_UNAVAILABLE)");
            break;
             
        }
	};
}
