package com.appollonius.stoicsdiary;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.res.Resources;
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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;


public class StoicActivity extends AppCompatActivity implements ChoiceFragment.OnFragmentInteractionListener {

    static final String TABLE_BASE = "diary";
    static final String TABLE_DESC = "feels";
    static final String COLUMN_DAY = "time_stamp";
    static final String COLUMN_CHOICE = "choice";
    static final String COLUMN_UPDATE_DATE = "last_updated";
    static final String COLUMN_UPDATE_COUNT = "update_count";
    static final String COLUMN_DESC_F_KEY = "diary_id";
    static final String COLUMN_WORDS = "words";
    static final Integer MAX_CHANGES = 3;
    static final Integer NUM_COLOR_THEMES = 3;  // This along with the strings should live somewhere else
    static final Integer NUM_TEXT_THEMES = 3;  // This along with the strings should live somewhere else

    private StoicDatabase db;
    SharedPreferences sp;
    ThemeColors themeColors;
    ThemeText themeText;
    Typeface font;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new StoicDatabase(this);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        themeColors = new ThemeColors();
        themeText = new ThemeText();
        font = Typeface.createFromAsset(getAssets(), "font-awesome-5-free-regular-400.otf");

        if (!sp.getBoolean("resetDatabaseOnStart", false)) {  // '!' to always reset
            rebuildDatabase();  // or truncateTables();
        }
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
    void setColorTheme(int id) { themeColors = new ThemeColors(id); }
    void setTextTheme(int id) { themeText = new ThemeText(id); }
    */
    void setNextColorTheme() { themeColors = new ThemeColors((themeColors.id % (NUM_COLOR_THEMES)) + 1); }
    void setNextTextTheme() { themeText = new ThemeText((themeText.id % (NUM_TEXT_THEMES)) + 1); }

    /*
     * BEGIN Database accessors
     */

    /**
     * For accessing in the UI - change this to return the choice and whether it can be changed
     * @param date Long
     * @return Boolean true, false or NULL
     */
    ContentValues getChoice(Long date) {
        ContentValues dayValues = readDayValues(date);
        ContentValues dayChoice = new ContentValues();
        if (dayValues.size() > 0) {  // Update
            dayChoice.put("isSet", true);
            dayChoice.put("isMutable", dayValues.getAsInteger(COLUMN_UPDATE_COUNT) < MAX_CHANGES);
            dayChoice.put(COLUMN_UPDATE_COUNT, dayValues.getAsInteger(COLUMN_UPDATE_COUNT));
            dayChoice.put(COLUMN_CHOICE, dayValues.getAsBoolean(COLUMN_CHOICE));
            dayChoice.put(COLUMN_WORDS, dayValues.getAsString(COLUMN_WORDS));
        } else {
            dayChoice.put("isSet", false);
        }
        dayChoice.put("choiceDate", date);
        return dayChoice;
    }

