/*
 * Copyright (c) 2013, 2014 Steffen Schaffert
 * Released under the MIT license.
 * http://www.tss-stuff.de/benzinverbrauch/license
 */
package de.steffenschaffert.benzinverbrauch.util;

import de.steffenschaffert.benzinverbrauch.R;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Handles DB queries via helper methods. Methods automatically get values for currently selected car.
 * @author Steffen Schaffert
 *
 */
public class DBAccess extends SQLiteOpenHelper {
	public static final String TAG = "DBAccess";//Logging
	public static final long ID_STANDARD_CAR = 0;
    public static final int DB_VERSION = 3;
	
	private SQLiteDatabase db;
	private Context context;

    public static class Usage {
		public static final String TABLE_NAME = "verbrauch";
		public static final String COLUMN_NAME_ID = "_id";
		public static final String COLUMN_NAME_DATE = "date";
		public static final String COLUMN_NAME_KM = "km";
		public static final String COLUMN_NAME_LITER = "liter";
		public static final String COLUMN_NAME_PRICE = "price";
		public static final String COLUMN_NAME_FULL = "full";
		public static final String COLUMN_NAME_FK_CAR = "fk_car";
	}
	
	public static class Car {
		public static final String TABLE_NAME = "auto";
		public static final String COLUMN_NAME_ID = "_id";
		public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_MILEAGE = "mileage";
	}
	
	public DBAccess(Context activity, String dbName) {
		super(activity,dbName,null,DB_VERSION);
		this.db=getWritableDatabase();
		this.context=activity;
		onCreate(db);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		try {
			String sql="CREATE TABLE IF NOT EXISTS " + Usage.TABLE_NAME +
					" (" +
						Usage.COLUMN_NAME_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
						Usage.COLUMN_NAME_DATE + " DATE NOT NULL," +
						Usage.COLUMN_NAME_KM + " REAL NOT NULL," +
						Usage.COLUMN_NAME_LITER + " REAL NOT NULL," +
						Usage.COLUMN_NAME_PRICE + " REAL NOT NULL," +
						Usage.COLUMN_NAME_FULL + " BOOLEAN NOT NULL," +
						Usage.COLUMN_NAME_FK_CAR + " INTEGER NOT NULL" +
					")";
			db.execSQL(sql);
			
			sql="CREATE TABLE IF NOT EXISTS " + Car.TABLE_NAME +
					" (" +
						Car.COLUMN_NAME_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
						Car.COLUMN_NAME_NAME + " TEXT NOT NULL," +
                        Car.COLUMN_NAME_MILEAGE + " REAL NOT NULL" +
					")";
			db.execSQL(sql);
			
			//Create car with ID 0 if it doesn't exist
			
			if(!carExists(ID_STANDARD_CAR)) {
				ContentValues data = new ContentValues();
				data.put(Car.COLUMN_NAME_ID, ID_STANDARD_CAR);
				data.put(Car.COLUMN_NAME_NAME,"DEFAULT");
                data.put(Car.COLUMN_NAME_MILEAGE, "0");
				db.insert(Car.TABLE_NAME, null, data);
			}
		} catch(Exception e) {
			Log.e(TAG, "Could not create DB.",e);
		}

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if(oldVersion == 1 && newVersion >= 2) {
			String sql="ALTER TABLE " + Usage.TABLE_NAME +
					" ADD COLUMN " + Usage.COLUMN_NAME_FK_CAR + " INTEGER NOT NULL DEFAULT 0";
			db.execSQL(sql);
		}
        // Added column mileage in car table in version 3
        if(oldVersion <= 2 && newVersion == 3) {
            String sql="ALTER TABLE " + Car.TABLE_NAME +
                    " ADD COLUMN " + Car.COLUMN_NAME_MILEAGE + " REAL NOT NULL DEFAULT 0";
            db.execSQL(sql);
        }
	}
	
	/**
	 * Saves an entry to DB for currently selected car.
	 */
	public void saveEntry(String date, String km, String liter, String price, String full) {
		ContentValues data = new ContentValues();
		data.put(Usage.COLUMN_NAME_DATE,date);
		data.put(Usage.COLUMN_NAME_KM, km);
		data.put(Usage.COLUMN_NAME_LITER, liter);
		data.put(Usage.COLUMN_NAME_PRICE,price);
		data.put(Usage.COLUMN_NAME_FULL,full);
		data.put(Usage.COLUMN_NAME_FK_CAR, getSelectedCarId());
		
		db.insert(Usage.TABLE_NAME, null, data);
	}
	
