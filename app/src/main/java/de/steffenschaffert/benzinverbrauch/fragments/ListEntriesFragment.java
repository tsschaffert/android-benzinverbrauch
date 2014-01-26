/*
 * Copyright (c) 2013, 2014 Steffen Schaffert
 * Released under the MIT license.
 * http://www.tss-stuff.de/benzinverbrauch/license
 */
package de.steffenschaffert.benzinverbrauch.fragments;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;
import de.steffenschaffert.benzinverbrauch.R;
import de.steffenschaffert.benzinverbrauch.activities.AddEntryActivity;
import de.steffenschaffert.benzinverbrauch.config.BenzinVerbrauchConfig;
import de.steffenschaffert.benzinverbrauch.util.DBAccess;
import de.steffenschaffert.benzinverbrauch.util.DateFormatter;

public class ListEntriesFragment extends Fragment {
	public static final String TAG = "ListEntriesFragment";

	private Activity parent;
	private DBAccess dbAccess;
	private SimpleCursorAdapter adapter;
	private DateFormatter dateFormatter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.list_entries_fragment, container, false);
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

		dateFormatter = new DateFormatter(parent);

		// Get entries from DB
		dbAccess = new DBAccess(parent, BenzinVerbrauchConfig.DB_FILENAME);
		Cursor cursor = dbAccess.createEntryListViewCursor();

		// Create adapter with cursor
		String[] showCols = new String[] { "date", "km", "liter", "price" };
		int[] showViews = new int[] { R.id.textViewListDate, R.id.textViewListKm, R.id.textViewListLiter, R.id.textViewListPrice };
		adapter = new SimpleCursorAdapter(parent, R.layout.entry, cursor, showCols, showViews);
		adapter.setViewBinder(viewBinder);

		// Connect ListView and adapter
		ListView listView = (ListView) parent.findViewById(R.id.listView1);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(itemClickListener);
		registerForContextMenu(listView);
	}

	// Formats ListView entries
	private ViewBinder viewBinder = new SimpleCursorAdapter.ViewBinder() {
		/**
		 * @param view
		 *            view element for current column
		 * @param cursor
		 *            points to current line
		 * @param columnIndex
		 *            column index for selected data (index is 0)
		 */
		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			switch (columnIndex) {
			case 1:
				String date = dateFormatter.convertStringFromDatabaseFormatToReadableFormat(cursor.getString(columnIndex));// Format date
				((TextView) view).setText(date + ":");
				break;
			case 2:
				((TextView) view).setText(String.format("%.1f", cursor.getDouble(columnIndex)) + "km");// Km
				break;
			case 3:
				((TextView) view).setText(String.format("%.2f", cursor.getDouble(columnIndex)) + "l");// Liter
				break;
			case 4:
				((TextView) view).setText(String.format("%.3f", cursor.getDouble(columnIndex) / 100.0) + "€");// Price
				break;
			default:
				((TextView) view).setText(cursor.getString(columnIndex));
				break;
			}
			return true;
		}
	};

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = parent.getMenuInflater();
		inflater.inflate(R.menu.context_list, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

		switch (item.getItemId()) {
		case R.id.context_delete:
			deleteEntry(info.id);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	/**
	 * Delete entry from DB by id
	 * 
	 * @param id
	 */
	private void deleteEntry(long id) {
		dbAccess.deleteEntry(id);
		adapter.changeCursor(dbAccess.createEntryListViewCursor());
		adapter.notifyDataSetChanged();
	}

	/**
	 * Start activity to edit entry
	 * 
	 * @param id
	 */
	private void editEntry(long id) {
		Intent intent = new Intent(parent, AddEntryActivity.class);
		intent.putExtra("id", id);
		startActivity(intent);
	}

	@Override
	public void onResume() {
		super.onResume();

		// Update view
		adapter.changeCursor(dbAccess.createEntryListViewCursor());
		adapter.notifyDataSetChanged();
	}

	// edit on click
	private OnItemClickListener itemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			editEntry(id);
		}
	};

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		// Close DB connection
		if (dbAccess != null) {
			dbAccess.close();
		}
	}
}
