import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';
import '../theme/app_colors.dart';
import '../theme/app_text_styles.dart';
import 'info_tag.dart';

class TodayCallCard extends StatelessWidget {
  final String summary;
  final String callTime;
  final String duration;
  final VoidCallback? onTap;

  const TodayCallCard({
    super.key,
    required this.summary,
    required this.callTime,
    required this.duration,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: const Color(0xFFF4FEFF),
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: AppColors.cardBorder, width: 1),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '오늘의 안부 전화',
              style: AppTextStyles.cardTitleUnderline,
            ),
            const SizedBox(height: 10),
            Text(
              summary,
              style: AppTextStyles.cardBody,
            ),
            const SizedBox(height: 10),
            Row(
              children: [
                InfoTag(
                  text: callTime,
                  icon: LucideIcons.clock,
                ),
                const SizedBox(width: 10),
                InfoTag(
                  text: duration,
                  icon: LucideIcons.alarmClock,
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
