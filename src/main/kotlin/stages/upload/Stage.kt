package stages.upload

import io.ktor.client.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import eprintHeader
import eprint
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.*


fun createClient(user: SonatypeUsername, pass: SonatypePassword): HttpClient =
    HttpClient {
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    eprint("# ktor ###\n$message\n# /ktor ##")
                    //Napier.v(message+"\n", null, "Ktor") // message + "\n\n"
                }
            }
            level = LogLevel.INFO
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

@JvmInline
value class StagingUri(val uri: URI) {
    init {
        require(uri.host.contains("oss.sonatype.org"))
    }

    val id get() = StagingRepoId(this.uri.path.split('/').last { it.isNotEmpty() })
}

/** When upload to staging succeeds, we will receive this object as JSON body. */
@Serializable
private data class StagingRepoCreatedResponse(val repositoryUris: List<String>)


suspend fun HttpClient.sendToStaging(file: Path): StagingUri {
    require(file.name.endsWith(".jar"))
    eprintHeader("Sending $file (${file.toFile().length()}) to Staging")
    return this.submitFormWithBinaryData(
        url = "https://s01.oss.sonatype.org/service/local/staging/bundle_upload",
        formData = formData {
            append("file", file.readBytes(),
                   headers = Headers.build {
                       set("Content-Type", "application/java-archive")
                       set(
                           "Content-Disposition",
                           """form-data; name="file"; filename="bundle.jar"""")
                   })
        }) {
    }.let { resp ->
        check(resp.status.value == 201) {
            "Sending failed: ${resp.status}.\n${resp.bodyAsText()}"
        }
        eprint("File $file successfully sent")
        //println(resp.bodyAsText())
        val result = Json.decodeFromString<StagingRepoCreatedResponse>(resp.bodyAsText())
        val uri = URI(result.repositoryUris.single())
        eprint("Staging repo URI: $uri")
        StagingUri(uri)
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


suspend fun HttpClient.promoteToCentral(uri: StagingUri) {
    eprintHeader("Promoting Staging to Release")
    this.post("https://s01.oss.sonatype.org/service/local/staging/bulk/promote") {
        setBody(
            Json.encodeToString(
                BulkPromoteRequest(
                    BulkPromoteRequest.BulkPromoteRequestData(
                        stagedRepositoryIds = listOf(uri.id)))
            ))
    }.let { resp ->
        check(resp.status.isSuccess()) {
            "Failed to promote:\n${resp.status}\n${resp.bodyAsText()}"
        }
    }
}
