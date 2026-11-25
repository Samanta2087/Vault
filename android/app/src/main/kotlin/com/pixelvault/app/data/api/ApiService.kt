package com.pixelvault.app.data.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ApiService {

    // Auth
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body body: Map<String, String>): Response<LoginResponse>

    @POST("api/auth/logout")
    suspend fun logout(): Response<Unit>

    // Chunked Resumable Upload
    @POST("api/media/upload/init")
    suspend fun initUpload(@Body request: UploadInitRequest): Response<UploadInitResponse>

    @PATCH("api/media/upload/{id}")
    suspend fun uploadChunk(
        @Path("id") uploadId: String,
        @Header("Content-Range") contentRange: String,
        @Body chunk: RequestBody
    ): Response<Unit>

    @POST("api/media/upload/{id}/complete")
    suspend fun completeUpload(@Path("id") uploadId: String): Response<MediaItemDto>

    // Media
    @GET("api/media")
    suspend fun getMediaTimeline(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("favorite") favorite: Boolean? = null,
        @Query("archived") archived: Boolean? = null
    ): Response<MediaPageDto>

    @GET("api/media/{id}")
    suspend fun getMediaById(@Path("id") id: String): Response<MediaItemDto>

    @PUT("api/media/{id}/favorite")
    suspend fun toggleFavorite(@Path("id") id: String): Response<Unit>

    @PUT("api/media/{id}/archive")
    suspend fun toggleArchive(@Path("id") id: String): Response<Unit>

    @DELETE("api/media/{id}")
    suspend fun softDeleteMedia(@Path("id") id: String): Response<Unit>

    @GET("api/media/signed-url/{id}")
    suspend fun getSignedUrl(@Path("id") id: String): Response<SignedUrlResponse>

    @GET("api/media/{id}/thumbnail")
    @Streaming
    suspend fun getThumbnail(@Path("id") id: String): Response<ResponseBody>
}
