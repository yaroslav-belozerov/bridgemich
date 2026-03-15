package tech.tarakoshka.bridgemich

import coil.intercept.Interceptor
import coil.request.ImageResult

fun authHeader(token: String, isApiKey: Boolean): Pair<String, String> =
    if (isApiKey) "x-api-key" to token else "Authorization" to "Bearer $token"

class UrlAuthInterceptor(val token: String, val isApiKey: Boolean = false) : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val (headerName, headerValue) = authHeader(token, isApiKey)
        val transformedRequest = request.newBuilder()
            .data(request.data)
            .addHeader(headerName, headerValue)
            .build()
        return chain.proceed(transformedRequest)
    }
}