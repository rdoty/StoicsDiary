package com.appollonius.stoicsdiary;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.time.LocalDate;
import java.util.ArrayList;


public class StoicActivity extends AppCompatActivity implements ChoiceFragment.OnFragmentInteractionListener {

    static final String TABLE_BASE = "diary";
    static final String TABLE_DESC = "feels";
    static final String COLUMN_DAY = "time_stamp";
    static final String COLUMN_VERDICT = "verdict";
    static final String COLUMN_UPDATE_DATE = "last_updated";
    static final String COLUMN_UPDATE_COUNT = "update_count";
    static final String COLUMN_DESC_F_KEY = "diary_id";
    static final String COLUMN_WORDS = "words";
    static final Integer MAX_CHANGES = 3;

    private StoicDatabase db;
    SharedPreferences sp;
    Typeface font;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new StoicDatabase(this);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        font = Typeface.createFromAsset(getAssets(), "font-awesome-5-free-regular-400.otf");
        //rebuildDatabase();  // or truncateTables();
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
        super.onSaveInstanceState(outState);  // Auto-generated method stub
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
        ArrayList<View> childViewList = new ArrayList<>();

        for (int i = 0; i < vg.getChildCount(); i++) {
            ArrayList<View> viewArrayList = new ArrayList<>();

            View child = vg.getChildAt(i);
            viewArrayList.add(v);
            viewArrayList.addAll(getAllChildren(child));

            childViewList.addAll(viewArrayList);
        }
        return childViewList;
    }

    /*
     * BEGIN Database accessors
     */

    /**
     * For accessing in the UI - change this to return the verdict and whether it can be changed
     * @param date Long
     * @return Boolean true, false or NULL
     */
    ContentValues getVerdict(Long date) {
        ContentValues dayValues = readDayValues(date);
        ContentValues dayVerdict = new ContentValues();
        if (dayValues.size() == 0) {  // Update
            dayVerdict.put("isSet", false);
        } else {
            dayVerdict.put("isSet", true);
            dayVerdict.put("isMutable", dayValues.getAsInteger(COLUMN_UPDATE_COUNT) < MAX_CHANGES);
            dayVerdict.put(COLUMN_UPDATE_COUNT, dayValues.getAsInteger(COLUMN_UPDATE_COUNT));
            dayVerdict.put(COLUMN_VERDICT, dayValues.getAsBoolean(COLUMN_VERDICT));
            dayVerdict.put(COLUMN_WORDS, dayValues.getAsString(COLUMN_WORDS));
        }
        return dayVerdict;
    }

    /**
     * For checking the current value and whether / when the value was and can be changed
     * @param date Long
     * @return ContentValues database key/values corresponding to the date value in COLUMN_DAY
     */
    ContentValues readDayValues(Long date) {
        String[] SELECT_COLS = new String[] {TABLE_BASE+".id", COLUMN_VERDICT, COLUMN_UPDATE_DATE, COLUMN_UPDATE_COUNT, COLUMN_WORDS};
        String Q_SELECT = String.format("SELECT %s FROM %s LEFT OUTER JOIN %s ON %s.%s=%s.%s WHERE %s=%s;",
                String.join(",", SELECT_COLS),
                TABLE_BASE,
                TABLE_DESC,
                TABLE_DESC, COLUMN_DESC_F_KEY, TABLE_BASE, "id",
                COLUMN_DAY, date);
        ContentValues dayValues = new ContentValues();

        SQLiteDatabase dbr = db.getReadableDatabase();

        Cursor cursor = dbr.rawQuery(Q_SELECT,null);
        if (cursor.moveToFirst()) {
            for (int i=0; i < cursor.getColumnCount(); i++) {
                dayValues.put(cursor.getColumnName(i), cursor.getString(i));
            }
        }
        cursor.close();
        return dayValues;
    }

    /**
     * Would like to find a more elegant upsert behavior, sqlite has 'INSERT OR REPLACE'
     * @param date Long from LocalDate
     * @param newVerdict Boolean value to assign
     * @return Boolean whether the set was a success
     */
    Boolean writeDayValue(Long date, Boolean newVerdict) {
        SQLiteDatabase dbw = db.getWritableDatabase();
        ContentValues oldValues = readDayValues(date);
        ContentValues newValues = new ContentValues();
        String LOG_STRING = "Setting long date %s to value %s, was %s";
        Boolean didWriteSucceed = false;

        newValues.put(COLUMN_VERDICT, newVerdict);
        newValues.put(COLUMN_UPDATE_DATE, LocalDate.now().toEpochDay());

        if (oldValues.size() > 0) {  // Update
            final short updates = Short.valueOf(oldValues.getAsString(COLUMN_UPDATE_COUNT));
            if (updates < MAX_CHANGES) {
                newValues.put(COLUMN_UPDATE_COUNT, updates + 1);
                dbw.update(StoicActivity.TABLE_BASE, newValues, String.format("%s=%s", COLUMN_DAY, Long.toString(date)), null);
                didWriteSucceed = true;
            } // else { // What to return when update fails due to count? }
        } else {  // Insert
            newValues.put(COLUMN_DAY, date);
            newValues.put(COLUMN_UPDATE_COUNT, 1);
            dbw.insert(StoicActivity.TABLE_BASE, null, newValues);
            didWriteSucceed = true;
        }
        Log.d("DateSet", String.format(LOG_STRING, date, newVerdict, oldValues.get(COLUMN_VERDICT)));
        dbw.close();
        return didWriteSucceed;
    }

    /**
     *
     * @param date Long from LocalDate
     * @param feels String the text to save
     * @return Boolean whether the set was a success
     */
    Boolean writeDayFeels(Long date, String feels) {
        Boolean didWriteSucceed = false;
        SQLiteDatabase dbw = db.getWritableDatabase();
        ContentValues oldValues = readDayValues(date);
        ContentValues newValues = new ContentValues();
        newValues.put(COLUMN_WORDS, feels);
        if (oldValues.size() > 0) {  // Update
            newValues.put(COLUMN_DESC_F_KEY, oldValues.getAsInteger("id"));
            if (oldValues.getAsString(COLUMN_WORDS) != null) {  // update
                didWriteSucceed = 1 == dbw.update(StoicActivity.TABLE_DESC, newValues, String.format("%s=%s", COLUMN_DESC_F_KEY, oldValues.getAsString("id")), null);
            } else {  // insert
                didWriteSucceed = -1 < dbw.insert(StoicActivity.TABLE_DESC, null, newValues);
            }
        }
        return didWriteSucceed;
    }

    /**
     * @return Long Date of the first entry in the database
     */
    long getEarliestEntryDate() {
        Long earliestDate;
        SQLiteDatabase dbr = db.getReadableDatabase();
        Cursor c = dbr.query(StoicActivity.TABLE_BASE, new String[] { String.format("min(%s)", COLUMN_DAY) },
                null, null,null, null, null);
        c.moveToFirst();
        earliestDate = c.getLong(0) * 86400;  // 24 * 60 * 60
        c.close();
        return earliestDate;
    }

    /*
     * These clear out the DB
     */
    void rebuildDatabase() {
        SQLiteDatabase dbw = db.getWritableDatabase();
        String Q_TABLE_DROP = "DROP TABLE IF EXISTS %s;";
        String Q_TABLE_MAKE = "CREATE TABLE %s (%s);";
        String COLUMNS_BASE = String.format(
                "id INTEGER PRIMARY KEY, %s DATE UNIQUE, %s DATE, %s TINYINT, %s BOOLEAN",
                COLUMN_DAY, COLUMN_UPDATE_DATE, COLUMN_UPDATE_COUNT, COLUMN_VERDICT);
        String COLUMNS_DESC = String.format(
                "id INTEGER PRIMARY KEY, %s INTEGER, %s VARCHAR(255), FOREIGN KEY (%s) REFERENCES %s(id)",
                COLUMN_DESC_F_KEY, COLUMN_WORDS, COLUMN_DESC_F_KEY, TABLE_BASE);

        dbw.execSQL(String.format(Q_TABLE_DROP, TABLE_BASE));
        dbw.execSQL(String.format(Q_TABLE_MAKE, TABLE_BASE, COLUMNS_BASE));
        dbw.execSQL(String.format(Q_TABLE_DROP, TABLE_DESC));
        dbw.execSQL(String.format(Q_TABLE_MAKE, TABLE_DESC, COLUMNS_DESC));
    }

    /*
    void truncateTables() {
        SQLiteDatabase dbw = db.getWritableDatabase();
        String Q_TABLETRUNC = "DELETE FROM %s; DELETE FROM SQLITE_SEQUENCE WHERE name='%s';";
        dbw.execSQL(String.format(Q_TABLETRUNC, TABLE_DESC, TABLE_DESC));
        dbw.execSQL(String.format(Q_TABLETRUNC, TABLE_BASE, TABLE_BASE));
    }

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

    /*
     * END Database accessors
     */
}
