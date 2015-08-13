package com.hdu.cfryan.activity;

import java.util.ArrayList;

import org.jivesoftware.smack.RosterEntry;

import com.hdu.cfryan.R;
import com.hdu.cfryan.db.AddPhonesProvider;
import com.hdu.cfryan.db.AddPhonesProvider.PhoneConstants;
import com.hdu.cfryan.util.ClassPathResource;
import com.hdu.cfryan.util.L;
import com.hdu.cfryan.util.PreferenceConstants;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class PhonesActivity extends Activity{

	Context mContext = null;
	private String mAddUser;
	private String mAddAlias;
	
	private ContentResolver mContentResolver;

	private static final String[] PHONES_PROJECTION = new String[] {
			Phone.DISPLAY_NAME, Phone.NUMBER, Photo.PHOTO_ID, Phone.CONTACT_ID };

	private static final int PHONES_DISPLAY_NAME_INDEX = 0;

	private static final int PHONES_NUMBER_INDEX = 1;

	private static final int PHONES_PHOTO_ID_INDEX = 2;

	private static final int PHONES_CONTACT_ID_INDEX = 3;

	private ArrayList<String> mContactsName = new ArrayList<String>();

	private ArrayList<String> mContactsNumber = new ArrayList<String>();

	private ArrayList<Bitmap> mContactsPhonto = new ArrayList<Bitmap>();
	
	private ArrayList<String> mStatus = new ArrayList<String>();
	
	private static final String[] PHONE_QUERY = new String[]
	{ PhoneConstants._ID, PhoneConstants.PHONE_NUM, PhoneConstants.NAME, PhoneConstants.STATUS };
	
	
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(android.os.Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case 0:
				Toast.makeText(mContext, "用户不存在", Toast.LENGTH_LONG).show();
			//	mXxService.addRosterItem(mAddUser, mAddUser.substring(0, mAddUser.indexOf("@")) , "friends");
				break;
			case 1:
				Toast.makeText(mContext, "用户在线", Toast.LENGTH_LONG).show();
				break;
			case 2:
				Toast.makeText(mContext, "用户离线", Toast.LENGTH_LONG).show();
				break;	
			case 3:
				mPhoneAdapter = new PhoneListAdapter(mContext);
				mListView.setAdapter(mPhoneAdapter);
				mProgressBarContainer.setVisibility(View.GONE);
				break;
			default:
				break;
			}
		}
	};
	
	ListView mListView = null;
	PhoneListAdapter mPhoneAdapter = null;
	LinearLayout mProgressBarContainer = null; 

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_phone_list);
		mContext = this;
		mListView = (ListView) findViewById(R.id.listview_phone); 

		mProgressBarContainer = (LinearLayout) findViewById(R.id.phone_load_progress_container);
		mProgressBarContainer.setVisibility(View.VISIBLE);
		
		mContentResolver = mContext.getContentResolver();
		
		Cursor c = mContentResolver.query(AddPhonesProvider.CONTENT_URI, PHONE_QUERY, null, null, null);
		c.moveToFirst();
		while (!c.isAfterLast())
		{
			String contactName = c.getString(c.getColumnIndex(PhoneConstants.NAME));
			String phoneNumber = c.getString(c.getColumnIndex(PhoneConstants.PHONE_NUM));
			String status = c.getString(c.getColumnIndex(PhoneConstants.STATUS));
			Log.i("status", status);
			mStatus.add(status);
			mContactsName.add(contactName);
			mContactsNumber.add(phoneNumber);
			c.moveToNext();
		}
		c.close();
		mHandler.sendEmptyMessage(3);
		

	}
	

	private class GetPhoneContactsThread extends Thread {

		@Override
		public void run() {
		//	getPhoneContacts();
			mHandler.sendEmptyMessage(3);
			
		}
	}

	
	private class QueuePresenceThread extends Thread {
		private String jid;

		public QueuePresenceThread(String jid) {
			this.jid = jid;
		}

		@Override
		public void run() {
			try {
				String url = "http://" + PreferenceConstants.DEFAULT_SERVER + ":9090/plugins/presence/status?jid=" 
							+ jid + "@" + PreferenceConstants.DEFAULT_SERVER_NAME + "&type=xml";
			//	short state = PresenceUtil.IsUserOnline(url);
			//	mHandler.sendEmptyMessage(state);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	class PhoneListAdapter extends BaseAdapter {
		public PhoneListAdapter(Context context) {
			mContext = context;
		}

		@Override
		public int getCount() {
			return mContactsName.size();
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final String phoneNum = mContactsNumber.get(position);
			
			ViewHolder viewHolder = null;
			if (convertView == null) {
				viewHolder = new ViewHolder();
				convertView = LayoutInflater.from(mContext).inflate(R.layout.phone_item,
						null);

				viewHolder.ivAvatar = (ImageView) convertView.findViewById(R.id.phone_item_avatar);
				viewHolder.tvTitle = (TextView) convertView.findViewById(R.id.phone_item_title);
				viewHolder.tvNumber = (TextView) convertView.findViewById(R.id.phone_item_number);
				viewHolder.btnAdd = (Button) convertView.findViewById(R.id.phone_item_add_btn);
				viewHolder.btnAdded = (Button) convertView.findViewById(R.id.phone_item_added_btn);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}
			
			//过滤联系人中非手机号
			if(ClassPathResource.isMobileNO(phoneNum) == false)
			{
				return convertView;
			}
			
			viewHolder.tvTitle.setText(mContactsName.get(position));
			viewHolder.tvNumber.setText(phoneNum);
		//	viewHolder.ivAvatar.setImageBitmap(mContactsPhonto.get(position));
			if(mStatus.get(position).equals("need to add"))
			{
				viewHolder.btnAdd.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						mAddUser = phoneNum+"@" + PreferenceConstants.DEFAULT_SERVER_NAME;
						QueuePresenceThread thread = new QueuePresenceThread(phoneNum);
						thread.start();
					}
				});
			}
			else
			{
				viewHolder.btnAdd.setVisibility(View.GONE);
				viewHolder.btnAdded.setVisibility(View.VISIBLE);
			}
			return convertView;
		}
	}
	
	class ViewHolder{
		ImageView ivAvatar;
		TextView  tvTitle;
		TextView  tvNumber;
		Button	  btnAdd;
		Button    btnAdded;
	}
	
	private void updatePhoneInDB(RosterEntry rosterEntry, String photoHashGet)
	{
		final ContentValues values = new ContentValues();
		values.put(PhoneConstants.PHONE_NUM, photoHashGet);
		mContentResolver.update(AddPhonesProvider.CONTENT_URI, values, PhoneConstants.PHONE_NUM + " = ?", new String[]
		{ rosterEntry.getUser() });
		Log.i("Avatar Update", "OK");
	}

	private void updateConstantsInDB(String Name, String phoneNum)
	{
		final ContentValues values = getContentValuesForPhones(Name, phoneNum);
		if (mContentResolver.update(AddPhonesProvider.CONTENT_URI, values, PhoneConstants.PHONE_NUM + " = ?", new String[]
		{ phoneNum }) == 0)// 如果数据库无此号码
			addPhoneToDB(Name, phoneNum);// 则添加到数据库
		}

	private void addPhoneToDB(String Name, String phoneNum)
	{
		ContentValues values = getContentValuesForPhones(Name, phoneNum);
		Uri uri = mContentResolver.insert(AddPhonesProvider.CONTENT_URI, values);
		L.i("addPhoneToDB: Inserted " + uri);

	}

	private ContentValues getContentValuesForPhones(String Name, String phoneNum)
	{
		final ContentValues values = new ContentValues();
		
		values.put(PhoneConstants.PHONE_NUM, phoneNum);
		values.put(PhoneConstants.NAME, Name);
		
		return values;
	}
	
	
}