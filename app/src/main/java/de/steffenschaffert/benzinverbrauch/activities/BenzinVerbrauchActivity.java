/*
 * Copyright (c) 2013 Steffen Schaffert
 * Released under the MIT license.
 * http://www.tss-stuff.de/benzinverbrauch/license
 */
package de.steffenschaffert.benzinverbrauch.activities;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Window;
import de.steffenschaffert.benzinverbrauch.R;
import de.steffenschaffert.benzinverbrauch.fragments.CalculateUsageFragment;
import de.steffenschaffert.benzinverbrauch.fragments.ListEntriesFragment;
import de.steffenschaffert.benzinverbrauch.fragments.ShowUsageFragment;

public class BenzinVerbrauchActivity extends FragmentActivity {
	private FragmentTabHost tabHost;
	private GestureDetectorCompat gestureDetector;

	public static final String KEY_CURRENT_TAB = "de.steffenschaffert.benzinverbrauch.KEY_CURRENT_TAB";
	public static final int TAB_COUNT = 3;

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Configure Action Bar if Android Version >= 3.0
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ActionBar actionBar = getActionBar();
			actionBar.setHomeButtonEnabled(false);
			actionBar.setDisplayShowTitleEnabled(false);
		}

		// Set settings to default if neccessary
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		gestureDetector = new GestureDetectorCompat(this, new MyGestureListener());

		// Setup tabs
		tabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
		tabHost.setup(this, getSupportFragmentManager(), R.id.realtabcontent);


		tabHost.addTab(tabHost.newTabSpec("usage").setIndicator(getString(R.string.showUsage_tab), getResources().getDrawable(R.drawable.ic_tab_usage)),
				ShowUsageFragment.class, null);
		tabHost.addTab(tabHost.newTabSpec("entry").setIndicator(getString(R.string.listEntries_tab), getResources().getDrawable(R.drawable.ic_tab_list)),
				ListEntriesFragment.class, null);
		tabHost.addTab(
				tabHost.newTabSpec("calc").setIndicator(getString(R.string.calculateUsage_tab), getResources().getDrawable(R.drawable.ic_tab_calculate)),
				CalculateUsageFragment.class, null);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		// Save active tab
		outState.putInt(KEY_CURRENT_TAB, tabHost.getCurrentTab());
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		// Restore active tab if available
		if (savedInstanceState != null) {
			tabHost.setCurrentTab(savedInstanceState.getInt(KEY_CURRENT_TAB, 0));
		}
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		// Forward context menu selection to active fragment (if
		// ListEntriesFragment)
		if (featureId == Window.FEATURE_CONTEXT_MENU) {
			Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.realtabcontent);
			if (currentFragment instanceof ListEntriesFragment) {
				boolean handled = ((ListEntriesFragment) currentFragment).onContextItemSelected(item);
				if (handled) {
					return true;
				}
			}
		}

		// Handle options menu
		int itemId = item.getItemId();
		Intent intent = null;
		switch (itemId) {
		case R.id.action_settings:
			intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_add:
			intent = new Intent(this, AddEntryActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_manageCars:
			intent = new Intent(this, ManageCarsActivity.class);
			startActivity(intent);
			return true;
		}
		return false;
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		// Call GestureDetector here to override ListView-Gestures
		gestureDetector.onTouchEvent(event);
		return super.dispatchTouchEvent(event);
	}

	private class MyGestureListener extends SimpleOnGestureListener {
		/**
		 * Switches tabs by swiping left and right
		 */
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			// Check if horizontal movement
			if (Math.abs(velocityX) > Math.abs(velocityY)) {
				int numberOfTabs = TAB_COUNT;// The method mTabHost.getChildCount() doesn't work here for some reason
				int currentTabIndex = tabHost.getCurrentTab();

				// Change index based on direction
				if (velocityX > 0) {
					currentTabIndex--;
				} else {
					currentTabIndex++;
				}

				// switch tab if new index is valid
				if (currentTabIndex < 0 || currentTabIndex >= numberOfTabs) {
					return false;
				} else {
					tabHost.setCurrentTab(currentTabIndex);
					return true;
				}
			} else {
				return false;
			}
		}
	}
}