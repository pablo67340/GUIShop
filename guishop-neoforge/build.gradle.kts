plugins {
    java
    id("net.neoforged.moddev") version "2.0.148"
}

// NeoForge for 1.21.1 runs on Java 21, unlike the Bukkit module which stays on
// 17 for older server compatibility.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

neoForge {
    version = providers.gradleProperty("neoForgeVersion").get()

    mods {
        register("guishop") {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        register("client") {
            client()
        }
        register("server") {
            server()
        }
    }
}

/**
 * The core jar, resolved on its own so its classes can be folded into the mod
 * jar. Declared separately rather than reaching into the other project's source
 * sets, which would break the configuration cache.
 */
val bundledCore: Configuration = configurations.create("bundledCore") {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

dependencies {
    implementation(project(":guishop-core"))
    bundledCore(project(":guishop-core"))

    // Development runs load the mod from its class directory, and NeoForge's
    // module layer will not see core from the plain compile classpath. This puts
    // it where the game's class loader can find it.
    add("additionalRuntimeClasspath", project(":guishop-core"))
}

// A mod has no "provided" scope for our own shared code, so core ships inside
// the mod jar. Development runs already see it via the compile classpath; this
// is what makes the distributable jar self-contained.
tasks.jar {
    from({ bundledCore.map { zipTree(it) } }) {
        exclude("META-INF/MANIFEST.MF")
        exclude("META-INF/maven/**")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.processResources {
    val tokens = mapOf(
        "version" to version.toString(),
        "neoForgeVersionRange" to providers.gradleProperty("neoForgeVersionRange").get(),
        "loaderVersionRange" to providers.gradleProperty("loaderVersionRange").get(),
        "minecraftVersionRange" to providers.gradleProperty("minecraftVersionRange").get(),
    )
    inputs.properties(tokens)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(tokens)
    }
}
