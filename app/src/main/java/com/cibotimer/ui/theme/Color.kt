package com.cibotimer.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// CIBOTIMER BRAND ACCENTS (DAL BLU ALL'AZZURRO)
// ==========================================
val CiboTimerAzure = Color(0xFF00B4D8)           // Vibrant Bright Azure / Cyan
val CiboTimerAzureLight = Color(0xFF38BDF8)      // Sky Azure
val CiboTimerAzureDark = Color(0xFF0077B6)       // Deep Azure Cobalt
val CiboTimerAzureGlow = Color(0xFF00E5FF)       // Electric Cyan Neon Glow
val CiboTimerAzureContainer = Color(0xFF0A2E3D)  // Dark Mode container

val CiboTimerBlue = Color(0xFF0284C7)            // Electric Cobalt Blue
val CiboTimerBlueDark = Color(0xFF0369A1)
val CiboTimerBlueLight = Color(0xFFBAE6FD)
val CiboTimerBlueContainer = Color(0xFF0C2B42)   // Dark Mode container

val FastingBlue = Color(0xFF38BDF8)              // Azzurro esatto della scritta "Digiunare" nella schermata Digiuno (#38BDF8)
val FastingBlueDarkText = Color(0xFF04202E)      // Blu notte molto scuro per testo e icone su FastingBlue (~#04202E)
val FastingBlueDark = Color(0xFF005A9C)
val FastingBlueContainer = Color(0xFF082038)
val SlateGray = Color(0xFF6B7A90)                // Grigio-azzurro per bordi a riposo e switch spento (~#6B7A90)
val TextSecondaryLight = Color(0xFFE2E8F0)       // Grigio chiaro ad alto contrasto (>10:1 su #131821) per testi descrittivi (~#E2E8F0)

val EatingAzure = Color(0xFF00E5FF)              // Azure/Cyan for Eating (Mangiare)
val EatingAzureDark = Color(0xFF0096C7)
val EatingAzureContainer = Color(0xFF072738)

val CiboTimerGreen = Color(0xFF00B4D8)           // Kept as alias to Azure for legacy references
val CiboTimerGreenLight = Color(0xFF38BDF8)
val CiboTimerGreenDark = Color(0xFF0077B6)
val CiboTimerGreenContainer = Color(0xFF0A283C)

val CiboTimerOrange = Color(0xFFFF9F1C)          // High-contrast Warm Orange
val CiboTimerOrangeDark = Color(0xFFEA580C)
val CiboTimerOrangeContainer = Color(0xFF381F05)

val CiboTimerAmberStart = Color(0xFFFF7A00)       // Sunset Orange for Interrompi CTA
val CiboTimerAmberEnd = Color(0xFFFFB020)

val CardBorderGlow = Color(0xFF1E3A5F)           // Premium subtle card border
val CardBgTranslucent = Color(0xFF0C1726)        // Luxury dark card background
val CardBgSecondary = Color(0xFF0E1F33)

// Deprecated aliases for compatibility
@Deprecated("Usa CiboTimerAzure", ReplaceWith("CiboTimerAzure"))
val BodyFastTeal = CiboTimerAzure
@Deprecated("Usa CiboTimerAzureDark", ReplaceWith("CiboTimerAzureDark"))
val BodyFastTealDark = CiboTimerAzureDark
@Deprecated("Usa CiboTimerAzureContainer", ReplaceWith("CiboTimerAzureContainer"))
val BodyFastTealLight = CiboTimerAzureContainer
@Deprecated("Usa Color(0xFF0E384D)")
val BodyFastTealSubtle = Color(0xFF0E384D)
@Deprecated("Usa CiboTimerAzure", ReplaceWith("CiboTimerAzure"))
val BodyFastAccent = CiboTimerAzure

@Deprecated("Usa CiboTimerOrange", ReplaceWith("CiboTimerOrange"))
val BodyFastOrange = CiboTimerOrange
@Deprecated("Usa CiboTimerOrangeDark", ReplaceWith("CiboTimerOrangeDark"))
val BodyFastOrangeDark = CiboTimerOrangeDark
@Deprecated("Usa CiboTimerOrangeContainer", ReplaceWith("CiboTimerOrangeContainer"))
val BodyFastOrangeLight = CiboTimerOrangeContainer

