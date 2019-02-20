import org.gradle.internal.impldep.org.fusesource.jansi.AnsiRenderer.test
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val logbackVersion = "1.2.3"
val ktorVersion = "1.1.2"
val jacksonVersion = "2.9.8"
val wiremockVersion = "2.19.0"
val logstashLogbackVersion = "5.3"
val prometheusVersion = "0.6.0"

val mainClass = "no.nav.helse.PleiepengesoknadProsesseringKt"

plugins {
    kotlin("jvm") version "1.3.21"
}

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.21")
    }
}

dependencies {
    compile(kotlin("stdlib-jdk8"))

    // Ktor Server
    compile("io.ktor:ktor-server-netty:$ktorVersion")
    compile("io.ktor:ktor-auth-jwt:$ktorVersion")
    compile ("io.ktor:ktor-jackson:$ktorVersion")

    // JSON Serialization
    compile ("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    // Logging
    compile ( "ch.qos.logback:logback-classic:$logbackVersion")
    compile ("net.logstash.logback:logstash-logback-encoder:$logstashLogbackVersion")

    // Prometheus
    compile("io.prometheus:simpleclient_common:$prometheusVersion")
    compile("io.prometheus:simpleclient_hotspot:$prometheusVersion")

    // Ktor Client
    compile ("io.ktor:ktor-client-core:$ktorVersion")
    compile ("io.ktor:ktor-client-core-jvm:$ktorVersion")
    compile ("io.ktor:ktor-client-json-jvm:$ktorVersion")
    compile ("io.ktor:ktor-client-jackson:$ktorVersion")
    compile ("io.ktor:ktor-client-apache:$ktorVersion")

    // PDF
    compile ( "com.openhtmltopdf:openhtmltopdf-pdfbox:0.0.1-RC17")

    // Test
    testCompile ("com.github.tomakehurst:wiremock:$wiremockVersion")
    testCompile("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }
    testCompile ("com.nimbusds:oauth2-oidc-sdk:5.56")
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

tasks.named<KotlinCompile>("compileTestKotlin") {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.named<Jar>("jar") {
    baseName = "app"

    manifest {
        attributes["Main-Class"] = mainClass
        attributes["Class-Path"] = configurations["compile"].map {
            it.name
        }.joinToString(separator = " ")
    }

    configurations["compile"].forEach {
        val file = File("$buildDir/libs/${it.name}")
        if (!file.exists())
            it.copyTo(file)
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "5.2.1"
}
