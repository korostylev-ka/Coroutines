package ru.netology.coroutines.dto

//Класс для постов, где мы "собираем" воедино данные по разным запросам
data class PostWithAuthorsAndComments(
    val id: Long,
    val authorId: Long,
    val name: String,
    val avatar: String,
    val content: String,
    val published: Long,
    val likedByMe: Boolean,
    val likes: Int = 0,
    var attachment: Attachment? = null,
    val comments: List<CommentWithAuthor>,
)