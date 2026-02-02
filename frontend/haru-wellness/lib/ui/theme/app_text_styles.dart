import 'package:flutter/material.dart';
import 'app_colors.dart';

class AppTextStyles {
  AppTextStyles._();

  // TAEBAEK font styles (for headings)
  static const TextStyle heading1 = TextStyle(
    fontFamily: 'TAEBAEK',
    fontSize: 36,
    fontWeight: FontWeight.w600,
    color: AppColors.textSecondary,
  );

  static const TextStyle heading2 = TextStyle(
    fontFamily: 'TAEBAEK',
    fontSize: 32,
    fontWeight: FontWeight.w600,
    color: AppColors.textSecondary,
  );

  // Freesentation font styles (for body text)
  static const TextStyle subtitle = TextStyle(
    fontFamily: 'Freesentation',
    fontSize: 22,
    fontWeight: FontWeight.w400,
    color: AppColors.textTertiary,
  );

  static const TextStyle bodyLarge = TextStyle(
    fontFamily: 'Freesentation',
    fontSize: 16,
    fontWeight: FontWeight.w500,
    color: AppColors.textPrimary,
    letterSpacing: -0.16,
    height: 1.4,
  );

  static const TextStyle bodyMedium = TextStyle(
    fontFamily: 'Freesentation',
    fontSize: 16,
    fontWeight: FontWeight.w400,
    color: AppColors.textPrimary,
    height: 1.0,
  );

  static const TextStyle button = TextStyle(
    fontFamily: 'TAEBAEK',
    fontSize: 16,
    fontWeight: FontWeight.w500,
    color: AppColors.buttonText,
    letterSpacing: -0.16,
  );

  static const TextStyle buttonOutlined = TextStyle(
    fontFamily: 'Freesentation',
    fontSize: 16,
    fontWeight: FontWeight.w500,
    color: AppColors.textPrimary,
    letterSpacing: -0.16,
  );

  static const TextStyle label = TextStyle(
    fontFamily: 'Freesentation',
    fontSize: 16,
    fontWeight: FontWeight.w400,
    color: AppColors.textPrimary,
    height: 1.0,
  );

  static const TextStyle link = TextStyle(
    fontFamily: 'Freesentation',
    fontSize: 16,
    fontWeight: FontWeight.w400,
    color: AppColors.textPrimary,
    decoration: TextDecoration.underline,
    height: 1.0,
  );

  // Dashboard styles
  static const TextStyle greeting = TextStyle(
    fontFamily: 'TAEBAEK',
    fontSize: 24,
    fontWeight: FontWeight.w600,
    color: AppColors.primary,
  );

  static const TextStyle greetingSubtitle = TextStyle(
    fontFamily: 'Freesentation',
    fontSize: 20,
    fontWeight: FontWeight.w400,
    color: AppColors.primaryLight,
  );

  static const TextStyle cardTitle = TextStyle(
    fontFamily: 'Freesentation',
    fontSize: 18,
    fontWeight: FontWeight.w400,
    color: AppColors.primaryDark,
  );

  static const TextStyle cardTitleLarge = TextStyle(
    fontFamily: 'Freesentation',
    fontSize: 22,
    fontWeight: FontWeight.w400,
    color: AppColors.primaryDark,
  );

  // Navigation styles
  static const TextStyle navLabelActive = TextStyle(
    fontFamily: 'Freesentation',
    fontSize: 12,
    fontWeight: FontWeight.w600,
    color: AppColors.navActive,
    height: 1.28,
  );

  static const TextStyle navLabelInactive = TextStyle(
    fontFamily: 'Freesentation',
    fontSize: 12,
    fontWeight: FontWeight.w500,
    color: AppColors.navInactive,
    height: 1.28,
  );

  // Screen title style
  static const TextStyle screenTitle = TextStyle(
    fontFamily: 'TAEBAEK',
    fontSize: 24,
    fontWeight: FontWeight.w600,
    color: AppColors.primary,
  );

