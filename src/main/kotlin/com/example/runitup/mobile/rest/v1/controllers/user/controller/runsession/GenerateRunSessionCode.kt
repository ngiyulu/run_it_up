//package com.example.runitup.mobile.rest.v1.controllers.user.controller.runsession
//
//import com.example.runitup.mobile.constants.AppConstant
//import com.example.runitup.mobile.exception.ApiRequestException
//import com.example.runitup.mobile.model.RunSession
//import com.example.runitup.mobile.rest.v1.controllers.BaseController
//import com.example.runitup.mobile.rest.v1.dto.Actor
//import com.example.runitup.mobile.rest.v1.dto.ActorType
//import com.example.runitup.mobile.rest.v1.dto.RunSessionAction
//import com.example.runitup.mobile.service.NumberGenerator
//import org.slf4j.MDC
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.stereotype.Service
//
//@Service
//class GenerateRunSessionCode: BaseController<GenerateCodeModel, RunSession>() {
//
//    @Autowired
//    lateinit var numberGenerator: NumberGenerator
//
//    override fun execute(request: GenerateCodeModel): RunSession {
//        val run = cacheManager.getRunSession(request.sessionId) ?: throw ApiRequestException(text("invalid_session_id"))
//        if(!run.privateRun){
//            return  run
//        }
//        runSessionEventLogger.log(
//            sessionId = run.id.orEmpty(),
//            action = RunSessionAction.USER_PROMOTED_FROM_WAITLIST,
//            actor = Actor(ActorType.ADMIN, jobId),
//            newStatus = null,
//            reason = "Updating authorization code, olde code = ${run.code.orEmpty()}",
//            correlationId = MDC.get(AppConstant.TRACE_ID),
//            metadata = mapOf(AppConstant.SOURCE to MDC.get(AppConstant.SOURCE))
//        )
//        run.code = numberGenerator.generateCode(5)
//        return cacheManager.updateRunSession(run)
//    }
//}
//
//data class GenerateCodeModel(val code:String, val sessionId:String)