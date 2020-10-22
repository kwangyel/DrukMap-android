package com.example.drukmap;

import com.graphhopper.ResponsePath;

import org.osmdroid.util.GeoPoint;

public interface OnLocationChange {
    void OnLocationChange(GeoPoint location);
}

