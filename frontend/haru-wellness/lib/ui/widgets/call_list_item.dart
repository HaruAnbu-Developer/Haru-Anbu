import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';
import '../theme/app_colors.dart';
import '../theme/app_text_styles.dart';

class CallListItem extends StatelessWidget {
  final String title;
  final DateTime dateTime;
  final int emotionScore;
  final VoidCallback? onTap;

  const CallListItem({
    super.key,
    required this.title,
    required this.dateTime,
    required this.emotionScore,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final isPositive = emotionScore >= 50;
    final emotionColor =
        isPositive ? AppColors.emotionPositive : AppColors.emotionNegative;
    final emotionIcon = isPositive ? LucideIcons.smile : LucideIcons.frown;
    final scoreStyle = isPositive
        ? AppTextStyles.emotionScorePositive
        : AppTextStyles.emotionScoreNegative;

    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 16),
        decoration: BoxDecoration(
          color: AppColors.background,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: AppColors.cardBorder, width: 1),
        ),
        child: Row(
          children: [
            // Title and Date
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: AppTextStyles.callItemTitle,
                  ),
                  const SizedBox(height: 4),
                  Text(
                    _formatDateTime(dateTime),
                    style: AppTextStyles.callItemDate,
                  ),
                ],
              ),
            ),
            // Emotion Score
            Column(
              children: [
                Icon(
                  emotionIcon,
                  size: 22,
                  color: emotionColor,
                ),
                const SizedBox(height: 2),
                Text(
                  emotionScore.toString(),
                  style: scoreStyle,
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  String _formatDateTime(DateTime dt) {
    final hour = dt.hour;
    final period = hour < 12 ? '오전' : '오후';
    final displayHour = hour == 0 ? 12 : (hour > 12 ? hour - 12 : hour);
    final minute = dt.minute.toString().padLeft(2, '0');

    return '${dt.year}. ${dt.month.toString().padLeft(2, '0')}. ${dt.day.toString().padLeft(2, '0')} $period $displayHour:$minute';
  }
}
