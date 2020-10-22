package com.example.drukmap;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.drukmap.services.VoiceInstructionService;
import com.example.drukmap.test.MockLocationEngine;
import com.example.drukmap.test.MockLocationService;
import com.graphhopper.ResponsePath;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.PointList;

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
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import java.util.List;
import java.util.Set;

public class NavigationActivity extends AppCompatActivity implements OnLocationChange{

    private MapView mapView;
    private IMapController mapController;
    AlertDialog alertDialog = null;
    MapsForgeTileProvider forge = null;

    MockLocationEngine mockLocationEngine = new MockLocationEngine();
    private final int CURRENT_LOCATION_MARKER_INDEX = 1;



//    MockLocationService mockLocationService;
    VoiceInstructionService voiceInstructionService;

//    boolean isBound = false;
    boolean isVoiceServiceBound = false;


    PathRouteProgress navigationRoute = MainActivity.currentRoute ;
    List<GeoPoint> locationUpdatePoints = new ArrayList<>();
    RouteProcessor routeProcessor = new RouteProcessor();
    Thread thread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        mapView = (MapView) findViewById(R.id.navmapview);
        MapsForgeTileSource.createInstance(this.getApplication());

        Button navigate = findViewById(R.id.navstart);

        //setup mock location engine
        mockLocationEngine.generateMockLocation(navigationRoute.getPath());
        mockLocationEngine.addLocationChangeListener(this);
        thread = new Thread(mockLocationEngine);

        Set<File> mapfiles = findMapFiles();
        File[] maps = new File[mapfiles.size()];
        maps = mapfiles.toArray(maps);


        if (maps == null || maps.length == 0) {
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
        } else {
            XmlRenderTheme theme = null;
            try {
                theme = new AssetsRenderTheme(this.getApplicationContext(), "renderthemes/", "rendertheme-v4.xml");
            } catch (Exception ex) {
                Log.e("kinley", "render theme error", ex);
                ex.printStackTrace();
            }

            MapsForgeTileSource fromFiles = MapsForgeTileSource.createFromFiles(maps, theme, "rendertheme-v4");
            forge = new MapsForgeTileProvider(
                    new SimpleRegisterReceiver(this.getApplication()),
                    fromFiles, null);

//            mapView.setTileProvider(forge);

            OnlineTileSourceBase googleSat = new XYTileSource("google sat tiles",
                    0, 22, 256, ".png", new String[]{
                    "http://mt0.google.com",
                    "http://mt1.google.com",
                    "http://mt2.google.com",
                    "http://mt3.google.com"
            }) {
                @Override
                public String getTileURLString(long pMapTileIndex) {
                    return getBaseUrl() + "/vt/lyrs=s&hl=en&x=" + MapTileIndex.getX(pMapTileIndex) + "&y=" + MapTileIndex.getY(pMapTileIndex) + "&z=" + MapTileIndex.getZoom(pMapTileIndex);
                }
            };
            mapView.setTileSource(googleSat);
            mapView.setMultiTouchControls(true);
            mapView.setBuiltInZoomControls(false);

            mapController = mapView.getController();
            mapController.setCenter(new GeoPoint(27.4712, 89.6339));
            mapController.setZoom(15.0);


            //setup current location marker


            Polyline routePolyline = null;
            if(navigationRoute!= null){
                routePolyline = createPathLayer(navigationRoute.getPath());
                mapView.getOverlays().add(routePolyline);
            }
        }

        navigate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("kinley","Starting the location engin from thread: "+Thread.currentThread().getName());

