package com.hdu.cfryan.smack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Registration;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.carbons.Carbon;
import org.jivesoftware.smackx.carbons.CarbonManager;
import org.jivesoftware.smackx.forward.Forwarded;
import org.jivesoftware.smackx.packet.DelayInfo;
import org.jivesoftware.smackx.packet.DelayInformation;
import org.jivesoftware.smackx.packet.VCard;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.ping.packet.Ping;
import org.jivesoftware.smackx.ping.provider.PingProvider;
import org.jivesoftware.smackx.provider.DelayInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.VCardProvider;
import org.jivesoftware.smackx.receipts.DeliveryReceipt;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.text.TextUtils;
import android.util.Log;

import com.hdu.cfryan.exception.XXException;
import com.hdu.cfryan.fragment.ContactFragment;
import com.hdu.cfryan.service.XXService;
import com.hdu.cfryan.R;
import com.hdu.cfryan.activity.WanglaiMainActivity;
import com.hdu.cfryan.adapter.ContactAdapter;
import com.hdu.cfryan.db.RosterProvider;
import com.hdu.cfryan.db.WanglaiDatabaseHelper;
import com.hdu.cfryan.db.RosterProvider.RosterConstants;
import com.hdu.cfryan.db.ChatProvider;
import com.hdu.cfryan.db.ChatProvider.ChatConstants;
import com.hdu.cfryan.db.AvatarProvider;
import com.hdu.cfryan.db.AvatarProvider.AvatarConstants;
import com.hdu.cfryan.db.AddPhonesProvider;
import com.hdu.cfryan.db.AddPhonesProvider.PhoneConstants;
import com.hdu.cfryan.db.NewFriendsProvider;
import com.hdu.cfryan.db.NewFriendsProvider.NewFriendsConstants;
import com.hdu.cfryan.util.FileUtils;
import com.hdu.cfryan.util.HttpDownloader;
import com.hdu.cfryan.util.HttpUploader;
import com.hdu.cfryan.util.PreferenceConstants;
import com.hdu.cfryan.util.PreferenceUtils;
import com.hdu.cfryan.util.StatusMode;
import com.hdu.cfryan.util.L;

public class SmackImpl implements Smack
{
	// �ͻ������ƺ����͡���Ҫ����������Ǽǣ��е�����QQ��ʾiphone����Android�ֻ����ߵĹ���
	public static final String XMPP_IDENTITY_NAME = "XMPP";// �ͻ�������
	public static final String XMPP_IDENTITY_TYPE = "phone";// �ͻ�������

	private static final int PACKET_TIMEOUT = 30000;// ��ʱʱ��
	// ����������Ϣ���ֶ�
	final static private String[] SEND_OFFLINE_PROJECTION = new String[]
	{ ChatConstants._ID, ChatConstants.JID, ChatConstants.MESSAGE, ChatConstants.DATE, ChatConstants.MEDIA_TYPE, ChatConstants.MEDIA_URL,
			ChatConstants.MEDIA_SIZE, ChatConstants.PACKET_ID };
	// ����������Ϣ���������ݿ��������Լ�����ȥ��OUTGOING������״̬ΪDS_NEW
	final static private String SEND_OFFLINE_SELECTION = ChatConstants.DIRECTION + " = " + ChatConstants.OUTGOING + " AND "
			+ ChatConstants.DELIVERY_STATUS + " = " + ChatConstants.DS_NEW;

	static//�౻���ص�ʱ��ִ���ҽ���ִ��һ��
	{
		registerSmackProviders();
	}

	// ��һЩ����������
	static void registerSmackProviders()
	{
		ProviderManager pm = ProviderManager.getInstance();
		// add IQ handling
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#info", new DiscoverInfoProvider());
		// add delayed delivery notifications
		pm.addExtensionProvider("delay", "urn:xmpp:delay", new DelayInfoProvider());
		pm.addExtensionProvider("x", "jabber:x:delay", new DelayInfoProvider());
		// add carbons and forwarding
		pm.addExtensionProvider("forwarded", Forwarded.NAMESPACE, new Forwarded.Provider());
		pm.addExtensionProvider("sent", Carbon.NAMESPACE, new Carbon.Provider());
		pm.addExtensionProvider("received", Carbon.NAMESPACE, new Carbon.Provider());
		// add delivery receipts
		pm.addExtensionProvider(DeliveryReceipt.ELEMENT, DeliveryReceipt.NAMESPACE, new DeliveryReceipt.Provider());
		pm.addExtensionProvider(DeliveryReceiptRequest.ELEMENT, DeliveryReceipt.NAMESPACE, new DeliveryReceiptRequest.Provider());
		// add my message extension
		// pm.addExtensionProvider(MessagePacketExtension.ELEMENT,
		// MessagePacketExtension.NAMESPACE, new
		// MessagePacketExtensionProvider());

		// add XMPP Ping (XEP-0199)
		pm.addIQProvider("ping", "urn:xmpp:ping", new PingProvider());
		// VCard
		pm.addIQProvider("vCard", "vcard-temp", new VCardProvider());

		ServiceDiscoveryManager.setIdentityName(XMPP_IDENTITY_NAME);
		ServiceDiscoveryManager.setIdentityType(XMPP_IDENTITY_TYPE);
	}

	private ConnectionConfiguration mXMPPConfig;// ��������
	private XMPPConnection mXMPPConnection;// ���Ӷ���
	private static XXService mService;// ������
	private Roster mRoster;// ��ϵ�˶���
	private final ContentResolver mContentResolver;// ���ݿ��������
	private final ContentResolver resolver;//ͨѶ¼��������

	private RosterListener mRosterListener;// ��ϵ�˶�̬����
	private PacketListener mPacketListener;// ��Ϣ��̬����
	private PacketListener mSendFailureListener;// ��Ϣ����ʧ�ܶ�̬����
	private PacketListener mPongListener;// ping pong��������̬����
	private PacketListener mAvatarListener;// Avatar�Ķ�̬����
	private PacketListener mAddListener;//������Ѽ���

	// ping-pong������
	private String mPingID;// ping��������id
	private long mPingTimestamp;// ʱ���
	private PendingIntent mPingAlarmPendIntent;// ��ͨ������������ping��������ʱ����
	private PendingIntent mPongTimeoutAlarmPendIntent;// �жϷ��������ӳ�ʱ������
	private static final String PING_ALARM = "com.zjstone.PING_ALARM";// ping����������BroadcastReceiver��Action
	private static final String PONG_TIMEOUT_ALARM = "com.zjstone.PONG_TIMEOUT_ALARM";// �ж����ӳ�ʱ������BroadcastReceiver��Action
	private Intent mPingAlarmIntent = new Intent(PING_ALARM);
	private Intent mPongTimeoutAlarmIntent = new Intent(PONG_TIMEOUT_ALARM);
	private PongTimeoutAlarmReceiver mPongTimeoutAlarmReceiver = new PongTimeoutAlarmReceiver();
	private BroadcastReceiver mPingAlarmReceiver = new PingAlarmReceiver();

	private boolean mIsShowOffline;// �Ƿ���ʾ������ϵ��
	// ������״̬
	private static final String OFFLINE_EXCLUSION = RosterConstants.STATUS_MODE + " != " + StatusMode.offline.ordinal();
	// ��ϵ�˲�ѯ����
	private static final String[] ROSTER_QUERY = new String[]
	{ RosterConstants._ID, RosterConstants.JID, RosterConstants.ALIAS, RosterConstants.STATUS_MODE, RosterConstants.STATUS_MESSAGE, };
	private static final String[] AVATAR_QUERY = new String[]
	{ AvatarConstants._ID, AvatarConstants.JID, AvatarConstants.ALIAS, AvatarConstants.PHOTO_HASH };
	private static final String[] PHONE_QUERY = new String[]
	{ PhoneConstants._ID, PhoneConstants.PHONE_NUM, PhoneConstants.NAME };
	
	private static final String[] PHONES_PROJECTION = new String[] {
		Phone.DISPLAY_NAME, Phone.NUMBER, Photo.PHOTO_ID, Phone.CONTACT_ID };

	private static final int PHONES_DISPLAY_NAME_INDEX = 0;
	private static final int PHONES_NUMBER_INDEX = 1;
	private static final int PHONES_PHOTO_ID_INDEX = 2;
	private static final int PHONES_CONTACT_ID_INDEX = 3;
	
	// ping-pong������

	public SmackImpl(XXService service)
	{
		String customServer = PreferenceUtils.getPrefString(service, PreferenceConstants.CUSTOM_SERVER, "");// �û��ֶ����õķ��������ƣ�����������û�ָ����������
		int port = PreferenceUtils.getPrefInt(service, PreferenceConstants.PORT, PreferenceConstants.DEFAULT_PORT_INT);// �˿ںţ�Ҳ�������û��ֶ����õ�
		String server = PreferenceUtils.getPrefString(service, PreferenceConstants.Server, PreferenceConstants.DEFAULT_SERVER);// Ĭ�ϵķ����������ȸ������
		boolean smackdebug = PreferenceUtils.getPrefBoolean(service, PreferenceConstants.SMACKDEBUG, false);// �Ƿ���Ҫsmack
																											// debug
		boolean requireSsl = PreferenceUtils.getPrefBoolean(service, PreferenceConstants.REQUIRE_TLS, false);// �Ƿ���Ҫssl��ȫ����
		if (customServer.length() > 0 || port != PreferenceConstants.DEFAULT_PORT_INT)
			this.mXMPPConfig = new ConnectionConfiguration(customServer, port, server);
		else
			this.mXMPPConfig = new ConnectionConfiguration(server); // use SRV

		this.mXMPPConfig.setReconnectionAllowed(true);
		this.mXMPPConfig.setSendPresence(true);
		this.mXMPPConfig.setCompressionEnabled(false); // disable for now
		this.mXMPPConfig.setDebuggerEnabled(true/* smackdebug */);
		//�����java.security.KeyStoreException: KeyStore jks implementation not found������
		this.mXMPPConfig.setTruststorePath("/system/etc/security/cacerts.bks");
		this.mXMPPConfig.setTruststorePassword("changeit");
		this.mXMPPConfig.setTruststoreType("bks");
		if (requireSsl)
			this.mXMPPConfig.setSecurityMode(ConnectionConfiguration.SecurityMode.required);

		this.mXMPPConnection = new XMPPConnection(mXMPPConfig);
		this.mService = service;
		mContentResolver = service.getContentResolver();
		resolver = service.getContentResolver();
	}
	

