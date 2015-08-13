package com.hdu.cfryan.activity;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jivesoftware.smack.packet.Message;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.AsyncQueryHandler;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.FloatMath;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.hdu.cfryan.R;
import com.hdu.cfryan.adapter.ChatAdapter;
import com.hdu.cfryan.adapter.FaceAdapter;
import com.hdu.cfryan.adapter.FacePageAdapter;
import com.hdu.cfryan.application.Wanglai;
import com.hdu.cfryan.db.ChatProvider;
import com.hdu.cfryan.db.RosterProvider;
import com.hdu.cfryan.db.ChatProvider.ChatConstants;
import com.hdu.cfryan.service.IConnectionStatusCallback;
import com.hdu.cfryan.service.XXService;
import com.hdu.cfryan.ui.view.CirclePageIndicator;
import com.hdu.cfryan.ui.xlistview.MsgListView;
import com.hdu.cfryan.ui.xlistview.MsgListView.IXListViewListener;
import com.hdu.cfryan.util.L;
import com.hdu.cfryan.util.PreferenceConstants;
import com.hdu.cfryan.util.PreferenceUtils;
import com.hdu.cfryan.util.StatusMode;
import com.hdu.cfryan.util.T;
import com.hdu.cfryan.util.XMPPHelper;

public class ChatActivity extends /* SwipeBack */Activity implements OnTouchListener, OnClickListener, IXListViewListener, IConnectionStatusCallback
{
	private boolean isCompressed = true;
	
	private static final int MEDIA_TYPE_IMAGE = 1;
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final int TAKE_IMAGE_ACTIVITY_REQUEST_CODE = 101;
	private static final int SEND_FILES_ACTIVITY_REQUEST_CODE = 102;
	private static final int SEND_BUSINESS_CARD_ACTIVITY_REQUEST_CODE = 103;

	private static final int SHOW_NOTHING = 1;
	private static final int SHOW_FACE_DIALOG = 2;
	private static final int SHOW_MEDIA_SELECT_DIALOG = 3;

	private static final int MSG_UPLOAD_BMP_SUCCESS = 0;
	private static final int MSG_UPLOAD_BMP_FAILED = 1;
	private static final int MSG_DOWNLOAD_SUCCESS = 2;
	private static final int MSG_DOWNLOAD_FAILURE = 3;

	static final int NONE = 0;
	static final int DRAG = 1;
	static final int ZOOM = 2;
	int mode = NONE;

	// Remember some things for zooming
	PointF start = new PointF();
	PointF mid = new PointF();
	float oldDist = 1f;

	private int mWindowState = SHOW_NOTHING;

	private String mImageFileName;
	
	private LocationManager locationManager;
	private String provider;
	private Location location;
	private Address address;

	public static final String INTENT_EXTRA_USERNAME = ChatActivity.class.getName() + ".username";// �ǳƶ�Ӧ��key
	private MsgListView mMsgListView;// �Ի�ListView
	private ViewPager mFaceViewPager;// ����ѡ��ViewPager
	private int mCurrentPage = 0;// ��ǰ����ҳ
	private Button mSendMsgBtn;// ������Ϣbutton

	private ImageButton mFaceBtn;// �л����̺ͱ����button
	// private ImageButton mKeyboardBtn;// �л����̺ͱ����button

	private ImageButton mAddMediaBtn; // ���Ͷ�ý��button
	private TextView mTitleNameView;// ������
	private ImageView mTitleStatusView;
	private EditText mMsgInput;// ��Ϣ�����
	private LinearLayout mFaceSelectWindow;// ���鸸����
	private WindowManager.LayoutParams mWindowNanagerParams;
	private InputMethodManager mInputMethodManager;
	private List<String> mFaceMapKeys;// �����Ӧ���ַ�������
	private String mPeerJabberID = null;// ��ǰ�����û���ID

	private ImageView mRecordSwitcher;
	private TextView mRecordBtn;
	private boolean mIsRecordState = false;

	boolean mIsMediaSelectState = false;

	private RelativeLayout mInputSendContainer;
	// private FrameLayout mSelectFaceOrMediaWindow;
	
	private View mChatBody, mPreviewDialog;
	private ImageView mPreview;
	private Matrix matrix = new Matrix();
	private Matrix savedMatrix = new Matrix();
	private CheckBox mIsCompressed;
	private Button mSendImage;
	
	//¼������
	private RecordEvent mRecordEvent;  //¼������
	private FrameLayout mChatBodyContainer; //����Ի�����Ĳ���������¼��������Ҫ�ڴ˻����������ȥ��;

	private GridView mMediaSelectWindow;
	ArrayList<HashMap<String, Object>> mMediaGridViewData = new ArrayList<HashMap<String, Object>>();

	private static final String[] PROJECTION_FROM = new String[]
	{ ChatProvider.ChatConstants._ID, ChatProvider.ChatConstants.DATE, ChatProvider.ChatConstants.DIRECTION, ChatProvider.ChatConstants.JID,
			ChatProvider.ChatConstants.MESSAGE, ChatProvider.ChatConstants.MEDIA_TYPE, ChatProvider.ChatConstants.MEDIA_URL,
			ChatProvider.ChatConstants.MEDIA_SIZE, ChatProvider.ChatConstants.DELIVERY_STATUS };// ��ѯ�ֶ�

