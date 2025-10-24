package com.example.runitup.mobile.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import java.time.LocalDate

class Waiver(
    @Id var id: String? = ObjectId().toString(),
    var userId: String = " ",
    var url:String = "",
    var approvedAt: LocalDate? = null,
    var status: WaiverStatus = WaiverStatus.PENDING,
    var note:String = "",
    var approvedBy: String? = null,
    var user: User? = null
):BaseModel(){

    fun approve(adminId:String, isApproved:Boolean, rejectionNotes:String){
        approvedAt = LocalDate.now()
        approvedBy = adminId
        status = if(isApproved)  WaiverStatus.APPROVED  else WaiverStatus.REJECTED
        note = if(isApproved) "" else rejectionNotes
    }
}

enum class WaiverStatus{
    APPROVED, REJECTED, PENDING
}