import io.kotest.matchers.shouldBe
import maven.Notation
import org.junit.jupiter.api.Test
import java.net.*


class MavenUrlsTest {
    val notation = Notation.parse("io.github.rtmigo:precise:0.1.0-dev19")
    @Test
    fun test_url_ending_with_slash() {
        MavenUrl(URL("https://s01.oss.sonatype.org/content/repositories/iogithubrtmigo-1018/"))
            .toPomUrl(notation).toString().shouldBe(
                "https://s01.oss.sonatype.org/content/repositories/iogithubrtmigo-1018/" +
                    "io/github/rtmigo/precise/0.1.0-dev19/precise-0.1.0-dev19.pom"
            )
    }

    @Test
    fun test_url_not_ending_with_slash() {
        MavenUrl(URL("https://s01.oss.sonatype.org/content/repositories/iogithubrtmigo-1018"))
            .toPomUrl(notation).toString().shouldBe(
                "https://s01.oss.sonatype.org/content/repositories/iogithubrtmigo-1018/" +
                    "io/github/rtmigo/precise/0.1.0-dev19/precise-0.1.0-dev19.pom"
            )
    }
}