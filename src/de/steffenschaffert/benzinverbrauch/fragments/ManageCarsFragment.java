/*
 * Copyright (c) 2013 Steffen Schaffert
 * Released under the MIT license.
 * http://www.tss-stuff.de/benzinverbrauch/license
 */
package de.steffenschaffert.benzinverbrauch.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import de.steffenschaffert.benzinverbrauch.R;
import de.steffenschaffert.benzinverbrauch.config.BenzinVerbrauchConfig;
import de.steffenschaffert.benzinverbrauch.util.DBAccess;

public class ManageCarsFragment extends Fragment implements OnClickListener {
	public static final String TAG = "ManageCarsFragment";

	private Activity parent;
	private DBAccess dbAccess;
	private SimpleCursorAdapter adapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.manage_cars_fragment, container, false);
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

		// Query DB
		dbAccess = new DBAccess(parent, BenzinVerbrauchConfig.DB_FILENAME);
		Cursor cursor = dbAccess.createCarListViewCursor();

		// Create adapter from cursor
		String[] showCols = new String[] { DBAccess.Car.COLUMN_NAME_NAME };
		int[] showViews = new int[] { R.id.textViewListCarName };
		adapter = new SimpleCursorAdapter(parent, R.layout.list_item_car, cursor, showCols, showViews);

		// Connect ListView and adapter
		ListView listView = (ListView) parent.findViewById(R.id.listViewCars);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(itemClickListener);
		registerForContextMenu(listView);

		// Button listener
		Button buttonAddCar = (Button) parent.findViewById(R.id.buttonAddCar);
		buttonAddCar.setOnClickListener(this);

		// Update textView with current Car
		updateCurrentCar();
	}

	private void updateCurrentCar() {
		// Get ID from SharedPrefs and query car name
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(parent);
		long id = prefs.getLong(parent.getString(R.string.prefSelectedCar_key), DBAccess.ID_STANDARD_CAR);
		String name = dbAccess.getCarName(id);

		// Update textview
		TextView textViewCurrentCar = (TextView) parent.findViewById(R.id.textViewCurrentCar);
		textViewCurrentCar.setText(String.format(parent.getString(R.string.manageCars_currentCar), name));
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch (id) {
		case R.id.buttonAddCar:
			addCarName();
			break;
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = parent.getMenuInflater();
		inflater.inflate(R.menu.context_car_list, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

		switch (item.getItemId()) {
		case R.id.context_delete:
			deleteCar(info.id);
			return true;
		case R.id.context_rename:
			renameCar(info.id);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	/**
	 * Creates a new car, name is taken from edittext
	 */
	public void addCarName() {
		// Get name
		EditText editTextNewCarName = (EditText) parent.findViewById(R.id.editTextNewCarName);
		String name = editTextNewCarName.getText().toString();

		// Name has to be set
		if (!name.equals("")) {
			dbAccess.addCar(name);
			editTextNewCarName.setText("");// Clear edittext

			updateListView();
		}
	}

	private void renameCar(long id) {
		// create edittext
		EditText editTextRenameCar = new EditText(parent);
		editTextRenameCar.setText(dbAccess.getCarName(id));

		// create listener
		OnClickListenerRenameDialog clickListenerRenameDialog = new OnClickListenerRenameDialog(id, editTextRenameCar);

		// build and show dialog
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(parent);
		dialogBuilder.setTitle(parent.getString(R.string.manageCars_newName));
		dialogBuilder.setView(editTextRenameCar);
		dialogBuilder.setPositiveButton(parent.getString(R.string.manageCars_buttonSave), clickListenerRenameDialog);
		dialogBuilder.setNegativeButton(parent.getString(R.string.manageCars_buttonCancel), null);
		dialogBuilder.create().show();
	}

	private void deleteCar(long id) {
		// DEFAULT car cannot be deleted
		if (id == DBAccess.ID_STANDARD_CAR) {
			Toast.makeText(parent, parent.getString(R.string.manageCars_errorCannotDelete), Toast.LENGTH_LONG).show();
		} else {
			OnClickListenerDeleteConfirm clickListenerDeleteConfirm = new OnClickListenerDeleteConfirm(id);

			// build and show dialog
			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(parent);
			dialogBuilder.setMessage(parent.getString(R.string.manageCars_confirmationDeleteCar));
			dialogBuilder.setPositiveButton(parent.getString(R.string.manageCars_buttonDelete), clickListenerDeleteConfirm);
			dialogBuilder.setNegativeButton(parent.getString(R.string.manageCars_buttonCancel), null);
			dialogBuilder.create().show();
		}
	}

	private void updateListView() {
		adapter.changeCursor(dbAccess.createCarListViewCursor());
		adapter.notifyDataSetChanged();
	}

	private void updateSelectedCarInSharedPrefsIfNecessary() {
		// get ID from SharedPrefs
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(parent);
		long id = prefs.getLong(parent.getString(R.string.prefSelectedCar_key), DBAccess.ID_STANDARD_CAR);

		// If car preference invalid, select first car
		if (!dbAccess.carExists(id)) {
			long newId = dbAccess.getFirstCarId();

			Editor edit = prefs.edit();
			edit.putLong(parent.getString(R.string.prefSelectedCar_key), newId);
			edit.commit();

			updateCurrentCar();
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		updateListView();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		// close DB connection
		if (dbAccess != null) {
			dbAccess.close();
		}
	}

	// Choose car on click
	private OnItemClickListener itemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			// Save selected car in SharedPrefs
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ManageCarsFragment.this.parent);
			Editor edit = prefs.edit();
			edit.putLong(ManageCarsFragment.this.parent.getString(R.string.prefSelectedCar_key), id);
			edit.commit();

			updateCurrentCar();
		}
	};

	// Custom listener to supply car id
	private class OnClickListenerDeleteConfirm implements DialogInterface.OnClickListener {
		private long id = -1;

		public OnClickListenerDeleteConfirm(long id) {
			this.id = id;
		}

		@Override
		public void onClick(DialogInterface dialog, int which) {
			dbAccess.deleteCar(id);
			updateSelectedCarInSharedPrefsIfNecessary();
			updateListView();
		}
	}

	// Custom listener to supply car id and edittext
	private class OnClickListenerRenameDialog implements DialogInterface.OnClickListener {
		private long id = -1;
		private EditText editTextRenameCar;

		public OnClickListenerRenameDialog(long id, EditText editTextRenameCar) {
			this.id = id;
			this.editTextRenameCar = editTextRenameCar;
		}

		@Override
		public void onClick(DialogInterface dialog, int which) {
			if (editTextRenameCar != null) {
				String newName = editTextRenameCar.getText().toString();
				if (!newName.equals("")) {
					dbAccess.renameCar(id, newName);
					updateListView();
					updateCurrentCar();// name might have changed
				}
			}
		}
	}
}
