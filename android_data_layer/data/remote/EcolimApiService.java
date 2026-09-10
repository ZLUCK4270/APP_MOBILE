package com.example.android.data.remote;

import com.google.gson.JsonObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface EcolimApiService {

    @POST("/api/v1/sync")
    Call<ResponseBody> syncRegistros(
            @Header("X-Device-ID") String deviceId,
            @Body JsonObject payload
    );

    @GET("/api/v1/residuos")
    Call<ResponseBody> getResiduos();

    @GET("/api/v1/registros")
    Call<ResponseBody> getRegistros(
            @Query("fecha") String fecha,
            @Query("residuo") String residuoId
    );

    @GET("/api/v1/reportes")
    Call<ResponseBody> getReportes(
            @Query("fecha_inicio") String fechaInicio,
            @Query("fecha_fin") String fechaFin,
            @Query("formato") String formato
    );

    @POST("/auth/register")
    Call<ResponseBody> registerUser(@Body JsonObject payload);

    @POST("/auth/login")
    Call<ResponseBody> loginUser(@Body JsonObject payload);

    @PUT("/usuarios/perfil/{id}")
    Call<ResponseBody> updatePerfil(
            @Path("id") int userId,
            @Body JsonObject payload
    );
}
