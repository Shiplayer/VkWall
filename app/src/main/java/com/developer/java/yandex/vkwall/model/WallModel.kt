package com.developer.java.yandex.vkwall.model

import android.annotation.SuppressLint
import android.util.Log
import com.developer.java.yandex.vkwall.MainActivity
import com.developer.java.yandex.vkwall.entity.User
import com.developer.java.yandex.vkwall.entity.VkWallEntity
import com.developer.java.yandex.vkwall.interactors.UserInteractor
import com.developer.java.yandex.vkwall.retrofit.VkApiService
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Created by Shiplayer on 02.12.18.
 */

class WallModel : MainActivity.DestroyListener {

    data class RequestData(val id: Int?, val domain: String?, val count: Int?)
    data class ResponseResult(val list: List<VkWallEntity>, val error: String)

    val userCache = mutableMapOf<Int, User>()
    private val poolExecutor = Executors.newFixedThreadPool(5)

    private val publish = PublishSubject.create<JsonElement>()
    private val publicWallMore = PublishSubject.create<JsonElement>()
    private val publishRequest = PublishSubject.create<RequestData>()
    private val disposable = CompositeDisposable()

    init {
        disposable.add(publishRequest
            .throttleFirst(1, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .subscribe {
                val obs = if (it.id != null) {
                    VkApiService.api.getWall(it.id, count = it.count, accessToken = MainActivity.token)
                } else {
                    VkApiService.api.getWall(domain = it.domain, count = it.count, accessToken = MainActivity.token)
                }
                obs.subscribeOn(Schedulers.io()).subscribe({ response ->
                    Log.i("WallModel", response.toString())
/*                    val list = parseResponse(it)

                    publish.onNext(list)*/
                    publish.onNext(response.body()!!)
                }, { error ->
                    error.printStackTrace()
                })
            })
    }

    fun addDispose(dispose: Disposable) {
        disposable.add(dispose)
    }

    fun parseResponse(it: JsonElement): ResponseResult {
        val list = mutableListOf<VkWallEntity>()
        val calendar = Calendar.getInstance()
        var error = ""
        if (it.asJsonObject.has("error")) {
            error = it.asJsonObject["error"].asJsonObject["error_msg"].asString
        } else {
            val response = it.asJsonObject["response"].asJsonObject

            val items = response["items"].asJsonArray
            parseUser(response)
            for (item in items) {
                calendar.timeInMillis = item.asJsonObject["date"].asLong * 1000
                val time = SimpleDateFormat("dd.MM.yy HH:mm").format(calendar.time)
                if (item.asJsonObject.has("text")) {
                    list.add(
                        VkWallEntity(
                            item.asJsonObject["from_id"].asInt,
                            time,
                            item.asJsonObject["text"].asString
                        )
                    )
                } else {
                    list.add(
                        VkWallEntity(
                            item.asJsonObject["from_id"].asInt,
                            time,
                            ""
                        )
                    )
                }
            }
        }
        return ResponseResult(list, error)
    }

    private fun parseUser(response: JsonObject) {
        if (response.has("groups") && response["groups"].asJsonArray.size() > 0) {
            val group = response["groups"].asJsonArray[0].asJsonObject
            addUser(
                User(
                    group["name"].asString,
                    "",
                    -group["id"].asInt,
                    group["photo_100"].asString
                )
            )
        }
        if (response.has("profile") && response["profiles"].asJsonArray.size() > 0) {
            val profile = response["profiles"].asJsonArray[0].asJsonObject
            addUser(
                User(
                    profile["first_name"].asString,
                    profile["last_name"].asString,
                    profile["id"].asInt,
                    profile["photo_100"].asString
                )
            )
        }
    }


    override fun onDestroy() {
        disposable.dispose()
    }

    fun getUser(id: Int, listener: UserInteractor): User? {
        if (userCache.containsKey(id)) {
            return userCache[id]
        }
        loadUser(id, listener)
        return null
    }

    private fun addUser(user: User) {
        if (!userCache.containsKey(user.id))
            userCache[user.id] = user
    }

    @SuppressLint("CheckResult")
    private fun loadUser(id: Int, listener: UserInteractor) {
        poolExecutor.submit {
            if (id < 0) {
                VkApiService.api.getGroupInfo((-id).toString(), MainActivity.token).subscribe({
                    val obj = it.asJsonObject
                    if (obj.has("error")) {

                    } else {
                        val info = obj["response"].asJsonArray[0].asJsonObject
                        val user = User(info["name"].asString, "", id, info["photo_100"].asString)
                        userCache[id] = user
                        Single.just(user).subscribeOn(AndroidSchedulers.mainThread()).subscribe({
                            listener.onSuccessful(user)
                        }, {
                            listener.onError(it)
                        })
                    }
                }, {
                    listener.onError(it)
                })
            } else {
                VkApiService.api.getUserInfo(id.toString(), MainActivity.token).subscribe({
                    val body = it.body()!!.asJsonObject["response"].asJsonArray
                    for (item in body) {
                        val itemObject = item.asJsonObject
                        val user = User(
                            itemObject["first_name"].asString,
                            itemObject["last_name"].asString,
                            id,
                            itemObject["photo_100"].asString
                        )
                        userCache[id] = user
                        Single.just(user).subscribeOn(AndroidSchedulers.mainThread()).subscribe({
                            listener.onSuccessful(user)
                        }, {
                            listener.onError(it)
                        })

                    }
                }, {
                    listener.onError(it)
                })
            }
        }
    }

    fun getWallHandler(): Observable<JsonElement> = publish

    fun getWallMoreHandler(): Observable<JsonElement> = publicWallMore

    fun loadWall(idWall: String, countWall: String) {
        val id = idWall.toIntOrNull()
        val count: Int? = if (countWall.isEmpty()) null else countWall.toInt()
        publishRequest.onNext(RequestData(id, idWall, count))
    }


    fun loadWallMore(idWall: String, countWall: String, offset: Int) {
        val id = idWall.toIntOrNull()
        val count: Int? = if (countWall.isEmpty()) null else countWall.toInt()
        val obs = if (id != null) {
            VkApiService.api.getWall(id, count = count, offset = offset, accessToken = MainActivity.token)
        } else {
            VkApiService.api.getWall(domain = idWall, count = count, offset = offset, accessToken = MainActivity.token)
        }
        disposable.add(
            obs.subscribeOn(Schedulers.io()).subscribe({
                publicWallMore.onNext(it.body()!!)
            }, {
                publicWallMore.onError(it)
            })
        )
    }

}