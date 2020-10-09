package com.example.drukmap;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.Trip;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.PointList;
import com.graphhopper.util.StopWatch;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import org.mapsforge.map.android.rendertheme.AssetsRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.mapsforge.MapsForgeTileProvider;
import org.osmdroid.mapsforge.MapsForgeTileSource;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.tileprovider.util.StorageUtils;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;



import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private MapView mapView;
    private IMapController mapController;
    AlertDialog alertDialog = null;
    MapsForgeTileProvider forge = null;
    private GraphHopper hopper;
    private volatile boolean prepareInProgress = false;
    private Polyline pathLayer = null;
    private volatile boolean shortestPathRunning = false;
    private File mapsFolder;
    private String currentArea = "bhutan";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapView = (MapView) findViewById(R.id.mapview);
        MapsForgeTileSource.createInstance(this.getApplication());
        ImageButton imgbtn = (ImageButton)findViewById(R.id.imageButton);
        Button getRoute = findViewById(R.id.getRoute);
        requestPermission();

        Set<File> mapfiles = findMapFiles();
        File[] maps = new File[mapfiles.size()];
        maps = mapfiles.toArray(maps);
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


        if(maps == null || maps.length == 0){
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                    this);

            // set title
            alertDialogBuilder.setTitle("No Mapsforge files found");

            // set dialog message
            alertDialogBuilder
                    .setMessage("In order to render map tiles, you'll need to either create or obtain mapsforge .map files. See https://github.com/mapsforge/mapsforge for more info. Store them in "
                            + Configuration.getInstance().getOsmdroidBasePath().getAbsolutePath())
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (alertDialog != null) alertDialog.dismiss();
                        }
                    });

            // create alert dialog
            alertDialog = alertDialogBuilder.create();

            // show it
            alertDialog.show();
        }else{
            XmlRenderTheme theme = null;
            try {
                theme = new AssetsRenderTheme(this.getApplicationContext() , "renderthemes/", "rendertheme-v4.xml");
            } catch (Exception ex) {
                Log.e("kinley","render theme error",ex);
                ex.printStackTrace();
            }

            MapsForgeTileSource fromFiles = MapsForgeTileSource.createFromFiles(maps, theme, "rendertheme-v4");
            forge = new MapsForgeTileProvider(
                    new SimpleRegisterReceiver(this.getApplication()),
                    fromFiles, null);

//            mapView.setTileProvider(forge);

            OnlineTileSourceBase googleSat = new XYTileSource("google sat tiles",
                    0,22,256,".png",new String[]{
                    "http://mt0.google.com",
                    "http://mt1.google.com",
                    "http://mt2.google.com",
                    "http://mt3.google.com"
            }){
                @Override
                public String getTileURLString(long pMapTileIndex) {
                    return getBaseUrl() + "/vt/lyrs=s&hl=en&x=" + MapTileIndex.getX(pMapTileIndex) + "&y=" + MapTileIndex.getY(pMapTileIndex) + "&z=" + MapTileIndex.getZoom(pMapTileIndex);
                }
            };
            mapView.setTileSource(googleSat);
            mapView.setMultiTouchControls(true);
            mapView.setBuiltInZoomControls(false);

            mapController = mapView.getController();
            mapController.setZoom(8.0);
            mapController.setCenter(new GeoPoint(27.5142, 90.4336));
        }
        loadGraphStorage();

        getRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!isReady()){
                    Log.e("kinley","map not ready");
                    return;
                }
                if(shortestPathRunning){
                    Log.e("kinley","Calculation in progress");
                    return;
                }else{
                    Log.e("kinley","caludating path");
                    calcPath(27.475302, 89.636357,27.429945, 89.646892);
                }
            }
        });
        imgbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapView.setTileProvider(forge);
            }
        });
    }

    private void requestPermission(){
        String [] reqPermission = new String [] {Manifest.permission.READ_EXTERNAL_STORAGE};
        int request_code = 2;
        if(ContextCompat.checkSelfPermission(MainActivity.this,reqPermission[0]) == PackageManager.PERMISSION_GRANTED){
            //do nothing
        }else{
            ActivityCompat.requestPermissions(MainActivity.this,reqPermission,request_code);
        }
    }

    boolean isReady() {
        // only return true if already loaded
        if (hopper != null)
            return true;
        if (prepareInProgress) {
            Log.e("kinley","Preparation still in progress");
            return false;
        }
        Log.e("kinley","Prepare finished but GraphHopper not ready. This happens when there was an error while loading the files");
        return false;
    }

    protected Set<File> findMapFiles(){
        Set<File> maps = new HashSet<>();
        List<StorageUtils.StorageInfo> storageList = StorageUtils.getStorageList();
        for (int i = 0; i < storageList.size(); i++){
            File f = new File(storageList.get(i).path + File.separator + "drukmap" + File.separator);
            Log.e("kinley",f.getAbsolutePath());
            if (f.exists()){
                maps.addAll(scan(f));
            }
        }
        return maps;
    }

    private Collection<? extends File> scan(File f){
        List<File> res = new ArrayList<>();
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().toLowerCase().endsWith(".map");
            }
        });
        if(files != null){
            Collections.addAll(res,files);
        }
        return res;
    }

    void loadGraphStorage(){
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

    private void finishPrepare(){
        prepareInProgress = false;
    }

    public Polyline createPathLayer(ResponsePath resp){
        Polyline pline = new Polyline();
        List<GeoPoint> gp = new ArrayList<>();

        PointList pointList = resp.getPoints();
        for (int i =0;i<pointList.getSize(); i++){
            gp.add(new GeoPoint(pointList.getLatitude(i),pointList.getLongitude(i)));
        }
        pline.setPoints(gp);
        return pline;
    }

    public void calcPath(final double fromlat, final double fromlng, final double tolat, final double tolng){
        new AsyncTask<Void, Void, ResponsePath>(){
            float time;
            protected ResponsePath doInBackground(Void... v){
                StopWatch sw = new StopWatch().start();
                GHRequest req = new GHRequest(fromlat,fromlng,tolat,tolng).setProfile("car");
                req.getHints().putObject(Parameters.Routing.INSTRUCTIONS,true);
                GHResponse resp = hopper.route(req);

//                DirectionsRoute route = DirectionsRoute.builder().distance(15.2).duration(12.1).geometry(resp.getBest().getPoints().toString()).voiceLanguage("en").build();
                Log.e("Kinley",resp.getBest().getInstructions().toString());

                time = sw.stop().getSeconds();
                if(resp.getAll().isEmpty()){
                    return null;
                }
                return resp.getBest();
            }

            protected void onPostExecute(ResponsePath resp){
                if(resp == null){
                    Log.e("kinley","cannot find path");
                }else if (!resp.hasErrors()){
                    pathLayer = createPathLayer(resp);
                    mapView.getOverlayManager().add(pathLayer);
                    mapView.invalidate();
                }else{
                    Log.e("kinley","error rendering path");
                }
                shortestPathRunning = false;
            }
        }.execute();
    }

}