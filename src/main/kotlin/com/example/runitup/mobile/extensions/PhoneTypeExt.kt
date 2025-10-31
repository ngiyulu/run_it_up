package com.example.runitup.mobile.extensions

import com.example.runitup.mobile.enum.PhoneType
import constant.HeaderConstants

fun String.convertToPhoneType(): PhoneType{
    if(this == HeaderConstants.ANDROID_TYPE){
        return  PhoneType.ANDROID
    }
    return  PhoneType.IOS
}