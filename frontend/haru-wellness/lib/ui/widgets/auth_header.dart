import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';
import '../theme/app_colors.dart';
import '../theme/app_text_styles.dart';

class AuthHeader extends StatelessWidget {
  final String title;
  final String subtitle;
  final VoidCallback? onBackPressed;

  const AuthHeader({
    super.key,
    required this.title,
    required this.subtitle,
    this.onBackPressed,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const SizedBox(height: 36),
        // Back Button
        GestureDetector(
          onTap: onBackPressed,
          child: const Icon(
            LucideIcons.chevronLeft,
            size: 24,
            color: AppColors.primary,
          ),
        ),
        const SizedBox(height: 36),
        // Title
        Text(
          title,
          style: AppTextStyles.heading2,
        ),
        const SizedBox(height: 2),
        // Subtitle
        Text(
          subtitle,
          style: AppTextStyles.subtitle,
        ),
      ],
    );
  }
}
