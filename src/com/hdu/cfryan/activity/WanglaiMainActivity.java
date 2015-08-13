package com.hdu.cfryan.activity;

import java.util.ArrayList;

import com.hdu.cfryan.R;
import com.hdu.cfryan.db.WanglaiDatabaseHelper;
import com.hdu.cfryan.fragment.*;
import com.hdu.cfryan.service.IAvatarStatusCallback;
import com.hdu.cfryan.service.IConnectionStatusCallback;
import com.hdu.cfryan.service.XXService;
import com.hdu.cfryan.util.L;
import com.hdu.cfryan.util.ActivityManager;
import com.hdu.cfryan.util.PreferenceConstants;
import com.hdu.cfryan.util.PreferenceUtils;

import android.R.layout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBar.TabListener;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabWidget;
import android.widget.TextView;

public class WanglaiMainActivity extends ActionBarActivity implements IAvatarStatusCallback, IConnectionStatusCallback
{
	private ActionBar actionBar;
	private MenuItem searchView;// 搜索视窗
	private MenuItem settingsView;// 设置视窗
	private ViewPager mPager;// 滑动页面
	private ArrayList<Fragment> mFragmentsList;// fragments页面
	private RecentChatFragment recentChatFragment;
	private ContactFragment contactFragment;
	private MeFragment meFragment;
	private XXService mXxService;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wang_lai_main);
		ActivityManager.getInstance().addActivity(this);
		// if (savedInstanceState == null)
		// {
		// getSupportFragmentManager().beginTransaction().add(R.id.container,
		// new PlaceholderFragment()).commit();
		// }

		startService(new Intent(WanglaiMainActivity.this, XXService.class));
		bindXMPPService();

		// 得到当前的ActionBar
		actionBar = getSupportActionBar();
		// 设置ActionBar标题
		actionBar.setTitle("往来");
		// 设置title下方的小标题
		// actionBar.setSubtitle("demo");

		// 定义ActionBar模式为NAVIGATION_MODE_TABS
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.addTab(actionBar.newTab().setText("消息").setTabListener(MyBarTabListener));

		actionBar.addTab(actionBar.newTab().setText("通讯录").setTabListener(MyBarTabListener));

		actionBar.addTab(actionBar.newTab().setText("我").setTabListener(MyBarTabListener));

		mPager = (ViewPager) findViewById(R.id.vPager);

		mFragmentsList = new ArrayList<Fragment>();

		recentChatFragment = new RecentChatFragment();
		contactFragment = new ContactFragment();
		meFragment = new MeFragment();

		mFragmentsList.add(recentChatFragment);
		mFragmentsList.add(contactFragment);
		mFragmentsList.add(meFragment);
		mPager.setAdapter(new MyFragmentPagerAdapter(getSupportFragmentManager(), mFragmentsList));

		mPager.setOnPageChangeListener(new MyPageScrollEvent());
		mPager.setCurrentItem(0);

	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		unbindXMPPService();
	}

	// 获取应用的服务
	ServiceConnection mServiceConnection = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			mXxService = ((XXService.XXBinder) service).getService();
			mXxService.registerAvatarStatusCallback(WanglaiMainActivity.this);
			// 开始连接xmpp服务器
			if (!mXxService.isAuthenticated())
			{
				String usr = PreferenceUtils.getPrefString(WanglaiMainActivity.this, PreferenceConstants.ACCOUNT, "");
				String password = PreferenceUtils.getPrefString(WanglaiMainActivity.this, PreferenceConstants.PASSWORD, "");
				mXxService.Login(usr, password);
		//		mXxService.setStatusFromConfig();

			} else
			{
				/*
				 * mTitleNameView.setText(XMPPHelper
				 * .splitJidAndServer(PreferenceUtils.getPrefString(
				 * MainActivity.this, PreferenceConstants.ACCOUNT, "")));
				 * setStatusImage(true);
				 */
				mXxService.setStatusFromConfig();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name)
		{
			mXxService.unRegisterAvatarStatusCallback();
			mXxService = null;
		}

	};

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
		bindService(mServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE + Context.BIND_DEBUG_UNBIND);
	}

	/**
	 * 用于ViewPager滑动时触发.
	 * 
	 * http://docs.eoeandroid.com/reference/android/support/v4/view/ViewPager.
	 * OnPageChangeListener.html onPageScrollStateChanged(int state) Called when
	 * the scroll state changes.
	 * 
	 * onPageScrolled(int position, float positionOffset, int
	 * positionOffsetPixels) This method will be invoked when the current page
	 * is scrolled, either as part of a programmatically initiated smooth scroll
	 * or a user initiated touch scroll.
	 * 
	 * onPageSelected(int position) This method will be invoked when a new page
	 * becomes selected.
	 */
	class MyPageScrollEvent implements OnPageChangeListener
	{
		private final static String TAG = "MyPageScrollEvent";

		@Override
		public void onPageScrollStateChanged(int arg0)
		{
			// TODO Auto-generated method stub
			Log.d(TAG, "onPageScrollStateChanged");
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2)
		{
			// TODO Auto-generated method stub
			Log.d(TAG, "onPageScrolled");
		}

		@Override
		public void onPageSelected(int arg0)
		{
			// TODO Auto-generated method stub
			Log.d(TAG, "onPageSelected");
			// 当滑动ViewPager 换页时，将ActionBar上的Tab显示到对应页
			actionBar.selectTab(actionBar.getTabAt(arg0));
		}
	}

	/**
	 * FragmentPagerAdapter继承自PagerAdapter,是其中的一种实现。 它将每一个页面表示为一个
	 * Fragment，并且每一个Fragment都将会保存到fragment manager当中。
	 * 而且，当用户没可能再次回到页面的时候，fragment manager才会将这个Fragment销毁。.
	 * 
	 * @author XCL
	 * 
	 */
	public class MyFragmentPagerAdapter extends FragmentPagerAdapter
	{
		private ArrayList<Fragment> fragmentsList;

		public MyFragmentPagerAdapter(FragmentManager fm)
		{
			super(fm);
		}

		public MyFragmentPagerAdapter(FragmentManager fm, ArrayList<Fragment> fragments)
		{
			super(fm);
			this.fragmentsList = fragments;
		}

		@Override
		public Fragment getItem(int arg0)
		{
			// TODO Auto-generated method stub
			return fragmentsList.get(arg0);
		}

		@Override
		public int getCount()
		{
			// TODO Auto-generated method stub
			return fragmentsList.size();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.options, menu);

		// 搜索视窗
		searchView = menu.findItem(R.id.menu_search);
		searchView.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		// 设置视窗
		settingsView = menu.findItem(R.id.menu_settings);
		settingsView.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment
	{

		public PlaceholderFragment()
		{
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			View rootView = inflater.inflate(R.layout.fragment_wang_lai_main, container, false);
			return rootView;
		}
	}

	private final TabListener MyBarTabListener = new TabListener()
	{
		private final static String TAG = "MyBarTabListener";

		@Override
		public void onTabReselected(Tab arg0, FragmentTransaction arg1)
		{
			// TODO Auto-generated method stub
			Log.d(TAG, "onTabReselected");
		}

		@Override
		public void onTabSelected(Tab arg0, FragmentTransaction arg1)
		{
			// TODO Auto-generated method stub
			Log.d(TAG, "onTabSelected");
			// 当更改ActionBar上的Tab页有更改时，ViewPager显示相关的页面
			if (mPager != null)
				mPager.setCurrentItem(arg0.getPosition());
		}

		@Override
		public void onTabUnselected(Tab arg0, FragmentTransaction arg1)
		{
			// TODO Auto-generated method stub
			Log.d(TAG, "onTabUnselected");
		}
	};

	@Override
	public void AvatarStatusChanged(int avatarStatus)
	{
		L.i("wanglaimain_____contact", "refresh!!!!");
		contactFragment.refresh();
	}

	@Override
	public void connectionStatusChanged(int connectedState, String reason)
	{
		switch (connectedState)
		{
		case XXService.CONNECTED:
			// mTitleNameView.setText(XMPPHelper.splitJidAndServer(PreferenceUtils
			// .getPrefString(MainActivity.this,
			// PreferenceConstants.ACCOUNT, "")));
			// mTitleProgressBar.setVisibility(View.GONE);
			// mTitleStatusView.setVisibility(View.GONE);
			// setStatusImage(true);
			break;
		case XXService.CONNECTING:
			// mTitleNameView.setText(R.string.login_prompt_msg);
			// mTitleProgressBar.setVisibility(View.VISIBLE);
			// mTitleStatusView.setVisibility(View.GONE);
			break;
		case XXService.DISCONNECTED:
			// mTitleNameView.setText(R.string.login_prompt_no);
			// mTitleProgressBar.setVisibility(View.GONE);
			// mTitleStatusView.setVisibility(View.GONE);
			break;

		default:
			break;
		}

	}

}
