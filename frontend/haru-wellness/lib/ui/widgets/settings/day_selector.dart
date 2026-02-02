import 'package:flutter/material.dart';
import '../../theme/app_colors.dart';
import '../../theme/app_text_styles.dart';

class DaySelector extends StatelessWidget {
  final Set<String> selectedDays;
  final ValueChanged<Set<String>> onChanged;

  static const List<String> days = ['월', '화', '수', '목', '금', '토', '일'];

  const DaySelector({
    super.key,
    required this.selectedDays,
    required this.onChanged,
  });

  void _toggleDay(String day) {
    final newSelection = Set<String>.from(selectedDays);
    if (newSelection.contains(day)) {
      newSelection.remove(day);
    } else {
      newSelection.add(day);
    }
    onChanged(newSelection);
  }

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: days.map((day) {
        final isSelected = selectedDays.contains(day);
        return GestureDetector(
          onTap: () => _toggleDay(day),
          child: Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(
              color: isSelected ? AppColors.primary : AppColors.cardBackground,
              borderRadius: BorderRadius.circular(8),
              border: Border.all(
                color: isSelected ? AppColors.primary : AppColors.cardBorder,
                width: 1,
              ),
            ),
            child: Center(
              child: Text(
                day,
                style: AppTextStyles.cardSubtitle.copyWith(
                  color: isSelected ? Colors.white : AppColors.primaryDark,
                ),
              ),
            ),
          ),
        );
      }).toList(),
    );
  }
}
