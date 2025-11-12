package com.example.runitup.mobile.rest.v1.controllers.user.controller.runsession

import com.example.runitup.mobile.constants.AppConstant
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.controllers.UserModelType
import com.example.runitup.mobile.rest.v1.dto.Actor
import com.example.runitup.mobile.rest.v1.dto.ActorType
import com.example.runitup.mobile.rest.v1.dto.RunSessionAction
import com.example.runitup.mobile.service.NumberGenerator
import com.example.runitup.mobile.service.RunSessionService
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GenerateRunSessionCode: BaseController<GenerateCodeModel, RunSession>() {

    @Autowired
    lateinit var runSessionService: RunSessionService

    @Autowired
    lateinit var numberGenerator: NumberGenerator

    override fun execute(request: GenerateCodeModel): RunSession {
        val user = getUser()
        var run: RunSession? = cacheManager.getRunSession(request.sessionId) ?: throw ApiRequestException(text("invalid_session_id"))
        if(!run!!.privateRun){
            return  run
        }
        runSessionEventLogger.log(
            sessionId = run.id.orEmpty(),
            action = RunSessionAction.USER_UPDATED_CODE,
            actor = if(user.type == UserModelType.USER) Actor(ActorType.USER, user.userId) else Actor(ActorType.ADMIN, user.userId),
            newStatus = null,
            reason = "Updating authorization code",
            correlationId = MDC.get(AppConstant.TRACE_ID),
            metadata = mapOf(AppConstant.SOURCE to MDC.get(AppConstant.SOURCE))
        )
        val code = numberGenerator.encryptCode(5)
        run.code = code
        run.plain = numberGenerator.decryptEncryptedCode(code)
        cacheManager.updateRunSession(run)
        run = runSessionService.getRunSession(true, run, run.id.orEmpty(), user.userId)
        return run!!
    }
}

data class GenerateCodeModel(val sessionId:String)