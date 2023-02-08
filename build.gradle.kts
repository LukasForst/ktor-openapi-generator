import org.gradle.jvm.tasks.Jar
import java.net.URL


plugins {
    kotlin("jvm") version "1.8.10"

    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    // we can not switch to 3.x.x because we want to keep it compatible with JVM 8
    id("net.nemerosa.versioning") version "2.15.1"
    id("org.jetbrains.dokka") version "1.7.20"
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
    implementation("io.ktor:ktor-server-core:2.2.3")
    implementation("io.ktor:ktor-server-auth:2.2.3")
    implementation("io.ktor:ktor-serialization-jackson:2.2.2")
    implementation("io.ktor:ktor-server-content-negotiation:2.2.2")
    implementation("io.ktor:ktor-server-status-pages:2.2.3")

    implementation("org.slf4j:slf4j-api:2.0.6")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.2") // needed for multipart parsing
    // when updating version here, don't forge to update version in OpenAPIGen.kt line 68
    implementation("org.webjars:swagger-ui:4.15.5")

    implementation("org.reflections:reflections:0.10.2") // only used while initializing

    // testing
    testImplementation("io.ktor:ktor-server-netty:2.2.2")
    testImplementation("io.ktor:ktor-server-core:2.2.2")
    testImplementation("io.ktor:ktor-server-test-host:2.2.3")
    testImplementation("io.ktor:ktor-server-auth:2.2.2")
    testImplementation("io.ktor:ktor-server-auth-jwt:2.2.2")
    testImplementation("io.ktor:ktor-server-content-negotiation:2.2.2")
    testImplementation("io.ktor:ktor-serialization-jackson:2.2.2")
    testImplementation("io.ktor:ktor-client-content-negotiation:2.2.2")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("stdlib-jdk8"))

    // we want to keep it compatible with java 8, thus we use 1.3 series, see
    // https://www.mail-archive.com/logback-user@qos.ch/msg05119.html
    testImplementation("ch.qos.logback:logback-classic:1.3.5") // logging framework for the tests

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2") // junit testing framework
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.2") // generated parameters for tests
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2") // testing runtime
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
