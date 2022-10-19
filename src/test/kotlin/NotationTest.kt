import io.kotest.matchers.shouldBe
import maven.Notation
import org.junit.jupiter.api.Test

class NotationTest {
    @Test
    fun test() {
        val n = Notation.parse("dev.domain:artifactus:1.2.0")
        n.group.string.shouldBe("dev.domain")
        n.artifact.string.shouldBe("artifactus")
        n.version.string.shouldBe("1.2.0")
    }

    @Test
    fun testToString() {
        val n = Notation.parse("dev.domain:artifactus:1.2.0")
        n.toString().shouldBe("dev.domain:artifactus:1.2.0")
    }
}