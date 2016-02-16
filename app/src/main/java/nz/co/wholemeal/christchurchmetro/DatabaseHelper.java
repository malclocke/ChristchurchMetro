/**
 * Copyright 2010 Malcolm Locke
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package nz.co.wholemeal.christchurchmetro;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

  public static String TAG = "DatabaseHelper";

  public static final int DATABASE_VERSION = 3;
  private static String DATABASE_NAME = "metroinfo.sqlite3";
  private static String CREATE_PLATFORMS = " CREATE TABLE platforms " +
    "(platform_tag INT, platform_number INT, name VARCHAR, road_name VARCHAR," +
    "latitude DOUBLE, longitude DOUBLE)";
  private static String CREATE_PATTERNS = "CREATE TABLE patterns " +
    "(route_number varchar, route_name varchar, destination varchar, " +
    "route_tag varchar, pattern_name varchar, direction varchar, " +
    "length integer, active boolean, color varchar, coordinates text)";
  private static String CREATE_PATTERNS_PLATFORMS = "CREATE TABLE patterns_platforms " +
    "(route_tag varchar, platform_tag varchar, " +
    "schedule_adherance_timepoint boolean)";
  private static String ADD_COLOR_TO_PATTERNS = "ALTER TABLE patterns ADD COLUMN color varchar";
  private static String ADD_COORDINATES_TO_PATTERNS = "ALTER TABLE patterns ADD COLUMN coordinates text";

  public DatabaseHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    try {
      db.execSQL(CREATE_PLATFORMS);
      db.execSQL(CREATE_PATTERNS);
      db.execSQL(CREATE_PATTERNS_PLATFORMS);
      Log.i(TAG, "Database load complete");
    } catch (SQLiteException e) {
      Log.e(TAG, "Error parsing SQL: " + e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion == 1) {
      try {
        db.execSQL(CREATE_PATTERNS);
        db.execSQL(CREATE_PATTERNS_PLATFORMS);
        Log.i(TAG, "Loaded routes tables");
      } catch (SQLiteException e) {
        Log.e(TAG, "Error parsing SQL: " + e.getMessage(), e);
        throw new RuntimeException(e);
      }
    }

    if (oldVersion == 2) {
      try {
        db.execSQL(ADD_COLOR_TO_PATTERNS);
        db.execSQL(ADD_COORDINATES_TO_PATTERNS);
        Log.i(TAG, "Added mid/mif to patterns");
      } catch (SQLiteException e) {
        Log.e(TAG, "Error parsing SQL: " + e.getMessage(), e);
        throw new RuntimeException(e);
      }
    }
  }
}
