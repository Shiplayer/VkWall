package com.developer.java.yandex.vkwall.entity

/**
 * Created by Shiplayer on 02.12.18.
 */

/**
 * Класс, который содержит информацию об пользователе или сообществе. В случае сообщества, [lastName] будет пустым
 * @param firstName - имя пользователя или название сообщества
 * @param lastName - фамилия пользователя или пустая строка для сообщества
 * @param id - айди пользователя или сообщества
 * @param urlPhoto - ссылка на фотографию пользователя или сообщества
 */
data class User(val firstName: String, val lastName: String, val id: Int, val urlPhoto: String)