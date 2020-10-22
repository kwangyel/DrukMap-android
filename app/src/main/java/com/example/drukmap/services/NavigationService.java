package com.example.drukmap.services;

import android.app.Service;
import android.content.Intent;
import android.graphics.Path;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.drukmap.GHAsyncTask;
import com.example.drukmap.OnRouteChange;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.StopWatch;

import java.io.File;
import java.util.Random;

public class NavigationService extends Service {
    public static GraphHopper hopper;
    private volatile boolean prepareInProgress = false;
    private File mapsFolder;
    private String currentArea = "bhutan";
    private final IBinder localBinder = new NavigationBinder();
    private OnRouteChange onRouteChangeListener;
    private boolean shortestPathRunning = false;


    public NavigationService(){}

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
//        return null;
        return localBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this,"Navigation Service started",Toast.LENGTH_SHORT).show();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this,"Navigation Service has exited.",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreate() {
        boolean greaterOrEqKitkat = Build.VERSION.SDK_INT >=19;
        if(greaterOrEqKitkat){
            if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                Log.e("kinley","Graphhopper navigation not usable without a storage");
                return;
            }
            mapsFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"/graphhopper/maps/");
        }else{
            mapsFolder = new File(Environment.getExternalStorageDirectory(),"/graphhopper/maps/");
            if(!mapsFolder.exists()){
                mapsFolder.mkdirs();
            }
        }

        new GHAsyncTask<Void, Void, Path>() {

            protected Path saveDoInBackground(Void... v) {
                GraphHopper tmpHopp = new GraphHopper().forMobile();
                tmpHopp.setProfiles(new Profile("car").setVehicle("car").setWeighting("fastest"));
                tmpHopp.getCHPreparationHandler().setCHProfiles(new CHProfile("car"));
                tmpHopp.load(new File(mapsFolder, currentArea).getAbsolutePath() + "-gh");
                hopper = tmpHopp;
                return null;
            }

            protected void onPostExecute(Path o) {
                if (hasError()) {
                    Log.e("kinley","error creating graph"+getErrorMessage());
                } else {
                    Log.d("kinley","graph created");
                }

                finishPrepare();
            }
        }.execute();
    }

    public boolean calcPath(final double fromlat, final double fromlng, final double tolat, final double tolng){
        if(!shortestPathRunning) {
            shortestPathRunning = true;
            new AsyncTask<Void, Void, ResponsePath>() {
                float time;

                protected ResponsePath doInBackground(Void... v) {
//                    StopWatch sw = new StopWatch().start();
                    GHRequest req = new GHRequest(fromlat, fromlng, tolat, tolng).setProfile("car");
                    req.getHints().putObject(Parameters.Routing.INSTRUCTIONS, true);
                    GHResponse resp = hopper.route(req);

//                DirectionsRoute route = DirectionsRoute.builder().distance(15.2).duration(12.1).geometry(resp.getBest().getPoints().toString()).voiceLanguage("en").build();
//                    Log.e("Kinley", resp.getBest().getInstructions().toString());

//                    time = sw.stop().getSeconds();
                    if (resp.getAll().isEmpty()) {
                        return null;
                    }

                    return resp.getBest();
                }

                protected void onPostExecute(ResponsePath resp) {
                    if (resp == null) {
                        Log.e("kinley", "cannot find path");
                    } else if (!resp.hasErrors()) {
                        onRouteChangeListener.onRouteChange(resp);
                    } else {
                        Log.e("kinley", "error rendering path");
                    }
                    shortestPathRunning = false;
                }
            }.execute();
            return true;
        }else{
            return false;
        }
    }

    public void setOnRouteChangeListener(OnRouteChange onRouteChangeListener) {
        this.onRouteChangeListener = onRouteChangeListener;
    }

    private void finishPrepare() { prepareInProgress = false; }


    public class NavigationBinder extends Binder {
        public NavigationService getService(){
            return NavigationService.this;
        }
    }

}
