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

```
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

<details><summary>Where to get Sonatype variables</summary>

You need
to [register](https://getstream.io/blog/publishing-libraries-to-mavencentral-2021/#registering-a-sonatype-account)
on the [Sonatype Jira](https://issues.sonatype.org/secure/Dashboard.jspa)
and chat with bots, until they **verify** that you can publish a package.
That gives you `SONATYPE_USERNAME` and `SONATYPE_PASSWORD` you can use for
publishing.



</details>
<details><summary>Where to get GPG variables</summary>

### Generate key (it gives you `MAVEN_GPG_PASSWORD`)

```bash
$ gpg --gen-key
```

`gpg` will interactively prompt you to choose a password for the new key. It is
this password that should later be placed in the variable `MAVEN_GPG_PASSWORD`.

### See your private key (it gives you `MAVEN_GPG_KEY`)

```bash
$ gpg --list-keys
```

```
pub   rsa3072 2022-10-18 [SC]
      1292EC426424C9BA0A581EE060C994FDCD3CADBD       << this is the ID
uid           [ultimate] John Doe <doe@example.com>
sub   rsa3072 2022-10-18 [E]
```


```bash
$ gpg --export-secret-keys --armor 1292EC426424C9BA0A581EE060C994FDCD3CADBD
```

```
-----BEGIN PGP PRIVATE KEY BLOCK-----

lQWGBGNOko0BDACzxxMh4EwjlOBRuV94reQglPp5Chzdw4yJHKBYffGGCy27nmde
Q05nuVbGJvHqv6jF1+zRNMIEKS/Ioa1C4jenEe0j3boGM2IgjHtPq7WuOeSR2ErX
...

-----END PGP PRIVATE KEY BLOCK-----
```

Or put it to environment variable (Bash):

```bash
$ MAVEN_GPG_KEY=$(gpg --export-secret-keys --armor 1292EC426424C9BA0A581EE060C994FDCD3CADBD)

$ export MAVEN_GPG_KEY 
```

### Send the public key to [a keyserver](https://unix.stackexchange.com/a/692097)

```bash
$ gpg --list-keys
```

```
pub   rsa3072 2022-10-18 [SC]
      1292EC426424C9BA0A581EE060C994FDCD3CADBD       << this is the ID
uid           [ultimate] John Doe <doe@example.com>
sub   rsa3072 2022-10-18 [E]
```


```bash
$ gpg --keyserver hkps://keys.openpgp.org --send-keys 1292EC426424C9BA0A581EE060C994FDCD3CADBD
```

Some servers will just store the key. Some may require prior email verification.
Some servers disappear. You have to choose the right one for the moment.


</details>

## Minimal configuration

We're using Gradle configuration to build a Maven package, but not push
it Central. Creating in this way seems like a reasonable compromise.

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

It is the second part of `my.domain:thelib:0.1.2`, i.e. `thelib`.

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

```
cd /path/to/thelib
java -jar mavence.jar central my.domain:thelib 
```

This single command will do all the necessary work: build, signing, staging
and release.

### Publish to Maven Local

This will place the package file inside the local `~/.m2` directory. This way
you can
test the package without sending it anywhere.

```
cd /path/to/thelib
java -jar mavence.jar local my.domain:thelib 
```

### Publish to Staging

Set environment variables `MAVEN_GPG_KEY`, `MAVEN_GPG_PASSWORD`
, `SONATYPE_USERNAME`, `SONATYPE_PASSWORD` and run:

```
cd /path/to/thelib
java -jar mavence.jar stage my.domain:thelib 
```

This will push the package to
a [temporary remote repository](https://s01.oss.sonatype.org/content/repositories/)
.
This way you can test the package without sending it to Central.

## Testing before publishing

Although the utility prints quite a lot, `stdout` remains clean and only
prints the result as JSON.

Bash:

```
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