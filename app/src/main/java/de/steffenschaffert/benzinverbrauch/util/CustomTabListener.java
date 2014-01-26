/*
 * Copyright (c) 2013, 2014 Steffen Schaffert
 * Released under the MIT license.
 * http://www.tss-stuff.de/benzinverbrauch/license
 */
package de.steffenschaffert.benzinverbrauch.util;

import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;

public class CustomTabListener implements ActionBar.TabListener {

    private ViewPager viewPager;
    public CustomTabListener(ViewPager viewPager) {
        this.viewPager = viewPager;
    }

    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        viewPager.setCurrentItem(tab.getPosition());
    }

    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        // ignore
    }

    public void onTabReselected(Tab tab, FragmentTransaction ft) {
        // ignore
    }
}