	private ContentObserver mContactObserver = new ContactObserver();// ��ϵ�����ݼ�������Ҫ�Ǽ����Է�����״̬
	private XXService mXxService;// Main����
	ServiceConnection mServiceConnection = new ServiceConnection()
	{

		@Override
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			mXxService = ((XXService.XXBinder) service).getService();
			mXxService.registerConnectionStatusCallback(ChatActivity.this);
			// ���û�������ϣ�����������xmpp������
			if (!mXxService.isAuthenticated())
			{
				String usr = PreferenceUtils.getPrefString(ChatActivity.this, PreferenceConstants.ACCOUNT, "");
				String password = PreferenceUtils.getPrefString(ChatActivity.this, PreferenceConstants.PASSWORD, "");
				mXxService.Login(usr, password);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name)
		{
			mXxService.unRegisterConnectionStatusCallback();
			mXxService = null;
		}

	};

	/**
	 * ������
	 */
	private void unbindXMPPService()
	{
		try
		{
			unbindService(mServiceConnection);
		} catch (IllegalArgumentException e)
		{
			L.e("Service wasn't bound!");
		}
	}

	/**
	 * �󶨷���
	 */
	private void bindXMPPService()
	{
		Intent mServiceIntent = new Intent(this, XXService.class);
		Uri chatURI = Uri.parse(mPeerJabberID);
		mServiceIntent.setData(chatURI);
		bindService(mServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);
		
		initData();// ��ʼ������
		initView();// ��ʼ��view
		
		//�õ�����Ի�����Ĳ��֣�¼��������Ҫ�ڴ˻����������ȥ��
		mChatBodyContainer = (FrameLayout) findViewById(R.id.chat_body);
		mRecordEvent = new RecordEvent(this, mChatBodyContainer); 
		mRecordEvent.init();
		
		setChatWindowAdapter();// ��ʼ���Ի�����
		getContentResolver().registerContentObserver(RosterProvider.CONTENT_URI, true, mContactObserver);// ��ʼ������ϵ�����ݿ�
		
//		initLocation();
		
	}

