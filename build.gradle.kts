import java.net.URI
import java.util.Properties
import java.util.zip.ZipFile

plugins {
    kotlin("jvm") version "2.4.21"
    kotlin("kapt") version "2.4.21"
    id("com.gradleup.shadow") version "9.6.1"
    `maven-publish`
}

group = "com.panomc.plugins"
version =
    (if (project.hasProperty("version") && project.findProperty("version") != "unspecified") project.findProperty("version") else "local-build")!!

val pf4jVersion = property("pf4jVersion") as String
val vertxVersion = property("vertxVersion") as String
val gsonVersion = property("gsonVersion") as String
val springContextVersion = property("springContextVersion") as String
val bootstrap = (project.findProperty("bootstrap") as String?)?.toBoolean() ?: false
val noui = project.hasProperty("noui")
val pluginsDir: File? = runCatching { rootProject.extra["pluginsDir"] as? File }.getOrNull()

val fastLoginVersion = "1.12-SNAPSHOT"

val os = System.getProperty("os.name").lowercase()
val arch = System.getProperty("os.arch").lowercase()

val isWindows = os.contains("win")
val isMac = os.contains("mac")
val isLinux = os.contains("nix") || os.contains("nux") || os.contains("linux")

val isAarch64 = arch.contains("aarch64") || arch.contains("arm64")
val isX64 = arch.contains("x86_64") || arch.contains("amd64")

val bunVersion = "1.2.0"
val bunPlatform = when {
    isWindows && isX64 -> "bun-windows-x64"
//    isWindows && isAarch64 -> "bun-windows-aarch64"
    isMac && isX64 -> "bun-darwin-x64"
    isMac && isAarch64 -> "bun-darwin-aarch64"
    isLinux && isX64 -> "bun-linux-x64"
    isLinux && isAarch64 -> "bun-linux-aarch64"
    else -> throw RuntimeException("Unsupported OS or Architecture")
}
val bunUrl = "https://github.com/oven-sh/bun/releases/download/bun-v$bunVersion/$bunPlatform.zip"

val bunDir = File(layout.buildDirectory.asFile.get().absolutePath, "bun")
val bunBinDir = File(bunDir, bunPlatform)
var bunBin = if (isWindows) File(bunBinDir, "bun.exe") else File(bunBinDir, "bun")

val pluginId = property("pluginId") as String

// api-level (pano-api migrate-v1): "panoApiLevel" of the pano-web-platform tree this plugin is built in, else "current" from
// the pano-api-level.properties inside the Pano jar on compileClasspath. `apiLevel=` in gradle.properties lowers it.
val panoApiLevel: String? by lazy {
    (findProperty("apiLevel") as String?)
        ?: (rootProject.findProperty("panoApiLevel") as String?)
        ?: configurations.findByName("compileClasspath")?.files?.firstNotNullOfOrNull { jar ->
            if (!jar.isFile || !jar.name.endsWith(".jar")) null
            else ZipFile(jar).use { zip ->
                zip.getEntry("pano-api-level.properties")?.let { entry ->
                    Properties().apply { load(zip.getInputStream(entry)) }.getProperty("current")
                }
            }
        }
}

val pluginName = property("pluginName") as String
val pluginDescription = findProperty("pluginDescription") as String?
val pluginPanoVersion = property("pluginPanoVersion") as String
val pluginClass = property("pluginClass") as String
val pluginDeveloper = property("pluginDeveloper") as String
val pluginLicense = findProperty("pluginLicense") as String?
val pluginSourceUrl = findProperty("pluginSourceUrl") as String?
val pluginDependencies = findProperty("pluginDependencies") as String?
val pluginRequires = findProperty("pluginRequires") as String?

val organization = findProperty("organization") as String?

// MC plugin dependency versions
val spigotApiVersion = property("spigotApiVersion") as String
val bungeecordApiVersion = property("bungeecordApiVersion") as String
val velocityApiVersion = property("velocityApiVersion") as String

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://jitpack.io")
    // MC platform repos
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://libraries.minecraft.net")
    // FastLogin (CodeMC)
    maven("https://repo.codemc.io/repository/maven-public/")
}

