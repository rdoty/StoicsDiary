package com.appollonius.stoicsdiary;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;


public class StoicActivity extends AppCompatActivity implements ChoiceFragment.OnFragmentInteractionListener {

    private StoicDatabase db;
    public static final String TABLE_BASE = "diary";
    public static final String TABLE_DESC = "feels";

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

    /**
     * This clears out the DB
     */
    private void truncateTables() {
        SQLiteDatabase dbw = db.getWritableDatabase();
        String Q_TRUNCTABLE = "DELETE FROM %s; DELETE FROM SQLITE_SEQUENCE WHERE name='%s';";
        dbw.execSQL(String.format(Q_TRUNCTABLE, TABLE_DESC, TABLE_DESC));
        dbw.execSQL(String.format(Q_TRUNCTABLE, TABLE_BASE, TABLE_BASE));
    }

    /**
     *
     */
    private void rebuildDatabase() {
        SQLiteDatabase dbw = db.getWritableDatabase();
        String Q_DROPTABLE = "DROP TABLE IF EXISTS %s;";
        String Q_CREATETABLE = "CREATE TABLE %s (%s);";
        String COLUMNS_BASE = "id integer PRIMARY KEY, timestamp date UNIQUE, last_updated date, value boolean";
        String COLUMNS_DESC = "id integer PRIMARY KEY, diary_id integer, words text, FOREIGN KEY (diary_id) REFERENCES %s(id)";

        dbw.execSQL(String.format(Q_DROPTABLE, TABLE_BASE));
        dbw.execSQL(String.format(Q_CREATETABLE, TABLE_BASE, COLUMNS_BASE));
        dbw.execSQL(String.format(Q_DROPTABLE, TABLE_DESC));
        dbw.execSQL(String.format(String.format(Q_CREATETABLE, TABLE_DESC, COLUMNS_DESC), TABLE_BASE));
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
