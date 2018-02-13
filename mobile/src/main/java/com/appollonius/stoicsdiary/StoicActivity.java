package com.appollonius.stoicsdiary;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.FileProvider;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.design.widget.TabLayout;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * This is the MainActivity
 */
public class StoicActivity extends AppCompatActivity implements ChoiceFragment.OnFragmentInteractionListener,
        HistoryFragment.OnFragmentInteractionListener, SummaryFragment.OnFragmentInteractionListener {
    // Fields used between activities/fragments
    static final String CHOICE_ISSET = "isSet";
    static final String CHOICE_ISMUTABLE = "isMutable";
    static final String CHOICE_DATE = "choiceDate";
    // Information for stored preferences, data, or business rules
    static final String INTENT_EXTRA_EXPORT = "initiate_export";  // Same as pref_export_key
    static final String INTENT_EXTRA_RESET_DB = "reset_db";  // Save as @string/pref_reset_key

    static final String PREF_KEY_USERNAME = "displayName";  // Same as
    static final String PREF_KEY_CUR_TEXT_THEME = "currentTextTheme";
    static final String PREF_KEY_CUR_COLOR_THEME = "currentColorTheme";

    static final Integer MAX_CHANGES = 3;
    static final String EXPORT_FILENAME = "sd_export.csv";

    // Major internal components
    SharedPreferences sp;
    Datastore ds;
    ThemeColors themeColors;
    ThemeText themeText;
    Typeface font;
    Integer mLatestNotificationId;  // Tracking this so we can reference/delete at runtime if necessary
    NotificationManager mNotificationManager;

    TabLayout tabLayout;
    ViewPager viewPager;
    private Long currentDay;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("VERSION", String.format("%s, v%s", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));

        setContentView(R.layout.activity_stoic);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        ds = new Datastore();
        themeColors = new ThemeColors();
        themeText = new ThemeText();
        font = Typeface.createFromAsset(getAssets(), "font-awesome-5-free-regular-400.otf");
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        viewPager = findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        deleteAllTempCacheFiles();

        initializeCurrentDay();
        initializeStartingTab();
        initializeNotificationChannel();
        initializeDailyReminder();
    }

    private void initializeStartingTab() {
        viewPager.setCurrentItem(getChoice(getCurrentDay()).getAsBoolean(CHOICE_ISSET) ? 1 : 0);
    }

    void initializeCurrentDay() { currentDay = Util.getLongVal(LocalDateTime.now()); }
    void setCurrentDay(Long currentDay) { this.currentDay = currentDay; }
    Long getCurrentDay() { return currentDay; }
    Integer getCurrentTab() { return viewPager.getCurrentItem(); }

    private void setupViewPager(ViewPager viewPager) {
        final ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(ChoiceFragment.newInstance(""), "CHOICE");
        adapter.addFragment(HistoryFragment.newInstance(""), "HISTORY");
        adapter.addFragment(SummaryFragment.newInstance(""), "SUMMARY");
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (adapter.getItem(position).getView() != null) {
                    updateUI(adapter.getItem(position).getView());
                    if (position == 2) {  // stats, refresh stat list in adapter
                        ((SummaryFragment)(adapter.getItem(position))).updateStatList();
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<android.support.v4.app.Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        ViewPagerAdapter(android.support.v4.app.FragmentManager manager) {
            super(manager);
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        void addFragment(android.support.v4.app.Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
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
                onClickAppAbout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.getBoolean(INTENT_EXTRA_EXPORT, false)) {
                getIntent().removeExtra(INTENT_EXTRA_EXPORT);
                exportToEmail();
            }
            if (extras.getBoolean(INTENT_EXTRA_RESET_DB, false)) {
                getIntent().removeExtra(INTENT_EXTRA_RESET_DB);
                ds.rebuildDatabase();
                Log.d("DB", "Database was reset by user.");
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) { super.onSaveInstanceState(outState); }

    @Override
    public void onFragmentInteraction(Uri uri) { } // you can leave it empty

    private void onClickPreferences() {
        Intent myIntent = new Intent(StoicActivity.this, SettingsActivity.class);
        myIntent.putExtra("key", "value"); //Optional parameters
        StoicActivity.this.startActivity(myIntent);
    }

    private void onClickAppAbout() {
        final AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
        String aboutText = getString(R.string.app_about_text, BuildConfig.VERSION_NAME);
        alertbox.setMessage(Html.fromHtml(aboutText, Html.FROM_HTML_MODE_COMPACT));
        alertbox.setNeutralButton("Ok", new DialogInterface.OnClickListener() { public void onClick(DialogInterface arg0, int arg1) { } } );
        alertbox.show();
    }

    /*
     * BEGIN Database accessors
     */

    /**
     * For accessing in the UI - change this to return the choice and whether it can be changed
     * @param date Long
     * @return Boolean true, false or NULL
     */
    ContentValues getChoice(Long date) {
        ContentValues dayValues = ds.readDayValues(date);
        ContentValues dayChoice = new ContentValues();
        if (dayValues.size() > 0) {  // Update
            dayChoice.put(CHOICE_ISSET, true);
            dayChoice.put(CHOICE_ISMUTABLE, dayValues.getAsInteger(Datastore.COLUMN_UPDATE_COUNT) < MAX_CHANGES);
            dayChoice.put(Datastore.COLUMN_UPDATE_COUNT, dayValues.getAsInteger(Datastore.COLUMN_UPDATE_COUNT));
            dayChoice.put(Datastore.COLUMN_CHOICE, dayValues.getAsBoolean(Datastore.COLUMN_CHOICE));
            dayChoice.put(Datastore.COLUMN_WORDS, dayValues.getAsString(Datastore.COLUMN_WORDS));
        } else {
            dayChoice.put(CHOICE_ISSET, false);
        }
        dayChoice.put(CHOICE_DATE, date);
        return dayChoice;
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
     * For export, format date as yyyy-mm-dd, choice as 1 or -1, words null value as empty string
     * @return File filename, null if failure
     */
    File exportToCSVFile() {
        final String HEADER_DATE = "Date";
        final String HEADER_CHOICE = "Choice";
        final String HEADER_WORDS = "Words";

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());

        try {
            ArrayList<ContentValues> exportData = ds.getExportData();
            File internalFile = new File(getApplicationContext().getFilesDir(), EXPORT_FILENAME);
            PrintWriter csvWriter = new PrintWriter(new FileWriter(internalFile,false));
            csvWriter.printf("%s, %s, %s\r\n", HEADER_DATE, HEADER_CHOICE, HEADER_WORDS);
            for (ContentValues data : exportData) {
                Date date = new Date(data.getAsLong(Datastore.COLUMN_DAY));
                String output = String.format("%s, %s, \"%s\"\r\n",
                        dateFormat.format(date),
                        data.getAsString(Datastore.COLUMN_CHOICE).equals("1") ? "1" : "-1",
                        data.getAsString(Datastore.COLUMN_WORDS) == null ? "" : data.getAsString(Datastore.COLUMN_WORDS));
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
     */
    void exportToEmail() {
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
        }
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
                sp.getString(PREF_KEY_USERNAME, getString(R.string.export_email_name_unknown)),
                Html.fromHtml(getString(R.string.export_email_body), Html.FROM_HTML_MODE_COMPACT).toString(),
                getString(R.string.export_email_closing),
                String.format("%s %s", getString(R.string.export_email_signature), getString(R.string.app_name)),
                getQuote()), Html.FROM_HTML_MODE_COMPACT).toString();
    }

/*
    / **
     *
     * @return String email body HTML content
     * /
    String getFeedbackEmailBody() {
        final String FORMAT_BODY = "<p>%s %s,</p><br/>" +
                "<p>%s</p><br/>" +
                "<p>%s</p><br/>" +
                "<p>%s</p><br/>" +
                "<p>-- </p><br/>" +
                "<p>%s</p><br/>"
                ;

        return Html.fromHtml(String.format(
                FORMAT_BODY,
                getString(R.string.export_email_salutation), getString(R.string.app_name),
                "TO DO",
                "TO DO",
                "TO DO",
                "TO DO"), Html.FROM_HTML_MODE_COMPACT).toString();
    }

    / **
     *
     * @return String email body HTML content
     * /
    String getTranslationEmailBody() {
        final String FORMAT_BODY = "<p>%s %s,</p><br/>" +
                "<p>%s</p><br/>" +
                "<p>%s</p><br/>" +
                "<p>%s</p><br/>" +
                "<p>-- </p><br/>" +
                "<p>%s</p><br/>"
                ;

        return Html.fromHtml(String.format(
                FORMAT_BODY,
                getString(R.string.export_email_salutation), getString(R.string.app_name),
                "TO DO",
                "TO DO",
                "TO DO",
                "TO DO"), Html.FROM_HTML_MODE_COMPACT).toString();
    }
*/
    void initializeNotificationChannel() {
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
     * mNotificationId is a unique integer your app uses to identify the notification.
     * e.g. to cancel the notification, call NotificationManager.cancel(mLatestNotificationId).
     */
    void initializeNotifications() {
        if (mNotificationManager != null) {
            mLatestNotificationId = new Random().nextInt();
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            Intent resultIntent = new Intent(this, StoicActivity.class);
            AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
            if (alarmManager != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 12);  // From settings
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);

                stackBuilder.addParentStack(StoicActivity.class);  // Adds the back stack for the Intent
                stackBuilder.addNextIntent(resultIntent);  // Adds the Intent that starts the Activity to the top of the stack
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(this, getString(R.string.channel_id))
                                .setSmallIcon(R.drawable.common_google_signin_btn_icon_light)
                                .setContentTitle(getText(R.string.notification_title))
                                .setContentText(getText(R.string.notification_text));

                mBuilder.setContentIntent(resultPendingIntent);

                mNotificationManager.notify(mLatestNotificationId, mBuilder.build());

                PendingIntent pendingIntent = PendingIntent.getService(StoicActivity.this, 0, resultIntent, 0);

                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 24*60*60*1000 , pendingIntent);  //set repeating every 24 hours
            }
        } else {
            Log.d("ERROR", "Could not initializeNotificationChannel, NOTIFICATION_SERVICE null");
        }
    }

    void sendTweet() {
        Intent tweetIntent = new Intent(Intent.ACTION_SEND);
        tweetIntent.putExtra(Intent.EXTRA_TEXT, "This is a Test."); // Contents
        tweetIntent.setType("text/plain");

        PackageManager packManager = getPackageManager();
        List<ResolveInfo> resolvedInfoList = packManager.queryIntentActivities(tweetIntent,  PackageManager.MATCH_DEFAULT_ONLY);

        boolean resolved = false;
        for(ResolveInfo resolveInfo: resolvedInfoList){
            if(resolveInfo.activityInfo.packageName.startsWith("com.twitter.android")){
                tweetIntent.setClassName(
                        resolveInfo.activityInfo.packageName,
                        resolveInfo.activityInfo.name );
                resolved = true;
                break;
            }
        }
        if (resolved) {
            startActivity(tweetIntent);
        } else {
            Toast.makeText(this, "Twitter app isn't found", Toast.LENGTH_LONG).show();
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

    public Boolean isDebugMode() {
        return sp.getBoolean(getString(R.string.pref_debug_key), false);
    }

    /**
     * Grabs a random quote from the resource file
     * @return String the quote to display
     */
    @NonNull
    public String getQuote() {
        return getQuote(new Random().nextInt(getResources().getStringArray(R.array.quotations).length));
    }

    /**
     * Grabs a specific quote from the resource file
     * @param quoteId Integer if null, gets a random quote
     * @return String the quote to display
     */
    @NonNull
    private String getQuote(Integer quoteId) {
        String[] array = getResources().getStringArray(R.array.quotations);
        return array[quoteId];
    }

    /**
     * This includes default colors, text prompts, maybe user-provided text, too.
     * Now always reset themes to preferences on any UI update - shouldn't be costly
     */
    void updateUI(View v) {
        themeColors = new ThemeColors();
        themeText = new ThemeText();

        RadioGroup radioGroupChoices = v.findViewById(R.id.GROUP_CHOICES);
        RadioButton yB = v.findViewById(R.id.BUTTON_YES);
        RadioButton nB = v.findViewById(R.id.BUTTON_NO);

        ContentValues selectedDayValues = getChoice(getCurrentDay());
        Boolean isChoiceSet = selectedDayValues.getAsBoolean(StoicActivity.CHOICE_ISSET);
        Boolean isChoiceEnabled = true;
        if (radioGroupChoices != null) {
            radioGroupChoices.clearCheck();  // Clear previous selection in case choice not set
            if (isChoiceSet) {  // Check the proper choice, also confirm whether we can change it
                isChoiceEnabled = selectedDayValues.getAsBoolean(StoicActivity.CHOICE_ISMUTABLE);
                radioGroupChoices.check(selectedDayValues.getAsBoolean(Datastore.COLUMN_CHOICE) ? R.id.BUTTON_YES : R.id.BUTTON_NO);
            } else {  // Prompt if today is selected but no choice has been made
                if (Util.getLongVal(LocalDateTime.now()).equals(getCurrentDay())) {
                    Toast.makeText(this, themeText.prompt, Toast.LENGTH_LONG).show();
                }
            }
            for (View child: Util.getAllChildren(radioGroupChoices)) {
                child.setEnabled(isChoiceEnabled);
            }
            yB.setText(
                    (isChoiceEnabled
                            ? themeText.choiceTextGood
                            : yB.isChecked()
                            ? themeText.choiceTextDisabledSelected
                            : themeText.choiceTextDisabledUnselected));
            yB.setVisibility((!isChoiceEnabled && !yB.isChecked()) ? View.GONE : View.VISIBLE);
            yB.setTextColor(themeColors.choiceColorGoodFg);
            yB.getBackground().setColorFilter(themeColors.choiceColorGoodBg, PorterDuff.Mode.SRC_ATOP);
            yB.getBackground().setTint(themeColors.choiceColorGoodBg);

            nB.setText(
                    (isChoiceEnabled
                            ? themeText.choiceTextBad
                            : nB.isChecked()
                            ? themeText.choiceTextDisabledSelected
                            : themeText.choiceTextDisabledUnselected));
            nB.setVisibility((!isChoiceEnabled && !nB.isChecked()) ? View.GONE : View.VISIBLE);
            nB.setTextColor(themeColors.choiceColorBadFg);
            nB.getBackground().setColorFilter(themeColors.choiceColorBadBg, PorterDuff.Mode.SRC_ATOP);
            nB.getBackground().setTint(themeColors.choiceColorBadBg);
            if (!isChoiceEnabled && getCurrentTab() == 0) {  // Only show this on choice tab
                Toast.makeText(this, getString(R.string.choice_buttons_disabled), Toast.LENGTH_SHORT).show();
            }
        }
        // Set the other controls to their proper state
        EditText editTextFeels = v.findViewById(R.id.EDIT_FEELS);
        if (editTextFeels != null) {
            editTextFeels.setHint(getText(isChoiceSet ? R.string.feels_prompt_enabled : R.string.feels_prompt_disabled));
            editTextFeels.setEnabled(isChoiceSet);
            editTextFeels.setText(selectedDayValues.getAsString(Datastore.COLUMN_WORDS));
        }
    }

    /**
     * Class container for all customizable UI element colors
     * Below are other related theme functionality that might be handy in the future
     final Integer NUM_COLOR_THEMES = getResources().getStringArray(R.array.pref_color_theme_values).length;
     void setNextColorTheme() { themeColors = new ThemeColors((themeColors.id % (NUM_COLOR_THEMES)) + 1); }
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
            this(Integer.valueOf(sp.getString(PREF_KEY_CUR_COLOR_THEME, "1")));
        }

        /**
         * This constructor will load the preferences for the corresponding themeId
         * Note: using temp vars to deal with final vars and possible exceptions
         * @param themeId Integer the themeColors in the resources to load
         */
        ThemeColors(Integer themeId) {
            id = themeId;  // Need to do validation on the number
            String style = String.format(Locale.US, "StoicColorTheme_%02d", themeId);
            Integer cCGB, cCGF, cCBB, cCBF, aCB;
            String n = null;
            cCGB = cCGF = cCBB = cCBF = aCB = null;

            try {
                TypedArray ta = obtainStyledAttributes(getResources().getIdentifier(style, "style", getPackageName()), R.styleable.StoicColorStyle);
                n = ta.getString(R.styleable.StoicTextStyle_displayName);
                aCB = ta.getColor(R.styleable.StoicColorStyle_colorPrimary, Color.TRANSPARENT);
                cCGB = ta.getColor(R.styleable.StoicColorStyle_colorGoodBg, Color.TRANSPARENT);
                cCGF = ta.getColor(R.styleable.StoicColorStyle_colorGoodFg, Color.TRANSPARENT);
                cCBB = ta.getColor(R.styleable.StoicColorStyle_colorBadBg, Color.TRANSPARENT);
                cCBF = ta.getColor(R.styleable.StoicColorStyle_colorBadFg, Color.TRANSPARENT);
                ta.recycle();
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
    }

    /**
     * Class container for all customizable UI element text
     * Below are other related theme functionality that might be handy in the future
     final Integer NUM_TEXT_THEMES = getResources().getStringArray(R.array.pref_text_theme_values).length;
     void setNextTextTheme() { themeText = new ThemeText((themeText.id % (NUM_TEXT_THEMES)) + 1); }
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
            this(Integer.valueOf(sp.getString(PREF_KEY_CUR_TEXT_THEME, "1")));
        }

        /**
         * This constructor will load the preferences for the corresponding themeId
         * Note: using temp vars to deal with final vars and possible exceptions
         * @param themeId Integer the themeColors in the resources to load
         */
        ThemeText(Integer themeId) {
            id = themeId;  // Need to do validation on the number
            String style = String.format(Locale.US, "StoicTextTheme_%02d", themeId);
            String n, p, cTG, cTB, cTDS, cTDU;
            n = p = cTG = cTB = cTDS = cTDU = null;

            try {
                TypedArray ta = obtainStyledAttributes(getResources().getIdentifier(style, "style", getPackageName()), R.styleable.StoicTextStyle);
                n = ta.getString(R.styleable.StoicTextStyle_displayName);
                p = ta.getString(R.styleable.StoicTextStyle_choicePrompt);
                cTG = ta.getString(R.styleable.StoicTextStyle_choiceGood);
                cTB = ta.getString(R.styleable.StoicTextStyle_choiceBad);
                cTDS = ta.getString(R.styleable.StoicTextStyle_choiceDisabledSelected);
                cTDU = ta.getString(R.styleable.StoicTextStyle_choiceDisabledUnselected);
                ta.recycle();
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
    }

    class Datastore {
        // Database fields
        static final String TABLE_BASE = "diary";
        static final String TABLE_DESC = "feels";
        static final String COLUMN_DAY = "time_stamp";
        static final String COLUMN_CHOICE = "choice";
        static final String COLUMN_UPDATE_DATE = "last_updated";
        static final String COLUMN_UPDATE_COUNT = "update_count";
        static final String COLUMN_DESC_F_KEY = "diary_id";
        static final String COLUMN_WORDS = "words";

        private StoicDatabase db;
        UserStatistics us;

        Datastore() {
            db = new StoicDatabase(getApplicationContext());
            us = new UserStatistics();
        }

        /**
         * For unit testing
         * @return SQLiteDatabase
         */
        SQLiteDatabase getWritableDatabase() {
            return db.getWritableDatabase();
        }

        /**
         * This does what it says
         */
        void rebuildDatabase() {
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

        /**
         * For checking the current value and whether/when the value was and can be changed
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

        void threadReCalc() {
            new Thread(new Runnable() {
                public void run(){
                    ds.us.recalculateStats();
                }
            }).start();
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
                        dbw.update(TABLE_BASE, newValues, String.format("%s=%s", COLUMN_DAY, Long.toString(date)), null);
                        didWriteSucceed = true;
                    } // else { // What to return when update fails due to count? }
                }
            } else {  // Insert
                newValues.put(COLUMN_DAY, date);
                newValues.put(COLUMN_UPDATE_COUNT, 1);
                dbw.insert(TABLE_BASE, null, newValues);
                didWriteSucceed = true;
            }
            Log.d("DateSet", String.format(LOG_STRING, date, theChoice, oldValues.get(COLUMN_CHOICE)));
            dbw.close();
            if (didWriteSucceed) { threadReCalc(); }
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
                    didWriteSucceed = 1 == dbw.update(TABLE_DESC, newValues, String.format("%s=%s", COLUMN_DESC_F_KEY, oldValues.getAsString("id")), null);
                } else {  // insert
                    didWriteSucceed = -1 < dbw.insert(TABLE_DESC, null, newValues);
                }
            }
            dbw.close();
            if (didWriteSucceed) { threadReCalc(); }
            return didWriteSucceed;
        }

        /**
         *
         * @return Long Date of the first entry in the database
         */
        Long getEarliestEntryDate() {
            Long earliestDate = Util.getLongVal(2017, 12, 2); // Hardcode debug
            Long noEntryDate = Util.getLongVal(LocalDateTime.now());
            if (!isDebugMode()) {
                SQLiteDatabase dbr = db.getReadableDatabase();
                Cursor c = dbr.query(TABLE_BASE, new String[] { String.format("min(%s)", COLUMN_DAY) },
                        null, null,null, null, null);
                c.moveToFirst();
                earliestDate = c.isNull(0) ? noEntryDate : c.getLong(0);
                c.close();
            }
            return earliestDate;
        }

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

        /**
         * Encapsulates the data we'll display to the user in the Summary tab
         * (visual) choices previous month (see 7 minute workout)
         */
        class UserStatistics {
            final LocalDate now = LocalDate.now(ZoneId.systemDefault());
            final Long dateMonthStart = now.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            final Long dateWeekStart = now.with(ChronoField.DAY_OF_WEEK, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            final Integer MONTH_LENGTH = now.lengthOfMonth();
            final Integer TODAY_OF_MONTH = now.getDayOfMonth();
            final String DAYS_HEADER = String.join(" ", Arrays.asList(getResources().getStringArray(R.array.stat_day_abbr)));
            final DateTimeFormatter dateFormatter =
                    DateTimeFormatter.ofLocalizedDate( FormatStyle.SHORT )
                            .withLocale( Locale.getDefault() ).withZone( ZoneId.systemDefault() );

            Long earliestChoiceMade;
            Long earliestWrittenHistory;
            Long latestTweet;  // Need to track this
            Long countCurrentConsecutiveChoicesMade;
            Long countMaximumConsecutiveChoicesMade;
            Long countChoicesMade;
            Long countChoicesChanged;  // sum of update_count minus count of choices
            Long countChoicesLocked;
            Long countWrittenHistoryThisWeek;
            Long countWrittenHistoryThisMonth;
            Long countWrittenHistoryAllTime;
            Long sumValueChoicesMadeThisWeek;
            Long sumValueChoicesMadeThisMonth;
            Long sumValueChoicesMadeAllTime;
            ContentValues countChoicesMadeByDayOfWeek = new ContentValues();
            ContentValues sumValueChoicesMadeByDayOfWeek = new ContentValues(); // Array w/M-Su, can use for weekday/weekends too
            ContentValues choicesMadeThisMonth = new ContentValues();  //
            // All these queries are hardcoded, clean them up and use constants and formatting as above
            final static String qECM = "SELECT min(time_stamp) FROM diary";
            final static String qEWH = "SELECT d.time_stamp, f.words FROM diary d LEFT OUTER JOIN feels f ON f.diary_id = d.id ORDER BY d.time_stamp;";
            final static String qCCM = "SELECT count(1) FROM diary";
            final static String qCCC = "SELECT (sum(update_count)-count(update_count)) AS count FROM diary";
            final static String qCCL = "SELECT count(update_count) FROM diary WHERE update_count > 2";
            final static String qCWHTAT = "SELECT count(words) FROM feels";
            final static String qSVCMTAT = "SELECT sum(choice) FROM diary";
            final static String qCWHTM = "SELECT count(words) FROM feels JOIN diary ON feels.diary_id=diary.id WHERE diary.time_stamp >= %s";
            final static String qCWHTW = "SELECT count(words) FROM feels JOIN diary ON feels.diary_id=diary.id WHERE diary.time_stamp >= %s";
            final static String qSVCMTM = "SELECT sum(CASE (choice) WHEN 0 THEN -1 WHEN 1 THEN 1 END) as choiceValue FROM diary WHERE time_stamp >= %s";
            final static String qSVCMTW = "SELECT sum(CASE (choice) WHEN 0 THEN -1 WHEN 1 THEN 1 END) as choiceValue FROM diary WHERE time_stamp >= %s";
            final static String qCMTM = "SELECT time_stamp, CASE (choice) WHEN 0 THEN -1 WHEN 1 THEN 1 END AS choice FROM diary WHERE time_stamp >= %s";
            final static String qCCCM = "SELECT strftime('%s', date(time_stamp / 1000, 'unixepoch', 'localtime')) / 86400 as day_num FROM diary ORDER BY day_num;";
            final static String qCCMBDOW  = "SELECT strftime('%w', date(time_stamp / 1000, 'unixepoch', 'localtime')) AS dayofweek, count(1) \n" +
                    "FROM diary GROUP BY dayofweek ORDER BY dayofweek";
            final static String qSVCMBDOW  = "SELECT strftime('%w', date(time_stamp / 1000, 'unixepoch', 'localtime')) AS dayofweek, " +
                    "sum(CASE (choice) WHEN 0 THEN -1 WHEN 1 THEN 1 END) as choiceValue \n" +
                    "FROM diary GROUP BY dayofweek ORDER BY dayofweek";

            /**
             *
             */
            private UserStatistics() {
                recalculateStats();
            }

            /**
             * This just formats the data currently in the UserStatistics class for display
             * These are displayed in the UI in the same order they're added to the list
             * @return Statistic[] array of stats to display
             */
            Statistic[] getStatsList() {
                ArrayList<Statistic> statsList = new ArrayList<>();

                statsList.add(new Statistic(getString(R.string.stat_title_choices_made_this_month),
                        assembleChoicesByCal(choicesMadeThisMonth)));
                statsList.add(new Statistic(getString(R.string.stat_title_count_choices_by_day_of_week),
                        assembleChoicesByDOW(countChoicesMadeByDayOfWeek)));
                statsList.add(new Statistic(getString(R.string.stat_title_sum_value_by_day_of_week),
                        assembleChoicesByDOW(sumValueChoicesMadeByDayOfWeek)));
                statsList.add(new Statistic(getString(R.string.stat_title_count_choices_made),
                        String.format(Locale.getDefault(), "%,d", countChoicesMade)));
                statsList.add(new Statistic(getString(R.string.stat_title_count_choices_changed),
                        String.format(Locale.getDefault(), "%,d", countChoicesChanged)));
                statsList.add(new Statistic(getString(R.string.stat_title_count_choices_locked),
                        String.format(Locale.getDefault(), "%,d", countChoicesLocked)));
                statsList.add(new Statistic(getString(R.string.stat_title_current_consecutive_choies),
                        String.format(Locale.getDefault(), "%,d", countCurrentConsecutiveChoicesMade)));
                statsList.add(new Statistic(getString(R.string.stat_title_maximum_consecutive_choices_made),
                        String.format(Locale.getDefault(), "%,d", countMaximumConsecutiveChoicesMade)));
                statsList.add(new Statistic(getString(R.string.stat_title_count_written_history_all_time),
                        String.format(Locale.getDefault(), "%,d", countWrittenHistoryAllTime)));
                statsList.add(new Statistic(getString(R.string.stat_title_earliest_choice_made),
                        dateFormatter.format(Instant.ofEpochMilli(earliestChoiceMade))));
                statsList.add(new Statistic(getString(R.string.stat_title_earliest_written_note),
                        dateFormatter.format(Instant.ofEpochMilli(earliestWrittenHistory))));
                statsList.add(new Statistic(getString(R.string.stat_title_latest_tweet),
                        dateFormatter.format(Instant.ofEpochMilli(latestTweet))));
                statsList.add(new Statistic(getString(R.string.stat_title_count_written_history_this_month),
                        String.format(Locale.getDefault(), "%,d", countWrittenHistoryThisMonth)));
                statsList.add(new Statistic(getString(R.string.stat_title_count_written_history_this_week),
                        String.format(Locale.getDefault(), "%,d", countWrittenHistoryThisWeek)));
                statsList.add(new Statistic(getString(R.string.stat_title_sum_value_choices_made_this_month),
                        String.format(Locale.getDefault(), "%,d", sumValueChoicesMadeThisMonth)));
                statsList.add(new Statistic(getString(R.string.stat_title_sum_value_choices_made_this_week),
                        String.format(Locale.getDefault(), "%,d", sumValueChoicesMadeThisWeek)));

                Statistic[] retVal = new Statistic[statsList.size()];
                retVal = statsList.toArray(retVal);
                return retVal;
            }

            /**
             * @param cv ContentValues with 7 keys for the day of the week (0-6) and values to display
             * @return String to output (assumes monospaced) with day-of-week headers
             */
            String assembleChoicesByDOW(ContentValues cv) {
                StringBuilder output = new StringBuilder();

                output.append(String.format("%s \n", DAYS_HEADER));
                for (int i = 0; i < 7; i++) {
                    String v = String.format(Locale.getDefault(),"%03d ", cv.getAsInteger(Integer.toString(i)));
                    output.append(v.equals("null ") ? "000 " : v);
                }
                return output.toString();
            }

            /**
             * @param cv ContentValues keys corresponding to any values in the DB for the current month
             * @return String multiline monospaced representing calendar of current month choices made
             */
            String assembleChoicesByCal(ContentValues cv) {
                final String CHOICE_NOT_MADE = ".";
                final String CHOICE_NOT_PAST = "?";
                final String FORMAT_VALUE = "%3s ";

                StringBuilder output = new StringBuilder();
                LocalDate date = now.withDayOfMonth(1);
                int month = now.getMonthValue();
                LocalDate endOfMonth = date.withDayOfMonth(MONTH_LENGTH).plusDays(1);
                while (date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                    date = date.minusDays(1);
                }
                output.append(String.format("%s\n", DAYS_HEADER));
                while (date.isBefore(endOfMonth)) {
                    StringBuilder row = new StringBuilder(7 * 5);
                    for (int index = 0; index < 7; index++) {
                        String dbValue = cv.getAsString(String.valueOf(date.getDayOfMonth()));
                        if (dbValue == null) {
                            dbValue = (date.getDayOfMonth() <= TODAY_OF_MONTH) ? CHOICE_NOT_MADE : CHOICE_NOT_PAST;
                        }
                        String value = (month != date.getMonthValue()) ? "" : dbValue;
                        row.append(String.format(Locale.getDefault(), FORMAT_VALUE, value));
                        date = date.plusDays(1);
                    }
                    output.append(String.format("%s\n",row));
                }
                return output.toString();
            }

            /**
             * Set the values of the member variables based on info in the DB
             */
            void recalculateStats() {
                // Don't have a field for this in the DB currently, so faking it
                final String qLT = String.format(Locale.US, "SELECT %d AS time_stamp", dateWeekStart);

                SQLiteDatabase dbr = db.getReadableDatabase();
                Cursor cursor;

                // Do some querying...
                latestTweet                         = DatabaseUtils.longForQuery(dbr, qLT, null);
                earliestChoiceMade                  = DatabaseUtils.longForQuery(dbr, qECM, null);
                earliestWrittenHistory              = DatabaseUtils.longForQuery(dbr, qEWH, null);
                countChoicesMade                    = DatabaseUtils.longForQuery(dbr, qCCM, null);
                countChoicesChanged                 = DatabaseUtils.longForQuery(dbr, qCCC, null);
                countChoicesLocked                  = DatabaseUtils.longForQuery(dbr, qCCL, null);
                sumValueChoicesMadeAllTime          = DatabaseUtils.longForQuery(dbr, qSVCMTAT, null);
                countWrittenHistoryAllTime          = DatabaseUtils.longForQuery(dbr, qCWHTAT, null);
                countWrittenHistoryThisMonth        = DatabaseUtils.longForQuery(dbr, String.format(qCWHTM, dateMonthStart), null);
                countWrittenHistoryThisWeek         = DatabaseUtils.longForQuery(dbr, String.format(qCWHTW, dateWeekStart), null);
                sumValueChoicesMadeThisMonth        = DatabaseUtils.longForQuery(dbr, String.format(qSVCMTM, dateMonthStart), null);
                sumValueChoicesMadeThisWeek         = DatabaseUtils.longForQuery(dbr, String.format(qSVCMTW, dateWeekStart), null);
                countMaximumConsecutiveChoicesMade  = 0L;
                countCurrentConsecutiveChoicesMade  = 0L;

                cursor = dbr.rawQuery(qSVCMBDOW, null);
                while (cursor.moveToNext()) {
                    sumValueChoicesMadeByDayOfWeek.put(cursor.getString(0), cursor.getInt(1));
                }
                cursor = dbr.rawQuery(qCCMBDOW, null);
                while (cursor.moveToNext()) {
                    countChoicesMadeByDayOfWeek.put(cursor.getString(0), cursor.getInt(1));
                }

                cursor = dbr.rawQuery(String.format(qCMTM, dateMonthStart), null);
                while (cursor.moveToNext()) {  // Get all of the choices recorded
                    Integer dayOfMonth = LocalDate.from(Instant.ofEpochMilli(cursor.getLong(0)).atZone(ZoneId.systemDefault())).getDayOfMonth();
                    choicesMadeThisMonth.put(String.valueOf(dayOfMonth), cursor.getInt(1));
                }

                ArrayList<Long> datesChoiceMade = new ArrayList<>();
                cursor = dbr.rawQuery(qCCCM, null);  // Liable to be slow...
                while (cursor.moveToNext()) {
                    datesChoiceMade.add(cursor.getLong(0));
                }
                setConsecutiveDates(datesChoiceMade);
                cursor.close();
            }

            /**
             * SIDE EFFECT of setting the member variables. Clean this up...
             * @param datesChoiceMade ArrayList<> of dates as days from unixepoch
             */
            private void setConsecutiveDates(final ArrayList<Long> datesChoiceMade) {
                int numConsec = 1;
                for (int i = 0; i < datesChoiceMade.size() - 1; i++) {
                    if (datesChoiceMade.get(i).equals(datesChoiceMade.get(i+1) - 1)) {
                        numConsec++;
                    } else {
                        if (numConsec > countMaximumConsecutiveChoicesMade) {
                            countMaximumConsecutiveChoicesMade = Integer.toUnsignedLong(numConsec);
                        }
                        numConsec = 1;
                    }
                }
                if (numConsec > countMaximumConsecutiveChoicesMade) {  // If max was at end, assign
                    countMaximumConsecutiveChoicesMade = Integer.toUnsignedLong(numConsec);
                }

                numConsec = 1;
                if (datesChoiceMade.get(datesChoiceMade.size() - 1) != now.toEpochDay()) {
                    countCurrentConsecutiveChoicesMade = 0L;
                } else {
                    for (int i = datesChoiceMade.size() - 1; i > 0; i--) {
                        if (datesChoiceMade.get(i).equals(datesChoiceMade.get(i - 1) + 1)) {
                            numConsec++;
                        } else {
                            countCurrentConsecutiveChoicesMade = Integer.toUnsignedLong(numConsec);
                            break;
                        }
                    }
                }
            }

            class Statistic {
                String title;
                String value;

                Statistic(String theTitle, String theValue) {
                    title = theTitle;
                    value = theValue;
                }
            }
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
         * Take the long date value we get from the CalendarView and normalize it so we're always
         * comparing/saving the same value in the database.
         * @param date Long the current date/time, which we'll chop according to the methods here
         * @return Long the date value that we'll use for storing and comparison
         */
        static Long getLongVal(Long date) {
            return Util.getLongVal(LocalDateTime.ofInstant(Instant.ofEpochMilli(date), ZoneId.systemDefault()));
        }

        /**
         * Use this to get the value we set the calendar to and the one we store in the DB
         * @param ldt LocalDateTime an object we're using to get the expected value
         * @return Long the date munged appropriately that we'll use for storing and comparison
         */
        static Long getLongVal(LocalDateTime ldt) {
            return ldt.truncatedTo(ChronoUnit.DAYS).atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli();
        }

    }
}
