package com.sentaroh.android.AutoPhoneUnlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.sentaroh.android.Utilities.StringUtil;
import com.sentaroh.android.Utilities.ThemeColorList;
import com.sentaroh.android.Utilities.ThemeUtil;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BtLeDeviceScanListAdapter extends ArrayAdapter<BtLeDeviceScanListItem>{
	private Activity mContext;
	private int mResourceId;
	private ArrayList<BtLeDeviceScanListItem>items;
	
	private ThemeColorList mThemeColorList=null;
	
	public BtLeDeviceScanListAdapter(Activity a, int textViewResourceId,
			ArrayList<BtLeDeviceScanListItem> objects) {
		super(a, textViewResourceId, objects);
		mContext = a;
		mResourceId = textViewResourceId;
		items=objects;
		mThemeColorList=ThemeUtil.getThemeColorList(a);
	}
	
	@Override
	final public int getCount() {
		return items.size();
	}
	
	
	public boolean isEmptyAdapter() {
		boolean result=true;
		if (items!=null) {
			if (items.size()>0) result=false;
		}
		return result;
	};

	final public void sort() {
		sort(items);
	}

	final static public void sort(ArrayList<BtLeDeviceScanListItem>items) {
		Collections.sort(items, new Comparator<BtLeDeviceScanListItem>() {
			@Override
			public int compare(BtLeDeviceScanListItem lhs, BtLeDeviceScanListItem rhs) {
				if (!lhs.name.equals(rhs.name)) return lhs.name.compareTo(rhs.name);
				return lhs.addr.compareTo(rhs.addr);
			}
		});
	}

	final public void remove(int i) {
		items.remove(i);
	}

	@Override
	final public void add(BtLeDeviceScanListItem lli) {
		items.add(lli);
		notifyDataSetChanged();
	}
	
	@Override
	final public BtLeDeviceScanListItem getItem(int i) {
		 return items.get(i);
	}
	
	final public ArrayList<BtLeDeviceScanListItem> getAllItem() {return items;}
	
	final public void setAllItem(ArrayList<BtLeDeviceScanListItem> p) {
		items.clear();
		if (p!=null) items.addAll(p);
		notifyDataSetChanged();
	}
	
//	@Override
//	public boolean isEnabled(int idx) {
//		 return getItem(idx).getActive().equals("A");
//	}

	private Drawable ll_default=null;
	@SuppressWarnings("deprecation")
	@Override
	final public View getView(int position, View convertView, ViewGroup parent) {
		final ViewHolder holder;
		
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(mResourceId, null);
            holder=new ViewHolder();
            holder.tv_name= (TextView) v.findViewById(R.id.scan_bt_le_device_list_item_name);
            holder.tv_addr= (TextView) v.findViewById(R.id.scan_bt_le_device_list_item_addr);
            holder.tv_rssi_min= (TextView) v.findViewById(R.id.scan_bt_le_device_list_item_rssi_min);
            holder.tv_rssi_max= (TextView) v.findViewById(R.id.scan_bt_le_device_list_item_rssi_max);
            holder.tv_rssi_current= (TextView) v.findViewById(R.id.scan_bt_le_device_list_item_rssi_last);
            holder.tv_last_time= (TextView) v.findViewById(R.id.scan_bt_le_device_list_item_last_time);
            holder.tv_scan_info= (TextView) v.findViewById(R.id.scan_bt_le_device_list_item_scan_info);
            holder.ll_rssi=(LinearLayout) v.findViewById(R.id.scan_bt_le_device_list_item_rssi_view);
            holder.ll_time=(LinearLayout) v.findViewById(R.id.scan_bt_le_device_list_item_time_view);
            holder.ll_scan_info=(LinearLayout) v.findViewById(R.id.scan_bt_le_device_list_item_scan_info_view);
            if (ll_default!=null) ll_default=v.getBackground();
            v.setTag(holder);
        } else {
        	holder= (ViewHolder)v.getTag();
        }
        final int pos=position;
        final BtLeDeviceScanListItem o = getItem(pos);
        if (o.isSelected) {
    		if (mThemeColorList.theme_is_light) v.setBackgroundColor(Color.argb(255, 0, 192, 192));
    		else v.setBackgroundColor(Color.argb(255, 0, 128, 128));
        } else {
        	v.setBackgroundDrawable(ll_default);
        }
        if (o != null) {
    		if (o.name==null) {
            	holder.tv_name.setText(mContext.getString(R.string.msgs_scan_bt_le_device_dlg_no_devices));
            	holder.tv_addr.setText("");
            	holder.ll_rssi.setVisibility(LinearLayout.GONE);
            	holder.ll_time.setVisibility(LinearLayout.GONE);
            	holder.ll_scan_info.setVisibility(LinearLayout.GONE);
    		} else {
            	holder.tv_name.setText(o.name);
            	holder.tv_addr.setText(o.addr);
            	holder.tv_rssi_min.setText(""+o.rssi[0]+"dBm");
            	holder.tv_rssi_max.setText(""+o.rssi[1]+"dBm");
            	holder.tv_rssi_current.setText(""+o.rssi[2]+"dBm");
            	holder.tv_last_time.setText(
            			StringUtil.convDateTimeTo_YearMonthDayHourMinSecMili(o.last_measured_time));
            	holder.tv_scan_info.setText(o.scan_record_format);
            	holder.ll_rssi.setVisibility(LinearLayout.VISIBLE);
            	holder.ll_time.setVisibility(LinearLayout.VISIBLE);
            	holder.ll_scan_info.setVisibility(LinearLayout.VISIBLE);
    		}
       	}
        return v;
	};

	public BtLeDeviceScanListItem getItem(String name, String addr) {
		for(BtLeDeviceScanListItem li:items) {
			if (li.name.equals(name) && li.addr.equals(addr)) {
				return li;
			}
		}
		return null;
	};

	public int getItemPos(String name, String addr) {
		for(BtLeDeviceScanListItem li:items) {
			if (li.name.equals(name) && li.addr.equals(addr)) {
				return items.indexOf(li);
			}
		}
		return -1;
	};

	public void setSelected(int pos, boolean selected) {
		for(BtLeDeviceScanListItem li:items) li.isSelected=false;
		items.get(pos).isSelected=selected;		
	};
	
	public void setAllItemUnselected() {
		for(BtLeDeviceScanListItem li:items) li.isSelected=false;
	};

	class ViewHolder {
		TextView tv_name, tv_addr;
		TextView tv_rssi_min, tv_rssi_max, tv_rssi_current, tv_last_time;
		TextView tv_scan_info;
		LinearLayout ll_rssi, ll_time, ll_scan_info;
	}
}

class BtLeDeviceScanListItem {
	public boolean isSelected=false;
	public String name="";
	public String addr="";
	public String scan_record_format="", scan_record_raw="";
	public long last_measured_time=0;
	public int[] rssi=new int[]{0,-999,-999};
}
