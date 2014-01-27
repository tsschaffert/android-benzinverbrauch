/*
 * Copyright (c) 2013, 2014 Steffen Schaffert
 * Released under the MIT license.
 * http://www.tss-stuff.de/benzinverbrauch/license
 */
package de.steffenschaffert.benzinverbrauch.util;

/**
 * Can be used in conjunction with a ViewPager. Fragments get notified if they become active
 * and can refresh their content.
 * @author Steffen Schaffert
 */
public interface OnRefreshListener {
    void onRefresh();
}
