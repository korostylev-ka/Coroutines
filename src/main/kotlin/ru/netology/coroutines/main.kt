package ru.netology.coroutines

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import ru.netology.coroutines.dto.*
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private val gson = Gson()
private const val BASE_URL = "http://127.0.0.1:9999"
private val client = OkHttpClient.Builder()
    .addInterceptor(HttpLoggingInterceptor(::println).apply {
        level = HttpLoggingInterceptor.Level.BODY
    })
    .connectTimeout(30, TimeUnit.SECONDS)
    .build()

fun main() {
    with(CoroutineScope(EmptyCoroutineContext)) {
        launch {
            try {
                //получаем список постов
                val posts = getPosts(client)
                    .map { post ->
                        //получаем автора поста
                        val author = async {
                            getAuthor(client, post.authorId)
                        }.await()
                        //получаем список комментариев c авторами к посту
                        val comments = async {
                            getCommentsWithAuthors(client, post.id)
                        }.await().map {
                            //получаем данные автора комментария
                            val author = getAuthor(client, it.authorId)
                            //копируем комментарий, заменяя null на данные автора
                            it.copy(
                                name = author.name,
                                avatar = author.avatar
                            )
                        }
                        //создаем обьект нового класса для поста, где указаны авторы и комментарии
                        async {
                            PostWithAuthorsAndComments(
                                id = post.id,
                                authorId = post.authorId,
                                name = author.name,
                                avatar =  author.avatar,
                                content = post.content,
                                published = post.published,
                                likedByMe = post.likedByMe,
                                comments = comments
                            )
                        }.await()

                        //возвращает результат по всем параллельныцм задачам в один список
                    }

                println("Список постов с автормами, комментариями и авторами их: $posts")

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    Thread.sleep(30_000L)

}

suspend fun OkHttpClient.apiCall(url: String): Response {
    return suspendCoroutine { continuation ->
        Request.Builder()
            .url(url)
            .build()
            .let(::newCall)
            .enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response)
                }

                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }
            })
    }
}

suspend fun <T> makeRequest(url: String, client: OkHttpClient, typeToken: TypeToken<T>): T =
    withContext(Dispatchers.IO) {
        client.apiCall(url)
            .let { response ->
                if (!response.isSuccessful) {
                    response.close()
                    throw RuntimeException(response.message)
                }
                val body = response.body ?: throw RuntimeException("response body is null")
                gson.fromJson(body.string(), typeToken.type)
            }
    }

//запрос постов
suspend fun getPosts(client: OkHttpClient): List<Post> =
    makeRequest("$BASE_URL/api/slow/posts", client, object : TypeToken<List<Post>>() {})

//запрос комментариев
suspend fun getComments(client: OkHttpClient, id: Long): List<Comment> =
    makeRequest("$BASE_URL/api/slow/posts/$id/comments", client, object : TypeToken<List<Comment>>() {})

//запрос комментариев
suspend fun getCommentsWithAuthors(client: OkHttpClient, id: Long): List<CommentWithAuthor> =
    makeRequest("$BASE_URL/api/slow/posts/$id/comments", client, object : TypeToken<List<CommentWithAuthor>>() {})

//запрос авторов
suspend fun getAuthor(client: OkHttpClient, id: Long): Author =
    makeRequest("$BASE_URL/api/slow/authors/$id", client, object : TypeToken<Author>() {})
