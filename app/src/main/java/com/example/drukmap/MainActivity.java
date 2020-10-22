package com.example.drukmap;

import android.Manifest;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Path;
import android.icu.util.ICUUncheckedIOException;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.drukmap.services.NavigationService;
import com.example.drukmap.test.MockLocationEngine;
import com.google.android.material.shape.ShapePath;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.PointList;
import com.graphhopper.util.StopWatch;

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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements OnRouteChange{
    private MapView mapView;
    private IMapController mapController;
    AlertDialog alertDialog = null;
    MapsForgeTileProvider forge = null;
    public static GraphHopper hopper;
    private volatile boolean prepareInProgress = false;
    private Polyline pathLayer = null;
    private volatile boolean shortestPathRunning = false;
    private String currentArea = "bhutan";
    public static PathRouteProgress currentRoute;
    NavigationService navigationService;
    boolean isBound = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapView = (MapView) findViewById(R.id.mapview);

        MapsForgeTileSource.createInstance(this.getApplication());
        ImageButton imgbtn = (ImageButton)findViewById(R.id.imageButton);
        Button getRoute = findViewById(R.id.getRoute);
        Button navigate = findViewById(R.id.navigate);
        requestPermission();

        Set<File> mapfiles = findMapFiles();
        File[] maps = new File[mapfiles.size()];
        maps = mapfiles.toArray(maps);

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
            mapController.setZoom(15.0);
            mapController.setCenter(new GeoPoint(27.4712, 89.6339));
        }

        navigate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentRoute !=null){
                    Intent intent = new Intent(MainActivity.this,NavigationActivity.class);
                    startActivity(intent);
                }else{
                    Toast.makeText(MainActivity.this, "Route not selected", Toast.LENGTH_SHORT).show();
                }
            }
        });
        getRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigationService.calcPath(27.475302, 89.636357,27.429945, 89.646892);
            }
        });
        imgbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapView.setTileProvider(forge);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, NavigationService.class);
        startService(intent);
        bindService(intent, navigationServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(isBound){
            unbindService(navigationServiceConnection);
            isBound = false;
        }
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


    private ServiceConnection navigationServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            NavigationService.NavigationBinder binderBrdige = (NavigationService.NavigationBinder) service;
            navigationService = binderBrdige.getService();
            navigationService.setOnRouteChangeListener(MainActivity.this);
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            navigationService = null;

        }
    };

    @Override
    public void onRouteChange(ResponsePath responsePath) {

        PathRouteProgress newroute = new PathRouteProgress(responsePath,responsePath.getDistance());
        this.currentRoute = newroute;

        if(!mapView.getOverlays().isEmpty()){
            mapView.getOverlays().remove(0);
        }
        Polyline route = createPathLayer(responsePath);

        route.getOutlinePaint().setColor(Color.rgb(66, 147, 245));
        route.getOutlinePaint().setStrokeWidth(15.0f);
        mapView.getOverlays().add(0,route);
        mapView.invalidate();
    }
}