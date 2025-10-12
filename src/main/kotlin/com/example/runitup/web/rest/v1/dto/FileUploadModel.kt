package com.example.runitup.web.rest.v1.dto

import org.springframework.web.multipart.MultipartFile

class FileUploadModel (val file: MultipartFile, val gymId: String?)