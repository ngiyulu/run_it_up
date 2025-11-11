package com.example.runitup.mobile.enum

inline fun <reified T : Enum<T>> String?.toEnumOrNull(): T? {
    if (this == null) return null
    return enumValues<T>().firstOrNull { it.name.equals(this, ignoreCase = true) }
}