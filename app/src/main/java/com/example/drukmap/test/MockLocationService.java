package com.example.drukmap.test;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.drukmap.OnLocationChange;
import com.example.drukmap.services.NavigationService;
import com.example.drukmap.turf.TurfConstants;
import com.example.drukmap.turf.TurfMeasurement;
import com.graphhopper.ResponsePath;
import com.graphhopper.util.PointList;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class MockLocationService extends Service {
    List<GeoPoint> mockLocations = new ArrayList<>();
    OnLocationChange onLocationChangeListener;
    private final IBinder localBinder = new MockLocationBinder();

    public MockLocationService(){}

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
       return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        super.onCreate();
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
        if(mockLocations.isEmpty()){
            Toast.makeText(this, "No mock locations set", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.e("kinley","this is the modked locations: "+mockLocations.toString());

        Handler mocker = new Handler();
        mocker.post(new Runnable() {
            @Override
            public void run() {
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
        });
    }

    public class MockLocationBinder extends Binder {
        public MockLocationService getService() {
            return MockLocationService.this;
        }
    }

}
