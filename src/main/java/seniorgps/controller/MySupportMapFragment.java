package seniorgps.controller;

import com.google.android.gms.maps.SupportMapFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MySupportMapFragment extends SupportMapFragment {
    public View mOriginalContentView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        mOriginalContentView = super.onCreateView(inflater, parent, savedInstanceState);
        return mOriginalContentView;
    }

    @Override
    public View getView() {
        return mOriginalContentView;
    }
}