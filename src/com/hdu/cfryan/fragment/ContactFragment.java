package com.hdu.cfryan.fragment;

import com.hdu.cfryan.R;
import com.hdu.cfryan.adapter.ContactAdapter;
import com.hdu.cfryan.adapter.ContactAdapter.ContactSortModel;
import com.hdu.cfryan.adapter.IndexBar;
import com.hdu.cfryan.adapter.IndexBar.OnTouchingLetterChangedListener;
import com.hdu.cfryan.service.IConnectionStatusCallback;
import com.hdu.cfryan.service.XXService;
import com.hdu.cfryan.smack.SmackImpl;
import com.hdu.cfryan.ui.view.ClearEditText;
import com.hdu.cfryan.util.L;
import com.hdu.cfryan.activity.LoginActivity;
import com.hdu.cfryan.activity.PhonesActivity;
import com.hdu.cfryan.activity.DetailInfoActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ContactFragment extends Fragment {
	private ListView mContactList;
	private IndexBar mIndexBar;
	private TextView mSelectLetterDialog;
	private ContactAdapter mContactAdapter;
	private ClearEditText mFilterEditText;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (container == null) {
			// Currently in a layout without a container, so no
			// reason to create our view.
			return null;
		}
		LayoutInflater myInflater = (LayoutInflater) getActivity()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = myInflater.inflate(R.layout.fragment_contact, container, false);
		initViews(layout);
		return layout;
	}
	
	public void initViews(View layout) {
		mIndexBar = (IndexBar) layout.findViewById(R.id.index_bar);
		mSelectLetterDialog = (TextView) layout
				.findViewById(R.id.select_letter_dialog);
		mIndexBar.setTextView(mSelectLetterDialog);
		// 设置右侧触摸监听
		mIndexBar
				.setOnTouchingLetterChangedListener(new OnTouchingLetterChangedListener() {
					
					@Override
					public void onTouchingLetterChanged(String s) {
						// 该字母首次出现的位置
						int position = mContactAdapter.getPositionForSection(s
								.charAt(0));
						if (position != -1) {
							mContactList.setSelection(position);
						}
					}
				});
		mContactList = (ListView) layout.findViewById(R.id.lv_contact_listview);
		mContactList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// 这里要利用adapter.getItem(position)来获取当前position所对应的对象
				String alias = ((ContactSortModel) mContactAdapter.getItem(position)).getRoster().getAlias();

				if("添加好友".equals(alias)){
					startPhoneActivity();
					return;
				}
				
				if("新朋友".equals(alias)){
//					startNewFriendsActivity();
					mContactAdapter.notifyDataSetChanged();
					Toast.makeText(getActivity(), alias, Toast.LENGTH_SHORT).show();
					return;
				}
				
				Toast.makeText(getActivity(), alias, Toast.LENGTH_SHORT).show();
				startDetailInfoActivity(((ContactSortModel) mContactAdapter
						.getItem(position)).getRoster().getJid(),
						alias);
			}
		});

		// 根据a-z进行排序源数据
		mContactAdapter = new ContactAdapter(getActivity(), true);
		
		mContactList.setAdapter(mContactAdapter);

		mFilterEditText = (ClearEditText) layout.findViewById(R.id.filter_edit);

		// 根据输入框输入值的改变来过滤搜索
		mFilterEditText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// 当输入框里面的值为空，更新为原来的列表，否则为过滤数据列表
				mContactAdapter.setFilterString(s.toString());
				mContactAdapter.notifyDataSetChanged();
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {

			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});
	}
	
	private void startPhoneActivity() {
		Intent intent = new Intent(getActivity(), PhonesActivity.class);
		startActivity(intent);
	}
	
	private void startDetailInfoActivity(String userJid, String alias) {

		Intent detailInfoIntent = new Intent(getActivity(),
				DetailInfoActivity.class);
		Uri userNameUri = Uri.parse(userJid);
		detailInfoIntent.setData(userNameUri);
		detailInfoIntent.putExtra(DetailInfoActivity.INTENT_EXTRA_USERNAME,
				alias);
		startActivity(detailInfoIntent);
	}
	
	public void refresh()
	{
		mContactAdapter.notifyDataSetChanged();
	}
	
}
