import 'package:flutter/material.dart';
import '../theme/app_colors.dart';
import '../theme/app_text_styles.dart';

class EmotionScoreCard extends StatelessWidget {
  final int score;
  final String description;
  final VoidCallback? onTap;

  const EmotionScoreCard({
    super.key,
    required this.score,
    required this.description,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        height: 160,
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: const Color(0xFFF4FEFF),
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: AppColors.cardBorder, width: 1),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '감정 스코어',
                  style: AppTextStyles.cardSubtitle,
                ),
                const SizedBox(height: 2),
                Text(
                  description,
                  style: AppTextStyles.cardDescription,
                ),
              ],
            ),
            Align(
              alignment: Alignment.bottomRight,
              child: Text(
                score.toString(),
                style: AppTextStyles.scoreNumber,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
