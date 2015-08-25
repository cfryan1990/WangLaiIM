package com.hdu.cfryan.activity;

import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;

import com.hdu.cfryan.R;
import com.hdu.cfryan.util.PreferenceConstants;
import com.hdu.cfryan.util.PreferenceUtils;
import com.hdu.cfryan.db.WanglaiDatabaseHelper;

public class SplashActivity extends Activity {
	private Handler mHandler;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mHandler = new Handler();
		String password = PreferenceUtils.getPrefString(this,
				PreferenceConstants.PASSWORD, "");
		if (!TextUtils.isEmpty(password)) {
			mHandler.postDelayed(gotoLoginAct, 1000);
			
		} else {
			
			mHandler.postDelayed(gotoMainAct, 3000);
		}
		
		setContentView(R.layout.splash);
	}

	Runnable gotoLoginAct = new Runnable() {

		@Override
		public void run() {
			
			startActivity(new Intent(SplashActivity.this, LoginActivity.class));
			finish();
		}
	};

	Runnable gotoMainAct = new Runnable() {

		@Override
		public void run() {
			startActivity(new Intent(SplashActivity.this, WanglaiMainActivity.class));
			finish();
		}
	};
}
