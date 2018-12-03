package com.developer.java.yandex.vkwall.retrofit

import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Класс для взаимодейсвтяи с vk api
 */
class VkApiService {

    companion object {
        val api by lazy {
            create()
        }

        private fun create(): VkApi {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.vk.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()

            return retrofit.create(VkApi::class.java)
        }
    }
}