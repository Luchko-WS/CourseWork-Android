package edu.vntu.sacmig.keem_16m;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class UsersActivity extends AppCompatActivity{

    //Константи для позначення відкритих діалогових вікон
    private final Integer NO_DIALOG = -1;
    private final Integer IS_OPERATION_LIST_DIALOG = 0;
    private final Integer IS_INFO_DIALOG = 1;
    private final Integer IS_CREATE_OR_EDIT_ITEM_DIALOG = 2;
    private final Integer IS_REMOVE_ITEM_DIALOG = 3;

    //Активне діалогове вікно
    private Dialog openedDialog;
    //Діалогові вікна
    private AlertDialog.Builder operationListDialog;
    private AlertDialog.Builder infoDialog;
    private AlertDialog.Builder addOrEditUserDialog;
    private AlertDialog.Builder removeItemDialog;

    //БД
    private DBHelper dbHelper;
    private SQLiteDatabase db;

    //Список ідентифікаторів користувачів в БД
    private List<Integer> userID;
    private Integer chosenListItemPosition;
    //Дані ліствью
    private ArrayAdapter<String> adapter;
    private List<String> rows = new ArrayList<>();

    private ImageButton buttonAdd;
    private ListView listViewUsers;
    //Вьюхи з діалогового вікна додання/редагування користувача
    private EditText nameEditText;
    private EditText weightEditText;

    //Зміні, що служать для збереження інфо з діалогового вікна
    //редагування/додання користувача
    private Boolean isEditTextsChanged = false;
    private String nameText = "";
    private String weightText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_users);

        dbHelper = new DBHelper(this);
        db = dbHelper.getWritableDatabase();

        buttonAdd = (ImageButton) findViewById(R.id.buttonAdd);
        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Додати користувача
                showDialogAddOrEditUser(-1);
            };
        });

        //Зчитування БД
        userID = new ArrayList<>();
        listViewUsers = (ListView) findViewById(R.id.userList);
        listViewUsers.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        Cursor c = db.query("users", null, null, null, null, null, null);
        //Якщо є записи, то читаємо
        if (c.moveToFirst()) {

            int idColIndex = c.getColumnIndex("id");
            int nameColIndex = c.getColumnIndex("name");

            do {
                rows.add(c.getString(nameColIndex));
                userID.add(c.getInt(idColIndex));
            } while (c.moveToNext());

            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, rows);
            listViewUsers.setAdapter(adapter);
        }

        //Виділення активного користувача
        if(listViewUsers.getCount() != 0 && CurrentUser.selectedItemID != -1){
            listViewUsers.setItemChecked(CurrentUser.selectedItemID, true);
        }

        //Довге натиснення на елементі списку
        listViewUsers.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                //Створення діалогу вибору операцій
                showDialogOperationList(position);
                return true;
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

       //Користувач не обраний зі списку (список порожній)
        if(rows.size() == 0) {
            CurrentUser.setUserInformation(-1, "невідомий", -1.0f, -1.0f);
        }
        //Зберігаємо інформацію про обраного корисувача
        else{
            Cursor user = db.rawQuery("SELECT * FROM users WHERE id=" + userID.get(listViewUsers.getCheckedItemPosition()), null);
            if (user.moveToFirst()) {

                int idColIndex = user.getColumnIndex("id");
                int nameColIndex = user.getColumnIndex("name");
                int weightColIndex = user.getColumnIndex("weight");
                int lostEnergyColIndex = user.getColumnIndex("lostEnergy");

                //Записуємо дані про активного користувача
                CurrentUser.setUserInformation(
                        user.getInt(idColIndex),
                        user.getString(nameColIndex),
                        user.getFloat(weightColIndex),
                        user.getFloat(lostEnergyColIndex));
                CurrentUser.selectedItemID = listViewUsers.getCheckedItemPosition();
            }
        }

        //Зписуємо дані активного користувача у файл, для зчитування даних при завантаженні додатку
        //Записуються дані також тоді, коли користувач не обраний
        final String options = CurrentUser.selectedItemID + "|" + CurrentUser.id;
        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                    openFileOutput("options.txt", MODE_PRIVATE)));
            bw.write(options);
            bw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Діалог операцій з користувачем (елементом списку)
    private void showDialogOperationList(final Integer chosenListItem){

        final String[] array = {"Показати інформацію", "Редагувати", "Видалити"};
        //Отримуємо позицію активного користувача в списку
        chosenListItemPosition = chosenListItem;

        operationListDialog = new AlertDialog.Builder(UsersActivity.this);
        operationListDialog.setTitle("Операції над записом");

        operationListDialog.setItems(array, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                switch (item){
                    case 0:
                        //Показати інформацію
                        showDialogAboutUserFromList(chosenListItem);
                        break;
                    case 1:
                        //Редагувати користувача
                        showDialogAddOrEditUser(chosenListItem);
                        break;
                    case 2:
                        //Видалення користувача
                        showDialogRemoveUserFromList(chosenListItem);
                        break;
                }
            }
        });

        //Діалог знищено
        operationListDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                operationListDialog = null;
            }
        });

        //Виводимо діалог зі списком операцій і вказуємо, що він активний
        openedDialog = operationListDialog.show();
    }

    //Діалог додання нового користувача та редагування інформації про користувача
    private void showDialogAddOrEditUser(final Integer chosenListItem){

        chosenListItemPosition = chosenListItem;

        addOrEditUserDialog = new AlertDialog.Builder(UsersActivity.this);

        final LayoutInflater inflater = UsersActivity.this.getLayoutInflater();
        final View view = inflater.inflate(R.layout.add_user_dialog, null);

        nameEditText = (EditText) view.findViewById(R.id.nameEditText);
        weightEditText = (EditText) view.findViewById(R.id.weightEditText);

        //Отриманий параметр позиції в списку. Редагування користувача
        if(chosenListItem != -1){

            //Зчитування даних про користувача
            Cursor user  = db.rawQuery("SELECT * FROM users WHERE id=" + userID.get(chosenListItem), null);
            if (user.moveToFirst()) {

                int nameColIndex = user.getColumnIndex("name");
                int weightColIndex = user.getColumnIndex("weight");

                if(isEditTextsChanged){
                    nameEditText.setText(nameText);
                    weightEditText.setText(weightText);
                    isEditTextsChanged = false;
                }
                else {
                    nameEditText.setText(user.getString(nameColIndex));
                    weightEditText.setText(String.valueOf(user.getFloat(weightColIndex)));
                }
            }

            addOrEditUserDialog
                    .setView(view)
                    .setTitle("Редагування даних користувача:")
                    .setPositiveButton("Змінити", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {

                            if(nameEditText.getText().toString().isEmpty()){
                                Toast.makeText(getApplicationContext(), "Вводьте, будь ласка, корректні дані!",
                                        Toast.LENGTH_LONG).show();
                                return;
                            }

                            //Оновлення даних користувача в БД
                            try{
                                db.execSQL("UPDATE users SET name='" + nameEditText.getText().toString() +
                                        "', weight=" + Float.valueOf(weightEditText.getText().toString()) +
                                        " WHERE id=" + userID.get(chosenListItem));
                            }
                            catch(Exception ex){
                                Toast.makeText(getApplicationContext(), "Вводьте, будь ласка, корректні дані!",
                                        Toast.LENGTH_LONG).show();
                                return;
                            }

                            //Оновлення в ліствью
                            adapter.remove(adapter.getItem(chosenListItem));
                            adapter.insert(nameEditText.getText().toString(), chosenListItem);
                            adapter.notifyDataSetChanged();

                            Toast.makeText(getApplicationContext(), "Дані оновлено!",
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Відмінити", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            return;
                        }
                    });
        }
        //Додання нового користувача. Параметр "позиція" не отримано
        else {

            if(isEditTextsChanged){
                nameEditText.setText(nameText);
                weightEditText.setText(weightText);
                isEditTextsChanged = false;
            }

            addOrEditUserDialog
                    .setTitle("Додання нового користувача:")
                    .setView(view)
                    .setPositiveButton("Додати", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {

                            ContentValues cv = new ContentValues();

                            if(nameEditText.getText().toString().isEmpty()){
                                Toast.makeText(getApplicationContext(), "Вводьте, будь ласка, корректні дані!",
                                        Toast.LENGTH_LONG).show();
                                return;
                            }
                            cv.put("name", nameEditText.getText().toString());

                            try {
                                cv.put("weight", Float.valueOf(weightEditText.getText().toString()));
                            }
                            catch (Exception ex) {
                                Toast.makeText(getApplicationContext(), "Вводьте, будь ласка, корректні дані!",
                                        Toast.LENGTH_LONG).show();
                                return;
                            }

                            //Запис в БД
                            cv.put("lostEnergy", 0.0);
                            db.insert("users", null, cv);

                            Cursor c = db.query("users", null, null, null, null, null, null);
                            if (c.moveToLast()) {
                                int idColIndex = c.getColumnIndex("id");
                                userID.add(c.getInt(idColIndex));
                            }

                            //Якщо адаптер був порожнім. Створюємо його
                            if(adapter == null){
                                rows.add(nameEditText.getText().toString());
                                adapter = new ArrayAdapter<>(UsersActivity.this, android.R.layout.simple_list_item_single_choice, rows);
                                listViewUsers.setAdapter(adapter);
                                adapter.notifyDataSetChanged();
                            }
                            //Адаптер не порожній. Оновлюємо його
                            else {
                                rows.add(nameEditText.getText().toString());
                                adapter.notifyDataSetChanged();
                            }

                            if(listViewUsers.getCount()==1){
                                listViewUsers.setItemChecked(0, true);
                            }

                            Toast.makeText(getApplicationContext(), "Нового користувача додано!",
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Відмінити", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            return;
                        }
                    });
        }

        addOrEditUserDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                addOrEditUserDialog = null;
            }
        });

        openedDialog = addOrEditUserDialog.show();
    }

    //Діалог з інформацією про користувача
    private void showDialogAboutUserFromList(final Integer chosenListItem){

        chosenListItemPosition = chosenListItem;
        Integer chosenUserID = userID.get(chosenListItem);

        //Зчитування з БД
        Cursor user  = db.rawQuery("SELECT * FROM users WHERE id=" +chosenUserID, null);
        if (user.moveToFirst()) {

            int idColIndex = user.getColumnIndex("id");
            int nameColIndex = user.getColumnIndex("name");
            int weightColIndex = user.getColumnIndex("weight");
            int lostEnergyColIndex = user.getColumnIndex("lostEnergy");

            infoDialog = new AlertDialog.Builder(UsersActivity.this);

            infoDialog.setTitle("Інформація про користувача:");
            infoDialog.setMessage("ID: " + user.getInt(idColIndex) + "\nІм'я: " + user.getString(nameColIndex)+
                    "\nМаса: " + Math.round(user.getFloat(weightColIndex)) +
                    " кг.\nПотрачено енергії:\n" + (float) Math.round(user.getFloat(lostEnergyColIndex)*10)/10 + " Ккал");

            infoDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    return;
                }
            });

            infoDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    infoDialog = null;
                }
            });

            openedDialog = infoDialog.show();
        }
    }

    //Діалог видалення користувача
    private void showDialogRemoveUserFromList(final Integer chosenListItem){

        chosenListItemPosition = chosenListItem;

        removeItemDialog = new AlertDialog.Builder(UsersActivity.this);

        removeItemDialog.setTitle("Видалення користувача:");
        removeItemDialog.setMessage("Чи дійсно бажаєте видалити користувача " + adapter.getItem(chosenListItem).toString() + "?");

        removeItemDialog.setPositiveButton("Так", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                //Видаляємо користувача з БД
                db.execSQL("DELETE FROM users WHERE id=" + userID.get(chosenListItem));
                userID.remove(userID.get(chosenListItem));

                //Відмічаємо активного користувача:
                //якщо видаляємо активного користувача
                if(listViewUsers.getCheckedItemPosition() == chosenListItem){
                    if(listViewUsers.getCount() != 1){
                        if(chosenListItem == listViewUsers.getCount()-1){
                            listViewUsers.setItemChecked(chosenListItem - 1, true);
                        }
                        else{
                            listViewUsers.setItemChecked(chosenListItem, true);
                        }
                    }
                }
                //видаляємо неактивного користувача (позиція яких вище позиції активного)
                else{
                    if(listViewUsers.getCount() != 1){
                        if(chosenListItem < listViewUsers.getCheckedItemPosition()){
                            listViewUsers.setItemChecked(listViewUsers.getCheckedItemPosition() - 1, true);
                        }
                    }
                }

                //Видаляємо з ліствью
                rows.remove(rows.get(chosenListItem));
                adapter.notifyDataSetChanged();

                Toast.makeText(getApplicationContext(), "Користувача видалено!",
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

        openedDialog = removeItemDialog.show();
    }

    //Відновлення стану активності
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        //Відновлюємо закритий діалог
        if(savedInstanceState.getInt("dialog") == IS_INFO_DIALOG){
            showDialogAboutUserFromList(savedInstanceState.getInt("itemPosition"));
        }
        else if(savedInstanceState.getInt("dialog") == IS_REMOVE_ITEM_DIALOG){
            showDialogRemoveUserFromList(savedInstanceState.getInt("itemPosition"));
        }
        else if(savedInstanceState.getInt("dialog") == IS_OPERATION_LIST_DIALOG){
            showDialogOperationList(savedInstanceState.getInt("itemPosition"));
        }
        else if(savedInstanceState.getInt("dialog") == IS_CREATE_OR_EDIT_ITEM_DIALOG){
            nameText = savedInstanceState.getString("nameText");
            weightText = savedInstanceState.getString("weightText");
            isEditTextsChanged = savedInstanceState.getBoolean("isEdited");
            showDialogAddOrEditUser(savedInstanceState.getInt("itemPosition"));
        }
    }

    //Збереження стану активності
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Жодний діалог не був активним
        if(infoDialog == null && removeItemDialog == null &&
                operationListDialog == null && addOrEditUserDialog == null) {
            outState.putInt("dialog", NO_DIALOG);
        }
        //Був відкритий діалог
        else {
            outState.putInt("itemPosition", chosenListItemPosition);
            openedDialog.dismiss();

            if (infoDialog != null) {
                outState.putInt("dialog", IS_INFO_DIALOG);
            }
            else if (removeItemDialog != null) {
                outState.putInt("dialog", IS_REMOVE_ITEM_DIALOG);
            }
            else if (operationListDialog != null) {
                outState.putInt("dialog", IS_OPERATION_LIST_DIALOG);
            }
            else if (addOrEditUserDialog != null) {
                outState.putInt("dialog", IS_CREATE_OR_EDIT_ITEM_DIALOG);
                outState.putString("nameText", nameEditText.getText().toString());
                outState.putString("weightText", weightEditText.getText().toString());
                outState.putBoolean("isEdited", true);
            }
        }
    }
}