	/**
	 * Updates existing entry. Car ID cannot be changed.
	 */
	public void updateEntry(String id, String date, String km, String liter, String price, String full) {
		ContentValues data = new ContentValues();
		data.put(Usage.COLUMN_NAME_DATE,date);
		data.put(Usage.COLUMN_NAME_KM, km);
		data.put(Usage.COLUMN_NAME_LITER, liter);
		data.put(Usage.COLUMN_NAME_PRICE,price);
		data.put(Usage.COLUMN_NAME_FULL,full);
		
		String[] whereArgs = {id};
		
		db.update(Usage.TABLE_NAME, data, Usage.COLUMN_NAME_ID+"=?", whereArgs);
	}
	
	/**
	 * Return cursor to entry
	 * Cursor doesn't get closed automatically
	 * @param id
	 * @return
	 */
	public Cursor getEntry(String id) {
		String[] cols = new String[] {Usage.COLUMN_NAME_DATE,Usage.COLUMN_NAME_KM,Usage.COLUMN_NAME_LITER,Usage.COLUMN_NAME_PRICE,Usage.COLUMN_NAME_FULL};
		String[] selectionArgs = {id};
		return db.query(Usage.TABLE_NAME,cols , Usage.COLUMN_NAME_ID+"=?", selectionArgs,null,null,null);
	}
	
	/**
	 * Calculates the Usage for all entries
	 */
	public double getUsagePer100KmComplete() {
		String sql = "SELECT " +
				"SUM("+Usage.COLUMN_NAME_LITER+")/SUM("+Usage.COLUMN_NAME_KM+")*100 " +
				"FROM "+Usage.TABLE_NAME+" " +
				"WHERE "+Usage.COLUMN_NAME_DATE+" <= (" +
					"SELECT "+Usage.COLUMN_NAME_DATE+" " +
					"FROM "+Usage.TABLE_NAME+" " +
					"WHERE "+Usage.COLUMN_NAME_FULL+"=1 " +
					"AND "+Usage.COLUMN_NAME_FK_CAR+"=? " +
					"ORDER BY "+Usage.COLUMN_NAME_DATE+" DESC, " + Usage.COLUMN_NAME_ID + " DESC " +
					"LIMIT 1" +
				") " +
				"AND "+Usage.COLUMN_NAME_FK_CAR+"=?";
		
		String selectedCarId = Long.toString(getSelectedCarId());
		
		Cursor c = this.db.rawQuery(sql, new String[]{ selectedCarId,selectedCarId });
		c.moveToFirst();
		double ret = c.getFloat(0);
		c.close();
		return ret;
	}
	
	/**
	 * Calculates the Usage for the last entry
	 * @return
	 */
	public double getUsagePer100KmLast() {
		String sql = "SELECT " +
				Usage.COLUMN_NAME_LITER+"/"+Usage.COLUMN_NAME_KM+"*100 " +
				"FROM "+Usage.TABLE_NAME+" " +
				"WHERE "+Usage.COLUMN_NAME_FULL+"=1 " +
				"AND "+Usage.COLUMN_NAME_FK_CAR+"=? " +
                "ORDER BY "+Usage.COLUMN_NAME_DATE+" DESC, " + Usage.COLUMN_NAME_ID + " DESC " +
				"LIMIT 1";
		Cursor c = this.db.rawQuery(sql, new String[]{ Long.toString(getSelectedCarId()) });
		
		// Return 0 if no entry in DB
		if(!c.moveToFirst()) {
			return 0;
		}

		double ret = c.getFloat(0);
		c.close();
		return ret;
	}
	
	/**
	 * Get Price for last entry
	 * @return
	 */
	public double getLastPrice() {
		String sql = "SELECT "+Usage.COLUMN_NAME_PRICE+" " +
				"FROM "+Usage.TABLE_NAME+" " +
				"WHERE "+Usage.COLUMN_NAME_FK_CAR+"=? " +
                "ORDER BY "+Usage.COLUMN_NAME_DATE+" DESC, " + Usage.COLUMN_NAME_ID + " DESC " +
				"LIMIT 1";
		Cursor c = this.db.rawQuery(sql, new String[]{ Long.toString(getSelectedCarId()) });
		
		// Return 0 if no entry in DB
		if(!c.moveToFirst()) {
			return 0;
		}

		double ret = c.getFloat(0);
		c.close();
		return ret;
	}
	
	/**
	 * Create cursor with entries for list view
	 * @return
	 */
	public Cursor createEntryListViewCursor() {
		String[] cols = new String[] {Usage.COLUMN_NAME_ID,Usage.COLUMN_NAME_DATE,Usage.COLUMN_NAME_KM,Usage.COLUMN_NAME_LITER,Usage.COLUMN_NAME_PRICE,Usage.COLUMN_NAME_FULL};
		return db.query(Usage.TABLE_NAME, cols, Usage.COLUMN_NAME_FK_CAR+"=?", new String[]{ Long.toString(getSelectedCarId()) }, null, null, Usage.COLUMN_NAME_DATE+" DESC, "+Usage.COLUMN_NAME_ID+" DESC");
	}
	
