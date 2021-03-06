package com.maddox.navigon;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ZoomControls;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.maddox.navigon.MapView;
import com.maddox.navigon.MapViewLocationListener;
import com.maddox.navigon.PointD;
import com.maddox.navigon.TilesProvider;


public class MapAppActivity extends Activity{

    private GPSTracker gpsTracker;

    private final class Save{
        public final static String GPS_LON = "gpsLon";
        public final static String GPS_LAT = "gpsLAT";
    }

    //do zachowania właściwości aby przy akcji on restore
    private final class Pref{
        public final static String SEEK_LON = "seek_lon";
        public final static String SEEK_LAT = "seek_lat";
        public final static String ZOOM = "zoom";
    }

    MapView mapView;
    TilesProvider tilesProvider;

    MapViewLocationListener locationListener;

    Location savedGpsLocation;

    ZoomControls zoomControls;

    //zoom +
    View.OnClickListener zoomIn_Click = new View.OnClickListener(){
        @Override
        public void onClick(View v){
            mapView.zoomIn();
        }
    };

    //zoom -
    View.OnClickListener zoomOut_Click = new View.OnClickListener(){
        @Override
        public void onClick(View v){
            mapView.zoomOut();
        }
    };

    @Override
    protected void onResume(){
        setContentView(R.layout.main);

        mapView = (MapView) findViewById(R.id.map);
        zoomControls = (ZoomControls) findViewById(R.id.zoom);

        initViews();
        restoreMapViewSettings();
        super.onResume();
    }

    void initViews(){
        //Pobranie pliku z baza danych mapy
//        String path = Environment.getExternalStorageDirectory() + "/mapapp/Lol.sqlitedb";
        String path = "/storage/sdcard1/mapapp/Lol.sqlitedb";
        tilesProvider = new TilesProvider(path);

        if (savedGpsLocation != null) mapView.setGpsLocation(gpsTracker.getLongitude(), gpsTracker.getLongitude());

        mapView.setTilesProvider(tilesProvider);

        mapView.refresh();
        zoomControls.setOnZoomInClickListener(zoomIn_Click);
        zoomControls.setOnZoomOutClickListener(zoomOut_Click);
    }

    @Override
    protected void onPause(){
        saveMapViewSettings();

        //locationListener.stop();

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.removeUpdates(locationListener);

        tilesProvider.close();
        tilesProvider.clear();

        mapView = null;

        super.onPause();
    }

    //Akcje klawiszowe
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        // Zoom +
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_Z){
            mapView.zoomIn();
            return true;
        }
        // Zoom -
        else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_X){
            mapView.zoomOut();
            return true;
        }
        // Centrowanie na marker
        if (keyCode == KeyEvent.KEYCODE_H || keyCode == KeyEvent.KEYCODE_FOCUS) {
            mapView.followMarker();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    // odtwarzanie stanu mapy
    void restoreMapViewSettings(){
        SharedPreferences pref = getSharedPreferences("View_Settings", MODE_PRIVATE);

        double lon, lat;
        int zoom;

        gpsTracker = new GPSTracker(getBaseContext());
        mapView.setGpsLocation(gpsTracker.getLongitude(), gpsTracker.getLatitude());
        mapView.invalidate();
        mapView.followMarker();

        mapView.refresh();
    }

    // zapisywanie stanu mapy
    void saveMapViewSettings(){
        SharedPreferences.Editor editor = getSharedPreferences("View_Settings", MODE_PRIVATE).edit();

        PointD seekLocation = mapView.getSeekLocation();
        editor.putString(Pref.SEEK_LON, Double.toString(seekLocation.x));
        editor.putString(Pref.SEEK_LAT, Double.toString(seekLocation.y));
        editor.putInt(Pref.ZOOM, mapView.getZoom());

        editor.commit();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState){
        if (mapView.getGpsLocation() != null){
            outState.putDouble(Save.GPS_LON, mapView.getGpsLocation().getLongitude());
            outState.putDouble(Save.GPS_LAT, mapView.getGpsLocation().getLatitude());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState){
        double gpsLon, gpsLat, gpsAlt;
        float gpsAcc;

        gpsLon = savedInstanceState.getDouble(Save.GPS_LON, 999);
        gpsLat = savedInstanceState.getDouble(Save.GPS_LAT, 999);

        if (gpsLon != 999 && gpsLat != 999){
            savedGpsLocation.setLongitude(gpsTracker.getLongitude());
            savedGpsLocation.setLatitude(gpsTracker.getLatitude());
        }

        super.onRestoreInstanceState(savedInstanceState);
    }
}