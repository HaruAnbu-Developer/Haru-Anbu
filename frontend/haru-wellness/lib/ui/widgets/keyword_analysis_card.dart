import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';
import '../theme/app_colors.dart';
import '../theme/app_text_styles.dart';
import 'info_tag.dart';

class KeywordAnalysisCard extends StatelessWidget {
  final List<String> positiveKeywords;
  final List<String> negativeKeywords;
  final VoidCallback? onTap;

  const KeywordAnalysisCard({
    super.key,
    required this.positiveKeywords,
    required this.negativeKeywords,
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
              '긍정/부정 키워드 분석',
              style: AppTextStyles.cardSubtitle,
            ),
            const SizedBox(height: 12),
            _buildKeywordSection(
              label: '긍정',
              keywords: positiveKeywords,
              icon: LucideIcons.smile,
            ),
            const SizedBox(height: 10),
            _buildKeywordSection(
              label: '부정',
              keywords: negativeKeywords,
              icon: LucideIcons.frown,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildKeywordSection({
    required String label,
    required List<String> keywords,
    required IconData icon,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: AppTextStyles.cardDescription,
        ),
        const SizedBox(height: 6),
        Wrap(
          spacing: 6,
          runSpacing: 6,
          children: keywords
              .map((keyword) => InfoTag(
                    text: keyword,
                    icon: icon,
                  ))
              .toList(),
        ),
      ],
    );
  }
}
