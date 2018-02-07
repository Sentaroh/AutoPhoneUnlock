package com.sentaroh.android.AutoPhoneUnlock;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.sentaroh.android.AutoPhoneUnlock.R;
import com.sentaroh.android.Utilities.NotifyEvent;

public class TrustItemListAdapter extends ArrayAdapter<TrustItem>{
	private Context mContext;
	private int mResourceId;
	private ArrayList<TrustItem>items;
	
	private NotifyEvent mNotifyCbClick=null;
	
	private NotifyEvent mNotifyEditClick=null;
	private NotifyEvent mNotifyAlarmClick=null;
	private NotifyEvent mNotifyEnableClick=null;
	
	private boolean mShowEnabled=true;
	
	public TrustItemListAdapter(Context context, int textViewResourceId,
			ArrayList<TrustItem> objects, NotifyEvent ntfy_cb, NotifyEvent ntfy_edit, 
			NotifyEvent ntfy_alarm, NotifyEvent ntfy_enable) {
		super(context, textViewResourceId, objects);
		mContext = context;
		mResourceId = textViewResourceId;
		items=objects;
		mNotifyCbClick=ntfy_cb;
		mNotifyEditClick=ntfy_edit;
		mNotifyAlarmClick=ntfy_alarm;
		mNotifyEnableClick=ntfy_enable;
	}
	
	@Override
	final public int getCount() {
		return items.size();
	}
	
	public void setShowEnabled(boolean p) {mShowEnabled=p;}
	
	private boolean mShowCheckBox=false;
	public void setShowCheckBox(boolean p) {mShowCheckBox=p;}
	public boolean isShowCheckBox() {return mShowCheckBox;}
	
	public void setNotifyCbClickListener(NotifyEvent ntfy) {mNotifyCbClick=ntfy;}
	
	public void setNotifyEditClickListener(NotifyEvent ntfy) {mNotifyEditClick=ntfy;}
	
	public boolean isAnyItemSelected() {
		boolean result=false;
		if (items!=null) {
			for(int i=0;i<items.size();i++) {
				if (items.get(i).isSelected()) {
					result=true;
					break;
				}
			}
		}
		return result;
	};

	public int getItemSelectedCount() {
		int result=0;
		if (items!=null) {
			for(int i=0;i<items.size();i++) {
				if (items.get(i).isSelected()) {
					result++;
				}
			}
		}
		return result;
	};
	
	public boolean isEmptyAdapter() {
		boolean result=true;
		if (items!=null) {
			if (items.size()>0) result=false;
		}
		return result;
	};

	public void setAllItemSelected(boolean p) {
		if (items!=null) {
			for(int i=0;i<items.size();i++) {
				items.get(i).setSelected(p);
			}
		}
	};

	final public void sort() {
		sort(items);
	}

	final static public void sort(ArrayList<TrustItem>items) {
		Collections.sort(items, new Comparator<TrustItem>() {
			@Override
			public int compare(TrustItem lhs, TrustItem rhs) {
				if (!lhs.trustItemName.equals(rhs.trustItemName)) return lhs.trustItemName.compareTo(rhs.trustItemName);
				if (!lhs.trustDeviceName.equals(rhs.trustDeviceName)) return lhs.trustDeviceName.compareTo(rhs.trustDeviceName);
				return lhs.trustDeviceAddr.compareToIgnoreCase(rhs.trustDeviceAddr);
			}
		});
	}

	final public void remove(int i) {
		items.remove(i);
	}

	@Override
	final public void add(TrustItem lli) {
		items.add(lli);
		notifyDataSetChanged();
	}
	
	@Override
	final public TrustItem getItem(int i) {
		 return items.get(i);
	}
	
	final public ArrayList<TrustItem> getAllItem() {return items;}
	
	final public void setAllItem(ArrayList<TrustItem> p) {
		items.clear();
		if (p!=null) items.addAll(p);
		notifyDataSetChanged();
	}
	
//	@Override
//	public boolean isEnabled(int idx) {
//		 return getItem(idx).getActive().equals("A");
//	}

