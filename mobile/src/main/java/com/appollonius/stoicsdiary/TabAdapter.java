package com.appollonius.stoicsdiary;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Created on 2018.01.28.
 */
public class TabAdapter extends FragmentPagerAdapter {
    public TabAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public Fragment getItem(int position) {
//        switch (position) {
//            case 0:
//                return (Fragment)ChoiceFragment.newInstance("");
//        }
        return null;
    }

    @Override
    public int getCount() {
        return 0;
    }
}
