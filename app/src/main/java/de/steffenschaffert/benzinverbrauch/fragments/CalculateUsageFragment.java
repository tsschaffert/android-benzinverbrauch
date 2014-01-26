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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import de.steffenschaffert.benzinverbrauch.R;
import de.steffenschaffert.benzinverbrauch.config.BenzinVerbrauchConfig;
import de.steffenschaffert.benzinverbrauch.util.DBAccess;

public class CalculateUsageFragment extends Fragment implements OnClickListener {
	public static final String TAG = "CalculateUsageFragement";
	private Activity parent;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.calculate_usage_fragment, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// Find parent activity
		parent = getActivity();
		if (parent == null) {
			Log.wtf(TAG, "Activity ist null");
			return;
		}

		// Click listener
		Button buttonCalculateUsage = (Button) parent.findViewById(R.id.buttonCalculateUsage);
		buttonCalculateUsage.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		// Find GUI elements
		TextView textViewCalculationUsage = (TextView) parent.findViewById(R.id.textViewCalculationUsage);
		TextView textViewCalculationPrice = (TextView) parent.findViewById(R.id.textViewCalculationPrice);
		EditText editTextCalculationDistance = (EditText) parent.findViewById(R.id.editTextCalculationDistance);

		// Get values from GUI
		double distance = 0;
		String distanceText = editTextCalculationDistance.getText().toString();
		try {
			distance = Double.parseDouble(distanceText);
		} catch (NumberFormatException e) {
			// Print error message and return
			Toast.makeText(parent, parent.getString(R.string.calculateUsage_errorDistanceEmpty), Toast.LENGTH_SHORT).show();
			return;
		}

		// Access DB
		DBAccess dbAccess = new DBAccess(parent, BenzinVerbrauchConfig.DB_FILENAME);
		double usage = dbAccess.getUsagePer100KmComplete();
		double price = dbAccess.getLastPrice();
		dbAccess.close();

		// Check if entries available
		if (usage != 0) {
			// Calculate
			double calculatedUsageForDistance = usage * distance / 100.0;
			double calculatedPriceForDistance = price / 100.0 * calculatedUsageForDistance;

			// Show calculations
			textViewCalculationUsage.setText(String.format(parent.getString(R.string.calculateUsage_calculatedUsage), calculatedUsageForDistance));
			textViewCalculationPrice.setText(String.format(parent.getString(R.string.calculateUsage_calculatedPrice), calculatedPriceForDistance));
		} else {
			// print message
			Toast.makeText(parent, parent.getString(R.string.calculateUsage_errorNoEntry), Toast.LENGTH_LONG).show();
		}
	}
}