	@Override
	final public View getView(int position, View convertView, ViewGroup parent) {
		final ViewHolder holder;
		
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(mResourceId, null);
            holder=new ViewHolder();
            holder.iv_icon= (ImageView) v.findViewById(R.id.trust_dev_list_view_item_icon);
            holder.tv_name= (TextView) v.findViewById(R.id.trust_dev_list_view_device_name);
            holder.tv_addr= (TextView) v.findViewById(R.id.trust_dev_list_view_device_addr);
            holder.tv_battery= (TextView) v.findViewById(R.id.trust_dev_list_view_device_battery);
            holder.tv_error_msg= (TextView) v.findViewById(R.id.trust_dev_list_view_device_error_msg);
            holder.tv_enabled= (SwitchCompat) v.findViewById(R.id.trust_dev_list_view_item_enabled);
            holder.cb_cb1= (CheckBox) v.findViewById(R.id.trust_dev_list_view_item_checkbox);
            holder.et_desc= (TextView) v.findViewById(R.id.trust_dev_list_view_item_name);
            holder.btn_edit= (ImageButton) v.findViewById(R.id.trust_dev_list_view_item_edit);
            holder.btn_alarm= (ImageButton) v.findViewById(R.id.trust_dev_list_view_item_alarm);
            v.setTag(holder);
        } else {
        	holder= (ViewHolder)v.getTag();
        }
        final int pos=position;
        final TrustItem o = getItem(pos);
        if (o != null) {
        	if (o.trustDeviceName==null) {
                holder.iv_icon.setVisibility(ImageView.GONE);
                holder.tv_name.setText(mContext.getString(R.string.msgs_trust_device_list_no_entries));
                holder.tv_addr.setVisibility(TextView.GONE);
                holder.tv_battery.setVisibility(TextView.GONE);
                holder.tv_error_msg.setVisibility(TextView.GONE);
                holder.tv_enabled.setVisibility(TextView.GONE);
                holder.btn_edit.setVisibility(TextView.GONE);
                holder.btn_alarm.setVisibility(TextView.GONE);
                holder.cb_cb1.setVisibility(CheckBox.GONE);
        	} else {
                holder.iv_icon.setVisibility(ImageView.VISIBLE);
                holder.tv_addr.setVisibility(TextView.VISIBLE);
                if (o.isBtLeDeviceConnectMode && o.isConnected()) {
                	holder.tv_battery.setVisibility(TextView.VISIBLE);
                	holder.tv_battery.setText(o.bleDeviceBatteryLevel+"%");
                } else holder.tv_battery.setVisibility(TextView.GONE);
                
                if (o.bleDeviceErrorMsg.equals("")) holder.tv_error_msg.setVisibility(TextView.GONE);
                else {
                	holder.tv_error_msg.setVisibility(TextView.VISIBLE);
                	holder.tv_error_msg.setText(o.bleDeviceErrorMsg);
                }
                
                if (mShowEnabled) holder.tv_enabled.setVisibility(TextView.VISIBLE);
                else holder.tv_enabled.setVisibility(TextView.GONE);
                
                holder.btn_edit.setVisibility(TextView.VISIBLE);
                
            	holder.tv_name.setText(o.trustDeviceName);
            	holder.tv_addr.setText(o.trustDeviceAddr);
            	
       	        if (mShowCheckBox) {
       	        	holder.tv_enabled.setEnabled(false);
       	        	holder.cb_cb1.setVisibility(CheckBox.VISIBLE);
       	        	holder.btn_edit.setVisibility(Button.GONE);
       	        } else {
       	        	holder.tv_enabled.setEnabled(true);
       	        	holder.cb_cb1.setVisibility(CheckBox.GONE);
       	        	holder.btn_edit.setVisibility(Button.VISIBLE);
       	        }
       	        
       	        if (o.hasImmedAlert && !mShowCheckBox && o.isEnabled() && o.isConnected()) {
       	        	holder.btn_alarm.setVisibility(Button.VISIBLE);
       	        } else {
       	        	holder.btn_alarm.setVisibility(Button.GONE);
       	        }

            	if (o.isEnabled()) {
            		holder.tv_name.setEnabled(true);
                	holder.tv_addr.setEnabled(true);
                	holder.tv_battery.setEnabled(true);
                	holder.et_desc.setEnabled(true);
                	holder.tv_enabled.setChecked(true);
            	} else {
            		holder.tv_name.setEnabled(false);
                	holder.tv_addr.setEnabled(false);
                	holder.tv_battery.setEnabled(false);
                	holder.et_desc.setEnabled(false);
                	holder.tv_enabled.setChecked(false);
            	}
            	
            	if (o.trustItemType==TrustItem.TYPE_WIFI_AP) {
            		if (o.isConnected() && o.isEnabled()) holder.iv_icon.setImageResource(R.drawable.device_wifi_on);
            		else holder.iv_icon.setImageResource(R.drawable.device_wifi_off);
            	} else if (o.trustItemType==TrustItem.TYPE_BLUETOOTH_CLASSIC_DEVICE) {
            		if (o.isConnected() && o.isEnabled()) holder.iv_icon.setImageResource(R.drawable.device_bluetooth_on);
            		else holder.iv_icon.setImageResource(R.drawable.device_bluetooth_off);
            	} else if (o.trustItemType==TrustItem.TYPE_BLUETOOTH_LE_DEVICE) {
            		if (o.isConnected() && o.isEnabled()) {
            			if (o.isBtLeDeviceConnectMode) holder.iv_icon.setImageResource(R.drawable.device_ble_conn_enabled);
            			else holder.iv_icon.setImageResource(R.drawable.device_ble_adv_enabled);
            		} else {
            			if (o.isBtLeDeviceConnectMode) holder.iv_icon.setImageResource(R.drawable.device_ble_conn_disabled);
            			else holder.iv_icon.setImageResource(R.drawable.device_ble_adv_disabled);
            		}
            	} else {
            		holder.iv_icon.setImageDrawable(null);
            	}
       	        
//       	        if (o.isEnabled()) holder.tv_enabled.setText(mContext.getString(R.string.msgs_trust_device_list_item_enabled));
//       	        else holder.tv_enabled.setText(mContext.getString(R.string.msgs_trust_device_list_item_disabled));
       	        
                holder.cb_cb1.setOnCheckedChangeListener(new OnCheckedChangeListener() {
        			@Override
        			public void onCheckedChanged(CompoundButton buttonView,
        				boolean isChecked) {
        				o.setSelected(isChecked);
        				if (mNotifyCbClick!=null && mShowCheckBox) 
        					mNotifyCbClick.notifyToListener(true, new Object[] {isChecked});
       				}
       			});
                holder.cb_cb1.setChecked(o.isSelected());
                
            	holder.et_desc.setText(o.trustItemName);
            	
            	holder.btn_edit.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
        				if (mNotifyEditClick!=null) 
        					mNotifyEditClick.notifyToListener(true, new Object[] {pos});
					}
       			});
            	holder.btn_alarm.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
        				if (mNotifyAlarmClick!=null) 
        					mNotifyAlarmClick.notifyToListener(true, new Object[] {
        							o.trustItemName, o.trustDeviceName, o.trustDeviceAddr});
					}
       			});
            	holder.tv_enabled.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						holder.tv_enabled.setChecked(!holder.tv_enabled.isChecked());
        				if (mNotifyEnableClick!=null) {
        					mNotifyEnableClick.notifyToListener(true, new Object[] {pos});
        				}
					}
       			});

        	}
       	}
        return v;
	};


	class ViewHolder {
		TextView tv_name, tv_addr, tv_battery, tv_error_msg;
		SwitchCompat tv_enabled;
		CheckBox cb_cb1;
		ImageView iv_icon;
		TextView et_desc;
		ImageButton btn_edit, btn_alarm;
	}
}

