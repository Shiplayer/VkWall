package com.developer.java.yandex.vkwall.interactors

import com.developer.java.yandex.vkwall.entity.User

/**
 * Created by Shiplayer on 02.12.18.
 */

interface UserInteractor{
    fun onSuccessful(user: User)
    fun onError(throwable: Throwable)
}