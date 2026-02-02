import 'package:flutter/material.dart';
import '../theme/app_colors.dart';
import '../theme/app_text_styles.dart';

class DashboardCard extends StatelessWidget {
  final String? title;
  final double height;
  final Color? backgroundColor;
  final Color? borderColor;
  final Widget? child;
  final VoidCallback? onTap;
  final bool centerTitle;
  final TextStyle? titleStyle;

  const DashboardCard({
    super.key,
    this.title,
    required this.height,
    this.backgroundColor,
    this.borderColor,
    this.child,
    this.onTap,
    this.centerTitle = false,
    this.titleStyle,
  });

  @override
  Widget build(BuildContext context) {
    final bgColor = backgroundColor ?? AppColors.cardBackground;
    final border = borderColor ?? AppColors.cardBorder;
    final textStyle = titleStyle ??
        (centerTitle ? AppTextStyles.cardTitleLarge : AppTextStyles.cardTitle);

    return GestureDetector(
      onTap: onTap,
      child: Container(
        height: height,
        width: double.infinity,
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: bgColor,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: border, width: 1),
        ),
        child: child ??
            (centerTitle
                ? Center(
                    child: Text(
                      title ?? '',
                      style: textStyle,
                    ),
                  )
                : Align(
                    alignment: Alignment.topLeft,
                    child: Text(
                      title ?? '',
                      style: textStyle,
                    ),
                  )),
      ),
    );
  }
}
