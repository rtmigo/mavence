import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Date
import java.text.SimpleDateFormat

plugins {
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
    application

}

group = "io.github.rtmigo"
version = "0.1.0" // -SNAPSHOT

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0") // testing
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0") // testing
    testImplementation("io.kotest:kotest-assertions-core:5.5.0")// testing

    implementation("com.github.pgreze:kotlin-process:1.4") // running processes
    implementation("com.github.aballano:mnemonik:2.1.1") // caching function results

    implementation("com.github.ajalt.clikt:clikt:3.5.0") // parsing cli args
    implementation("org.jsoup:jsoup:1.14.3") // parsing xml
    implementation("org.jline:jline:3.21.0") // terminal size

    implementation("org.redundent:kotlin-xml-builder:1.8.0")  // building XML (unused)
    implementation("io.github.aakira:napier:2.6.1") // logging (unused)

    val ktor = "2.1.2"
    implementation("io.ktor:ktor-client-core:$ktor") // http
    implementation("io.ktor:ktor-client-cio:$ktor") // http
    implementation("io.ktor:ktor-client-auth:$ktor") // http
    implementation("io.ktor:ktor-client-logging:$ktor") // http
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}

// BUILD INFO //////////////////////////////////////////////////////////////////////////////////////

val generateBuildKt = tasks.register("generateBuildKt") {
    doFirst {
        File("src/main/kotlin/Build.kt").writeText(
            """
        // DO NOT EDIT. Generated by Gradle task "${this.name}"
        object Build { 
            const val version = "${project.version}"
            const val date = "${SimpleDateFormat("yyyy-MM-dd").format(Date())}"
        }    
    """.trimIndent())
    }
}

tasks.classes {
    dependsOn(generateBuildKt)
}

// UBER JAR ////////////////////////////////////////////////////////////////////////////////////////

tasks.withType<Jar> {
    // UBER JAR
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    manifest {
        attributes["Main-Class"] = application.mainClass
    }
}

tasks.register<Jar>("uberJar") {
    //// UBER JAR
    archiveFileName.set(project.name + ".uber.jar")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from(sourceSets.main.get().output)
    exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
    dependsOn(configurations.runtimeClasspath)
    from({
             configurations.runtimeClasspath.get()
                 .filter { it.name.endsWith("jar") }
                 .map { zipTree(it) }
         })
    doLast {
        println("JAR size %.2f MB".format(archiveFile.get().asFile.length() / 1_000_000.0))
        println("Run with: alias ${archiveBaseName.get()}='java -jar ${archiveFile.get()}'")
        //#println("OR: java -jar ${archiveFile.get()}")
    }
}