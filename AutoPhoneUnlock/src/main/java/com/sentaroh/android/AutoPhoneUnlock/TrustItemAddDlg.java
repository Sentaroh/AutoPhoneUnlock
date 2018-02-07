package com.sentaroh.android.AutoPhoneUnlock;

import static com.sentaroh.android.AutoPhoneUnlock.CommonConstants.UNKNOWN_LE_DEVICE_ADDR;
import static com.sentaroh.android.AutoPhoneUnlock.CommonConstants.UNKNOWN_LE_DEVICE_NAME;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;
import com.sentaroh.android.Utilities.Dialog.MessageDialogFragment;
import com.sentaroh.android.Utilities.Dialog.ProgressSpinDialogFragment;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;

public class TrustItemAddDlg {

	private TrustItemListAdapter mTrustDeviceTableAdapter=null;
	private Activity mActivity=null;
	private GlobalParameters mGp=null;
	@SuppressWarnings("unused")
	private CommonUtilities mUtil=null;
	private Context mContext=null;
	private FragmentManager mFragmentManager=null;
	
	private TrustItemListAdapter mAdapterTrustList=null;
	
	private ArrayList<TrustItem> mTrustDeviceTable=null;
	
	private EnvironmentParms mEnvParms=null;
	
	public TrustItemAddDlg(Activity a, Context c, GlobalParameters gp, EnvironmentParms ep, 
			CommonUtilities cu, FragmentManager fm,
			TrustItemListAdapter ta) {
		mActivity=a;
		mGp=gp;
		mUtil=cu;
		mContext=c;
		mAdapterTrustList=ta;
		mFragmentManager=fm;
		mEnvParms=ep;
		mTrustDeviceTable=CommonUtilities.loadTrustedDeviceTable(mContext, mEnvParms);
	};
	
	public void showDlg(final NotifyEvent ntfy_save_btn) {
		final Dialog dialog = new Dialog(mActivity);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.add_trust_dev_dlg);
		LinearLayout ll_dlg_view=(LinearLayout)dialog.findViewById(R.id.add_trust_dev_dlg_view);
		LinearLayout ll_title_view=(LinearLayout)dialog.findViewById(R.id.add_trust_dev_dlg_title_view);
		TextView dlg_title=(TextView)dialog.findViewById(R.id.add_trust_dev_dlg_title);
		ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);
		ll_title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		dlg_title.setTextColor(mGp.themeColorList.text_color_dialog_title);
		final Button btn_cancel= (Button) dialog.findViewById(R.id.add_trust_dev_dlg_btn_cancel);
		final Button btn_ok = (Button) dialog.findViewById(R.id.add_trust_dev_dlg_btn_ok);
		final Button btn_refresh = (Button) dialog.findViewById(R.id.add_trust_dev_dlg_btn_refresh);
		final ImageButton btn_done = (ImageButton) dialog.findViewById(R.id.add_trust_dev_dlg_btn_done);
		final CheckedTextView ctv_add_without_addr=(CheckedTextView)dialog.findViewById(R.id.add_trust_dev_dlg_add_device_without_addr);

		
		ArrayList<TrustItem> btl=new ArrayList<TrustItem>();
		mTrustDeviceTableAdapter=
				new TrustItemListAdapter(mActivity, R.layout.trust_dev_list_view_item, btl, null, null, null, null);
		mTrustDeviceTableAdapter.setShowEnabled(false);
		final ListView lv=(ListView)dialog.findViewById(R.id.add_trust_dev_dlg_listview);
		lv.setAdapter(mTrustDeviceTableAdapter);
		buildAddDeviceList(ctv_add_without_addr.isChecked());

		ctv_add_without_addr.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				ctv_add_without_addr.setChecked(!ctv_add_without_addr.isChecked());
				btn_refresh.performClick();
			}
		});

		setAddDevTitle(dialog, mTrustDeviceTableAdapter);

    	NotifyEvent ntfy_cb_listener=new NotifyEvent(mContext);
    	ntfy_cb_listener.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				if (mTrustDeviceTableAdapter.isShowCheckBox()) {
					setAddDevTitle(dialog, mTrustDeviceTableAdapter);
					if (mTrustDeviceTableAdapter.isAnyItemSelected()) btn_ok.setEnabled(true);
					else btn_ok.setEnabled(false);
				}
			};

			@Override
			public void negativeResponse(Context c, Object[] o) {}
    	});
    	mTrustDeviceTableAdapter.setNotifyCbClickListener(ntfy_cb_listener);
		mTrustDeviceTableAdapter.setShowCheckBox(true);
		mTrustDeviceTableAdapter.notifyDataSetChanged();

		CommonDialog.setDlgBoxSizeLimit(dialog,true);
		
    	lv.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
				if (mTrustDeviceTableAdapter.isShowCheckBox()) {
					mTrustDeviceTableAdapter.getItem(pos).setSelected(!mTrustDeviceTableAdapter.getItem(pos).isSelected());
					mTrustDeviceTableAdapter.notifyDataSetChanged();
					setAddDevTitle(dialog, mTrustDeviceTableAdapter);
					if (mTrustDeviceTableAdapter.isAnyItemSelected()) btn_ok.setEnabled(true);
					else btn_ok.setEnabled(false);
				}
