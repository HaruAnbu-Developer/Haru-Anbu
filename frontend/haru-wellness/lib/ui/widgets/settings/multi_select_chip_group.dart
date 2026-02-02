import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';
import '../../theme/app_colors.dart';
import '../../theme/app_text_styles.dart';

class MultiSelectChipGroup extends StatelessWidget {
  final List<String> options;
  final Set<String> selected;
  final ValueChanged<Set<String>> onChanged;

  const MultiSelectChipGroup({
    super.key,
    required this.options,
    required this.selected,
    required this.onChanged,
  });

  void _toggleOption(String option) {
    final newSelection = Set<String>.from(selected);
    if (newSelection.contains(option)) {
      newSelection.remove(option);
    } else {
      newSelection.add(option);
    }
    onChanged(newSelection);
  }

  IconData _getIconForOption(String option) {
    switch (option) {
      case 'Push':
        return LucideIcons.bell;
      case 'KakaoTalk':
        return LucideIcons.messageCircle;
      case 'SMS':
        return LucideIcons.messageSquare;
      default:
        return LucideIcons.circle;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 12,
      runSpacing: 12,
      children: options.map((option) {
        final isSelected = selected.contains(option);
        return GestureDetector(
          onTap: () => _toggleOption(option),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            decoration: BoxDecoration(
              color: isSelected ? AppColors.primary : AppColors.cardBackground,
              borderRadius: BorderRadius.circular(8),
              border: Border.all(
                color: isSelected ? AppColors.primary : AppColors.cardBorder,
                width: 1,
              ),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(
                  _getIconForOption(option),
                  size: 18,
                  color: isSelected ? Colors.white : AppColors.primary,
                ),
                const SizedBox(width: 8),
                Text(
                  option,
                  style: AppTextStyles.cardSubtitle.copyWith(
                    color: isSelected ? Colors.white : AppColors.primaryDark,
                  ),
                ),
              ],
            ),
          ),
        );
      }).toList(),
    );
  }
}
