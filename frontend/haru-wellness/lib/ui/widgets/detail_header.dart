import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';
import '../theme/app_colors.dart';
import '../theme/app_text_styles.dart';

class DetailHeader extends StatelessWidget {
  final String title;
  final String? subtitle;
  final VoidCallback? onBackPressed;

  const DetailHeader({
    super.key,
    required this.title,
    this.subtitle,
    this.onBackPressed,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Back Button
        GestureDetector(
          onTap: onBackPressed,
          child: const Icon(
            LucideIcons.chevronLeft,
            size: 24,
            color: AppColors.primary,
          ),
        ),
        const SizedBox(height: 16),
        // Title
        Text(
          title,
          style: AppTextStyles.detailTitle,
        ),
        if (subtitle != null) ...[
          const SizedBox(height: 4),
          // Subtitle (date)
          Text(
            subtitle!,
            style: AppTextStyles.detailSubtitle,
          ),
        ],
      ],
    );
  }
}
