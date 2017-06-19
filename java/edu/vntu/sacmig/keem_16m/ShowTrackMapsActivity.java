package edu.vntu.sacmig.keem_16m;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class ShowTrackMapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private LocationManager locationManager;

    private Marker marker;
    private GoogleMap mMap;
    private Location previousLocation;

    private TextView nameText, dateText, lengthText, userText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_track_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        nameText = (TextView) findViewById(R.id.nameText);
        nameText.setText("Назва треку: " + GPSTracks.LoadedTrack.name.toString());

        dateText= (TextView) findViewById(R.id.dateText);
        dateText.setText("Дата створення: " + GPSTracks.LoadedTrack.date.toString());

        lengthText = (TextView) findViewById(R.id.lengthText);
        lengthText.setText("Довжина: " + Math.round(GPSTracks.LoadedTrack.length * 100)/100 + " м.");

        userText = (TextView) findViewById(R.id.userText);
        userText.setText("Користувач: " + GPSTracks.LoadedTrack.user.toString());

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 5,  MapsActivity.this);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 15, ShowTrackMapsActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(ShowTrackMapsActivity.this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //Якщо були раніше отримані координати, то виводимо по них маркер
        if(previousLocation != null) {
            marker = mMap.addMarker(new MarkerOptions().position(
                    new LatLng(
                            previousLocation.getLatitude(),
                            previousLocation.getLongitude()))
                    .title("Ви знаходитесь тут"));
        }

        //Будуємо полігон маршруту та виводимо на карту
        PolylineOptions polylineOptions = new PolylineOptions()
                .geodesic(true)
                .color(Color.BLUE);

        for(GPSPoint i : GPSTracks.LoadedTrack.points){
            polylineOptions.add(new LatLng(i.getLatitude(), i.getLongitude()));
        }
        mMap.addPolyline(polylineOptions);

        //Виводимо на карту маркер і точку початку маршруту
        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.starttrack))
                .title("Початок маршруту")
                .position(new LatLng(
                        GPSTracks.LoadedTrack.points.get(0).getLatitude(),
                        GPSTracks.LoadedTrack.points.get(0).getLongitude())));

        mMap.addCircle(new CircleOptions()
                .center(new LatLng(
                        GPSTracks.LoadedTrack.points.get(0).getLatitude(),
                        GPSTracks.LoadedTrack.points.get(0).getLongitude()))
                .radius(1)
                .strokeColor(Color.BLACK)
                .fillColor(Color.BLUE));

        //Виводимо на карту маркер і точку кінця маршруту
        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.endtrack))
                .title("Кінець маршруту")
                .position(new LatLng(
                        GPSTracks.LoadedTrack.points.get(GPSTracks.LoadedTrack.points.size()-1).getLatitude(),
                        GPSTracks.LoadedTrack.points.get(GPSTracks.LoadedTrack.points.size()-1).getLongitude())));

        mMap.addCircle(new CircleOptions()
                .center(new LatLng(
                        GPSTracks.LoadedTrack.points.get(GPSTracks.LoadedTrack.points.size()-1).getLatitude(),
                        GPSTracks.LoadedTrack.points.get(GPSTracks.LoadedTrack.points.size()-1).getLongitude()))
                .radius(1)
                .strokeColor(Color.BLACK)
                .fillColor(Color.BLUE));

        //Переміщаємо камеру на початок маршруту
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
                GPSTracks.LoadedTrack.points.get(0).getLatitude(),
                GPSTracks.LoadedTrack.points.get(0).getLongitude()), 15));

    }

    @Override
    public void onLocationChanged(Location location) {
        //Якщо отримали координати із точністю 5 м
        if (location != null || location.getAccuracy() < 5) {
            //Видаляємо існуючий маркер
            if (marker != null) {
                marker.remove();
            }
            //Виводимо маркер
            marker = mMap.addMarker(new MarkerOptions().position(
                    new LatLng(location.getLatitude(), location.getLongitude())).title("Ви знаходитесь тут"));
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    //Відновлення попереднього стану активності
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //Якщо була збережена попередня координата
        if(savedInstanceState.getBoolean("isMarker")) {
            previousLocation = new Location(LOCATION_SERVICE);
            previousLocation.setLatitude(savedInstanceState.getDouble("latitude"));
            previousLocation.setLongitude(savedInstanceState.getDouble("longitude"));
        }
        else{
            previousLocation = null;
        }
    }

    //Збереження стану активності
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Якщо отримали координату, то зберігаємо її
        if (marker != null) {
            outState.putDouble("latitude", marker.getPosition().latitude);
            outState.putDouble("longitude", marker.getPosition().longitude);
            outState.putBoolean("isMarker", true);
        }
        else{
            outState.putBoolean("isMarker", false);
        }
    }
}