package com.example.drukmap.test;

import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.example.drukmap.OnLocationChange;
import com.example.drukmap.turf.TurfConstants;
import com.example.drukmap.turf.TurfMeasurement;
import com.graphhopper.ResponsePath;
import com.graphhopper.util.PointList;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class MockLocationEngine implements Runnable{
    List<GeoPoint> mockLocations = new ArrayList<>();
    OnLocationChange onLocationChangeListener = null;

    @Override
    public void run() {
        Log.e("kinley","Running mock location updates in thread: "+Thread.currentThread().getName());
        if(mockLocations.isEmpty()){
            Log.e("kinley","mocked locations cannot be empty whil starting mock routing");
            return;
        }
        if(onLocationChangeListener == null){
            Log.e("kinley","Need to initialize onLocationChangeListener");
            return;
        }
        Log.e("kinley","this is the modked locations: "+mockLocations.toString());
        startMockLocationDispatch();
    }

    public void generateMockLocation(ResponsePath path){
        Log.e("kinley",path.toString());
        PointList pts = path.getPoints();
        double totalDistance = path.getDistance(); //in meters


        for(double i = 0; i < totalDistance; i = i +13){
            mockLocations.add(TurfMeasurement.along(pts,i, TurfConstants.UNIT_METERS));
        }
    }


    public void addLocationChangeListener(OnLocationChange onLocationChangeListener){
        this.onLocationChangeListener = onLocationChangeListener;
    }

    public void startMockLocationDispatch(){
        int i =0;
        for (GeoPoint locations : mockLocations){
            onLocationChangeListener.OnLocationChange(locations);
            Log.e("kinley",Integer.toString(i));
            i++;
            try{
                Thread.sleep(1000);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }
}
