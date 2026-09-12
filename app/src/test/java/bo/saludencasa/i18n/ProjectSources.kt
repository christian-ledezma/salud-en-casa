package bo.saludencasa.i18n

import java.io.File

// Shared file lookup for the static-analysis tests in this package. JVM unit
// tests have no access to the Android resource system, so these tests read
// the source tree and the resource files directly from disk.
internal object ProjectSources {
    private val localeValuesDirName = Regex("""^values(-[a-z]{2}(-r[A-Z]{2})?)?$""")

    fun projectRoot(): File {
        val workingDir = System.getProperty("user.dir") ?: error("The user.dir system property is not set.")
        var dir = File(workingDir).absoluteFile
        while (!File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile ?: error("Could not locate the project root from $workingDir.")
        }
        return dir
    }

    fun mainKotlinFiles(): List<File> =
        File(projectRoot(), "app/src/main/java")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

    fun localeStringsXmlFiles(): List<File> {
        val resDir = File(projectRoot(), "app/src/main/res")
        return resDir
            .listFiles()
            .orEmpty()
            .filter { it.isDirectory && localeValuesDirName.matches(it.name) }
            .mapNotNull { dir -> File(dir, "strings.xml").takeIf(File::exists) }
    }
}
