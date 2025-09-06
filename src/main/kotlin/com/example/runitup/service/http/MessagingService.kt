package com.example.runitup.service.http

import ServiceResult
import com.example.runitup.model.User
import com.ngiyulu.runitup.messaging.runitupmessaging.dto.conversation.CreateConversationModel
import com.ngiyulu.runitup.messaging.runitupmessaging.dto.conversation.CreateParticipantModel
import constant.ServiceConstant
import model.messaging.Conversation
import model.messaging.MessagingUser
import model.messaging.Participant
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono


@Service
class MessagingService {

    @Autowired
    @Qualifier(ServiceConstant.messagingService) lateinit var  client: WebClient

    fun hello():Mono<ServiceResult<String>>{
        return client.get()
            .uri("/api/v1/user/hello")
            .accept(MediaType.APPLICATION_JSON)
            .exchangeToMono { resp ->
                if (resp.statusCode().is2xxSuccessful) {
                    resp.bodyToMono(String::class.java).map { ServiceResult.ok(it) }
                } else {
                    resp.bodyToMono(String::class.java).defaultIfEmpty("")
                        .map { body ->
                            ServiceResult.err(
                                status = resp.statusCode().value(),
                                source = "service-a",
                                message = "Downstream returned ${resp.statusCode()}",
                                body = body
                            )
                        }
                }
            }
            .onErrorResume { ex ->
                Mono.just(
                    ServiceResult.err(
                        status = -1,
                        source = "service-a",
                        message = ex.message ?: ex.javaClass.simpleName
                    )
                )
            }
    }



    fun createConversation(conversation: CreateConversationModel):Mono<ServiceResult<Conversation>>{
        return client.post()
            .uri("/api/v1/conversation/create")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(conversation)
            .exchangeToMono { resp ->
                if (resp.statusCode().is2xxSuccessful) {
                    resp.bodyToMono(Conversation::class.java).map { ServiceResult.ok(it) }
                } else {
                    resp.bodyToMono(String::class.java).defaultIfEmpty("")
                        .map { body ->
                            ServiceResult.err(
                                status = resp.statusCode().value(),
                                source = "service-a",
                                message = "Downstream returned ${resp.statusCode()}",
                                body = body
                            )
                        }
                }
            }
            .onErrorResume { ex ->
                Mono.just(
                    ServiceResult.err(
                        status = -1,
                        source = "service-a",
                        message = ex.message ?: ex.javaClass.simpleName
                    )
                )
            }
    }



    fun createUser(user: MessagingUser):Mono<ServiceResult<MessagingUser>>{
        return client.post()
            .uri("/api/v1/user/create")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(user)
            .exchangeToMono { resp ->
                if (resp.statusCode().is2xxSuccessful) {
                    resp.bodyToMono(MessagingUser::class.java).map { ServiceResult.ok(it) }
                } else {
                    resp.bodyToMono(String::class.java).defaultIfEmpty("")
                        .map { body ->
                            ServiceResult.err(
                                status = resp.statusCode().value(),
                                source = "service-a",
                                message = "Downstream returned ${resp.statusCode()}",
                                body = body
                            )
                        }
                }
            }
            .onErrorResume { ex ->
                Mono.just(
                    ServiceResult.err(
                        status = -1,
                        source = "service-a",
                        message = ex.message ?: ex.javaClass.simpleName
                    )
                )
            }
    }


    fun createParticipant(model: CreateParticipantModel):Mono<ServiceResult<Participant>>{
        return client.post()
            .uri("/api/v1/conversation/participant/add")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(model)
            .exchangeToMono { resp ->
                if (resp.statusCode().is2xxSuccessful) {
                    resp.bodyToMono(Participant::class.java).map { ServiceResult.ok(it) }
                } else {
                    resp.bodyToMono(String::class.java).defaultIfEmpty("")
                        .map { body ->
                            ServiceResult.err(
                                status = resp.statusCode().value(),
                                source = "service-a",
                                message = "Downstream returned ${resp.statusCode()}",
                                body = body
                            )
                        }
                }
            }
            .onErrorResume { ex ->
                Mono.just(
                    ServiceResult.err(
                        status = -1,
                        source = "service-a",
                        message = ex.message ?: ex.javaClass.simpleName
                    )
                )
            }
    }




}