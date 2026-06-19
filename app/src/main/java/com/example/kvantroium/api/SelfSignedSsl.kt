package com.example.kvantroium.api

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Доверяет самоподписанным сертификатам на внутренних API-серверах.
 * Адрес сервера задаётся пользователем, поэтому pinning одного CA здесь невозможен.
 */
object SelfSignedSsl {
    private val trustManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    val socketFactory: SSLSocketFactory = SSLContext.getInstance("TLS")
        .apply { init(null, arrayOf<TrustManager>(trustManager), SecureRandom()) }
        .socketFactory

    val hostnameVerifier = HostnameVerifier { _, _ -> true }
}