                thread.start();
//                Log.e("kinley",navigationRoute.getPath().toString());
//                ResponsePath pr = navigationRoute.getPath();
//                mockLocationService.generateMockLocation(pr);
//                mockLocationService.startMockLocationDispatch();
//                simulateNavigation();
            }
        });

    }
    protected void simulateNavigation(){
        Log.e("kinley","these are the location points"+locationUpdatePoints.toString());
        if(locationUpdatePoints.isEmpty()){
            Toast.makeText(this,"Points not defined for simulation",Toast.LENGTH_LONG).show();
            return;
        }
        locationUpdatePoints.remove(0);
        while(!locationUpdatePoints.isEmpty()){
            navigate(locationUpdatePoints.remove(0));
            try{
                Thread.sleep(1000);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }

    }
    protected void navigate(GeoPoint geoPoint){

    }

    protected Set<File> findMapFiles() {
        Set<File> maps = new HashSet<>();
        List<StorageUtils.StorageInfo> storageList = StorageUtils.getStorageList();
        for (int i = 0; i < storageList.size(); i++) {
            File f = new File(storageList.get(i).path + File.separator + "drukmap" + File.separator);
            Log.e("kinley", f.getAbsolutePath());
            if (f.exists()) {
                maps.addAll(scan(f));
            }
        }
        return maps;
    }

    private Collection<? extends File> scan(File f) {
        List<File> res = new ArrayList<>();
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().toLowerCase().endsWith(".map");
            }
        });
        if (files != null) {
            Collections.addAll(res, files);
        }
        return res;
    }

    public Polyline createPathLayer(ResponsePath resp) {
        Polyline pline = new Polyline();
        List<GeoPoint> gp = new ArrayList<>();

        PointList pointList = resp.getPoints();
        for (int i = 0; i < pointList.getSize(); i++) {
            gp.add(new GeoPoint(pointList.getLatitude(i), pointList.getLongitude(i)));
        }
        pline.setPoints(gp);
        return pline;
    }


    @Override
    protected void onStart() {
        super.onStart();
//        Intent intent = new Intent(this, MockLocationService.class);
//        startService(intent);
//        bindService(intent,mockLocationServiceConnection,BIND_AUTO_CREATE);

        Intent voiceServiceIntent = new Intent(this, VoiceInstructionService.class);
        startService(voiceServiceIntent);
        bindService(voiceServiceIntent,voiceInstructionServiceConnection,BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
//        if(isBound){
//            unbindService(mockLocationServiceConnection);
//            isBound = false;
//        }
        if(isVoiceServiceBound){
            unbindService(voiceInstructionServiceConnection);
            isVoiceServiceBound = false;
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
    }

//    private ServiceConnection mockLocationServiceConnection = new ServiceConnection() {
//        @Override
//        public void onServiceConnected(ComponentName name, IBinder service) {
//            MockLocationService.MockLocationBinder binderBridge = (MockLocationService.MockLocationBinder) service;
//            mockLocationService = binderBridge.getService();
//            mockLocationService.addLocationChangeListener(NavigationActivity.this);
//            isBound = true;
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName name) {
//            isBound = false;
//            mockLocationService = null;
//        }
//    };

    private ServiceConnection voiceInstructionServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            VoiceInstructionService.VoiceInstructionBinder binderBridge = (VoiceInstructionService.VoiceInstructionBinder) service;
            voiceInstructionService = binderBridge.getService();
            isVoiceServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isVoiceServiceBound = false;
            voiceInstructionService = null;
        }
    };

    public void updateMarker(GeoPoint newLocation){
        Marker newMarker = new Marker(mapView);
        if(mapView.getOverlays().size() >= 2){
            mapView.getOverlayManager().remove(CURRENT_LOCATION_MARKER_INDEX);
        }
        newMarker.setPosition(newLocation);
        mapView.getOverlayManager().add(CURRENT_LOCATION_MARKER_INDEX,newMarker);
        mapView.invalidate();
    }

    @Override
    public void OnLocationChange(GeoPoint newLocation) {
        runOnUiThread(new Runnable() {
            public void run() {
                updateMarker(newLocation);
            }
        });
        PathRouteProgress updatedRoute = routeProcessor.buildNewRouteProgress(newLocation,navigationRoute);
//        Log.e("kinley","the step is distance remainign" + navigationRoute.getStepDistanceRemaining());
//        Log.e("kinley","the step is " + navigationRoute.getLegIndex());
        if(updatedRoute.getStepDistanceRemaining() < 50){
            int currentLegIndex = updatedRoute.getLegIndex();
            if( currentLegIndex == (updatedRoute.getLegs() - 1)){
                if(updatedRoute.getPath().getInstructions().get(currentLegIndex).getSign() == 4){
                    Log.e("kinley","you have arrived");
                    voiceInstructionService.arrival();
                }
            }
            Instruction nextManeuver = updatedRoute.getPath().getInstructions().get(currentLegIndex + 1);
            int direction = nextManeuver.getSign();
            switch (direction){
                case -1: case -2: case -3:
                    Log.e("kinley","Turn left");
                    voiceInstructionService.turnLeft();
                    break;
                case 1: case 2: case 3:
                    Log.e("kinley","Turn right");
                    voiceInstructionService.turnRight();
                    break;
                case 0:
                    voiceInstructionService.straight();
                    Log.e("kinley","Go straight");
                    break;
                default:
                    Toast.makeText(this, "Unknokwn direction", Toast.LENGTH_SHORT).show();
                    break;
            }
        }

//        Log.e("kinley","Location updated: "+ newLocation.toDoubleString());
    }
}
