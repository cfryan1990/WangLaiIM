package com.hdu.cfryan.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.hdu.cfryan.R;
import com.hdu.cfryan.service.IConnectionStatusCallback;
import com.hdu.cfryan.service.XXService;
import com.hdu.cfryan.ui.view.CustomDialog;
import com.hdu.cfryan.util.ActivityManager;
import com.hdu.cfryan.util.DialogUtil;
import com.hdu.cfryan.ui.switcher.Switch;
import com.hdu.cfryan.util.L;
import com.hdu.cfryan.util.PreferenceConstants;
import com.hdu.cfryan.util.PreferenceUtils;
import com.hdu.cfryan.util.XMPPHelper;

public class SettingsActvity extends Activity implements OnClickListener,
		OnCheckedChangeListener, IConnectionStatusCallback{
	//private TextView mTitleNameView;
	private View mAccountSettingView;
	private ImageView mHeadIcon;
	private ImageView mStatusIcon;
	private TextView mStatusView;
	private TextView mNickView;
	private Switch mShowOfflineRosterSwitch;
	private Switch mNotifyRunBackgroundSwitch;
	private Switch mNewMsgSoundSwitch;
	private Switch mNewMsgVibratorSwitch;
	private Switch mNewMsgLedSwitch;
	private Switch mVisiableNewMsgSwitch;
	private Switch mShowHeadSwitch;
	private Switch mConnectionAutoSwitch;
	private Switch mPoweronReceiverMsgSwitch;
	private Switch mSendCrashSwitch;
	private View mFeedBackView;
	private View mAboutView;
	private Button mExitBtn;
	private View mExitMenuView;
	private Button mExitCancleBtn;
	private Button mExitConfirmBtn;
	private XXService mXxService;
	
	  @Override
	protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.activity_settings);
	        startService(new Intent(SettingsActvity.this, XXService.class));
			bindXMPPService();
	        InitView();
	        readData();
	  }
	  
	  ServiceConnection mServiceConnection = new ServiceConnection()
		{

			@Override
			public void onServiceConnected(ComponentName name, IBinder service)
			{
				mXxService = ((XXService.XXBinder) service).getService();
				mXxService.registerConnectionStatusCallback(SettingsActvity.this);
				// 开始连接xmpp服务器
			}

			@Override
			public void onServiceDisconnected(ComponentName name)
			{
				mXxService.unRegisterConnectionStatusCallback();
				mXxService = null;
			}

		};
		
		@Override
		protected void onDestroy()
		{
			super.onDestroy();
			unbindXMPPService();
		}

		private void unbindXMPPService()
		{
			try
			{
				unbindService(mServiceConnection);
				L.i(LoginActivity.class, "[SERVICE] Unbind");
			} catch (IllegalArgumentException e)
			{
				L.e(LoginActivity.class, "Service wasn't bound!");
			}
		}

		private void bindXMPPService()
		{
			L.i(LoginActivity.class, "[SERVICE] Unbind");
			Intent mServiceIntent = new Intent(this, XXService.class);
		//	mServiceIntent.setAction(LOGIN_ACTION);
			bindService(mServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE + Context.BIND_DEBUG_UNBIND);
		}
	
	public void InitView() {
		mExitMenuView = LayoutInflater.from(this).inflate(
				R.layout.common_menu_dialog_2btn_layout, null);
		
		mExitCancleBtn = (Button) mExitMenuView.findViewById(R.id.btnCancel);
		mExitConfirmBtn = (Button) mExitMenuView
				.findViewById(R.id.btn_exit_comfirm);
		mExitConfirmBtn.setText(R.string.exit);
		mExitCancleBtn.setOnClickListener(this);
		mExitConfirmBtn.setOnClickListener(this);
		
		//mTitleNameView = (TextView) view.findViewById(R.id.ivTitleName);
		//mTitleNameView.setText(R.string.settings_fragment_title);
		mAccountSettingView = findViewById(R.id.accountSetting);
		mAccountSettingView.setOnClickListener(this);
		mHeadIcon = (ImageView) findViewById(R.id.face);
		mStatusIcon = (ImageView) findViewById(R.id.statusIcon);
		mStatusView = (TextView) findViewById(R.id.status);
		mNickView = (TextView) findViewById(R.id.nick);
		mShowOfflineRosterSwitch = 
				(Switch) findViewById(R.id.show_offline_roster_switch);
		mShowOfflineRosterSwitch.setOnCheckedChangeListener(this);
		mNotifyRunBackgroundSwitch =
				(Switch)findViewById(R.id.notify_run_background_switch);
		mNotifyRunBackgroundSwitch.setOnCheckedChangeListener(this);
		mNewMsgSoundSwitch = 
				(Switch) findViewById(R.id.new_msg_sound_switch);
		mNewMsgSoundSwitch.setOnCheckedChangeListener(this);
		mNewMsgVibratorSwitch = 
				(Switch) findViewById(R.id.new_msg_vibrator_switch);
		mNewMsgSoundSwitch.setOnCheckedChangeListener(this);
		mNewMsgLedSwitch = 
				(Switch)findViewById(R.id.new_msg_led_switch);
		mNewMsgLedSwitch.setOnCheckedChangeListener(this);
		mVisiableNewMsgSwitch = 
				(Switch)findViewById(R.id.visiable_new_msg_switch);
		mVisiableNewMsgSwitch.setOnCheckedChangeListener(this);
		mShowHeadSwitch = 
				(Switch)findViewById(R.id.show_head_switch);
		mShowHeadSwitch.setOnCheckedChangeListener(this);
		mConnectionAutoSwitch = 
				(Switch)findViewById(R.id.connection_auto_switch);
		mConnectionAutoSwitch.setOnCheckedChangeListener(this);
		mPoweronReceiverMsgSwitch = 
				(Switch) findViewById(R.id.poweron_receiver_msg_switch);
		mPoweronReceiverMsgSwitch.setOnCheckedChangeListener(this);
		mSendCrashSwitch = 
				(Switch) findViewById(R.id.send_crash_switch);
		mSendCrashSwitch.setOnCheckedChangeListener(this);
		mFeedBackView = findViewById(R.id.set_feedback);
		mAboutView = findViewById(R.id.set_about);
		
		mExitBtn = (Button) findViewById(R.id.exit_app);
		mFeedBackView.setOnClickListener(this);
		mAboutView.setOnClickListener(this);
		mExitBtn.setOnClickListener(this);
		
	}

	@Override
	public void onResume() {
		super.onResume();
		readData();
	}

	public void readData() {
		mHeadIcon.setImageResource(R.drawable.ic_launcher);
//		mStatusIcon.setImageResource(MainActivity.mStatusMap
//				.get(PreferenceUtils.getPrefString(this,
//						PreferenceConstants.STATUS_MODE,
//						PreferenceConstants.AVAILABLE)));
		mStatusView.setText(PreferenceUtils.getPrefString(this,
				PreferenceConstants.STATUS_MESSAGE,
				getString(R.string.status_available)));
		mNickView
				.setText(XMPPHelper.splitJidAndServer(PreferenceUtils
						.getPrefString(this,
								PreferenceConstants.ACCOUNT, "")));
	//	mShowOfflineRosterSwitch.setChecked(PreferenceUtils.getPrefBoolean(
	//			this, PreferenceConstants.SHOW_OFFLINE, true));

		mNotifyRunBackgroundSwitch.setChecked(PreferenceUtils.getPrefBoolean(
				this, PreferenceConstants.FOREGROUND, true));
		mNewMsgSoundSwitch.setChecked(PreferenceUtils.getPrefBoolean(
				this, PreferenceConstants.SCLIENTNOTIFY, false));
		mNewMsgVibratorSwitch.setChecked(PreferenceUtils.getPrefBoolean(
				this, PreferenceConstants.VIBRATIONNOTIFY, true));
		mNewMsgLedSwitch.setChecked(PreferenceUtils.getPrefBoolean(
				this, PreferenceConstants.LEDNOTIFY, true));
		mVisiableNewMsgSwitch.setChecked(PreferenceUtils.getPrefBoolean(
				this, PreferenceConstants.TICKER, true));
		mShowHeadSwitch.setChecked(PreferenceUtils.getPrefBoolean(
				this, PreferenceConstants.SHOW_MY_HEAD, true));
		mConnectionAutoSwitch.setChecked(PreferenceUtils.getPrefBoolean(
				this, PreferenceConstants.AUTO_RECONNECT, true));
		mPoweronReceiverMsgSwitch.setChecked(PreferenceUtils.getPrefBoolean(
				this, PreferenceConstants.AUTO_START, true));
		mSendCrashSwitch.setChecked(PreferenceUtils.getPrefBoolean(
				this, PreferenceConstants.REPORT_CRASH, true));
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		switch (buttonView.getId()) {
		case R.id.show_offline_roster_switch:
			PreferenceUtils.setPrefBoolean(this,
					PreferenceConstants.SHOW_OFFLINE, isChecked);
		//	mFragmentCallBack.getMainActivity().updateRoster();
			break;
		case R.id.notify_run_background_switch:
			PreferenceUtils.setPrefBoolean(this,
					PreferenceConstants.FOREGROUND, isChecked);
			break;
		case R.id.new_msg_sound_switch:
			PreferenceUtils.setPrefBoolean(this,
					PreferenceConstants.SCLIENTNOTIFY, isChecked);
			break;
		case R.id.new_msg_vibrator_switch:
			PreferenceUtils.setPrefBoolean(this,
					PreferenceConstants.VIBRATIONNOTIFY, isChecked);
			break;
		case R.id.new_msg_led_switch:
			PreferenceUtils.setPrefBoolean(this,
					PreferenceConstants.LEDNOTIFY, isChecked);
			break;
		case R.id.visiable_new_msg_switch:
			PreferenceUtils.setPrefBoolean(this,
					PreferenceConstants.TICKER, isChecked);
			break;
		case R.id.show_head_switch:
			PreferenceUtils.setPrefBoolean(this,
					PreferenceConstants.SHOW_MY_HEAD, isChecked);
			break;
		case R.id.connection_auto_switch:
			PreferenceUtils.setPrefBoolean(this,
					PreferenceConstants.AUTO_RECONNECT, isChecked);
			break;
		case R.id.poweron_receiver_msg_switch:
			PreferenceUtils.setPrefBoolean(this,
					PreferenceConstants.AUTO_START, isChecked);
			break;
		case R.id.send_crash_switch:
			PreferenceUtils.setPrefBoolean(this,
					PreferenceConstants.REPORT_CRASH, isChecked);

			break;
		default:
			break;
		}
	}

	private Dialog mExitDialog;

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.set_feedback:
	//		startActivity(new Intent(this, FeedBackActivity.class));
			break;
		case R.id.set_about:
	//		startActivity(new Intent(this, AboutActivity.class));
			break;
		case R.id.exit_app: 
			if (mExitDialog == null)
				mExitDialog = DialogUtil.getMenuDialog(this,
						mExitMenuView);
			mExitDialog.show();
			break;
		case R.id.btnCancel:
			if (mExitDialog != null && mExitDialog.isShowing())
				mExitDialog.dismiss();
			break;
		case R.id.btn_exit_comfirm:
			
			if (mXxService != null) {
				mXxService.logout();// 注销
				mXxService.stopSelf();// 停止服务
			}
			if(mExitDialog.isShowing()){
				mExitDialog.cancel();
			}
			ActivityManager.getInstance().exit();
			this.finish();
			break;
		case R.id.accountSetting:
			logoutDialog();
			break;
		default:
			break;
		}
	}

	public void logoutDialog() {
		new CustomDialog.Builder(this)
				.setTitle(this.getString(R.string.open_switch_account))
				.setMessage(this.getString(R.string.open_switch_account_msg))
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								if (mXxService != null) {
									mXxService.logout();// 注销
								}
								dialog.dismiss();
								startActivity(new Intent(SettingsActvity.this,
										LoginActivity.class));
								ActivityManager.getInstance().exit();
								finish();
							}
						})
				.setNegativeButton(android.R.string.no,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						}).create().show();
	}

	@Override
	public void connectionStatusChanged(int connectedState, String reason)
	{
		// TODO Auto-generated method stub
		
	}
}
