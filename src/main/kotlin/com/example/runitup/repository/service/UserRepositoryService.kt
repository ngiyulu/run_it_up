package com.example.runitup.repository.service

import com.example.runitup.model.RunSession
import com.example.runitup.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserRepositoryService {

    @Autowired
    lateinit var userRepository: UserRepository

    fun deleteSessionFromUser(userId: String, sessionId: String){
        userRepository.findById(userId).orElseThrow { NoSuchElementException("Parent not found") }
            .apply { runSessions.removeAll { it.id.toString() == sessionId || it.hostedBy == userId } }
            .let { userRepository.save(it) }
    }

}