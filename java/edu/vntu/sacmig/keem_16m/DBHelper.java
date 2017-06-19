package edu.vntu.sacmig.keem_16m;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    public DBHelper(Context context) {
        super(context, "myDB", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users ("
                + "id integer primary key autoincrement,"
                + "name text,"
                + "weight real,"
                + "lostEnergy real" + ");");

        db.execSQL("CREATE TABLE tracks ("
                + "id integer primary key autoincrement,"
                + "name text,"
                + "date text,"
                + "points text,"
                + "length real,"
                + "userName text,"
                + "lostEnergy real" + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
