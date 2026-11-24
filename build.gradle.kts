import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    java
    id("com.gradleup.shadow") version "9.6.1"
}

group = "com.pablo67340.GUIShop"
version = "9.4.4"
description = "This plugin is the ultimate solution to all the signs, the chests, the glitches."

// Substituted into plugin.yml, the bundled configs and the storefront docs.
val mcVersionMin = "1.13"
val mcVersionMax = "1.21.11"
val javaVersionMin = "17"
val supportedPlatforms = "Paper/Spigot/Folia/Bukkit"

val lombokVersion = "1.18.34"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

// The project predates the Maven standard layout: sources live in src/ and the
// bundled configs in resources/, which processResources assembles by hand below.
sourceSets.main {
    java.setSrcDirs(listOf("src"))
    resources.setSrcDirs(emptyList<String>())
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://repo.glaremasters.me/repository/permissionsex/")
    maven("https://repo.tcoded.com/releases")
    maven("https://mvn-repo.arim.space/lesser-gpl3/")
    maven("https://ci.ender.zone/plugin/repository/everything/")
    maven("https://libraries.minecraft.net/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.21.10-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.github.retrooper:packetevents-spigot:2.7.0")
    compileOnly("com.github.milkbowl:vault:1.7.3") { isTransitive = false }

    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")

    // javax.annotation.Nullable. Maven got this transitively via spigot-api's
    // guava, but guava's module metadata keeps it off the compile variant.
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    implementation("com.github.cryptomorin:XSeries:13.7.1")
    implementation("com.tcoded:FoliaLib:0.5.1")
    implementation("com.googlecode.json-simple:json-simple:1.1.1") {
        exclude(group = "junit", module = "junit")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

val resourceTokens = mapOf(
    "project.version" to version.toString(),
    "mc.version.range" to "$mcVersionMin - $mcVersionMax",
    "java.version.min" to javaVersionMin,
    "supported.platforms" to supportedPlatforms,
)

// Literal token replacement rather than Gradle's expand(), which would try to
// evaluate the bare $ in currency symbols and price strings as an expression.
fun CopySpec.substituteTokens() {
    filteringCharset = "UTF-8"
    filter<ReplaceTokens>(
        "beginToken" to "\${",
        "endToken" to "}",
        "tokens" to resourceTokens,
    )
}

tasks.processResources {
    // resources/cache.yml and resources/Data/cache.yml both land at the jar
    // root; the Data copy is declared second and wins, matching the Maven build.
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    from("resources") {
        include(
            "plugin.yml",
            "config.yml",
            "menu.yml",
            "cache.yml",
            "README.txt",
            "messages.yml",
            "internal_messages.yml",
            "worth.yml",
            "dynamicpricing.yml",
            "economy.yml",
            "transaction.yml",
        )
        substituteTokens()
    }
    from("resources/Dictionary") {
        include(
            "potion-names.txt",
            "spawner-names.txt",
            "material-names.txt",
            "enchantment-names.txt",
            "item-flags.txt",
        )
        substituteTokens()
    }
    from("resources/Data") {
        include("cache.yml", "inventories.yml")
        substituteTokens()
    }
    from("resources/shops") {
        include("*.yml")
        into("shops")
        substituteTokens()
    }
}

// Storefront copy with ${project.version} and friends resolved, for pasting into
// the Spigot, Modrinth and DevBukkit pages.
val processDocs by tasks.registering(Copy::class) {
    destinationDir = layout.buildDirectory.dir("processed-docs").get().asFile
    from(files("spigot.bbcode", "modrinth.md", "dev-bukkit.html")) {
        substituteTokens()
    }
    from("wiki") {
        include("*.md")
        into("wiki")
        substituteTokens()
    }
}

tasks.jar {
    archiveClassifier = "original"
}

tasks.shadowJar {
    archiveClassifier = ""

    relocate("com.cryptomorin.xseries", "com.pablo67340.guishop.shade.xseries")
    relocate("com.tcoded.folialib", "com.pablo67340.guishop.shade.folialib")
    relocate("org.json.simple", "com.pablo67340.guishop.shade.json")

    dependencies {
        include(dependency("com.github.cryptomorin:XSeries"))
        include(dependency("com.tcoded:FoliaLib"))
        include(dependency("com.googlecode.json-simple:json-simple"))
    }

    // FoliaLib is reached reflectively, so it has to survive minimization whole.
    minimize {
        exclude(dependency("com.tcoded:FoliaLib"))
    }

    // minimize() alone retains XSeries classes the plugin never references,
    // because its base registries point at them. Trimmed to match the shade
    // filter the Maven build has shipped with since 9.4.x. Both the original and
    // relocated prefixes are listed since filters may run either side of
    // relocation.
    for (pkg in listOf("com/cryptomorin/xseries", "com/pablo67340/guishop/shade/xseries")) {
        exclude("$pkg/XAttribute*.class")
        exclude("$pkg/XBiome*.class")
        exclude("$pkg/XEntityType*.class")
        exclude("$pkg/XItemFlag*.class")
        exclude("$pkg/particles/**")
    }

    exclude("META-INF/maven/**")

    // Stops Paper's PluginRemapper from trying to remap the jar on startup.
    manifest {
        attributes("paperweight-mappings-namespace" to "mojang+yarn")
    }
}

tasks.assemble {
    dependsOn(processDocs)
}
