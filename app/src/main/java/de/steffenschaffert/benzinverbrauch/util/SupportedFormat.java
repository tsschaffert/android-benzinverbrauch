/*
 * Copyright (c) 2013, 2014 Steffen Schaffert
 * Released under the MIT license.
 * http://www.tss-stuff.de/benzinverbrauch/license
 */
package de.steffenschaffert.benzinverbrauch.util;

import java.text.SimpleDateFormat;

/**
 * Manages date formats
 * @author Steffen Schaffert
 *
 */
public class SupportedFormat {
	protected SimpleDateFormat dateFormat;
	protected String dateFormatString;
	
	public SupportedFormat(String dateFormatString)
	{
		this.dateFormat = new SimpleDateFormat(dateFormatString);
		this.dateFormatString = dateFormatString;
	}
	
	public SimpleDateFormat getDateFormat()
	{
		return dateFormat;
	}
	
	public String getDateFormatString()
	{
		return dateFormatString;
	}
}
