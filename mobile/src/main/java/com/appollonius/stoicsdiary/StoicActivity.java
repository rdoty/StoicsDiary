package com.appollonius.stoicsdiary;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;


public class StoicActivity extends AppCompatActivity implements ChoiceFragment.OnFragmentInteractionListener {

    private StoicDatabase db;
    static final String TABLE_BASE = "diary";
    static final String TABLE_DESC = "feels";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new StoicDatabase(this);
        // rebuildDatabase();  // or truncateTables();
        setContentView(R.layout.activity_stoic);

        FragmentManager fm = getFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_choice);
        if (fragment == null) {
            fragment = new ChoiceFragment();

            FragmentTransaction ft = fm.beginTransaction();
            ft.add(R.id.fragment_choice, fragment, "fragment_choice");
            ft.commit();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);  // TODO Auto-generated method stub
    }

    @Override
    public void onFragmentInteraction(Uri uri){
        //you can leave it empty
    }

    /*
     * UTILITY METHODS BELOW
     * Mostly shared code called from fragments
     */

    /**
     * Modified from stackoverflow
     * @param v View
     * @return ArrayList of children
     */
    public ArrayList<View> getAllChildren(View v) {
        if (!(v instanceof ViewGroup)) {
            ArrayList<View> viewArrayList = new ArrayList<>();
            viewArrayList.add(v);
            return viewArrayList;
        }

        ViewGroup vg = (ViewGroup) v;
        ArrayList<View> result = new ArrayList<>();

        for (int i = 0; i < vg.getChildCount(); i++) {
            ArrayList<View> viewArrayList = new ArrayList<>();

            View child = vg.getChildAt(i);
            viewArrayList.add(v);
            viewArrayList.addAll(getAllChildren(child));

            result.addAll(viewArrayList);
        }
        return result;
    }

    /*
     * BEGIN Database accessors
     */

    /**
     * For convenience
     * @param date Long
     * @return Boolean true, false or NULL
     */
    Boolean getDayValue(Long date) {
        String Q_SELECTVALUE = "SELECT value from diary WHERE timestamp=%s;";
        SQLiteDatabase dbr = db.getReadableDatabase();

        Cursor cursor = dbr.rawQuery(String.format(Q_SELECTVALUE, date), null);
        Boolean retVal = cursor.moveToFirst() ? 1 == cursor.getInt(0) : null;

        cursor.close();
        return retVal;
    }

    /**
     * TODO Find an elegant upsert behavior, sqlite has 'INSERT OR REPLACE'
     * @param date Long from LocalDate
     * @param newValue Boolean value to assign
     * @return Boolean whether the set was a success
     */
    Boolean setDayValue(Long date, Boolean newValue) {
        String logString = "Setting long date %s to value %s, was %s";
        SQLiteDatabase dbw = db.getWritableDatabase();
        ContentValues dbValue = new ContentValues();
        dbValue.put("value", newValue);
        Boolean originalValue = getDayValue(date);
        if (originalValue != null) {  // Update
            dbw.update(StoicActivity.TABLE_BASE, dbValue, "timestamp=" + date, null);
        } else {  // Insert
            dbValue.put("timestamp", date);
            dbw.insert(StoicActivity.TABLE_BASE, null, dbValue);
        }
        Log.d("DateSet", String.format(logString, date, newValue, originalValue));
        dbw.close();
        return newValue;
    }

    /**
     * TODO: Earliest recorded date in DB
     * @return Long Date
     */
    long getEarliestEntryDate() {
        Long retVal;
        SQLiteDatabase dbr = db.getReadableDatabase();
        Cursor c = dbr.query(StoicActivity.TABLE_BASE, new String[] { "min(timestamp)" },
                null, null,null, null, null);
        c.moveToFirst();
        retVal = c.getLong(0);
        c.close();
        return retVal * 86400;  // 24 * 60 * 60
    }

    /**
     * This clears out the DB
     */
    void truncateTables() {
        SQLiteDatabase dbw = db.getWritableDatabase();
        String Q_TABLETRUNC = "DELETE FROM %s; DELETE FROM SQLITE_SEQUENCE WHERE name='%s';";
        dbw.execSQL(String.format(Q_TABLETRUNC, TABLE_DESC, TABLE_DESC));
        dbw.execSQL(String.format(Q_TABLETRUNC, TABLE_BASE, TABLE_BASE));
    }

    /**
     *
     */
    void rebuildDatabase() {
        SQLiteDatabase dbw = db.getWritableDatabase();
        String Q_TABLE_DROP = "DROP TABLE IF EXISTS %s;";
        String Q_TABLE_MAKE = "CREATE TABLE %s (%s);";
        String COLUMNS_BASE = "id INTEGER PRIMARY KEY, time_stamp DATE UNIQUE, last_updated DATE, update_count TINYINT, value BOOLEAN";
        String COLUMNS_DESC = "id INTEGER PRIMARY KEY, diary_id INTEGER, words VARCHAR(255), FOREIGN KEY (diary_id) REFERENCES %s(id)";

        dbw.execSQL(String.format(Q_TABLE_DROP, TABLE_BASE));
        dbw.execSQL(String.format(Q_TABLE_MAKE, TABLE_BASE, COLUMNS_BASE));
        dbw.execSQL(String.format(Q_TABLE_DROP, TABLE_DESC));
        dbw.execSQL(String.format(String.format(Q_TABLE_MAKE, TABLE_DESC, COLUMNS_DESC), TABLE_BASE));
    }

    /*
     * END Database accessors
     */

    /*
    private void dropAllUserTables(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        //noinspection TryFinallyCanBeTryWithResources not available with API < 19
        try {
            List<String> tables = new ArrayList<>(cursor.getCount());

            while (cursor.moveToNext()) {
                tables.add(cursor.getString(0));
            }

            for (String table : tables) {
                if (table.startsWith("sqlite_")) {
                    continue;
                }
                db.execSQL("DROP TABLE IF EXISTS " + table);
                Log.v(LOG_TAG, "Dropped table " + table);
            }
        } finally {
            cursor.close();
        }
    }
    */
}
