import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../theme/app_colors.dart';
import '../../widgets/detail_header.dart';
import '../../widgets/section_header.dart';
import '../../widgets/primary_button.dart';
import '../../widgets/settings/toggle_switch_tile.dart';
import '../../widgets/settings/multi_select_chip_group.dart';

class NotificationSettingsScreen extends StatefulWidget {
  const NotificationSettingsScreen({super.key});

  @override
  State<NotificationSettingsScreen> createState() =>
      _NotificationSettingsScreenState();
}

class _NotificationSettingsScreenState
    extends State<NotificationSettingsScreen> {
  bool _emergencyAlerts = true;
  bool _dailyReport = true;
  bool _weeklySummary = false;
  Set<String> _notificationMethods = {'Push'};

  static const List<String> _methodOptions = ['Push', 'KakaoTalk', 'SMS'];

  void _handleSave() {
    // TODO: Save notification settings
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('알림 설정이 저장되었습니다'),
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
                      title: '알림 설정',
                      onBackPressed: () => context.pop(),
                    ),
                    const SizedBox(height: 32),

                    // Notification types
                    const SectionHeader(title: '알림 종류'),
                    const SizedBox(height: 12),
                    ToggleSwitchTile(
                      title: '긴급 알림 (SOS)',
                      subtitle: '위험 상황 감지 시 즉시 알림',
                      value: _emergencyAlerts,
                      onChanged: (value) {
                        setState(() {
                          _emergencyAlerts = value;
                        });
                      },
                    ),
                    const SizedBox(height: 12),
                    ToggleSwitchTile(
                      title: '일일 리포트 알림',
                      subtitle: '매일 안부 전화 요약 리포트',
                      value: _dailyReport,
                      onChanged: (value) {
                        setState(() {
                          _dailyReport = value;
                        });
                      },
                    ),
                    const SizedBox(height: 12),
                    ToggleSwitchTile(
                      title: '주간 요약 리포트',
                      subtitle: '매주 월요일 주간 분석 리포트',
                      value: _weeklySummary,
                      onChanged: (value) {
                        setState(() {
                          _weeklySummary = value;
                        });
                      },
                    ),
                    const SizedBox(height: 24),

                    // Notification methods
                    const SectionHeader(title: '알림 수신 방법'),
                    const SizedBox(height: 12),
                    MultiSelectChipGroup(
                      options: _methodOptions,
                      selected: _notificationMethods,
                      onChanged: (methods) {
                        setState(() {
                          _notificationMethods = methods;
                        });
                      },
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
