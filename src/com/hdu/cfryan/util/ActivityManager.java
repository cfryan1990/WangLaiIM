package com.hdu.cfryan.util;

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.util.Log;
/**
 * ���ڴ����˳�����ʱ�����˳����е�activity������д��ͨ����
 *
 */
public class ActivityManager {

	private List<Activity> activityList = new LinkedList<Activity>();
	private static ActivityManager instance;

	private ActivityManager() {
	}

	// ����ģʽ�л�ȡΨһ��MyApplicationʵ��
	public static ActivityManager getInstance() {
		if (null == instance) {
			instance = new ActivityManager();
		}
		return instance;
	}

	// ���Activity��������
	public void addActivity(Activity activity) {
		activityList.add(activity);
	}

	// ��������Activity��finish
	public void exit() {
		for (Activity activity : activityList) {
			Log.i("name",activity.getLocalClassName());
		            activity.finish();  
		}
	}
}