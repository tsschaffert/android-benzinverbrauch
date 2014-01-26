/*
 * Copyright (c) 2013, 2014 Steffen Schaffert
 * Released under the MIT license.
 * http://www.tss-stuff.de/benzinverbrauch/license
 */
package de.steffenschaffert.benzinverbrauch.activities;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import de.steffenschaffert.benzinverbrauch.R;
import de.steffenschaffert.benzinverbrauch.util.CustomFragmentPagerAdapter;
import de.steffenschaffert.benzinverbrauch.util.CustomTabListener;

public class BenzinVerbrauchActivity extends ActionBarActivity {
	public static final String KEY_CURRENT_TAB = "de.steffenschaffert.benzinverbrauch.KEY_CURRENT_TAB";

    private ViewPager viewPager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

        // Configure ActionBar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Set settings to default if necessary
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Create ViewPager to manage Tabs
        CustomFragmentPagerAdapter pagerAdapter = new CustomFragmentPagerAdapter(getSupportFragmentManager());
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOnPageChangeListener(
            new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    // Select ActionBar tab on swipe
                    getSupportActionBar().setSelectedNavigationItem(position);
                }
            });

        // Create Tabs
        ActionBar.TabListener tabListener = new CustomTabListener(viewPager);
        Tab tabConsumption = actionBar.newTab()
                .setText(R.string.showUsage_tab)
                .setTabListener(tabListener)
                .setIcon(R.drawable.ic_tab_usage);
        Tab tabList = actionBar.newTab()
                .setText(R.string.listEntries_tab)
                .setTabListener(tabListener)
                .setIcon(R.drawable.ic_tab_list);
        Tab tabCalculate = actionBar.newTab()
                .setText(R.string.calculateUsage_tab)
                .setTabListener(tabListener)
                .setIcon(R.drawable.ic_tab_calculate);

        actionBar.addTab(tabConsumption);
        actionBar.addTab(tabList);
        actionBar.addTab(tabCalculate);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		// Save active tab
		outState.putInt(KEY_CURRENT_TAB, viewPager.getCurrentItem());
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		// Restore active tab if available
		if (savedInstanceState != null) {
			viewPager.setCurrentItem(savedInstanceState.getInt(KEY_CURRENT_TAB, 0));
		}
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
}