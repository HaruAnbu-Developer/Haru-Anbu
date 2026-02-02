import 'package:flutter/material.dart';
import '../theme/app_colors.dart';
import '../theme/app_text_styles.dart';

class AiSummaryCard extends StatelessWidget {
  final String summary;
  final VoidCallback? onTap;

  const AiSummaryCard({
    super.key,
    required this.summary,
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
              'AI 요약',
              style: AppTextStyles.cardSubtitle,
            ),
            const SizedBox(height: 10),
            Text(
              summary,
              style: AppTextStyles.cardBody,
            ),
          ],
        ),
      ),
    );
  }
}