//				editTrustItem(null, adapter, adapter.getItem(pos));
			}
    	});

//    	lv.setOnItemLongClickListener(new OnItemLongClickListener(){
//			@Override
//			public boolean onItemLongClick(AdapterView<?> parent, View view,
//					int position, long id) {
//				editTrustItem(null, adapter, adapter.getItem(position));
//				return true;
//			}
//    	});

    	btn_done.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				ntfy_save_btn.notifyToListener(true, null);
				dialog.dismiss();
			}
    	});

    	btn_refresh.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				mTrustDeviceTableAdapter.clear();
				buildAddDeviceList(ctv_add_without_addr.isChecked());
				mTrustDeviceTableAdapter.notifyDataSetChanged();
			}
    	});
    	
    	btn_ok.setEnabled(false);
    	btn_ok.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				for(int i=0;i<mTrustDeviceTableAdapter.getCount();i++) {
					if (mTrustDeviceTableAdapter.getItem(i).isSelected()) {
						TrustItem new_item=new TrustItem();
						new_item.trustItemType=mTrustDeviceTableAdapter.getItem(i).trustItemType;
						new_item.trustDeviceName=mTrustDeviceTableAdapter.getItem(i).trustDeviceName;
						new_item.trustItemName=mTrustDeviceTableAdapter.getItem(i).trustItemName;
						if (!ctv_add_without_addr.isChecked()) {
							new_item.trustDeviceAddr=mTrustDeviceTableAdapter.getItem(i).trustDeviceAddr;
						} 
						mAdapterTrustList.add(new_item);
//						Log.v("","type="+new_item.trustedItemType);
					}
				}
				mAdapterTrustList.sort();
				mAdapterTrustList.notifyDataSetChanged();
				ntfy_save_btn.notifyToListener(true, null);
				dialog.dismiss();
			}
    	});

    	btn_cancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				dialog.dismiss();
				ntfy_save_btn.notifyToListener(false, null);
			}
    	});

		dialog.show();

	};
	
	private void buildAddDeviceList(boolean name_only) {
//		ArrayList<TrustDeviceListItem> tdl=new ArrayList<TrustDeviceListItem>();
		BluetoothAdapter bta=BluetoothAdapter.getDefaultAdapter();
		String bt_msg="", wifi_msg="";
		if (bta!=null && bta.isEnabled()) {
			buildBluetoothDeviceList(name_only);
//			if(nodev) {
//		        bt_msg=mContext.getString(R.string.msgs_trust_device_add_new_bt_no_dev_msg);
//			}
		} else {
	        bt_msg=mContext.getString(R.string.msgs_trust_device_add_new_bt_adapter_on_msg);
		}
		WifiManager wm=(WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
		if (wm.isWifiEnabled()) {
			String addr="", ssid="";
			ssid=CommonUtilities.getWifiSsidName(wm);
			if (!name_only) addr=wm.getConnectionInfo().getBSSID();

			if (ssid!=null && !ssid.equals("")) {
				boolean found=false;
				for(int i=0;i<mAdapterTrustList.getCount();i++) {
					if (mAdapterTrustList.getItem(i).trustDeviceName.equals(ssid) &&
							mAdapterTrustList.getItem(i).trustDeviceAddr.equalsIgnoreCase(addr)) {
						found=true;
						break;
					}
				}
				if (found) {
					wifi_msg=mContext.getString(R.string.msgs_trust_device_add_new_wifi_ap_msg);
				} else {
					TrustItem tli=new TrustItem();
					tli.trustDeviceName=ssid;
					tli.trustDeviceAddr=addr;
					tli.trustItemType=TrustItem.TYPE_WIFI_AP;
					tli.setConnected(true);
					tli.trustItemName=mContext.getString(R.string.msgs_trust_device_list_default_item_name);
					mTrustDeviceTableAdapter.add(tli);
				}
			}
		} else {
			wifi_msg=mContext.getString(R.string.msgs_trust_device_turn_on_wifi_msg);
		}
		
		if (!bt_msg.equals("") || !wifi_msg.equals("")) {
			String msg_txt="";
			if (bt_msg.equals("")) msg_txt=wifi_msg;
			else {
				if (wifi_msg.equals("")) msg_txt=bt_msg;
				else msg_txt=bt_msg+"\n"+wifi_msg;
			}
	        MessageDialogFragment cdf =MessageDialogFragment.newInstance(false, "W",
	        		mContext.getString(R.string.msgs_trust_device_add_new_dev_some_error),msg_txt);
	        cdf.showDialog(mFragmentManager,cdf,null);
		}
		mTrustDeviceTableAdapter.notifyDataSetChanged();
	};

	private LeScanCallback mLeScanCallback=null;
	
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	private void buildBluetoothLeDeviceList() {
		int scan_time=10;
		final BluetoothAdapter bta=BluetoothAdapter.getDefaultAdapter();
		final ProgressSpinDialogFragment psdf=ProgressSpinDialogFragment.newInstance(
				mContext.getString(R.string.msgs_trust_device_le_scan_title), 
				String.format(mContext.getString(R.string.msgs_trust_device_le_scan_msg),scan_time),
				mContext.getString(R.string.msgs_trust_device_le_scan_cancel),
				mContext.getString(R.string.msgs_trust_device_le_scan_canceling));
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {}
			@Override
			public void negativeResponse(Context c, Object[] o) {
				bta.stopLeScan(mLeScanCallback);
				psdf.dismiss();
			}
		});
		psdf.showDialog(mFragmentManager, psdf, ntfy, false);
		//final ArrayList<TrustDeviceListItem> ble_list=new ArrayList<TrustDeviceListItem>();
		//final ArrayList<TrustDeviceListItem> clasic_list=new ArrayList<TrustDeviceListItem>();
		final Handler hndl=new Handler();
		mLeScanCallback=new LeScanCallback(){
			@Override
			public void onLeScan(BluetoothDevice device, int rssi,
					byte[] scanRecord) {
				final String name=device.getName()==null?UNKNOWN_LE_DEVICE_NAME:device.getName();
				final String addr=device.getAddress()==null?UNKNOWN_LE_DEVICE_ADDR:device.getAddress();;
				if (name.equals("") && addr.equals("")) return;
				boolean trust_found=false;
				for(TrustItem trust_li:mTrustDeviceTable) {
					if (trust_li.trustDeviceName.equals(name)) {
						if (trust_li.trustDeviceAddr.equalsIgnoreCase(addr)) {
							trust_found=true;
							break;
						}
					}
				}
//				Log.v("","le trust found="+trust_found+", name="+name);
				if (!trust_found) {
					boolean adapter_found=false;
					for(TrustItem adapter_li:mTrustDeviceTableAdapter.getAllItem()) {
//						Log.v("","adapter name="+adapter_li.trustedDeviceName+", addr="+adapter_li.trustedDeviceAddr);
						if (adapter_li.trustDeviceName.equals(name)) {
							if (adapter_li.trustDeviceAddr.equalsIgnoreCase(addr)) {
								adapter_found=true;
								break;
							}
						}
					}
//					Log.v("","le adapter found="+adapter_found+", name_only="+name_only);
					if (!adapter_found) {
						final TrustItem n_tdli=new TrustItem();
						n_tdli.trustDeviceName=name;
						n_tdli.trustDeviceAddr=addr;
						n_tdli.setConnected(true);
						n_tdli.trustItemName=mContext.getString(R.string.msgs_trust_device_list_default_item_name);
						n_tdli.trustItemType=TrustItem.TYPE_BLUETOOTH_LE_DEVICE;
						hndl.post(new Runnable(){
							@Override
							public void run() {
								mTrustDeviceTableAdapter.add(n_tdli);
								mTrustDeviceTableAdapter.notifyDataSetChanged();
							}
						});
//						Log.v("","le added");
					}
				}
			}
		};
		bta.startLeScan(mLeScanCallback);
		hndl.postDelayed(new Runnable(){
			@Override
			public void run() {
				bta.stopLeScan(mLeScanCallback);
				psdf.dismiss();
			}
		}, 1000*scan_time);
	};
	
	@SuppressLint("NewApi")
	private void buildBluetoothDeviceList(final boolean name_only) {
		final BluetoothAdapter bta=BluetoothAdapter.getDefaultAdapter();
		final Handler hndl=new Handler();
		if (mEnvParms.isBluetoothLeSupported) {
			if (!name_only) buildBluetoothLeDeviceList();
		}
		Set<BluetoothDevice> bd_list=bta.getBondedDevices();
		Iterator<BluetoothDevice> bDeviceIterator = bd_list.iterator();
 	    while (bDeviceIterator.hasNext()) {
 	    	BluetoothDevice device = bDeviceIterator.next();
			final String name=device.getName()==null?UNKNOWN_LE_DEVICE_NAME:device.getName();
			final String addr=device.getAddress()==null?UNKNOWN_LE_DEVICE_ADDR:device.getAddress();;
 	    	boolean trust_found=false;
 	    	for(int i=0;i<mTrustDeviceTable.size();i++) {
 	    		TrustItem tli=mTrustDeviceTable.get(i);
 	    		if(tli.trustItemType==TrustItem.TYPE_BLUETOOTH_CLASSIC_DEVICE ||
 	    				tli.trustItemType==TrustItem.TYPE_BLUETOOTH_LE_DEVICE) {
 	    			if (tli.trustDeviceName.equals(name)) {
 	    				String c_addr="";
 	    				if (!name_only) {
 	 	    				c_addr=addr;
 	    				}
 	    				if (tli.trustDeviceAddr.equalsIgnoreCase(c_addr)) {
 	    					trust_found=true;
 	    					break;
 	    				}
 	    			}
 	    		}
 	    	}
// 	    	Log.v("","tr found="+trust_found+", name="+device.getName());
 	    	if (!trust_found) {
 	    		TrustItem bdli=new TrustItem();
 	    		if (Build.VERSION.SDK_INT>=18) {
 	 	    		if (device.getType()==BluetoothDevice.DEVICE_TYPE_CLASSIC) bdli.trustItemType=TrustItem.TYPE_BLUETOOTH_CLASSIC_DEVICE;
 	 	    		else if (device.getType()==BluetoothDevice.DEVICE_TYPE_LE) bdli.trustItemType=TrustItem.TYPE_BLUETOOTH_LE_DEVICE;
 	    		} else {
 	    			bdli.trustItemType=TrustItem.TYPE_BLUETOOTH_CLASSIC_DEVICE;
 	    		}
// 	    		Log.v("","type="+bdli.trustedItemType);
 	    		if (bdli.trustItemType==TrustItem.TYPE_BLUETOOTH_CLASSIC_DEVICE || 
 	    				bdli.trustItemType==TrustItem.TYPE_BLUETOOTH_LE_DEVICE) {
 	    			Log.v("","type="+bdli.trustItemType+", no="+name_only+", name="+device.getName());
 	    			if (name_only && bdli.trustItemType==TrustItem.TYPE_BLUETOOTH_LE_DEVICE) {
 	    				//ignore ble device
 	    			} else {
 	 	 	 	    	bdli.trustDeviceName=UNKNOWN_LE_DEVICE_NAME;
 	 	 	 	    	if (device.getName()!=null) bdli.trustDeviceName=name;
 	 	 	 	    	if (!name_only) bdli.trustDeviceAddr=addr;
 	 					bdli.trustItemName=mContext.getString(R.string.msgs_trust_device_list_default_item_name);
 	 	 	 	    	bdli.setConnected(true);
// 	 	 	 	    	Log.v("","added "+bdli.trustedDeviceName);
 	 	 	 	    	mTrustDeviceTableAdapter.add(bdli);
 	 	 	 			hndl.post(new Runnable(){
 	 	 	 				@Override
 	 	 	 				public void run() {
 	 	 	 					mTrustDeviceTableAdapter.notifyDataSetChanged();
 	 	 	 				}
 	 	 	 			});
 	    			}
 	    		}
 	    	}
 	    }

	};
	
	private void setAddDevTitle(Dialog dialog, TrustItemListAdapter adapter) {
		final TextView dlg_title = (TextView) dialog.findViewById(R.id.add_trust_dev_dlg_title);
		int sel=adapter.getItemSelectedCount();
		int tot=adapter.getCount();
		if (sel==0) dlg_title.setText(mContext.getString(R.string.msgs_trust_device_add_title));
		else {
			dlg_title.setText(""+sel+"/"+tot);
		}
	};


}
