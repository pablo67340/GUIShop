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

repositories {
    mavenCentral()
}

val yamlDependency = "org.yaml:snakeyaml:2.3"
val sqliteDependency = "org.xerial:sqlite-jdbc:3.47.1.0"

/**
 * The native sqlite builds worth carrying, as paths inside the driver jar.
 * <p>
 * The driver ships 25 of them and they are 20 MB uncompressed, which is thirty
 * times the rest of the mod put together. These five cover every machine a
 * Minecraft server realistically runs on. Anything else falls back to the JSON
 * store rather than failing, which is what [GuiShopDatabase] is for.
 */
val keptSqliteNatives = listOf(
    "org/sqlite/native/Windows/x86_64",
    "org/sqlite/native/Linux/x86_64",
    "org/sqlite/native/Linux/aarch64",
    "org/sqlite/native/Mac/x86_64",
    "org/sqlite/native/Mac/aarch64",
)

/**
 * Our own code to fold into the mod jar, resolved on its own. Declared as a
 * configuration rather than reaching into the other project's source sets,
 * which would break the configuration cache.
 */
val bundledLibs: Configuration = configurations.create("bundledLibs") {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

/**
 * Third party code, which goes under a prefix rather than at the jar root.
 * <p>
 * A mod jar is a module and a module exports every package in it, so shipping
 * these at the root offers `org.sqlite` and `org.yaml.snakeyaml` to the game's
 * module layer. Any other mod or hybrid server that supplies the same packages
 * then fails to resolve, which takes the whole server down rather than just us.
 * `BundledLibraries` loads them back out of the prefix at runtime.
 */
val isolatedLibs: Configuration = configurations.create("isolatedLibs") {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

dependencies {
    implementation(project(":guishop-core"))
    bundledLibs(project(":guishop-core"))

    // Reads the same shop YAML the Bukkit plugin uses, so an existing server's
    // configs work unchanged. Minecraft bundles Gson and NeoForge bundles
    // NightConfig, but neither reads YAML.
    isolatedLibs(yamlDependency)

    // Balances and statistics. Paper and Spigot ship a JDBC driver, which is
    // why the plugin can just ask for one; nothing in Minecraft or NeoForge
    // does, so it travels with us.
    isolatedLibs(sqliteDependency)

    // Development runs load the mod from its class directory, and NeoForge's
    // module layer will not see these from the plain compile classpath. This
    // puts them where the game's class loader can find them.
    add("additionalRuntimeClasspath", project(":guishop-core"))
    add("additionalRuntimeClasspath", yamlDependency)
    add("additionalRuntimeClasspath", sqliteDependency)
}

// A mod has no "provided" scope for our own shared code, so core and the YAML
// parser ship inside the mod jar. Development runs see them via
// additionalRuntimeClasspath; this is what makes the distributable jar
// self-contained.
tasks.jar {
    from({ bundledLibs.map { zipTree(it) } }) {
        exclude("META-INF/MANIFEST.MF")
        exclude("META-INF/maven/**")
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("module-info.class")
    }

    from({ isolatedLibs.map { zipTree(it) } }) {
        exclude("META-INF/MANIFEST.MF")
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("module-info.class")

        // Multi-release overlays and GraalVM hints. Nothing reads either once
        // the jar is no longer the unit being loaded from, and the classes
        // they replace are all present in the base copy.
        exclude("META-INF/versions/**")
        exclude("META-INF/native-image/**")

        // Written as a predicate rather than a list of the unwanted paths, so
        // a platform added by a future driver release is dropped by default
        // instead of quietly adding megabytes.
        val kept = keptSqliteNatives
        exclude { element ->
            val path = element.relativePath.pathString.replace('\\', '/')
            path.startsWith("org/sqlite/native/") && kept.none { path.startsWith("$it/") }
        }

        // Must match BundledLibraries.PREFIX.
        into("guishoplibs")
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
