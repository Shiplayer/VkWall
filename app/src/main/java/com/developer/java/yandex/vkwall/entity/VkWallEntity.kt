package com.developer.java.yandex.vkwall.entity

import android.os.Parcel
import android.os.Parcelable
import java.util.*

data class VkWallEntity(val id:Int, val date: String, val content:String) : Parcelable{
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
