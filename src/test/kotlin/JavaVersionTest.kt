import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tools.javaVersionToInt

class JavaVersionTest {
    @Test
    fun test() {
        javaVersionToInt("1.6").shouldBe(6)
        javaVersionToInt("1.8").shouldBe(8)
        javaVersionToInt("1.8.345").shouldBe(8)
        javaVersionToInt("19").shouldBe(19)
        javaVersionToInt("9.1").shouldBe(9)

        shouldThrow<IllegalArgumentException> {
            javaVersionToInt("")
        }
        shouldThrow<IllegalArgumentException> {
            javaVersionToInt("labuda")
        }
        shouldThrow<IllegalArgumentException> {
            javaVersionToInt("1.a.2")
        }
    }
}