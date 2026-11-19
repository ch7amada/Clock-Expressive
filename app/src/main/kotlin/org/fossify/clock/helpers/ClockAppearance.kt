package org.fossify.clock.helpers

enum class ClockThemeMode {
    System, Dark, Light;

    fun isDark(systemDark: Boolean): Boolean = when (this) {
        System -> systemDark
        Dark -> true
        Light -> false
    }
}

enum class ClockPalette {
    System, Indigo, Teal, Violet, Plum,
}

data class ClockAppearance(
    val mode: ClockThemeMode = ClockThemeMode.System,
    val palette: ClockPalette = ClockPalette.System,
)
