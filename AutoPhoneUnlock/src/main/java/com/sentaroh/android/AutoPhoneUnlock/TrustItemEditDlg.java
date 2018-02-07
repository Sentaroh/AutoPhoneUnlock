package com.sentaroh.android.AutoPhoneUnlock;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;

public class TrustItemEditDlg {
	private Activity mActivity=null;
	private GlobalParameters mGp=null;
	@SuppressWarnings("unused")
	private CommonUtilities mUtil=null;
	private Context mContext=null;
	@SuppressWarnings("unused")
	private FragmentManager mFragmentManager=null;
	@SuppressWarnings("unused")
	private TrustItemListAdapter mAdapterTrustList=null;
	@SuppressWarnings("unused")
	private ArrayList<TrustItem> mTrustDeviceTable=null;
	private EnvironmentParms mEnvParms=null;
	
	public TrustItemEditDlg(Activity a, Context c, GlobalParameters gp, EnvironmentParms ep, 
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
	
	public void showDlg(final NotifyEvent p_ntfy, 
			final TrustItemListAdapter adapter, final TrustItem tdli) {
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mActivity);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.edit_trust_item_dlg);
		
		LinearLayout ll_dlg_view=(LinearLayout) dialog.findViewById(R.id.edit_trust_item_dlg_view);
		ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

