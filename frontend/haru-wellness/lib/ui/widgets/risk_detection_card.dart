import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';
import '../theme/app_colors.dart';
import '../theme/app_text_styles.dart';
import 'info_tag.dart';

class RiskDetectionCard extends StatelessWidget {
  final String description;
  final List<String> risks;
  final VoidCallback? onTap;

  const RiskDetectionCard({
    super.key,
    required this.description,
    required this.risks,
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
                  '위험감지',
                  style: AppTextStyles.cardSubtitle,
                ),
                const SizedBox(height: 2),
                Text(
                  description,
                  style: AppTextStyles.cardDescription,
                ),
              ],
            ),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: risks.asMap().entries.map((entry) {
                final isLast = entry.key == risks.length - 1;
                return Padding(
                  padding: EdgeInsets.only(bottom: isLast ? 0 : 2),
                  child: InfoTag(
                    text: entry.value,
                    icon: LucideIcons.triangleAlert,
                  ),
                );
              }).toList(),
            ),
          ],
        ),
      ),
    );
  }
}