  // Call list item styles
  static const TextStyle callItemTitle = TextStyle(
    fontFamily: 'Freesentation',
    fontSize: 18,
    fontWeight: FontWeight.w400,
    color: AppColors.primaryDark,
  );

  static const TextStyle callItemDate = TextStyle(
    fontFamily: 'Freesentation',
    fontSize: 12,
    fontWeight: FontWeight.w400,
    color: AppColors.primary,
  );

  static const TextStyle emotionScorePositive = TextStyle(
    fontFamily: 'Freesentation',
    fontSize: 12,
    fontWeight: FontWeight.w400,
    color: AppColors.emotionPositive,
  );

  static const TextStyle emotionScoreNegative = TextStyle(
    fontFamily: 'Freesentation',
    fontSize: 12,
    fontWeight: FontWeight.w400,
    color: AppColors.emotionNegative,
  );

  // My page styles
  static const TextStyle profileName = TextStyle(
    fontFamily: 'Freesentation',
    fontSize: 18,
    fontWeight: FontWeight.w400,
    color: AppColors.primaryDark,
  );

  static const TextStyle profileSubtitle = TextStyle(
    fontFamily: 'Freesentation',
    fontSize: 14,
    fontWeight: FontWeight.w400,
    color: AppColors.primary,
  );

  static const TextStyle sectionHeader = TextStyle(
    fontFamily: 'Freesentation',
    fontSize: 16,
    fontWeight: FontWeight.w400,
    color: AppColors.primaryDark,
  );

  static const TextStyle menuItemTitle = TextStyle(
    fontFamily: 'Freesentation',
    fontSize: 18,
    fontWeight: FontWeight.w400,
    color: AppColors.primaryDark,
  );

  // Detail header styles
  static const TextStyle detailTitle = TextStyle(
    fontFamily: 'TAEBAEK',
    fontSize: 20,
    fontWeight: FontWeight.w600,
    color: AppColors.primary,
  );

  static const TextStyle detailSubtitle = TextStyle(
    fontFamily: 'Freesentation',
    fontSize: 15,
    fontWeight: FontWeight.w400,
    color: AppColors.primaryLight,
  );

  // Home card styles
  static const TextStyle cardTitleUnderline = TextStyle(
    fontFamily: 'Freesentation',
    fontSize: 14,
    fontWeight: FontWeight.w500,
    color: AppColors.primaryDark,
    decoration: TextDecoration.underline,
  );

  static const TextStyle cardBody = TextStyle(
    fontFamily: 'TAEBAEK',
    fontSize: 14,
    fontWeight: FontWeight.w400,
    color: AppColors.primaryDark,
    height: 1.5,
  );

  static const TextStyle cardSubtitle = TextStyle(
    fontFamily: 'Freesentation',
    fontSize: 14,
    fontWeight: FontWeight.w500,
    color: AppColors.primaryDark,
  );

  static const TextStyle cardDescription = TextStyle(
    fontFamily: 'Freesentation',
    fontSize: 12,
    fontWeight: FontWeight.w400,
    color: AppColors.primaryDark,
  );

  static const TextStyle tag = TextStyle(
    fontFamily: 'Freesentation',
    fontSize: 12,
    fontWeight: FontWeight.w400,
    color: AppColors.primaryDark,
  );

  static const TextStyle scoreNumber = TextStyle(
    fontFamily: 'TAEBAEK',
    fontSize: 28,
    fontWeight: FontWeight.w400,
    color: AppColors.primaryDark,
  );

  static const TextStyle mapTitle = TextStyle(
    fontFamily: 'Freesentation',
    fontSize: 14,
    fontWeight: FontWeight.w700,
    color: Colors.white,
  );

  static const TextStyle mapSubtitle = TextStyle(
    fontFamily: 'Freesentation',
    fontSize: 12,
    fontWeight: FontWeight.w400,
    color: Colors.white,
  );
}
