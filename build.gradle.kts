plugins {
    java
    id("io.github.goooler.shadow") version "8.1.8"
    id("io.papermc.paperweight.userdev") version "1.7.1"
}

group = "vn.casino"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.tcoded.com/releases")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    // Paper dev bundle
    paperweight.paperDevBundle("1.21-R0.1-SNAPSHOT")

    // InventoryFramework
    implementation("com.github.stefvanschie.inventoryframework:IF:0.10.13")

    // Database
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("org.xerial:sqlite-jdbc:3.45.2.0")

    // Redis
    implementation("redis.clients:jedis:5.1.2")

    // Cache
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // Folia support
    implementation("com.tcoded:FoliaLib:0.5.1")

    // Economy
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")

    // PlaceholderAPI
    compileOnly("me.clip:placeholderapi:2.11.6")

    // Configuration
    implementation("org.spongepowered:configurate-yaml:4.1.2")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.21:3.94.0")
    testImplementation("org.awaitility:awaitility:4.2.1")
    testImplementation("com.h2database:h2:2.2.224")
}

tasks {
    assemble {
        dependsOn(shadowJar)
    }

    shadowJar {
        archiveClassifier.set("")

        relocate("com.github.stefvanschie.inventoryframework", "vn.casino.libs.if")
        relocate("com.zaxxer.hikari", "vn.casino.libs.hikari")
        relocate("redis.clients.jedis", "vn.casino.libs.jedis")
        relocate("com.github.benmanes.caffeine", "vn.casino.libs.caffeine")
        relocate("com.tcoded.folialib", "vn.casino.libs.folialib")
        relocate("org.spongepowered.configurate", "vn.casino.libs.configurate")
        relocate("org.postgresql", "vn.casino.libs.postgresql")
        relocate("org.sqlite", "vn.casino.libs.sqlite")

        minimize {
            exclude(dependency("com.github.stefvanschie.inventoryframework:.*"))
            exclude(dependency("org.postgresql:.*"))
            exclude(dependency("org.xerial:.*"))
        }
    }

    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    processResources {
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(
                "version" to project.version,
                "name" to project.name
            )
        }
    }

    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = false
        }
    }
}
