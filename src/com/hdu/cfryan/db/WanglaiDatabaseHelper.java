package com.hdu.cfryan.db;

import org.jivesoftware.smack.util.StringUtils;

import com.hdu.cfryan.db.AddPhonesProvider.PhoneConstants;
import com.hdu.cfryan.db.AvatarProvider.AvatarConstants;
import com.hdu.cfryan.db.ChatProvider.ChatConstants;
import com.hdu.cfryan.db.NewFriendsProvider.NewFriendsConstants;
import com.hdu.cfryan.db.RosterProvider.RosterConstants;
import com.hdu.cfryan.util.PreferenceConstants;
import com.hdu.cfryan.util.L;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public final class WanglaiDatabaseHelper extends SQLiteOpenHelper
{
	
	private static String DATABASE_NAME = "wanglai.db";
	private static final int DATABASE_VERSION = 1;
	private static final String TAG = "WanglaiProvider";
	
	private static WanglaiDatabaseHelper mInstance = null;

	public WanglaiDatabaseHelper(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	//µ¥ÀýÄ£Ê½
	public synchronized static WanglaiDatabaseHelper getInstance(Context context)
	{ 
		if (mInstance == null) { 
		mInstance = new WanglaiDatabaseHelper(context); 
		} 
		return mInstance; 
	};
	
	@Override
	public void onCreate(SQLiteDatabase db)
	{
		infoLog("creating new roster table");
		db.execSQL("CREATE TABLE " + PreferenceConstants.TABLE_ROSTER + " ("
				+ BaseColumns._ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ RosterConstants.JID
				+ " TEXT UNIQUE ON CONFLICT REPLACE, "
				+ RosterConstants.ALIAS + " TEXT, "
				+ RosterConstants.STATUS_MODE + " INTEGER, "
				+ RosterConstants.STATUS_MESSAGE + " TEXT, "
				+ RosterConstants.OWNER + " TEXT, "
				+ RosterConstants.GROUP + " TEXT);");
		db.execSQL("CREATE INDEX idx_roster_group ON " + PreferenceConstants.TABLE_ROSTER
				+ " (" + RosterConstants.GROUP + ")");
		db.execSQL("CREATE INDEX idx_roster_alias ON " + PreferenceConstants.TABLE_ROSTER
				+ " (" + RosterConstants.ALIAS + ")");
		db.execSQL("CREATE INDEX idx_roster_status ON " + PreferenceConstants.TABLE_ROSTER
				+ " (" + RosterConstants.STATUS_MODE + ")");
		
		
		infoLog("creating new avatar table");
		db.execSQL("CREATE TABLE " + PreferenceConstants.TABLE_AVATAR + " ("
				+ BaseColumns._ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ AvatarConstants.JID
				+ " TEXT UNIQUE ON CONFLICT REPLACE, "
				+ AvatarConstants.ALIAS + " TEXT, "
				+ AvatarConstants.PHOTO_HASH + " TEXT);");
		
		infoLog("creating new chat table");
		db.execSQL("CREATE TABLE " + PreferenceConstants.TABLE_CHATS + " (" + BaseColumns._ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ ChatConstants.DATE + " INTEGER,"
				+ ChatConstants.DIRECTION + " INTEGER,"
				+ ChatConstants.JID	+ " TEXT,"
				+ ChatConstants.MESSAGE + " TEXT,"
				+ ChatConstants.MEDIA_TYPE + " TEXT,"
				+ ChatConstants.MEDIA_URL + " TEXT,"
				+ ChatConstants.MEDIA_SIZE + " TEXT,"
				+ ChatConstants.DELIVERY_STATUS + " INTEGER,"
				+ ChatConstants.PACKET_ID + " TEXT);");
		
		infoLog("creating new newfriends table");
		db.execSQL("CREATE TABLE " + PreferenceConstants.TABLE_NEW_FRIENDS + " ("
				+ BaseColumns._ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ NewFriendsConstants.JID
				+ " TEXT UNIQUE ON CONFLICT REPLACE, "
				+ NewFriendsConstants.STATUS + " TEXT, "
				+ NewFriendsConstants.NAME + " TEXT);");
		
		infoLog("creating new localphones table");
		db.execSQL("CREATE TABLE " + PreferenceConstants.TABLE_PHONE + " ("
				+ BaseColumns._ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ PhoneConstants.PHONE_NUM
				+ " TEXT UNIQUE ON CONFLICT REPLACE, "
				+ PhoneConstants.STATUS + " TEXT, "
				+ PhoneConstants.NAME + " TEXT);");
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		// TODO Auto-generated method stub
		
	}
	
	private static void infoLog(String data) {
		L.i(TAG, data);
	}

}