	/**
	 * ��½
	 */
	@Override
	public boolean login(String account, String password) throws XXException
	{
		// ��½ʵ��
		try
		{
			if (mXMPPConnection.isConnected())
			{// �����ж��Ƿ������ŷ���������Ҫ�ȶϿ�
				try
				{
					mXMPPConnection.disconnect();
				} catch (Exception e)
				{
					L.d("conn.disconnect() failed: " + e);
				}
			}
			SmackConfiguration.setPacketReplyTimeout(PACKET_TIMEOUT);// ���ó�ʱʱ��
			SmackConfiguration.setKeepAliveInterval(-1);
			SmackConfiguration.setDefaultPingInterval(0);
			registerRosterListener();// ������ϵ�˶�̬�仯
			mXMPPConnection.connect();
			if (!mXMPPConnection.isConnected())
			{
				throw new XXException("SMACK connect failed without exception!");
			}
			mXMPPConnection.addConnectionListener(new ConnectionListener()
			{
				@Override
				public void connectionClosedOnError(Exception e)
				{
					mService.postConnectionFailed(e.getMessage());// ���ӹر�ʱ����̬����������
				}

				@Override
				public void connectionClosed()
				{
				}

				@Override
				public void reconnectingIn(int seconds)
				{
				}

				@Override
				public void reconnectionFailed(Exception e)
				{
				}

				@Override
				public void reconnectionSuccessful()
				{
				}
			});
			initServiceDiscovery();// �������������Ϣ����,������Ϣ��Ҫ��ִ���ж��Ƿ��ͳɹ�
			// SMACK auto-logins if we were authenticated before
			if (!mXMPPConnection.isAuthenticated())
			{
				String ressource = PreferenceUtils.getPrefString(mService, PreferenceConstants.RESSOURCE, XMPP_IDENTITY_NAME);
				mXMPPConnection.login(account, password, ressource);
			}
			setStatusFromConfig();// ��������״̬

		} catch (XMPPException e)
		{
			throw new XXException(e.getLocalizedMessage(), e.getWrappedThrowable());
		} catch (Exception e)
		{
			// actually we just care for IllegalState or NullPointer or XMPPEx.
			L.e(SmackImpl.class, "login(): " + Log.getStackTraceString(e));
			throw new XXException(e.getLocalizedMessage(), e.getCause());
		}
		registerAllListener();// ע������������¼�����������Ϣ
		return mXMPPConnection.isAuthenticated();
	}