		final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.edit_trust_item_dlg_title_view);
		final TextView title = (TextView) dialog.findViewById(R.id.edit_trust_item_dlg_title);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		title.setTextColor(mGp.themeColorList.text_color_dialog_title);

		final TextView dlg_msg = (TextView) dialog.findViewById(R.id.edit_trust_item_dlg_msg);
		dlg_msg.setVisibility(TextView.GONE);
		final Button btn_ok = (Button) dialog.findViewById(R.id.edit_trust_item_dlg_btn_ok);
		final Button btn_cancel = (Button) dialog.findViewById(R.id.edit_trust_item_dlg_btn_cancel);

		final EditText et_item_name=(EditText)dialog.findViewById(R.id.edit_trust_item_dlg_trust_item_name);
		final TextView tv_device_name=(TextView)dialog.findViewById(R.id.edit_trust_item_dlg_trust_device_name);
		final TextView tv_device_addr=(TextView)dialog.findViewById(R.id.edit_trust_item_dlg_trust_device_addr);
		
		final TextView tv_item_type=(TextView)dialog.findViewById(R.id.edit_trust_item_dlg_trust_item_type);
		
		final LinearLayout detect_type_view = (LinearLayout) dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_type_view);
		final RadioGroup rg_detect_type=(RadioGroup)dialog.findViewById(R.id.edit_trust_item_dlg_trust_rg_detect_type);
		final RadioButton rb_detect_adv=(RadioButton)dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_type_advertising);
		final RadioButton rb_detect_conn=(RadioButton)dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_type_connect);
		
		final LinearLayout connect_option_view = (LinearLayout) dialog.findViewById(R.id.edit_trust_item_dlg_trust_device_connect_option_view);
		final RadioGroup rg_notify_tag=(RadioGroup)dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_notify_tag);
		final RadioButton rb_notify_tag_none=(RadioButton)dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_notify_tag_none);
		final RadioButton rb_notify_tag_vibration=(RadioButton)dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_notify_tag_vibration);
		final RadioButton rb_notify_tag_alarm=(RadioButton)dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_notify_tag_alarm);
		
		final LinearLayout notify_host_view = (LinearLayout) dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_notify_host_virew);
		notify_host_view.setVisibility(LinearLayout.VISIBLE);//GONE);
		final RadioGroup rg_notify_host=(RadioGroup)dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_notify_host);
		final RadioButton rb_notify_host_none=(RadioButton)dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_notify_host_none);
		final RadioButton rb_notify_host_vibration=(RadioButton)dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_notify_host_vibration);
		final RadioButton rb_notify_host_alarm=(RadioButton)dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_notify_host_alarm);
		
		final RadioGroup rg_notify_button=(RadioGroup)dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_notify_button);
		final RadioButton rb_notify_button_none=(RadioButton)dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_notify_button_none);
		final RadioButton rb_notify_button_vibration=(RadioButton)dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_notify_button_vibration);
		final RadioButton rb_notify_button_alarm=(RadioButton)dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_notify_button_alarm);
		
		CommonDialog.setDlgBoxSizeCompact(dialog);
		btn_ok.setEnabled(false);
		
		et_item_name.setText(tdli.trustItemName);
		tv_device_name.setText(tdli.trustDeviceName);
		tv_device_addr.setText(tdli.trustDeviceAddr);
		
		if (tdli.trustItemType==TrustItem.TYPE_WIFI_AP) {
			detect_type_view.setVisibility(LinearLayout.GONE);
			tv_item_type.setText(mActivity.getString(R.string.msgs_trust_device_list_edit_trust_item_type_wifi));
		} else if (tdli.trustItemType==TrustItem.TYPE_BLUETOOTH_CLASSIC_DEVICE) {
			detect_type_view.setVisibility(LinearLayout.GONE);
			tv_item_type.setText(mActivity.getString(R.string.msgs_trust_device_list_edit_trust_item_type_bt_classic));
		} else if (tdli.trustItemType==TrustItem.TYPE_BLUETOOTH_LE_DEVICE) {
			detect_type_view.setVisibility(LinearLayout.VISIBLE);
			tv_item_type.setText(mActivity.getString(R.string.msgs_trust_device_list_edit_trust_item_type_bt_le));
			if (tdli.isBtLeDeviceConnectMode) rb_detect_conn.setChecked(true);
			else rb_detect_adv.setChecked(true);
		}

		if (rb_detect_conn.isChecked()) {
			connect_option_view.setVisibility(LinearLayout.VISIBLE);
		} else {
			connect_option_view.setVisibility(LinearLayout.GONE);
		}
   		if (tdli.bleDeviceLinkLossActionToTag==TrustItem.BLE_DEVICE_ACTION_NONE) rb_notify_tag_none.setChecked(true);
		else if (tdli.bleDeviceLinkLossActionToTag==TrustItem.BLE_DEVICE_ACTION_VIBRATION) rb_notify_tag_vibration.setChecked(true);
		else if (tdli.bleDeviceLinkLossActionToTag==TrustItem.BLE_DEVICE_ACTION_ALARM) rb_notify_tag_alarm.setChecked(true);

		if (tdli.bleDeviceLinkLossActionToHost==TrustItem.BLE_DEVICE_ACTION_NONE) rb_notify_host_none.setChecked(true);
		else if (tdli.bleDeviceLinkLossActionToHost==TrustItem.BLE_DEVICE_ACTION_VIBRATION) rb_notify_host_vibration.setChecked(true);
		else if (tdli.bleDeviceLinkLossActionToHost==TrustItem.BLE_DEVICE_ACTION_ALARM) rb_notify_host_alarm.setChecked(true);

		if (tdli.bleDeviceNotifyButtonAction==TrustItem.BLE_DEVICE_ACTION_NONE) rb_notify_button_none.setChecked(true);
		else if (tdli.bleDeviceNotifyButtonAction==TrustItem.BLE_DEVICE_ACTION_VIBRATION) rb_notify_button_vibration.setChecked(true);
		else if (tdli.bleDeviceNotifyButtonAction==TrustItem.BLE_DEVICE_ACTION_ALARM) rb_notify_button_alarm.setChecked(true);

		rg_detect_type.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				auditInputValue(dialog, tdli);
				showHideConnectOption(dialog, rb_detect_conn.isChecked());
			}
		});

		rg_notify_tag.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				auditInputValue(dialog, tdli);
				showHideConnectOption(dialog, rb_detect_conn.isChecked());
			}
		});

		rg_notify_host.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				auditInputValue(dialog, tdli);
				showHideConnectOption(dialog, rb_detect_conn.isChecked());
			}
		});

		rg_notify_button.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				auditInputValue(dialog, tdli);
				showHideConnectOption(dialog, rb_detect_conn.isChecked());
			}
		});

