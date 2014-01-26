/*
 * Copyright (c) 2013, 2014 Steffen Schaffert
 * Released under the MIT license.
 * http://www.tss-stuff.de/benzinverbrauch/license
 */
package de.steffenschaffert.benzinverbrauch.util;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import de.steffenschaffert.benzinverbrauch.fragments.CalculateUsageFragment;
import de.steffenschaffert.benzinverbrauch.fragments.ListEntriesFragment;
import de.steffenschaffert.benzinverbrauch.fragments.ShowUsageFragment;

public class CustomFragmentPagerAdapter extends FragmentPagerAdapter{
    public static final int TAB_COUNT = 3;

    public CustomFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int i) {
        switch (i) {
            case 0:
                return new ShowUsageFragment();
            case 1:
                return new ListEntriesFragment();
            case 2:
                return new CalculateUsageFragment();
        }

        return null;
    }

    @Override
    public int getCount() {
        return TAB_COUNT;
    }
}
