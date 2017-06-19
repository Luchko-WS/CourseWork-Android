package edu.vntu.sacmig.keem_16m;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, View.OnClickListener {

    //Константи для позначення відкритих діалогових вікон
    private final Integer NO_DIALOG = -1;
    private final Integer IS_SAVE_TRACK_DIALOG = 0;

    //Активне діалогове вікно
    private Dialog dialog;
    //Діалогові вікна
    private AlertDialog.Builder saveTrackDialog;

    //БД
    private DBHelper dbHelper;
    private SQLiteDatabase db;

    private GoogleMap mMap;

    private LocationManager locationManager;
    private Location previousLocation;
    private Location startLocation;

    private Marker marker, startMarker, VNTUMarker;
    private Circle startCircle;
    private Polyline polyline;
    private PolylineOptions polylineOptions;

    private Button buttonUsers, buttonTracks, buttonStart;
    private TextView speedText, currentUserText, distanceText, lostEnergyText;

    private EditText nameEditText;

    //Зміні, що служать для збереження інфо з діалогового вікна збереження маршруту
    private Boolean isEditTextChanged = false;
    private String nameText = "";

    //Швидкість користувача
    private Float userSpeed = 0.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        dbHelper = new DBHelper(this);
        db = dbHelper.getWritableDatabase();

        speedText = (TextView) findViewById(R.id.speedText);
        distanceText = (TextView) findViewById(R.id.distanceText);
        currentUserText = (TextView) findViewById(R.id.currentUserText);
        lostEnergyText = (TextView) findViewById(R.id.lostEnergyText);

        lostEnergyText.setText("0 ККал.");
        distanceText.setText("0 м.");
        speedText.setText(userSpeed + " км/год.");
        currentUserText.setText(CurrentUser.name);

        //Додаток щойно завантажився і даних про активного користувача ще не завантажено
        if (CurrentUser.id == -1) {
            //Читаємо дані з файлу (створюється в UserActivity::onPause)
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        openFileInput("options.txt")));

                String[] options = br.readLine().split("\\|");
                CurrentUser.selectedItemID = Integer.valueOf(options[0]);
                CurrentUser.id = Integer.valueOf(options[1]);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //Якщо був активний користувач в попередньому сеансі, то завантажуємо його дані
            if (CurrentUser.id != -1) {

                Cursor user = db.rawQuery("SELECT * FROM users WHERE id=" + CurrentUser.id, null);
                if (user.moveToFirst()) {

                    int idColIndex = user.getColumnIndex("id");
                    int nameColIndex = user.getColumnIndex("name");
                    int weightColIndex = user.getColumnIndex("weight");
                    int lostEnergyColIndex = user.getColumnIndex("lostEnergy");

                    CurrentUser.setUserInformation(
                            user.getInt(idColIndex),
                            user.getString(nameColIndex),
                            user.getFloat(weightColIndex),
                            user.getFloat(lostEnergyColIndex));
                }
            }
        }

        buttonUsers = (Button) findViewById(R.id.buttonUsers);
        buttonUsers.setOnClickListener(this);
        buttonTracks = (Button) findViewById(R.id.buttonTracks);
        buttonTracks.setOnClickListener(this);
        buttonStart = (Button) findViewById(R.id.buttonStart);
        buttonStart.setOnClickListener(this);
        buttonStart.setEnabled(false);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }

    @Override
    public void onResume() {
        super.onResume();

        //Виводимо ім'я активного користувача (коли було обрано іншого користувача)
        currentUserText.setText(CurrentUser.name);

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
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 10, MapsActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(MapsActivity.this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //Виводимо останні відомі координати користувача (маркер)
        if (previousLocation != null) {
            marker = mMap.addMarker(new MarkerOptions().position(
                    new LatLng(previousLocation.getLatitude(), previousLocation.getLongitude())).title("Ви знаходитесь тут"));
        }
        else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(49.233003f, 28.410248f), 17));
            VNTUMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(49.233003f, 28.410248f))
                    .title(getString(R.string.vntu))
                    .snippet(getString(R.string.kafedra)));
        }

        //Коли карта перестворюється під час навігації (наприклад, при повороті екрану)
        //Перемальовуємо маршрут (полігон, стартову точку)
        if (GPSTracks.CurrentTrack.isActive) {
            if (CurrentUser.id != -1) {
                lostEnergyText.setText((float) Math.round(GPSTracks.CurrentTrack.lostEnergy * 10) / 10 + " ККал.");
            }
            distanceText.setText(Math.round(GPSTracks.CurrentTrack.length) + " м.");

            startCircle = mMap.addCircle(new CircleOptions()
                    .center(new LatLng(
                            startLocation.getLatitude(),
                            startLocation.getLongitude()))
                    .radius(1)
                    .strokeColor(Color.BLACK)
                    .fillColor(Color.RED));

            startMarker = mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.starttrack))
                    .title("Початок маршруту")
                    .position(new LatLng(
                            startLocation.getLatitude(),
                            startLocation.getLongitude())));

            polylineOptions = new PolylineOptions()
                    .geodesic(true)
                    .color(Color.RED);

            for (GPSPoint i : GPSTracks.CurrentTrack.points) {
                polylineOptions.add(new LatLng(i.getLatitude(), i.getLongitude()));
            }
            polyline = mMap.addPolyline(polylineOptions);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        //Якщо отримані координати
        if (location != null) {

            //Відбувається запис маршруту
            if (GPSTracks.CurrentTrack.isActive) {

                //Записуємо отримані координати до поточного маршруту
                GPSTracks.CurrentTrack.points.add(new GPSPoint(location.getLatitude(), location.getLongitude()));

                //Додаємо нові координати до полігону
                polylineOptions.add(new LatLng(location.getLatitude(), location.getLongitude()));
                //Якщо існує полігон, то видаляємо
                if (polyline != null) {
                    polyline.remove();
                }
                //Виведення полігону
                polyline = mMap.addPolyline(polylineOptions);

                //Якщо були відомі попередні координати
                if (previousLocation != null) {

                    //Розраховуємо сумарну довжину
                    GPSTracks.CurrentTrack.length += location.distanceTo(previousLocation);

                    //Розраховуємо кількість витраченої енергії
                    //Швидкість бігу (звичайного) = 2 м/с
                    //Формула для бігу виглядає так: E = m * s
                    //Швидкість ходьби = 1 м/с, тому
                    // формула виглядає так: E = 0,5 * m * S
                    if (CurrentUser.id != -1) {
                        GPSTracks.CurrentTrack.lostEnergy += LostEnergyCalculator.calc(
                                location.getSpeed(),
                                CurrentUser.weight,
                                (location.distanceTo(previousLocation) / 1000));

                        lostEnergyText.setText((float) Math.round(GPSTracks.CurrentTrack.lostEnergy * 10) / 10 + " ККал.");
                    }
                    distanceText.setText(Math.round(GPSTracks.CurrentTrack.length) + " м.");
                }
            }

            //Отримуємо швидкість рху користувача
            userSpeed = (float) (Math.round(location.getSpeed() * 10) / 10) * 60 * 60 / 1000;
            speedText.setText(String.valueOf(userSpeed) + " км/год.");

            previousLocation = location;
            buttonStart.setEnabled(true);

            //Видалення існуючого маркера
            if (marker != null) {
                marker.remove();
            }
            //Якщо маркер ще не було створено (координат користувача ще не було), то переміщаємо камеру на нього
            else {
                if(VNTUMarker != null){
                    VNTUMarker.remove();
                }
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
                        location.getLatitude(), location.getLongitude()), 17));
            }

            //Виводимо маркер поточної позиції користувача
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

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            //Натиснута кнопка "Користувачі"
            case R.id.buttonUsers:
                intent = new Intent(MapsActivity.this, UsersActivity.class);
                startActivity(intent);
                break;
            //Натиснута кнопка "Маршрути"
            case R.id.buttonTracks:
                intent = new Intent(MapsActivity.this, TracksActivity.class);
                startActivity(intent);
                break;
            //Натиснути кнопка Запуску/Зупинки маршруту
            case R.id.buttonStart:
                //Старт маршруту
                if (!GPSTracks.CurrentTrack.isActive) {
                    if (previousLocation != null) {

                        //Переміщаємо камеру на початок маршруту
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
                                previousLocation.getLatitude(), previousLocation.getLongitude()), 14));

                        startLocation = previousLocation;

                        //Виводимо точку і маркер початкової точки
                        startCircle = mMap.addCircle(new CircleOptions()
                                .center(new LatLng(
                                        startLocation.getLatitude(),
                                        startLocation.getLongitude()))
                                .radius(1)
                                .strokeColor(Color.BLACK)
                                .fillColor(Color.RED));

                        startMarker = mMap.addMarker(new MarkerOptions()
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.starttrack))
                                .title("Початок маршруту")
                                .position(new LatLng(
                                        startLocation.getLatitude(),
                                        startLocation.getLongitude())));

                        //Створюємо полігон
                        polylineOptions = new PolylineOptions()
                                .geodesic(true)
                                .color(Color.RED);

                        //Обнуляємо дані в об'єкті про поточний маршрут
                        GPSTracks.CurrentTrack.length = 0.0;
                        GPSTracks.CurrentTrack.lostEnergy = 0.0;
                        GPSTracks.CurrentTrack.points.clear();
                        //Запис маршруту увімкнено!!!
                        GPSTracks.CurrentTrack.isActive = true;

                        //Додаємо до полігону початкову точку маршруту
                        GPSTracks.CurrentTrack.points.add(
                                new GPSPoint(startLocation.getLatitude(), startLocation.getLongitude()));
                        polylineOptions.add(new LatLng(startLocation.getLatitude(), startLocation.getLongitude()));
                        if (polyline != null) {
                            polyline.remove();
                        }

                        //Ховаємо зайві кнопки
                        buttonStart.setText("Зупинити маршрут");
                        buttonTracks.setVisibility(View.INVISIBLE);
                        buttonUsers.setVisibility(View.INVISIBLE);
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "Ваша GPS-позиція поки не отримана. Спробуйте, будь ласка, пізніше.",
                                Toast.LENGTH_LONG).show();
                    }
                }
                //Зупинка маршруту
                else {

                    //Якщо маршрут не складається з однієї точки
                    if (GPSTracks.CurrentTrack.length != 0) {
                        //Оновлюємо дані активного користувача
                        if (CurrentUser.id != -1) {
                            db.execSQL("UPDATE users SET lostEnergy=" +
                                    (CurrentUser.lostEnergy + GPSTracks.CurrentTrack.lostEnergy) +
                                    " WHERE id=" + CurrentUser.id);
                        }
                        //Виводимо діалог збереження маршруту
                        showSaveTrackDialog();
                    }

                    //Видаляємо позначення початкової точки
                    startMarker.remove();
                    startCircle.remove();

                    //Видаляємо полігон
                    if (polyline != null) {
                        polyline.remove();
                    }

                    //Запис маршруту вимкнено!!!
                    GPSTracks.CurrentTrack.isActive = false;

                    lostEnergyText.setText("0 ККал.");
                    distanceText.setText("0 м.");
                    buttonStart.setText("Розпочати маршрут");
                    buttonTracks.setVisibility(View.VISIBLE);
                    buttonUsers.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    //Діалог збереження маршруту
    private void showSaveTrackDialog() {

        nameEditText = new EditText(MapsActivity.this);
        nameEditText.setHint("Назва маршруту");

        saveTrackDialog = new AlertDialog.Builder(MapsActivity.this)
                .setTitle("Збереження маршруту")
                .setMessage("Чи бажаєте зберегти маршрут?")
                .setView(nameEditText);

        if (isEditTextChanged) {
            nameEditText.setText(nameText);
            isEditTextChanged = false;
        }

        //Записуємо маршрут
        saveTrackDialog.setPositiveButton("Так", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                ContentValues cv;
                //Додаємо запис про даний маршрут в таблицю маршрутів
                cv = new ContentValues();

                Date currentDate = new Date();
                cv.put("date", formatDateString(currentDate.toString()));
                if (!nameEditText.getText().toString().isEmpty()) {
                    cv.put("name", nameEditText.getText().toString());
                } else {
                    cv.put("name", formatDateString(currentDate.toString()));
                }
                cv.put("points", "tableName");
                cv.put("length", GPSTracks.CurrentTrack.length);
                cv.put("userName", CurrentUser.name);
                cv.put("lostEnergy", GPSTracks.CurrentTrack.lostEnergy);
                db.insert("tracks", null, cv);

                //Створюємо таблицю із точками даного маршруту
                String newTableName = "track";
                Cursor cursor = db.rawQuery("SELECT * FROM tracks WHERE date='" +
                        formatDateString(currentDate.toString()) + "'", null);
                if (cursor.moveToFirst()) {

                    //Вказуємо назву новоствореної таблиці в записі про поточний маршрут
                    int idColIndex = cursor.getColumnIndex("id");
                    newTableName += cursor.getInt(idColIndex);
                    db.execSQL("UPDATE tracks SET points='" + newTableName +
                            "' WHERE id=" + cursor.getInt(idColIndex));
                }

                //Записуємо точки
                db.execSQL("CREATE TABLE " + newTableName + " ("
                        + "id integer primary key autoincrement,"
                        + "latitude real,"
                        + "longitude real" + ");");

                for (GPSPoint i : GPSTracks.CurrentTrack.points) {
                    cv = new ContentValues();
                    cv.put("latitude", i.getLatitude());
                    cv.put("longitude", i.getLongitude());

                    db.insert(newTableName, null, cv);
                }

                Toast.makeText(getApplicationContext(), "Трек " +
                                nameEditText.getText().toString() + " додано!",
                        Toast.LENGTH_SHORT).show();

            }
        });

        saveTrackDialog.setNegativeButton("Ні", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                return;
            }
        });

        saveTrackDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                saveTrackDialog = null;
            }
        });

        dialog = saveTrackDialog.show();
    }

    //Відновлення стану активності
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //Був маркер з поточними координатами користувача. Відновлюємо його
        if (savedInstanceState.getBoolean("isPreviousMarker")) {
            previousLocation = new Location(LOCATION_SERVICE);
            previousLocation.setLatitude(savedInstanceState.getDouble("latitude"));
            previousLocation.setLongitude(savedInstanceState.getDouble("longitude"));
            buttonStart.setEnabled(true);
        }
        //Відновлення маркеру початку маршруту
        if (savedInstanceState.getBoolean("isStartMarker")) {
            startLocation = new Location(LOCATION_SERVICE);
            startLocation.setLatitude(savedInstanceState.getDouble("startLatitude"));
            startLocation.setLongitude(savedInstanceState.getDouble("startLongitude"));
        }
        //Відновлення вигляду активності після запуску маршруту
        if (GPSTracks.CurrentTrack.isActive) {
            buttonStart.setText("Зупинити маршрут");
            buttonTracks.setVisibility(View.INVISIBLE);
            buttonUsers.setVisibility(View.INVISIBLE);
        }

        //Відновлення активного діалогу збереження маршруту
        if (savedInstanceState.getInt("dialog") == IS_SAVE_TRACK_DIALOG) {
            nameText = savedInstanceState.getString("nameText");
            isEditTextChanged = savedInstanceState.getBoolean("isEdited");
            showSaveTrackDialog();
        }
        userSpeed = savedInstanceState.getFloat("speed");

    }

    //збереження стану активності
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Якщо існують поточні координати користувача
        if (previousLocation != null) {
            outState.putDouble("latitude", previousLocation.getLatitude());
            outState.putDouble("longitude", previousLocation.getLongitude());
            outState.putBoolean("isPreviousMarker", true);
            marker.remove();
        } else {
            outState.putBoolean("isPreviousMarker", false);
        }

        //Якщо існують координати початку маршруту
        if (startLocation != null) {
            outState.putDouble("startLatitude", startLocation.getLatitude());
            outState.putDouble("startLongitude", startLocation.getLongitude());
            outState.putBoolean("isStartMarker", true);
            startMarker.remove();
            if (polyline != null) {
                polyline.remove();
            }
        } else {
            outState.putBoolean("isStartMarker", false);
        }

        //Якщо активний діалог збереження маршруту
        if (saveTrackDialog != null) {
            outState.putInt("dialog", IS_SAVE_TRACK_DIALOG);
            outState.putString("nameText", nameEditText.getText().toString());
            outState.putBoolean("isEdited", true);

            dialog.dismiss();
        } else {
            outState.putInt("dialog", NO_DIALOG);
        }
        outState.putFloat("speed", userSpeed);
    }

    //Функція форматування рядку дати і часу в звичний формат
    public static String formatDateString(String dateString) {

        String[] tokens = dateString.split(" ");
        String formatString = "";

        switch (tokens[0]) {
            case "Sun":
                formatString += "Нд ";
                break;
            case "Mon":
                formatString += "Пн ";
                break;
            case "Tue":
                formatString += "Вт ";
                break;
            case "Wed":
                formatString += "Ср ";
                break;
            case "Thu":
                formatString += "Чт ";
                break;
            case "Fri":
                formatString += "Пт ";
                break;
            case "Sat":
                formatString += "Сб ";
                break;
        }

        formatString += tokens[2] + " ";

        switch (tokens[1]) {
            case "Jan":
                formatString += "січня ";
                break;
            case "Feb":
                formatString += "лютого ";
                break;
            case "Mar":
                formatString += "березня ";
                break;
            case "Apr":
                formatString += "квітня ";
                break;
            case "May":
                formatString += "травня ";
                break;
            case "Jun":
                formatString += "червня ";
                break;
            case "Jul":
                formatString += "липня ";
                break;
            case "Aug":
                formatString += "серпня ";
                break;
            case "Sep":
                formatString += "вересня ";
                break;
            case "Oct":
                formatString += "жовтня ";
                break;
            case "Nov":
                formatString += "листопада ";
                break;
            case "Dec":
                formatString += "грудня ";
                break;
        }

        formatString += tokens[5] + " " + tokens[3];
        return formatString;
    }
}