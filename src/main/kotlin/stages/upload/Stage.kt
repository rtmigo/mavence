package stages.upload

import io.github.aakira.napier.*
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import printHeader
import printerr
import java.net.URI
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

suspend fun authenticate(user: SonatypeUsername, pass: SonatypePassword): HttpClient =
    HttpClient {
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    printerr("# ktor ###\n$message\n# /ktor ##")
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


suspend fun HttpClient.sendToStaging(file: Path): URI {
    //file.copyTo(Path("/home/rtmigo/Lab/Code/kotlin/rtmaven/bundle.jar"))
    printerr("Sending file $file (${file.toFile().length()})")
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
            "Sending failed: ${resp.status}.\n" +
                "${resp.bodyAsText()}"
        }
        printerr("File $file successfully sent")
        //println(resp.bodyAsText())
        val result = Json.decodeFromString<ResponseCreated>(resp.bodyAsText())

        val uri = URI(result.repositoryUris.single())
        printerr("Staging repo URI: $uri")
        uri
    }
}

@Serializable
private data class ResponseCreated(val repositoryUris: List<String>)
