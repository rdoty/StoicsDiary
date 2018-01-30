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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

import com.appollonius.stoicsdiary.StoicActivity.Util;


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

    /**
     * Convenience to declutter some calls above
     * @return Theme object from the activity
     */
    private StoicActivity.ThemeColors getTC() { return ((StoicActivity)getActivity()).themeColors; }
    private StoicActivity.ThemeText getTW() { return ((StoicActivity)getActivity()).themeText; }
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

        return inflater.inflate(R.layout.fragment_choice, container, false);
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
        initializeTheme();
        initializeCalendar();
        updateUI(false);
    }

    /**
     * This includes default colors, text prompts, maybe user-provided text, too.
     */
    private void initializeTheme() {
        ((TextView)getView(R.id.TEXT_PROMPT)).setText(getTW().prompt);
        ((TextView)getView(R.id.TEXT_QUOTE)).setText(getQuote());

        ((Button)getView(R.id.BUTTON_YES)).setText(getTW().choiceTextGood);
        ((Button)getView(R.id.BUTTON_YES)).setTextColor(getTC().choiceColorGoodFg);
        //((Button)getView(R.id.BUTTON_YES)).getBackground().setColorFilter(getTC().choiceColorGoodBg, PorterDuff.Mode.DST_OUT);

        ((Button)getView(R.id.BUTTON_NO)).setText(getTW().choiceTextBad);
        ((Button)getView(R.id.BUTTON_NO)).setTextColor(getTC().choiceColorBadFg);
        //((Button)getView(R.id.BUTTON_NO)).getBackground().setColorFilter(getTC().choiceColorBadBg, PorterDuff.Mode.DST_OUT);
    }

    /**
     *
     */
    private void initializeCalendar() {
        CalendarView calendarView = getActivity().findViewById(R.id.history);
        calendarView.setMaxDate(calendarView.getDate());
        calendarView.setMinDate(((StoicActivity)getActivity()).getEarliestEntryDate());
    }

    /**
     * Grabs a random quote from the resource file
     * @return String the quote to display
     */
    @NonNull
    private String getQuote() {
        return getQuote(new Random().nextInt(StoicActivity.NUM_QUOTES) + 1);
    }

    /**
     * Grabs a specific quote from the resource file
     * @param quoteId Integer if null, gets a random quote
     * @return String the quote to display
     */
    @NonNull
    private String getQuote(Integer quoteId) {
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
        Long date = Util.getLongVal(year, month + 1, dayOfMonth);
        view.setDate(date);  // Actually update the calendar UI
        updateUI(true);  // Set UI based on data for the day, enable/disable controls as needed
   }

    private void onClickChoice(int choiceId) {
        updateUI(writeSelectedValue(R.id.BUTTON_YES == choiceId));
    }

    private void onClickFeelsSave() {
        Boolean success = ((StoicActivity)getActivity()).writeDayFeels(getCalendarSelectedDate(),
                ((EditText)getView(R.id.EDIT_FEELS)).getText().toString()
        );
        updateUI(success);
    }

    private void onClickFeelsTweet() {
        ((StoicActivity)getActivity()).setNextColorTheme();  // For testing themes
        ((StoicActivity)getActivity()).setNextTextTheme();  // For testing themes
        initializeTheme();
        Log.d("EXPORT_EMAIL", Boolean.toString(((StoicActivity)getActivity()).exportToEmail()));
    }

    /**
     * This retrieves the values for the selected day from the database
     * and updates the UI. This should be called after values have been
     * written to the database.
     * e.g. disabling buttons if there are no more changes allowed
     */
    private void updateUI(Boolean writeSuccessful) {
        String logFormat = "Write %s - Date %s (%s), %s updates";
        Boolean isChoiceEnabled = true;
        Boolean isChoiceSet;

        selectedDayValues = ((StoicActivity)getActivity()).getChoice(getCalendarSelectedDate());
        // Get references to all the controls we'll be updating
        RadioGroup radioGroupChoices = getActivity().findViewById(R.id.GROUP_CHOICES);
        EditText editTextFeels = getActivity().findViewById(R.id.EDIT_FEELS);
        RadioButton buttonYes = getActivity().findViewById(R.id.BUTTON_YES);
        RadioButton buttonNo = getActivity().findViewById(R.id.BUTTON_NO);

        // Clean up the radio button logic here
        // TODO Consider hiding the unselected choice button once the choice is locked
        radioGroupChoices.clearCheck();  // Clear previous selection in case choice not set
        isChoiceSet = selectedDayValues.getAsBoolean(StoicActivity.CHOICE_ISSET);
        if (isChoiceSet) {  // Check the proper choice, also confirm whether we can change it
            isChoiceEnabled = selectedDayValues.getAsBoolean(StoicActivity.CHOICE_ISMUTABLE);
            radioGroupChoices.check(selectedDayValues.getAsBoolean(StoicActivity.COLUMN_CHOICE) ? R.id.BUTTON_YES : R.id.BUTTON_NO);
        }
        for (View child: Util.getAllChildren(radioGroupChoices)) {
            child.setEnabled(isChoiceEnabled);
        }
        buttonYes.setText(
                (isChoiceEnabled
                        ? getTW().choiceTextGood
                        : buttonYes.isChecked()
                            ? getTW().choiceTextDisabledSelected
                            : getTW().choiceTextDisabledUnselected));
        buttonNo.setText(
                (isChoiceEnabled
                        ? ((StoicActivity)getActivity()).themeText.choiceTextBad
                        : buttonNo.isChecked()
                            ? getTW().choiceTextDisabledSelected
                            : getTW().choiceTextDisabledUnselected));

        // Set the other controls to their proper state
        Integer hintResource = isChoiceSet ? R.string.feels_prompt_enabled : R.string.feels_prompt_disabled;
        editTextFeels.setHint(getText(hintResource));
        editTextFeels.setEnabled(isChoiceSet);
        setFeelsText(selectedDayValues.getAsString(StoicActivity.COLUMN_WORDS));

        // Debug output
        String logOutput = String.format(logFormat, writeSuccessful,
                Instant.ofEpochMilli(selectedDayValues.getAsLong(StoicActivity.CHOICE_DATE)),
                selectedDayValues.getAsLong(StoicActivity.CHOICE_DATE),
                selectedDayValues.getAsInteger(StoicActivity.COLUMN_UPDATE_COUNT));
        Log.d("DateSelected", logOutput);
        setDebugText(logOutput);
    }

    /**
     *
     * @return Long the date currently selected in the calendar UI
     */
    @NonNull
    private Long getCalendarSelectedDate() {
        return ((CalendarView)getView(R.id.history)).getDate();
    }
    private void setFeelsText(String text) {
        ((EditText)getView(R.id.EDIT_FEELS)).setText(text);
    }
    private void setDebugText(String text) {
        ((TextView)getView(R.id.TEXT_DEBUG)).setText(text);
    }

    /**
     * Get the current date selected from the UI and write the value to database
     * @param value Boolean value to write
     * @return Boolean success of write
     */
    private Boolean writeSelectedValue(Boolean value) {
        return ((StoicActivity)getActivity()).writeDayValue(getCalendarSelectedDate(), value);
    }

    /**
     * Convenience to declutter some calls above
     * @param id Integer of the control's ID
     * @return View what we got - would be nice if this was the actual inherited type returned
     */
    private View getView(Integer id) {
        return getActivity().findViewById(id);
    }
}
