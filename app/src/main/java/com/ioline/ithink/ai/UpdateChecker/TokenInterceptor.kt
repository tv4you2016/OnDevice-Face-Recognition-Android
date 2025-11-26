package com.ioline.ithink.ai.UpdateChecker


import okhttp3.Interceptor
import okhttp3.Response

class TokenInterceptor : Interceptor {
    companion object {
        var token: String = ""
    }


    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url

        val authToken = token

        val newUrl = if (authToken.isNotEmpty()) {
            originalUrl.newBuilder()
                .addQueryParameter("token", authToken)
                .build()
        } else {
            originalUrl
        }

        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }
}
