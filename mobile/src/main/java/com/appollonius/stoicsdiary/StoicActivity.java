package com.appollonius.stoicsdiary;

import android.app.AlarmManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
//import android.support.v4.app.Fragment;
//import android.support.v4.app.FragmentManager;
//import android.support.v4.app.FragmentPagerAdapter;
//import android.support.v4.view.PagerAdapter;
//import android.support.v4.view.ViewPager;
//import android.support.design.widget.TabLayout;
//import android.support.v7.widget.Toolbar;
//import android.widget.Adapter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

/**
 * This is the MainActivity
 */
public class StoicActivity extends AppCompatActivity implements PageFragment.OnFragmentInteractionListener,
        ChoiceFragment.OnFragmentInteractionListener {
    // Database fields
    static final String TABLE_BASE = "diary";
    static final String TABLE_DESC = "feels";
    static final String COLUMN_DAY = "time_stamp";
    static final String COLUMN_CHOICE = "choice";
    static final String COLUMN_UPDATE_DATE = "last_updated";
    static final String COLUMN_UPDATE_COUNT = "update_count";
    static final String COLUMN_DESC_F_KEY = "diary_id";
    static final String COLUMN_WORDS = "words";
    // Fields used between activities/fragments
    static final String CHOICE_ISSET = "isSet";
    static final String CHOICE_ISMUTABLE = "isMutable";
    static final String CHOICE_DATE = "choiceDate";
    // Information for stored preferences / data / business rules
    static final String PREF_USERNAME_KEY = "userFirstName";
    static final String PREF_CUR_TEXT_THEME_KEY = "currentTextTheme";
    static final String PREF_CUR_COLOR_THEME_KEY = "currentColorTheme";
    static final Integer MAX_CHANGES = 3;
    static final Integer NUM_COLOR_THEMES = 3;  // This along with the strings should live somewhere else
    static final Integer NUM_TEXT_THEMES = 3;  // This along with the strings should live somewhere else
    static final Integer NUM_QUOTES = 6;  // Figure count out dynamically
    static final String EXPORT_FILENAME = "sd_export.csv";

    // Major internal components
    private StoicDatabase db;
    SharedPreferences sp;
    ThemeColors themeColors;
    ThemeText themeText;
    Typeface font;
    Integer mLatestNotificationId;  // Tracking this so we can reference/delete at runtime if necessary

//    TabLayout tabLayout;
//    ViewPager viewPager;
//    PagerAdapter adapter;
//
    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_pref:
                onClickPreferences();
                return true;
            case R.id.item_about:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onClickPreferences() {
        Intent myIntent = new Intent(StoicActivity.this, SettingsActivity.class);
        myIntent.putExtra("key", "value"); //Optional parameters
        StoicActivity.this.startActivity(myIntent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stoic);
        db = new StoicDatabase(this);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        themeColors = new ThemeColors();
        themeText = new ThemeText();
        font = Typeface.createFromAsset(getAssets(), "font-awesome-5-free-regular-400.otf");

        initToolbar();
//        tabLayout = findViewById(R.id.tab_layout);
//        viewPager = findViewById(R.id.view_pager);
//
//        adapter = new TabAdapter(getSupportFragmentManager());
//        viewPager.setAdapter(adapter);
//        tabLayout.setupWithViewPager(viewPager);
//
//        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
//
//            @Override
//            public void onTabSelected(TabLayout.Tab tab) {
//
//            }
//
//            @Override
//            public void onTabUnselected(TabLayout.Tab tab) {
//
//            }
//
//            @Override
//            public void onTabReselected(TabLayout.Tab tab) {
//
//            }
//        });
//
        if (sp.getBoolean("resetDatabaseOnStart", false)) {  // Should fire from a settings button instead
            rebuildDatabase();  // or truncateTables();
        }
        deleteAllTempCacheFiles();
        initializeNotificationChannel();
        initializeDailyReminder();

        FragmentManager fm = getFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_choice);
        if (fragment == null) {
            fragment = new ChoiceFragment();

            FragmentTransaction ft = fm.beginTransaction();
            ft.add(R.id.fragment_choice, fragment, getString(R.string.fragment_choice));
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

    /* Unused currently. Called when user selects in preferences? Or just call updateUI?
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
            dayChoice.put(CHOICE_ISSET, true);
            dayChoice.put(CHOICE_ISMUTABLE, dayValues.getAsInteger(COLUMN_UPDATE_COUNT) < MAX_CHANGES);
            dayChoice.put(COLUMN_UPDATE_COUNT, dayValues.getAsInteger(COLUMN_UPDATE_COUNT));
            dayChoice.put(COLUMN_CHOICE, dayValues.getAsBoolean(COLUMN_CHOICE));
            dayChoice.put(COLUMN_WORDS, dayValues.getAsString(COLUMN_WORDS));
        } else {
            dayChoice.put(CHOICE_ISSET, false);
        }
        dayChoice.put(CHOICE_DATE, date);
        return dayChoice;
    }

    /**
     * For checking the current value and whether / when the value was and can be changed
     * @param date Long
     * @return ContentValues database key/values corresponding to the date value in COLUMN_DAY
     */
    ContentValues readDayValues(Long date) {
        String[] SELECT_COLS = new String[] {TABLE_BASE+".id", COLUMN_CHOICE, COLUMN_UPDATE_DATE, COLUMN_UPDATE_COUNT, COLUMN_WORDS};
        String Q_SELECT = String.format(
                "SELECT %s FROM %s LEFT OUTER JOIN %s ON %s.%s=%s.%s WHERE %s=%s;",
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
            if (!oldValues.getAsBoolean(COLUMN_CHOICE).equals(theChoice)) {  // #154818575
                final short updates = Short.valueOf(oldValues.getAsString(COLUMN_UPDATE_COUNT));
                if (updates < MAX_CHANGES) {
                    newValues.put(COLUMN_UPDATE_COUNT, updates + 1);
                    dbw.update(StoicActivity.TABLE_BASE, newValues, String.format("%s=%s", COLUMN_DAY, Long.toString(date)), null);
                    didWriteSucceed = true;
                } // else { // What to return when update fails due to count? }
            }
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
        Long earliestDate = Util.getLongVal(2017, 12, 2); // Hardcode for testing
        SQLiteDatabase dbr = db.getReadableDatabase();
        Cursor c = dbr.query(StoicActivity.TABLE_BASE, new String[] { String.format("min(%s)", COLUMN_DAY) },
                null, null,null, null, null);
        c.moveToFirst();
        //earliestDate = c.getLong(0);  // When done testing, this should be from DB
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

    ArrayList<ContentValues> getExportData() {
        String[] SELECT_COLS = new String[] {TABLE_BASE+".id", COLUMN_DAY, COLUMN_CHOICE, COLUMN_WORDS};
        String Q_SELECT = String.format("SELECT %s FROM %s LEFT OUTER JOIN %s ON %s.%s=%s.%s ORDER BY %s;",
                String.join(",", SELECT_COLS),
                TABLE_BASE, TABLE_DESC, TABLE_DESC, COLUMN_DESC_F_KEY, TABLE_BASE, "id", COLUMN_DAY);

        SQLiteDatabase dbr = db.getReadableDatabase();

        Cursor cursor = dbr.rawQuery(Q_SELECT,null);
        ArrayList<ContentValues> retVal = new ArrayList<>();
        ContentValues map;

        while (cursor.moveToNext()) {
            map = new ContentValues();
            DatabaseUtils.cursorRowToContentValues(cursor, map);
            retVal.add(map);
        }
        cursor.close();
        return retVal;
    }
    /*
     * END Database accessors
     */

    /**
     * Right now we just have the one CSV file for exporting data, but there
     * may be more in the future. Call this at app start to clear out any cruft
     */
    void deleteAllTempCacheFiles() {
        Boolean filesWereDeleted = false;  // Don't really care about this value TBH
        // All files in the cache folder
        ArrayList<File> files = new ArrayList<>(Arrays.asList(getApplicationContext().getCacheDir().listFiles()));
        // Add any internal files we know we want to delete below
        files.add(new File(getApplicationContext().getFilesDir(), EXPORT_FILENAME));

        for (File cacheFile : files) {
            filesWereDeleted = cacheFile.exists() && cacheFile.delete();
        }
        Log.d("DEBUG", String.format("deleteAllTempCacheFiles: %s", filesWereDeleted));
    }

    /**
     * Export database fields into more friendly CSV format
     * For export, format date as yyyy-mm-dd, choice as 1 / -1, words null value as empty string
     * @return File filename, null if failure
     */
    File exportToCSVFile() {
        final String HEADER_DATE = "Date";
        final String HEADER_CHOICE = "Choice";
        final String HEADER_WORDS = "Words";

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());

        try {
            ArrayList<ContentValues> exportData = getExportData();
            File internalFile = new File(getApplicationContext().getFilesDir(), EXPORT_FILENAME);
            PrintWriter csvWriter = new PrintWriter(new FileWriter(internalFile,false));
            csvWriter.printf("%s, %s, %s\r\n", HEADER_DATE, HEADER_CHOICE, HEADER_WORDS);
            for (ContentValues data : exportData) {
                Date date = new Date(data.getAsLong(COLUMN_DAY));
                String output = String.format("%s, %s, \"%s\"\r\n",
                        dateFormat.format(date),
                        data.getAsString(COLUMN_CHOICE).equals("1") ? "1" : "-1",
                        data.getAsString(COLUMN_WORDS) == null ? "" : data.getAsString(COLUMN_WORDS));
                csvWriter.print(output);
                Log.d("EXPORT_ROW", output);
            }
            csvWriter.close();
            return internalFile;
        } catch(IOException e) {
            Log.d("ERROR", e.getMessage());
        }
        return null;
    }

    /**
     * Generates a CSV file, then opens the configured email client with the file attachment
     * @return Boolean whether the activity returned successfully. Need to try/catch to confirm tho
     */
    Boolean exportToEmail() {
        File csvFile = exportToCSVFile();
        if (csvFile != null) {
            Uri path = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", csvFile);
            Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
            emailIntent.setType("text/html");
            emailIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.export_email_subject));
            emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, getExportEmailBody());
            emailIntent.putExtra(Intent.EXTRA_STREAM, path);
            startActivity(Intent.createChooser(emailIntent, getText(R.string.export_email_intent)));
            return true;  // Figure out when it's safe (and simplest way) to clear out the cache
        }
        return false;
    }

    /**
     * Just returns the content for the body assembled from a number of resources
     * @return String email body HTML content
     */
    String getExportEmailBody() {
        final String FORMAT_BODY = "<p>%s %s,</p><br/>" +
                "<p>%s</p><br/>" +
                "<p>%s</p><br/>" +
                "<p>%s</p><br/>" +
                "<p>-- </p><br/>" +
                "<p>%s</p><br/>"
                ;

        return Html.fromHtml(String.format(FORMAT_BODY,
                getString(R.string.export_email_salutation),
                sp.getString(PREF_USERNAME_KEY, getString(R.string.export_email_name_unknown)),
                Html.fromHtml(getString(R.string.export_email_body), Html.FROM_HTML_MODE_COMPACT).toString(),
                getString(R.string.export_email_closing),
                String.format("%s %s", getString(R.string.export_email_signature), getString(R.string.app_name)),
                ((ChoiceFragment)getFragmentManager().findFragmentById(R.id.fragment_choice)).getQuote()
                ), Html.FROM_HTML_MODE_COMPACT).toString();
    }

    void initializeNotificationChannel() {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (mNotificationManager != null) {
            String id = getString(R.string.channel_id);  // of the channel.
            CharSequence name = getString(R.string.channel_name);  // user-visible
            String description = getString(R.string.channel_description);  // user-visible
            NotificationChannel mChannel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT);

            // Configure the notification channel.
            mChannel.setDescription(description);
            mChannel.enableLights(true);
            mChannel.setLightColor(R.color.colorNotificationLight);  // if the device supports this feature.
            mChannel.enableVibration(true);
            mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            mNotificationManager.createNotificationChannel(mChannel);
        } else {
            Log.d("ERROR", "Could not initializeNotificationChannel, NOTIFICATION_SERVICE null");
        }
    }

    /**
     * This just fires off a test notification at the moment.
     */
    void notifyUser() {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (mNotificationManager != null) {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this, getString(R.string.channel_id))
                            .setSmallIcon(R.drawable.common_google_signin_btn_icon_light)
                            .setContentTitle(getText(R.string.notification_title))
                            .setContentText(getText(R.string.notification_text));
            // Creates an explicit intent for an Activity in your app
            Intent resultIntent = new Intent(this, StoicActivity.class);

            // The stack builder object will contain an artificial back stack
            // for the started Activity. This ensures that navigating backward
            // from the Activity leads out of your app to the Home screen.
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            // Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(StoicActivity.class);
            // Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(resultPendingIntent);

            // mNotificationId is a unique integer your app uses to identify the
            // notification. For example, to cancel the notification, you can pass its ID
            // number to NotificationManager.cancel().
            Random random = new Random();
            mLatestNotificationId = random.nextInt();
            mNotificationManager.notify(mLatestNotificationId, mBuilder.build());
        }
    }

    /**
     * This method checks the currently set preferences, and updates the scheduled
     * notifications accordingly.
     */
    void initializeDailyReminder() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            Intent myIntent = new Intent(StoicActivity.this, StoicActivity.class);
            PendingIntent pending = PendingIntent.getService(StoicActivity.this, 0, myIntent, 0);

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, sp.getInt("notification_hour", 21));
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 24 * 60 * 60 * 1000, pending);  //set repeating every 24 hours
        } else {
            Log.d("ERROR", "initializeDailyReminder failed to getSystemService(ALARM_SERVICE)");
        }
    }

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
            this(sp.getInt(PREF_CUR_COLOR_THEME_KEY, new Random().nextInt(NUM_COLOR_THEMES) + 1));
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
                n = getText(getResources().getIdentifier(sId("theme_%02d_name"),"string", BuildConfig.APPLICATION_ID)).toString();
                cCGB = getResources().getIdentifier(sId("theme_%02d_bg_good"),"color", BuildConfig.APPLICATION_ID);
                cCGF = getResources().getIdentifier(sId("theme_%02d_fg_good"),"color", BuildConfig.APPLICATION_ID);
                cCBB = getResources().getIdentifier(sId("theme_%02d_bg_bad"),"color", BuildConfig.APPLICATION_ID);
                cCBF = getResources().getIdentifier(sId("theme_%02d_fg_bad"),"color", BuildConfig.APPLICATION_ID);
                aCB = getResources().getIdentifier(sId("theme_%02d_bg_bad"),"color", BuildConfig.APPLICATION_ID);
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
        private String sId(String stringFormat) {
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
            this(sp.getInt(PREF_CUR_TEXT_THEME_KEY, new Random().nextInt(NUM_TEXT_THEMES) + 1));
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
         * @return Long the date munged appropriately that we'll use for storing and comparison
         */
        static Long getLongVal(int year, int month, int dayOfMonth) {
            return getLongVal(LocalDateTime.of(year, month, dayOfMonth, 0, 0));
        }
        /**
         * Use this to get the value we set the calendar to and the one we store in the DB
         * @param ldt LocalDateTime an object we're using to get the expected value
         * @return Long the date munged appropriately that we'll use for storing and comparison
         */
        static Long getLongVal(LocalDateTime ldt) {
            return ldt.atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli();
        }

        /**
         * Take the long date value we get from the CalendarView and normalize it so we're always
         * comparing/saving the same value in the database.
         * @param date Long the current date/time, which we'll chop according to the methods here
         * @return Long the date value that we'll use for storing and comparison
         */
        static Long getLongVal(Long date) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeZone(TimeZone.getDefault());
            cal.setTimeInMillis(date);
            return Util.getLongVal(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
        }
    }
}
