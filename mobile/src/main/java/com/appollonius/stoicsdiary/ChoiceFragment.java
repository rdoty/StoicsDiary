package com.appollonius.stoicsdiary;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.TextView;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;


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

    public ChoiceFragment() {
        // Required empty public constructor
    }

    private static final String ARG_PARAM1 = "param1";
    private String mParam1;
    /**
     * Use this factory method to create a new instance of this fragment using the provided parameters.
     * (choose names that match the fragment initialization parameters)
     *
     * @param param1 String TODO: Rename/change types and number of parameters, incl arguments above
     * @return A new instance of fragment ChoiceFragment.
     */
    public static ChoiceFragment newInstance(String param1) {
        ChoiceFragment fragment = new ChoiceFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
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
        /**
         * @param uri Uri  TODO: Update argument type and name
         */
        void onFragmentInteraction(Uri uri);
    }
    private OnFragmentInteractionListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(R.layout.fragment_choice, container, false);
        bindEventHandlers(v);
        initializeUI(v);
        return v;
    }

    /**
     * Initialization of all UI elements on startup
     * @param v This is passed because we are calling this from onCreateView before it completes
     */
    private void bindEventHandlers(View v) {
        ArrayList<View> children = ((StoicActivity)getActivity()).getAllChildren(v);
        for (View child: children) {
            if (child instanceof AppCompatButton) {  // Android Activity/Fragment event handling sucks
                child.setOnClickListener(this);
            } else if (child instanceof CalendarView) {
                ((CalendarView)child).setOnDateChangeListener(this);
            }
        }
    }

    /**
     *
     * @param v This is passed because we are calling this from onCreateView before it completes
     */
    private void initializeCalendar(View v) {
        CalendarView calendarView = v.findViewById(R.id.history);
        calendarView.setMaxDate(calendarView.getDate());
        Long minDate = ((StoicActivity)getActivity()).getEarliestEntryDate();
        //calendarView.setMinDate((minDate * 1000);  // Skip while debugging
    }

    /**
     * Set other UI element behavior, including random text
     * @param v This is passed because we are calling this from onCreateView before it completes
     */
    private void initializeUI(View v) {
        final int NUM_PROMPTS = 2;
        final int NUM_SELECTIONS = 2;
        final int NUM_QUOTES = 5;
        Random r = new Random();

        initializeCalendar(v);

        // Set text on UI elements
        String num = String.format(Locale.US, "%02d", r.nextInt(NUM_PROMPTS) + 1);
        TextView prompt = v.findViewById(R.id.TEXT_PROMPT);  // Set prompt to random selection
        prompt.setText(this.getResources().getIdentifier("prompt_" + num,"string", BuildConfig.APPLICATION_ID));

        num = String.format(Locale.US, "%02d", r.nextInt(NUM_SELECTIONS) + 1);
        Button buttonYes = v.findViewById(R.id.BUTTON_YES);
        buttonYes.setText(this.getResources().getIdentifier("good_" + num,"string", BuildConfig.APPLICATION_ID));
        Button buttonNo = v.findViewById(R.id.BUTTON_NO);
        buttonNo.setText(this.getResources().getIdentifier("bad_" + num,"string", BuildConfig.APPLICATION_ID));

        num = String.format(Locale.US, "%02d", r.nextInt(NUM_QUOTES) + 1);
        EditText editText = v.findViewById(R.id.EDIT_FEELS);
        editText.setText(this.getResources().getIdentifier("quote_" + num,"string", BuildConfig.APPLICATION_ID));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.BUTTON_YES:
                onClickYes(); break;
            case R.id.BUTTON_NO:
                onClickNo(); break;
        }
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

    @Override
    public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
        String logString = "Date is %s (%s), value %s";
        LocalDateTime date = LocalDateTime.of(year, month + 1, dayOfMonth, 0, 0);
        view.setDate(date.toEpochSecond(ZoneOffset.UTC) * 1000);  // Actually update the calendar

        Boolean dayValue = ((StoicActivity)getActivity()).getVerdict(date.toLocalDate().toEpochDay());
        String valueText = dayValue != null ? Boolean.toString(dayValue) : "NOT CHOSEN";
        Log.d("DateSelected",
                String.format(logString, date, date.toLocalDate().toEpochDay(), valueText));

        // Update UI elements
        setFeelsText(String.format(logString, date, date.toLocalDate().toEpochDay(), valueText));
        // Change button states based on dayValue -- using radiobuttons?
    }

    private void onClickYes() {
        setFeelsText(writeSelectedValue(true) ? "SET TO TRUE" : "FAILED TO SET");
    }

    private void onClickNo() {
        setFeelsText(writeSelectedValue(false) ? "SET TO FALSE" : "FAILED TO SET");
    }

    /**
     * Get the current date selected from the UI and write the value to database
     * @param value Boolean value to write
     * @return Boolean success of write
     */
    private Boolean writeSelectedValue(Boolean value) {
        CalendarView calendarView = getActivity().findViewById(R.id.history);
        Long date = LocalDate.from(Instant.ofEpochSecond(calendarView.getDate() / 1000).atZone(ZoneOffset.UTC)).toEpochDay();
        //Long dateToo = calendarView.getDate() / 86400 / 1000;
        return ((StoicActivity)getActivity()).setDayValue(date, value);
    }

    private void setFeelsText(String text) {
        EditText t = getActivity().findViewById(R.id.EDIT_FEELS);
        t.setText(text);
    }
}