//		if (et_device_name.getText().length()>0) btn_ok.setEnabled(true);
//		else btn_ok.setEnabled(false);
		btn_ok.setEnabled(false);

		et_item_name.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable arg0) {
				auditInputValue(dialog, tdli);
			}
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,int arg3) {}
		});
		
		//OK button
		btn_ok.setOnClickListener(new View.OnClickListener() {
			@SuppressLint("DefaultLocale")
			public void onClick(View v) {
				dialog.dismiss();
				tdli.trustItemName=et_item_name.getText().toString();
//				tdli.trustDeviceName=tv_device_name.getText().toString();

				if (rb_detect_conn.isChecked()) tdli.isBtLeDeviceConnectMode=true;
				else tdli.isBtLeDeviceConnectMode=false;
				
				if (rb_notify_tag_none.isChecked()) tdli.bleDeviceLinkLossActionToTag=TrustItem.BLE_DEVICE_ACTION_NONE;
				else if (rb_notify_tag_vibration.isChecked()) tdli.bleDeviceLinkLossActionToTag=TrustItem.BLE_DEVICE_ACTION_VIBRATION;
				else if (rb_notify_tag_alarm.isChecked()) tdli.bleDeviceLinkLossActionToTag=TrustItem.BLE_DEVICE_ACTION_ALARM;
				
				if (rb_notify_host_none.isChecked()) tdli.bleDeviceLinkLossActionToHost=TrustItem.BLE_DEVICE_ACTION_NONE;
				else if (rb_notify_host_vibration.isChecked()) tdli.bleDeviceLinkLossActionToHost=TrustItem.BLE_DEVICE_ACTION_VIBRATION;
				else if (rb_notify_host_alarm.isChecked()) tdli.bleDeviceLinkLossActionToHost=TrustItem.BLE_DEVICE_ACTION_ALARM;

				if (rb_notify_button_none.isChecked()) tdli.bleDeviceNotifyButtonAction=TrustItem.BLE_DEVICE_ACTION_NONE;
				else if (rb_notify_button_vibration.isChecked()) tdli.bleDeviceNotifyButtonAction=TrustItem.BLE_DEVICE_ACTION_VIBRATION;
				else if (rb_notify_button_alarm.isChecked()) tdli.bleDeviceNotifyButtonAction=TrustItem.BLE_DEVICE_ACTION_ALARM;

				adapter.sort();
				adapter.notifyDataSetChanged();
				if (p_ntfy!=null) p_ntfy.notifyToListener(true, null);
			}
		});
		// CANCELボタンの指定
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
//		dialog.setCancelable(false);
//		dialog.setOnKeyListener(new DialogOnKeyListener(context));
		dialog.show();
	};

	private void showHideConnectOption(Dialog dialog, boolean show) {
		final LinearLayout connect_option_view = (LinearLayout) 
				dialog.findViewById(R.id.edit_trust_item_dlg_trust_device_connect_option_view);
		if (show) {
			connect_option_view.setVisibility(LinearLayout.VISIBLE);
		} else {
			connect_option_view.setVisibility(LinearLayout.GONE);
		}
	};
	
	private void auditInputValue(Dialog dialog, TrustItem tdli) {
		final Button btn_ok = (Button) dialog.findViewById(R.id.edit_trust_item_dlg_btn_ok);
//		final Button btn_cancel = (Button) dialog.findViewById(R.id.edit_trust_item_dlg_btn_cancel);

		final EditText et_item_name=(EditText)dialog.findViewById(R.id.edit_trust_item_dlg_trust_item_name);
//		final TextView tv_device_name=(TextView)dialog.findViewById(R.id.edit_trust_item_dlg_trust_device_name);
//		final TextView tv_device_addr=(TextView)dialog.findViewById(R.id.edit_trust_item_dlg_trust_device_addr);
//		final TextView tv_item_type=(TextView)dialog.findViewById(R.id.edit_trust_item_dlg_trust_item_type);
		
//		final LinearLayout detect_type_view = (LinearLayout) dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_type_view);
//		final RadioGroup rg_detect_type=(RadioGroup)dialog.findViewById(R.id.edit_trust_item_dlg_trust_rg_detect_type);
		final RadioButton rb_detect_adv=(RadioButton)dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_type_advertising);
		final RadioButton rb_detect_conn=(RadioButton)dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_type_connect);
		
