import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

version = "0.1.0"
group = "org.eln2"

buildscript {
    repositories {
        mavenCentral()
    }
}

repositories {
    mavenCentral()
    maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
    maven(url = "https://kotlin.bintray.com/kotlinx")
}

plugins {
    java
    kotlin("jvm") version "1.5.10"
    jacoco
    idea
    id("org.jetbrains.dokka") version "1.5.31"
}

dependencies {
    implementation("org.jetbrains.kotlin", "kotlin-stdlib", "1.5.31")
    implementation("org.apache.commons", "commons-math3", "3.6.1")

    testImplementation("org.assertj", "assertj-core", "3.21.0")
    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.8.1")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "5.8.1")
}

tasks {
    named<Test>("test") {
        useJUnitPlatform()

        // *Always* run tests.
        // Ideally we'd cache the test output and print that instead, but this will do for now.
        outputs.upToDateWhen { false }

        // Print pass/fail for all tests to console, and exceptions if there are any.
        testLogging {
            events =
                setOf(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.STANDARD_ERROR)
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true
            showStandardStreams = System.getenv("eln2.core.debug") != ""

            // At log-level INFO or DEBUG, print everything.
            debug {
                events = TestLogEvent.values().toSet()
            }
            info {
                events = debug.events
            }
        }

        // Print a nice summary afterwards.
        afterSuite(KotlinClosure2<TestDescriptor, TestResult, Unit>({ desc, result ->
            if (desc.parent == null) { // will match the outermost suite
                val output =
                    "Results: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} passed, ${result.failedTestCount} failed, ${result.skippedTestCount} skipped)"
                val startItem = "|  "
                val endItem = "  |"
                val repeatLength = startItem.length + output.length + endItem.length
                println(
                    '\n' + "- ".repeat(repeatLength) + '\n' + startItem + output + endItem + '\n' + "-".repeat(
                        repeatLength
                    )
                )
            }
        }))
    }

    jacocoTestReport {
        reports {
            xml.isEnabled = true
            csv.isEnabled = false
        }
    }

    dokkaHtml {
        dokkaSourceSets.configureEach {
            includeNonPublic.set(true)
        }
    }
}

// By default build everything, put it somewhere convenient, and run the tests.
defaultTasks = mutableListOf("build", "test")

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "11"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "11"
}
