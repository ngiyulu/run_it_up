package com.example.runitup.mobile.exception

class ApiUnauhorizedException: RuntimeException {
    constructor( message:String):super(message)
    constructor(message:String, throwable:Throwable): super(message, throwable)


}