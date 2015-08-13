package com.hdu.cfryan.adapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.hdu.cfryan.R;
import com.hdu.cfryan.activity.LoginActivity;
import com.hdu.cfryan.db.RosterProvider;
import com.hdu.cfryan.db.WanglaiDatabaseHelper;
import com.hdu.cfryan.db.RosterProvider.RosterConstants;
import com.hdu.cfryan.service.XXService;
import com.hdu.cfryan.service.XXService.XXBinder;
import com.hdu.cfryan.smack.Smack;
import com.hdu.cfryan.smack.SmackImpl;
import com.hdu.cfryan.util.L;
import com.hdu.cfryan.util.PreferenceConstants;
import com.hdu.cfryan.util.PreferenceUtils;
import com.hdu.cfryan.util.StatusMode;

import android.R.layout;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

public class ContactAdapter extends BaseAdapter implements SectionIndexer
{
	private Context mContext;
	private ContentResolver mContentResolver;
	private boolean mIsShowOffline;    // �Ƿ���ʾ������ϵ��

	private List<ContactSortModel> mContactList = null;
	private CharacterParser mCharacterParser;
	private String mFilterString;
	private boolean mIsContact;

	// ������״̬
	private static final String OFFLINE_EXCLUSION = RosterConstants.STATUS_MODE + " != " + StatusMode.offline.ordinal();
	// ��ϵ�˲�ѯ����
	private static final String[] ROSTER_QUERY = new String[]
	{ RosterConstants._ID, RosterConstants.JID, RosterConstants.ALIAS, RosterConstants.STATUS_MODE, RosterConstants.STATUS_MESSAGE, };

	@Override
	public void notifyDataSetChanged()
	{
		mContactList = queryData("friends");
		super.notifyDataSetChanged();
	}
	

	public String getFilterString()
	{
		return mFilterString;
	}

	public void setFilterString(String filterString)
	{
		this.mFilterString = filterString;
	}

	/**
	 * ��ϵ�˵�����������ϵ���б����Ƭ���ܻ��õ���isContact������
	 * @param context
	 * @param isContact
	 */
	public ContactAdapter(Context context, boolean isContact)
	{
		mContext = context;
		mIsContact = isContact;
		mContentResolver = context.getContentResolver();
	//	mIsShowOffline = PreferenceUtils.getPrefBoolean(mContext, PreferenceConstants.SHOW_OFFLINE, true);
		mCharacterParser = CharacterParser.getInstance();
		mContactList = queryData("friends");
	}
	
	/**
	 * ��ListView���ݷ����仯ʱ,���ô˷���������ListView
	 * 
	 * @param list
	 */
	/*
	 * public void updateListView(List<SortModel> list){ this.mSortList = list;
	 * notifyDataSetChanged(); }
	 */

	@Override
	public int getCount()
	{
		return mContactList.size();
	}

