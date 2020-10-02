package com.example.drukmap;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.icu.util.Output;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

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

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapView = (MapView) findViewById(R.id.mapview);
        MapsForgeTileSource.createInstance(this.getApplication());
        ImageButton imgbtn = (ImageButton)findViewById(R.id.imageButton);

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
            mapController.setZoom(8.0);
            mapController.setCenter(new GeoPoint(27.5142, 90.4336));
        }


        imgbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapView.setTileProvider(forge);
            }
        });
    }
    protected void copyToStorage(Context context){
        AssetManager assetManager = context.getAssets();
        String path = Environment.getExternalStorageDirectory()+"/drukmap/";
        try{
            InputStream in = assetManager.open("bhutan.map");
            OutputStream out = new FileOutputStream(path+"bhutan.map");
            byte[] buffer = new byte[1024];
            int read = in.read(buffer);
            while (read != -1){
                out.write(buffer,0,read);
                read = in.read(buffer);
            }
        }catch (Exception e){
            e.printStackTrace();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}