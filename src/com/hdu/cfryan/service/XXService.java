package com.hdu.cfryan.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.packet.VCard;

import android.R.integer;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.hdu.cfryan.R;
//import com.zjstone.activity.BaseActivity;
import com.hdu.cfryan.activity.LoginActivity;
//import com.zjstone.activity.BaseActivity.BackPressHandler;
import com.hdu.cfryan.activity.WanglaiMainActivity;
import com.hdu.cfryan.application.XXBroadcastReceiver;
import com.hdu.cfryan.application.XXBroadcastReceiver.EventHandler;
import com.hdu.cfryan.db.WanglaiDatabaseHelper;
import com.hdu.cfryan.exception.XXException;
import com.hdu.cfryan.smack.SmackImpl;
import com.hdu.cfryan.util.ImageTools;
import com.hdu.cfryan.util.L;
import com.hdu.cfryan.util.NetUtil;
import com.hdu.cfryan.util.PreferenceConstants;
import com.hdu.cfryan.util.PreferenceUtils;
import com.hdu.cfryan.util.T;

public class XXService extends BaseService implements EventHandler//, BackPressHandler
{
	public static final int CONNECTED = 0;
	public static final int DISCONNECTED = -1;
	public static final int CONNECTING = 1;
	public static final int AVATARFINISHED = 0;
	public static final int AVATARUNFINISHED = -1;
	public static final String PONG_TIMEOUT = "pong timeout";// ���ӳ�ʱ
	public static final String NETWORK_ERROR = "network error";// �������
	public static final String LOGOUT = "logout";// �ֶ��˳�
	public static final String LOGIN_FAILED = "login failed";// ��¼ʧ��
	public static final String DISCONNECTED_WITHOUT_WARNING = "disconnected without warning";// û�о���ĶϿ�����

	private IBinder mBinder = new XXBinder();
	private IConnectionStatusCallback mConnectionStatusCallback;
	private static IAvatarStatusCallback mAvatarStatusCallback;
	private SmackImpl mSmackable;
	private Thread mConnectingThread;
	private static Handler mMainHandler = new Handler();

	private boolean mIsFirstLoginAction;
	// �Զ����� start
	private static final int RECONNECT_AFTER = 5;
	private static final int RECONNECT_MAXIMUM = 10 * 60;// �������ʱ����
	private static final String RECONNECT_ALARM = "com.zjstone.RECONNECT_ALARM";
	// private boolean mIsNeedReConnection = false; // �Ƿ���Ҫ����
	private int mConnectedState = DISCONNECTED; // �Ƿ��Ѿ�����
	private int mReconnectTimeout = RECONNECT_AFTER;
	private Intent mAlarmIntent = new Intent(RECONNECT_ALARM);
	private PendingIntent mPAlarmIntent;
	private BroadcastReceiver mAlarmReceiver = new ReconnectAlarmReceiver();
	// �Զ����� end
	private ActivityManager mActivityManager;
	private HashSet<String> mIsBoundTo = new HashSet<String>();// �������浱ǰ����������������

	/**
	 * ע��ע������������ʱ����״̬�仯�ص�
	 * 
	 * @param cb
	 */
	public void registerConnectionStatusCallback(IConnectionStatusCallback cb)
	{
		mConnectionStatusCallback = cb;
	}

	public void unRegisterConnectionStatusCallback()
	{
		mConnectionStatusCallback = null;
	}
	
	
	/**
	 * ע��ͷ��״̬�仯�ص�
	 * @param cb
	 */
	public void registerAvatarStatusCallback(IAvatarStatusCallback cb)
	{
		mAvatarStatusCallback = cb;
	}

	public void unRegisterAvatarStatusCallback()
	{
		mAvatarStatusCallback = null;
	}
	
	

	@Override
	public IBinder onBind(Intent intent)
	{
		L.i(XXService.class, "[SERVICE] onBind");
		String chatPartner = intent.getDataString();
		if ((chatPartner != null))
		{
			mIsBoundTo.add(chatPartner);
		}
		String action = intent.getAction();
		if (!TextUtils.isEmpty(action) && TextUtils.equals(action, LoginActivity.LOGIN_ACTION))
		{
			mIsFirstLoginAction = true;
		} else
		{
			mIsFirstLoginAction = false;
		}
		return mBinder;
	}

	@Override
	public void onRebind(Intent intent)
	{
		super.onRebind(intent);
		String chatPartner = intent.getDataString();
		if ((chatPartner != null))
		{
			mIsBoundTo.add(chatPartner);
		}
		String action = intent.getAction();
		if (!TextUtils.isEmpty(action) && TextUtils.equals(action, LoginActivity.LOGIN_ACTION))
		{
			mIsFirstLoginAction = true;
		} else
		{
			mIsFirstLoginAction = false;
		}
	}

