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
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import retrofit2.Response
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Created by Shiplayer on 02.12.18.
 */

class WallModel : MainActivity.DestroyListener{

    data class RequestData(val id:Int?, val domain:String?, val count:Int?)

    private val userCache = mutableMapOf<Int, User>()
    private val poolExecutor = Executors.newFixedThreadPool(5)

    private val publish = PublishSubject.create<List<VkWallEntity>>()
    private val publicWallMore = PublishSubject.create<List<VkWallEntity>>()
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
                obs.observeOn(Schedulers.io()).subscribe({
                    Log.i("WallModel", it.toString())
                    val list = parseResponse(it)
                    publish.onNext(list)
                }, {
                    it.printStackTrace()
                })
            })
    }

    private fun parseResponse(it: Response<JsonElement>): MutableList<VkWallEntity> {
        val list = mutableListOf<VkWallEntity>()
        val calendar = Calendar.getInstance()
        if (it.body()!!.asJsonObject.has("error")) {
            //TODO сделать уведомление, если произошла ошибка
        } else {
            val response = it.body()!!.asJsonObject["response"].asJsonObject

            val items = response["items"].asJsonArray
            parseUser(response)
            for (item in items) {
                calendar.timeInMillis = item.asJsonObject["date"].asLong * 1000
                if (item.asJsonObject.has("text")) {
                    list.add(
                        VkWallEntity(
                            item.asJsonObject["from_id"].asInt,
                            calendar.time,
                            item.asJsonObject["text"].asString
                        )
                    )
                } else {
                    list.add(
                        VkWallEntity(
                            item.asJsonObject["from_id"].asInt,
                            calendar.time,
                            ""
                        )
                    )
                }
            }
        }
        return list
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

    public fun getUser(id:Int, listener:UserInteractor) : User?{
        if(userCache.containsKey(id)){
            return userCache[id]
        }
        loadUser(id, listener)
        return null
    }

    fun addUser(user: User){
        if(!userCache.containsKey(user.id))
            userCache[user.id] = user
    }

    @SuppressLint("CheckResult")
    private fun loadUser(id: Int, listener: UserInteractor) {
        poolExecutor.submit{
            VkApiService.api.getUserInfo(id.toString(), MainActivity.token).subscribe({
                val body = it.body()!!.asJsonObject["response"].asJsonArray
                for(item in body) {
                    val itemObject = item.asJsonObject
                    val user = User(itemObject["first_name"].asString, itemObject ["last_name"].asString, id, itemObject["photo_100"].asString)
                    userCache[id] = user
                    Observable.just(user).subscribeOn(AndroidSchedulers.mainThread()).subscribe {
                        listener.onSuccessful(user)
                    }

                }
            }, {
                listener.onError(it)
            })
        }
    }

    fun loadWall(idWall:String, countWall:String) : Observable<List<VkWallEntity>>{
        val id= idWall.toIntOrNull()
        val count: Int? = if (countWall.isEmpty()) null else countWall.toInt()
        publishRequest.onNext(RequestData(id, idWall, count))
        return publish
    }

    fun loadWallMore(idWall: String, countWall: String?) : Observable<List<VkWallEntity>> {
        val id= idWall.toIntOrNull()
        val count: Int? = countWall?.toInt()
        val obs = if(id != null) {
            VkApiService.api.getWall(id, count = count, accessToken = MainActivity.token)
        } else {
            VkApiService.api.getWall(domain = idWall, count = count, accessToken = MainActivity.token)
        }
        disposable.add(obs.subscribeOn(Schedulers.io()).subscribe({
            publicWallMore.onNext(parseResponse(it))
        }, {
            publicWallMore.onError(it)
        })
        )
        return publicWallMore
    }

}