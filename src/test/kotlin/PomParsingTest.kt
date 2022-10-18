import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PomParsingTest {
    @Test
    fun parse() {
        //
        val xmlCode = """
            <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
            <modelVersion>4.0.0</modelVersion>
            <groupId>io.github.rtmigo</groupId>
            <artifactId>precise</artifactId>
            <version>0.1.0-dev23</version>
            <packaging>jar</packaging>
            <name>precise</name>
            <description>
            Kotlin/JVM compensated summation of Double sequences to calculate sum, mean, standard deviation
            </description>
            <url>https://github.com/rtmigo/precise_kt#readme</url>
            <licenses>
            <license>
            <name>MIT</name>
            <url>
            https://github.com/rtmigo/precise_kt/blob/master/LICENSE
            </url>
            </license>
            </licenses>
            <developers>
            <developer>
            <name>Artsiom iG</name>
            <email>ortemeo@gmail.com</email>
            </developer>
            </developers>
            <scm>
            <connection>scm:git://github.com/rtmigo/precise_kt.git</connection>
            <url>https://github.com/rtmigo/precise_kt</url>
            </scm>
            <dependencies>
            <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-stdlib-jdk8</artifactId>
            <version>1.7.20</version>
            <scope>runtime</scope>
            </dependency>
            </dependencies>
            </project>            
        """.trimIndent().trim()
        val p = PomXml(xmlCode)
        p.group().string.shouldBe("io.github.rtmigo")
        p.artifact().string.shouldBe("precise")
        p.version().string.shouldBe("0.1.0-dev23")
    }
}