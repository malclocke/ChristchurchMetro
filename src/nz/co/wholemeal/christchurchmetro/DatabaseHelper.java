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
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.InputStream;
import java.io.IOException;

public class DatabaseHelper extends SQLiteOpenHelper {

  public static String TAG = "DatabaseHelper";

  public static final int DATABASE_VERSION = 2;
  private static String DATABASE_NAME = "metroinfo.sqlite3";
  private static String CREATE_PLATFORMS = " CREATE TABLE platforms " +
    "(platform_tag INT, platform_number INT, name VARCHAR, road_name VARCHAR," +
    "latitude DOUBLE, longitude DOUBLE)";
  private static String CREATE_PATTERNS = "CREATE TABLE patterns " +
    "(route_number varchar, route_name varchar, destination varchar, " +
    "route_tag varchar, pattern_name varchar, direction varchar, " +
    "length integer, active boolean)";
  private static String CREATE_PATTERNS_PLATFORMS = "CREATE TABLE patterns_platforms " +
    "(route_tag varchar, platform_tag varchar, " +
    "schedule_adherance_timepoint boolean)";

  private Context context;

  public DatabaseHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
    this.context = context;
  }

  private String getSQLFileContent(Resources resources, int resourceId)
      throws IOException {
    InputStream inputStream = resources.openRawResource(resourceId);
    int size = inputStream.available();
    byte[] buffer = new byte[size];
    inputStream.read(buffer);
    inputStream.close();
    return new String(buffer);
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
  }

  private void loadSqlFromResource(SQLiteDatabase db, int resource)
                throws IOException, SQLiteException {
    String sql = getSQLFileContent(context.getResources(), resource);
    for (String statement : sql.split(";")) {
      db.execSQL(statement);
    }
  }
}
