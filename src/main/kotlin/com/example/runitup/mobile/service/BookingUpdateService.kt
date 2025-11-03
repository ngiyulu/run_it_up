package com.example.runitup.mobile.service

import com.example.runitup.mobile.model.AuthRole
import com.example.runitup.mobile.model.Booking
import com.example.runitup.mobile.model.BookingChangeEvent
import com.example.runitup.mobile.model.ChangeType
import com.example.runitup.mobile.repository.BookingChangeEventRepository
import com.example.runitup.mobile.repository.PaymentAuthorizationRepository
import com.example.runitup.mobile.rest.v1.dto.session.JoinSessionModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BookingUpdateService {

    @Autowired
     lateinit var authRepo: PaymentAuthorizationRepository

    @Autowired
    lateinit var paymentService: PaymentService



    @Autowired
    lateinit var changeRepo: BookingChangeEventRepository



     fun deltaChange(userId:String, request: JoinSessionModel, booking: Booking): DeltaType{
         val oldPartySize = booking.partySize
         val newPartySize = request.getTotalParticipants()
         println("oldPartySize = $oldPartySize")
         println("newPartySize = $newPartySize")

         return if( oldPartySize == newPartySize){
             DeltaType.SAME
         } else if(newPartySize < oldPartySize){
             DeltaType.DECREASED
         }else {
             DeltaType.INCREASED
         }

     }

}
enum class DeltaType{
    INCREASED, DECREASED, SAME
}