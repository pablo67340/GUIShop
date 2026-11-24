import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    java
    id("com.gradleup.shadow") version "9.6.1"
}

// Keeps the released artifact named GUIShop-<version>.jar rather than taking
// the module directory name.
base {
    archivesName = "GUIShop"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
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
    implementation(project(":guishop-core"))

    compileOnly("org.spigotmc:spigot-api:1.21.10-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.github.retrooper:packetevents-spigot:2.7.0")
    compileOnly("com.github.milkbowl:vault:1.7.3") { isTransitive = false }

    compileOnly("org.projectlombok:lombok:${property("lombokVersion")}")
    annotationProcessor("org.projectlombok:lombok:${property("lombokVersion")}")

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
    "mc.version.range" to "${project.property("mcVersionMin")} - ${project.property("mcVersionMax")}",
    "java.version.min" to project.property("javaVersionMin").toString(),
    "supported.platforms" to project.property("supportedPlatforms").toString(),
)

// Literal token replacement rather than Gradle's expand(), which would try to
// evaluate the bare $ in currency symbols and price strings as an expression.
tasks.processResources {
    filteringCharset = "UTF-8"
    filter<ReplaceTokens>(
        "beginToken" to "\${",
        "endToken" to "}",
        "tokens" to resourceTokens,
    )
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
        include(project(":guishop-core"))
        include(dependency("com.github.cryptomorin:XSeries"))
        include(dependency("com.tcoded:FoliaLib"))
        include(dependency("com.googlecode.json-simple:json-simple"))
    }

    minimize {
        // FoliaLib is reached reflectively, and core is first-party code that
        // other platform modules compile against, so neither gets trimmed.
        exclude(project(":guishop-core"))
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
