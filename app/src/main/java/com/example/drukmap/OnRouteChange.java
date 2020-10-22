package com.example.drukmap;

import com.graphhopper.ResponsePath;

public interface OnRouteChange {
    void onRouteChange(ResponsePath responsePath);
}