dependencies {
    if (bootstrap) {
        compileOnly(project(":Pano"))
    } else {
        compileOnly("com.github.panomc:pano:v1.0.0-alpha.560")
    }

    // pano-mc-plugin Core — provides PlatformManager, PlatformRequest, PlatformMessageResponse
    compileOnly("com.github.panomc.pano-mc-plugin:Core:v1.0.0-alpha.45")

    // Unit tests of the error classes (they extend the host's Error).
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.14.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.14.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.14.4")
    // The tests use the host's classes: the umbrella build takes the project, a standalone build the same Pano as above.
    if (bootstrap) {
        testImplementation(project(":Pano"))
    } else {
        testImplementation("com.github.panomc:pano:v1.0.0-alpha.560")
    }
    testImplementation("io.vertx:vertx-web:${vertxVersion}")
    testImplementation("io.vertx:vertx-lang-kotlin:${vertxVersion}")
    testImplementation("com.google.code.gson:gson:${gsonVersion}")

    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly(kotlin("reflect"))

    compileOnly("org.pf4j:pf4j:${pf4jVersion}")
    // Vert.x types carry io.vertx.codegen.annotations.Nullable; Kotlin 2.4 needs the annotation on the compile classpath
    compileOnly("io.vertx:vertx-codegen-api:${vertxVersion}")
    kapt("org.pf4j:pf4j:${pf4jVersion}")
    compileOnly("io.vertx:vertx-web:${vertxVersion}")
    compileOnly("io.vertx:vertx-web-client:${vertxVersion}")
    compileOnly("io.vertx:vertx-lang-kotlin:${vertxVersion}")
    compileOnly("io.vertx:vertx-lang-kotlin-coroutines:${vertxVersion}")
    compileOnly("io.vertx:vertx-jdbc-client:${vertxVersion}")
    compileOnly("io.vertx:vertx-json-schema:${vertxVersion}")
    compileOnly("io.vertx:vertx-web-validation:${vertxVersion}")
    compileOnly("io.vertx:vertx-mysql-client:${vertxVersion}")

    // https://mvnrepository.com/artifact/org.springframework/spring-context
    compileOnly("org.springframework:spring-context:${springContextVersion}")

    compileOnly("io.papermc.paper:paper-api:${spigotApiVersion}")
    compileOnly("net.md-5:bungeecord-api:${bungeecordApiVersion}")
    compileOnly("com.velocitypowered:velocity-api:${velocityApiVersion}")
    annotationProcessor("com.velocitypowered:velocity-api:${velocityApiVersion}")

    // FastLogin API — compileOnly since FastLogin is a separate plugin at runtime
    compileOnly("com.github.games647:fastlogin.core:${fastLoginVersion}")
    compileOnly("com.github.games647:fastlogin.bukkit:${fastLoginVersion}")
    compileOnly("com.github.games647:fastlogin.bungee:${fastLoginVersion}")
    compileOnly("com.github.games647:fastlogin.velocity:${fastLoginVersion}")
}

tasks {
    register("installBun") {
        doLast {
            if (!bunBin.exists()) {
                println("🚀 Couldn't find Bun, downloading: $bunUrl")

                val zipFile = File(bunDir, "$bunPlatform.zip")
                zipFile.parentFile.mkdirs()

                URI(bunUrl).toURL().openStream().use { input ->
                    zipFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                copy {
                    from(zipTree(zipFile))
                    into(bunDir)
                }

                if (!isWindows) {
                    bunBin.setExecutable(true)
                }

                zipFile.delete()
                println("✅ Bun successfully downloaded: ${bunBin.absolutePath}")
            } else {
                println("✅ Bun is downloaded already: ${bunBin.absolutePath}")
            }
        }
    }

    register("buildUI", Exec::class) {
        dependsOn("installBun")
        println(bunBin.absolutePath)
        commandLine(bunBin.absolutePath, "run", "build")
    }

    register("zipPluginUI", Zip::class) {
        dependsOn("buildUI")

        from("src/main/resources/plugin-ui")
        archiveFileName.set("plugin-ui.zip")
        destinationDirectory.set(file("src/main/resources"))

        doLast {
            val pluginUIFolder = file("src/main/resources/plugin-ui")
            if (pluginUIFolder.exists()) {
                pluginUIFolder.deleteRecursively()
            }
        }

        outputs.upToDateWhen { false }
    }

    shadowJar {
        manifest {
            attributes["id"] = pluginId
            panoApiLevel?.let { attributes["api-level"] = it }
            attributes["name"] = pluginName
            pluginDescription?.let { attributes["description"] = it }
            attributes["pano-version"] = pluginPanoVersion
            attributes["main-class"] = pluginClass
            attributes["version"] = version
            attributes["developer"] = pluginDeveloper
            pluginLicense?.let { attributes["license"] = it }
            pluginSourceUrl?.let { attributes["source-url"] = it }
            pluginDependencies?.let { attributes["dependencies"] = it }
            pluginRequires?.let { attributes["requires"] = it }
        }

        archiveFileName.set("$pluginId-$version.jar")

        dependencies {
            exclude(dependency("io.vertx:vertx-core"))
            exclude {
                it.moduleGroup == "io.netty" || it.moduleGroup == "org.slf4j"
            }
        }
    }

    register("copyJar") {
        pluginsDir?.let {
            doLast {
                copy {
                    from(shadowJar.get().archiveFile.get().asFile.absolutePath)
                    into(it)
                }
            }
        }

        outputs.upToDateWhen { false }
        mustRunAfter(shadowJar)
    }

    jar {
        enabled = false
        dependsOn(shadowJar)
        dependsOn("copyJar")
    }
}

tasks.named("build") {
    if (!noui) {
        dependsOn("zipPluginUI")
    }
}

tasks.named<ProcessResources>("processResources") {
    if (!noui) {
        dependsOn("zipPluginUI")
    }

    filesMatching(listOf("plugin.yml", "bungee.yml", "velocity-plugin.json")) {
        expand(mapOf("version" to version))
    }
}

publishing {
    repositories {
        maven {
            name = pluginId
            url = uri("https://maven.pkg.github.com/$organization/$pluginId")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME_GITHUB")
                password = project.findProperty("gpr.token") as String? ?: System.getenv("TOKEN_GITHUB")
            }
        }
    }

    publications {
        create<MavenPublication>("shadow") {
            artifactId = pluginId
            from(components["shadow"])
        }
    }
}

// The jar runs on Pano (Java 11) and its mc/ classes on Minecraft servers (Java 17+). It is compiled by a JDK 17 (the
// server APIs on the classpath and kapt's javac need it) but emits Java 11 bytecode against the Java 11 API, so the Pano
// side loads on JRE 11; the Pano side never touches the mc/ classes.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        freeCompilerArgs.add("-Xjdk-release=11")
    }
}

configurations.named("compileClasspath") {
    attributes { attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17) }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
