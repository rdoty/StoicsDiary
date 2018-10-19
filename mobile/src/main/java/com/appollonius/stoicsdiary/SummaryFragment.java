package com.appollonius.stoicsdiary;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SummaryFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SummaryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SummaryFragment extends androidx.fragment.app.ListFragment {
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
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
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
     * @return A new instance of fragment SummaryFragment.
     */
    // TODO: Rename and change types and number of parameters
    static SummaryFragment newInstance(String param1) {
        SummaryFragment fragment = new SummaryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);  // TODO: Rename and change types and number of parameters
        fragment.setArguments(args);
        return fragment;
    }

    private StoicActivity mA;  // rdoty - helps to call StoicActivity instance without null compiler warnings
    private StatsAdapter adapter;

    public SummaryFragment() { }  // Required empty public constructor

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
        View rootView = inflater.inflate(R.layout.fragment_summary, container,false);

        // Construct the data source
        ArrayList<StoicActivity.Datastore.UserStatistics.Statistic> statsArray = new ArrayList<>();
        adapter = new StatsAdapter(getContext(), statsArray);  // to convert the array to views

        ListView listView = rootView.findViewById(android.R.id.list);
        listView.setAdapter(adapter);  // Attach the adapter to a ListView

        updateStatList();
        return rootView;
    }

    public void onButtonPressed(Uri uri) {  // TODO: Rename method, update argument and hook into UI
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
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

    @Override
    public void onResume() {
        super.onResume();
        updateStatList();
    }

    void updateStatList() {
        adapter.clear();
        adapter.addAll(mA.ds.us.getStatsList());
    }

    @Override
    public void onListItemClick(ListView l, View v, int pos, long id) {
        super.onListItemClick(l, v, pos, id);
        Toast.makeText(getActivity(), "Item " + pos + " was clicked", Toast.LENGTH_SHORT).show();
    }

    public class StatsAdapter extends ArrayAdapter<StoicActivity.Datastore.UserStatistics.Statistic> {
        StatsAdapter(Context context, ArrayList<StoicActivity.Datastore.UserStatistics.Statistic> stats) {
            super(context, 0, stats);
        }

        @Override
        public int getItemViewType(int position) {
            return 0; // To support heterogeneous items e.g. getItem(position).color.ordinal();
        }

        // Total number of types is the number of enum values
        @Override
        public int getViewTypeCount() {
            return 1; // To support heterogeneous items e.g. SimpleColor.ColorValues.values().length;
        }

        @Override @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            // Get the data item for this position
            StoicActivity.Datastore.UserStatistics.Statistic stat = getItem(position);

            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_summary_item, parent, false);
            }
            // Lookup view for data population
            TextView statTitle = convertView.findViewById(R.id.statTitle);
            TextView statValue = convertView.findViewById(R.id.statValue);

            if (stat != null) {  // Populate the data into the template view using the data object
                statTitle.setText(stat.title);
                statValue.setText(stat.value);
                // #156601325 Style items
                statTitle.setTextColor(StoicActivity.themeColors.colorStatTitle);
                statValue.setTextColor(StoicActivity.themeColors.colorStatValue);
            }
            return convertView;  // Return the completed view to render on screen
        }
    }
}
