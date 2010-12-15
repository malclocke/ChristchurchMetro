package nz.co.wholemeal.christchurchmetro;

import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.InputStream;
import java.io.IOException;
import java.sql.SQLException;

public class DatabaseHelper extends SQLiteOpenHelper {

  public static String TAG = "DatabaseHelper";

  public static final int DATABASE_VERSION = 1;
  private static String DATABASE_NAME = "metroinfo.sqlite3";
  private static String CREATE_PLATFORMS = " CREATE TABLE platforms " +
    "(platform_tag INT, platform_number INT, name VARCHAR, road_name VARCHAR," +
    "latitude DOUBLE, longitude DOUBLE)";

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
      String sql = getSQLFileContent(context.getResources(), R.raw.database_sql);
      db.execSQL(CREATE_PLATFORMS);
      db.beginTransaction();
      try {
        for (String statement : sql.split(";")) {
          db.execSQL(statement);
        }
        db.setTransactionSuccessful();
      } finally {
        db.endTransaction();
      }
      Log.i(TAG, "Database load complete");
    } catch (IOException e) {
      Log.e(TAG, "Error reading SQL file: " + e.getMessage(), e);
      throw new RuntimeException(e);
    } catch (SQLiteException e) {
      Log.e(TAG, "Error parsing SQL: " + e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
  }
}
