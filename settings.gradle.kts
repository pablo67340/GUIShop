pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.neoforged.net/releases")
    }
}

rootProject.name = "GUIShop"

// Platform-agnostic shop model, pricing, economy and statistics.
include(":guishop-core")

// The Bukkit/Spigot/Paper/Folia plugin.
include(":guishop-bukkit")

// The NeoForge mod for Minecraft 1.21.1.
include(":guishop-neoforge")
