package ru.netology.coroutines.dto

data class Attachment(
    val url: String,
    val description: String,
    val type: String,
)

data class AttachmentType(
    val type: String
)