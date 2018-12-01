package com.developer.java.yandex.vkwall.retrofit

import com.google.gson.JsonElement
import io.reactivex.Completable
import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface VkApi {
    @GET("method/wall.get")
    fun getWall(@Query("owner_id") id:Int, @Query("count") count:Int, @Query("access_token") accessToken:String, @Query("v") v:String = "5.92"): Single<Response<JsonElement>>

    @GET("method/wall.get")
    fun getAllWall(@Query("owner_id") id:Int, @Query("access_token") accessToken:String, @Query("v") v:String = "5.92"): Single<Response<JsonElement>>
}