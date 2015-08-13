

package com.hdu.cfryan.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.hdu.cfryan.R;
import com.hdu.cfryan.db.WanglaiDatabaseHelper;
import com.hdu.cfryan.exception.XXAdressMalformedException;
import com.hdu.cfryan.service.IConnectionStatusCallback;
import com.hdu.cfryan.service.XXService;
import com.hdu.cfryan.util.DialogUtil;
import com.hdu.cfryan.util.L;
import com.hdu.cfryan.util.PreferenceConstants;
import com.hdu.cfryan.util.PreferenceUtils;
import com.hdu.cfryan.util.T;
import com.hdu.cfryan.util.XMPPHelper;

public class LoginActivity extends Activity implements
		IConnectionStatusCallback, TextWatcher ,OnClickListener{
	public static final String LOGIN_ACTION = "com.hdu.cfryan.action.LOGIN";
	private static final int LOGIN_OUT_TIME = 0;
	private Button mLoginBtn;
	private Button mRegistBtn;
	private EditText mAccountEt;
	private EditText mPasswordEt;
	private XXService mXxService;
	private Dialog mLoginDialog;
	private ConnectionOutTimeProcess mLoginOutTimeProcess;
	private String mAccount;
	private String mPassword;
	private TextView mTipsTextView;
	private Animation mTipsAnimation;
	

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case LOGIN_OUT_TIME:
				if (mLoginOutTimeProcess != null
						&& mLoginOutTimeProcess.running)
					mLoginOutTimeProcess.stop();
				if (mLoginDialog != null && mLoginDialog.isShowing())
					mLoginDialog.dismiss();
				T.showShort(LoginActivity.this, R.string.timeout_try_again);
				break;

			default:
				break;
			}
		}

	};
	
	ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mXxService = ((XXService.XXBinder) service).getService();
			mXxService.registerConnectionStatusCallback(LoginActivity.this);
			// 开始连接xmpp服务器
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mXxService.unRegisterConnectionStatusCallback();
			mXxService = null;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		startService(new Intent(LoginActivity.this, XXService.class));
		bindXMPPService();
		setContentView(R.layout.activity_login);
		initView();
	}

	@Override
	protected void onResume() {
		super.onResume();
//		if (TextUtils.equals(PreferenceUtils.getPrefString(this,
//				PreferenceConstants.APP_VERSION, ""),
//				getString(R.string.app_version))
//				&& !TextUtils.isEmpty(PreferenceUtils.getPrefString(this,
//						PreferenceConstants.ACCOUNT, ""))) {
//			//mTipsViewRoot.setVisibility(View.GONE);
//		} else {
//			// mTipsViewRoot.setVisibility(View.VISIBLE);
//			PreferenceUtils.setPrefString(this,
//					PreferenceConstants.APP_VERSION,
//					getString(R.string.app_version));
//		}
//		if (mTipsTextView != null && mTipsAnimation != null)
//			mTipsTextView.startAnimation(mTipsAnimation);
//		ChangeLog cl = new ChangeLog(this);
//		if (cl.firstRun()) {
//			cl.getFullLogDialog().show();
//		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mTipsTextView != null && mTipsAnimation != null)
			mTipsTextView.clearAnimation();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindXMPPService();
		if (mLoginOutTimeProcess != null) {
			mLoginOutTimeProcess.stop();
			mLoginOutTimeProcess = null;
		}
	}

	private void initView() {
		//mAutoSavePasswordCK = (CheckBox) findViewById(R.id.auto_save_password);
		//mHideLoginCK = (CheckBox) findViewById(R.id.hide_login);
		//mSilenceLoginCK = (CheckBox) findViewById(R.id.silence_login);
		//mUseTlsCK = (CheckBox) findViewById(R.id.use_tls);
		//mTipsViewRoot = findViewById(R.id.login_help_view);
		mAccountEt = (EditText) findViewById(R.id.account_input);
		mPasswordEt = (EditText) findViewById(R.id.password);
		mLoginBtn = (Button) findViewById(R.id.login);
		mRegistBtn = (Button) findViewById(R.id.regist);
		String account = PreferenceUtils.getPrefString(this,
				PreferenceConstants.ACCOUNT, "");
		String password = PreferenceUtils.getPrefString(this,
				PreferenceConstants.PASSWORD, "");
		if (!TextUtils.isEmpty(account))
			mAccountEt.setText(account);
		if (!TextUtils.isEmpty(password))
			mPasswordEt.setText(password);
		mAccountEt.addTextChangedListener(this);
		mLoginDialog = DialogUtil.getLoginDialog(this);
		mLoginOutTimeProcess = new ConnectionOutTimeProcess();
		mRegistBtn.setOnClickListener(this);
	}

	public void onLoginClick(View v) {
		mAccount = mAccountEt.getText().toString();
		mAccount = splitAndSaveServer(mAccount);
		mPassword = mPasswordEt.getText().toString();
		if (TextUtils.isEmpty(mAccount)) {
			T.showShort(this, R.string.null_account_prompt);
			return;
		}
		if (TextUtils.isEmpty(mPassword)) {
			T.showShort(this, R.string.password_input_prompt);
			return;
		}
		if (mLoginOutTimeProcess != null && !mLoginOutTimeProcess.running)
			mLoginOutTimeProcess.start();
		if (mLoginDialog != null && !mLoginDialog.isShowing())
			mLoginDialog.show();
		if (mXxService != null) {
			mXxService.Login(mAccount, mPassword);
		}
	}

	private String splitAndSaveServer(String account) {
		if (!account.contains("@"))
			return account;
		String customServer = PreferenceUtils.getPrefString(this,
				PreferenceConstants.CUSTOM_SERVER, "");
		String[] res = account.split("@");
		String userName = res[0];
		String server = res[1];
		// check for gmail.com and other google hosted jabber accounts
		if ("gmail.com".equals(server) || "googlemail.com".equals(server)
				|| PreferenceConstants.DEFAULT_SERVER.equals(customServer)) {
			// work around for gmail's incompatible jabber implementation:
			// send the whole JID as the login, connect to talk.google.com
			userName = account;

		}
		PreferenceUtils.setPrefString(this, PreferenceConstants.Server, server);
		return userName;
	}

	private void unbindXMPPService() {
		try {
			unbindService(mServiceConnection);
			L.i(LoginActivity.class, "[SERVICE] Unbind");
		} catch (IllegalArgumentException e) {
			L.e(LoginActivity.class, "Service wasn't bound!");
		}
	}

	private void bindXMPPService() {
		L.i(LoginActivity.class, "[SERVICE] Unbind");
		Intent mServiceIntent = new Intent(this, XXService.class);
		mServiceIntent.setAction(LOGIN_ACTION);
		bindService(mServiceIntent, mServiceConnection,
				Context.BIND_AUTO_CREATE + Context.BIND_DEBUG_UNBIND);
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}

	@Override
	public void afterTextChanged(Editable s) {
		try {
			XMPPHelper.verifyJabberID(s);
			mLoginBtn.setEnabled(true);
			mAccountEt.setTextColor(Color.parseColor("#ff333333"));
		} catch (XXAdressMalformedException e) {
			mLoginBtn.setEnabled(false);
			mAccountEt.setTextColor(Color.RED);
		}
	}

	private void save2Preferences() {
		boolean isAutoSavePassword = true;//mAutoSavePasswordCK.isChecked();
		boolean isUseTls = false;//mUseTlsCK.isChecked();
		boolean isSilenceLogin = false;//mSilenceLoginCK.isChecked();
		boolean isHideLogin = false;//mHideLoginCK.isChecked();
		PreferenceUtils.setPrefString(this, PreferenceConstants.ACCOUNT,
				mAccount);// 帐号是一直保存的
		if (isAutoSavePassword)
			PreferenceUtils.setPrefString(this, PreferenceConstants.PASSWORD,
					mPassword);
		else
			PreferenceUtils.setPrefString(this, PreferenceConstants.PASSWORD,
					"");

		PreferenceUtils.setPrefBoolean(this, PreferenceConstants.REQUIRE_TLS,
				isUseTls);
		PreferenceUtils.setPrefBoolean(this, PreferenceConstants.SCLIENTNOTIFY,
				isSilenceLogin);
		if (isHideLogin)
			PreferenceUtils.setPrefString(this,
					PreferenceConstants.STATUS_MODE, PreferenceConstants.XA);
		else
			PreferenceUtils.setPrefString(this,
					PreferenceConstants.STATUS_MODE,
					PreferenceConstants.AVAILABLE);
	}

	// 登录超时处理线程
	class ConnectionOutTimeProcess implements Runnable {
		public boolean running = false;
		private long startTime = 0L;
		private Thread thread = null;

		ConnectionOutTimeProcess() {
		}

		@Override
		public void run() {
			while (true) {
				if (!this.running)
					return;
				if (System.currentTimeMillis() - this.startTime > 20 * 1000L) {
					mHandler.sendEmptyMessage(LOGIN_OUT_TIME);
				}
				try {
					Thread.sleep(10L);
				} catch (Exception localException) {
				}
			}
		}

		public void start() {
			try {
				this.thread = new Thread(this);
				this.running = true;
				this.startTime = System.currentTimeMillis();
				this.thread.start();
			} finally {
			}
		}

		public void stop() {
			try {
				this.running = false;
				this.thread = null;
				this.startTime = 0L;
			} finally {
			}
		}
	}

	@Override
	public void connectionStatusChanged(int connectedState, String reason) {
		// TODO Auto-generated method stub
		if (mLoginDialog != null && mLoginDialog.isShowing())
			mLoginDialog.dismiss();
		if (mLoginOutTimeProcess != null && mLoginOutTimeProcess.running) {
			mLoginOutTimeProcess.stop();
			mLoginOutTimeProcess = null;
		}
		if (connectedState == XXService.CONNECTED) {
			save2Preferences();
			startActivity(new Intent(this, WanglaiMainActivity.class));
			finish();
		} else if (connectedState == XXService.DISCONNECTED)
			T.showLong(LoginActivity.this, getString(R.string.request_failed)
					+ reason);
	}

	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
		case R.id.regist:
	//		Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
	//		startActivity(intent);
			break;

		}
		
	}
}
