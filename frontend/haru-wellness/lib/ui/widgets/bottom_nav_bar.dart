import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';
import '../theme/app_colors.dart';
import '../theme/app_text_styles.dart';

class BottomNavBar extends StatelessWidget {
  final int currentIndex;
  final ValueChanged<int> onTap;

  const BottomNavBar({
    super.key,
    required this.currentIndex,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: AppColors.background,
        border: Border(
          top: BorderSide(
            color: AppColors.navBorder,
            width: 0.6,
          ),
        ),
      ),
      padding: const EdgeInsets.only(top: 8, bottom: 16),
      child: Row(
        children: [
          _buildNavItem(
            icon: LucideIcons.house,
            label: '홈',
            index: 0,
          ),
          _buildNavItem(
            icon: LucideIcons.phone,
            label: '안부',
            index: 1,
          ),
          _buildNavItem(
            icon: LucideIcons.user,
            label: '마이',
            index: 2,
          ),
        ],
      ),
    );
  }

  Widget _buildNavItem({
    required IconData icon,
    required String label,
    required int index,
  }) {
    final isActive = currentIndex == index;
    final color = isActive ? AppColors.navActive : AppColors.navInactive;
    final textStyle =
        isActive ? AppTextStyles.navLabelActive : AppTextStyles.navLabelInactive;

    return Expanded(
      child: GestureDetector(
        onTap: () => onTap(index),
        behavior: HitTestBehavior.opaque,
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 12),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                icon,
                size: 22,
                color: color,
              ),
              const SizedBox(height: 4),
              Text(
                label,
                style: textStyle,
                textAlign: TextAlign.center,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