//		final LinearLayout connect_option_view = (LinearLayout) dialog.findViewById(R.id.edit_trust_item_dlg_trust_device_connect_option_view);
//		final RadioGroup rg_notify_tag=(RadioGroup)dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_notify_tag);
		final RadioButton rb_notify_tag_none=(RadioButton)dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_notify_tag_none);
		final RadioButton rb_notify_tag_vibration=(RadioButton)dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_notify_tag_vibration);
		final RadioButton rb_notify_tag_alarm=(RadioButton)dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_notify_tag_alarm);
		
//		final RadioGroup rg_notify_host=(RadioGroup)dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_notify_host);
		final RadioButton rb_notify_host_none=(RadioButton)dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_notify_host_none);
		final RadioButton rb_notify_host_vibration=(RadioButton)dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_notify_host_vibration);
		final RadioButton rb_notify_host_alarm=(RadioButton)dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_notify_host_alarm);
		
//		final RadioGroup rg_notify_button=(RadioGroup)dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_notify_button);
		final RadioButton rb_notify_button_none=(RadioButton)dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_notify_button_none);
		final RadioButton rb_notify_button_vibration=(RadioButton)dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_notify_button_vibration);
		final RadioButton rb_notify_button_alarm=(RadioButton)dialog.findViewById(R.id.edit_trust_item_dlg_trust_detect_notify_button_alarm);

		btn_ok.setEnabled(false);
		if (!et_item_name.getText().toString().equals(tdli.trustItemName) ) {
			btn_ok.setEnabled(true);
		}
		if (tdli.isBtLeDeviceConnectMode) {
			if (!rb_detect_conn.isChecked()) btn_ok.setEnabled(true);
		} else {
			if (!rb_detect_adv.isChecked()) btn_ok.setEnabled(true);
		}
		
		if (rb_detect_conn.isChecked()) {
			int action_tag=0, action_host=0, action_button=0;
			
			if (rb_notify_tag_none.isChecked()) action_tag=TrustItem.BLE_DEVICE_ACTION_NONE;
			else if (rb_notify_tag_vibration.isChecked()) action_tag=TrustItem.BLE_DEVICE_ACTION_VIBRATION;
			else if (rb_notify_tag_alarm.isChecked()) action_tag=TrustItem.BLE_DEVICE_ACTION_ALARM;
			
			if (rb_notify_host_none.isChecked()) action_host=TrustItem.BLE_DEVICE_ACTION_NONE;
			else if (rb_notify_host_vibration.isChecked()) action_host=TrustItem.BLE_DEVICE_ACTION_VIBRATION;
			else if (rb_notify_host_alarm.isChecked()) action_host=TrustItem.BLE_DEVICE_ACTION_ALARM;

			if (rb_notify_button_none.isChecked()) action_button=TrustItem.BLE_DEVICE_ACTION_NONE;
			else if (rb_notify_button_vibration.isChecked()) action_button=TrustItem.BLE_DEVICE_ACTION_VIBRATION;
			else if (rb_notify_button_alarm.isChecked()) action_button=TrustItem.BLE_DEVICE_ACTION_ALARM;
			
			if (tdli.bleDeviceLinkLossActionToTag!=action_tag || 
					tdli.bleDeviceLinkLossActionToHost!=action_host ||
					tdli.bleDeviceNotifyButtonAction!=action_button ) {
				btn_ok.setEnabled(true);
			}
		}
		
	}
	
}
