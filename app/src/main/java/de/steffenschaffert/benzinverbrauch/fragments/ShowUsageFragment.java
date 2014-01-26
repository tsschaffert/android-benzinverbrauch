/*
 * Copyright (c) 2013, 2014 Steffen Schaffert
 * Released under the MIT license.
 * http://www.tss-stuff.de/benzinverbrauch/license
 */
package de.steffenschaffert.benzinverbrauch.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import de.steffenschaffert.benzinverbrauch.R;
import de.steffenschaffert.benzinverbrauch.config.BenzinVerbrauchConfig;
import de.steffenschaffert.benzinverbrauch.util.DBAccess;

public class ShowUsageFragment extends Fragment {
	public static final String TAG = "ShowUsageFragment";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.show_usage_fragment, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onResume() {
		super.onResume();

		// Find parent activity
		Activity parent = getActivity();
		if (parent == null) {
			Log.wtf(TAG, "Activity ist null");
			return;
		}

		// Query DB
		DBAccess db = new DBAccess(parent, BenzinVerbrauchConfig.DB_FILENAME);
		double usageComplete = db.getUsagePer100KmComplete();
		double usageLast = db.getUsagePer100KmLast();

		// Show values
		TextView textViewUsageComplete = (TextView) parent.findViewById(R.id.textViewUsageComplete);
		textViewUsageComplete.setText(String.format(getString(R.string.showUsage_usageComplete), usageComplete));

		TextView textViewUsageLast = (TextView) parent.findViewById(R.id.textViewUsageLast);
		textViewUsageLast.setText(String.format(getString(R.string.showUsage_usageLast), usageLast));

		db.close();
	}
}
