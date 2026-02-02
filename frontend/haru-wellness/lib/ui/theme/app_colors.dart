import 'package:flutter/material.dart';

class AppColors {
  AppColors._();

  // Primary colors
  static const Color primary = Color(0xFF27CC74);
  static const Color primaryDark = Color(0xFF1A8A4F);
  static const Color primaryLight = Color(0xFFA9E8BC);
  static const Color primaryLighter = Color(0xFFD4F4E0);

  // Background colors
  static const Color background = Color(0xFFFFFFFF);
  static const Color backgroundOverlay = Color(0xFFD9D9D9);

  // Text colors
  static const Color textPrimary = Color(0xFF0A3D1F);
  static const Color textSecondary = Color(0xFF27CC74);
  static const Color textTertiary = Color(0xFF6DB88A);

  // Border colors
  static const Color borderPrimary = Color(0xFFD4F4E0);
  static const Color borderSecondary = Color(0xFFDDDDDD);

  // Button colors
  static const Color buttonPrimary = Color(0xFF26C66A);
  static const Color buttonText = Color(0xFFFFFFFF);

  // Gradient colors for image overlay
  static const List<Color> imageOverlayGradient = [
    Color(0x0027CC74), // rgba(39,204,116,0)
    Color(0x591A8A4F), // rgba(26,138,79,0.35)
    Color(0xB3105432), // rgba(16,84,50,0.7)
  ];

  // Card colors
  static const Color cardBackground = Color(0xFFF4FAF6);
  static const Color cardBackgroundAlt = Color(0xFFEAF5EE);
  static const Color cardBorder = Color(0xFFEAF5EE);

  // Navigation colors
  static const Color navActive = Color(0xFF1A8A4F);
  static const Color navInactive = Color(0xFFA9E8BC);
  static const Color navBorder = Color(0x4D1A8A4F); // rgba(26,138,79,0.3)

  // Emotion colors
  static const Color emotionPositive = Color(0xFF27CC74);
  static const Color emotionNegative = Color(0xFFF09193);
}
