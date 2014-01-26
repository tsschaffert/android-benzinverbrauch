/*
 * Copyright (c) 2013, 2014 Steffen Schaffert
 * Released under the MIT license.
 * http://www.tss-stuff.de/benzinverbrauch/license
 */
package de.steffenschaffert.benzinverbrauch.util;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import de.steffenschaffert.benzinverbrauch.R;

/**
 * Handles date formatting
 * 
 * @author Steffen Schaffert
 * 
 */
public class DateFormatter {
	private Context context;

	public static final String TAG = "DateFormatter";// Logging

	private HashMap<SupportedFormatEnum, SupportedFormat> supportedFormats;

	public enum SupportedFormatEnum {
		DATABASE, GERMAN, GERMAN_SHORT
	};

	private final SupportedFormatEnum supportedFormatDatabase = SupportedFormatEnum.DATABASE;

	public DateFormatter(Context context) {
		this.context = context;

		// Initialise supported formats
		supportedFormats = new HashMap<DateFormatter.SupportedFormatEnum, SupportedFormat>();
		supportedFormats.put(SupportedFormatEnum.DATABASE, new SupportedFormat("yyyy-MM-dd"));
		supportedFormats.put(SupportedFormatEnum.GERMAN, new SupportedFormat("dd.MM.yyyy"));
		supportedFormats.put(SupportedFormatEnum.GERMAN_SHORT, new SupportedFormat("d.M.yy"));
	}

	/**
	 * Converts a date string from user format to DB format
	 * 
	 * @param dateString
	 * @return date string in DB format, input string on error
	 */
	public String convertStringFromReadableFormatToDatabaseFormat(String dateString) {
		SupportedFormatEnum selectedDateFormat = getSelectedReadableDateFormat();

		return convertStringFromFormatAToFormatB(dateString, selectedDateFormat, supportedFormatDatabase);
	}

	/**
	 * Converts a date string from DB format to user format
	 * 
	 * @return date string in user format, input string on error
	 */
	public String convertStringFromDatabaseFormatToReadableFormat(String dateString) {
		SupportedFormatEnum selectedDateFormat = getSelectedReadableDateFormat();

		return convertStringFromFormatAToFormatB(dateString, supportedFormatDatabase, selectedDateFormat);
	}

	/**
	 * Helper method to convert date string between formats
	 * 
	 * @param dateString
	 * @param sourceFormat
	 * @param targetFormat
	 * @return date string in target format, input string on error
	 */
	private String convertStringFromFormatAToFormatB(String dateString, SupportedFormatEnum sourceFormat, SupportedFormatEnum targetFormat) {
		// Check if conversion neccessary
		if (sourceFormat == targetFormat)
			return dateString;

		try {
			// Try to convert format via Calendar object
			Date date = supportedFormats.get(sourceFormat).getDateFormat().parse(dateString);
			Calendar c = Calendar.getInstance();
			c.setTime(date);
			return getStringFromCalendar(c, targetFormat);
		} catch (ParseException e) {
			Log.e(TAG, "convertStringFromFormatAToFormatB: Parsing failed.", e);
			return dateString;
		}
	}

	/**
	 * Gibt das durch ein Kalender-Objekt repräsentierte Datum als String
	 * zurück. Returns the represented date as string
	 * 
	 * @param c
	 * @param format
	 * @return date string in target format
	 */
	private String getStringFromCalendar(Calendar c, SupportedFormatEnum format) {
		return supportedFormats.get(format).getDateFormat().format(c.getTime());
	}

	/**
	 * Converts date string in user format to int array. Returns current date on
	 * error
	 * 
	 * @param dateString
	 * @return int array: [0]=>year,[1]=>month,[2]=>day
	 */
	public int[] parseReadableDateStringToIntArray(String dateString) {
		int year, month, day;
		Calendar c = Calendar.getInstance();// Defaults to current date
		SupportedFormatEnum selectedDateFormat = getSelectedReadableDateFormat();

		try {
			Date date = supportedFormats.get(selectedDateFormat).getDateFormat().parse(dateString);// Try parsing date
			c.setTime(date);
		} catch (ParseException e) {
			// default was current date
			Log.i(TAG, "parseReadableDateStringToIntArray: Parsing failed.", e);
		}

		// Datepicker can't handle year < 1900
		if (c.get(Calendar.YEAR) < 1900) {
			c = Calendar.getInstance();
		}

		year = c.get(Calendar.YEAR);
		month = c.get(Calendar.MONTH);
		day = c.get(Calendar.DAY_OF_MONTH);

		return new int[] { year, month, day };
	}

	/**
	 * Converts date object to date string in user format
	 * 
	 * @param date
	 * @return date string in user format
	 */
	public String formatDateToReadableString(Date date) {
		SupportedFormatEnum selectedDateFormat = getSelectedReadableDateFormat();
		return supportedFormats.get(selectedDateFormat).getDateFormat().format(date);
	}

	/**
	 * Converts int array (see parseReadableDateStringToIntArray) to date string
	 * in user format
	 * 
	 * @param year
	 * @param month
	 *            January=0,...,December=11 ("interesting" Java format)
	 * @param day
	 * @return date string in user format
	 */
	public String formatDateToReadableString(int year, int month, int day) {
		SupportedFormatEnum selectedDateFormat = getSelectedReadableDateFormat();

		// Create calendar object
		Calendar c = Calendar.getInstance();
		c.set(year, month, day);

		return supportedFormats.get(selectedDateFormat).getDateFormat().format(c.getTime());
	}

	/**
	 * Check if date string is in valid user format
	 * 
	 * @param dateString
	 * @return
	 */
	public boolean isValidReadableFormat(String dateString) {
		SupportedFormatEnum selectedDateFormat = getSelectedReadableDateFormat();
		return isValidFormat(dateString, selectedDateFormat);
	}

	/**
	 * Check if date string is in valid DB format
	 * 
	 * @param dateString
	 * @return
	 */
	public boolean isValidDatabaseFormat(String dateString) {
		return isValidFormat(dateString, supportedFormatDatabase);
	}

	/**
	 * Helper method to check date string
	 * 
	 * @param dateString
	 * @param format
	 * @return
	 */
	private boolean isValidFormat(String dateString, SupportedFormatEnum format) {
		try {
			supportedFormats.get(format).getDateFormat().parse(dateString);
			return true;
		} catch (ParseException e) {
			return false;
		}
	}

	/**
	 * Read user format from SharedPrefs and returns SupportedFormatEnum type.
	 * DB format is default
	 * 
	 * @return
	 */
	public SupportedFormatEnum getSelectedReadableDateFormat() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String format = prefs.getString(context.getString(R.string.prefDateFormat_key), "DATABASE");

		if (format.equals("DATABASE")) {
			return SupportedFormatEnum.DATABASE;
		} else if (format.equals("GERMAN")) {
			return SupportedFormatEnum.GERMAN;
		} else if (format.equals("GERMAN_SHORT")) {
			return SupportedFormatEnum.GERMAN_SHORT;
		} else {
			Log.w(TAG, "date format '" + format + "' unknown.");
			return SupportedFormatEnum.DATABASE;
		}
	}
}