	/**
	 * ע��
	 * 
	 * @param account
	 *            ע���ʺ�
	 * @param password
	 *            ע������
	 * @return 1��ע��ɹ� 0��������û�з��ؽ��2������˺��Ѿ�����3��ע��ʧ��
	 */
	public String regist(String account, String password)
	{
		try
		{
			if (mXMPPConnection.isConnected())
			{// �����ж��Ƿ������ŷ���������Ҫ�ȶϿ�
				try
				{
					mXMPPConnection.disconnect();
				} catch (Exception e)
				{
					L.d("conn.disconnect() failed: " + e);
				}
			}
			mXMPPConnection.connect();
		} catch (XMPPException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (mXMPPConnection == null)
			return "0";
		Registration reg = new Registration();
		reg.setType(IQ.Type.SET);
		reg.setTo(mXMPPConnection.getServiceName());
		reg.setUsername(account);// ע������createAccountע��ʱ��������username������jid���ǡ�@��ǰ��Ĳ��֡�
		reg.setPassword(password);
		reg.addAttribute("android", "geolo_createUser_android");// ���addAttribute����Ϊ�գ������������������־��android�ֻ������İɣ���������
		PacketFilter filter = new AndFilter(new PacketIDFilter(reg.getPacketID()), new PacketTypeFilter(IQ.class));
		PacketCollector collector = mXMPPConnection.createPacketCollector(filter);
		mXMPPConnection.sendPacket(reg);
		IQ result = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
		// Stop queuing results
		collector.cancel();// ֹͣ����results���Ƿ�ɹ��Ľ����
		if (result == null)
		{
			Log.e("RegistActivity", "No response from server.");
			return "0";
		} else if (result.getType() == IQ.Type.RESULT)
		{
			return "1";
		} else
		{ // if (result.getType() == IQ.Type.ERROR)
			if (result.getError().toString().equalsIgnoreCase("conflict(409)"))
			{
				Log.e("RegistActivity", "IQ.Type.ERROR: " + result.getError().toString());
				return "2";
			} else
			{
				Log.e("RegistActivity", "IQ.Type.ERROR: " + result.getError().toString());
				return "3";
			}
		}
	}

	/**
	 * ע�����еļ���
	 */
	private void registerAllListener()
	{
		// actually, authenticated must be true now, or an exception must have
		// been thrown.
		if (isAuthenticated())
		{
			registerMessageListener();// ע������Ϣ����
			registerMessageSendFailureListener();// ע����Ϣ����ʧ�ܼ���
			registerPongListener();// ע���������Ӧping��Ϣ����
			registerAvatarListener();// ע��ͷ����¼���
			registerAddLinster();//ע�������Ӽ���
			
			sendOfflineMessages();// ����������Ϣ
			if (mService == null)
			{
				mXMPPConnection.disconnect();
				return;
			}
			// we need to "ping" the service to let it know we are actually
			// connected, even when no roster entries will come in
			mService.rosterChanged();
		}
	}

	/************ start ����Ϣ���� ********************/
	/************ ��Ҫ������Ĵ������ݰ��Ĳ�ͬ���ͣ��ı������ԡ�ͼƬ���ļ� ***************/
	private void registerMessageListener()
	{
		// do not register multiple packet listeners
		if (mPacketListener != null)
			mXMPPConnection.removePacketListener(mPacketListener);

		PacketTypeFilter filter = new PacketTypeFilter(Message.class);

		mPacketListener = new PacketListener()
		{
			@Override
			public void processPacket(Packet packet)
			{
				try
				{
					if (packet instanceof Message)
					{// �������Ϣ����
						Message msg = (Message) packet;
						String mediaType = (String) msg.getProperty(ChatConstants.MEDIA_TYPE);
						String chatMessage = msg.getBody();
						Log.i("Message:", "receive a msg!");

						// try to extract a carbon
						Carbon cc = CarbonManager.getCarbon(msg);
						if (cc != null && cc.getDirection() == Carbon.Direction.received)
						{// �յ�����Ϣ
							L.d("carbon: " + cc.toXML());
							msg = (Message) cc.getForwarded().getForwardedPacket();
							chatMessage = msg.getBody();
							Log.i("Receive Message:", chatMessage);

							// fall through
						} else if (cc != null && cc.getDirection() == Carbon.Direction.sent)
						{// ������Լ����͵���Ϣ������ӵ����ݿ��ֱ�ӷ���
							L.d("carbon: " + cc.toXML());
							msg = (Message) cc.getForwarded().getForwardedPacket();
							chatMessage = msg.getBody();
							if (chatMessage == null)
								return;

							Log.i("Send Message:", chatMessage);

							addChatMessageToDB(ChatConstants.OUTGOING, msg, ChatConstants.DS_SENT_OR_READ, System.currentTimeMillis(),
									msg.getPacketID());

							return;// �ǵ�Ҫ����
						}

						if (chatMessage == null && !isMultimediaPkt(mediaType))
						{
							return;// �����ϢΪ�գ�ֱ�ӷ�����
						}

						if (msg.getType() == Message.Type.error)
						{
							chatMessage = "<Error> " + chatMessage;// �������Ϣ����
						}

						long ts;// ��Ϣʱ���
						DelayInfo timestamp = (DelayInfo) msg.getExtension("delay", "urn:xmpp:delay");
						if (timestamp == null)
							timestamp = (DelayInfo) msg.getExtension("x", "jabber:x:delay");
						if (timestamp != null)
							ts = timestamp.getStamp().getTime();
						else
							ts = System.currentTimeMillis();

						if (isMultimediaPkt(mediaType))
						{
							String mediaUrl = (String) msg.getProperty(ChatConstants.MEDIA_URL);
							if (mediaUrl != null)
								Log.i("receive a media msg", mediaUrl);
							StartReceiveMediaFile(msg, ts, mediaType);
						} else
						{
							addChatMessageToDB(ChatConstants.INCOMING, msg, ChatConstants.DS_NEW, ts, msg.getPacketID());

							String fromJID = getJabberID(msg.getFrom());// ��Ϣ���Զ���
							mService.newMessage(fromJID, chatMessage);// ֪ͨservice�������Ƿ���Ҫ��ʾ֪ͨ����
						}

					}
				} catch (Exception e)
				{
					// SMACK silently discards exceptions dropped from
					// processPacket :(
					L.e("failed to process packet:");
					e.printStackTrace();
				}
			}
		};

		mXMPPConnection.addPacketListener(mPacketListener, filter);// ������ؽ����ˣ�������䣬ǰ��Ķ��ǰ׷ѹ���
	}

	private class DownloadImgThread extends Thread
	{
		private Message mMsg;
		private long mTs;

		public DownloadImgThread(Message msg, long ts)
		{
			mMsg = msg;
			mTs = ts;
		}

		@Override
		public void run()
		{
			try
			{
				String fileUrl = (String) mMsg.getProperty(ChatConstants.MEDIA_URL);
				String dirName = "wanglai_in" + File.separator + "Images" + File.separator;
				String fileName = fileUrl.substring(fileUrl.lastIndexOf(File.separator) + 1);

				int result = HttpDownloader.downloadImage(fileUrl, dirName, fileName);
				if (result == 0)
				{
					Log.i("File path", new FileUtils().getSDPATH() + dirName + fileName);
					mMsg.setProperty(ChatConstants.MEDIA_URL, new FileUtils().getSDPATH() + dirName + fileName);
					addChatMessageToDB(ChatConstants.INCOMING, mMsg, ChatConstants.DS_DOWNLOAD_SUCCESS, mTs, mMsg.getPacketID());

					String fromJID = mMsg.getFrom();
					mService.newMessage(fromJID, ChatConstants.MEDIA_TYPE_IMAGE);
				} else
				{
					addChatMessageToDB(ChatConstants.INCOMING, mMsg, ChatConstants.DS_DOWNLOAD_FAILED, mTs, mMsg.getPacketID());
				}
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private class DownloadFileThread extends Thread
	{
		private Message mMsg;
		private long mTs;
		private String mMediaType;

		public DownloadFileThread(Message msg, long ts, String mediaType)
		{
			mMsg = msg;
			mTs = ts;
			mMediaType = mediaType;
		}

		@Override
		public void run()
		{
			try
			{
				String packetid = mMsg.getPacketID();
				String fileUrl = (String) mMsg.getProperty(ChatConstants.MEDIA_URL);
				String fileName = fileUrl.substring(fileUrl.lastIndexOf(File.separator) + 1);
				String dirName = null;
				if (ChatConstants.MEDIA_TYPE_AUDIO.equals(mMediaType))
				{
					dirName = "wanglai_in" + File.separator + "Audio" + File.separator;
				} else if (ChatConstants.MEDIA_TYPE_FILE.equals(mMediaType))
				{
					dirName = "wanglai_in" + File.separator + "Files" + File.separator;
				}

				int result = HttpDownloader.downloadFile(fileUrl, dirName, fileName);
				if (result == 0)
				{
					changeMessageDeliveryStatus(packetid,ChatConstants.DS_DOWNLOAD_SUCCESS);
					//media_url����ļ��ڱ��ص�ȫ·��
					mMsg.setProperty(ChatConstants.MEDIA_URL, android.os.Environment.getExternalStorageDirectory() + "/" + dirName + fileName);
					addChatMessageToDB(ChatConstants.INCOMING, mMsg, ChatConstants.DS_DOWNLOAD_SUCCESS, mTs, mMsg.getPacketID());
					String fromJID = mMsg.getFrom();
					mService.newMessage(fromJID, mMediaType);
				} else
				{
					changeMessageDeliveryStatus(packetid, ChatConstants.DS_DOWNLOAD_FAILED);
					addChatMessageToDB(ChatConstants.INCOMING, mMsg, ChatConstants.DS_DOWNLOAD_FAILED, mTs, mMsg.getPacketID());
				}
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}

	}

	protected void StartReceiveMediaFile(Message msg, long ts, String mediaType)
	{
		if (ChatConstants.MEDIA_TYPE_IMAGE.equals(mediaType))
		{
			DownloadImgThread thread = new DownloadImgThread(msg, ts);
			thread.start();
		} else if (ChatConstants.MEDIA_TYPE_AUDIO.equals(mediaType))
		{
			DownloadFileThread thread = new DownloadFileThread(msg, ts, ChatConstants.MEDIA_TYPE_AUDIO);
			thread.start();
		} else if (ChatConstants.MEDIA_TYPE_FILE.equals(mediaType))
		{
			addChatMessageToDB(ChatConstants.INCOMING, msg, ChatConstants.DS_DOWNLOAD_READY, ts, msg.getPacketID());
		//	DownloadFileThread thread = new DownloadFileThread(msg, ts, ChatConstants.MEDIA_TYPE_FILE);
		//	thread.start();
		}
		
	}

	/**
	 * ����Ϣ��ӵ����ݿ�
	 * 
	 * @param direction
	 *            �Ƿ�Ϊ�յ�����ϢINCOMINGΪ�յ���OUTGOINGΪ�Լ�����
	 * @param JID
	 *            ����Ϣ��Ӧ��jid
	 * @param message
	 *            ��Ϣ����
	 * @param delivery_status
	 *            ��Ϣ״̬ DS_NEWΪ����Ϣ��DS_SENT_OR_READΪ�Լ����������Ѷ�����Ϣ
	 * @param ts
	 *            ��Ϣʱ���
	 * @param packetID
	 *            ������Ϊ������ÿһ����Ϣ���ɵ���Ϣ����id
	 */
	private void addChatMessageToDB(int direction, Message message, int delivery_status, long ts, String packetID)
	{
		ContentValues values = new ContentValues();

		String jid = new String();
		String mediaUrl = null;
		String mediaSize = null;

		String mediaType = (String) message.getProperty("mediaType");
		if (!ChatConstants.MEDIA_TYPE_NORMAL.equals(mediaType))
		{
			mediaUrl = (String) message.getProperty("mediaUrl");
			mediaSize = (String) message.getProperty("mediaSize");
		}

		if (direction == ChatConstants.OUTGOING)
		{
			jid = getJabberID(message.getTo());
		} else
		{
			jid = getJabberID(message.getFrom());
		}

		values.put(ChatConstants.DIRECTION, direction);
		values.put(ChatConstants.JID, jid);

		values.put(ChatConstants.MESSAGE, message.getBody());
		values.put(ChatConstants.DELIVERY_STATUS, delivery_status);
		values.put(ChatConstants.MEDIA_TYPE, mediaType);
		values.put(ChatConstants.MEDIA_URL, mediaUrl);
		values.put(ChatConstants.MEDIA_SIZE, mediaSize);
		values.put(ChatConstants.DATE, ts);
		values.put(ChatConstants.PACKET_ID, packetID);

		mContentResolver.insert(ChatProvider.CONTENT_URI, values);
	}

	/************ end ����Ϣ���� ********************/

	
	/***************** start ������Ϣ����ʧ��״̬ ***********************/
	private void registerMessageSendFailureListener()
	{
		// do not register multiple packet listeners
		if (mSendFailureListener != null)
			mXMPPConnection.removePacketSendFailureListener(mSendFailureListener);

		PacketTypeFilter filter = new PacketTypeFilter(Message.class);

		mSendFailureListener = new PacketListener()
		{
			@Override
			public void processPacket(Packet packet)
			{
				try
				{
					if (packet instanceof Message)
					{
						Message msg = (Message) packet;
						String chatMessage = msg.getBody();

						Log.d("SmackableImp",
								"message " + chatMessage + " could not be sent (ID:" + (msg.getPacketID() == null ? "null" : msg.getPacketID()) + ")");
						changeMessageDeliveryStatus(msg.getPacketID(), ChatConstants.DS_NEW);// ����Ϣ����ʧ��ʱ��������Ϣ���Ϊ����Ϣ���´��ٷ���
					}
				} catch (Exception e)
				{
					// SMACK silently discards exceptions dropped from
					// processPacket :(
					L.e("failed to process packet:");
					e.printStackTrace();
				}
			}
		};

		mXMPPConnection.addPacketSendFailureListener(mSendFailureListener, filter);// ���Ҳ�ǹؼ�����
	}

	/**
	 * �ı���Ϣ״̬
	 * 
	 * @param packetID
	 *            ��Ϣ��id
	 * @param new_status
	 *            ��״̬����
	 */
	public void changeMessageDeliveryStatus(String packetID, int new_status)
	{
		ContentValues cv = new ContentValues();
		cv.put(ChatConstants.DELIVERY_STATUS, new_status);
		Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/" + PreferenceConstants.TABLE_CHATS);
		mContentResolver.update(rowuri, cv, ChatConstants.PACKET_ID + " = ? AND " + ChatConstants.DIRECTION + " = " + ChatConstants.OUTGOING,
				new String[]
				{ packetID });
	}

	/**
	 * �ı���Ϣ״̬
	 * 
	 * @param packetID
	 *            ��Ϣ��id
	 * @param new_status
	 *            ��״̬����
	 */
	public void changeMessageDeliveryStatusAndURL(String packetID, int new_status, String newURL)
	{
		ContentValues cv = new ContentValues();
		cv.put(ChatConstants.DELIVERY_STATUS, new_status);
		cv.put(ChatConstants.MEDIA_URL, newURL);
		Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/" + PreferenceConstants.TABLE_CHATS);
		mContentResolver.update(rowuri, cv, ChatConstants.PACKET_ID + " = ? AND " + ChatConstants.DIRECTION + " = " + ChatConstants.OUTGOING,
				new String[]
				{ packetID });
	}

	/**
	 * �ı���Ϣ״̬
	 * 
	 * @param packetID
	 *            ��Ϣ��id
	 * @param new_status
	 *            ��״̬����
	 */
	public void changeUploaingMediaMessageDeliveryStatus(String packetID, int new_status)
	{
		ContentValues cv = new ContentValues();
		cv.put(ChatConstants.DELIVERY_STATUS, new_status);
		Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/" + PreferenceConstants.TABLE_CHATS);
		mContentResolver.update(rowuri, cv, ChatConstants.PACKET_ID + " = ? AND " + ChatConstants.DIRECTION + " = " + ChatConstants.OUTGOING,
				new String[]
				{ packetID });
	}

	/***************** end ������Ϣ����ʧ��״̬ ***********************/

	
	/***************** start ����ping��������Ϣ ***********************/
	private void registerPongListener()
	{
		// reset ping expectation on new connection
		mPingID = null;// ��ʼ��ping��id

		if (mPongListener != null)
			mXMPPConnection.removePacketListener(mPongListener);// ���Ƴ�֮ǰ��������

		mPongListener = new PacketListener()
		{

			@Override
			public void processPacket(Packet packet)
			{
				if (packet == null)
					return;

				if (packet.getPacketID().equals(mPingID))
				{// ������������ص���ϢΪping������ʱ����Ϣ��˵��û�е���
					L.i(String.format("Ping: server latency %1.3fs", (System.currentTimeMillis() - mPingTimestamp) / 1000.));
					mPingID = null;
					((AlarmManager) mService.getSystemService(Context.ALARM_SERVICE)).cancel(mPongTimeoutAlarmPendIntent);// ȡ����ʱ����
				}
			}

		};

		mXMPPConnection.addPacketListener(mPongListener, new PacketTypeFilter(IQ.class));// ��ʽ��ʼ����
		mPingAlarmPendIntent = PendingIntent.getBroadcast(mService.getApplicationContext(), 0, mPingAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);// ��ʱping���������Դ���ȷ���Ƿ����
		mPongTimeoutAlarmPendIntent = PendingIntent.getBroadcast(mService.getApplicationContext(), 0, mPongTimeoutAlarmIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);// ��ʱ����
		mService.registerReceiver(mPingAlarmReceiver, new IntentFilter(PING_ALARM));// ע�ᶨʱping�������㲥������
		mService.registerReceiver(mPongTimeoutAlarmReceiver, new IntentFilter(PONG_TIMEOUT_ALARM));// ע�����ӳ�ʱ�㲥������
		((AlarmManager) mService.getSystemService(Context.ALARM_SERVICE)).setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
				+ AlarmManager.INTERVAL_FIFTEEN_MINUTES, AlarmManager.INTERVAL_FIFTEEN_MINUTES, mPingAlarmPendIntent);// 15����ping�Դ˷�����
	}

	/**
	 * BroadcastReceiver to trigger reconnect on pong timeout.
	 */
	private class PongTimeoutAlarmReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context ctx, Intent i)
		{
			L.d("Ping: timeout for " + mPingID);
			mService.postConnectionFailed(XXService.PONG_TIMEOUT);
			logout();// ��ʱ�ͶϿ�����
		}
	}

	/**
	 * BroadcastReceiver to trigger sending pings to the server
	 */
	private class PingAlarmReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context ctx, Intent i)
		{
			if (mXMPPConnection.isAuthenticated())
			{
				sendServerPing();// �յ�ping�����������ӣ���pingһ�·�����
			} else
				L.d("Ping: alarm received, but not connected to server.");
		}
	}