class TrustItem implements Externalizable{
	private static final long serialVersionUID = 1L;
	private boolean isSelected=false;
	private boolean isConnected=false;
	public int trustItemType=TYPE_WIFI_AP;
	public static final int TYPE_WIFI_AP=0;
	public static final int TYPE_BLUETOOTH_CLASSIC_DEVICE=1;
	public static final int TYPE_BLUETOOTH_LE_DEVICE=2;
	
	public boolean isBtLeDeviceConnectMode=false;
	public transient BluetoothGatt bluetoothLeGatt=null;
	
	public int bleDeviceBatteryLevel=-1;
	
	public String bleDeviceErrorMsg="";
	
	public final static int BLE_DEVICE_ACTION_NONE=0;
	public final static int BLE_DEVICE_ACTION_VIBRATION=1;
	public final static int BLE_DEVICE_ACTION_ALARM=2;
	public int bleDeviceLinkLossActionToTag=BLE_DEVICE_ACTION_NONE;
	public int bleDeviceLinkLossActionToHost=BLE_DEVICE_ACTION_NONE;
	public int bleDeviceNotifyButtonAction=BLE_DEVICE_ACTION_NONE;
	
	public transient NotifyEvent notifyGattCallback=null;
	public final static String NOTIFY_GATT_CALLBACK_TYPE_CONNECTION_STATE_CHANGED="onConnectionStateChange";
	public final static String NOTIFY_GATT_CALLBACK_TYPE_SERVICE_DISCOVERED="onServicesDiscovered";
	public final static String NOTIFY_GATT_CALLBACK_TYPE_CHAR_CHANGED="onCharacteristicChanged";
	public final static String NOTIFY_GATT_CALLBACK_TYPE_CHAR_READ="onCharacteristicRead";
	public final static String NOTIFY_GATT_CALLBACK_TYPE_CHAR_WRITE="onCharacteristicWrite";
	
