package tech.tarakoshka.bridgemich

import coil.intercept.Interceptor
import coil.request.ImageResult

class UrlAuthInterceptor(val token: String) : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val transformedRequest = request.newBuilder()
            .data(request.data)
            .addHeader("Authorization", "Bearer $token")
            .build()
        return chain.proceed(transformedRequest)
    }
}