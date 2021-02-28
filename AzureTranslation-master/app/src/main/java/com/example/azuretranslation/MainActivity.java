package com.example.azuretranslation;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceControl;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    SQLiteDatabase db;
    DBHelper dbHelper;
    EditText input;
    TextView output;
    List<String> languages = new ArrayList<>();
    ArrayAdapter<String> adapter;
    ArrayAdapter<String> adapter2;
    SharedPreferences sp;
    String from = "eng", to = "ru";
    // Экземпляр библиотеки и интерфейса можно использовать для всех обращений к сервису
    // формируем экземпляр библиотеки
    Retrofit retrofit = new Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create()) // ответ от сервера в виде строки
            .baseUrl(AzureTranslationAPI.API_URL) // адрес API сервера
            .build();

    AzureTranslationAPI api = retrofit.create(AzureTranslationAPI.class); // описываем, какие функции реализованы

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dbHelper = new DBHelper(this);
        db = dbHelper.getWritableDatabase();

        input = findViewById(R.id.input);
        output = findViewById(R.id.output);

        Spinner spinner1 = findViewById(R.id.spinner1);
        adapter = new ArrayAdapter<>(getBaseContext(), android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner1.setAdapter(adapter);
        AdapterView.OnItemSelectedListener itemSelectedListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                from = (String) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        adapter.notifyDataSetChanged();
        spinner1.setOnItemSelectedListener(itemSelectedListener);
        Spinner spinner2 = findViewById(R.id.spinner2);
        adapter2 = new ArrayAdapter<>(getBaseContext(), android.R.layout.simple_spinner_item, languages);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner2.setAdapter(adapter2);
        AdapterView.OnItemSelectedListener itemSelectedListener2 = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                to = (String) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        spinner2.setOnItemSelectedListener(itemSelectedListener2);

        sp = getSharedPreferences("settings", Context.MODE_PRIVATE);
        Time now = new Time();
        now.setToNow();
        Time dflt = new Time();
        dflt.set(0,0,0,0,0,0);
        String lastCall = sp.getString("lastCall", dflt.format3339(false));
        Time last = new Time();
        last.parse3339(lastCall);
        last.monthDay++;
        last.normalize(true);

        if (now.after(last)) {
            Call<LanguagesResponse> call = api.getLanguages(); // создаём объект-вызов
            call.enqueue(new LanguagesCallback());

        }
        else {
            Cursor c = db.rawQuery("SELECT * FROM languages", null);

            while (c.moveToNext()) {
                String name = c.getString(c.getColumnIndex("lng"));
                languages.add(name);

            }
            adapter.notifyDataSetChanged();
            adapter2.notifyDataSetChanged();
        }
    }

    // TODO: создать аналогичным образом класс для ответа сервера при переводе текста
    class LanguagesCallback implements Callback<LanguagesResponse> {

        @Override
        public void onResponse(Call<LanguagesResponse> call, Response<LanguagesResponse> response) {
            if (response.isSuccessful()) {
                // TODO: response.body() содержит массив языков, доступных для перевода
                db.execSQL("delete from languages");
                Log.d("mytag", "response: " + response.body());
                languages.addAll(response.body().translation.keySet());
                adapter.notifyDataSetChanged();
                adapter2.notifyDataSetChanged();

                for (String lng:response.body().translation.keySet()
                     ) {
                    ContentValues values = new ContentValues();
                    values.put("lng", lng);
                    db.insert("languages", null, values);
                }
                Cursor c = db.rawQuery("SELECT * FROM languages ", null);
                c.moveToFirst();

                Time now = new Time();
                now.setToNow();
                SharedPreferences.Editor editor = sp.edit();
                editor.putString("lastCall", now.format3339(false));
                editor.apply();

            } else {
                // TODO: выводить Toast и сообщение в журнал в случае ошибки
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Failed response", Toast.LENGTH_SHORT);
                toast.show();
                Log.d("mytag", "failed response");
            }
        }

        @Override
        public void onFailure(Call<LanguagesResponse> call, Throwable t) {
            // TODO: выводить Toast и сообщение в журнал в случае ошибки
            Toast toast = Toast.makeText(getApplicationContext(),
                    "Failure", Toast.LENGTH_SHORT);
            toast.show();
            Log.d("mytag", "failure");
        }
    }

    class TranslatedCallback implements Callback<TranslatedText[]> {

        @Override
        public void onResponse(Call<TranslatedText[]> call, Response<TranslatedText[]> response) {
            if (response.isSuccessful()) {
                Log.d("mytag", "response: " + response.body());
                String translatedText = response.body()[0].toString();
                output.setText(translatedText);

            } else {
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Failed translate response; code "+response.code(), Toast.LENGTH_SHORT);
                toast.show();
                Log.d("mytag", "failure");
            }
        }

        @Override
        public void onFailure(Call<TranslatedText[]> call, Throwable t) {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "Failed to translate", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    public void Translate(View v){

        TranslatedText.Translation text = new TranslatedText.Translation();

        text.text = String.valueOf(input.getText());
        TranslatedText.Translation[] array = {text};

        Call<TranslatedText[]> call = api.translate(from, to, array); // создаём объект-вызов
        call.enqueue(new TranslatedCallback());
        //curl -X POST "https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&from=en&to=zh-Hans"
        // -H "Ocp-Apim-Subscription-Key: <client-secret>" -H "Content-Type: application/json; charset=UTF-8" -d
        // "[{'Text':'Hello, what is your name?'}]"
    }
}
