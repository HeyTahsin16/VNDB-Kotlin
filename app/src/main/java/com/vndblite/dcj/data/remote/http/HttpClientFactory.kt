package com.vndblite.dcj.data.remote.http

import com.vndblite.dcj.core.AppInfo
import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.OkHttpClient

/**
 * Factory for the shared [OkHttpClient] with bounded timeouts (N4) and an interceptor
 * that attaches the honest `VNDBKotlin/<versionName>` User-Agent on every request (R10).
 */
object HttpClientFactory {
    const val CONNECT_TIMEOUT_SECONDS: Long = 10L
    const val READ_WRITE_TIMEOUT_SECONDS: Long = 20L
    const val CALL_TIMEOUT_SECONDS: Long = 30L
    const val HEADER_USER_AGENT: String = "User-Agent"

    fun create(
        userAgent: String = AppInfo.userAgent
    ): OkHttpClient {
        val userAgentInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header(HEADER_USER_AGENT, userAgent)
                .build()
            chain.proceed(request)
        }
        return OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(READ_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(userAgentInterceptor)
            .build()
    }
}
