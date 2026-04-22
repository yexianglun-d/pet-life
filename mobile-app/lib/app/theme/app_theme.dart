import 'package:flutter/material.dart';

/// 应用主题定义。
///
/// 新版视觉基线不再沿用冷淡工具风，而是围绕“温暖、轻松、陪伴感”建立统一样式。
abstract final class AppTheme {
  static ThemeData get lightTheme {
    final ColorScheme colorScheme = ColorScheme.fromSeed(
      seedColor: AppThemePalette.primary,
      brightness: Brightness.light,
      surface: AppThemePalette.surface,
    ).copyWith(
      primary: AppThemePalette.primary,
      onPrimary: Colors.white,
      secondary: AppThemePalette.mint,
      onSecondary: AppThemePalette.title,
      error: AppThemePalette.danger,
      onError: Colors.white,
      surface: AppThemePalette.surface,
      onSurface: AppThemePalette.title,
      outline: AppThemePalette.line,
      surfaceContainerHighest: AppThemePalette.surfaceRaised,
    );

    final TextTheme textTheme = Typography.blackMountainView.copyWith(
      headlineSmall: const TextStyle(
        fontSize: 30,
        fontWeight: FontWeight.w700,
        color: AppThemePalette.title,
        height: 1.15,
      ),
      titleLarge: const TextStyle(
        fontSize: 22,
        fontWeight: FontWeight.w700,
        color: AppThemePalette.title,
        height: 1.2,
      ),
      titleMedium: const TextStyle(
        fontSize: 17,
        fontWeight: FontWeight.w700,
        color: AppThemePalette.title,
        height: 1.3,
      ),
      bodyLarge: const TextStyle(
        fontSize: 16,
        fontWeight: FontWeight.w500,
        color: AppThemePalette.body,
        height: 1.6,
      ),
      bodyMedium: const TextStyle(
        fontSize: 14,
        fontWeight: FontWeight.w500,
        color: AppThemePalette.body,
        height: 1.6,
      ),
      bodySmall: const TextStyle(
        fontSize: 12,
        fontWeight: FontWeight.w600,
        color: AppThemePalette.muted,
        height: 1.5,
      ),
      labelLarge: const TextStyle(
        fontSize: 15,
        fontWeight: FontWeight.w700,
        color: AppThemePalette.title,
      ),
    );

    return ThemeData(
      useMaterial3: true,
      colorScheme: colorScheme,
      textTheme: textTheme,
      scaffoldBackgroundColor: AppThemePalette.background,
      splashFactory: InkSparkle.splashFactory,
      appBarTheme: const AppBarTheme(
        backgroundColor: AppThemePalette.background,
        foregroundColor: AppThemePalette.title,
        elevation: 0,
        centerTitle: false,
      ),
      cardTheme: const CardThemeData(
        color: AppThemePalette.surface,
        elevation: 0,
        margin: EdgeInsets.zero,
      ),
      dividerTheme: const DividerThemeData(
        color: AppThemePalette.line,
        thickness: 1,
        space: 1,
      ),
      navigationBarTheme: NavigationBarThemeData(
        height: 76,
        backgroundColor: AppThemePalette.surface,
        surfaceTintColor: Colors.transparent,
        indicatorColor: AppThemePalette.navIndicator,
        labelTextStyle: WidgetStatePropertyAll<TextStyle>(
          textTheme.bodySmall!.copyWith(
            fontWeight: FontWeight.w700,
          ),
        ),
        iconTheme: WidgetStateProperty.resolveWith<IconThemeData>(
          (Set<WidgetState> states) {
            final bool isSelected = states.contains(WidgetState.selected);
            return IconThemeData(
              color: isSelected
                  ? AppThemePalette.primaryDeep
                  : AppThemePalette.muted,
              size: 24,
            );
          },
        ),
      ),
      chipTheme: ChipThemeData(
        backgroundColor: AppThemePalette.warmTint,
        side: BorderSide.none,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(999),
        ),
        labelStyle: textTheme.bodySmall!.copyWith(
          color: AppThemePalette.title,
        ),
        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: AppThemePalette.primary,
          foregroundColor: Colors.white,
          disabledBackgroundColor: AppThemePalette.line,
          disabledForegroundColor: AppThemePalette.muted,
          elevation: 0,
          minimumSize: const Size.fromHeight(52),
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(20),
          ),
          textStyle: textTheme.labelLarge,
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: AppThemePalette.title,
          side: const BorderSide(color: AppThemePalette.line),
          minimumSize: const Size.fromHeight(52),
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(20),
          ),
          textStyle: textTheme.labelLarge,
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: AppThemePalette.surfaceRaised,
        hintStyle: textTheme.bodyMedium?.copyWith(
          color: AppThemePalette.muted,
        ),
        labelStyle: textTheme.bodyMedium?.copyWith(
          color: AppThemePalette.muted,
        ),
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 18,
          vertical: 18,
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(22),
          borderSide: BorderSide.none,
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(22),
          borderSide: const BorderSide(color: AppThemePalette.line),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(22),
          borderSide: const BorderSide(
            color: AppThemePalette.primary,
            width: 1.5,
          ),
        ),
      ),
      snackBarTheme: SnackBarThemeData(
        backgroundColor: AppThemePalette.title,
        contentTextStyle: textTheme.bodyMedium?.copyWith(
          color: Colors.white,
        ),
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(18),
        ),
      ),
    );
  }
}

/// 应用主色板。
abstract final class AppThemePalette {
  static const Color background = Color(0xFFFFF8F2);
  static const Color surface = Color(0xFFFFFDFC);
  static const Color surfaceRaised = Color(0xFFFFF3E9);
  static const Color warmTint = Color(0xFFFFE9D8);
  static const Color navIndicator = Color(0xFFFFE2D1);
  static const Color line = Color(0xFFF0DDCD);
  static const Color title = Color(0xFF5E473A);
  static const Color body = Color(0xFF7A6256);
  static const Color muted = Color(0xFFA08779);
  static const Color primary = Color(0xFFE68C72);
  static const Color primaryDeep = Color(0xFFC9674F);
  static const Color mint = Color(0xFFA8C8A3);
  static const Color butter = Color(0xFFF4D793);
  static const Color sky = Color(0xFFB6DAE6);
  static const Color rose = Color(0xFFF1C5B8);
  static const Color danger = Color(0xFFD97669);
  static const Color success = Color(0xFF78A47F);

  static const List<BoxShadow> softShadow = <BoxShadow>[
    BoxShadow(
      color: Color(0x14000000),
      blurRadius: 26,
      offset: Offset(0, 12),
    ),
  ];
}
