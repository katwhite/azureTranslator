package com.example.azuretranslation;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface AzureTranslationAPI {
    String API_URL = "https://api.cognitive.microsofttranslator.com";
    // TODO: рекомендуется использовать свой ключ, чтобы получить доп. балл
    String key = "cdb991dbc7b8453797ea2610567b74b7"; //
    // TODO: регион указать отдельной переменной
    String region = "eastasia";
    // запрос языков работает без ключа
    @GET("/languages?api-version=3.0&scope=translation")
    Call<LanguagesResponse> getLanguages();

    @POST("/translate?api-version=3.0&") // путь к API
    @Headers({
            "Content-Type: application/json",  "Ocp-Apim-Subscription-Key: "+ key, "Ocp-Apim-Subscription-Region: "+ region,
            // TODO: указать ключ и регион
    })

    // TranslatedText - формат ответа от сервера
    // Тип ответа - TranslatedText, действие - translate, содержание запроса - пустое (нет полей формы)
    // TODO: с помощью аннотации @Body передать поля запроса к API (текст для перевода)
    // см. примеры https://square.github.io/retrofit/
    Call<TranslatedText[]> translate(@Query("from") String lng1, @Query("to") String lng, @Body TranslatedText.Translation[] text);
}