	public boolean hasImmedAlert=false;
	public int searchFindCount=0;
	
	private boolean isEnabled=true;
	
	public String trustDeviceName="";
	public String trustDeviceAddr="";
	
	public String trustItemName="";
	
	public boolean isSelected() {return isSelected;}
	public void setSelected(boolean p) {isSelected=p;}
	
	public boolean isConnected() {return isConnected;}
	public void setConnected(boolean p) {isConnected=p;}			

	public boolean isEnabled() {return isEnabled;}
	public void setEnabled(boolean p) {isEnabled=p;}
	@Override
	public void readExternal(ObjectInput input) throws IOException,
			ClassNotFoundException {
		isSelected=input.readBoolean();
		isConnected=input.readBoolean();
		trustItemType=input.readInt();
		
		isBtLeDeviceConnectMode=input.readBoolean();
		
		bleDeviceBatteryLevel=input.readInt();
		
		bleDeviceErrorMsg=input.readUTF();
		
		bleDeviceLinkLossActionToTag=input.readInt();
		bleDeviceLinkLossActionToHost=input.readInt();
		bleDeviceNotifyButtonAction=input.readInt();
		
		hasImmedAlert=input.readBoolean();
		
		isEnabled=input.readBoolean();
		
		trustDeviceName=input.readUTF();
		trustDeviceAddr=input.readUTF();
		
		trustItemName=input.readUTF();
		
	}
	@Override
	public void writeExternal(ObjectOutput output) throws IOException {
		output.writeBoolean(isSelected);
		output.writeBoolean(isConnected);
		output.writeInt(trustItemType);
		
		output.writeBoolean(isBtLeDeviceConnectMode);
		
		output.writeInt(bleDeviceBatteryLevel);
		
		output.writeUTF(bleDeviceErrorMsg);
		
		output.writeInt(bleDeviceLinkLossActionToTag);
		output.writeInt(bleDeviceLinkLossActionToHost);
		output.writeInt(bleDeviceNotifyButtonAction);
		
		output.writeBoolean(hasImmedAlert);
		
		output.writeBoolean(isEnabled);
		
		output.writeUTF(trustDeviceName);
		output.writeUTF(trustDeviceAddr);
		
		output.writeUTF(trustItemName);
	}			

}

