import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.roborazzi) apply false
}

subprojects {
    tasks.withType<Test>().configureEach {
        val xfsTestTmp = rootProject.layout.buildDirectory
            .dir("tmp/unitTestJvm/${project.path.trimStart(':').replace(':', '_')}")
            .get()
            .asFile
        doFirst {
            xfsTestTmp.mkdirs()
        }
        maxHeapSize = "1024m"
        maxParallelForks = 1
        jvmArgs(
            "-XX:MaxMetaspaceSize=384m",
            "-XX:-UsePerfData",
            "-Djava.io.tmpdir=${xfsTestTmp.absolutePath}",
            "-Dorg.conscrypt.native.workdir=${xfsTestTmp.absolutePath}",
            "-Djna.tmpdir=${xfsTestTmp.absolutePath}"
        )
        systemProperty("java.io.tmpdir", xfsTestTmp.absolutePath)
        systemProperty("org.conscrypt.native.workdir", xfsTestTmp.absolutePath)
        systemProperty("jna.tmpdir", xfsTestTmp.absolutePath)
        systemProperty("robolectric.graphicsMode", "NATIVE")
        systemProperty("robolectric.pixelCopyRenderMode", "hardware")
        testLogging {
            events("failed")
            exceptionFormat = TestExceptionFormat.FULL
            showStandardStreams = false
        }
    }
}
