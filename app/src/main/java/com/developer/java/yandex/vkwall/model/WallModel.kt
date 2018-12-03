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
import io.reactivex.Notification
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Created by Shiplayer on 02.12.18.
 */

/**
 * Модель, в которой хранится кэш пользователей и с помощью которой можно загрузить посты
 */
class WallModel : MainActivity.DestroyListener {

    /**
     * Класс, в котором сформированны параметры, с помощью которых отправляются запросы на vk api
     * @param id - айди пользователя или сообщества
     * @param domain - если не указан [id], то отправляем запрос по которкому имени
     * @param count - количество записей, которые нужно получить
     */
    data class RequestData(val id: Int?, val domain: String?, val count: Int?)

    /**
     * Ответ, полученный с vk api и обработанный пробрасывается до [MainActivity] и уже там обрабатывается
     * @param list - список полученных постов
     * @param error - сообщение об ошибке, если она возникла, иначе пустая строка
     * @param total - общее количество записей
     */
    data class ResponseResult(var list: List<VkWallEntity>, val error: String, val total: Int)

    /**
     * Кэш пользователей, чтобы не отправлять каждый раз, чтобы выяснить пользователя по id
     */
    private val userCache = mutableMapOf<Int, User>()

    /**
     * Используется для получения юзеров, если мы не нашли их в нашем кэше
     */
    private val poolExecutor = Executors.newFixedThreadPool(5)

    /**
     * Уведомляет о том, что он получил посты или ошибку при выполнении запроса
     */
    private val mPublisher = PublishSubject.create<Notification<JsonElement>>()

    /**
     * Уведомляет о том, что он смог получить еще посты или ошибку при выполнении запроса для получения еще больше постов
     */
    private val mPublishWallMore = PublishSubject.create<Notification<JsonElement>>()

    /**
     * Используется для отправки запросов на сервер
     */
    private val publishRequest = PublishSubject.create<RequestData>()

    /**
     * Компосиция всех disposes, которые нужно будет освободить
     */
    private val disposable = CompositeDisposable()

