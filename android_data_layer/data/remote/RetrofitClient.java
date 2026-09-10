package com.example.android.data.remote;

import com.example.android.utils.Constants;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class RetrofitClient {

    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(Constants.HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(Constants.HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .writeTimeout(Constants.HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(Constants.API_BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static EcolimApiService getApiService() {
        return getClient().create(EcolimApiService.class);
    }
}