@Deprecated("Usa CiboTimerAzure", ReplaceWith("CiboTimerAzure"))
val BodyFastBlue = CiboTimerAzure
@Deprecated("Usa CiboTimerAzureDark", ReplaceWith("CiboTimerAzureDark"))
val BodyFastBlueDark = CiboTimerAzureDark
@Deprecated("Usa CiboTimerAzureContainer", ReplaceWith("CiboTimerAzureContainer"))
val BodyFastBlueLight = CiboTimerAzureContainer

// ==========================================
// DARK THEME PALETTE
// ==========================================
val BackgroundDark = Color(0xFF0A0D12)          // Deep OLED background
val SurfaceDark = Color(0xFF131821)             // Rich dark card surface
val SurfaceVariantDark = Color(0xFF1C2330)      // High-contrast nested container
val BorderDark = Color(0xFF263242)              // Visible boundary borders
val CardBackground = SurfaceDark
val DividerColor = BorderDark

val TextPrimaryDark = Color(0xFFFFFFFF)         // 100% pure high-contrast white
val TextSecondaryDark = Color(0xFF94A3B8)       // Cool clear slate
val TextTertiaryDark = Color(0xFF64748B)

// Button high-contrast text
val ButtonTextOnAccent = Color(0xFF031019)      // Crisp black on bright azure
val ButtonTextOnDark = Color(0xFFFFFFFF)

// Aliases
val BackgroundLight = BackgroundDark
val SurfaceLight = SurfaceDark
val TextPrimary = TextPrimaryDark
val TextSecondary = TextSecondaryDark

// ==========================================
// METABOLIC STAGES
// ==========================================
val Stage1Blue = Color(0xFF38BDF8)
val Stage2LightBlue = Color(0xFF00B4D8)
val Stage3Orange = Color(0xFFFB923C)
val Stage4Pink = Color(0xFFF472B6)
val Stage5Purple = Color(0xFFC084FC)
val Stage6DeepPurple = Color(0xFFA78BFA)
val Stage7Green = Color(0xFF34D399)

// ==========================================
// COMPACT TIMER SCREEN PALETTE (SPEC CIBOTIMER)
// ==========================================
val CiboBg = Color(0xFF0B1220)              // Sfondo ~#0B1220
val CiboCard = Color(0xFF141D30)            // Card ~#141D30
val CiboCardBorder = Color(0xFF1E2D4A)      // Bordo card
val CiboTextPrimary = Color(0xFFFFFFFF)     // Testo primario bianco puro
val CiboTextSecondary = Color(0xFFA8B3C7)   // Testo secondario ~#A8B3C7 (>4.5:1 contrast)
val CiboAccent = FastingBlue          // Azzurro #38BDF8 per digiuno, anello, giorno attivo (identico a FastingBlue)
val CiboMealGreen = Color(0xFF10B981)       // Bel verde smeraldo fresco per i pasti / finestra di alimentazione
val CiboMealGreenLight = Color(0xFF34D399)  // Verde menta chiaro per highlight
val CiboAmber = CiboMealGreen               // Alias pasti per compatibilità: verde smeraldo al posto del giallo
val CiboStopRed = Color(0xFFEF4444)         // Rosso brillante per il pulsante Stop/Interrompi
val CiboStopOrange = CiboStopRed            // Alias compatibilità verso il nuovo rosso
val CiboTrack = Color(0xFF162238)           // Traccia tenue per l'anello e le barre di avanzamento

// Modifica Piano Bottom Sheet
val CiboSheetBg = Color(0xFF101A2D)         // Sfondo sheet ~#101A2D
val CiboSheetCard = Color(0xFF172238)       // Card modal ~#172238
val CiboStepperTonal = Color(0xFF26385A)    // Stepper minus button tonal #26385A
val CiboButtonDarkText = Color(0xFF0B1220)  // Testo scuro su pulsante Salva pill


