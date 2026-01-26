/*
 * SPDX-FileCopyrightText: 2025 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    alias(libs.plugins.google.protobuf)
    checkstyle
    `maven-publish`
}

java {
    withSourcesJar()
    withJavadocJar()
}

sourceSets {
    main {
        java.srcDir("../timeago-parser/src/main/java")
    }
}

// Protobuf files would uselessly end up in the JAR otherwise, see
// https://github.com/google/protobuf-gradle-plugin/issues/390
tasks.jar {
    exclude("**/*.proto")
    includeEmptyDirs = false
}

tasks.test {
    // Test logging setup
    testLogging {
        events = setOf(TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        showStandardStreams = true
        exceptionFormat = TestExceptionFormat.FULL
    }

    // Pass on downloader type to tests for different CI jobs. See DownloaderFactory.java and ci.yml
    if (System.getProperties().containsKey("downloader")) {
        systemProperty("downloader", System.getProperty("downloader"))
    }
    useJUnitPlatform()
    dependsOn(tasks.checkstyleMain) // run checkstyle when testing
}

// https://checkstyle.org/#JRE_and_JDK
// Note: Using Java 11 for compatibility with old Android versions
tasks.withType<Checkstyle>().configureEach {
    // For Gradle 7.x compatibility, we use the default Java launcher
    // which should be Java 11 as configured in the root build.gradle.kts
}

checkstyle {
    configDirectory.set(rootProject.layout.projectDirectory.dir("checkstyle"))
    isIgnoreFailures = false
    isShowViolations = true
    toolVersion = libs.versions.checkstyle.get()
}

// Exclude Protobuf generated files from Checkstyle
tasks.checkstyleMain {
    exclude(
        "org/schabi/newpipe/extractor/services/youtube/protos",
        "org/schabi/newpipe/extractor/timeago"
    )
}

tasks.checkstyleTest {
    isEnabled = false // do not checkstyle test files
}

dependencies {
    implementation(libs.newpipe.nanojson)
    implementation(libs.jsoup)
    implementation(libs.google.jsr305)
    implementation(libs.google.protobuf)

    implementation(libs.mozilla.rhino.core)

    checkstyle(libs.puppycrawl.checkstyle)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.params)

    testImplementation(libs.squareup.okhttp)
    testImplementation(libs.google.gson)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.lib.get()}"
    }

    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                named("java") {
                    option("lite")
                }
            }
        }
    }
}

// Run "./gradlew publishReleasePublicationToLocalRepository" to generate release JARs locally
publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "net.newpipe"
            artifactId = "extractor"
            version = rootProject.version.toString()

            afterEvaluate {
                from(components["java"])
            }

            pom {
                name.set("NewPipe Extractor")
                description.set("A library for extracting data from streaming websites, used in NewPipe")
                url.set("https://github.com/TeamNewPipe/NewPipeExtractor")

                licenses {
                    license {
                        name.set("GNU GENERAL PUBLIC LICENSE, Version 3")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.txt")
                    }
                }

                scm {
                    url.set("https://github.com/TeamNewPipe/NewPipeExtractor")
                    connection.set("scm:git:git@github.com:TeamNewPipe/NewPipeExtractor.git")
                    developerConnection.set("scm:git:git@github.com:TeamNewPipe/NewPipeExtractor.git")
                }

                developers {
                    developer {
                        id.set("newpipe")
                        name.set("Team NewPipe")
                        email.set("team@newpipe.net")
                    }
                }
            }
        }
        repositories {
            maven {
                name = "local"
                url = uri(layout.buildDirectory.dir("maven"))
            }
        }
    }
}
