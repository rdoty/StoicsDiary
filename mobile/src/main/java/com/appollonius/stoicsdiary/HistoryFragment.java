package com.appollonius.stoicsdiary;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import java.time.LocalDateTime;
import java.util.ArrayList;

import com.appollonius.stoicsdiary.StoicActivity.Util;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HistoryFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HistoryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HistoryFragment extends android.support.v4.app.Fragment implements View.OnClickListener,
        CalendarView.OnDateChangeListener {

    private static final String ARG_PARAM1 = "param1";
    String mParam1;
    /**
     * Use this factory method to create a new instance of this fragment using the provided parameters.
     * (choose names that match the fragment initialization parameters)
     *
     * @param param1 String TODO: Rename/change types and number of parameters, incl arguments above
     * @return A new instance of fragment HistoryFragment.
     */
    public static HistoryFragment newInstance(String param1) {
        HistoryFragment fragment = new HistoryFragment();
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
         * @param uri Uri  TO DO Update argument type and name
         */
        void onFragmentInteraction(Uri uri);
    }
    private OnFragmentInteractionListener mListener;

    public HistoryFragment() { }  // Required empty public constructor
    StoicActivity mA;  // rdoty - helps to call StoicActivity instance without null compiler warnings

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
        }
        mA = (StoicActivity)getActivity();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        mA.updateUI(getView());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        bindEventHandlers();
        initializeUI();
    }

    /**
     * Initialization of all UI elements on startup
     * Note: Android Activity/Fragment event handling sucks
     */
    private void bindEventHandlers() {
        ArrayList<View> children = Util.getAllChildren(getView());
        for (View child: children) {
            if (child instanceof RadioButton || child instanceof Button) {
                child.setOnClickListener(this);
            } else if (child instanceof CalendarView) {
                ((CalendarView)child).setOnDateChangeListener(this);
            }
        }
    }

    /**
     * Set other UI element behavior, including random text
     */
    private void initializeUI() {
        initializeCalendar();
        ((TextView)mA.findViewById(R.id.TEXT_QUOTE)).setText(mA.getQuote());
        mA.updateUI(getView());
    }

    /**
     *
     */
    private void initializeCalendar() {
        CalendarView calendarView = mA.findViewById(R.id.history);
        calendarView.setMaxDate(Util.getLongVal(LocalDateTime.now()));
        calendarView.setMinDate(mA.getEarliestEntryDate());
        calendarView.setDate(Util.getLongVal(mA.getCurrentDay()));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.BUTTON_YES:
            case R.id.BUTTON_NO:
                onClickChoice(v.getId()); break;
            case R.id.BUTTON_FEELS_SAVE:
                onClickFeelsSave(); break;
            case R.id.BUTTON_FEELS_TWEET:
                onClickFeelsTweet(); break;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(
                    String.format("%s must implement OnFragmentInteractionListener", context.toString()));
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
        mA.setCurrentDay(Util.getLongVal(year, month + 1, dayOfMonth));
        view.setDate(mA.getCurrentDay());  // Update calendar with normalized value
        mA.updateUI(getView());  // Set UI based on data for the day, enable/disable controls as needed
   }

    /**
     * @param choiceId int The id of the choice button selected
     */
    private void onClickChoice(int choiceId) {
        Boolean success = mA.writeDayValue(mA.getCurrentDay(), R.id.BUTTON_YES == choiceId);
        Log.d("DEBUG", String.format("writeDayValue result: %b", success));
        mA.updateUI(getView());
    }

    private void onClickFeelsSave() {
        Boolean success = mA.writeDayFeels(mA.getCurrentDay(),
                ((EditText)mA.findViewById(R.id.EDIT_FEELS)).getText().toString());
        Log.d("DEBUG", String.format("writeDayFeels result: %b", success));
        mA.updateUI(getView());
    }

    private void onClickFeelsTweet() {
        mA.sendTweet();
    }
}
