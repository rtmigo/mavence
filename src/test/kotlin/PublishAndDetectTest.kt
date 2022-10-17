import io.kotest.matchers.booleans.shouldBeTrue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.io.path.exists

class PublishAndDetectTest {
    @Test
    fun test(): Unit = runBlocking {
        val f = Paths.get("src/test/sample/deployable")
            .toGradlew().publishAndDetect("earth")
        // тут мы уже знаем, что артифакт был обновлён, причём ровно один
        f.file.exists().shouldBeTrue()
        // конвертируя в ArtifactDir мы также убедимся, что в каталоге POM и JAR
        f.toMavenArtifactDir().path.exists().shouldBeTrue()
    }
}