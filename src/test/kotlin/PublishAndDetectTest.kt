import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeGreaterThan
import kotlinx.coroutines.runBlocking
import maven.GroupArtifact
import org.junit.jupiter.api.Test
import stages.build.*
import java.nio.file.Paths

class PublishAndDetectTest {
    @Test
    fun test(): Unit = runBlocking {
        val f = Paths.get("src/test/sample/deployable")
            .toGradlew().publishAndDetect(GroupArtifact.parse("io.github.rtmigo:libr"),"mylib")
        // тут мы уже знаем, что артефакт в ".m2" был обновлён, причём ровно один артефакт
        f.file.toFile().exists().shouldBeTrue()
        // конвертируя в ArtifactDir мы также убедимся, что в каталоге POM и JAR
        f.toMavenArtifactDir().asUnsignedFileset().files.size.shouldBeGreaterThan(2)
    }
}