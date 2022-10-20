# [mavence](https://github.com/rtmigo/mavence) # experimental

CLI utility for publishing Gradle projects (Kotlin, Java, etc.) to Maven
Central.

This essentially does the same thing as
the [Signing](https://docs.gradle.org/current/userguide/signing_plugin.html) and
[Nexus](https://github.com/gradle-nexus/publish-plugin) plugins.

<details><summary>Why not use plugins?</summary>

- Building locally
- Publishing somewhere

These tasks are almost unrelated.

By placing publishing logic in a build script, you make the foundation of the
project complex, big and ugly.

However, we still use some Gradle plugins. The `maven-publish` creates, i.e.
builds a local copy of the Maven package.

</details>

## Install and run

Just get the
latest [mavence.jar](https://github.com/rtmigo/mavence/releases/latest/download/mavence.jar)
from the [releases page](https://github.com/rtmigo/mavence/releases).

Run with

```bash
java -jar mavence.jar
```

## Setting the environment

Before publishing, you will need to set the following four environment
variables:

| variable             | wtf                                                       |
|----------------------|-----------------------------------------------------------|
| `SONATYPE_USERNAME`  | Username for Sonatype JIRA (optionally replaced by token) |
| `SONATYPE_PASSWORD`  | Password for Sonatype JIRA (optionally replaced by token) |
| `MAVEN_GPG_KEY`      | Locally generated private key in ASCII armor              |  
| `MAVEN_GPG_PASSWORD` | Password protecting the private key                       |

<details><summary>Here where to get them</summary>

There is no document in the universe yet that would describe the process in
detail, but without imposing too much. So prepare to a dull journey to the dusty
circles of hell.

1. You need to [register](https://getstream.io/blog/publishing-libraries-to-mavencentral-2021/#registering-a-sonatype-account)
   on the [Sonatype Jira](https://issues.sonatype.org/secure/Dashboard.jspa)
   and chat with bots, until they **verify** that you can publish a package.
   That gives you `SONATYPE_USERNAME` and `SONATYPE_PASSWORD` you can use for
   publishing.

2. You generate GPG keys in your own terminal. At that point, they are just
   files. It remains to figure out what are **public**, **private** keys and
   what is a **password**. The public key must be sent 
   to [a keyserver](https://unix.stackexchange.com/a/692097), and the
   private and password are to be exported to variables `MAVEN_GPG_KEY`
   and `MAVEN_GPG_PASSWORD`.

</details>

## Minimal configuration

We still use Gradle as a base as it makes the project compatible.

#### build.gradle.kts

```kotlin
plugins {
    id("java-library")
    id("maven-publish")
}

java {
    withSourcesJar()
    withJavadocJar()
}

group = "my.domain"
version = "0.1.2"

publishing {
    publications {
        create<MavenPublication>("thelib") {
            from(components["java"])
            pom {
                val repo = "thelib"
                val owner = "doe"

                name.set("thelib")
                description.set("There are dumber things than copy-pasting")
                url.set("https://github.com/$owner/$repo")

                developers {
                    developer {
                        name.set("John Doe")
                        email.set("doe@sample.com")
                    }
                }
                scm {
                    connection.set("scm:git://github.com/$owner/$repo.git")
                    url.set("https://github.com/$owner/$repo")
                }
                licenses {
                    license {
                        name.set("Apache 2.0 License")
                        url.set("https://github.com/$owner/$repo/blob/HEAD/LICENSE")
                    }
                }
            }
        }
    }
}
```

#### settings.gradle.kts

```kotlin
rootProject.name = "thelib"
```

### Package name

The published package will have a version like `my.domain:thelib:0.1.2`.

<details><summary>Group and Version</summary>

It is the first and third part of `my.domain:thelib:0.1.2`,
i.e. `my.domain`
and `0.1.2`.

They can be defined in `build.gradle.kts` like that:

```kotlin
group = "my.domain"
version = "0.1.2"
```

</details>

<details><summary>Artifact</summary>

It is the second part of `my.domain:thelib:1.0.0`, i.e. `thelib`.

`mavence` takes it
from [archivesBaseName](https://docs.gradle.org/current/dsl/org.gradle.api.Project.html#org.gradle.api.Project:archivesBaseName)
Gradle property.

#### If we release the root project:

```
thelib/                   <<< dir name will be the artifact name 
    src/
    build.gradle.kts
    settings.gradle.kts   <<< unless redefined here
```

The redefine the root project name, add the following:

```kotlin
// settings.gradle.kts

rootProject.name = "newname"
```     

#### If we release a subproject:

```
myrootproject/ 
    thelib/               <<< dir name will be the artifact name
        src/
        build.gradle.kts
    settings.gradle.kts    
```

</details>

## Keep in mind

If sending a package fails for any reason, try to edit meta-data.
Sonatype servers do not return meaningful error responses. They can simply
return a "server error" code, or accept the package but silently ignore it.

## Publishing

### Publish to Maven Central

Set environment variables `MAVEN_GPG_KEY`, `MAVEN_GPG_PASSWORD`
, `SONATYPE_USERNAME`, `SONATYPE_PASSWORD` and run:

```bash
cd /path/to/thelib
java -jar mavence.jar central io.github.doe:thelib 
```

This single command will do all the necessary work: build, signing, staging
and release.

### Publish to Maven Local

This will place the package file inside the local `~/.m2` directory. This way
you can
test the package without sending it anywhere.

```bash
cd /path/to/thelib
java -jar mavence.jar local io.github.doe:thelib 
```

### Publish to Staging

Set environment variables `MAVEN_GPG_KEY`, `MAVEN_GPG_PASSWORD`
, `SONATYPE_USERNAME`, `SONATYPE_PASSWORD` and run:

```bash
cd /path/to/thelib
java -jar mavence.jar stage io.github.doe:thelib 
```

This will push the package to
a [temporary remote repository](https://s01.oss.sonatype.org/content/repositories/)
.
This way you can test the package without sending it to Central.

## Testing before publishing

Although the utility prints quite a lot, `stdout` remains clean and only
prints the result as JSON.

Bash:

```bash
JSON=$(java -jar mavence.jar local my.domain:thelib)

echo $JSON
```

Output:

```json
{
  "group": "my.domain",
  "artifact": "thelib",
  "version": "0.1.2",
  "notation": "my.domain:thelib:0.1.2",
  "mavenRepo": "file:///home/doe/.m2"
}
```

Using this data, you can test the package before it is sent.

I usually use Python and [tempground](https://pypi.org/project/tempground/) for
such testing.

## License

Copyright © 2022 [Artsiom iG](https://github.com/rtmigo).
Released under the [ISC License](LICENSE).