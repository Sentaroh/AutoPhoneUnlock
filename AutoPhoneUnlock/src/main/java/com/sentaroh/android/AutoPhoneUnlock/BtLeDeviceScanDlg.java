package com.sentaroh.android.AutoPhoneUnlock;

import static com.sentaroh.android.AutoPhoneUnlock.CommonConstants.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.sentaroh.android.AutoPhoneUnlock.ScanRecord.ScanRecordCompat;
import com.sentaroh.android.Utilities.LocalMountPoint;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.StringUtil;
import com.sentaroh.android.Utilities.ThreadCtrl;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;

public class BtLeDeviceScanDlg {
	
	private AppCompatActivity mActivity=null;
	private GlobalParameters mGp=null;
	private EnvironmentParms mEp=null;
	private CommonUtilities mUtil=null;
	private BluetoothAdapter mBluetoothAdapter=null;

	public BtLeDeviceScanDlg(AppCompatActivity a, GlobalParameters gp, EnvironmentParms ep,
			CommonUtilities cu) {
		mActivity=a;
		mGp=gp;
		mEp=ep;
		mUtil=cu;
		mBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
	};

	private void setCheckedTextViewListener(final CheckedTextView ctv) {
		ctv.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				ctv.setChecked(!ctv.isChecked());
			}
		});
	};

	private LeScanCallback mScanCallbackApi18=null;
	private ScanCallback mScanCallbackApi21=null;
	private ListView mDeviceListView=null;
	private BtLeDeviceScanListAdapter mDeviceListAdapter=null;
	
	private CheckedTextView mNotifyWhenNewDeviceAdded=null;
	private CheckedTextView mScanPeriodNolimit=null;
	private TextView mDlgMsg=null;
	private Button mBtnSaveResult=null, mBtnClearResult=null, mBtnClose=null, 
			mBtnScanBegin=null, mBtnScanCancel=null;
	private ProgressBar mProgressBar=null;
	private TextView mProgressMsg=null;

	private ThreadCtrl mTcScanPeriodTimer=null;

	private boolean mIsDeviceListEmpty=true;
	@SuppressLint("NewApi")
	public void scanDialog(final NotifyEvent p_ntfy, String dev_name, String dev_addr) {
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mActivity);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.scan_bt_le_device_dlg);
		
		LinearLayout ll_dlg_view=(LinearLayout) dialog.findViewById(R.id.scan_bt_le_device_dlg_view);
		ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

		final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.scan_bt_le_device_dlg_title_view);
		final TextView title = (TextView) dialog.findViewById(R.id.scan_bt_le_device_dlg_title);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		title.setTextColor(mGp.themeColorList.text_color_dialog_title);

		mDlgMsg = (TextView) dialog.findViewById(R.id.scan_bt_le_device_dlg_msg);
		mDlgMsg.setVisibility(TextView.GONE);
		mBtnClose = (Button) dialog.findViewById(R.id.scan_bt_le_device_dlg_btn_close);
		mBtnScanBegin = (Button) dialog.findViewById(R.id.scan_bt_le_device_dlg_btn_scan);
		mBtnScanCancel = (Button) dialog.findViewById(R.id.scan_bt_le_device_dlg_btn_cancel);
		mBtnSaveResult = (Button) dialog.findViewById(R.id.scan_bt_le_device_dlg_btn_save_result);
		mBtnClearResult = (Button) dialog.findViewById(R.id.scan_bt_le_device_dlg_btn_clear_result);
		mBtnScanCancel.setVisibility(Button.GONE);
		
		mProgressBar=(ProgressBar) dialog.findViewById(R.id.scan_bt_le_device_dlg_progress_bar);
		mProgressMsg=(TextView) dialog.findViewById(R.id.scan_bt_le_device_dlg_progress_msg);
		
		mDeviceListView=(ListView) dialog.findViewById(R.id.scan_bt_le_device_dlg_device_list);
		mDeviceListAdapter=new BtLeDeviceScanListAdapter(mActivity, R.layout.scan_bt_le_device_list_item, new ArrayList<BtLeDeviceScanListItem>());
		mDeviceListView.setAdapter(mDeviceListAdapter);
		initDeviceList();
		mDeviceListView.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				BtLeUtil.sendAlertByAddr(mActivity, mUtil, null, 
//						"00:1B:DC:42:1D:A3",
						mDeviceListAdapter.getItem(position).addr,
						BtLeUtil.ALERT_TYPE_ALARM);
			}
		});
		
		mNotifyWhenNewDeviceAdded=(CheckedTextView) dialog.findViewById(R.id.scan_bt_le_device_dlg_notify_to_user);
		setCheckedTextViewListener(mNotifyWhenNewDeviceAdded);
		mScanPeriodNolimit=(CheckedTextView) dialog.findViewById(R.id.scan_bt_le_device_dlg_no_scan_period);
		setCheckedTextViewListener(mScanPeriodNolimit);
		
		mProgressBar.setVisibility(ProgressBar.GONE);
		mProgressMsg.setVisibility(TextView.GONE);
		
		CommonDialog.setDlgBoxSizeLimit(dialog, true);

		final Handler hndl=new Handler();
		setScanCallback();
		mTcScanPeriodTimer=new ThreadCtrl();
		// Scanボタンの指定
		mBtnScanBegin.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (BluetoothAdapter.getDefaultAdapter()!=null && 
						BluetoothAdapter.getDefaultAdapter().isEnabled())
					performScan(hndl);
			}
		});
		// CANCELボタンの指定
		mBtnScanCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				synchronized(mTcScanPeriodTimer) {
					mTcScanPeriodTimer.setDisabled();
					mTcScanPeriodTimer.notify();
				}
				synchronized(mDeviceListAdapter) {
					mDeviceListAdapter.setAllItemUnselected();
					mDeviceListAdapter.notifyDataSetChanged();
				}
			}
		});
		// Clearボタンの指定
		mBtnClearResult.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				initDeviceList();
			}
		});
		// Saveボタンの指定
		mBtnSaveResult.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				saveResult(mDeviceListAdapter);
			}
		});
		// CLOSEボタンの指定
		mBtnClose.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				synchronized(mTcScanPeriodTimer) {
					mTcScanPeriodTimer.setDisabled();
					mTcScanPeriodTimer.notify();
				}
				dialog.dismiss();
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				mBtnClose.performClick();
			}
		});
