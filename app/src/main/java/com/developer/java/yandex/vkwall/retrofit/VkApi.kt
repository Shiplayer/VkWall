package com.developer.java.yandex.vkwall.retrofit

import com.google.gson.JsonElement
import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface VkApi {
    @GET("method/wall.get")
    fun getWall(@Query("owner_id") id:Int? = null, @Query("domain") domain:String? = null, @Query("offset") offset:Int = 0, @Query("count") count:Int? = null, @Query("access_token") accessToken:String, @Query("extended") ext:Boolean = true, @Query("v") v:String = "5.92"): Single<Response<JsonElement>>

    //@GET("method/wall.get")
    //fun getAllWall(@Query("owner_id") id:Int, @Query("access_token") accessToken:String, @Query("v") v:String = "5.92"): Single<Response<JsonElement>>

    @GET("method/users.get")
    fun getUserInfo(@Query("user_ids") userId: String, @Query("access_token") accessToken:String, @Query("fields") fields: String = "photo_100", @Query("v") v:String = "5.92"): Single<Response<JsonElement>>

    @GET("method/groups.getById")
    fun getGroupInfo(@Query("group_id") id:String, @Query("access_token") accessToken:String, @Query("v") v:String = "5.92"): Single<JsonElement>
}