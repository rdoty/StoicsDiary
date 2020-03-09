package com.appollonius.stoicsdiary;

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import java.util.ArrayList;
import java.util.Objects;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ChoiceFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ChoiceFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChoiceFragment extends androidx.fragment.app.Fragment implements View.OnClickListener {
    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     * NOTE: Update argument type and name for onFragmentInteraction as desired
     */
    interface OnFragmentInteractionListener {
    }
    private OnFragmentInteractionListener mListener;

    // TODO: Rename fragment initialization parameter argument(s) change types, choose names that match.
    private static final String ARG_PARAM1 = "param1";
    private String mParam1;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @return A new instance of fragment ChoiceFragment.
     */
    static ChoiceFragment newInstance(String param1) {
        ChoiceFragment fragment = new ChoiceFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);  // TODO: Rename and change types and number of parameters
        fragment.setArguments(args);
        return fragment;
    }

    private StoicActivity mA;  // rdoty - helps to call StoicActivity instance without null compiler warnings
    public ChoiceFragment() { }  // Required empty public constructor

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
        return inflater.inflate(R.layout.fragment_choice, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        bindEventHandlers();
    }

    /**
     * Initialization of all UI elements on startup
     * Note: Android Activity/Fragment event handling sucks
     */
    private void bindEventHandlers() {
        ArrayList<View> children = StoicActivity.Util.getAllChildren(getView());
        for (View child: children) {
            if (child instanceof RadioButton) {
                child.setOnClickListener(this);
            }
        }
    }

    /**
     * @param choiceId int The id of the choice button selected
     */
    private void onClickChoice(int choiceId) {
        mA.ds.writeDayValue(mA.getCurrentDay(),R.id.BUTTON_YES == choiceId);
        mA.updateUI(Objects.requireNonNull(getView()));
        mA.viewPager.setCurrentItem(1, true);  // Switch to History tab
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.BUTTON_YES:
            case R.id.BUTTON_NO:
                onClickChoice(v.getId()); break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mA.updateUI(Objects.requireNonNull(getView()));
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

}
