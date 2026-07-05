plugins {
    kotlin("jvm") version "2.3.20"
    id("com.gradleup.shadow") version "8.3.0"
}

group = "com.timaf"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}

dependencies {
    // Sadece public Paper API kullanıyoruz, NMS gerekmiyor.
    // Bu yüzden paperweight-userdev'e ihtiyacımız yok, sade compileOnly yeterli.
    // 26.1'den itibaren versiyon formatı değişti: <mcversion>.build.<build>
    // ".build.+" en son build'i otomatik çeker (eski SNAPSHOT mantığının karşılığı).
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
}

val targetJavaVersion = 25
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