	/**
	 * Create cursor with cars for list view
	 * @return
	 */
	public Cursor createCarListViewCursor() {
		String[] cols = new String[] {Car.COLUMN_NAME_ID,Car.COLUMN_NAME_NAME,Car.COLUMN_NAME_MILEAGE};
		return db.query(Car.TABLE_NAME, cols, null, null, null, null, Car.COLUMN_NAME_ID);
	}
	
	/**
	 * Delete entry with specified id
	 * @param id
	 */
	public void deleteEntry(long id) {
		String[] whereArgs = { Long.toString(id) };
		db.delete(Usage.TABLE_NAME, Usage.COLUMN_NAME_ID+"=?", whereArgs);
	}
	
	/**
	 * Create a new car
	 * @param name
	 */
	public void addCar(String name) {
		ContentValues data = new ContentValues();
		data.put(Car.COLUMN_NAME_NAME,name);
        data.put(Car.COLUMN_NAME_MILEAGE,"0");
		db.insert(Car.TABLE_NAME, null, data);
	}
	
	/**
	 * Get car name
	 * @param id
	 * @return
	 */
	public String getCarName(long id) {
		Cursor c = db.query(Car.TABLE_NAME, new String[]{Car.COLUMN_NAME_NAME}, Car.COLUMN_NAME_ID+"=?", new String[]{Long.toString(id)}, null, null, null);
		
		//Return empty string if car doesn't exist
		if(!c.moveToFirst()) {
			return "";
		}

		String ret = c.getString(0);
		c.close();
		return ret;
	}

    /**
     * Get car mileage
     * @param id
     * @return
     */
    public double getCarMileage(long id) {
        Cursor c = db.query(Car.TABLE_NAME, new String[]{Car.COLUMN_NAME_MILEAGE}, Car.COLUMN_NAME_ID+"=?", new String[]{Long.toString(id)}, null, null, null);

        //Return 0 if car doesn't exist
        if(!c.moveToFirst()) {
            return 0;
        }

        double ret = c.getDouble(0);
        c.close();
        return ret;
    }
	
	/**
	 * Deletes selected car and all connected entries. 
	 * Car with ID 0 cannot be deleted
	 * @param id
	 */
	public void deleteCar(long id) {
		//Car with ID 0 cannot be deleted
		if(id==ID_STANDARD_CAR) return;
		
		String[] whereArgs = { Long.toString(id) };
		
		//delete entries for car
		db.delete(Usage.TABLE_NAME, Usage.COLUMN_NAME_FK_CAR+"=?", whereArgs);
		
		//delete car
		db.delete(Car.TABLE_NAME, Car.COLUMN_NAME_ID+"=?", whereArgs);
	}
	
	/**
	 * Renames car
	 * @param id
	 * @param newName
	 */
	public void renameCar(long id, String newName) {
		ContentValues data = new ContentValues();
		data.put(Car.COLUMN_NAME_NAME,newName);
		
		String[] whereArgs = {Long.toString(id)};
		
		db.update(Car.TABLE_NAME, data, Car.COLUMN_NAME_ID+"=?", whereArgs);
	}

    /**
     * Sets the current mileage for the car
     * @param id
     * @param mileage
     */
    public void setCarMileage(long id, String mileage) {
        ContentValues data = new ContentValues();
        data.put(Car.COLUMN_NAME_MILEAGE,mileage);

        String[] whereArgs = {Long.toString(id)};

        db.update(Car.TABLE_NAME, data, Car.COLUMN_NAME_ID+"=?", whereArgs);
    }
	
	/**
	 * Cheks if car exists
	 * @param id
	 * @return
	 */
	public boolean carExists(long id) {
		String[] selectionArgs = new String[]{Long.toString(id)};
		
		Cursor c = db.query(Car.TABLE_NAME, new String[]{Car.COLUMN_NAME_ID}, Car.COLUMN_NAME_ID+"=?", selectionArgs, null, null, null);
		boolean ret = c.getCount()>0;
		
		c.close();
		return ret;
	}
	
	/**
	 * Return car with lowest ID, should always be 0
	 * @return
	 */
	public long getFirstCarId() {
		Cursor c = db.query(Car.TABLE_NAME, new String[]{Car.COLUMN_NAME_ID}, null, null, null, null, Car.COLUMN_NAME_ID, "1");
		
		if(!c.moveToFirst()) {
			//Shouldn't exist...
			Log.e(TAG, "Es existiert kein Auto in der DB.");
		}

		long ret = c.getLong(0);
		c.close();
		return ret;
	}
	
	/**
	 * Get selected car from SharedPrefs
	 * @return
	 */
	private long getSelectedCarId() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getLong(context.getString(R.string.prefSelectedCar_key), ID_STANDARD_CAR);
	}

	public synchronized void close() {
		if(this.db != null) {
			db.close();
			db=null;
		}
		
		super.close();
	}
}
