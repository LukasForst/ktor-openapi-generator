import org.gradle.jvm.tasks.Jar
import java.net.URL


plugins {
    kotlin("jvm") version "1.7.10"

    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"

    id("net.nemerosa.versioning") version "2.15.1"
    id("org.jetbrains.dokka") version "1.7.10"
}

group = "dev.forst"
base.archivesName.set("ktor-open-api")
version = (versioning.info?.tag ?: versioning.info?.lastTag ?: versioning.info?.build) ?: "SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    // Ktor server dependencies
    val ktorVersion = "2.1.1"
    implementation("io.ktor", "ktor-server-core", ktorVersion)
    implementation("io.ktor", "ktor-server-auth", ktorVersion)
    implementation("io.ktor", "ktor-serialization-jackson", ktorVersion)
    implementation("io.ktor", "ktor-server-content-negotiation", ktorVersion)
    implementation("io.ktor", "ktor-server-status-pages", ktorVersion)

    implementation("org.slf4j:slf4j-api:2.0.1")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.4") // needed for multipart parsing
    implementation("org.webjars:swagger-ui:4.14.0")

    implementation("org.reflections:reflections:0.10.2") // only used while initializing

    // testing
    testImplementation("io.ktor", "ktor-server-netty", ktorVersion)
    testImplementation("io.ktor", "ktor-server-core", ktorVersion)
    testImplementation("io.ktor", "ktor-server-test-host", ktorVersion)
    testImplementation("io.ktor", "ktor-server-auth", ktorVersion)
    testImplementation("io.ktor", "ktor-server-auth-jwt", ktorVersion)
    testImplementation("io.ktor", "ktor-server-content-negotiation", ktorVersion)
    testImplementation("io.ktor", "ktor-serialization-jackson", ktorVersion)
    testImplementation("io.ktor", "ktor-client-content-negotiation", ktorVersion)

    testImplementation(kotlin("test"))
    testImplementation(kotlin("stdlib-jdk8"))

    // we want to keep it compatible with java 8, thus we use 1.3 series, see
    // https://www.mail-archive.com/logback-user@qos.ch/msg05119.html
    testImplementation("ch.qos.logback", "logback-classic", "1.3.1") // logging framework for the tests

    val junitVersion = "5.9.0"
    testImplementation("org.junit.jupiter", "junit-jupiter-api", junitVersion) // junit testing framework
    testImplementation("org.junit.jupiter", "junit-jupiter-params", junitVersion) // generated parameters for tests
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", junitVersion) // testing runtime
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    test {
        useJUnitPlatform()
    }

    dokkaHtml {
        outputDirectory.set(File("$buildDir/docs"))

        dokkaSourceSets {
            configureEach {
                displayName.set("Ktor OpenAPI/Swagger 3 Generator")

                sourceLink {
                    localDirectory.set(file("src/main/kotlin"))
                    remoteUrl.set(URL("https://github.com/LukasForst/ktor-openapi-generator/tree/master/src/main/kotlin"))
                    remoteLineSuffix.set("#L")
                }
            }
        }
    }
}

// ------------------------------------ Deployment Configuration  ------------------------------------
// deployment configuration - deploy with sources and documentation
val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val javadocJar by tasks.creating(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

// name the publication as it is referenced
val publication = "mavenJava"
publishing {
    // create jar with sources and with javadoc
    publications {
        create<MavenPublication>(publication) {
            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)

            pom {
                name.set("Ktor OpenAPI/Swagger 3 Generator")
                description.set("The Ktor OpenAPI Generator is a library to automatically generate the descriptor as you route your ktor application.")
                url.set("https://github.com/LukasForst/ktor-openapi-generator")
                packaging = "jar"
                licenses {
                    license {
                        name.set("Apache-2.0 License")
                        url.set("https://github.com/LukasForst/ktor-openapi-generator/blob/master/LICENSE")
                    }
                }
                developers {
                    developer {
                        id.set("wicpar")
                        name.set("Frédéric Nieto")
                    }
                    developer {
                        id.set("lukasforst")
                        name.set("Lukas Forst")
                        email.set("lukas@forst.dev")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/LukasForst/ktor-openapi-generator.git")
                    url.set("https://github.com/LukasForst/ktor-openapi-generator")
                }
            }
        }
    }
}

signing {
    val signingKeyId = project.findProperty("gpg.keyId") as String? ?: System.getenv("GPG_KEY_ID")
    val signingKey = project.findProperty("gpg.key") as String? ?: System.getenv("GPG_KEY")
    val signingPassword = project.findProperty("gpg.keyPassword") as String? ?: System.getenv("GPG_KEY_PASSWORD")

    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    sign(publishing.publications[publication])
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(project.findProperty("ossrh.username") as String? ?: System.getenv("OSSRH_USERNAME"))
            password.set(project.findProperty("ossrh.password") as String? ?: System.getenv("OSSRH_PASSWORD"))
        }
    }
}
