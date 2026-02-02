import 'package:flutter/material.dart';
import '../theme/app_colors.dart';
import '../theme/app_text_styles.dart';

class MenuItemWidget extends StatelessWidget {
  final String title;
  final Widget? icon;
  final VoidCallback? onTap;

  const MenuItemWidget({
    super.key,
    required this.title,
    this.icon,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        height: 59,
        padding: const EdgeInsets.symmetric(horizontal: 22),
        decoration: BoxDecoration(
          color: AppColors.background,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: AppColors.cardBorder, width: 1),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              title,
              style: AppTextStyles.menuItemTitle,
            ),
            if (icon != null) icon!,
          ],
        ),
      ),
    );
  }
}
