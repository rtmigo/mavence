import io.kotest.matchers.*
import io.kotest.matchers.booleans.shouldBeTrue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import stages.build.*
import java.nio.file.*
import kotlin.io.path.absolute

class GradleTest {
    @Test
    fun testVersion() = runBlocking {
        Paths.get(".").absolute().toGradlew().getGradleVersion()[0].isDigit().shouldBeTrue()
    }

    @Test
    fun testProperties() = runBlocking {
        ArtifactDir(Paths.get(".").absolute()).gradleVersion()[0].isDigit().shouldBeTrue()
    }

    @Test
    fun testOwnDependencies() = runBlocking {
        val x = ArtifactDir(Paths.get(".").absolute()).dependencies("runtimeClasspath")
        x.filter { it.notation.artifact.string=="kotlin-stdlib-jdk8" }.size.shouldBe(1)
    }
}