package com.appollonius.stoicsdiary;

import android.content.ContentValues;
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
import android.widget.RadioGroup;
import android.widget.TextView;

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

    private ContentValues selectedDayValues;
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
     * Note: Android Activity/Fragment event handling sucks
     * @param v View passing this because we are calling this from onCreateView before it completes
     */
    private void bindEventHandlers(View v) {
        ArrayList<View> children = ((StoicActivity)getActivity()).getAllChildren(v);
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
     * @param v View passing this because we are calling this from onCreateView before it completes
     */
    private void initializeUI(View v) {
        initializeTheme(v);
        initializeCalendar(v);
    }

    /**
     * This includes default colors, text prompts, maybe user-provided text, too.
     * @param v View passing this because we are calling this from onCreateView before it completes
     */
    private void initializeTheme(View v) {
        ((TextView)v.findViewById(R.id.TEXT_PROMPT)).setText(((StoicActivity)getActivity()).themeWords.prompt);
        ((Button)v.findViewById(R.id.BUTTON_YES)).setText(((StoicActivity)getActivity()).themeWords.choiceTextGood);
        ((Button)v.findViewById(R.id.BUTTON_YES)).setTextColor(((StoicActivity)getActivity()).themeColors.choiceColorGoodFg);
        //((Button)v.findViewById(R.id.BUTTON_YES)).setBackgroundColor(themeColors.choiceColorGoodBg);  // Need to modify tint not color

        ((Button)v.findViewById(R.id.BUTTON_NO)).setText(((StoicActivity)getActivity()).themeWords.choiceTextBad);
        ((Button)v.findViewById(R.id.BUTTON_NO)).setTextColor(((StoicActivity)getActivity()).themeColors.choiceColorBadFg);
        //((Button)v.findViewById(R.id.BUTTON_NO)).setBackgroundColor(themeColors.choiceColorBadBg);  // Need to modify tint not color

        ((TextView)v.findViewById(R.id.TEXT_DEBUG)).setText(getQuote(null));
    }

    /**
     *
     * @param v View passing this because we are calling this from onCreateView before it completes
     */
    private void initializeCalendar(View v) {
        CalendarView calendarView = v.findViewById(R.id.history);
        calendarView.setMaxDate(calendarView.getDate());
        calendarView.setMinDate(((StoicActivity)getActivity()).getEarliestEntryDate());
    }

    /**
     * Grabs a quote from the resource
     * @param quoteId Integer if null, gets a random quote
     * @return String the quote to display
     */
    private String getQuote(@Nullable Integer quoteId) {
        final int NUM_QUOTES = 5;  // Figure count out dynamically
        if (null == quoteId)
            quoteId = new Random().nextInt(NUM_QUOTES) + 1;
        String num = String.format(Locale.US, "%02d", quoteId);
        return this.getText(getResources().getIdentifier("quote_" + num,"string", BuildConfig.APPLICATION_ID)).toString();
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
        Long date = ((StoicActivity)getActivity()).getLongVal(year, month + 1, dayOfMonth);
        view.setDate(date);  // Actually update the calendar UI
        onWriteUpdateUI(true);  // Set UI based on data for the day, enable/disable controls as needed
   }

    private void onClickChoice(int choiceId) {
        onWriteUpdateUI(writeSelectedValue(R.id.BUTTON_YES == choiceId));
    }
    /**
     * This retrieves the values for the selected day from the database
     * and updates the UI. This should be called after values have been
     * written to the database.
     * e.g. disabling buttons if there are no more changes allowed
     */
    private void onWriteUpdateUI(Boolean writeSuccessful) {
        String logFormat = "Write %s - Date %s (%s), %s updates";
        Boolean enableChoice = true;
        Boolean isChoiceSet;

        selectedDayValues = ((StoicActivity)getActivity()).getChoice(getSelectedCalendarDate());
        // Get references to all the controls we'll be updating
        RadioGroup radioGroupChoices = getActivity().findViewById(R.id.GROUP_CHOICES);
        EditText editTextFeels = getActivity().findViewById(R.id.EDIT_FEELS);
        RadioButton buttonYes = getActivity().findViewById(R.id.BUTTON_YES);
        RadioButton buttonNo = getActivity().findViewById(R.id.BUTTON_NO);

        radioGroupChoices.clearCheck();  // Clear previous selection in case choice not set

        isChoiceSet = selectedDayValues.getAsBoolean("isSet");
        if (isChoiceSet) {  // Check the proper choice, also confirm whether we can change it
            enableChoice = selectedDayValues.getAsBoolean("isMutable");
            radioGroupChoices.check(selectedDayValues.getAsBoolean(StoicActivity.COLUMN_CHOICE) ? R.id.BUTTON_YES : R.id.BUTTON_NO);
        }
        for (View child: ((StoicActivity)getActivity()).getAllChildren(radioGroupChoices)) {
            child.setEnabled(enableChoice);
        }
        // Consider doing this (but simplify logic)
        buttonYes.setText(
                (enableChoice
                        ? ((StoicActivity)getActivity()).themeWords.choiceTextGood
                        : buttonYes.isChecked()
                            ? ((StoicActivity)getActivity()).themeWords.choiceTextDisabledSelected
                            : ((StoicActivity)getActivity()).themeWords.choiceTextDisabledUnselected));
        buttonNo.setText(
                (enableChoice
                        ? ((StoicActivity)getActivity()).themeWords.choiceTextBad
                        : buttonNo.isChecked()
                            ? ((StoicActivity)getActivity()).themeWords.choiceTextDisabledSelected
                            : ((StoicActivity)getActivity()).themeWords.choiceTextDisabledUnselected));

        Integer hintResource = isChoiceSet ? R.string.feels_prompt_enabled : R.string.feels_prompt_disabled;
        editTextFeels.setHint(getText(hintResource));
        editTextFeels.setEnabled(isChoiceSet);
        setFeelsText(selectedDayValues.getAsString(StoicActivity.COLUMN_WORDS));

        // Debug output
        String logOutput = String.format(logFormat,
                writeSuccessful, "yy/mm/dd",
                selectedDayValues.getAsLong("choiceDate"),
                selectedDayValues.getAsInteger(StoicActivity.COLUMN_UPDATE_COUNT));
        Log.d("DateSelected", logOutput);
        setDebugText(logOutput);
    }

    private void onClickFeelsTweet() {

    }

    /**
     *
     * @return Long the date currently selected in the calendar UI
     */
    private Long getSelectedCalendarDate() {
        CalendarView calendarView = getActivity().findViewById(R.id.history);
        return calendarView.getDate();
    }

    private void onClickFeelsSave() {
        Boolean success = ((StoicActivity)getActivity()).writeDayFeels(
                getSelectedCalendarDate(),
                ((EditText)getActivity().findViewById(R.id.EDIT_FEELS)).getText().toString()
        );
        setDebugText(String.format("Did feels save? %s", success.toString()));
    }

    /**
     * Get the current date selected from the UI and write the value to database
     * @param value Boolean value to write
     * @return Boolean success of write
     */
    private Boolean writeSelectedValue(Boolean value) {
        return ((StoicActivity)getActivity()).writeDayValue(getSelectedCalendarDate(), value);
    }

    private void setFeelsText(String text) {
        ((EditText)getActivity().findViewById(R.id.EDIT_FEELS)).setText(text);
    }
    private void setDebugText(String text) {
        ((TextView)getActivity().findViewById(R.id.TEXT_DEBUG)).setText(text);
    }
}