    /**
     * For checking the current value and whether / when the value was and can be changed
     * @param date Long
     * @return ContentValues database key/values corresponding to the date value in COLUMN_DAY
     */
    ContentValues readDayValues(Long date) {
        String[] SELECT_COLS = new String[] {TABLE_BASE+".id", COLUMN_CHOICE, COLUMN_UPDATE_DATE, COLUMN_UPDATE_COUNT, COLUMN_WORDS};
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
     * @param theChoice Boolean value to assign
     * @return Boolean whether the set was a success
     */
    Boolean writeDayValue(Long date, Boolean theChoice) {
        SQLiteDatabase dbw = db.getWritableDatabase();
        ContentValues oldValues = readDayValues(date);
        ContentValues newValues = new ContentValues();
        String LOG_STRING = "Setting long date %s to value %s, was %s";
        Boolean didWriteSucceed = false;

        newValues.put(COLUMN_CHOICE, theChoice);
        newValues.put(COLUMN_UPDATE_DATE, Util.getLongVal(LocalDateTime.now()));

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
        Log.d("DateSet", String.format(LOG_STRING, date, theChoice, oldValues.get(COLUMN_CHOICE)));
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
     *
     * @return Long Date of the first entry in the database
     */
    Long getEarliestEntryDate() {
        Long earliestDate = Util.getLongVal(2018, 1, 2); // Hardcode for testing
        SQLiteDatabase dbr = db.getReadableDatabase();
        Cursor c = dbr.query(StoicActivity.TABLE_BASE, new String[] { String.format("min(%s)", COLUMN_DAY) },
                null, null,null, null, null);
        c.moveToFirst();
        //earliestDate = c.getLong(0);  // This should be from DB
        c.close();
        return earliestDate;
    }

    /**
     * This does what it says
     */
    private void rebuildDatabase() {
        SQLiteDatabase dbw = db.getWritableDatabase();
        String Q_TABLE_DROP = "DROP TABLE IF EXISTS %s;";
        String Q_TABLE_MAKE = "CREATE TABLE %s (%s);";
        String COLUMNS_BASE = String.format(
                "id INTEGER PRIMARY KEY, %s DATE UNIQUE, %s DATE, %s TINYINT, %s BOOLEAN",
                COLUMN_DAY, COLUMN_UPDATE_DATE, COLUMN_UPDATE_COUNT, COLUMN_CHOICE);
        String COLUMNS_DESC = String.format(
                "id INTEGER PRIMARY KEY, %s INTEGER, %s VARCHAR(255), FOREIGN KEY (%s) REFERENCES %s(id)",
                COLUMN_DESC_F_KEY, COLUMN_WORDS, COLUMN_DESC_F_KEY, TABLE_BASE);

        dbw.execSQL(String.format(Q_TABLE_DROP, TABLE_BASE));
        dbw.execSQL(String.format(Q_TABLE_MAKE, TABLE_BASE, COLUMNS_BASE));
        dbw.execSQL(String.format(Q_TABLE_DROP, TABLE_DESC));
        dbw.execSQL(String.format(Q_TABLE_MAKE, TABLE_DESC, COLUMNS_DESC));
    }

    /* Other ways of clearing the data in the DB
    private void truncateTables() {
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

    /**
     * Class container for all customizable UI element colors
     */
    class ThemeColors {
        final Integer id;
        final String name;
        final Integer choiceColorGoodBg;
        final Integer choiceColorGoodFg;
        final Integer choiceColorBadBg;
        final Integer choiceColorBadFg;
        final Integer appColorBg;

        /**
         * Base constructor should get the themeId preference and loads the values associated
         */
        ThemeColors() {
            this(sp.getInt("colorThemeId", 1));
        }

        /**
         * This constructor will load the preferences for the corresponding themeId
         * Note: using temp vars to deal with final vars and possible exceptions
         * @param themeId Integer the themeColors in the resources to load
         */
        ThemeColors(Integer themeId) {
            id = themeId;  // Need to do validation on the number
            Integer cCGB, cCGF, cCBB, cCBF, aCB;
            String n = null;
            cCGB = cCGF = cCBB = cCBF = aCB = null;

            try {
                n = getText(getResources().getIdentifier(sfmt("theme_%02d_name"),"string", BuildConfig.APPLICATION_ID)).toString();
                cCGB = getResources().getIdentifier(sfmt("theme_%02d_bg_good"),"color", BuildConfig.APPLICATION_ID);
                cCGF = getResources().getIdentifier(sfmt("theme_%02d_fg_good"),"color", BuildConfig.APPLICATION_ID);
                cCBB = getResources().getIdentifier(sfmt("theme_%02d_bg_bad"),"color", BuildConfig.APPLICATION_ID);
                cCBF = getResources().getIdentifier(sfmt("theme_%02d_fg_bad"),"color", BuildConfig.APPLICATION_ID);
                aCB = getResources().getIdentifier(sfmt("theme_%02d_bg_bad"),"color", BuildConfig.APPLICATION_ID);
            } catch (Resources.NotFoundException e) {
                Log.d("Exception", e.getMessage());  // Report to user?
            } finally {
                name = n;
                choiceColorGoodBg = cCGB;
                choiceColorGoodFg = cCGF;
                choiceColorBadBg = cCBB;
                choiceColorBadFg = cCBF;
                appColorBg = aCB;
            }
        }

        /**
         * This assumes id has already been set by the constructor
         * @param stringFormat String to be modified
         * @return String to use in getIdentifier
         */
        private String sfmt(String stringFormat) {
            return String.format(Locale.US, stringFormat, this.id);
        }
    }

    /**
     * Class container for all customizable UI element text
     */
    class ThemeText {
        final Integer id;
        final String name;
        final String prompt;
        final String choiceTextGood;
        final String choiceTextBad;
        final String choiceTextDisabledSelected;
        final String choiceTextDisabledUnselected;

        /**
         * Base constructor should get the themeId preference and loads the values associated
         */
        ThemeText() {
            this(sp.getInt("textThemeId", new Random().nextInt(NUM_TEXT_THEMES) + 1));
        }

        /**
         * This constructor will load the preferences for the corresponding themeId
         * Note: using temp vars to deal with final vars and possible exceptions
         * @param themeId Integer the themeColors in the resources to load
         */
        ThemeText(Integer themeId) {
            id = themeId;  // Need to do validation on the number
            String n, p, cTG, cTB, cTDS, cTDU;
            n = p = cTG = cTB = cTDS = cTDU = null;

            try {
                n = getText(getResources().getIdentifier(sfmt("theme_%02d_name"),"string", BuildConfig.APPLICATION_ID)).toString();
                p = getText(getResources().getIdentifier(sfmt("theme_%02d_prompt"),"string", BuildConfig.APPLICATION_ID)).toString();
                cTG = getText(getResources().getIdentifier(sfmt("theme_%02d_good"),"string", BuildConfig.APPLICATION_ID)).toString();
                cTB = getText(getResources().getIdentifier(sfmt("theme_%02d_bad"),"string", BuildConfig.APPLICATION_ID)).toString();
                cTDS = getText(getResources().getIdentifier(sfmt("theme_%02d_choice_disabled_selected"),"string", BuildConfig.APPLICATION_ID)).toString();
                cTDU = getText(getResources().getIdentifier(sfmt("theme_%02d_choice_disabled_unselected"),"string", BuildConfig.APPLICATION_ID)).toString();
            } catch (Resources.NotFoundException e) {
                Log.d("Exception", e.getMessage());  // Report to user?
            } finally {
                name = n;
                prompt = p;
                choiceTextGood = cTG;
                choiceTextBad = cTB;
                choiceTextDisabledSelected = cTDS;
                choiceTextDisabledUnselected = cTDU;
            }
        }

        /**
         * This assumes id has already been set by the constructor
         * @param stringFormat String to be modified
         * @return String to use in getIdentifier
         */
        private String sfmt(String stringFormat) {
            return String.format(Locale.US, stringFormat, this.id);
        }
    }

    /**
     * UTILITY METHODS BELOW
     * Mostly shared code called from fragments
     */
    static class Util {
        Util() {

        }

        /**
         * Modified from stackoverflow
         * @param v View
         * @return ArrayList of children
         */
        static ArrayList<View> getAllChildren(View v) {
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

        /**
         *
         * @param year int
         * @param month int 1-12
         * @param dayOfMonth int 1-31
         * @return Long the date munged appropriately
         */
        static Long getLongVal(int year, int month, int dayOfMonth) {
            return getLongVal(LocalDateTime.of(year, month, dayOfMonth, 0, 0));
        }
        /**
         * Use this to get the value we set the calendar to and the one we store in the DB
         * @param ldt LocalDateTime an object we're using to get the expected value
         * @return Long the date munged appropriately
         */
        static Long getLongVal(LocalDateTime ldt) {
            return ldt.atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli();
        }
    }
}
