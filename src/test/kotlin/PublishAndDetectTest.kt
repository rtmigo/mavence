import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeGreaterThan
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import stages.build.*
import java.nio.file.Paths

class PublishAndDetectTest {
    @Test
    fun test(): Unit = runBlocking {
        val f = Paths.get("src/test/sample/deployable")
            .toGradlew().publishAndDetect("earth")
        // тут мы уже знаем, что артефакт в ".m2" был обновлён, причём ровно один артефакт
        f.file.exists().shouldBeTrue()
        // конвертируя в ArtifactDir мы также убедимся, что в каталоге POM и JAR
        f.toMavenArtifactFiles().files.size.shouldBeGreaterThan(2)
    }
}