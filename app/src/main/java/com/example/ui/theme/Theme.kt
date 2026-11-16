package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.data.model.AppThemeMode

private val NovaDarkColorScheme = darkColorScheme(
  primary = NovaCyan,
  onPrimary = NovaDeepNavy,
  primaryContainer = NovaElevatedNavy,
  onPrimaryContainer = NovaCyan,
  secondary = NovaElectricBlue,
  onSecondary = NovaTextPrimaryDark,
  secondaryContainer = NovaCardGlass,
  onSecondaryContainer = NovaTextPrimaryDark,
  tertiary = NovaViolet,
  onTertiary = NovaTextPrimaryDark,
  background = NovaDeepNavy,
  onBackground = NovaTextPrimaryDark,
  surface = NovaMidnightSurface,
  onSurface = NovaTextPrimaryDark,
  surfaceVariant = NovaElevatedNavy,
  onSurfaceVariant = NovaTextSecondaryDark,
  outline = NovaBorderNavy,
  error = NovaCoral,
  onError = NovaTextPrimaryDark,
)

private val NovaLightColorScheme = lightColorScheme(
  primary = NovaBluePrimaryLight,
  onPrimary = NovaLightSurface,
  primaryContainer = NovaLightSurfaceVariant,
  onPrimaryContainer = NovaTextPrimaryLight,
  secondary = NovaIndigoLight,
  onSecondary = NovaLightSurface,
  secondaryContainer = NovaLightSurfaceVariant,
  onSecondaryContainer = NovaTextPrimaryLight,
  tertiary = NovaVioletLight,
  onTertiary = NovaLightSurface,
  background = NovaLightBackground,
  onBackground = NovaTextPrimaryLight,
  surface = NovaLightSurface,
  onSurface = NovaTextPrimaryLight,
  surfaceVariant = NovaLightSurfaceVariant,
  onSurfaceVariant = NovaTextSecondaryLight,
  outline = NovaLightBorder,
  error = NovaCoral,
  onError = NovaLightSurface,
)

val NovaShapes = Shapes(
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(18.dp),
  large = RoundedCornerShape(24.dp),
  extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun NovaVpnTheme(
  themeMode: AppThemeMode = AppThemeMode.DARK,
  content: @Composable () -> Unit,
) {
  val isDark = when (themeMode) {
    AppThemeMode.DARK -> true
    AppThemeMode.LIGHT -> false
    AppThemeMode.SYSTEM -> isSystemInDarkTheme()
  }

  val colorScheme = if (isDark) NovaDarkColorScheme else NovaLightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = NovaTypography,
    shapes = NovaShapes,
    content = content,
  )
}
