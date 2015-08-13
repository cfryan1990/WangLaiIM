package com.hdu.cfryan.adapter;

import java.util.Comparator;

import com.hdu.cfryan.adapter.ContactAdapter.ContactSortModel;


public class PinyinComparator implements Comparator<ContactSortModel> {

	//
	@Override
	public int compare(ContactSortModel o1, ContactSortModel o2) {
		if (o1.getSortLetters().equals("@")
				|| o2.getSortLetters().equals("#")) {
			return -1;
		} else if (o1.getSortLetters().equals("#")
				|| o2.getSortLetters().equals("@")) {
			return 1;
		} else {
			return o1.getSortLetters().compareTo(o2.getSortLetters());
		}
	}

}
