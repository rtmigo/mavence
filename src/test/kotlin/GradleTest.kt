import io.kotest.matchers.*
import io.kotest.matchers.booleans.shouldBeTrue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import stages.build.*
import java.nio.file.*

class GradleTest {
    @Test
    fun testVersion() = runBlocking {
        Paths.get(".").toGradlew().getGradleVersion()[0].isDigit().shouldBeTrue()
    }

    @Test
    fun testProperties() = runBlocking {
        ArtifactDir(Paths.get(".")).gradleVersion()[0].isDigit().shouldBeTrue()
    }

    @Test
    fun testOwnDependencies() = runBlocking {
        val x = ArtifactDir(Paths.get(".")).dependencies("runtimeClasspath")
        x.filter { it.notation.artifact.string=="kotlin-stdlib-jdk8" }.size.shouldBe(1)
    }
}