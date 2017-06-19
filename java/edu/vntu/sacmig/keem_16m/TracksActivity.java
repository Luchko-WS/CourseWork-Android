package edu.vntu.sacmig.keem_16m;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class TracksActivity extends AppCompatActivity {

    //Константи для позначення відкритих діалогових вікон
    private final Integer NO_DIALOG = -1;
    private final Integer IS_OPERATION_LIST_DIALOG = 0;
    private final Integer IS_INFO_DIALOG = 1;
    private final Integer IS_REMOVE_ITEM_DIALOG = 2;

    //Активне діалогове вікно
    private Dialog dialog;
    //Діалогові вікна
    private AlertDialog.Builder operationListDialog;
    private AlertDialog.Builder infoDialog;
    private AlertDialog.Builder removeItemDialog;

    //БД
    private DBHelper dbHelper;
    private SQLiteDatabase db;

    //Список ідентифікаторів маршрутів в БД
    private List<Integer> tracksID;
    private Integer chosenListItemPosition;
    //Адаптер для ліствью
    private List<String> rows = new ArrayList<>();
    private ArrayAdapter adapter;

    private ListView listViewTracks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracks);

        dbHelper = new DBHelper(this);
        db = dbHelper.getWritableDatabase();

        //Зчитування маршрутів з БД
        tracksID = new ArrayList<>();
        listViewTracks = (ListView) findViewById(R.id.trackList);

        Cursor c = db.query("tracks", null, null, null, null, null, null);
        //Якщо є дані, то зчитуємо
        if (c.moveToFirst()) {

            int idColIndex = c.getColumnIndex("id");
            int nameColIndex = c.getColumnIndex("name");
            int userColIndex = c.getColumnIndex("userName");

            do {
                rows.add(c.getString(nameColIndex) + " (" + c.getString(userColIndex) + ")");
                tracksID.add(c.getInt(idColIndex));
            } while (c.moveToNext());

            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows);
            listViewTracks.setAdapter(adapter);
        }

        listViewTracks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Показати інформацію про маршрут
                showDialogAboutTrackFromList(position);
            }
        });

        listViewTracks.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                //Показати діалог зі списком операцій
                showDialogOperationList(position);
                return true;
            }
        });
    }

    //Діалог зі списком операцій
    private void showDialogOperationList(final Integer chosenListItem){

        //Отримуємо позицію в списку активного елемента
        chosenListItemPosition = chosenListItem;
        final String[] array = {"Показати інформацію", "Показати на мапі...", "Видалити"};

        //Створення діалогу вибору операцій
        operationListDialog = new AlertDialog.Builder(TracksActivity.this);
        operationListDialog.setTitle("Операції над записом");

        operationListDialog.setItems(array, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                switch (item){
                    case 0:
                        //Показати інформацію про маршрут
                        showDialogAboutTrackFromList(chosenListItem);
                        break;
                    case 1:
                        //Показати маршрут на карті
                        loadGPSTrack(tracksID.get(chosenListItem));
                        Intent intent = new Intent(TracksActivity.this, ShowTrackMapsActivity.class);
                        startActivity(intent);
                        break;
                    case 2:
                        //Видалення маршруту
                        showDialogRemoveTrackFromList(chosenListItem);
                        break;
                }
            }
        });

        operationListDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                operationListDialog = null;
            }
        });

        dialog = operationListDialog.show();
    }

    //Показати інформаію про маршрут
    private void showDialogAboutTrackFromList(final Integer chosenListItem) {

        chosenListItemPosition = chosenListItem;
        final Integer chosenTrackID = tracksID.get(chosenListItem);

        //Зчитуємо з БД
        Cursor track = db.rawQuery("SELECT * FROM tracks WHERE id=" + chosenTrackID, null);
        if (track.moveToFirst()) {

            int idColIndex = track.getColumnIndex("id");
            int nameColIndex = track.getColumnIndex("name");
            int dateColIndex = track.getColumnIndex("date");
            int lengthColIndex = track.getColumnIndex("length");
            int userNameColIndex = track.getColumnIndex("userName");
            int lostEnergyColIndex = track.getColumnIndex("lostEnergy");

            infoDialog = new AlertDialog.Builder(TracksActivity.this);

            infoDialog.setTitle("Інформація про маршрут:");
            infoDialog.setMessage("ID: " + track.getInt(idColIndex) +
                    "\nНазва: " + track.getString(nameColIndex) +
                    "\nДата: " + track.getString(dateColIndex) +
                    "\nДовжина: " + Math.round(track.getDouble(lengthColIndex)*100)/100 + " м." +
                    "\nКористувач: " + track.getString(userNameColIndex) +
                    "\nПотрачено енергії: " + (float) Math.round(track.getDouble(lostEnergyColIndex)*10)/10 + " ККал");

            infoDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    return;
                }
            });

            infoDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    dialog.dismiss();
                    infoDialog = null;
                }
            });

            dialog = infoDialog.show();
        }
    }

    //Показати діалог видалення маршруту
    private void showDialogRemoveTrackFromList(final Integer chosenListItem){

        chosenListItemPosition = chosenListItem;
        final Integer chosenTrackID = tracksID.get(chosenListItem);

        removeItemDialog = new AlertDialog.Builder(TracksActivity.this);

        removeItemDialog.setTitle("Видалення маршруту:");
        removeItemDialog.setMessage("Чи дійсно бажаєте видалити маршрут " + adapter.getItem(chosenListItem).toString() + "?");

        removeItemDialog.setPositiveButton("Так", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                //Отримуємо назву таблиці, в якій зберігаються точки маршруту
                Cursor track = db.rawQuery("SELECT * FROM tracks WHERE id=" + chosenTrackID, null);

                //Видаляємо таблиця з точками маршруту
                if (track.moveToFirst()) {
                    int pointsColIndex = track.getColumnIndex("points");
                    db.execSQL("DROP TABLE " + track.getString(pointsColIndex).toString());
                }

                //Видаляємо запис з таблиці маршрутів
                db.execSQL("DELETE FROM tracks WHERE id=" + chosenTrackID);
                tracksID.remove(tracksID.get(chosenListItem));

                rows.remove(rows.get(chosenListItem));
                adapter.notifyDataSetChanged();

                Toast.makeText(getApplicationContext(), "Маршрут видалено!",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        });

        removeItemDialog.setNegativeButton("Ні", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                return;
            }
        });

        removeItemDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                removeItemDialog = null;
            }
        });

        dialog = removeItemDialog.show();
    }

    //завантажити маршрут в пам'ять для відображення на карті
    private void loadGPSTrack(final Integer chosenTrackID){

        //Завантаження даних з БД і запис в об'єкт GPSTracks.loadedTrack
        Cursor track = db.rawQuery("SELECT * FROM tracks WHERE id=" + chosenTrackID, null);
        if (track.moveToFirst()) {

            int pointsColIndex = track.getColumnIndex("points");
            int nameColIndex = track.getColumnIndex("name");
            int dateColIndex = track.getColumnIndex("date");
            int lengthColIndex = track.getColumnIndex("length");
            int userNameColIndex = track.getColumnIndex("userName");

            GPSTracks.LoadedTrack.name = track.getString(nameColIndex);
            GPSTracks.LoadedTrack.date = track.getString(dateColIndex);
            GPSTracks.LoadedTrack.length = track.getDouble(lengthColIndex);
            GPSTracks.LoadedTrack.user = track.getString(userNameColIndex);

            Cursor cursor = db.rawQuery("SELECT * FROM " + track.getString(pointsColIndex).toString(), null);
            if (cursor.moveToFirst()) {

                int latitudeColIndex = cursor.getColumnIndex("latitude");
                int longitudeColIndex = cursor.getColumnIndex("longitude");

                GPSTracks.LoadedTrack.points.clear();
                do {
                    GPSTracks.LoadedTrack.points.add(new GPSPoint(cursor.getDouble(latitudeColIndex),
                            cursor.getDouble(longitudeColIndex)));
                } while (cursor.moveToNext());
            }
        }
    }

    //Відновлення стану активності
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //Відновлення закритого активного діалогу
        if(savedInstanceState.getInt("dialog") == IS_INFO_DIALOG){
            showDialogAboutTrackFromList(savedInstanceState.getInt("itemPosition"));
        }
        else if(savedInstanceState.getInt("dialog") == IS_REMOVE_ITEM_DIALOG){
            showDialogRemoveTrackFromList(savedInstanceState.getInt("itemPosition"));
        }
        else if(savedInstanceState.getInt("dialog") == IS_OPERATION_LIST_DIALOG){
            showDialogOperationList(savedInstanceState.getInt("itemPosition"));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //Якщо жодний діалог не був активним
        if(infoDialog == null && removeItemDialog == null &&
                operationListDialog == null) {
            outState.putInt("dialog", NO_DIALOG);
        }
        //Був активний діалог
        else {
            outState.putInt("itemPosition", chosenListItemPosition);
            dialog.dismiss();

            if (infoDialog != null) {
                outState.putInt("dialog", IS_INFO_DIALOG);
            }
            else if (removeItemDialog != null) {
                outState.putInt("dialog", IS_REMOVE_ITEM_DIALOG);
            }
            else if (operationListDialog != null) {
                outState.putInt("dialog", IS_OPERATION_LIST_DIALOG);
            }
        }

    }
}