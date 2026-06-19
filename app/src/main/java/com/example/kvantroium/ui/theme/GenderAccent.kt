package com.example.kvantroium.ui.theme

enum class GenderAccent {
    Male,
    Female,
    Unknown;

    companion object {
        fun fromRaw(value: String?): GenderAccent {
            val normalized = value?.trim()?.lowercase().orEmpty()
            return when (normalized) {
                "м", "m", "male", "мужской" -> Male
                "ж", "f", "female", "женский" -> Female
                else -> Unknown
            }
        }
    }
}

fun genderAccentColors(accent: GenderAccent, darkTheme: Boolean): KvantExtendedColors {
    if (darkTheme) return DarkKvantColors
    return when (accent) {
        GenderAccent.Female -> FemaleLightKvantColors
        else -> LightKvantColors
    }
}
