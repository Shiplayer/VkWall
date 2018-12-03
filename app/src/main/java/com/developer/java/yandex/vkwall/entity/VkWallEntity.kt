package com.developer.java.yandex.vkwall.entity

import android.os.Parcel
import android.os.Parcelable

/**
 * Класс, который хранит айди владельца этого поста, время и содержимое данного поста, реализует интерфейс [Parcelable]
 * для сохранения состояния этого объекта
 * @param id - айди пользователя или сообщества, который разместил этот пост
 * @param date - время, когда опубликовали данный пост
 * @param content - содержимое данного поста
 */
data class VkWallEntity(val id: Int, val date: String, val content: String) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, p1: Int) {
        parcel.writeInt(id)
        parcel.writeString(date)
        parcel.writeString(content)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<VkWallEntity> {
        override fun createFromParcel(parcel: Parcel): VkWallEntity {
            return VkWallEntity(parcel)
        }

        override fun newArray(size: Int): Array<VkWallEntity?> {
            return arrayOfNulls(size)
        }
    }

}
