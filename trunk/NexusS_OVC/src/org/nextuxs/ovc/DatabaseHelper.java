package org.nextuxs.ovc;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * @author NexTuxS
 */
public class DatabaseHelper extends SQLiteOpenHelper {

  public static String log_tag = "OVC";
  public static SQLiteDatabase DB = null;
  public static final String DATABASE_NAME = "ovc.db";
  public static final int DATABASE_VERSION = 1;
  public static final String KEYZ_TABLE_NAME = "keyz";
  public static final String KEYZ_CARD_ID = "card_id";
  public static final String KEYZ_SECTOR = "sector";
  public static final String KEYZ_AB = "ab";
  public static final String KEYZ_THE_KEY = "the_key";

  /**
   * @param context
   */
  public DatabaseHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  private void open() throws SQLException {
    if (DB == null || !DB.isOpen()) {
      DB = getWritableDatabase();
    }
  }

 public void makeTables() {
    open();
    String ifNotExists = " IF NOT EXISTS ";
    Log.d(log_tag, "Before Query");
    String sql = "CREATE TABLE " + ifNotExists + KEYZ_TABLE_NAME + " ("
            + KEYZ_CARD_ID + " TEXT, "
            + KEYZ_SECTOR + " TEXT, "
            + KEYZ_AB + " TEXT, "
            + KEYZ_THE_KEY + " TEXT "
            + ");";
    DB.execSQL(sql);
    close();
  }

  @Override
  public void onCreate(SQLiteDatabase sqld) {
  }

  @Override
  public void onUpgrade(SQLiteDatabase sqld, int i, int i1) {
  }

  public void insertKey(String card_id, int sector, String AB, String the_key) {
    open();
    String sql = "INSERT INTO " + KEYZ_TABLE_NAME + " VALUES ('"
            + card_id + "', '"
            + sector + "', '"
            + AB + "', '"
            + the_key + "');";
    DB.execSQL(sql);
    close();
  }

  public String getKey(String card_id, int sector, String AB) throws RuntimeException {
    open();
    String sql = "SELECT * FROM "
            + KEYZ_TABLE_NAME + " WHERE "
            + KEYZ_CARD_ID + " = '" + card_id + "' AND "
            + KEYZ_SECTOR + " = '" + sector + "' AND "
            + KEYZ_AB + " = '" + AB + "'";
    Cursor c = DB.rawQuery(sql, null);
    //Log.i("NFC", sql + " --> rows: " + c.getCount());

    if (c.getCount() == 0) {
      Log.e(log_tag, "Geen KEY !");
      Log.e(log_tag, "SQL: " + sql);
      throw new NoKeyInDBRuntimeException("db.getKey(" + card_id + "," + sector + "," + AB + "): NO KEY in Database!");
    }
    c.moveToFirst();
    String s = c.getString(3);
    c.close();
    close();
    return s;
  }

  public void clearDB() {
    open();
    DB.execSQL("DELETE FROM " + KEYZ_TABLE_NAME);
    close();
  }
}
