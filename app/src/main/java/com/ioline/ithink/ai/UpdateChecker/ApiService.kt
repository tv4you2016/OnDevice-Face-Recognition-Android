package com.ioline.ithink.ai.UpdateChecker


import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiService {
    data class VersionResponse(
        val version_code: Int,
        val url: String,
        val version : String
    )

    @Multipart
    @POST("version.php")
    suspend fun checkAppUpdate(
        @Part("type") type: RequestBody,
        @Part("name") name: RequestBody
    ): Response<VersionResponse>






}