package com.dikiyserge.glassmosaic

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable

@Suppress("DEPRECATION")
inline fun <reified T> Bundle.getParcelableData(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(key, T::class.java)
    } else {
        getParcelable(key)
    }
}

@Suppress("DEPRECATION")
inline fun <reified T> Intent.getParcelableExtraData(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, T::class.java)
    } else {
        getParcelableExtra(key)
    }
}

@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Parcel.readParcelableListData(list: List<T>) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        readParcelableList(list, T::class.java.classLoader, T::class.java)
    } else {
        readParcelableList(list, T::class.java.classLoader)
    }
}

@Suppress("DEPRECATION")
inline fun <reified T> Parcel.readParcelableData(): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        readParcelable(T::class.java.classLoader, T::class.java)
    } else {
        readParcelable(T::class.java.classLoader)
    }
}

@Suppress("DEPRECATION")
inline fun <reified T: Parcelable> Parcel.readParcelableArrayData(): Array<out Parcelable>? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        readParcelableArray(T::class.java.classLoader, T::class.java)
    } else {
        return readParcelableArray(T::class.java.classLoader)
    }
}