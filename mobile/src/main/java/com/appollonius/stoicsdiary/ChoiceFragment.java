package com.appollonius.stoicsdiary;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.EditText;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ChoiceFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ChoiceFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChoiceFragment extends android.app.Fragment implements View.OnClickListener,
        CalendarView.OnDateChangeListener {

    private StoicDatabase db;

    // TODO: Rename parameter arguments, choose names that match the fragment initialization parameters
    // e.g. ARG_ITEM_NUMBER
//    private static final String ARG_PARAM1 = "param1";
//    private static final String ARG_PARAM2 = "param2";
//    private String mParam1;  // TODO: Rename and change types of parameters
//    private String mParam2;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @ param param1 Parameter 1.
     * @ param param2 Parameter 2.
     * @return A new instance of fragment ChoiceFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ChoiceFragment newInstance(/*String param1, String param2*/) {
        ChoiceFragment fragment = new ChoiceFragment();
        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public ChoiceFragment() {
        // Required empty public constructor
    }

    private OnFragmentInteractionListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (getArguments() != null) {
//            mParam1 = getArguments().getString(ARG_PARAM1);
//            mParam2 = getArguments().getString(ARG_PARAM2);
//        }
        db = new StoicDatabase(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(R.layout.fragment_choice, container, false);

        ArrayList<View> children = ((StoicActivity)getActivity()).getAllChildren(v);
        for (View child: children) {
            if (child instanceof AppCompatButton) {  // Android Activity/Fragment event handling sucks
                child.setOnClickListener(this);
            } else if (child instanceof CalendarView) {
                initializeCalendar((CalendarView)child);
            }
        }
        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(
                    String.format("%s must implement OnFragmentInteractionListener",
                            context.toString())
            );
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);  // TODO: Update argument type and name
    }

    public void initializeCalendar(CalendarView calendar) {
        calendar.setOnDateChangeListener(this);
        calendar.setMaxDate(calendar.getDate());
        calendar.setMinDate(getEarliestEntryDate() * 1000);  // TODO: Earliest recorded date in DB
    }

    @Override
    public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
        String logString = "Date is %s (%s), value %s";
        LocalDateTime date = LocalDateTime.of(year, month + 1, dayOfMonth, 0, 0);
        view.setDate(date.toEpochSecond(ZoneOffset.UTC) * 1000);  // Actually update the calendar

        Boolean dayValue = getDayValue(date.toLocalDate().toEpochDay());
        String valueText = dayValue != null ? Boolean.toString(dayValue) : "NOT CHOSEN";
        Log.d("DateSelected",
                String.format(logString, date, date.toLocalDate().toEpochDay(), valueText));

        // Update UI elements
        setFeelsText(String.format(logString, date, date.toLocalDate().toEpochDay(), valueText));
        // Change button states based on dayValue
    }

    /**
     * For convenience
     * @param date Long
     * @return Boolean true, false or NULL
     */
    private Boolean getDayValue(Long date) {
        String Q_SELECTVALUE = "SELECT value from diary WHERE timestamp=%s;";
        SQLiteDatabase dbr = db.getReadableDatabase();

        Cursor cursor = dbr.rawQuery(String.format(Q_SELECTVALUE, date), null);
        Boolean retVal = cursor.moveToFirst() ? 1 == cursor.getInt(0) : null;

        cursor.close();
        return retVal;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonYes:
                onClickYes(); break;
            case R.id.buttonNo:
                onClickNo(); break;
        }
    }

    public void onClickYes() {
        setFeelsText(Boolean.toString(setSelectedValue(true)));
    }

    public void onClickNo() {
        setFeelsText(Boolean.toString(setSelectedValue(false)));
    }

    public Boolean setSelectedValue(Boolean value) {
        CalendarView calendarView = getView().findViewById(R.id.history);
        Long date = LocalDate.from(Instant.ofEpochSecond(calendarView.getDate() / 1000).atZone(ZoneOffset.UTC)).toEpochDay();
        Long dateToo = calendarView.getDate() / 24 / 60 / 60 / 1000;
        return setDayValue(date, value);
    }

    public void setFeelsText(String text) {
        EditText t = getActivity().findViewById(R.id.feels_text);
        t.setText(text);
    }

    /**
     * TODO Find an elegant upsert behavior, sqlite has 'INSERT OR REPLACE'
     */
    private Boolean setDayValue(Long date, Boolean newValue) {
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

    private long getEarliestEntryDate() {
        Long retVal;
        SQLiteDatabase dbr = db.getReadableDatabase();
        Cursor c = dbr.query(StoicActivity.TABLE_BASE, new String[] { "min(timestamp)" },
                null, null,null, null, null);
        c.moveToFirst();
        retVal = c.getLong(0);
        c.close();
        return retVal * 24 * 60 * 60;
    }
}