	@Override
	public Object getItem(int position)
	{
		return mContactList.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	protected List<Roster> getRosters(String groupname)
	{
		List<Roster> childList = new ArrayList<Roster>();
		
		//��ѯ���� group ��  owner��owner������û���½���⣩
		String selectWhere = RosterConstants.GROUP + " = ?" + " AND " + RosterConstants.OWNER + " = ?";
	//	if (!mIsShowOffline)
	//		selectWhere += " AND " + OFFLINE_EXCLUSION;
		Cursor cursor = mContentResolver.query(RosterProvider.CONTENT_URI, ROSTER_QUERY, selectWhere, new String[]
		{ groupname , PreferenceUtils.getPrefString(mContext, PreferenceConstants.ACCOUNT, "")}, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast())
		{
			Roster roster = new Roster();
			roster.setJid(cursor.getString(cursor.getColumnIndexOrThrow(RosterConstants.JID)));
			roster.setAlias(cursor.getString(cursor.getColumnIndexOrThrow(RosterConstants.ALIAS)));
			roster.setStatus_message(cursor.getString(cursor.getColumnIndexOrThrow(RosterConstants.STATUS_MESSAGE)));
			roster.setStatusMode(cursor.getString(cursor.getColumnIndexOrThrow(RosterConstants.STATUS_MODE)));
			childList.add(roster);
			cursor.moveToNext();
		}
		cursor.close();
		return childList;
	}

	

	private List<ContactSortModel> queryData(String groupname)
	{
		// mSortList = new ArrayList<SortModel>();
		if (mContactList == null)
		{
			mContactList = new ArrayList<ContactSortModel>();
		} else
		{
			mContactList.clear();
		}

		List<Roster> rosters = getRosters(groupname);

		if(mIsContact)
		{
			ContactSortModel sm1 = new ContactSortModel();
			Roster roster = new Roster();
			roster.setAlias("��Ӻ���");
			sm1.setRoster(roster);
			sm1.setSortLetters("@");
			mContactList.add(sm1);

			ContactSortModel sm2 = new ContactSortModel();
			Roster roster2 = new Roster();
			roster2.setAlias("������");
			sm2.setRoster(roster2);
			sm2.setSortLetters("@");
			mContactList.add(sm2);
		}

		for (int i = 0; i < rosters.size(); i++)
		{
			ContactSortModel sortModel = new ContactSortModel();

			sortModel.setRoster(rosters.get(i));
			// ȡroster��alias�ǳƣ���������ת����ƴ��
			String pinyin = mCharacterParser.getSelling(rosters.get(i).getAlias());
			String sortString = pinyin.substring(0, 1).toUpperCase();

			// ������ʽ���ж�����ĸ�Ƿ���Ӣ����ĸ
			if (sortString.matches("[A-Z]"))
			{
				sortModel.setSortLetters(sortString.toUpperCase());
			} else
			{
				sortModel.setSortLetters("#");
			}

			mContactList.add(sortModel);
		}

		return filterData(mFilterString, mContactList);
	}

	/**
	 * ����������е�ֵ���������ݲ�����ListView
	 * 
	 * @param filterStr
	 */
	private List<ContactSortModel> filterData(String filterStr, List<ContactSortModel> contactList)
	{
		List<ContactSortModel> filterContactList = new ArrayList<ContactSortModel>();

		if (TextUtils.isEmpty(filterStr))
		{
			filterContactList = contactList;
		} else
		{
			filterContactList.clear();
			for (ContactSortModel sortModel : contactList)
			{
				String alias = sortModel.getRoster().getAlias();
				if (alias.indexOf(filterStr.toString()) != -1 || mCharacterParser.getSelling(alias).startsWith(filterStr.toString()))
				{
					filterContactList.add(sortModel);
				}
			}

		}

		// ����a-z��������
		Collections.sort(filterContactList, new PinyinComparator());
		return filterContactList;
		// this.updateListView(filterDateList);
	}

	@Override
	public View getView(final int position, View view, ViewGroup arg2)
	{
		ViewHolder viewHolder = null;
		final ContactSortModel item = (ContactSortModel) getItem(position);
		if (view == null)
		{
			viewHolder = new ViewHolder();
			view = LayoutInflater.from(mContext).inflate(R.layout.contact_item, null);
			viewHolder.tvTitle = (TextView) view.findViewById(R.id.title);
			viewHolder.tvLetter = (TextView) view.findViewById(R.id.catalog);
			view.setTag(viewHolder);
		} else
		{
			viewHolder = (ViewHolder) view.getTag();
		}

		// ����position��ȡ���������ĸ��Char asciiֵ
		int section = getSectionForPosition(position);

		// �����ǰλ�õ��ڸ÷�������ĸ��Char��λ�� ������Ϊ�ǵ�һ�γ���
		if ("��Ӻ���".equals(item.getRoster().getAlias()) || "������".equals(item.getRoster().getAlias()))
		{
			viewHolder.tvLetter.setVisibility(View.GONE);
		} else if (position == getPositionForSection(section))
		{
			viewHolder.tvLetter.setVisibility(View.VISIBLE);
			viewHolder.tvLetter.setText(item.getSortLetters());

		} else
		{
			viewHolder.tvLetter.setVisibility(View.GONE);
		}
		
		viewHolder.tvTitle.setText(item.getRoster().getAlias());

		ImageView image = (ImageView) view.findViewById(R.id.iv_icon); // ���ImageView����
		viewHolder.tvImage = image;
		if ("��Ӻ���".equals(item.getRoster().getAlias()))
		{
			viewHolder.tvImage.setBackgroundResource(R.drawable.contact_add_friends);
		} else if ("������".equals(item.getRoster().getAlias()))
		{
			viewHolder.tvImage.setBackgroundResource(R.drawable.contact_new_friends);
		} else
		{
			viewHolder.tvImage.setBackgroundResource(R.drawable.default_mobile_avatar);
			
			File file = new File("mnt/sdcard/Wanglai/Avatar/" + item.getRoster().getJid() + ".png");
			if (file.exists())
			{
				Bitmap bitmap = getLoacalBitmap("mnt/sdcard/Wanglai/Avatar/" + item.getRoster().getJid() + ".png"); // �ӱ���ȡͼƬvcard
				// ����Bitmap
				image.setImageBitmap(bitmap);
				// ����Imageview
				image.setAdjustViewBounds(true);
				image.setMaxHeight(60);
				image.setMaxWidth(60);
				viewHolder.tvImage = image;
				viewHolder.tvImage.setBackgroundResource(0);
			}
		}

		return view;
	}

	/**
	 * ����ListView�ĵ�ǰλ�û�ȡ���������ĸ��Char asciiֵ
	 */
	@Override
	public int getSectionForPosition(int position)
	{
		return mContactList.get(position).getSortLetters().charAt(0);
	}

	/**
	 * ���ݷ��������ĸ��Char asciiֵ��ȡ���һ�γ��ָ�����ĸ��λ��
	 */
	@Override
	public int getPositionForSection(int section)
	{
		for (int i = 0; i < getCount(); i++)
		{
			String sortWord = mContactList.get(i).getSortLetters();
			char firstChar = sortWord.toUpperCase().charAt(0);
			if (firstChar == section)
			{
				return i;
			}
		}

		return -1;
	}


	@Override
	public Object[] getSections()
	{
		return null;
	}

	/**
	 * ���ر���ͼƬ��Vcard��
	 * 
	 * @param url
	 * @return
	 */
	public static Bitmap getLoacalBitmap(String url)
	{
		try
		{
			FileInputStream fis = new FileInputStream(url);
			return BitmapFactory.decodeStream(fis); // ����ת��ΪBitmapͼƬ

		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * contact��viewHolder��������Ԫ�أ���ϵ�˷�����ĸ����ϵ�����ƣ���ϵ��ͷ��
	 * @author Ryan
	 *
	 */
	
	final static class ViewHolder
	{
		TextView tvLetter;
		TextView tvTitle;
		ImageView tvImage;
	}
	
	/**
	 * ��ϵ����Roster��ʵ����
	 * @author Ryan
	 *
	 */
	public class Roster
	{
		private String jid;
		private String alias;
		private String statusMode;
		private String statusMessage;

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

		public String getStatusMode()
		{
			return statusMode;
		}

		public void setStatusMode(String statusMode)
		{
			this.statusMode = statusMode;
		}

		public String getStatusMessage()
		{
			return statusMessage;
		}

		public void setStatus_message(String statusMessage)
		{
			this.statusMessage = statusMessage;
		}
	}
	
	/**
	 * Roster���ǳ�����ĸ��Roster������ɵ�ʵ����
	 * @author Ryan
	 *
	 */
	public class ContactSortModel
	{
		private Roster roster;
		private String sortLetters; // ��ʾ����ƴ��������ĸ

		public Roster getRoster()
		{
			return roster;
		}

		public void setRoster(Roster roster)
		{
			this.roster = roster;
		}

		public String getSortLetters()
		{
			return sortLetters;
		}

		public void setSortLetters(String sortLetters)
		{
			this.sortLetters = sortLetters;
		}
	}



}