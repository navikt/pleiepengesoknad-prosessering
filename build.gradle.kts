import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val dusseldorfKtorVersion = "1.2.3.ec226d3"
val ktorVersion = ext.get("ktorVersion").toString()
val slf4jVersion = ext.get("slf4jVersion").toString()
val kotlinxCoroutinesVersion = ext.get("kotlinxCoroutinesVersion").toString()

val openhtmltopdfVersion = "1.0.0"
val kafkaEmbeddedEnvVersion = "2.2.0"
val kafkaVersion = "2.3.0" // Alligned med version fra kafka-embedded-env
val handlebarsVersion = "4.1.2"

val mainClass = "no.nav.helse.PleiepengesoknadProsesseringKt"

plugins {
    kotlin("jvm") version "1.3.41"
    id("com.github.johnrengelman.shadow") version "5.1.0"
}

buildscript {
    apply("https://raw.githubusercontent.com/navikt/dusseldorf-ktor/ec226d3ba5b4d5fbc8782d3d934dc5ed0690f85d/gradle/dusseldorf-ktor.gradle.kts")
}

dependencies {
    // Server
    compile ( "no.nav.helse:dusseldorf-ktor-core:$dusseldorfKtorVersion")
    compile ( "no.nav.helse:dusseldorf-ktor-jackson:$dusseldorfKtorVersion")
    compile ( "no.nav.helse:dusseldorf-ktor-metrics:$dusseldorfKtorVersion")
    compile ( "no.nav.helse:dusseldorf-ktor-health:$dusseldorfKtorVersion")
    compile ( "no.nav.helse:dusseldorf-ktor-auth:$dusseldorfKtorVersion")
    compile ("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:$kotlinxCoroutinesVersion")
    
    // Client
    compile ( "no.nav.helse:dusseldorf-ktor-client:$dusseldorfKtorVersion")
    compile ( "no.nav.helse:dusseldorf-oauth2-client:$dusseldorfKtorVersion")

    // PDF
    compile ( "com.openhtmltopdf:openhtmltopdf-pdfbox:$openhtmltopdfVersion")
    compile ( "com.openhtmltopdf:openhtmltopdf-slf4j:$openhtmltopdfVersion")
    compile("org.slf4j:jcl-over-slf4j:$slf4jVersion")
    compile("com.github.jknack:handlebars:$handlebarsVersion")

    // Kafka
    compile("org.apache.kafka:kafka-streams:$kafkaVersion")

    // Test
    testCompile("org.apache.kafka:kafka-clients:$kafkaVersion")
    testCompile ("no.nav:kafka-embedded-env:$kafkaEmbeddedEnvVersion")
    testCompile ( "no.nav.helse:dusseldorf-ktor-test-support:$dusseldorfKtorVersion")
    testCompile("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }
    testCompile("org.skyscreamer:jsonassert:1.5.0")
}

repositories {
    maven("https://dl.bintray.com/kotlin/ktor")
    maven("https://kotlin.bintray.com/kotlinx")
    maven("http://packages.confluent.io/maven/")

    jcenter()
    mavenLocal()
    mavenCentral()
}


java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<ShadowJar> {
    archiveBaseName.set("app")
    archiveClassifier.set("")
    manifest {
        attributes(
            mapOf(
                "Main-Class" to mainClass
            )
        )
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "5.5.1"
}
