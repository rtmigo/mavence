import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class DeveloperTest {
    @Test
    fun parseWithEmail() {
        val x = Developer.parse("Richard Hendricks <richard@piedpiper.io>")
        x.name.shouldBe("Richard Hendricks")
        x.email.shouldBe("richard@piedpiper.io")
    }

    @Test
    fun parseWithout() {
        val x = Developer.parse("Richard Hendricks")
        x.name.shouldBe("Richard Hendricks")
        x.email.shouldBe(null)
    }
}