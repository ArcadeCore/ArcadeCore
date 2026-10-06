plugins {
    id("java")
    id("jacoco")
}

group = "org.drappula"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JacocoReport> {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}