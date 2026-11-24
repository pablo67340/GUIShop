plugins {
    java
}

// Java 17 rather than 21 so the Bukkit plugin keeps running on the older
// servers it still supports; the NeoForge module compiles this as a dependency.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:${property("lombokVersion")}")
    annotationProcessor("org.projectlombok:lombok:${property("lombokVersion")}")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}
