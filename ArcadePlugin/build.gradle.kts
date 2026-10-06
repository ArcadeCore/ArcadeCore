plugins {
    id("java-library")
    id("jacoco")
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.gradleup.shadow") version "9.4.3"
    kotlin("jvm")
}

import java.time.Duration

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    implementation("dev.dejvokep:boosted-yaml:1.3.7")
    implementation("dev.dejvokep:boosted-yaml-spigot:1.5")
    implementation("org.xerial:sqlite-jdbc:3.53.2.0")
    implementation(kotlin("stdlib-jdk8"))

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

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    test {
        useJUnitPlatform()
    }

    withType<JacocoReport> {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.21.11")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version, "description" to project.description)
        filesMatching("paper-plugin.yml") {
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
kotlin {
    jvmToolchain(21)
}