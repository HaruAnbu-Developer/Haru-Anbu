import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';
import '../../theme/app_colors.dart';
import '../../theme/app_text_styles.dart';

class TimePickerTile extends StatelessWidget {
  final TimeOfDay time;
  final VoidCallback onTap;

  const TimePickerTile({
    super.key,
    required this.time,
    required this.onTap,
  });

  String _getPeriod(TimeOfDay time) {
    return time.period == DayPeriod.am ? '오전' : '오후';
  }

  String _getTimeString(TimeOfDay time) {
    final hour = time.hourOfPeriod == 0 ? 12 : time.hourOfPeriod;
    final minute = time.minute.toString().padLeft(2, '0');
    return '$hour:$minute';
  }

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
          children: [
            const Icon(
              LucideIcons.clock,
              size: 22,
              color: AppColors.primary,
            ),
            const SizedBox(width: 12),
            Text(
              _getPeriod(time),
              style: AppTextStyles.profileSubtitle,
            ),
            const SizedBox(width: 8),
            Text(
              _getTimeString(time),
              style: AppTextStyles.menuItemTitle,
            ),
            const Spacer(),
            const Icon(
              LucideIcons.chevronRight,
              size: 22,
              color: AppColors.primary,
            ),
          ],
        ),
      ),
    );
  }
}
