plugins {
    kotlin("jvm") version "2.0.21"
}

group = "com.fossift.asciicam"
version = "0.1.0"

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
