package ru.netology.coroutines.dto

//класс комментариев и авторов к ним
data class CommentWithAuthor(
    val id: Long,
    val postId: Long,
    val authorId: Long,
    val name: String,
    val avatar: String,
    val content: String,
    val published: Long,
    val likedByMe: Boolean,
    val likes: Int = 0,
)