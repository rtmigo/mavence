import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NotationTest {
    @Test
    fun test() {
        val n = Notation.parse("dev.domain:artifactus:1.2.0")
        n.group.string.shouldBe("dev.domain")
        n.artifact.string.shouldBe("artifactus")
        n.version.string.shouldBe("1.2.0")
    }
}