	@Override
	public boolean onUnbind(Intent intent)
	{
		String chatPartner = intent.getDataString();
		if ((chatPartner != null))
		{
			mIsBoundTo.remove(chatPartner);
		}
		return true;
	}

	public class XXBinder extends Binder
	{
		public XXService getService()
		{
			return XXService.this;
		}
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		XXBroadcastReceiver.mListeners.add(this);
	//	BaseActivity.mListeners.add(this);
		mActivityManager = ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE));
		mPAlarmIntent = PendingIntent.getBroadcast(this, 0, mAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		registerReceiver(mAlarmReceiver, new IntentFilter(RECONNECT_ALARM));
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		if (intent != null && intent.getAction() != null && TextUtils.equals(intent.getAction(), XXBroadcastReceiver.BOOT_COMPLETED_ACTION))
		{
			String account = PreferenceUtils.getPrefString(XXService.this, PreferenceConstants.ACCOUNT, "");
			String password = PreferenceUtils.getPrefString(XXService.this, PreferenceConstants.PASSWORD, "");
			if (!TextUtils.isEmpty(account) && !TextUtils.isEmpty(password))
				Login(account, password);
		}
		mMainHandler.removeCallbacks(monitorStatus);
		mMainHandler.postDelayed(monitorStatus, 1000L);// ���Ӧ���Ƿ��ں�̨�����߳�
		return START_STICKY;
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		XXBroadcastReceiver.mListeners.remove(this);
	//	BaseActivity.mListeners.remove(this);
		((AlarmManager) getSystemService(Context.ALARM_SERVICE)).cancel(mPAlarmIntent);// ȡ����������
		unregisterReceiver(mAlarmReceiver);// ע���㲥����
		logout();
	}

	// ��¼
	public void Login(final String account, final String password)
	{
		if (NetUtil.getNetworkState(this) == NetUtil.NETWORN_NONE)
		{
			connectionFailed(NETWORK_ERROR);
			return;
		}
		if (mConnectingThread != null)
		{
			L.i("a connection is still goign on!");
			return;
		}
		mConnectingThread = new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					postConnecting();
					mSmackable = new SmackImpl(XXService.this);
					if (mSmackable.login(account, password))
					{
						// ��½�ɹ�
						postConnectionScuessed();
						
						mSmackable.initAvatar();
						new GetPhoneContactsThread().start();
					} else
					{
						// ��½ʧ��
						postConnectionFailed(LOGIN_FAILED);
					}
				} catch (XXException e)
				{
					String message = e.getLocalizedMessage();
					// ��½ʧ��
					if (e.getCause() != null)
						message += "\n" + e.getCause().getLocalizedMessage();
					postConnectionFailed(message);
					L.i(XXService.class, "YaximXMPPException in doConnect():");
					e.printStackTrace();
				} finally
				{
					if (mConnectingThread != null)
						synchronized (mConnectingThread)
						{
							mConnectingThread = null;
						}
				}
			}
		};
		mConnectingThread.start();
	}

	// ע��
	public int regist(final String account, final String password)
	{
		int registstate = 0;
		if (NetUtil.getNetworkState(this) == NetUtil.NETWORN_NONE)
		{
			connectionFailed(NETWORK_ERROR);
		}

		mSmackable = new SmackImpl(XXService.this);
		String state = mSmackable.regist(account, password);

		if (state.equals("1"))
		{
			registstate = 1;
		} else if (state.equals("2"))
		{
			registstate = 2;
		} else if (state.equals("3"))
		{
			registstate = 3;
		}
		return registstate;
	}

	public void changeImage(final Bitmap f)
	{
		if (NetUtil.getNetworkState(this) == NetUtil.NETWORN_NONE)
		{
			connectionFailed(NETWORK_ERROR);
			return;
		}
		Thread t = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					mSmackable = new SmackImpl(XXService.this);
					if (!mSmackable.isAuthenticated())
					{
						mSmackable.login(PreferenceUtils.getPrefString(getApplicationContext(), PreferenceConstants.ACCOUNT, ""),
								PreferenceUtils.getPrefString(getApplicationContext(), PreferenceConstants.PASSWORD, ""));
					}

				} catch (XXException e1)
				{
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				try
				{
					if(mSmackable.setUserImage(ImageTools.bitmapToBytes(f)))
					{
						handler.sendEmptyMessage(1);
					}
					else
					{
						handler.sendEmptyMessage(0);
					}
					
				} catch (XMPPException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		t.start();

	}

	// �˳�
	public boolean logout()
	{
		// mIsNeedReConnection = false;// �ֶ��˳��Ͳ���Ҫ����������
		boolean isLogout = false;
		if (mConnectingThread != null)
		{
			synchronized (mConnectingThread)
			{
				try
				{
					mConnectingThread.interrupt();
					mConnectingThread.join(50);
				} catch (InterruptedException e)
				{
					L.e("doDisconnect: failed catching connecting thread");
				} finally
				{
					mConnectingThread = null;
				}
			}
		}
		if (mSmackable != null)
		{
			isLogout = mSmackable.logout();
			mSmackable = null;
		}
		connectionFailed(LOGOUT);// �ֶ��˳�
		return isLogout;
	}

	// ������Ϣ
	public void sendMessage(Message message, int ds, Boolean compress)
	{
		if (mSmackable != null)
		{
			mSmackable.sendMessage(message, ds, compress);
		} else
		{
			Log.e("net", "failed");
			SmackImpl.saveAsOfflineMessage(getContentResolver(), message);
		}
	}

	// �Ƿ������Ϸ�����
	public boolean isAuthenticated()
	{
		if (mSmackable != null)
		{
			return mSmackable.isAuthenticated();
		}

		return false;
	}

	// ���֪ͨ��
	public void clearNotifications(String Jid)
	{
		clearNotification(Jid);
	}

	/**
	 * ��UI�߳�����ʧ�ܷ���
	 * 
	 * @param reason
	 */
	public void postConnectionFailed(final String reason)
	{
		mMainHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				connectionFailed(reason);
			}
		});
	}

	// ��������״̬
	public void setStatusFromConfig()
	{
		mSmackable.setStatusFromConfig();
	}

	// ������ϵ��
	public void addRosterItem(String user, String alias, String group)
	{
		if (!mSmackable.isAuthenticated())
		{
			Toast.makeText(getApplicationContext(), "δ���ӷ������������µ�½��", Toast.LENGTH_LONG).show();
			try
			{
				mSmackable.login(PreferenceUtils.getPrefString(getApplicationContext(), PreferenceConstants.ACCOUNT, ""),
						PreferenceUtils.getPrefString(getApplicationContext(), PreferenceConstants.PASSWORD, ""));
			} catch (XXException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		try
		{
			mSmackable.addRosterItem(user, alias, group);
			requestAuthorizationForRosterItem(user);
			
		} catch (XXException e)
		{
			T.showShort(this, e.getMessage());
			L.e("exception in addRosterItem(): " + e.getMessage());
		}
	}


	// ɾ����ϵ��
	public void removeRosterItem(String user)
	{
		try
		{
			mSmackable.removeRosterItem(user);
		} catch (XXException e)
		{
			T.showShort(this, e.getMessage());
			L.e("exception in removeRosterItem(): " + e.getMessage());
		}
	}

	// ��������ϵ��
	public void renameRosterItem(String user, String newName)
	{
		try
		{
			mSmackable.renameRosterItem(user, newName);
		} catch (XXException e)
		{
			T.showShort(this, e.getMessage());
			L.e("exception in renameRosterItem(): " + e.getMessage());
		}
	}

	public void requestAuthorizationForRosterItem(String user)
	{
		mSmackable.requestAuthorizationForRosterItem(user);
	}

	/**
	 * UI�̷߳�������ʧ��
	 * 
	 * @param reason
	 */
	private void connectionFailed(String reason)
	{
		L.i(XXService.class, "connectionFailed: " + reason);
		mConnectedState = DISCONNECTED;// ���µ�ǰ����״̬
		if (mSmackable != null)
			mSmackable.setStatusOffline();// ��������ϵ�˱��Ϊ����
		if (TextUtils.equals(reason, LOGOUT))
		{// ������ֶ��˳�
			((AlarmManager) getSystemService(Context.ALARM_SERVICE)).cancel(mPAlarmIntent);
			return;
		}
		// �ص�
		if (mConnectionStatusCallback != null)
		{
			mConnectionStatusCallback.connectionStatusChanged(mConnectedState, reason);
			if (mIsFirstLoginAction)// ����ǵ�һ�ε�¼,�����¼ʧ��Ҳ����Ҫ����
				return;
		}

		// ����������ʱ,ֱ�ӷ���
		if (NetUtil.getNetworkState(this) == NetUtil.NETWORN_NONE)
		{
			((AlarmManager) getSystemService(Context.ALARM_SERVICE)).cancel(mPAlarmIntent);
			return;
		}

		String account = PreferenceUtils.getPrefString(XXService.this, PreferenceConstants.ACCOUNT, "");
		String password = PreferenceUtils.getPrefString(XXService.this, PreferenceConstants.PASSWORD, "");
		// �ޱ�����ʺ�����ʱ��Ҳֱ�ӷ���
		if (TextUtils.isEmpty(account) || TextUtils.isEmpty(password))
		{
			L.d("account = null || password = null");
			return;
		}
		// ��������ֶ��˳�������Ҫ�������ӣ�������������
		if (PreferenceUtils.getPrefBoolean(this, PreferenceConstants.AUTO_RECONNECT, true))
		{
			L.d("connectionFailed(): registering reconnect in " + mReconnectTimeout + "s");
			((AlarmManager) getSystemService(Context.ALARM_SERVICE)).set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + mReconnectTimeout
					* 1000, mPAlarmIntent);
			mReconnectTimeout = mReconnectTimeout * 2;
			if (mReconnectTimeout > RECONNECT_MAXIMUM)
				mReconnectTimeout = RECONNECT_MAXIMUM;
		} else
		{
			((AlarmManager) getSystemService(Context.ALARM_SERVICE)).cancel(mPAlarmIntent);
		}

	}

	private void postConnectionScuessed()
	{
		mMainHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				connectionScuessed();
			}

		});
	}
	
	private static void postAvatarDownloadFinished()
	{
		mMainHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				avatarDownloadFinished();
			}

		});
	}

	private void connectionScuessed()
	{
		mConnectedState = CONNECTED;// �Ѿ�������
		mReconnectTimeout = RECONNECT_AFTER;// ����������ʱ��

		if (mConnectionStatusCallback != null)
			mConnectionStatusCallback.connectionStatusChanged(mConnectedState, "");
	}
	
	private static void avatarDownloadFinished()
	{

		if (mAvatarStatusCallback != null)
			mAvatarStatusCallback.AvatarStatusChanged(AVATARFINISHED);
	}

	// �����У�֪ͨ�����߳���һЩ����
	private void postConnecting()
	{
		// TODO Auto-generated method stub
		mMainHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				connecting();
			}
		});
	}

	private void connecting()
	{
		// TODO Auto-generated method stub
		mConnectedState = CONNECTING;// ������
		if (mConnectionStatusCallback != null)
			mConnectionStatusCallback.connectionStatusChanged(mConnectedState, "");
	}

	// �յ�����Ϣ
	public void newMessage(final String from, final String message)
	{
		mMainHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (!PreferenceUtils.getPrefBoolean(XXService.this, PreferenceConstants.SCLIENTNOTIFY, false))
					MediaPlayer.create(XXService.this, R.raw.office).start();
				if (!isAppOnForeground())
					notifyClient(from, mSmackable.getNameForJID(from), message, !mIsBoundTo.contains(from));
				 T.showLong(XXService.this, from + ": " + message);

			}

		});
	}

	// ��ϵ�˸ı�
	public void rosterChanged()
	{
		// gracefully handle^W ignore events after a disconnect
		if (mSmackable == null)
			return;
		if (mSmackable != null && !mSmackable.isAuthenticated())
		{
			L.i("rosterChanged(): disconnected without warning");
			connectionFailed(DISCONNECTED_WITHOUT_WARNING);
		}
	}

	/**
	 * ����֪ͨ��
	 * 
	 * @param message
	 */
	public void updateServiceNotification(String message)
	{
		if (!PreferenceUtils.getPrefBoolean(this, PreferenceConstants.FOREGROUND, true))
			return;
		String title = PreferenceUtils.getPrefString(this, PreferenceConstants.ACCOUNT, "");
		Notification n = new Notification(R.drawable.login_default_avatar, title, System.currentTimeMillis());
		n.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;

		Intent notificationIntent = new Intent(this, WanglaiMainActivity.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		n.contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		n.setLatestEventInfo(this, title, message, n.contentIntent);
		startForeground(SERVICE_NOTIFICATION, n);
	}

	// �жϳ����Ƿ��ں�̨���е�����
	Runnable monitorStatus = new Runnable()
	{
		@Override
		public void run()
		{
			try
			{
				L.i("monitorStatus is running... " + getPackageName());
				mMainHandler.removeCallbacks(monitorStatus);
				// ����ں�̨���в�����������
				if (!isAppOnForeground())
				{
					L.i("app run in background...");
					// if (isAuthenticated())
					updateServiceNotification(getString(R.string.run_bg_ticker));
					return;
				} else
				{
					stopForeground(true);
				}
				// mMainHandler.postDelayed(monitorStatus, 1000L);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	};

	public boolean isAppOnForeground()
	{
		List<RunningTaskInfo> taskInfos = mActivityManager.getRunningTasks(1);
		if (taskInfos.size() > 0 && TextUtils.equals(getPackageName(), taskInfos.get(0).topActivity.getPackageName()))
		{
			return true;
		}

		// List<RunningAppProcessInfo> appProcesses = mActivityManager
		// .getRunningAppProcesses();
		// if (appProcesses == null)
		// return false;
		// for (RunningAppProcessInfo appProcess : appProcesses) {
		// // L.i("liweiping", appProcess.processName);
		// // The name of the process that this object is associated with.
		// if (appProcess.processName.equals(mPackageName)
		// && appProcess.importance ==
		// RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
		// return true;
		// }
		// }
		return false;
	}

	// �Զ������㲥
	private class ReconnectAlarmReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context ctx, Intent i)
		{
			L.d("Alarm received.");
			if (!PreferenceUtils.getPrefBoolean(XXService.this, PreferenceConstants.AUTO_RECONNECT, true))
			{
				return;
			}
			if (mConnectedState != DISCONNECTED)
			{
				L.d("Reconnect attempt aborted: we are connected again!");
				return;
			}
			String account = PreferenceUtils.getPrefString(XXService.this, PreferenceConstants.ACCOUNT, "");
			String password = PreferenceUtils.getPrefString(XXService.this, PreferenceConstants.PASSWORD, "");
			if (TextUtils.isEmpty(account) || TextUtils.isEmpty(password))
			{
				L.d("account = null || password = null");
				return;
			}
			Login(account, password);
		}
	}

	@Override
	public void onNetChange()
	{
		if (NetUtil.getNetworkState(this) == NetUtil.NETWORN_NONE)
		{// ���������Ͽ�����������
			connectionFailed(NETWORK_ERROR);
			return;
		}
		if (isAuthenticated())// ����Ѿ������ϣ�ֱ�ӷ���
			return;
		String account = PreferenceUtils.getPrefString(XXService.this, PreferenceConstants.ACCOUNT, "");
		String password = PreferenceUtils.getPrefString(XXService.this, PreferenceConstants.PASSWORD, "");
		if (TextUtils.isEmpty(account) || TextUtils.isEmpty(password))// ���û���ʺţ�Ҳֱ�ӷ���
			return;
		if (!PreferenceUtils.getPrefBoolean(this, PreferenceConstants.AUTO_RECONNECT, true))// ����Ҫ����
			return;
		Login(account, password);// ����
	}

//	@Override
//	public void activityOnResume()
//	{
//		L.i("activity onResume ...");
//		mMainHandler.post(monitorStatus);
//	}
//
//	@Override
//	public void activityOnPause()
//	{
//		L.i("activity onPause ...");
//		mMainHandler.postDelayed(monitorStatus, 1000L);
//	}

	private static byte[] getFileBytes(File file) throws IOException
	{
		BufferedInputStream bis = null;
		try
		{
			bis = new BufferedInputStream(new FileInputStream(file));
			int bytes = (int) file.length();
			byte[] buffer = new byte[bytes];
			int readBytes = bis.read(buffer);
			if (readBytes != buffer.length)
			{
				throw new IOException("Entire file not read");
			}
			return buffer;
		} finally
		{
			if (bis != null)
			{
				bis.close();
			}
		}
	}
	
	//ͷ���ϴ�״̬��handle
	private Handler handler = new Handler()
	{
		@Override
		public void handleMessage(android.os.Message msg)
		{

			if(msg.what == 0)
			{
				Toast.makeText(getApplicationContext(), "ͷ���ϴ�ʧ�ܣ�", Toast.LENGTH_SHORT).show();
			}
			else if(msg.what == 1)
			{
				Toast.makeText(getApplicationContext(), "ͷ���ϴ��ɹ���", Toast.LENGTH_SHORT).show();
			}
			
		};
	};
	
	private class GetPhoneContactsThread extends Thread {

		@Override
		public void run() {
			mSmackable.getPhoneContacts();
		}
	}
	
	public static void getVcard(final VCard vcard, final String userString)
	{
		Thread t = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				FileOutputStream out = null;
				try
				{
					out = new FileOutputStream("mnt/sdcard/Wanglai/Avatar/" + userString + ".png");
					out.write(vcard.getAvatar());
					out.close();
					postAvatarDownloadFinished();
					
				} catch (Exception e)
				{
					e.printStackTrace();
				}

			}
		});
		t.start();
	}

}
