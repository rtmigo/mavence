import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Date
import java.text.SimpleDateFormat

plugins {
    kotlin("jvm") version "1.7.20"
    application
}

group = "io.github.rtmigo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("org.redundent:kotlin-xml-builder:1.8.0")
    implementation("com.github.pgreze:kotlin-process:1.4")
    implementation("com.github.aballano:mnemonik:2.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    //implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
    implementation("com.github.ajalt.clikt:clikt:3.5.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
    testImplementation("io.kotest:kotest-assertions-core:5.5.0")
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

//application {
//    mainClass.set("rtmaven.MainKt")
//}

tasks.withType<Jar> {
    // UBER JAR
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    manifest {
        attributes["Main-Class"] = application.mainClass
    }
}

val bi = tasks.register("buildInfo") {
    doFirst {
        File("src/main/kotlin/Build.kt").writeText(
            """
        // DO NOT EDIT. GENERATED BY GRADLE TASK
        object Build { 
            const val version = "${project.version}"
            const val date = "${SimpleDateFormat("yyyy-MM-dd").format(Date())}"
        }    
    """.trimIndent())
    }
}

tasks.classes {
    dependsOn(bi)
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
        println("Run with: java -jar ${archiveFile.get()}")
    }
}