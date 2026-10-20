package com.vndblite.dcj.ui.theme

import com.vndblite.dcj.domain.model.ThemeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeSpecsTest {

    @Test
    fun `all 11 ThemeId values have matching ThemeSpecs in canonical order`() {
        assertEquals(11, ThemeSpecs.all.size)
        assertEquals(ThemeId.entries, ThemeSpecs.all.map { it.id })
        ThemeId.entries.forEach { id ->
            assertEquals(id, ThemeSpecs.forId(id).id)
        }
    }

    @Test
    fun `hasDarkSurface classifies Ever17 and Touhou as light surfaces (B7, F7)`() {
        val ev17 = ThemeSpecs.forId(ThemeId.EV17)
        val toho = ThemeSpecs.forId(ThemeId.TOHO)

        assertFalse("Ever17 (#6EC8D2) is a light surface", ev17.hasDarkSurface)
        assertEquals("Light Palette", ev17.paletteLabel)

        assertFalse("Touhou (#AFAFAF) is a light surface", toho.hasDarkSurface)
        assertEquals("Light Palette", toho.paletteLabel)
    }

    @Test
    fun `hasDarkSurface matches relative luminance threshold across all 11 themes (B7, F7)`() {
        val expectedDarkIds = setOf(
            ThemeId.AGL,
            ThemeId.FATE,
            ThemeId.CARNVL,
            ThemeId.TSUKHME,
            ThemeId.SAYA
        )
        val expectedLightIds = setOf(
            ThemeId.AIR,
            ThemeId.EV17,
            ThemeId.HGRSHI,
            ThemeId.TOHO,
            ThemeId.SEINRKN,
            ThemeId.LILBUSTS
        )

        ThemeSpecs.all.forEach { spec ->
            if (spec.id in expectedDarkIds) {
                assertTrue("${spec.id} should have dark surface", spec.hasDarkSurface)
                assertEquals("Dark Palette", spec.paletteLabel)
            } else {
                assertTrue("${spec.id} should be in expectedLightIds", spec.id in expectedLightIds)
                assertFalse("${spec.id} should have light surface", spec.hasDarkSurface)
                assertEquals("Light Palette", spec.paletteLabel)
            }
        }
    }

    @Test
    fun `all 11 themes satisfy WCAG AA 4_5 to 1 contrast ratio for body text (U2)`() {
        ThemeSpecs.all.forEach { spec ->
            val primaryContrast = spec.surfaceTextContrastRatio()
            val variantContrast = spec.surfaceVariantTextContrastRatio()

            assertTrue(
                "${spec.id} primary text contrast ($primaryContrast) must be >= $MIN_WCAG_AA_CONTRAST_RATIO",
                primaryContrast >= MIN_WCAG_AA_CONTRAST_RATIO
            )
            assertTrue(
                "${spec.id} variant text contrast ($variantContrast) must be >= $MIN_WCAG_AA_CONTRAST_RATIO",
                variantContrast >= MIN_WCAG_AA_CONTRAST_RATIO
            )
        }
    }

    @Test
    fun `buildColorScheme maps surface accent and onSurface consistently (U2, A9)`() {
        ThemeSpecs.all.forEach { spec ->
            val scheme = ThemeSpecs.buildColorScheme(spec)
            assertEquals(spec.accentColor, scheme.primary)
            assertEquals(spec.surfaceColor, scheme.surface)
            assertEquals(spec.onSurfaceColor, scheme.onSurface)
            assertEquals(spec.surfaceColor, scheme.background)
            assertEquals(spec.onSurfaceColor, scheme.onBackground)
        }
    }

    @Test
    fun `buildVndbTypography scales all 15 Material 3 text styles by fontSizeOffset (U2)`() {
        val base = buildVndbTypography(0f)
        val scaled = buildVndbTypography(2f)

        assertEquals(base.displayLarge.fontSize.value + 2f, scaled.displayLarge.fontSize.value, 0.01f)
        assertEquals(base.displayMedium.fontSize.value + 2f, scaled.displayMedium.fontSize.value, 0.01f)
        assertEquals(base.displaySmall.fontSize.value + 2f, scaled.displaySmall.fontSize.value, 0.01f)
        assertEquals(base.headlineLarge.fontSize.value + 2f, scaled.headlineLarge.fontSize.value, 0.01f)
        assertEquals(base.headlineMedium.fontSize.value + 2f, scaled.headlineMedium.fontSize.value, 0.01f)
        assertEquals(base.headlineSmall.fontSize.value + 2f, scaled.headlineSmall.fontSize.value, 0.01f)
        assertEquals(base.titleLarge.fontSize.value + 2f, scaled.titleLarge.fontSize.value, 0.01f)
        assertEquals(base.titleMedium.fontSize.value + 2f, scaled.titleMedium.fontSize.value, 0.01f)
        assertEquals(base.titleSmall.fontSize.value + 2f, scaled.titleSmall.fontSize.value, 0.01f)
        assertEquals(base.bodyLarge.fontSize.value + 2f, scaled.bodyLarge.fontSize.value, 0.01f)
        assertEquals(base.bodyMedium.fontSize.value + 2f, scaled.bodyMedium.fontSize.value, 0.01f)
        assertEquals(base.bodySmall.fontSize.value + 2f, scaled.bodySmall.fontSize.value, 0.01f)
        assertEquals(base.labelLarge.fontSize.value + 2f, scaled.labelLarge.fontSize.value, 0.01f)
        assertEquals(base.labelMedium.fontSize.value + 2f, scaled.labelMedium.fontSize.value, 0.01f)
        assertEquals(base.labelSmall.fontSize.value + 2f, scaled.labelSmall.fontSize.value, 0.01f)
    }
}
