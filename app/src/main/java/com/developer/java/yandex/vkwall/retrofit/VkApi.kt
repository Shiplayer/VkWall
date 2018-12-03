package com.developer.java.yandex.vkwall.retrofit

import com.google.gson.JsonElement
import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Интерфейс, в котором указаны методы для отправки запросов на сервер
 */
interface VkApi {

    /**
     * Для получения списка постов
     * @param id - айди пользователя или сообщества, по умолчанию null
     * @param domain - короткое имя пользователя или сообщества, по умолчанию null
     * @param offset - смещение, которое необходимо сделать, по умолчанию 0
     * @param count - количество, которое необходимо получить, по умолчанию null
     * @param accessToken - токен, который используется для отправки запросов
     * @param ext - получать расширенную информацию, по умолчанию true
     * @param v - версия используемой vk api, по умолчанию 5.92
     */
    @GET("method/wall.get")
    fun getWall(
        @Query("owner_id") id: Int? = null,
        @Query("domain") domain: String? = null,
        @Query("offset") offset: Int = 0,
        @Query("count") count: Int? = null,
        @Query("access_token") accessToken: String,
        @Query("extended") ext: Boolean = true,
        @Query("v") v: String = "5.92"
    ): Single<Response<JsonElement>>

    /**
     * Для получения информации пользователя
     * @param userId - айди или короткое имя пользователя
     * @param accessToken - токен, который используется для отправки запросов
     * @param fields - строка, в которую указываются дополнительные поля, для получения информации об пользователе или сообществе
     * @param v - версия используемой vk api, по умолчанию 5.92
     */
    @GET("method/users.get")
    fun getUserInfo(
        @Query("user_ids") userId: String,
        @Query("access_token") accessToken: String,
        @Query("fields") fields: String = "photo_100",
        @Query("v") v: String = "5.92"
    ): Single<Response<JsonElement>>

    /**
     * Для получения информации об сообществе
     * @param id - айди сообщества
     * @param accessToken - токен, который используется для отправки запросов
     * @param v - версия используемой vk api, по умолчанию 5.92
     */
    @GET("method/groups.getById")
    fun getGroupInfo(
        @Query("group_id") id: String,
        @Query("access_token") accessToken: String,
        @Query("v") v: String = "5.92"
    ): Single<JsonElement>
}