//		dialog.setCancelable(false);
//		dialog.setOnKeyListener(new DialogOnKeyListener(context));
		dialog.show();
	};

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private boolean startScanner() {
		boolean scan_success=true;
		if (Build.VERSION.SDK_INT>=21) {
			List<ScanFilter>sf=new ArrayList<ScanFilter>();
			ScanSettings.Builder sb=new ScanSettings.Builder();
			sb.setReportDelay(0);
			sb.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
			if (mBluetoothAdapter.isEnabled()) mBluetoothAdapter.getBluetoothLeScanner().startScan(sf, sb.build(), mScanCallbackApi21);
		} else {
			if (mBluetoothAdapter.isEnabled()) scan_success=mBluetoothAdapter.startLeScan(mScanCallbackApi18);
		}
		return scan_success;
	};
	
	private void initDeviceList() {
		synchronized(mDeviceListAdapter) {
			mDeviceListAdapter.clear();
			BtLeDeviceScanListItem rlmi=new BtLeDeviceScanListItem();
			rlmi.name=null;
			mDeviceListAdapter.add(rlmi);
			mIsDeviceListEmpty=true;
			mDeviceListAdapter.notifyDataSetChanged();
			mBtnSaveResult.setEnabled(false);
			mBtnClearResult.setEnabled(false);
		}
	};
	
	private void performScan(final Handler hndl) {
		mBtnScanBegin.setVisibility(Button.GONE);
		mBtnClearResult.setVisibility(Button.GONE);
		mBtnScanCancel.setVisibility(Button.VISIBLE);
		mTcScanPeriodTimer.setEnabled();
		mDlgMsg.setVisibility(TextView.GONE);
		mScanPeriodNolimit.setVisibility(CheckedTextView.GONE);
		mProgressMsg.setVisibility(TextView.VISIBLE);
		if (!mScanPeriodNolimit.isChecked()) {
			mProgressMsg.setText("");
			mProgressBar.setVisibility(ProgressBar.VISIBLE);
		} else {
			mProgressBar.setVisibility(ProgressBar.VISIBLE);
			mProgressMsg.setText(mActivity.getString(R.string.msgs_scan_bt_le_device_dlg_progress_no_scan_period));
		}
		Thread th=new Thread(){
			@Override
			public void run() {
				final int timer=mEp.settingBtLeScanDialogIntervalTime;
				final String pb_msg=mActivity.getString(R.string.msgs_scan_bt_le_device_dlg_progress_msg);
				int count=0;
				if (startScanner()) {
					boolean sw_exit=false;
					while(!sw_exit) {
						synchronized(mTcScanPeriodTimer) {
							try {
								mTcScanPeriodTimer.wait(100);
								if (!mTcScanPeriodTimer.isEnabled()) {
									sw_exit=true;
								} else {
									if (!mScanPeriodNolimit.isChecked()) {
										updateProgressMsg(hndl, pb_msg, timer-count);
										count+=100;
										if (!mScanPeriodNolimit.isChecked() && count>=timer) sw_exit=true; 
									}
								}
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
					stopScanner(hndl);
				} else {
					setScanFailedMsg(hndl, "");
				}
			}
		};
		th.setName("BtLeScanTimer");
		th.start();
	};
	
	private void updateProgressMsg(Handler hndl, final String pb_msg, final int count) {
		hndl.post(new Runnable(){
			@Override
			public void run() {
				mProgressMsg.setText(String.format(pb_msg, count/1000+1));
			}
		});
	};
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private void stopScanner(Handler hndl) {
		if (Build.VERSION.SDK_INT>=21) {
			if (mBluetoothAdapter.isEnabled()) mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallbackApi21);
		} else {
			if (mBluetoothAdapter.isEnabled()) mBluetoothAdapter.stopLeScan(mScanCallbackApi18);
		}
		hndl.postDelayed(new Runnable(){
			@Override
			public void run() {
				mDeviceListAdapter.setAllItemUnselected();
				mDeviceListAdapter.notifyDataSetChanged();
				mScanPeriodNolimit.setVisibility(CheckedTextView.VISIBLE);
				mBtnScanBegin.setVisibility(Button.VISIBLE);
				mBtnScanCancel.setVisibility(Button.GONE);
				mBtnClearResult.setVisibility(Button.VISIBLE);
				mProgressMsg.setVisibility(TextView.GONE);
				mProgressBar.setVisibility(ProgressBar.GONE);
			}
		},100);
	};
	
	private void setScanFailedMsg(Handler hndl, final String reason) {
		hndl.post(new Runnable(){
			@Override
			public void run() {
				mDlgMsg.setVisibility(TextView.VISIBLE);
				if (reason.equals("")) mDlgMsg.setText("Scan failed");
				else mDlgMsg.setText("Scan failed, "+reason);
			}
		});
	};
	
	@SuppressLint("NewApi")
	private void setScanCallback() {
		final Handler hndl=new Handler();
		if (Build.VERSION.SDK_INT>=21) {
			mScanCallbackApi21=new ScanCallback(){
	    		@Override
	    		public void onBatchScanResults (List<ScanResult> results) {
	    			mUtil.addDebugMsg(1, "W", "onBatchScanResults entered, size="+results.size());
	    		}
	    		@Override
	    		public void onScanFailed (int errorCode) {
	    			mUtil.addDebugMsg(1, "W", "onScanFiled entered, error_code="+errorCode);
					synchronized(mTcScanPeriodTimer) {
						mTcScanPeriodTimer.setDisabled();
						mTcScanPeriodTimer.notify();
					}
					setScanFailedMsg(hndl, "Error Code="+errorCode);
	    		}
	    		@Override
	    		public void onScanResult (int callbackType, ScanResult result) {
	    			BluetoothDevice device=result.getDevice();
	    			updateDeviceList(hndl, device, result.getRssi(), result.getScanRecord().getBytes());
	    		}
			};
		} else {
			mScanCallbackApi18=new LeScanCallback(){
				@Override
				public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
					updateDeviceList(hndl, device,rssi, scanRecord);
				}
			};
		}
	};
	
	private void updateDeviceList(Handler hndl, BluetoothDevice device, int rssi, final byte[] ba) {
		final String name=device.getName()==null?UNKNOWN_LE_DEVICE_NAME:device.getName();
		final String addr=device.getAddress()==null?UNKNOWN_LE_DEVICE_ADDR:device.getAddress();;
		final int t_rssi=rssi;
		int ba_len=ba.length;//>=30?30:ba.length;
		String scan_rec_wk="";
		String sep="";
		for(int i=0;i<ba_len;i+=4) {
			scan_rec_wk+=sep+StringUtil.getHexString(ba, i, (ba_len-i)>=4?4:(ba_len-i));
			sep=", ";
		}
		final String scan_record=scan_rec_wk;
		hndl.post(new Runnable(){
			@Override
			public void run() {
				synchronized(mDeviceListAdapter) {
					if (mIsDeviceListEmpty) mDeviceListAdapter.remove(0);
					BtLeDeviceScanListItem rlmi=mDeviceListAdapter.getItem(name, addr);
					if (rlmi!=null) {
						if (rlmi.rssi[0]>t_rssi) rlmi.rssi[0]=t_rssi;  
						if (rlmi.rssi[1]<t_rssi) rlmi.rssi[1]=t_rssi;
						rlmi.rssi[2]=t_rssi;
						rlmi.last_measured_time=System.currentTimeMillis();
						mDeviceListAdapter.setAllItemUnselected();
						rlmi.isSelected=true;
						mDeviceListAdapter.notifyDataSetChanged();
					} else {
						ScanRecordCompat sr=ScanRecordCompat.parseFromBytes(ba);
						rlmi=new BtLeDeviceScanListItem();
						rlmi.last_measured_time=System.currentTimeMillis();
						rlmi.name=name;
						rlmi.addr=addr;
						rlmi.rssi[0]=t_rssi;  
						rlmi.rssi[1]=t_rssi;
						rlmi.rssi[2]=t_rssi;
						rlmi.scan_record_format=sr.toString();
						rlmi.scan_record_raw=scan_record;
						mDeviceListAdapter.setAllItemUnselected();
						rlmi.isSelected=true;
						mDeviceListAdapter.add(rlmi);
//						mDeviceListAdapter.sort();
						mDeviceListAdapter.notifyDataSetChanged();
						if (mNotifyWhenNewDeviceAdded.isChecked()) notifyUser();
						mIsDeviceListEmpty=false;
						mBtnSaveResult.setEnabled(true);
						mBtnClearResult.setEnabled(true);
						mUtil.addDebugMsg(3, "W", "new device added, "+sr.toString());
					}
					int pos=mDeviceListAdapter.getItemPos(name, addr);
					mDeviceListView.setSelection(pos);
				};
			}
		});
	};
	
	private void notifyUser() {
		final String pattern="3";
		Thread th=new Thread(){
    		@Override
    		public void run(){
    	    	Vibrator vibrator = (Vibrator) mActivity.getSystemService(Context.VIBRATOR_SERVICE);
    	    	if (pattern.equals("1")) vibrator.vibrate(new long[]{0,200},-1);
    	    	else if (pattern.equals("2")) vibrator.vibrate(new long[]{0,150,40,150},-1);
    	    	else vibrator.vibrate(new long[]{0,150,40,150,40,150},-1);
    		}
		};
		th.start();
	};
	
	private void saveResult(BtLeDeviceScanListAdapter adapter) {
		String dir=LocalMountPoint.getExternalStorageDir()+"/"+APPLICATION_TAG;
		File df=new File(dir);
		if (!df.exists()) df.mkdirs();
		String ts=StringUtil.convDateTimeTo_YearMonthDayHourMinSec(System.currentTimeMillis())
				.replaceAll("/", "-")
				.replaceAll(" ", "_")
				.replaceAll(":", "-");
		String fp=dir+"/"+"scan_result_"+ts+".txt";
		File lf=new File(fp);
		try {
			FileWriter fw=new FileWriter(lf);
			BufferedWriter bw=new BufferedWriter(fw,4096*32);
			String line_sep="";
			String item_sep="\t";
			for(BtLeDeviceScanListItem li:adapter.getAllItem()) {
				String out=li.name+item_sep+li.addr+item_sep+li.rssi[0]+item_sep+li.rssi[1]+item_sep+li.rssi[2]+
						item_sep+StringUtil.convDateTimeTo_YearMonthDayHourMinSecMili(li.last_measured_time)+
						item_sep+li.scan_record_format+
						item_sep+li.scan_record_raw;
				bw.write(line_sep+out);
				line_sep="\n";
			}
			bw.flush();
			bw.close();
			Toast toast=Toast.makeText(mActivity, 
					String.format(mActivity.getString(R.string.msgs_scan_bt_le_device_dlg_save_ended),fp), Toast.LENGTH_SHORT);
			toast.show();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	};
	
}
