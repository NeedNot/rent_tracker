package net.neednot.renttracker.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import kotlin.test.Test
import kotlin.test.assertTrue

class ArchitectureTest {
    private val productionFiles = Konsist.scopeFromProduction().files

    @Test
    fun featureNeverImportsAnotherFeature() {
        assertNoViolations(
            productionFiles.flatMap { file ->
                val feature = file.packageName.featureName() ?: return@flatMap emptyList()
                file.importNames
                    .filter { it.featureName() != null && it.featureName() != feature }
                    .map { "${file.path} imports $it" }
            },
        )
    }

    @Test
    fun coreNeverImportsFeature() {
        assertNoViolations(
            productionFiles
                .filter { it.packageName.startsWith(CORE) }
                .flatMap { file ->
                    file.importNames.filter { it.startsWith(FEATURE) }.map { "${file.path} imports $it" }
                },
        )
    }

    @Test
    fun modelImportsOnlySerialization() {
        assertNoViolations(
            productionFiles
                .filter { it.packageName.startsWith(MODEL) }
                .flatMap { file ->
                    file.importNames
                        .filterNot { it.startsWith("kotlinx.serialization.") || it.startsWith(MODEL) }
                        .map { "${file.path} imports $it" }
                },
        )
    }

    @Test
    fun onlyDataPackagesImportModel() {
        assertNoViolations(
            productionFiles
                .filterNot { it.packageName.startsWith(MODEL) || "data" in it.packageName.split(".") }
                .flatMap { file ->
                    file.importNames.filter { it.startsWith(MODEL) }.map { "${file.path} imports $it" }
                },
        )
    }

    @Test
    fun presentationNeverImportsItsFeatureData() {
        assertNoViolations(
            productionFiles
                .filter { it.packageName.startsWith(FEATURE) && ".presentation" in it.packageName }
                .flatMap { file ->
                    val featureData = "$FEATURE${file.packageName.featureName()}.data"
                    file.importNames.filter { it.startsWith(featureData) }.map { "${file.path} imports $it" }
                },
        )
    }

    @Test
    fun commonCodeHasNoAndroidOrJavaImports() {
        val platformPrefixes = listOf("android.", "java.", "javax.")
        assertNoViolations(
            Konsist
                .scopeFromProject()
                .files
                .filter { "/src/commonMain/" in it.path }
                .flatMap { file ->
                    file.importNames
                        .filter { name -> platformPrefixes.any(name::startsWith) }
                        .map { "${file.path} imports $it" }
                },
        )
    }

    private val KoFileDeclaration.packageName: String
        get() = packagee?.name.orEmpty()

    private val KoFileDeclaration.importNames: List<String>
        get() = imports.map { it.name }

    private fun String.featureName(): String? {
        if (!startsWith(FEATURE)) return null
        return removePrefix(FEATURE).substringBefore(".")
    }

    private fun assertNoViolations(violations: List<String>) {
        assertTrue(violations.isEmpty(), violations.joinToString(separator = "\n", prefix = "\n"))
    }

    private companion object {
        const val ROOT = "net.neednot.renttracker"
        const val CORE = "$ROOT.core."
        const val FEATURE = "$ROOT.feature."
        const val MODEL = "$ROOT.model"
    }
}
