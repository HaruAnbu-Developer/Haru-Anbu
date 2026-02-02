import 'package:flutter/material.dart';
import '../theme/app_colors.dart';
import '../theme/app_text_styles.dart';

class InfoCard extends StatelessWidget {
  final String? title;
  final double height;
  final Color? backgroundColor;
  final Widget? child;
  final VoidCallback? onTap;

  const InfoCard({
    super.key,
    this.title,
    required this.height,
    this.backgroundColor,
    this.child,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final bgColor = backgroundColor ?? AppColors.cardBackgroundAlt;

    return GestureDetector(
      onTap: onTap,
      child: Container(
        height: height,
        width: double.infinity,
        padding: const EdgeInsets.all(22),
        decoration: BoxDecoration(
          color: bgColor,
          borderRadius: BorderRadius.circular(8),
        ),
        child: child ??
            Center(
              child: Text(
                title ?? '',
                style: AppTextStyles.cardTitleLarge,
              ),
            ),
      ),
    );
  }
}
