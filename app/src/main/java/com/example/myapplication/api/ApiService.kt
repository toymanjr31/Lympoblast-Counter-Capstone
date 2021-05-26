package com.example.myapplication.api

import okhttp3.MultipartBody
import com.example.myapplication.response.PostImageResponse
import com.example.myapplication.response.ResultResponse
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface ApiService {

    companion object {
        operator fun invoke(): ApiService {
            return Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }

    @GET("/api/result/opencv/{filename}")
    fun getResult(
        @Path("filename") filename: String
    ): Call<ResultResponse>

    @Multipart
    @POST("api/upload")
    fun uploadImage(
        @Part file: MultipartBody.Part
    ): Call<PostImageResponse>
}