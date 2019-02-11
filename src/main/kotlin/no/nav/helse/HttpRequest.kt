package no.nav.helse

import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.call.receive
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import io.ktor.http.*
import io.prometheus.client.Histogram
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URL

private val logger: Logger = LoggerFactory.getLogger("nav.HttpRequest")

object HttpRequest {
    suspend inline fun <reified T>monitored(
        httpClient: HttpClient,
        httpRequest: HttpRequestBuilder,
        expectedStatusCodes : List<HttpStatusCode> = listOf(HttpStatusCode.OK),
        histogram: Histogram? = null) : T {
        val builtHttpRequest =  httpRequest.build()
        var httpResponse : HttpResponse? = null
        try {
            httpResponse = httpClient.call(httpRequest).response
            if (expectedStatusCodes.contains(httpResponse.status)) {
                try {
                    return httpResponse.receive()
                } catch (cause: Throwable) {
                    throw HttpRequestException("Klarte ikke å mappe respons fra '${builtHttpRequest.method.value}@${builtHttpRequest.url}'. Mottok respons '${httpResponse.readText()}'", cause)
                }
            } else {
                throw HttpRequestException("Mottok uventet '${httpResponse.status}' fra '${builtHttpRequest.method.value}@${builtHttpRequest.url}' med melding '${httpResponse.readText()}'")
            }
        } catch (cause: HttpRequestException) {
            throw cause
        } catch (cause: Throwable) {
            throw HttpRequestException("Kommunikasjonsfeil mot '${builtHttpRequest.method.value}@${builtHttpRequest.url}'", cause)
        } finally {
            histogram?.startTimer()?.observeDuration()
            try {
                httpResponse?.close()
            } catch (ignore: Throwable) {}
        }
    }

    private fun Throwable.getRootCause() : Throwable {
        var rootCause= this

        while (rootCause.cause != null && rootCause.cause != rootCause) {
            rootCause = rootCause.cause!!
        }

        return rootCause
    }

    class HttpRequestException : RuntimeException {
        constructor(message: String) : super(message)
        constructor(message: String, cause: Throwable) : super(message, cause.getRootCause())
    }

    fun buildURL(
        baseUrl: URL,
        pathParts: List<String> = listOf(),
        queryParameters: Map<String, String> = mapOf()
    ): URL {
        val withBasePath= mutableListOf(baseUrl.path)
        withBasePath.addAll(pathParts)

        val parametersBuilder = ParametersBuilder()
        queryParameters.forEach { queryParameter ->
            parametersBuilder.append(queryParameter.key, queryParameter.value)
        }

        val urlBuilder = URLBuilder(
            parameters = parametersBuilder
        )
            .takeFrom(baseUrl.toString())
            .trimmedPath(withBasePath)

        val url = urlBuilder.build().toURI().toURL()
        logger.info("Built URL '$url'")
        return url
    }

    private fun URLBuilder.trimmedPath(pathParts : List<String>): URLBuilder  {
        val trimmedPathParts = mutableListOf<String>()
        pathParts.forEach { part ->
            if (part.isNotBlank()) {
                trimmedPathParts.add(part.trimStart('/').trimEnd('/'))
            }
        }
        return path(trimmedPathParts)
    }

}