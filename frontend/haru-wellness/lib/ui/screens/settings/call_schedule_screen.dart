import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../theme/app_colors.dart';
import '../../widgets/detail_header.dart';
import '../../widgets/section_header.dart';
import '../../widgets/primary_button.dart';
import '../../widgets/settings/day_selector.dart';
import '../../widgets/settings/time_picker_tile.dart';

class CallScheduleScreen extends StatefulWidget {
  const CallScheduleScreen({super.key});

  @override
  State<CallScheduleScreen> createState() => _CallScheduleScreenState();
}

class _CallScheduleScreenState extends State<CallScheduleScreen> {
  Set<String> _selectedDays = {'월', '수', '금'};
  TimeOfDay _selectedTime = const TimeOfDay(hour: 10, minute: 0);

  Future<void> _showTimePicker() async {
    final TimeOfDay? picked = await showTimePicker(
      context: context,
      initialTime: _selectedTime,
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            colorScheme: const ColorScheme.light(
              primary: AppColors.primary,
              onPrimary: Colors.white,
              secondary: AppColors.primary,
              onSecondary: Colors.white,
              surface: AppColors.background,
              onSurface: AppColors.primaryDark,
              tertiary: AppColors.primary,
              onTertiary: Colors.white,
              tertiaryContainer: AppColors.primaryLighter,
              onTertiaryContainer: AppColors.primaryDark,
            ),
            timePickerTheme: TimePickerThemeData(
              backgroundColor: AppColors.background,
              hourMinuteColor: AppColors.cardBackground,
              hourMinuteTextColor: AppColors.primaryDark,
              dayPeriodColor: WidgetStateColor.resolveWith((states) {
                if (states.contains(WidgetState.selected)) {
                  return AppColors.primary;
                }
                return AppColors.cardBackground;
              }),
              dayPeriodTextColor: WidgetStateColor.resolveWith((states) {
                if (states.contains(WidgetState.selected)) {
                  return Colors.white;
                }
                return AppColors.primaryDark;
              }),
              dayPeriodBorderSide: const BorderSide(
                color: AppColors.cardBorder,
                width: 1,
              ),
              dayPeriodShape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(8),
              ),
              dialHandColor: AppColors.primary,
              dialBackgroundColor: AppColors.cardBackground,
              dialTextColor: WidgetStateColor.resolveWith((states) {
                if (states.contains(WidgetState.selected)) {
                  return Colors.white;
                }
                return AppColors.primaryDark;
              }),
              entryModeIconColor: AppColors.primary,
              helpTextStyle: TextStyle(
                color: AppColors.primary,
                fontFamily: 'Freesentation',
                fontSize: 12,
              ),
              hourMinuteTextStyle: TextStyle(
                fontFamily: 'TAEBAEK',
                fontSize: 48,
                fontWeight: FontWeight.w400,
              ),
              dayPeriodTextStyle: TextStyle(
                fontFamily: 'Freesentation',
                fontSize: 14,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          child: child!,
        );
      },
    );
    if (picked != null) {
      setState(() {
        _selectedTime = picked;
      });
    }
  }

  void _handleSave() {
    // TODO: Save schedule settings
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('스케줄이 저장되었습니다'),
        backgroundColor: AppColors.primary,
      ),
    );
    context.pop();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(22, 48, 22, 22),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Header
                    DetailHeader(
                      title: '안부 스케줄 설정',
                      onBackPressed: () => context.pop(),
                    ),
                    const SizedBox(height: 32),

                    // Day selection
                    const SectionHeader(title: '요일 선택'),
                    const SizedBox(height: 12),
                    DaySelector(
                      selectedDays: _selectedDays,
                      onChanged: (days) {
                        setState(() {
                          _selectedDays = days;
                        });
                      },
                    ),
                    const SizedBox(height: 24),

                    // Time selection
                    const SectionHeader(title: '통화 시간'),
                    const SizedBox(height: 12),
                    TimePickerTile(
                      time: _selectedTime,
                      onTap: _showTimePicker,
                    ),
                  ],
                ),
              ),
            ),
            // Save button fixed at bottom
            Padding(
              padding: const EdgeInsets.fromLTRB(22, 0, 22, 22),
              child: PrimaryButton(
                text: '저장하기',
                onPressed: _handleSave,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
