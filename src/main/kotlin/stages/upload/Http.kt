package stages.upload

import MavenUrl

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import maven.Notation
import toPomUrl
import tools.*
import java.net.*
import java.nio.file.Path
import java.text.*
import kotlin.io.path.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes


private fun insecureHttpLogging() = System.getenv("INSECURE_HTTP_LOGGING") == "1"

fun createClient(user: SonatypeUsername, pass: SonatypePassword): HttpClient =
    HttpClient {

        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 10)
            exponentialDelay()
            retryOnException(maxRetries = 10) // HttpTimeout выбрасывает исключения
        }

        install(HttpTimeout) {
            this.requestTimeoutMillis = 10*60*1000
            this.connectTimeoutMillis = 5*1000
            this.socketTimeoutMillis = this.connectTimeoutMillis
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    eprint("# ktor ###\n$message\n# /ktor ##")
                }
            }
            level = if (insecureHttpLogging())
                LogLevel.HEADERS
            else
                LogLevel.INFO // avoid exposing credentials in public logs
        }

        install(Auth) {
            // В https://bit.ly/3s8d5uO и https://bit.ly/3eIWLgS более прямые обращения к странице
            // логина. А здесь переписано более метауровнево - это "базовая" аутентификация,
            // её умеет делать плагин, и её можно автоматически сделать одновременно с отправкой
            basic {
                this.credentials {
                    BasicAuthCredentials(username = user.string, password = pass.string)
                }
                sendWithoutRequest { request ->
                    request.url.host == "s01.oss.sonatype.org" // эти реквизиты только для Sonatype
                }
            }
        }
    }//.also { Napier.base(DebugAntilog()) }


@JvmInline
@Serializable
value class StagingRepoId(val string: String)


class StagingUri(url: URL) : MavenUrl(url) {
    init {
        require(url.host.contains("oss.sonatype.org"))
    }

    val id get() = StagingRepoId(this.url.path.split('/').last { it.isNotEmpty() })
}

/** When upload to staging succeeds, we will receive this object as JSON body. */
@Serializable
private data class StagingRepoCreatedResponse(val repositoryUris: List<String>)


fun humanReadableByteCountSI(bytes: Long): String {
    var bytes = bytes
    if (-1000 < bytes && bytes < 1000) {
        return "$bytes B"
    }
    val ci: CharacterIterator = StringCharacterIterator("kMGTPE")
    while (bytes <= -999950 || bytes >= 999950) {
        bytes /= 1000
        ci.next()
    }
    return String.format("%.1f %cB", bytes / 1000.0, ci.current())
}

suspend fun HttpClient.sendToStaging(file: Path, notation: Notation): StagingUri {

    require(file.name.endsWith(".jar"))
    eprintHeader(
        "Uploading ${file.name} " +
            "(${humanReadableByteCountSI(file.toFile().length())}) to Staging")
    return this.submitFormWithBinaryData(
        url = "https://s01.oss.sonatype.org/service/local/staging/bundle_upload",
        formData = formData {
            append("file.jar", file.readBytes(),
                   headers = Headers.build {
                       set(
                           "Content-Disposition",
                           """form-data; name="file.jar"; filename="file.jar"""")
                   }
            )
        }) {
    }.let { resp ->
        check(resp.status.value == 201) {
            "Sending failed: ${resp.status}.\n${resp.bodyAsText()}"
        }
        eprint("File ${file.name} sent successfully")

        val result = Json.decodeFromString<StagingRepoCreatedResponse>(resp.bodyAsText())
        val uri = URI(result.repositoryUris.single())
        eprint()
        eprint("Staging repo:\n$uri")
        eprint()
        val resultUrl = StagingUri(uri.toURL())
        this.waitForUri(resultUrl, notation)
        resultUrl
    }
}

@Serializable
private data class BulkPromoteRequest(
    val data: BulkPromoteRequestData,
) {
    @Serializable
    data class BulkPromoteRequestData(
        val autoDropAfterRelease: Boolean = true,
        val description: String = "",
        val stagedRepositoryIds: List<StagingRepoId>,
    )
}

suspend fun HttpClient.waitForUri(
    uri: StagingUri,
    notation: Notation,
    maxWait: kotlin.time.Duration = 2.minutes,
) {
    val pomUrl = uri.toPomUrl(notation)
    eprint("POM URL: $pomUrl")
    eprint()

    suspend fun delayWithDots(attempt: Int) =
        repeat(attempt) {
            delay((attempt * 200).milliseconds)
            System.err.print('.')
        }

    val startMs = System.currentTimeMillis()

    for (attempt in (1..Int.MAX_VALUE)) {
        System.err.print("($attempt) Requesting POM... ")
        if (this.get(uri.toPomUrl(notation)).status.isSuccess()) {
            System.err.println("SUCCESS!!")
            break
        }
        System.err.print("Not yet. ")

        if ((System.currentTimeMillis() - startMs) < maxWait.inWholeMilliseconds) {
            System.err.print("Sleeping")
            delayWithDots(attempt.coerceAtMost(15))
            System.err.println()
        } else
            error("Failed to get the artifact. Maybe you uploaded non-unique version?")
    }
}


private val json = Json { encodeDefaults = true }

suspend fun HttpClient.promoteToCentral(uri: StagingUri) {
    eprintHeader("Promoting Staging to Release")
    this.post("https://s01.oss.sonatype.org/service/local/staging/bulk/promote") {
        setBody(
            json.encodeToString(
                BulkPromoteRequest(
                    BulkPromoteRequest.BulkPromoteRequestData(
                        stagedRepositoryIds = listOf(uri.id)))
            ))
        headers {
            contentType(ContentType.Application.Json)
        }
    }
        .let { resp ->
            check(resp.status.isSuccess()) {
                "Failed to promote:\n${resp.status}\n${resp.bodyAsText()}"
            }
        }
}
