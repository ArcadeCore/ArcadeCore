plugins {
    id("java")
    id("jacoco")
}

group = "org.drappula"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    // Nullability annotations are compile-time only; missing annotation classes are ignored at runtime.
    compileOnly("org.jspecify:jspecify:1.0.0")
    compileOnly("org.jetbrains:annotations:24.1.0")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    // compileOnly deps are absent from the test classpath
    testImplementation("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    testImplementation("org.jspecify:jspecify:1.0.0")
    testImplementation("org.jetbrains:annotations:24.1.0")
    testImplementation("com.google.code.findbugs:jsr305:3.0.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Production code must load on Java 8 servers (1.8 era); tests may use the newer JDK.
tasks.named<JavaCompile>("compileJava") {
    options.release.set(8)
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