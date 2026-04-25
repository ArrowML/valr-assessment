plugins {
    kotlin("jvm") version "1.9.22"
    application
}

group = "com.exchange"
version = "1.0.0"

repositories {
    mavenCentral()
}

val vertxVersion = "4.5.3"
val jacksonVersion = "2.16.1"
val junitVersion = "5.10.1"

dependencies {
    implementation("io.vertx:vertx-core:$vertxVersion")
    implementation("io.vertx:vertx-web:$vertxVersion")
    implementation("io.vertx:vertx-web-client:$vertxVersion")

    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation("org.slf4j:slf4j-api:2.0.11")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    testImplementation("io.vertx:vertx-junit5:$vertxVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("org.assertj:assertj-core:3.25.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
}

application {
    mainClass.set("com.exchange.ApplicationKt")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
