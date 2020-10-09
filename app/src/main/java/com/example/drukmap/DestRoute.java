package com.example.drukmap;

import com.graphhopper.ResponsePath;

import org.osmdroid.views.overlay.Polyline;

public class DestRoute {

    private Polyline geometry = null;
    private double distance = 0.0;
    private ResponsePath route = null;
    private Point


    DestRoute(ResponsePath r,double d,Polyline p){
        this.route = r;
        this.distance = d;
        this.geometry = p;
    }

    public Polyline getGeometry(){
        return this.geometry;
    }
    public void setGeometry(Polyline p){
        this.geometry = p;
    }
    public double getDistance(){
        return this.distance;
    }
    public void setDistance(double d){
        this.distance = d;
    }
    public ResponsePath getRoute(){
        return this.route;
    }
    public void setRoute(ResponsePath r){
        this.route = r;
    }
}
