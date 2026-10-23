package com.vndblite.dcj.ui.components

import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.GenrePreset
import com.vndblite.dcj.domain.model.Language
import com.vndblite.dcj.domain.model.Platform
import com.vndblite.dcj.domain.model.ThemeId
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class AssetPathsTest {

    private fun resolveAssetsRoot(): File {
        val candidates = listOf(
            File("src/main/assets"),
            File("app/src/main/assets"),
            File("../assets"),
            File("assets")
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Could not locate assets directory from working directory ${File(".").absolutePath}")
    }

    @Test
    fun `every referenced asset path exists as a non-empty file on disk`() {
        val assetsRoot = resolveAssetsRoot()
        val allPaths = AssetPaths.allReferencedAssetPaths()
        assertTrue(allPaths.isNotEmpty(), "Expected non-empty set of referenced asset paths")

        allPaths.forEach { relativePath ->
            val assetFile = File(assetsRoot, relativePath)
            assertTrue(
                assetFile.isFile && assetFile.length() > 0L,
                "Missing or empty asset file at ${assetFile.path} (referenced as '$relativePath')"
            )
        }
    }

    @Test
    fun `all 11 themes 34 languages 6 statuses and 8 genre presets map to distinct existing assets`() {
        val assetsRoot = resolveAssetsRoot()

        val themePaths = ThemeId.entries.map { AssetPaths.themeBackgroundPath(it) }.toSet()
        assertEquals(11, themePaths.size)

        val flagPaths = Language.entries.map { AssetPaths.languageFlagPath(it) }.toSet()
        assertEquals(34, flagPaths.size)

        val statusPaths = CollectionStatus.entries.map { AssetPaths.collectionStatusIconPath(it) }.toSet()
        assertEquals(6, statusPaths.size)

        val genrePaths = GenrePreset.entries.map { AssetPaths.genreArtPath(it) }.toSet()
        assertEquals(8, genrePaths.size)

        Platform.entries.forEach { platform ->
            val path = AssetPaths.platformIconPath(platform)
            assertTrue(File(assetsRoot, path).isFile, "Missing platform icon for ${platform.code}: $path")
        }
    }
}
