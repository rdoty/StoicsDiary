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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
     *
     * @param v View passing this because we are calling this from onCreateView before it completes
     */
    private void initializeCalendar(View v) {
        CalendarView calendarView = v.findViewById(R.id.history);
        calendarView.setMaxDate(calendarView.getDate());
        Long minDate = ((StoicActivity)getActivity()).getEarliestEntryDate();
        calendarView.setMinDate(minDate * 1000);
    }

    /**
     * Set other UI element behavior, including random text
     * @param v View passing this because we are calling this from onCreateView before it completes
     */
    private void initializeUI(View v) {
        initializeCalendar(v);
        initializeTheme(v);
    }

    /**
     * This includes default colors, text prompts, maybe user-provided text, too.
     * @param v View passing this because we are calling this from onCreateView before it completes
     */
    private void initializeTheme(View v) {
        ContentValues theme = getCurrentTheme();

        TextView prompt = v.findViewById(R.id.TEXT_PROMPT);  // Set prompt to random selection
        prompt.setText(theme.getAsString("prompt"));

        Button buttonYes = v.findViewById(R.id.BUTTON_YES);
        buttonYes.setText(theme.getAsString("text_good"));
        //buttonYes.setBackgroundColor(theme.getAsInteger("color_bg_good"));  // Need to modify tint
        buttonYes.setTextColor(theme.getAsInteger("color_fg_good"));

        Button buttonNo = v.findViewById(R.id.BUTTON_NO);
        buttonNo.setText(theme.getAsString("text_bad"));
        //buttonNo.setBackgroundColor(theme.getAsInteger("color_bg_bad"));  // Need to modify tint
        buttonNo.setTextColor(theme.getAsInteger("color_fg_bad"));

        TextView debugText = v.findViewById(R.id.TEXT_DEBUG);
        debugText.setText(getQuote(null));
    }

    /**
     * Get the final strings associated with each themed resource
     * @return Object All resource strings associated with the theme
     */
    private ContentValues getCurrentTheme() {
        /* Example setting SharedPreferences for future reference
        SharedPreferences.Editor e = ((StoicActivity)getActivity()).sp.edit();
        e.putBoolean("themeId", 1);
        e.apply();
         */
        Integer themeId = ((StoicActivity)getActivity()).sp.getInt("themeId", new Random().nextInt(2) + 1);
        return getCustomTheme(themeId);  // Get ID from preferences
    }

    /**
     *
     * @param themeId int the ID of the theme to get
     * @return Object All resource strings associated with the theme
     */
    private ContentValues getCustomTheme(int themeId) {
        ContentValues theme = new ContentValues();
        theme.put("id", themeId);
        theme.put("name", String.format(Locale.US, "Random Theme #%02d", themeId));
        theme.put("prompt", getText(getResources().getIdentifier(String.format(Locale.US, "theme_%02d_prompt", themeId),"string", BuildConfig.APPLICATION_ID)).toString());
        theme.put("text_good", getText(getResources().getIdentifier(String.format(Locale.US, "theme_%02d_good", themeId),"string", BuildConfig.APPLICATION_ID)).toString());
        theme.put("text_bad", getText(getResources().getIdentifier(String.format(Locale.US, "theme_%02d_bad", themeId),"string", BuildConfig.APPLICATION_ID)).toString());
        theme.put("color_bg_app", getResources().getIdentifier(String.format(Locale.US, "theme_%02d_bg_bad", themeId),"color", BuildConfig.APPLICATION_ID));
        theme.put("color_bg_good", getResources().getIdentifier(String.format(Locale.US, "theme_%02d_bg_good", themeId),"color", BuildConfig.APPLICATION_ID));
        theme.put("color_bg_bad", getResources().getIdentifier(String.format(Locale.US, "theme_%02d_bg_bad", themeId),"color", BuildConfig.APPLICATION_ID));
        theme.put("color_fg_good", getResources().getIdentifier(String.format(Locale.US, "theme_%02d_fg_good", themeId),"color", BuildConfig.APPLICATION_ID));
        theme.put("color_fg_bad", getResources().getIdentifier(String.format(Locale.US, "theme_%02d_fg_bad", themeId),"color", BuildConfig.APPLICATION_ID));
        return theme;
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
                onClickYes(); break;
            case R.id.BUTTON_NO:
                onClickNo(); break;
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
        String logFormat = "%s (%s), %s updates";
        Boolean enableVerdict = true;
        Boolean isVerdictSet;

        ZonedDateTime date = LocalDateTime.of(year, month + 1, dayOfMonth, 0, 0)
                .atZone(ZoneOffset.systemDefault());
        view.setDate(date.toInstant().toEpochMilli());  // Actually update the calendar UI

        // Get references to all the controls we'll be updating
        RadioGroup radioGroupVerdicts = getActivity().findViewById(R.id.VERDICT_CHOICES);
        EditText editTextFeels = getActivity().findViewById(R.id.EDIT_FEELS);

        // Set UI based on data for the day, enable/disable controls as needed
        radioGroupVerdicts.clearCheck();
        ContentValues dayValues = ((StoicActivity)getActivity()).getVerdict(date.toLocalDate().toEpochDay());
        isVerdictSet = dayValues.getAsBoolean("isSet");
        if (isVerdictSet) {
            enableVerdict = dayValues.getAsBoolean("isMutable");
            radioGroupVerdicts.check(dayValues.getAsBoolean(StoicActivity.COLUMN_VERDICT) ? R.id.BUTTON_YES : R.id.BUTTON_NO);
        }
        for (View child: ((StoicActivity)getActivity()).getAllChildren(radioGroupVerdicts)) {
            child.setEnabled(enableVerdict);
        }
        Integer hintResource = isVerdictSet ? R.string.feels_prompt_enabled : R.string.feels_prompt_disabled;
        editTextFeels.setHint(getText(hintResource));
        editTextFeels.setEnabled(isVerdictSet);
        setFeelsText(dayValues.getAsString(StoicActivity.COLUMN_WORDS));

        // Debug output
        String logOutput = String.format(logFormat, date, date.toLocalDate().toEpochDay(), dayValues.getAsInteger(StoicActivity.COLUMN_UPDATE_COUNT));
        Log.d("DateSelected", logOutput);
        setDebugText(logOutput);
    }

    private void onClickYes() {
        setDebugText(writeSelectedValue(true) ? "SET TO TRUE" : "FAILED TO SET");
    }

    private void onClickNo() {
        setDebugText(writeSelectedValue(false) ? "SET TO FALSE" : "FAILED TO SET");
    }

    private void onClickFeelsTweet() {

    }

    /**
     * Note thisGeneratedTheSameLongValueToo = calendarView.getDate() / 86400 / 1000;
     * @return Long the date currently selected in the calendar UI
     */
    private Long getSelectedCalendarDate() {
        CalendarView calendarView = getActivity().findViewById(R.id.history);
        return LocalDate.from(Instant.ofEpochSecond(calendarView.getDate() / 1000)
                .atZone(ZoneOffset.systemDefault())).toEpochDay();
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
