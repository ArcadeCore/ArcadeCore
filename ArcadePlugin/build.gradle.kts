plugins {
    id("java-library")
    id("jacoco")
    id("com.gradleup.shadow") version "9.4.3"
}

import java.time.Duration

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    compileOnly("org.jspecify:jspecify:1.0.0")
    compileOnly("org.jetbrains:annotations:24.1.0")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    implementation("dev.dejvokep:boosted-yaml:1.3.7")
    implementation("dev.dejvokep:boosted-yaml-spigot:1.5")
    implementation("org.xerial:sqlite-jdbc:3.53.2.0")

    implementation(project(":ArcadeAPI"))

    // compileOnly paper-api is absent from the test runtime classpath,
    // so tests that touch Bukkit/Adventure classes need it explicitly.
    // MockBukkit first: it ships its own paper-api and must win classpath order.
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.116.3")
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation("net.kyori:adventure-text-logger-slf4j:4.24.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Production code must load on Java 8 servers (1.8 era); tests may use the newer JDK.
tasks.named<JavaCompile>("compileJava") {
    options.release.set(8)
}

tasks {
    shadowJar {
        // JDBC drivers register through META-INF/services; without merging, the shaded sqlite driver is invisible.
        mergeServiceFiles()
    }

    test {
        useJUnitPlatform()
    }

    withType<JacocoReport> {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    processResources {
        val props = mapOf("version" to version, "description" to project.description)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    // Real smoke test: boots Paper via run-paper's runServer inside tmux,
    // asserts ArcadeCore enables cleanly, exercises a console command, stops.
    // Usage: ./gradlew :ArcadePlugin:smokeTest  (or bash scripts/smoke-test.sh)
    register("smokeTest", Exec::class) {
        group = "verification"
        description = "Boot a real Paper server in tmux and smoke-test ArcadeCore enable + console command."
        dependsOn("shadowJar")
        commandLine("bash", rootProject.file("scripts/smoke-test.sh").absolutePath)
        // Server boot + Paper download can take several minutes.
        timeout.set(Duration.ofMinutes(15))
    }
}
java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}
