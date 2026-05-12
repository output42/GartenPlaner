package de.gartenplaner.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import de.gartenplaner.R

// ── Font-Familien ─────────────────────────────────────────────────────────────
// Font-Dateien müssen in res/font/ abgelegt werden.
// Siehe res/font/FONTS.md für Download-Anleitung.

val NunitoFamily = FontFamily(
    Font(R.font.nunito_regular,   FontWeight.Normal),
    Font(R.font.nunito_semibold,  FontWeight.SemiBold),
    Font(R.font.nunito_bold,      FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)

val DmMonoFamily = FontFamily(
    Font(R.font.dm_mono_regular, FontWeight.Normal),
    Font(R.font.dm_mono_medium,  FontWeight.Medium),
)

// ── Material 3 Typography ─────────────────────────────────────────────────────

val GartenPlanerTypography = Typography(
    displayLarge  = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.ExtraBold, fontSize = 57.sp),
    displayMedium = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.ExtraBold, fontSize = 45.sp),
    displaySmall  = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.Bold,      fontSize = 36.sp),
    headlineLarge = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.Bold,      fontSize = 32.sp),
    headlineMedium= TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.Bold,      fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.Bold,      fontSize = 24.sp),
    titleLarge    = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp),
    titleMedium   = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.Bold,      fontSize = 16.sp),
    titleSmall    = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.SemiBold,  fontSize = 14.sp),
    bodyLarge     = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.Normal,    fontSize = 16.sp),
    bodyMedium    = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.Normal,    fontSize = 14.sp),
    bodySmall     = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.Normal,    fontSize = 12.sp),
    labelLarge    = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.Bold,      fontSize = 14.sp),
    labelMedium   = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.Bold,      fontSize = 12.sp),
    labelSmall    = TextStyle(fontFamily = DmMonoFamily, fontWeight = FontWeight.Medium,    fontSize = 10.sp),
)
