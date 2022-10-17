import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class FindSingleChangeTest {
    @Test
    fun find() {
        keysOfChanges(
            mapOf("a" to 1, "b" to 2, "c" to 3),
            mapOf("a" to 1, "b" to 5, "c" to 3),
        ).shouldBe(listOf("b"))
    }

    @Test
    fun nothingChanged() {
        keysOfChanges(
            mapOf("a" to 1, "b" to 2, "c" to 3),
            mapOf("a" to 1, "b" to 2, "c" to 3),
        ).shouldBe(listOf())

    }

    @Test
    fun moreThanOneChanged() {
        keysOfChanges(
            mapOf("a" to 1, "b" to 2, "c" to 3),
            mapOf("a" to 10, "b" to 20, "c" to 3),
        ).shouldBe(listOf("a", "b"))
    }

}