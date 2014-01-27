/*
 * Copyright (c) 2013, 2014 Steffen Schaffert
 * Released under the MIT license.
 * http://www.tss-stuff.de/benzinverbrauch/license
 */
package de.steffenschaffert.benzinverbrauch.fragments;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

import de.steffenschaffert.benzinverbrauch.R;
import de.steffenschaffert.benzinverbrauch.config.BenzinVerbrauchConfig;
import de.steffenschaffert.benzinverbrauch.util.DBAccess;
import de.steffenschaffert.benzinverbrauch.util.DateFormatter;

public class AddEntryFragment extends Fragment implements OnClickListener {
	public static final String TAG = "AddEntryFragment";

	private long id = -1;
    private boolean useMileageForCalculation = false;
	private DatePickerDialog datePickerDialog;
	private DateFormatter dateFormatter;
	private Activity parent;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.add_entry_fragment, container, false);
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

		// Find gui elements
		Button cancel = (Button) parent.findViewById(R.id.buttonCancel);
		Button save = (Button) parent.findViewById(R.id.buttonSave);
		Button buttonDatepicker = (Button) parent.findViewById(R.id.buttonDatepicker);
		EditText editTextDate = (EditText) parent.findViewById(R.id.editTextDate);

		// set click-listeners
		cancel.setOnClickListener(this);
		save.setOnClickListener(this);
		buttonDatepicker.setOnClickListener(this);

		dateFormatter = new DateFormatter(parent);

		// Check if new entry or edit event
		Intent mIntent = parent.getIntent();
		id = mIntent.getLongExtra("id", -1);

		if (id == -1) { // New entry
			// Set default date (today)
			Calendar cal = Calendar.getInstance();
			editTextDate.setText(dateFormatter.formatDateToReadableString(cal.getTime()));

            // Change field description if mileage is used for calculation
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(parent);
            useMileageForCalculation = prefs.getBoolean(parent.getString(R.string.prefDataEntryMethod_key), false);
            if(useMileageForCalculation) {
                TextView textViewKm = (TextView)parent.findViewById(R.id.textViewKm);
                EditText editTextKm = (EditText)parent.findViewById(R.id.editTextKm);
                textViewKm.setText(parent.getString(R.string.addEntry_kmMileage));
                editTextKm.setHint(parent.getString(R.string.addEntry_kmMileage));
            }
		} else { // Edit entry
			loadEntry();
		}
	}

	private OnDateSetListener dateSetListener = new OnDateSetListener() {
		@Override
		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			// Convert parameter to date string and write to edit field
			EditText editTextDate = (EditText) parent.findViewById(R.id.editTextDate);
			editTextDate.setText(dateFormatter.formatDateToReadableString(year, monthOfYear, dayOfMonth));
		}
	};

	// Click handler
	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch (id) {
		case R.id.buttonCancel:
			cancelHandler();
			break;
		case R.id.buttonSave:
			saveHandler();
			break;
		case R.id.buttonDatepicker:
			datepickerHandler();
			break;
		}
	}

	/**
	 * Loads an existing entry from database. class member "id" has to be != -1
	 */
	private void loadEntry() {
		if (id == -1)
			return;

		// DB query
		DBAccess db = new DBAccess(parent, BenzinVerbrauchConfig.DB_FILENAME);
		Cursor cursor = db.getEntry(Long.toString(id));

		if (!cursor.moveToFirst())
			return;

		// date conversion
		String date = dateFormatter.convertStringFromDatabaseFormatToReadableFormat(cursor.getString(0));

		// Set entries in UI
		EditText editTextDate = (EditText) parent.findViewById(R.id.editTextDate);
		editTextDate.setText(date);

		EditText editTextKm = (EditText) parent.findViewById(R.id.editTextKm);
		editTextKm.setText(cursor.getString(1));

		EditText editTextFuel = (EditText) parent.findViewById(R.id.editTextFuel);
		editTextFuel.setText(cursor.getString(2));

		EditText editTextPrice = (EditText) parent.findViewById(R.id.editTextPrice);
		editTextPrice.setText(cursor.getString(3));

		CheckBox checkBoxFull = (CheckBox) parent.findViewById(R.id.checkBoxFull);
		checkBoxFull.setChecked(cursor.getInt(4) == 1);

		cursor.close();
		db.close();
	}

	private void cancelHandler() {
		parent.finish();
	}

	/**
	 * Saves a new entry to DB / updates an existing entry if class member "id"
	 * is != -1 User entries are checked.
	 */
	private void saveHandler() {
		DBAccess db = new DBAccess(parent, BenzinVerbrauchConfig.DB_FILENAME);

		// Find GUI elements
		EditText editTextDate = (EditText) parent.findViewById(R.id.editTextDate);
		EditText editTextKm = (EditText) parent.findViewById(R.id.editTextKm);
		EditText editTextFuel = (EditText) parent.findViewById(R.id.editTextFuel);
		EditText editTextPrice = (EditText) parent.findViewById(R.id.editTextPrice);
		CheckBox checkBoxFull = (CheckBox) parent.findViewById(R.id.checkBoxFull);

		// Get user entries
		String date = editTextDate.getText().toString();
		String km = editTextKm.getText().toString();
		String fuel = editTextFuel.getText().toString();
		String price = editTextPrice.getText().toString();
		String full = checkBoxFull.isChecked() ? "1" : "0";

		// Check for empty entries
		if (date.equals("") || km.equals("") || fuel.equals("") || price.equals("")) {
			Toast.makeText(parent, getString(R.string.addEntry_errorEmptyFields), Toast.LENGTH_SHORT).show();
		}
		// Check date format
		else if (!dateFormatter.isValidReadableFormat(date)) {
			Toast.makeText(parent, getString(R.string.addEntry_errorDateFormat), Toast.LENGTH_SHORT).show();
		}
		// OK
		else {
			// Convert date for DB
			date = dateFormatter.convertStringFromReadableFormatToDatabaseFormat(date);

			if (id == -1) { // New entry
                // Check whether distance driven needs to be calculated from mileage
                if (useMileageForCalculation) {
                    try {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(parent);
                        long selectedCarId = prefs.getLong(parent.getString(R.string.prefSelectedCar_key), DBAccess.ID_STANDARD_CAR);

                        // Calculate distance
                        double lastMileage = db.getCarMileage(selectedCarId);
                        double kmDouble = Double.parseDouble(km);
                        kmDouble = kmDouble - lastMileage;

                        // Validate
                        if(kmDouble < 0) {
                            // Input makes no sense
                            Toast.makeText(parent, parent.getString(R.string.addEntry_errorCalculatedDistanceNegative), Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Save input as last mileage
                        db.setCarMileage(selectedCarId, km);

                        // Set distance driven
                        km = ""+kmDouble;
                    }
                    catch (NumberFormatException e) {
                        Log.e(TAG, "Number parsing failed for mileage. Could not calculate distance driven.");
                    }
                }

				db.saveEntry(date, km, fuel, price, full);
			} else { // Update entry
				db.updateEntry(Long.toString(id), date, km, fuel, price, full);
			}

			parent.finish();
		}

		db.close();
	}

	private void datepickerHandler() {
		// Initialise datepicker with date from input field if possible
		EditText editTextDate = (EditText) parent.findViewById(R.id.editTextDate);
		String dateString = editTextDate.getText().toString();

		// Parse date, returns current date on error
		int[] dateArray = dateFormatter.parseReadableDateStringToIntArray(dateString);

		// Create and show dialog
		datePickerDialog = new DatePickerDialog(parent, dateSetListener, dateArray[0], dateArray[1], dateArray[2]);
		datePickerDialog.show();
	}
}
