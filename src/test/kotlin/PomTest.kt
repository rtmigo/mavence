import io.kotest.matchers.booleans.shouldBeTrue
import maven.*
import org.junit.jupiter.api.Test
import stages.build.Dependency

class PomTest {
    @Test
    fun test() {
        val xml = PomData(
            Notation.parse("dev.domain:artifactus:1.2.0"),
            description = "Something",
            GithubRepo("richend", "piedpiper"),
            licenseName = "BSD",
            developers = listOf(
                Developer("Richard Hendricks", "richard@piedpiper.io"),
                Developer("Dinesh Chugtai", "dinesh@piedpiper.io"),
                Developer("Bertram Gilfoyle", "bertram@gilfoyle.me")
            ),
            dependencies = listOf(
                Dependency(
                    Notation.parse("org.mapdb:mapdb:3.0.8"),
                    "runtime"),
                Dependency(
                    Notation.parse("org.apache.commons:commons-collections4:4.4"),
                    "runtime")
            )
        ).toXml()
        xml.contains("artifactus").shouldBeTrue()
        //#println(xml)
    }
}