package com.example.runitup.exception

class ApiRequestException: RuntimeException {
    constructor( message:String):super(message)
    constructor(message:String, throwable:Throwable): super(message, throwable)


}