package com.hdu.cfryan.fragment;

import com.hdu.cfryan.R;
import com.hdu.cfryan.activity.SettingsActvity;
//import com.zjstone.activity.SettingsActvity;
//import com.zjstone.activity.meInfoActivity;
import com.hdu.cfryan.util.PreferenceConstants;
import com.hdu.cfryan.util.PreferenceUtils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

public class MeFragment  extends Fragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onAttach(Activity activity) {
		// TODO Auto-generated method stub
		super.onAttach(activity);
	}

	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (container == null){
			return null;
		}
		
		final String usr = PreferenceUtils.getPrefString(getActivity(),
				PreferenceConstants.ACCOUNT, "");
		
		View layout = inflater.inflate(R.layout.fragment_me, container,	false);
		TextView name = (TextView) layout.findViewById(R.id.tv_me_alias);
		name.setText(usr);
		View setting = layout.findViewById(R.id.me_settings);
		setting.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				startActivity(new Intent(getActivity(), SettingsActvity.class));
			}
		});
		
		View me = layout.findViewById(R.id.me_detail);
		me.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
		//		startActivity(new Intent(getActivity(), meInfoActivity.class));
			}
		});
		
		return layout;
	}
	
}
