import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.should
import io.kotest.matchers.string.shouldStartWith
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.nio.file.Path

class GradleTest {
    @Test
    fun testVersion() = runBlocking {
        Path.of(".").toGradlew().version()[0].isDigit().shouldBeTrue()
    }

    @Test
    fun testProperties() = runBlocking {
        ArtifactDir(Path.of(".")).gradleVersion()[0].isDigit().shouldBeTrue()
    }
}