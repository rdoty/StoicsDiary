package com.appollonius.stoicsdiary;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;

import com.appollonius.stoicsdiary.StoicActivity.Util;
import static org.junit.Assert.*;

/**
 * Will execute on the development machine (host).
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 * 2018.01.25 created by rdoty
 */
public class StoicActivityTest {
    private StoicActivity activity;
    private Util util;

    @Before
    public void setUp() throws Exception {
        activity = new StoicActivity();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetLongVal() throws Exception {
        LocalDateTime zdt = LocalDateTime.of(2018,1,1,0,0,0);
        assertEquals(Util.getLongVal(zdt), Util.getLongVal(2018, 1, 1));
    }

    @Test
    public void testGetEarliestEntryDate() throws Exception {
    }

}