	private void initLocation()
	{
		 //��ȡ��LocationManager����
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        //����һ��Criteria����
        Criteria criteria = new Criteria();
        //���ô��Ծ�ȷ��
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        //�����Ƿ���Ҫ���غ�����Ϣ
        criteria.setAltitudeRequired(false);
        //�����Ƿ���Ҫ���ط�λ��Ϣ
        criteria.setBearingRequired(false);
        //�����Ƿ������ѷ���
        criteria.setCostAllowed(true);
        //���õ������ĵȼ�
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        //�����Ƿ���Ҫ�����ٶ���Ϣ
        criteria.setSpeedRequired(false);
 
        //�������õ�Criteria���󣬻�ȡ����ϴ˱�׼��provider����
        String currentProvider = locationManager.getBestProvider(criteria, true);
        Log.d("Location", "currentProvider: " + currentProvider);
        //���ݵ�ǰprovider�����ȡ���һ��λ����Ϣ
        Location currentLocation = locationManager.getLastKnownLocation(currentProvider);
        //���λ����ϢΪnull�����������λ����Ϣ
        if(currentLocation == null){
            locationManager.requestLocationUpdates(currentProvider, 0, 0, locationListener);
        }
        //ֱ��������һ��λ����ϢΪֹ�����δ������һ��λ����Ϣ������ʾĬ�Ͼ�γ��
        //ÿ��10���ȡһ��λ����Ϣ
        while(true){
            currentLocation = locationManager.getLastKnownLocation(currentProvider);
            if(currentLocation != null){
                Log.d("Location", "Latitude: " + currentLocation.getLatitude());
                Log.d("Location", "location: " + currentLocation.getLongitude());
                break;
            }else{
                Log.d("Location", "Latitude: " + 0);
                Log.d("Location", "location: " + 0);
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                 Log.e("Location", e.getMessage());
            }
        }
        
        //������ַ����ʾ
        Geocoder geoCoder = new Geocoder(this);
        try {
            int latitude = (int) currentLocation.getLatitude();
            int longitude = (int) currentLocation.getLongitude();
            List<Address> list = geoCoder.getFromLocation(latitude, longitude, 2);
            for(int i=0; i<list.size(); i++){
                Address address = list.get(i); 
                Toast.makeText(this, address.getCountryName() + address.getAdminArea() + address.getFeatureName(), Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Toast.makeText(this,e.getMessage(), Toast.LENGTH_LONG).show();
        }
		
	}

	private Handler handler = new Handler()
	{
		@Override
		public void handleMessage(android.os.Message msg)
		{ // �÷�������UI���߳���ִ��
			switch (msg.what)
			{
			case MSG_UPLOAD_BMP_SUCCESS:
				sendImageMessage(mImageFileName, true);
				break;
			case MSG_UPLOAD_BMP_FAILED:
				Toast.makeText(ChatActivity.this, "R.string.error", Toast.LENGTH_LONG).show();
				alert();
				break;
			}
			super.handleMessage(msg);
		};
	};

	@Override
	protected void onResume()
	{
		super.onResume();
		updateContactStatus();// ������ϵ��״̬
	}

	@Override
	protected void onPause()
	{
		super.onPause();
	}

	// ��ѯ��ϵ�����ݿ��ֶ�
	private static final String[] STATUS_QUERY = new String[]
	{ RosterProvider.RosterConstants.STATUS_MODE, RosterProvider.RosterConstants.STATUS_MESSAGE, };
	private static final String TAG = null;

	private void updateContactStatus()
	{
		Cursor cursor = getContentResolver().query(RosterProvider.CONTENT_URI, STATUS_QUERY, RosterProvider.RosterConstants.JID + " = ?",
				new String[]
				{ mPeerJabberID }, null);
		int MODE_IDX = cursor.getColumnIndex(RosterProvider.RosterConstants.STATUS_MODE);
		int MSG_IDX = cursor.getColumnIndex(RosterProvider.RosterConstants.STATUS_MESSAGE);

		if (cursor.getCount() == 1)
		{
			cursor.moveToFirst();
			int status_mode = cursor.getInt(MODE_IDX);
			String status_message = cursor.getString(MSG_IDX);
			L.d("contact status changed: " + status_mode + " " + status_message);
			mTitleNameView.setText(XMPPHelper.splitJidAndServer(getIntent().getStringExtra(INTENT_EXTRA_USERNAME)));
			int statusId = StatusMode.values()[status_mode].getDrawableId();
			if (statusId != -1)
			{// �����Ӧ����״̬
				// Drawable icon = getResources().getDrawable(statusId);
				// mTitleNameView.setCompoundDrawablesWithIntrinsicBounds(icon,
				// null,
				// null, null);
				mTitleStatusView.setImageResource(statusId);
				mTitleStatusView.setVisibility(View.VISIBLE);
			} else
			{
				mTitleStatusView.setVisibility(View.GONE);
			}
		}
		cursor.close();
	}

	/**
	 * ��ϵ�����ݿ�仯����
	 * 
	 */
	private class ContactObserver extends ContentObserver
	{
		public ContactObserver()
		{
			super(new Handler());
		}

		@Override
		public void onChange(boolean selfChange)
		{
			L.d("ContactObserver.onChange: " + selfChange);
			updateContactStatus();// ��ϵ��״̬�仯ʱ��ˢ�½���
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		if (hasWindowFocus())
			unbindXMPPService();// ������
		getContentResolver().unregisterContentObserver(mContactObserver);
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus)
	{
		super.onWindowFocusChanged(hasFocus);
		// ���ڻ�ȡ������ʱ�󶨷���ʧȥ���㽫���
		if (hasFocus)
			bindXMPPService();
		else
			unbindXMPPService();
	}

	private void initData()
	{
		mPeerJabberID = getIntent().getDataString().toLowerCase();// ��ȡ��������id
		this.setTitle(mPeerJabberID.split("@")[0]);

		// ������map��key������������
		Set<String> keySet = Wanglai.getInstance().getFaceMap().keySet();
		mFaceMapKeys = new ArrayList<String>();
		mFaceMapKeys.addAll(keySet);
	}

	/**
	 * ����ѡ���
	 */
	private void setMediaSelectWindowAdapter()
	{
		ArrayList<HashMap<String, Object>> data = new ArrayList<HashMap<String, Object>>();

		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("ItemImage", R.drawable.app_panel_pic_icon);
		map.put("ItemText", "ͼƬ");
		data.add(map);

		HashMap<String, Object> map1 = new HashMap<String, Object>();
		map1.put("ItemImage", R.drawable.app_panel_video_icon);
		map1.put("ItemText", "����");
		data.add(map1);

		HashMap<String, Object> map2 = new HashMap<String, Object>();
		map2.put("ItemImage", R.drawable.app_panel_location_icon);
		map2.put("ItemText", "λ��");
		data.add(map2);

		HashMap<String, Object> map3 = new HashMap<String, Object>();
		map3.put("ItemImage", R.drawable.app_panel_add_icon);
		map3.put("ItemText", "�ĵ�");
		data.add(map3);

		HashMap<String, Object> map4 = new HashMap<String, Object>();
		map4.put("ItemImage", R.drawable.app_panel_businesscard_icon);
		map4.put("ItemText", "��Ƭ");
		data.add(map4);

		SimpleAdapter adapter = new SimpleAdapter(this, // ûʲô����
				data,// ������Դ
				R.layout.grid_view_item,// night_item��XMLʵ��
				new String[]
				{ "ItemImage", "ItemText" }, new int[]
				{ R.id.ItemImage, R.id.ItemText });
		mMediaSelectWindow.setAdapter(adapter);
	}

	/**
	 * ���������Adapter
	 */
	private void setChatWindowAdapter()
	{
		String selection = ChatConstants.JID + "='" + mPeerJabberID + "'";// +
																			// "OR"
		Log.i("Jid", mPeerJabberID);
		// �첽��ѯ���ݿ�
		new AsyncQueryHandler(getContentResolver())
		{

			@Override
			protected void onQueryComplete(int token, Object cookie, Cursor cursor)
			{
				// ListAdapter adapter = new ChatWindowAdapter(cursor,
				// PROJECTION_FROM, PROJECTION_TO, mWithJabberID);
				ListAdapter adapter = new ChatAdapter(ChatActivity.this, cursor, PROJECTION_FROM);

				// ListAdapter adapter = new ChatAdapterEx(ChatActivity.this,
				// cursor);

				mMsgListView.setAdapter(adapter);
				mMsgListView.setSelection(adapter.getCount() - 1);
			}

		}.startQuery(0, null, ChatProvider.CONTENT_URI, PROJECTION_FROM, selection, null, null);
		// ͬ����ѯ���ݿ⣬����ֹͣʹ��,��������Ӵ�ʱ�����½���ʧȥ��Ӧ
		// Cursor cursor = managedQuery(ChatProvider.CONTENT_URI,
		// PROJECTION_FROM,
		// selection, null, null);
		// ListAdapter adapter = new ChatWindowAdapter(cursor, PROJECTION_FROM,
		// PROJECTION_TO, mWithJabberID);
		// mMsgListView.setAdapter(adapter);
		// mMsgListView.setSelection(adapter.getCount() - 1);
	}

	private void initView()
	{
		mChatBody = findViewById(R.id.chat_body);
		mPreviewDialog = findViewById(R.id.preview_dialog);
		mPreview = (ImageView) findViewById(R.id.preview_picture);
		mPreview.setOnTouchListener(this);
		mPreview.setLongClickable(true);
		mIsCompressed = (CheckBox) findViewById(R.id.uncompressed_option);
		mSendImage = (Button) findViewById(R.id.send_image_btn);
		
		mSendImage.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if(mIsCompressed.isChecked() == false)
				{
					isCompressed = false;
				}
				sendImageMessage(mImageFileName, isCompressed);
				mChatBody.setVisibility(View.VISIBLE);
				mMediaSelectWindow.setVisibility(View.VISIBLE);
				mPreviewDialog.setVisibility(View.GONE);
			}
		});

		mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		mWindowNanagerParams = getWindow().getAttributes();

		mTitleNameView = (TextView) findViewById(R.id.ivTitleName);
		mTitleStatusView = (ImageView) findViewById(R.id.ivTitleStatus);

		mMsgListView = (MsgListView) findViewById(R.id.msg_listView);
		mMsgListView.setOnTouchListener(this);
		mMsgListView.setPullLoadEnable(false);
		mMsgListView.setXListViewListener(this);

		mRecordSwitcher = (ImageView) findViewById(R.id.record_switcher);
		mRecordSwitcher.setOnClickListener(this);

		mInputSendContainer = (RelativeLayout) findViewById(R.id.chat_input_send_container);
		mRecordBtn = (TextView) findViewById(R.id.record_btn);
		mRecordBtn.setOnTouchListener(new OnTouchListener()
		{
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1)
			{
				return false;
			}
		});