	/***************** end ����ping��������Ϣ ***********************/

	
	/***************** start ����������Ϣ ***********************/
	public void sendOfflineMessage(Message message)
	{
		long ts = 0;
		String packetID = null;
		int _id = 0;

		ContentValues mark_sent = new ContentValues();
		mark_sent.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_SENT_OR_READ);

		DelayInformation delay = new DelayInformation(new Date(ts));
		message.addExtension(delay);
		message.addExtension(new DelayInfo(delay));
		message.addExtension(new DeliveryReceiptRequest());

		if ((packetID != null) && (packetID.length() > 0))
		{
			message.setPacketID(packetID);
		} else
		{
			packetID = message.getPacketID();
			mark_sent.put(ChatConstants.PACKET_ID, packetID);
		}

		Uri rowUri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/" + PreferenceConstants.TABLE_CHATS + "/" + _id);
		// ����Ϣ���Ϊ�ѷ����ٵ��÷��ͣ���Ϊ���������Ϣ��δ���ͳɹ�����SendFailListener���±����Ϣ
		mContentResolver.update(rowUri, mark_sent, null, null);
		mXMPPConnection.sendPacket(message); // must be after marking
												// delivered, otherwise it
												// may override the
												// SendFailListener
	}

	/***************** start ����������Ϣ ***********************/
	public void sendOfflineMessages()
	{
		Cursor cursor = mContentResolver.query(ChatProvider.CONTENT_URI, SEND_OFFLINE_PROJECTION, SEND_OFFLINE_SELECTION, null, null);// ��ѯ���ݿ��ȡ������Ϣ�α�

		final int _ID_COL = cursor.getColumnIndexOrThrow(BaseColumns._ID);
		final int JID_COL = cursor.getColumnIndexOrThrow(ChatConstants.JID);
		final int MSG_COL = cursor.getColumnIndexOrThrow(ChatConstants.MESSAGE);
		final int TS_COL = cursor.getColumnIndexOrThrow(ChatConstants.DATE);
		final int MEDIA_TYPE_COL = cursor.getColumnIndexOrThrow(ChatConstants.MEDIA_TYPE);
		final int MEDIA_URL_COL = cursor.getColumnIndexOrThrow(ChatConstants.MEDIA_URL);
		final int MEDIA_SIZE_COL = cursor.getColumnIndexOrThrow(ChatConstants.MEDIA_SIZE);
		final int PACKETID_COL = cursor.getColumnIndexOrThrow(ChatConstants.PACKET_ID);

		ContentValues mark_sent = new ContentValues();
		mark_sent.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_SENT_OR_READ);
		while (cursor.moveToNext())
		{// ����֮��������Ϣ����
			int _id = cursor.getInt(_ID_COL);
			String toJID = cursor.getString(JID_COL);
			String body = cursor.getString(MSG_COL);
			String packetID = cursor.getString(PACKETID_COL);
			long ts = cursor.getLong(TS_COL);
			L.d("sendOfflineMessages: " + toJID + " > " + body);

			Message message = new Message(toJID, Message.Type.chat);
			message.setBody(body);
			String mediaType = cursor.getString(MEDIA_TYPE_COL);
			message.setProperty(ChatConstants.MEDIA_TYPE, cursor.getString(MEDIA_TYPE_COL));
			if (!mediaType.equals("Normal"))
			{
				message.setProperty(ChatConstants.MEDIA_URL, cursor.getString(MEDIA_URL_COL));

				message.setProperty(ChatConstants.MEDIA_SIZE, cursor.getString(MEDIA_SIZE_COL));
			}

			DelayInformation delay = new DelayInformation(new Date(ts));
			message.addExtension(delay);
			message.addExtension(new DelayInfo(delay));
			message.addExtension(new DeliveryReceiptRequest());

			if ((packetID != null) && (packetID.length() > 0))
			{
				message.setPacketID(packetID);
			} else
			{
				packetID = message.getPacketID();
				mark_sent.put(ChatConstants.PACKET_ID, packetID);
			}

			Uri rowUri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/" + PreferenceConstants.TABLE_CHATS + "/" + _id);
			// ����Ϣ���Ϊ�ѷ����ٵ��÷��ͣ���Ϊ���������Ϣ��δ���ͳɹ�����SendFailListener���±����Ϣ
			mContentResolver.update(rowUri, mark_sent, null, null);
			mXMPPConnection.sendPacket(message); // must be after marking
													// delivered, otherwise it
													// may override the
													// SendFailListener
		}
		cursor.close();
	}
	
	/**
	 * ��Ϊ������Ϣ�洢���������Լ�����ʱ����
	 * 
	 * @param cr
	 * @param toJID
	 * @param message
	 */
	public static void saveAsOfflineMessage(ContentResolver cr, String toJID, String message)
	{
		ContentValues values = new ContentValues();
		values.put(ChatConstants.DIRECTION, ChatConstants.OUTGOING);
		values.put(ChatConstants.JID, toJID);
		values.put(ChatConstants.MESSAGE, message);
		values.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_NEW);
		values.put(ChatConstants.DATE, System.currentTimeMillis());

		cr.insert(ChatProvider.CONTENT_URI, values);
	}

	public static void saveAsOfflineMessage(ContentResolver cr, Message message)
	{
		/*
		 * values.put(ChatConstants.DIRECTION, direction);
		 * values.put(ChatConstants.JID, jid);
		 * 
		 * values.put(ChatConstants.MESSAGE, message.getBody());
		 * values.put(ChatConstants.DELIVERY_STATUS, delivery_status);
		 * values.put(ChatConstants.MESSAGE_TYPE, mediaType);
		 * values.put(ChatConstants.URL, mediaUrl);
		 * values.put(ChatConstants.SIZE, mediaSize);
		 * values.put(ChatConstants.DATE, ts);
		 * values.put(ChatConstants.PACKET_ID, packetID);
		 */

		ContentValues values = new ContentValues();
		values.put(ChatConstants.DIRECTION, ChatConstants.OUTGOING);
		values.put(ChatConstants.JID, message.getTo());
		values.put(ChatConstants.MESSAGE, message.getBody());
		values.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_NEW);
		values.put(ChatConstants.MEDIA_TYPE, (String) message.getProperty("mediaType"));
		values.put(ChatConstants.MEDIA_URL, (String) message.getProperty("mediaUrl"));
		values.put(ChatConstants.MEDIA_SIZE, (String) message.getProperty("mediaSize"));
		values.put(ChatConstants.DATE, System.currentTimeMillis());

		cr.insert(ChatProvider.CONTENT_URI, values);
	}

	public static void saveImageMessageOnUploading(ContentResolver cr, Message message, int ds)
	{
		ContentValues values = new ContentValues();
		values.put(ChatConstants.DIRECTION, ChatConstants.OUTGOING);
		values.put(ChatConstants.JID, message.getTo());
		values.put(ChatConstants.MESSAGE, message.getBody());
		values.put(ChatConstants.DELIVERY_STATUS, ds);
		values.put(ChatConstants.MEDIA_TYPE, (String) message.getProperty("mediaType"));
		values.put(ChatConstants.MEDIA_URL, (String) message.getProperty("mediaUrl"));
		values.put(ChatConstants.MEDIA_SIZE, (String) message.getProperty("mediaSize"));
		values.put(ChatConstants.DATE, System.currentTimeMillis());
		values.put(ChatConstants.PACKET_ID, message.getPacketID());

		cr.insert(ChatProvider.CONTENT_URI, values);
	}

	/***************** end ����������Ϣ ***********************/
	
	
	/******************************* start ��ϵ�����ݿ��¼����� **********************************/
	private void registerRosterListener()
	{
		mRoster = mXMPPConnection.getRoster();
		mRosterListener = new RosterListener()
		{
			private boolean isFristRoter;

			@Override
			public void presenceChanged(Presence presence)
			{// ��ϵ��״̬�ı䣬�������߻��뿪������֮��
				L.i("presenceChanged(" + presence.getFrom() + "): " + presence);
				String jabberID = getJabberID(presence.getFrom());
				RosterEntry rosterEntry = mRoster.getEntry(jabberID);
				updateRosterEntryInDB(rosterEntry);// ������ϵ�����ݿ�
				mService.rosterChanged();// �ص�֪ͨ������Ҫ�������ж�һ���Ƿ����
			}

			@Override
			public void entriesUpdated(Collection<String> entries)
			{// �������ݿ⣬��һ�ε�½
				// TODO
				// Auto-generated
				// method
				// stub
				L.i("entriesUpdated(" + entries + ")");
				for (String entry : entries)
				{
					RosterEntry rosterEntry = mRoster.getEntry(entry);
					updateRosterEntryInDB(rosterEntry);
				}
				mService.rosterChanged();// �ص�֪ͨ������Ҫ�������ж�һ���Ƿ����
			}

			@Override
			public void entriesDeleted(Collection<String> entries)
			{// �к���ɾ��ʱ��
				L.i("entriesDeleted(" + entries + ")");
				for (String entry : entries)
				{
					deleteRosterEntryFromDB(entry);
				}
				mService.rosterChanged();// �ص�֪ͨ������Ҫ�������ж�һ���Ƿ����
			}

			@Override
			public void entriesAdded(Collection<String> entries)
			{// ������Ӻ���ʱ��������û�е����Ի���ȷ�ϣ�ֱ����ӵ����ݿ�
				L.i("entriesAdded(" + entries + ")");
				ContentValues[] cvs = new ContentValues[entries.size()];
				int i = 0;
				for (String entry : entries)
				{
					RosterEntry rosterEntry = mRoster.getEntry(entry);
					cvs[i++] = getContentValuesForRosterEntry(rosterEntry);
				}
				mContentResolver.bulkInsert(RosterProvider.CONTENT_URI, cvs);
				if (isFristRoter)
				{
					isFristRoter = false;
					mService.rosterChanged();// �ص�֪ͨ������Ҫ�������ж�һ���Ƿ����
				}
			}
		};
		mRoster.addRosterListener(mRosterListener);
	}
	
	/**
	 * �ָ��׺�����JID
	 * @return
	 */
	private String getJabberID(String from)
	{
		String[] res = from.split("/");
	//	return res[0].toLowerCase();
		return res[0];
	}
	
	
	/**
	 * �ָ�@����ȡǰ�沿��
	 * @return
	 */
	private String getUserNameFront(String from)
	{
		String[] res = from.split("@");
	//	return res[0].toLowerCase();
		return res[0];
	}

	/**
	 * ������ϵ�����ݿ�
	 * 
	 * @param entry
	 *            ��ϵ��RosterEntry����
	 */
	private void updateRosterEntryInDB(final RosterEntry entry)
	{
		final ContentValues values = getContentValuesForRosterEntry(entry);

		if (mContentResolver.update(RosterProvider.CONTENT_URI, values, RosterConstants.JID + " = ?", new String[]
		{ entry.getUser() }) == 0)// ������ݿ��޴˺���
			addRosterEntryToDB(entry);// ����ӵ����ݿ�
	}

	/**
	 * ��ӵ����ݿ�
	 * 
	 * @param entry
	 *            ��ϵ��RosterEntry����
	 */
	private void addRosterEntryToDB(final RosterEntry entry)
	{
		ContentValues values = getContentValuesForRosterEntry(entry);
		Uri uri = mContentResolver.insert(RosterProvider.CONTENT_URI, values);
		L.i("addRosterEntryToDB: Inserted " + uri);
	}

	/**
	 * ����ϵ�˴����ݿ���ɾ��
	 * 
	 * @param jabberID
	 */
	private void deleteRosterEntryFromDB(final String jabberID)
	{
		int count = mContentResolver.delete(RosterProvider.CONTENT_URI, RosterConstants.JID + " = ?", new String[]
		{ jabberID });
		L.i("deleteRosterEntryFromDB: Deleted " + count + " entries");
	}

	/**
	 * ����ϵ��RosterEntryת����ContentValues������洢���ݿ�
	 * 
	 * @param entry
	 * @return
	 */
	private ContentValues getContentValuesForRosterEntry(final RosterEntry entry)
	{
		final ContentValues values = new ContentValues();

		values.put(RosterConstants.JID, entry.getUser());
		values.put(RosterConstants.ALIAS, getName(entry));

		Presence presence = mRoster.getPresence(entry.getUser());
		values.put(RosterConstants.STATUS_MODE, getStatusInt(presence));
		values.put(RosterConstants.STATUS_MESSAGE, presence.getStatus());
		values.put(RosterConstants.GROUP, getGroup(entry.getGroups()));
		values.put(RosterConstants.OWNER, getUserNameFront(mXMPPConnection.getUser()));

		return values;
	}

	/**
	 * ������ȡ����
	 * 
	 * @param groups
	 * @return
	 */
	private String getGroup(Collection<RosterGroup> groups)
	{
		for (RosterGroup group : groups)
		{
			return group.getName();
		}
		return "";
	}

	/**
	 * ��ȡ��ϵ������
	 * 
	 * @param rosterEntry
	 * @return
	 */
	private String getName(RosterEntry rosterEntry)
	{
		String name = rosterEntry.getName();
		if (name != null && name.length() > 0)
		{
			return name;
		}
		name = StringUtils.parseName(rosterEntry.getUser());
		if (name.length() > 0)
		{
			return name;
		}
		return rosterEntry.getUser();
	}

	/**
	 * ��ȡ״̬
	 * 
	 * @param presence
	 * @return
	 */
	private StatusMode getStatus(Presence presence)
	{
		if (presence.getType() == Presence.Type.available)
		{
			if (presence.getMode() != null)
			{
				return StatusMode.valueOf(presence.getMode().name());
			}
			return StatusMode.available;
		}
		return StatusMode.offline;
	}

	private int getStatusInt(final Presence presence)
	{
		return getStatus(presence).ordinal();
	}

	/******************************* end ��ϵ�����ݿ��¼����� **********************************/

	/**
	 * �������������Ϣ����,������Ϣ��Ҫ��ִ���ж϶Է��Ƿ��Ѷ�����Ϣ
	 */
	private void initServiceDiscovery()
	{
		// register connection features
		ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(mXMPPConnection);
		if (sdm == null)
			sdm = new ServiceDiscoveryManager(mXMPPConnection);

		sdm.addFeature("http://jabber.org/protocol/disco#info");

		// reference PingManager, set ping flood protection to 10s
		PingManager.getInstanceFor(mXMPPConnection).setPingMinimumInterval(10 * 1000);
		// reference DeliveryReceiptManager, add listener

		DeliveryReceiptManager dm = DeliveryReceiptManager.getInstanceFor(mXMPPConnection);
		dm.enableAutoReceipts();
		dm.registerReceiptReceivedListener(new DeliveryReceiptManager.ReceiptReceivedListener()
		{
			@Override
			public void onReceiptReceived(String fromJid, String toJid, String receiptId)
			{
				L.d(SmackImpl.class, "got delivery receipt for " + receiptId);
				changeMessageDeliveryStatus(receiptId, ChatConstants.DS_ACKED);// ���Ϊ�Է��Ѷ���ʵ���������˵����⣬������ʵû�����ϴ�״̬
			}
		});
	}

	@Override
	public void setStatusFromConfig()
	{// �����Լ��ĵ�ǰ״̬�����ⲿ�������
		boolean messageCarbons = PreferenceUtils.getPrefBoolean(mService, PreferenceConstants.MESSAGE_CARBONS, true);
		String statusMode = PreferenceUtils.getPrefString(mService, PreferenceConstants.STATUS_MODE, PreferenceConstants.AVAILABLE);
		String statusMessage = PreferenceUtils
				.getPrefString(mService, PreferenceConstants.STATUS_MESSAGE, mService.getString(R.string.status_online));
		int priority = PreferenceUtils.getPrefInt(mService, PreferenceConstants.PRIORITY, 0);
		if (messageCarbons)
			CarbonManager.getInstanceFor(mXMPPConnection).sendCarbonsEnabled(true);

		Presence presence = new Presence(Presence.Type.available);
	//	Mode mode = Mode.valueOf(statusMode);
	//	presence.setMode(mode);
	//	presence.setStatus(statusMessage);
	//	presence.setPriority(priority);
		mXMPPConnection.sendPacket(presence);
	}

	@Override
	public boolean isAuthenticated()
	{// �Ƿ�������������ϣ���������ⲿ�������
		if (mXMPPConnection != null)
		{
			return (mXMPPConnection.isConnected() && mXMPPConnection.isAuthenticated());
		}
		return false;
	}

	@Override
	public void addRosterItem(String user, String alias, String group) throws XXException
	{// �����ϵ�ˣ����ⲿ�������
		addRosterEntry(user, alias, group);
		
	}

	private void addRosterEntry(String user, String alias, String group) throws XXException
	{
		mRoster = mXMPPConnection.getRoster();
		try
		{
			mRoster.createEntry(user, alias, new String[]
			{ group });
		} catch (XMPPException e)
		{
			throw new XXException(e.getLocalizedMessage());
		}
	}

	@Override
	public void removeRosterItem(String user) throws XXException
	{// ɾ����ϵ�ˣ����ⲿ�������
		// TODO
		// Auto-generated
		// method
		// stub
		L.d("removeRosterItem(" + user + ")");

		removeRosterEntry(user);
		mService.rosterChanged();
	}

	private void removeRosterEntry(String user) throws XXException
	{
		mRoster = mXMPPConnection.getRoster();
		try
		{
			RosterEntry rosterEntry = mRoster.getEntry(user);

			if (rosterEntry != null)
			{
				mRoster.removeEntry(rosterEntry);
			}
		} catch (XMPPException e)
		{
			throw new XXException(e.getLocalizedMessage());
		}
	}

	@Override
	public void renameRosterItem(String user, String newName) throws XXException
	{// ��������ϵ�ˣ����ⲿ�������
		// TODO Auto-generated method stub
		mRoster = mXMPPConnection.getRoster();
		RosterEntry rosterEntry = mRoster.getEntry(user);

		if (!(newName.length() > 0) || (rosterEntry == null))
		{
			throw new XXException("JabberID to rename is invalid!");
		}
		rosterEntry.setName(newName);
	}

	@Override
	public void requestAuthorizationForRosterItem(String user)
	{// ������Է�������Ӻ�������
		// TODO
		// Auto-generated
		// method stub
		Presence response = new Presence(Presence.Type.subscribe);
		response.setTo(user);
		mXMPPConnection.sendPacket(response);
		Log.i("adduser", user);
	}

	@Override
	public void sendMessage(Message message, int ds, Boolean compress)
	{
		message.addExtension(new DeliveryReceiptRequest());
		String mediaType = (String) message.getProperty("mediaType");
		if (isAuthenticated())
		{
			if (ds == ChatConstants.DS_SENT_OR_READ)
			{
				addChatMessageToDB(ChatConstants.OUTGOING, message, ds, System.currentTimeMillis(), message.getPacketID());
				sendChatMessage(message);
			} else if (ds == ChatConstants.DS_UPLOADING && mediaType.equals("Image"))
			{
				addChatMessageToDB(ChatConstants.OUTGOING, message, ds, System.currentTimeMillis(), message.getPacketID());
				sendImageMessage(message, compress);
			} else if (ds == ChatConstants.DS_UPLOADING && mediaType.equals("Audio"))
			{
				addChatMessageToDB(ChatConstants.OUTGOING, message, ds, System.currentTimeMillis(), message.getPacketID());
				sendAudioMessage(message);
			} else if (ds == ChatConstants.DS_UPLOADING && mediaType.equals("File"))
			{
				addChatMessageToDB(ChatConstants.OUTGOING, message, ds, System.currentTimeMillis(), message.getPacketID());
				sendFileMessage(message);
			}
		} else
		{
			// send offline -> store to DB
			addChatMessageToDB(ChatConstants.OUTGOING, message, ChatConstants.DS_NEW, System.currentTimeMillis(), message.getPacketID());
		}

	}

	private void sendFileMessage(Message message)
	{
		UploadFileThread thread = new UploadFileThread(message);
		thread.start();
	}

	private class UploadFileThread extends Thread
	{
		public static final String REQYEST_URL = PreferenceConstants.REMOTE_HOST + "/AndroidUploadFileWeb/FileUploadServlet";
		private static final String REMOTE_URL_ROOT = PreferenceConstants.REMOTE_HOST + "/AndroidUploadFileWeb/Files/";

		private Message message;

		public UploadFileThread(Message message)
		{
			this.message = message;
		}

		@Override
		public void run()
		{
			try
			{
				String fileUrl = (String) message.getProperty("mediaUrl");

				int result = HttpUploader.uploadFile(fileUrl, REQYEST_URL);
				String packetID = message.getPacketID();
				if (HttpUploader.SUCCESS == result)
				{
					// ��Ǹ���ϢΪ�ϴ��ɹ�
					changeMessageDeliveryStatus(packetID, ChatConstants.DS_UPLOAD_SUCCESS);

					String fileRemoteURL = REMOTE_URL_ROOT + fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
					Log.i("�ϴ��ļ�", fileRemoteURL);
					message.setProperty(ChatConstants.MEDIA_URL, fileRemoteURL);

					sendChatMessage(message);
				} else
				{
					// ��Ǹ���ϢΪ�ϴ�ʧ��
					changeMessageDeliveryStatus(packetID, ChatConstants.DS_UPLOAD_FAILED);
				}
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private void sendAudioMessage(Message message)
	{
		UploadFileThread thread = new UploadFileThread(message);
		thread.start();
	}

	private void sendChatMessage(Message message)
	{
		mXMPPConnection.sendPacket(message);
	}

	private void sendImageMessage(Message message, Boolean compress)
	{
		UploadImgThread thread = new UploadImgThread(message, compress);
		thread.start();
	}

	private class UploadImgThread extends Thread
	{
		public static final String REQYEST_URL = PreferenceConstants.REMOTE_HOST + "/AndroidUploadImageWeb/ImageUploadServlet";
		private static final String REMOTE_URL_ROOT = PreferenceConstants.REMOTE_HOST + "/AndroidUploadImageWeb/Images/";

		private Message message;
		private Boolean compress;

		public UploadImgThread(Message message, Boolean compress)
		{
			this.message = message;
			this.compress = compress;
		}

		@Override
		public void run()
		{
			String packetID = message.getPacketID();
			try
			{
				String fileUrl = (String) message.getProperty("mediaUrl");

				int result = HttpUploader.uploadImage(fileUrl, REQYEST_URL, compress);
				if (HttpUploader.SUCCESS == result)
				{
					// ��Ǹ���ϢΪ�ϴ��ɹ�
					changeMessageDeliveryStatus(packetID, ChatConstants.DS_UPLOAD_SUCCESS);

					int start = fileUrl.lastIndexOf("/");
					String imageRemoteURL = REMOTE_URL_ROOT + fileUrl.substring(start + 1);
					Log.i("mediaUrl", imageRemoteURL);
					message.setProperty(ChatConstants.MEDIA_URL, imageRemoteURL);
					sendChatMessage(message);
				} else
				{
					// ��Ǹ���ϢΪ�ϴ�ʧ��
					changeMessageDeliveryStatus(packetID, ChatConstants.DS_UPLOAD_FAILED);
				}
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	public void sendServerPing()
	{
		if (mPingID != null)
		{// ��ʱ˵����һ��ping��������δ��Ӧ��ֱ�ӷ��أ�ֱ�����ӳ�ʱ
			L.d("Ping: requested, but still waiting for " + mPingID);
			return; // a ping is still on its way
		}
		Ping ping = new Ping();
		ping.setType(Type.GET);
		ping.setTo(PreferenceUtils.getPrefString(mService, PreferenceConstants.Server, PreferenceConstants.DEFAULT_SERVER));
		mPingID = ping.getPacketID();// ��id��ʵ��������ɣ�����Ψһ��
		mPingTimestamp = System.currentTimeMillis();
		L.d("Ping: sending ping " + mPingID);
		mXMPPConnection.sendPacket(ping);// ����ping��Ϣ

		// register ping timeout handler: PACKET_TIMEOUT(30s) + 3s
		((AlarmManager) mService.getSystemService(Context.ALARM_SERVICE)).set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + PACKET_TIMEOUT
				+ 3000, mPongTimeoutAlarmPendIntent);// ��ʱ��Ҫ������ʱ�жϵ������ˣ�ʱ����Ϊ30+3��
	}

	@Override
	public String getNameForJID(String jid)
	{
		if (null != this.mRoster.getEntry(jid) && null != this.mRoster.getEntry(jid).getName() && this.mRoster.getEntry(jid).getName().length() > 0)
		{
			return this.mRoster.getEntry(jid).getName();
		} else
		{
			return jid;
		}
	}

	@Override
	public boolean logout()
	{// ע����¼
		L.d("unRegisterCallback()");
		// remove callbacks _before_ tossing old connection
		try
		{
			mXMPPConnection.getRoster().removeRosterListener(mRosterListener);
			mXMPPConnection.removePacketListener(mPacketListener);
			mXMPPConnection.removePacketSendFailureListener(mSendFailureListener);
			mXMPPConnection.removePacketListener(mPongListener);
			((AlarmManager) mService.getSystemService(Context.ALARM_SERVICE)).cancel(mPingAlarmPendIntent);
			((AlarmManager) mService.getSystemService(Context.ALARM_SERVICE)).cancel(mPongTimeoutAlarmPendIntent);
			mService.unregisterReceiver(mPingAlarmReceiver);
			mService.unregisterReceiver(mPongTimeoutAlarmReceiver);
		} catch (Exception e)
		{
			// ignore it!
			return false;
		}
		if (mXMPPConnection.isConnected())
		{
			// work around SMACK's #%&%# blocking disconnect()
			new Thread()
			{
				@Override
				public void run()
				{
					L.d("shutDown thread started");
					mXMPPConnection.disconnect();
					L.d("shutDown thread finished");
				}
			}.start();
		}
		// setStatusOffline();
		this.mService = null;
		return true;
	}

	/**
	 * ��������ϵ�˱��Ϊ����״̬
	 */
	public void setStatusOffline()
	{
		ContentValues values = new ContentValues();
		values.put(RosterConstants.STATUS_MODE, StatusMode.offline.ordinal());
		mContentResolver.update(RosterProvider.CONTENT_URI, values, null, null);
	}

	
	/******************************* start��ϵ��ͷ��Vcard���� ������Vcard���ػ���Vcard����**********************************/
	/**
	 * ��ȡͷ�񱣴浽����
	 * 
	 */
	public void initAvatar()
	{
		try
		{
			getUserVCard(mXMPPConnection);
		} catch (XMPPException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mRoster = mXMPPConnection.getRoster();
		System.out.println(mRoster.getEntries().size());

		Collection<RosterGroup> entriesGroup = mRoster.getGroups();
		for (RosterGroup group : entriesGroup)
		{
			Collection<RosterEntry> entries = group.getEntries();
			Log.i("---", group.getName());
			for (RosterEntry entry : entries)
			{
				// ��ȡÿ��Roster��Avatar��Ϣ
				updateAvatarRosterInDB(entry);
				Log.i("---", "user: " + entry.getUser());
				getUserImage(mXMPPConnection, entry.getUser(), false);
				Log.i("---", "type: " + entry.getType());
				Log.i("---", "status: " + entry.getStatus());
				Log.i("---", "groups: " + entry.getGroups());
			}
		}
	}

	/**
	 * ��ȡ�û���vcard��Ϣ
	 * 
	 * @param connection
	 * @param user
	 * @return
	 * @throws XMPPException
	 */
	private VCard getUserVCard(XMPPConnection connection) throws XMPPException
	{
		final String me = getJabberID(connection.getUser()).split("@")[0]; 
		// ���ͷ���Ƿ����
		if (new File("mnt/sdcard/Wanglai/Avatar/" + me + ".png").exists())
		{
			System.out.println("�ļ��Ѵ���");
			return null;
		}
		
		final VCard vcard = new VCard();
		vcard.load(connection);
		
		if (vcard == null || vcard.getAvatar() == null)
		{
			return null;
		}

		File dir = new File("mnt/sdcard/Wanglai/Avatar/");
		// �����ʱ�ļ�����Ŀ¼�����ڣ����ȴ���
		if (!dir.exists())
		{
			if (!createDir("mnt/sdcard/Wanglai/Avatar/"))
			{
				System.out.println("������ʱ�ļ�ʧ�ܣ����ܴ�����ʱ�ļ����ڵ�Ŀ¼��");
				return null;
			}
		}
		Thread t = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				FileOutputStream out = null;
				try
				{
					out = new FileOutputStream("mnt/sdcard/Wanglai/Avatar/" + me + ".png");
					out.write(vcard.getAvatar());
					out.close();
				} catch (Exception e)
				{
					e.printStackTrace();
				}

			}
		});
		t.start();

		return vcard;
	}

	/**
	 * ��ȡ�û�ͷ����Ϣ
	 */
	public static void getUserImage(XMPPConnection connection, String user, boolean isSub)
	{
		final String userString = user;
		try
		{
			if (!isSub)
			{
				// ���ͷ���Ƿ����
				if (new File("mnt/sdcard/Wanglai/Avatar/" + userString + ".png").exists())
				{
					System.out.println("�ļ��Ѵ���");
					return;
				}
			}

			System.out.println("��ȡ�û�ͷ����Ϣ: " + user);
			final VCard vcard = new VCard();
			vcard.load(connection, user.substring(0, user.indexOf("@")) + "@" + connection.getServiceName());

			if (vcard == null || vcard.getAvatar() == null)
			{
				return;
			}

			File dir = new File("mnt/sdcard/Wanglai/Avatar/");
			// �����ʱ�ļ�����Ŀ¼�����ڣ����ȴ���
			if (!dir.exists())
			{
				if (!createDir("mnt/sdcard/Wanglai/Avatar/"))
				{
					System.out.println("������ʱ�ļ�ʧ�ܣ����ܴ�����ʱ�ļ����ڵ�Ŀ¼��");
					return;
				}
			}

			mService.getVcard(vcard, userString);
//			Thread t = new Thread(new Runnable()
//			{
//				@Override
//				public void run()
//				{
//					FileOutputStream out = null;
//					try
//					{
//						out = new FileOutputStream("mnt/sdcard/Wanglai/Avatar/" + userString + ".png");
//						out.write(vcard.getAvatar());
//						out.close();
//						
//						
//					} catch (Exception e)
//					{
//						e.printStackTrace();
//					}
//
//				}
//			});
//			t.start();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * �������Ŀ¼�������򴴽�Ŀ¼
	 * 
	 * @param destDirName
	 * @return
	 */
	public static boolean createDir(String destDirName)
	{
		File dir = new File(destDirName);
		if (dir.exists())
		{
			System.out.println("����Ŀ¼" + destDirName + "ʧ�ܣ�Ŀ��Ŀ¼�Ѿ�����");
			return false;
		}
		if (!destDirName.endsWith(File.separator))
		{
			destDirName = destDirName + File.separator;
		}
		// ����Ŀ¼
		if (dir.mkdirs())
		{
			System.out.println("����Ŀ¼" + destDirName + "�ɹ���");
			return true;
		} else
		{
			System.out.println("����Ŀ¼" + destDirName + "ʧ�ܣ�");
			return false;
		}
	}

	private boolean isMultimediaPkt(String mediaType)
	{
		return ChatConstants.MEDIA_TYPE_AUDIO.equals(mediaType) || ChatConstants.MEDIA_TYPE_FILE.equals(mediaType)
				|| ChatConstants.MEDIA_TYPE_IMAGE.equals(mediaType);
	}

	public class AvatarRoster
	{
		private String jid;
		private String alias;

		public String getJid()
		{
			return jid;
		}

		public void setJid(String jid)
		{
			this.jid = jid;
		}

		public String getAlias()
		{
			return alias;
		}

		public void setAlias(String alias)
		{
			this.alias = alias;
		}

	}

	private void registerAvatarListener()
	{
		// do not register multiple packet listeners
		if (mAvatarListener != null)
			mXMPPConnection.removePacketListener(mAvatarListener);

		PacketTypeFilter filter = new PacketTypeFilter(Presence.class);

		mAvatarListener = new PacketListener()
		{
			@Override
			public void processPacket(Packet packet)
			{
				try
				{
					if (packet instanceof Presence)
					{// �������Ϣ����
						Presence msg = (Presence) packet;
						String publisher = msg.getFrom().substring(0, msg.getFrom().indexOf("@"));
						// ���˵�Presence�з�"vcard-temp:x:update"
						if (msg.getExtension("x", "vcard-temp:x:update") == null)
						{
							return;
						}

						String photoHashGet = msg.getExtension("x", "vcard-temp:x:update").toXML();

						L.i(photoHashGet);
						// ��ϵ��״̬�ı䣬�������߻��뿪������֮��
						L.i("presenceChanged(" + publisher + "): " + msg);

						Cursor c = mContentResolver.query(AvatarProvider.CONTENT_URI, AVATAR_QUERY, null, null, null);
						c.moveToFirst();
						while (!c.isAfterLast())
						{
							String name = c.getString(c.getColumnIndex(RosterConstants.JID)).substring(0, c.getString(c.getColumnIndex(RosterConstants.JID)).indexOf("@"));
							if (name.equals(publisher))
							{
								System.out.println("��ȡ��photohash��" + photoHashGet);
								System.out.println("���ݿ��е�photohash��" + c.getString(c.getColumnIndex(AvatarConstants.PHOTO_HASH)));
								if (c.getString(c.getColumnIndex(AvatarConstants.PHOTO_HASH)) == null
										|| !(c.getString(c.getColumnIndex(AvatarConstants.PHOTO_HASH)).equals(photoHashGet)))
								{
									String jabberID = getJabberID(msg.getFrom());
									getUserImage(mXMPPConnection, jabberID, true);
									System.out.println(jabberID);
									RosterEntry rosterEntry = mRoster.getEntry(jabberID);
									updateAvatarInDB(rosterEntry, photoHashGet);// ������ϵ�����ݿ�
								}
							}
							c.moveToNext();
						}
						c.close();
					}
				} catch (Exception e)
				{
					// SMACK silently discards exceptions dropped from
					// processPacket :(
					L.e("failed to process packet:");
					e.printStackTrace();
				}
			}
		};

		mXMPPConnection.addPacketListener(mAvatarListener, filter);// ������ؽ����ˣ�������䣬ǰ��Ķ��ǰ׷ѹ���
	}

	private void updateAvatarInDB(RosterEntry rosterEntry, String photoHashGet)
	{
		final ContentValues values = new ContentValues();
		values.put(AvatarConstants.PHOTO_HASH, photoHashGet);
		mContentResolver.update(AvatarProvider.CONTENT_URI, values, RosterConstants.JID + " = ?", new String[]
		{ rosterEntry.getUser() });
		Log.i("Avatar Update", "OK");
	}

	private void updateAvatarRosterInDB(RosterEntry rosterEntry)
	{
		// TODO Auto-generated method stub
		final ContentValues values = getContentValuesForAvatar(rosterEntry);
		if (mContentResolver.update(AvatarProvider.CONTENT_URI, values, AvatarConstants.JID + " = ?", new String[]
		{ rosterEntry.getUser() }) == 0)// ������ݿ��޴˺���
			addAvatarRosterToDB(rosterEntry);// ����ӵ����ݿ�
	}

	private void addAvatarRosterToDB(RosterEntry rosterEntry)
	{
		// TODO Auto-generated method stub
		ContentValues values = getContentValuesForAvatar(rosterEntry);
		Uri uri = mContentResolver.insert(AvatarProvider.CONTENT_URI, values);
		L.i("addAvatarToDB: Inserted " + uri);

	}

	private ContentValues getContentValuesForAvatar(final RosterEntry entry)
	{
		final ContentValues values = new ContentValues();

		values.put(AvatarConstants.JID, entry.getUser());
		values.put(AvatarConstants.ALIAS, getName(entry));
		return values;
	}

	/**
	 * �޸��û�ͷ��
	 * 
	 * @param connection
	 * 
	 * @param f
	 * 
	 * @throws XMPPException
	 * 
	 * @throws IOException
	 */
	public boolean setUserImage(final byte[] image) throws XMPPException
	{
		final VCard card = new VCard();
		card.load(mXMPPConnection);
		boolean state = false;
		try
		{
			PacketFilter filter = new AndFilter(new PacketIDFilter(card.getPacketID()), new PacketTypeFilter(IQ.class));
			PacketCollector collector = mXMPPConnection.createPacketCollector(filter);
			String encodeImage = StringUtils.encodeBase64(image);
			card.setAvatar(image, encodeImage);
			card.setEncodedImage(encodeImage);
			card.setField("PHOTO", "<TYPE>image/jpg</TYPE><BINVAL>" + encodeImage + "</BINVAL>", true);
			Log.i("other", "�ϴ�ͷ��ķ�����");
			card.save(mXMPPConnection);
			IQ iq = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
			if (iq != null && iq.getType() == IQ.Type.RESULT)
			{
				state = true;
			}
			else 
			{
				state = false;
			}
		} catch (XMPPException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return state;
	}
	
	/************************ end��ϵ��ͷ��Vcard���� ������Vcard���ػ���Vcard���� ********************************/
	
	
	/********************** start ��Ӻ���,�ֻ�ͨѶ¼��ϵ�˴��뱾�����ݿ⣬��¼ʱ�����ϵ���Ƿ����***********************/
	/**
	 * ��ȡ�ֻ���ϵ��
	 */
	@SuppressLint("NewApi")
	public void getPhoneContacts() {

		Cursor phoneCursor = resolver.query(Phone.CONTENT_URI,
				PHONES_PROJECTION, null, null, null);
		ArrayList <String>repeatNum = new ArrayList<String>();
		if (phoneCursor != null) {
			while (phoneCursor.moveToNext()) {
				boolean state = true;
				 for (int i=0,lsize=repeatNum.size();i<lsize;i++)
				 { 
					 if(repeatNum.get(i).equals(phoneCursor.getPosition()+""))
						{
						 Log.i("ddddddddddddd",phoneCursor.getPosition()+"");
						 state = false;
						}
				 } 
				 if(!state)
				 {
					 continue;
				 }
				//��ϵ�˺���淶��
				String phoneNumber = phoneCursor.getString(PHONES_NUMBER_INDEX).replaceAll("-", "").replaceAll("^(\\+86)", "").trim();;
				if (TextUtils.isEmpty(phoneNumber))
					continue;
				
				String contactName = phoneCursor
						.getString(PHONES_DISPLAY_NAME_INDEX);
				//���浱ǰ��ѯָ��״̬
				int position = phoneCursor.getPosition();
				
				Log.i("dsdsdsd",position+"");
				while (phoneCursor.moveToNext())
				{
					String number = phoneCursor.getString(PHONES_NUMBER_INDEX);
					if (TextUtils.isEmpty(number))
						continue;
					
					if(phoneNumber.equals(number))
					{
						int positionrepeat = phoneCursor.getPosition();
						repeatNum.add(positionrepeat+"");
						Log.i("www",phoneNumber);
						String otherContactName = phoneCursor
								.getString(PHONES_DISPLAY_NAME_INDEX);
						contactName = contactName + " ��  " + otherContactName;
					}
				}
				//�ָ���ѯָ��
				phoneCursor.moveToPosition(position);
				
			//	Long contactid = phoneCursor.getLong(PHONES_CONTACT_ID_INDEX);

//				Long photoid = phoneCursor.getLong(PHONES_PHOTO_ID_INDEX);
//
//				Bitmap contactPhoto = null;
//
//				if (photoid > 0) {
//					Uri uri = ContentUris.withAppendedId(
//							ContactsContract.Contacts.CONTENT_URI, contactid);
//					InputStream input = ContactsContract.Contacts
//							.openContactPhotoInputStream(resolver, uri);
//					contactPhoto = BitmapFactory.decodeStream(input);
//				} else {
//					contactPhoto = BitmapFactory.decodeResource(getResources(),
//							R.drawable.default_mobile_avatar);
//				}
				
				//�ж�ͨѶ¼��ĺ����Ƿ��Ѿ�����ӣ��������ݿ�
				String status = "need to add";
				mRoster = mXMPPConnection.getRoster();
				Collection<RosterGroup> entriesGroup = mRoster.getGroups();
				for (RosterGroup group : entriesGroup)
				{
					Collection<RosterEntry> entries = group.getEntries();
					for (RosterEntry entry : entries)
					{
						if(phoneNumber.equals(entry.getName()))
						{
							status = "added";
						}
					}
				}
				updateConstantsInDB(contactName, phoneNumber, status);
			}
			phoneCursor.close();
		}
	}
	
	private void updateConstantsInDB(String Name, String phoneNum, String status)
	{
		final ContentValues values = getContentValuesForPhones(Name, phoneNum, status);
		if (mContentResolver.update(AddPhonesProvider.CONTENT_URI, values, PhoneConstants.PHONE_NUM + " = ?", new String[]
		{ phoneNum }) == 0)// ������ݿ��޴˺���
			addPhoneToDB(Name, phoneNum, status);// ����ӵ����ݿ�
	}

	private void addPhoneToDB(String Name, String phoneNum, String status)
	{
		ContentValues values = getContentValuesForPhones(Name, phoneNum, status);
		Uri uri = mContentResolver.insert(AddPhonesProvider.CONTENT_URI, values);
		L.i("addPhoneToDB: Inserted " + uri);

	}

	private ContentValues getContentValuesForPhones(String Name, String phoneNum, String status)
	{
		final ContentValues values = new ContentValues();
		
		values.put(PhoneConstants.PHONE_NUM, phoneNum);
		values.put(PhoneConstants.NAME, Name);
		values.put(PhoneConstants.STATUS, status);
		return values;
	}
	
	/********************** end ��Ӻ���,�ֻ�ͨѶ¼��ϵ�˴��뱾�����ݿ⣬��¼ʱ�����ϵ���Ƿ����***********************/
	
	
	/**************************** start ������������������ݿ�******************************/
	private void registerAddLinster()
	{
		// do not register multiple packet listeners
		if (mAddListener != null)
					mXMPPConnection.removePacketListener(mAddListener);
		 Log.i("Presence", "PresenceService-----" + (mXMPPConnection == null));  
	        if (mXMPPConnection != null && mXMPPConnection.isConnected()  
	                && mXMPPConnection.isAuthenticated()) {//�Ѿ���֤������£�������ȷ�յ�Presence����Ҳ���ǵ�½��  
	            final String loginuser = mXMPPConnection.getUser().substring(0,  
	            		mXMPPConnection.getUser().lastIndexOf("@"));  
	            //���Ϊ����������   ���˳�Presence��  
	            PacketFilter filter = new AndFilter(new PacketTypeFilter(  
	                    Presence.class));  
	            mAddListener = new PacketListener() {  
	                @Override  
	                public void processPacket(Packet packet) {  
	                    Log.i("Presence", "PresenceService------" + packet.toXML());  
	                    //��API��֪��   Presence��Packet������  
	                    if (packet instanceof Presence) {  
	                        Log.i("Presence", packet.toXML());  
	                        Presence presence = (Presence) packet;  
	                        //Presence���кܶ෽�����ɲ鿴API   
	                        String from = presence.getFrom();//���ͷ�  
	                        String to = presence.getTo();//���շ�  
	                        //Presence.Type��7��״̬  
	                        if (presence.getType().equals(Presence.Type.subscribe)) {//��������  
	                        	Log.i("subscribe","OK");
	                        	updateNewFriendsInDB(presence.getFrom().toString(), presence.getFrom().toString(), "subscribe");
	                              
	                        } else if (presence.getType().equals(  
	                                Presence.Type.subscribed)) {//ͬ����Ӻ���  
	                        	Log.i("subscribed","OK");
	                              
	                        } else if (presence.getType().equals(  
	                                Presence.Type.unsubscribe)) {//�ܾ���Ӻ���  ��  ɾ������  
	                        	Log.i("unsubscribe","OK");
	                              
	                        } else if (presence.getType().equals(  
	                                Presence.Type.unsubscribed)) {//�����û�õ�  
	                        } else if (presence.getType().equals(  
	                                Presence.Type.unavailable)) {//��������   Ҫ���º����б����������յ����󣬷��㲥��ָ��ҳ��   �����б�  
	                              
	                        } else {//��������  
	                              
	                        }  
	                    }  
	                }  
	            };  
	            mXMPPConnection.addPacketListener(mAddListener, filter);  
	        }  
	}
	
	private void updateNewFriendsInDB(String Name, String JID, String status)
	{
		final ContentValues values = getContentValuesForNewFriends(Name, JID, status);
		if (mContentResolver.update(NewFriendsProvider.CONTENT_URI, values, NewFriendsConstants.JID + " = ?", new String[]
		{ JID }) == 0)// ������ݿ��޴�JID
			addNewFriendsToDB(Name, JID, status);// ����ӵ����ݿ�
	}

	private void addNewFriendsToDB(String Name, String JID, String status)
	{
		ContentValues values = getContentValuesForNewFriends(Name, JID, status);
		Uri uri = mContentResolver.insert(NewFriendsProvider.CONTENT_URI, values);
		L.i("addNewFriendsToDB: Inserted " + uri);
	}

	private ContentValues getContentValuesForNewFriends(String Name, String JID, String status)
	{
		final ContentValues values = new ContentValues();
		
		values.put(NewFriendsConstants.JID, JID);
		values.put(NewFriendsConstants.NAME, Name);
		values.put(NewFriendsConstants.STATUS, status);
		return values;
	}
	
	/**************************** end ������������������ݿ�******************************/
	
	
}
