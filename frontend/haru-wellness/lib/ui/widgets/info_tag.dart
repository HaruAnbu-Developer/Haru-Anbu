import 'package:flutter/material.dart';
import '../theme/app_colors.dart';
import '../theme/app_text_styles.dart';

class InfoTag extends StatelessWidget {
  final String text;
  final IconData? icon;
  final Color? iconColor;

  const InfoTag({
    super.key,
    required this.text,
    this.icon,
    this.iconColor,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(8),
      decoration: BoxDecoration(
        color: AppColors.background,
        borderRadius: BorderRadius.circular(4),
        border: Border.all(color: AppColors.cardBorder, width: 1),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[
            Icon(
              icon,
              size: 12,
              color: iconColor ?? AppColors.primaryDark,
            ),
            const SizedBox(width: 4),
          ],
          Text(
            text,
            style: AppTextStyles.tag,
          ),
        ],
      ),
    );
  }
}