		mMsgInput = (EditText) findViewById(R.id.chat_msg_input);
		mMsgInput.setOnTouchListener(this);
		mMsgInput.addTextChangedListener(new TextWatcher()
		{

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
				// TODO Auto-generated method stub

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{
			}

			@Override
			public void afterTextChanged(Editable s)
			{
				if (s.length() > 0)
				{
					mSendMsgBtn.setVisibility(View.VISIBLE);
					mAddMediaBtn.setVisibility(View.GONE);
				} else
				{
					mSendMsgBtn.setVisibility(View.GONE);
					mAddMediaBtn.setVisibility(View.VISIBLE);
				}
			}
		});

		mFaceBtn = (ImageButton) findViewById(R.id.face_btn);
		mFaceBtn.setOnClickListener(this);

		mSendMsgBtn = (Button) findViewById(R.id.send_msg_btn);
		mSendMsgBtn.setOnClickListener(this);

		mAddMediaBtn = (ImageButton) findViewById(R.id.add_media_btn);
		mAddMediaBtn.setOnClickListener(this);

		mFaceSelectWindow = (LinearLayout) findViewById(R.id.face_select_dialog);
		mFaceViewPager = (ViewPager) findViewById(R.id.face_pager);
		initFacePage();// ��ʼ������

		mMediaSelectWindow = (GridView) findViewById(R.id.media_select_dialog);
		setMediaSelectWindowAdapter();
		mMediaSelectWindow.setOnItemClickListener(new ItemClickListener());

	}

	// ��������¼�ư�ťʱ
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{

		if (!Environment.getExternalStorageDirectory().exists())
		{
			Toast.makeText(this, "No SDCard", Toast.LENGTH_LONG).show();
			return false;
		}

		if (!new File(android.os.Environment.getExternalStorageDirectory() + "/Wanglai/Recordings/").exists())
		{
			if (!createDir(android.os.Environment.getExternalStorageDirectory() + "/Wanglai/Recordings/"))
			{
				System.out.println("������ʱ�ļ�ʧ�ܣ����ܴ�����ʱ�ļ����ڵ�Ŀ¼��");
				return false;
			}
		}

		boolean recordresult = mRecordEvent.RecordTouchEvent(mIsRecordState, mRecordBtn, event);
		
		if (recordresult)
		{
			sendAuidoMessage(mRecordEvent.audioUrl, mRecordEvent.recordTime + "");
			mMsgListView.setSelection(mMsgListView.getCount() - 1);
			
			Log.i("audioUrl", mRecordEvent.audioUrl);
			Log.i("audioTime", mRecordEvent.recordTime + "");
		}
		
		return super.onTouchEvent(event);
	}


	// ��AdapterView������(���������߼���)���򷵻ص�Item�����¼�
	class ItemClickListener implements OnItemClickListener
	{
		/** Create a File for saving an image or video */
		private File getOutputMediaFile(int type)
		{
			File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Wanglai");

			if (!mediaStorageDir.exists())
			{
				if (!mediaStorageDir.mkdirs())
				{
					Log.d("Wanglai", "failed to create directory");
					return null;
				}
			}

			// Create a media file name
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
			File mediaFile = null;
			if (type == MEDIA_TYPE_IMAGE)
			{
				mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
			}

			return mediaFile;
		}

		/** Create a file Uri for saving an image or video */
		private Uri getOutputMediaFileUri(int type)
		{
			return Uri.fromFile(getOutputMediaFile(type));
		}

		@Override
		public void onItemClick(AdapterView<?> view,// The AdapterView where the
													// click happened
				View arg1,// The view within the AdapterView that was clicked
				int position,// The position of the view in the adapter
				long arg3// The row id of the item that was clicked
		)
		{
			HashMap<String, Object> item = (HashMap<String, Object>) view.getItemAtPosition(position);

			String itemText = (String) item.get("ItemText");
			// mMsgInput.setText(itemText);

			int REQUEST_CODE;
			if ("ͼƬ".equals(itemText))
			{
				Intent intent = new Intent();
				intent.setType("image/*");
				intent.setAction(Intent.ACTION_GET_CONTENT);
				startActivityForResult(intent, TAKE_IMAGE_ACTIVITY_REQUEST_CODE);

			} else if ("����".equals(itemText))
			{
				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				//�Զ��屣������ͼƬ��uri��������ͬ����ʵ·��
				Uri fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
				intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
				startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
			}
			else if ("λ��".equals(itemText))
			{
				
			}
			else if ("�ĵ�".equals(itemText))
			{
//				Intent intent = new Intent(getApplicationContext(), FormFiles.class);
//				startActivityForResult(intent, SEND_FILES_ACTIVITY_REQUEST_CODE);
			}
			else if ("��Ƭ".equals(itemText))
			{
//				Intent intent = new Intent(getApplicationContext(), BusinessCardActivity.class);
//				startActivityForResult(intent, SEND_BUSINESS_CARD_ACTIVITY_REQUEST_CODE);
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, final int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		BitmapFactory.Options options;
		Bitmap bmp;
		switch (requestCode)
		{
			
		case CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE:
			
			mChatBody.setVisibility(View.GONE);
			mMediaSelectWindow.setVisibility(View.GONE);
			mPreviewDialog.setVisibility(View.VISIBLE);

			options = new BitmapFactory.Options();
			options.inSampleSize = 5;
			
			//���趨�Զ���·����ʱ����ȡͼƬ
			Uri captureImgUri = data.getData();
			mImageFileName = captureImgUri.getPath();
			Log.i("CAPTURE_IMAGE_file_path", mImageFileName);
			bmp = BitmapFactory.decodeFile(mImageFileName, options);
			
			//Ĭ��ϵͳ·��ʱ��ȡͼƬ
//			Bundle bundle = data.getExtras();
//			bmp = (Bitmap)bundle.get("data");
			
			mPreview.setImageBitmap(bmp);
			break;
		case TAKE_IMAGE_ACTIVITY_REQUEST_CODE:

			mChatBody.setVisibility(View.GONE);
			mMediaSelectWindow.setVisibility(View.GONE);
			mPreviewDialog.setVisibility(View.VISIBLE);

			if (resultCode == RESULT_OK)
			{
				if (data != null)
				{
					Uri takeImgUri = data.getData();
					Log.i(TAG, "uri = " + takeImgUri);
					
					try
					{
						//uriת��ʵ·��
						Cursor cursor = getContentResolver().query(takeImgUri, new String[]{MediaColumns.DATA}, null, null, null);
						if (cursor != null)
						{
							int colunm_index = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
							cursor.moveToFirst();
							String path = cursor.getString(colunm_index);
							/***
							 * ���������һ���ж���Ҫ��Ϊ�˵����������ѡ�񣬱��磺ʹ�õ��������ļ��������Ļ���
							 * ��ѡ����ļ��Ͳ�һ����ͼƬ�ˣ������Ļ��������ж��ļ��ĺ�׺�� �����ͼƬ��ʽ�Ļ�����ô�ſ���
							 */
							if (path.endsWith("jpg") || path.endsWith("png"))
							{
								mImageFileName = path;
								options = new BitmapFactory.Options();
								options.inSampleSize = 5;
								bmp = BitmapFactory.decodeFile(mImageFileName, options);
								Log.i("TAKE_IMAGE_file_path",mImageFileName);
								mPreview.setImageBitmap(bmp);
							} else
							{
								alert();
							}
						} else
						{
							alert();
						}
					} catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			}
			break;
		case SEND_FILES_ACTIVITY_REQUEST_CODE:
			String filepath = data.getStringExtra("filepath");
			if(filepath.length() > 0)
			{
				Log.i("�ļ�·��",filepath);
				sendFileMessage(filepath);
			}
			break;
		case SEND_BUSINESS_CARD_ACTIVITY_REQUEST_CODE:
			String businesscard = data.getStringExtra("businesscard");
			
			if (mXxService != null)
			{
				Message message = new Message();
				Log.i("JabberID", mPeerJabberID);
				message.setTo(mPeerJabberID);
				message.setBody(businesscard);
				message.setProperty(ChatConstants.MEDIA_TYPE, ChatConstants.MEDIA_TYPE_NORMAL);
				mXxService.sendMessage(message, ChatConstants.DS_SENT_OR_READ, false);

				if (!mXxService.isAuthenticated())
					T.showShort(this, "��Ϣ�Ѿ����������");
			}
		}
	}

	@Override
	public void onRefresh()
	{
		mMsgListView.stopRefresh();
	}

	@Override
	public void onLoadMore()
	{
		// do nothing
	}

	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
		case R.id.face_btn:
			mInputMethodManager.hideSoftInputFromWindow(mMsgInput.getWindowToken(), 0);
			try
			{
				Thread.sleep(80);// �����ʱ���һ����Ļ������
			} catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			mMediaSelectWindow.setVisibility(View.GONE);
			mFaceSelectWindow.setVisibility(View.VISIBLE);
			mWindowState = SHOW_FACE_DIALOG;
			break;

		case R.id.send_msg_btn:// ������Ϣ
			sendTextMessageIfNotNull();
			break;

		case R.id.add_media_btn:
			mInputMethodManager.hideSoftInputFromWindow(mMsgInput.getWindowToken(), 0);
			try
			{
				Thread.sleep(80);// �����ʱ���һ����Ļ������
			} catch (InterruptedException e)
			{
				e.printStackTrace();
			}

			mFaceSelectWindow.setVisibility(View.GONE);
			mMediaSelectWindow.setVisibility(View.VISIBLE);
			mWindowState = SHOW_MEDIA_SELECT_DIALOG;

			break;

		case R.id.record_switcher:
			if (mIsRecordState)
			{
				mRecordSwitcher.setBackgroundResource(R.drawable.chatting_setmode_voice_btn);
				mInputSendContainer.setVisibility(View.VISIBLE);
				mRecordBtn.setVisibility(View.GONE);

			} else
			{
				mRecordSwitcher.setBackgroundResource(R.drawable.chatting_setmode_keyboard_btn);
				mRecordBtn.setVisibility(View.VISIBLE);
				mInputSendContainer.setVisibility(View.GONE);
			}

			mFaceSelectWindow.setVisibility(View.GONE);
			mMediaSelectWindow.setVisibility(View.GONE);
			mIsRecordState = !mIsRecordState;
			mWindowState = SHOW_NOTHING;
			break;

		default:
			break;
		}
	}

	private void sendTextMessageIfNotNull()
	{
		if (mMsgInput.getText().length() >= 1)
		{
			if (mXxService != null)
			{
				Message message = new Message();
				Log.i("JabberID", mPeerJabberID);
				message.setTo(mPeerJabberID);
				message.setBody(mMsgInput.getText().toString());
				message.setProperty(ChatConstants.MEDIA_TYPE, ChatConstants.MEDIA_TYPE_NORMAL);
				mXxService.sendMessage(message, ChatConstants.DS_SENT_OR_READ, false);

				if (!mXxService.isAuthenticated())
					T.showShort(this, "��Ϣ�Ѿ����������");
			}
			mMsgInput.setText(null);
			// mSendMsgBtn.setEnabled(false);
		}
	}

	private void sendAuidoMessage(String audioUrl, String audioTime)
	{
		if (mXxService != null)
		{
			Message message = new Message();
			message.setTo(mPeerJabberID);
			message.setProperty(ChatConstants.MEDIA_TYPE, ChatConstants.MEDIA_TYPE_AUDIO);
			message.setProperty("mediaUrl", audioUrl);
			message.setProperty("mediaSize", audioTime);

			Log.i("Message", "Send Audio");

			mXxService.sendMessage(message, ChatConstants.DS_UPLOADING, false);
			if (!mXxService.isAuthenticated())
				T.showShort(this, "��Ϣ�Ѿ����������");
		}
	}

	private void sendFileMessage(String fileUrl)
	{
		if (mXxService != null)
		{
			Message message = new Message();
			message.setTo(mPeerJabberID);
			message.setBody("");
			message.setProperty(ChatConstants.MEDIA_TYPE, ChatConstants.MEDIA_TYPE_FILE);
			message.setProperty(ChatConstants.MEDIA_URL, fileUrl);
			message.setProperty(ChatConstants.MEDIA_SIZE, "1.5");

			mXxService.sendMessage(message, ChatConstants.DS_UPLOADING, false);
			if (!mXxService.isAuthenticated())
				T.showShort(this, "��Ϣ�Ѿ����������");
		}
	}

	private void sendImageMessage(String imageUrl, Boolean compress)
	{
		if (mXxService != null)
		{
			Message message = new Message();
			message.setTo(mPeerJabberID);
			message.setProperty(ChatConstants.MEDIA_TYPE, ChatConstants.MEDIA_TYPE_IMAGE);
			message.setProperty(ChatConstants.MEDIA_URL, imageUrl);
			message.setProperty(ChatConstants.MEDIA_SIZE, "10.5");

			mXxService.sendMessage(message, ChatConstants.DS_UPLOADING, compress);
			if (!mXxService.isAuthenticated())
				T.showShort(this, "��Ϣ�Ѿ����������");
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		//Ԥ��ͼƬʱ��㴥��ʵ�����ź��ƶ�
		if (event.getActionMasked() == MotionEvent.ACTION_POINTER_UP)
			Log.d("Infor", "������");
		switch (event.getActionMasked())
		{
		case MotionEvent.ACTION_DOWN:
			matrix.set(mPreview.getImageMatrix());
			savedMatrix.set(matrix);
			start.set(event.getX(), event.getY());
			Log.d("Infor", "������...");
			mode = DRAG;
			break;
		case MotionEvent.ACTION_POINTER_DOWN: // ��㴥��
			oldDist = this.spacing(event);
			if (oldDist > 10f)
			{
				Log.d("Infor", "oldDist" + oldDist);
				savedMatrix.set(matrix);
				midPoint(mid, event);
				mode = ZOOM;
			}
			break;
		case MotionEvent.ACTION_POINTER_UP:
			mode = NONE;
			break;
		case MotionEvent.ACTION_MOVE:
			if (mode == DRAG)
			{ // ��ʵ��ͼƬ���϶�����...
				matrix.set(savedMatrix);
				matrix.postTranslate(event.getX() - start.x, event.getY() - start.y);
			} else if (mode == ZOOM)
			{// ��ʵ��ͼƬ�����Ź���...
				float newDist = spacing(event);
				if (newDist > 10)
				{
					matrix.set(savedMatrix);
					float scale = newDist / oldDist;
					matrix.postScale(scale, scale, mid.x, mid.y);
				}
			}
			break;
		}
		mPreview.setImageMatrix(matrix);

		switch (v.getId())
		{
		case R.id.msg_listView:
			mInputMethodManager.hideSoftInputFromWindow(mMsgInput.getWindowToken(), 0);
			mFaceSelectWindow.setVisibility(View.GONE);
			mMediaSelectWindow.setVisibility(View.GONE);

			mWindowState = SHOW_NOTHING;

			break;

		case R.id.chat_msg_input:
			mInputMethodManager.showSoftInput(mMsgInput, 0);
			mFaceSelectWindow.setVisibility(View.GONE);
			mMediaSelectWindow.setVisibility(View.GONE);

			if (mMsgInput.getText().length() > 0)
			{
				mSendMsgBtn.setVisibility(View.VISIBLE);
				mAddMediaBtn.setVisibility(View.GONE);
			} else
			{
				mSendMsgBtn.setVisibility(View.GONE);
				mAddMediaBtn.setVisibility(View.VISIBLE);
			}
			// mSendMsgBtn.setVisibility(View.VISIBLE);
			// mAddMediaBtn.setVisibility(View.GONE);

			mWindowState = SHOW_NOTHING;

			break;

		default:
			break;
		}
		return false;
	}

	private void initFacePage()
	{
		// TODO Auto-generated method stub
		List<View> lv = new ArrayList<View>();
		for (int i = 0; i < Wanglai.NUM_PAGE; ++i)
			lv.add(getGridView(i));

		FacePageAdapter adapter = new FacePageAdapter(lv);
		mFaceViewPager.setAdapter(adapter);
		mFaceViewPager.setCurrentItem(mCurrentPage);

		CirclePageIndicator indicator = (CirclePageIndicator) findViewById(R.id.indicator);
		indicator.setViewPager(mFaceViewPager);
		adapter.notifyDataSetChanged();
		mFaceSelectWindow.setVisibility(View.GONE);
		indicator.setOnPageChangeListener(new OnPageChangeListener()
		{

			@Override
			public void onPageSelected(int arg0)
			{
				mCurrentPage = arg0;
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2)
			{
				// do nothing
			}

			@Override
			public void onPageScrollStateChanged(int arg0)
			{
				// do nothing
			}
		});

	}

	private GridView getGridView(int i)
	{
		// TODO Auto-generated method stub
		GridView gv = new GridView(this);
		gv.setNumColumns(7);
		gv.setSelector(new ColorDrawable(Color.TRANSPARENT));// ����GridViewĬ�ϵ��Ч��
		gv.setBackgroundColor(Color.TRANSPARENT);
		gv.setCacheColorHint(Color.TRANSPARENT);
		gv.setHorizontalSpacing(1);
		gv.setVerticalSpacing(1);
		gv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		gv.setGravity(Gravity.CENTER);
		gv.setAdapter(new FaceAdapter(this, i));
		gv.setOnTouchListener(forbidenScroll());
		gv.setOnItemClickListener(new OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
			{
				// TODO Auto-generated method stub
				if (arg2 == Wanglai.NUM)
				{// ɾ������λ��
					int selection = mMsgInput.getSelectionStart();
					String text = mMsgInput.getText().toString();
					if (selection > 0)
					{
						String text2 = text.substring(selection - 1);
						if ("]".equals(text2))
						{
							int start = text.lastIndexOf("[");
							int end = selection;
							mMsgInput.getText().delete(start, end);
							return;
						}
						mMsgInput.getText().delete(selection - 1, selection);
					}
				} else
				{
					int count = mCurrentPage * Wanglai.NUM + arg2;
					// ע�͵Ĳ��֣���EditText����ʾ�ַ���
					// String ori = msgEt.getText().toString();
					// int index = msgEt.getSelectionStart();
					// StringBuilder stringBuilder = new StringBuilder(ori);
					// stringBuilder.insert(index, keys.get(count));
					// msgEt.setText(stringBuilder.toString());
					// msgEt.setSelection(index + keys.get(count).length());

					// �����ⲿ�֣���EditText����ʾ����
					Bitmap bitmap = BitmapFactory.decodeResource(getResources(),
							(Integer) Wanglai.getInstance().getFaceMap().values().toArray()[count]);
					if (bitmap != null)
					{
						int rawHeigh = bitmap.getHeight();
						int rawWidth = bitmap.getHeight();
						int newHeight = 40;
						int newWidth = 40;
						// ������������
						float heightScale = ((float) newHeight) / rawHeigh;
						float widthScale = ((float) newWidth) / rawWidth;
						// �½�������
						Matrix matrix = new Matrix();
						matrix.postScale(heightScale, widthScale);
						// ����ͼƬ����ת�Ƕ�
						// matrix.postRotate(-30);
						// ����ͼƬ����б
						// matrix.postSkew(0.1f, 0.1f);
						// ��ͼƬ��Сѹ��
						// ѹ����ͼƬ�Ŀ�͸��Լ�kB��С����仯
						Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, rawWidth, rawHeigh, matrix, true);
						ImageSpan imageSpan = new ImageSpan(ChatActivity.this, newBitmap);
						String emojiStr = mFaceMapKeys.get(count);
						SpannableString spannableString = new SpannableString(emojiStr);
						spannableString.setSpan(imageSpan, emojiStr.indexOf('['), emojiStr.indexOf(']') + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
						mMsgInput.append(spannableString);
					} else
					{
						String ori = mMsgInput.getText().toString();
						int index = mMsgInput.getSelectionStart();
						StringBuilder stringBuilder = new StringBuilder(ori);
						stringBuilder.insert(index, mFaceMapKeys.get(count));
						mMsgInput.setText(stringBuilder.toString());
						mMsgInput.setSelection(index + mFaceMapKeys.get(count).length());
					}
				}
			}
		});
		return gv;
	}

	// ��ֹ��pageview�ҹ���
	private OnTouchListener forbidenScroll()
	{
		return new OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				if (event.getAction() == MotionEvent.ACTION_MOVE)
				{
					Log.i("PapeView", "OnTouch");
					return true;
				}
				return false;
			}
		};
	}

	@Override
	public void connectionStatusChanged(int connectedState, String reason)
	{
		// TODO Auto-generated method stub

	}

	public enum Type
	{
		normal,

		chat,

		groupchat,

		headline,

		error;

		public static Type fromString(String name)
		{
			try
			{
				return Type.valueOf(name);
			} catch (Exception e)
			{
				return normal;
			}
		}
	}

	private void Nav(String url)
	{
		Uri uri = Uri.parse(url);
		startActivity(new Intent(Intent.ACTION_VIEW, uri));
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{

		if (keyCode == KeyEvent.KEYCODE_BACK && SHOW_NOTHING != mWindowState)
		{
			mFaceSelectWindow.setVisibility(View.GONE);
			mMediaSelectWindow.setVisibility(View.GONE);
			mWindowState = SHOW_NOTHING;

			if (mMsgInput.getText().length() > 0)
			{
				mSendMsgBtn.setVisibility(View.VISIBLE);
				mAddMediaBtn.setVisibility(View.GONE);
			} else
			{
				mSendMsgBtn.setVisibility(View.GONE);
				mAddMediaBtn.setVisibility(View.VISIBLE);
			}

			return true;
		}
		return super.onKeyDown(keyCode, event);
	}


	/*
	 * private class DownloadImgThread extends Thread{ private String mImageUrl;
	 * 
	 * public DownloadImgThread(String imageUrl){ mImageUrl = imageUrl; }
	 * 
	 * @Override public void run() { try { Bitmap bmp =
	 * ImageService.getImage(mImageUrl); if(bmp != null){
	 * handler.obtainMessage(MSG_SUCCESS, bmp).sendToTarget(); }else {
	 * handler.obtainMessage(MSG_FAILURE).sendToTarget(); } } catch (Exception
	 * e) { e.printStackTrace(); } } }
	 */

	private void alert()
	{
		Dialog dialog = new AlertDialog.Builder(this).setTitle("��ʾ").setMessage("�ϴ��ļ�ʧ��")
				.setPositiveButton("ȷ��", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						// mImageUrl = null;
					}
				}).create();
		dialog.show();
	}

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

	private float spacing(MotionEvent event)
	{

		float x = event.getX(0) - event.getX(1);

		float y = event.getY(0) - event.getY(1);

		return FloatMath.sqrt(x * x + y * y);

	}

	private void midPoint(PointF point, MotionEvent event)
	{

		float x = event.getX(0) + event.getX(1);

		float y = event.getY(0) + event.getY(1);

		point.set(x / 2, y / 2);

	}

	
	
	//����λ�ü�����
    private LocationListener locationListener = new LocationListener(){
        //λ�÷����ı�ʱ����
        @Override
        public void onLocationChanged(Location location) {
            Log.d("Location", "onLocationChanged");
            Log.d("Location", "onLocationChanged Latitude" + location.getLatitude());
                 Log.d("Location", "onLocationChanged location" + location.getLongitude());
        }

        //providerʧЧʱ����
        @Override
        public void onProviderDisabled(String provider) {
            Log.d("Location", "onProviderDisabled");
        }

        //provider����ʱ����
        @Override
        public void onProviderEnabled(String provider) {
            Log.d("Location", "onProviderEnabled");
        }

        //״̬�ı�ʱ����
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d("Location", "onStatusChanged");
        }
    };
    
}
