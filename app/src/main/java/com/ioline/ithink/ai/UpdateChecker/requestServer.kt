package com.ioline.ithink.ai.UpdateChecker



import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object requestServer {

     public const val BASE_URL_UPDATE = "http://ioline.no-ip.org/api/repository/"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(TokenInterceptor()) // Token via URL
        .build()


    val instanceUpdate: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_UPDATE)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }



    suspend fun checkUpdate(currentVersionCode: Int): UpdateResult {
        val typeBody = "app".toRequestBody("text/plain".toMediaTypeOrNull())
        val nameBody = "mordomus_tavo".toRequestBody("text/plain".toMediaTypeOrNull())
        //val release_type = "beta".toRequestBody("text/plain".toMediaTypeOrNull())
        return try {

                val response = instanceUpdate.checkAppUpdate(typeBody, nameBody )
                if (response.isSuccessful) {
                    val versionInfo = response.body()
                    if (versionInfo != null && versionInfo.version_code > currentVersionCode) {
                        UpdateResult.UpdateAvailable(
                            versionInfo.version_code,
                            versionInfo.url,
                            versionInfo.version
                        )
                    } else {
                        UpdateResult.AlreadyUpToDate
                    }
                }
                else {
                    if (response.code() == 401) {
                        throw HttpException(response) // <- dispara retry
                    }
                    //val errorMessage = response.errorBody()?.string() ?: "UPDATE->Erro desconhecido"
                    //UpdateResult.Error(errorMessage)
                    UpdateResult.Error
                }
        } catch (e: Exception) {
            Log.e("UPDATE","Erro inesperado: ${e.message ?: "Erro desconhecido"}")
            UpdateResult.Error
        }
    }



}

