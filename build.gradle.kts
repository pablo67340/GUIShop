import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    base
}

// Deliberately thin: each module declares its own toolchain, repositories and
// dependencies, so the NeoForge module can target Java 21 and its own plugin
// ecosystem without having to override anything inherited from here.
allprojects {
    group = "com.pablo67340.GUIShop"
    version = providers.gradleProperty("guishopVersion").get()
}

val docTokens = mapOf(
    "project.version" to version.toString(),
    "mc.version.range" to "${providers.gradleProperty("mcVersionMin").get()} - " +
        providers.gradleProperty("mcVersionMax").get(),
    "java.version.min" to providers.gradleProperty("javaVersionMin").get(),
    "supported.platforms" to providers.gradleProperty("supportedPlatforms").get(),
    "modded.mc.version" to providers.gradleProperty("moddedMcVersion").get(),
    "neoforge.version" to providers.gradleProperty("neoForgeVersion").get(),
)

// Storefront copy with ${project.version} and friends resolved, for pasting into
// the Spigot, Modrinth and DevBukkit pages.
val processDocs = tasks.register<Copy>("processDocs") {
    destinationDir = layout.buildDirectory.dir("processed-docs").get().asFile
    filteringCharset = "UTF-8"
    filter<ReplaceTokens>("beginToken" to "\${", "endToken" to "}", "tokens" to docTokens)

    from(files("spigot.bbcode", "modrinth.md", "dev-bukkit.html", "curseforge.html"))
    from("wiki") {
        include("*.md")
        into("wiki")
    }
}

tasks.assemble {
    dependsOn(processDocs)
}
