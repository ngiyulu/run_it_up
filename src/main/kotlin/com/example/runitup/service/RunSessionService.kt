package com.example.runitup.service

import com.example.runitup.dto.RunUser
import com.example.runitup.model.RunSession
import com.stripe.model.Charge
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RunSessionService(): BaseService(){

    @Autowired
    lateinit var paymentService: PaymentService
    fun joinSession(runUser: RunUser, runSession: RunSession, stripeToken:String, amount:Long): RunSession?{
        val sessionCharge: Charge = paymentService.createHoldChargeWithToken(stripeToken, amount) ?: return null
        runUser.stripeChargeId = sessionCharge.id
        runSession.playersSignedUp.add(runUser)
        for (i in 0 until runUser.guest) {
            val guestUser = RunUser(runUser.name+"$i", null, null, runUser.imageUrl,0, true, runUser.userId.orEmpty())
            runSession.playersSignedUp.add(guestUser)
        }
        return runSession
    }

    fun updateSessionGuest(guest:Int, runUser: RunUser, runSession: RunSession, stripeToken:String, amount:Long): RunSession?{
        val sessionCharge: Charge = paymentService.updateHoldCharge(runUser.stripeChargeId.orEmpty(), amount)?: return  null
        runUser.stripeChargeId = sessionCharge.id
        return runSession
    }


}