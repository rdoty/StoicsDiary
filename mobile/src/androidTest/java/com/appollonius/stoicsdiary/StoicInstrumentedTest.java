package com.appollonius.stoicsdiary;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static androidx.test.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class StoicInstrumentedTest {
    private Instrumentation.ActivityMonitor monitor;
    private StoicActivity mStoicActivity;
    private Instrumentation mInstrumentation;
    private SharedPreferences mLoginPrefs;

    public StoicInstrumentedTest() {
        super();
    }

    @Before
    public void setUp() throws Exception {
        //mStoicActivity = getActivity();
        mInstrumentation = getInstrumentation();
        monitor = mInstrumentation.addMonitor(StoicActivity.class.getName(), null, false);
    }

    @After
    public void tearDown() throws Exception {
        mInstrumentation.removeMonitor(monitor);
    }

    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
        assertEquals("com.appollonius.stoicsdiary", appContext.getPackageName());
    }

    @Test
    public void confirmDatabases() throws Exception {
        Context appContext = InstrumentationRegistry.getTargetContext();
        assertNotEquals(0, appContext.databaseList().length);
        appContext.startActivity(new Intent(Intent.ACTION_DEFAULT));

    }

    private void addTestData() {
        String query = "INSERT INTO diary VALUES(%s, %s, %s, %s)";
        Long date = Instant.now().truncatedTo(ChronoUnit.DAYS).toEpochMilli();
        mStoicActivity.ds.getWritableDatabase().execSQL(String.format(query, date, date, 1, 1));
    }
}