    /**
     * При создании объекта класс, подписывается на получения запросов на получение данных, которые приходят с пользовательского интерфейса
     */
    init {
        disposable.add(publishRequest
            .throttleFirst(1, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .subscribe {
                val obs = if (it.id != null) {
                    VkApiService.api.getWall(it.id, count = it.count, accessToken = MainActivity.TOKEN)
                } else {
                    VkApiService.api.getWall(domain = it.domain, count = it.count, accessToken = MainActivity.TOKEN)
                }
                obs.subscribeOn(Schedulers.io()).subscribe({ response ->
                    Log.i("WallModel", response.toString())
/*                    val list = parseResponse(it)

                    mPublisher.onNext(list)*/
                    mPublisher.onNext(Notification.createOnNext(response.body()!!))
                }, { error ->
                    mPublisher.onNext(Notification.createOnError(error))
                })
            })
    }

    fun addDispose(dispose: Disposable) {
        disposable.add(dispose)
    }


    /**
     * Полученный ответ с vk api обрабатывается, проверятеся на наличие ошибок и добавляем юзера (владельца этой страницы) в кэш пользователей
     * @param it - объект класса [Notification], в котором может содержаться ошибка, которая связанна с соединение
     * или ответ с сервера, если все прошло успешно
     * @return Возвращаем результат обработки ответа, обернутого в класс [ResponseResult]
     */
    @SuppressLint("SimpleDateFormat")
    fun parseResponse(it: Notification<JsonElement>): ResponseResult {
        if (it.isOnNext) {
            val element = it.value!!
            val list = mutableListOf<VkWallEntity>()
            val calendar = Calendar.getInstance()
            var error = ""
            var total = -1
            if (element.asJsonObject.has("error")) {
                error = element.asJsonObject["error"].asJsonObject["error_msg"].asString
            } else {
                val response = element.asJsonObject["response"].asJsonObject
                total = response["count"].asInt
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
            return ResponseResult(list, error, total)
        } else {
            return ResponseResult(listOf(), it.error!!.message!!, -1)
        }
    }

    /**
     * Обрабатываем пользователя или сообщество и добавляем в кэш пользователей
     * @param response - [JsonObject] в котом содержится информация об пользователе или сообществе
     */
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

    /**
     * Если [MainActivity] уничтожается, то освободить местро
     */
    override fun onDestroy() {
        disposable.dispose()
    }

    /**
     * Получение пользователя по [id], в случае какой-то ошибки ответ отправится с помощью [listener]
     * @param id - айди пользователя или сообщества
     * @param listener - использутеся для отправки уведомления об успешной загрузке или ошибке
     * @return возвращается пользователь [User]
     */
    fun getUser(id: Int, listener: UserInteractor): User? {
        if (userCache.containsKey(id)) {
            return userCache[id]
        }
        loadUser(id, listener)
        return null
    }

    /**
     * Добавляет пользователя в кэщ
     * @param user - Пользователь, которого необходимо добавить в кэш
     */
    private fun addUser(user: User) {
        if (!userCache.containsKey(user.id))
            userCache[user.id] = user
    }

    /**
     * Отправка запроса о получении информации пользователя или сообщества в отдельном потоке.
     * @param id - айди пользователя или сообщества
     * @param listener - объект класса [UserInteractor] для обработки полученного ответа с сервера
     */
    @SuppressLint("CheckResult")
    private fun loadUser(id: Int, listener: UserInteractor) {
        poolExecutor.submit {
            if (id < 0) {
                VkApiService.api.getGroupInfo((-id).toString(), MainActivity.TOKEN).subscribe({
                    val obj = it.asJsonObject
                    if (obj.has("error")) {
                        listener.onError(Throwable(obj["error_msg"].asString))
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
                VkApiService.api.getUserInfo(id.toString(), MainActivity.TOKEN).subscribe({
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

    /**
     * Получение наблюдателя за получением ответа с сервера
     */
    fun getWallHandler(): Observable<Notification<JsonElement>> = mPublisher

    /**
     * Получение наблюдателя за получением ответа с сервера о получении еще постов
     */
    fun getWallMoreHandler(): Observable<Notification<JsonElement>> = mPublishWallMore

    /**
     * Метод для отправки запроса на получение списка постов для данной страницы
     * @param idWall - строка, в которой содержится id или короткое имя для данной страницы
     * @param countWall - строка, в которой содержится количество, которое нужно загрузить
     */
    fun loadWall(idWall: String, countWall: String) {
        val id = idWall.toIntOrNull()
        val count: Int? = if (countWall.isEmpty()) null else countWall.toInt()
        publishRequest.onNext(RequestData(id, idWall, count))
    }

    /**
     * Метод для отправки запроса на получение следующих постов для данной страницы
     * @param idWall - строка, в которой содержится id или короткое имя для данной страницы
     * @param countWall - строка, в которой содержится количество, которое нужно загрузить
     * @param offset - с какого поста необходиом продолжить
     */
    fun loadWallMore(idWall: String, countWall: String, offset: Int) {
        val id = idWall.toIntOrNull()
        val count: Int? = if (countWall.isEmpty()) null else countWall.toInt()
        val obs = if (id != null) {
            VkApiService.api.getWall(id, count = count, offset = offset, accessToken = MainActivity.TOKEN)
        } else {
            VkApiService.api.getWall(domain = idWall, count = count, offset = offset, accessToken = MainActivity.TOKEN)
        }
        disposable.add(
            obs.subscribeOn(Schedulers.io()).subscribe({
                mPublishWallMore.onNext(Notification.createOnNext(it.body()!!))
            }, {
                mPublishWallMore.onNext(Notification.createOnError(it))
            })
        )
    }

}