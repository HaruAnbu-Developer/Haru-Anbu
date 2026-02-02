import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../data/models/call_detail_model.dart';
import '../../theme/app_colors.dart';
import '../../theme/app_text_styles.dart';
import '../../widgets/call_list_item.dart';

class CallHistoryScreen extends StatelessWidget {
  const CallHistoryScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(22, 48, 22, 0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '안부 전화',
                style: AppTextStyles.screenTitle,
              ),
              const SizedBox(height: 22),
              Expanded(
                child: _buildCallList(context),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildCallList(BuildContext context) {
    final calls = CallDetailDummyData.calls;

    return SingleChildScrollView(
      child: Column(
        children: calls.map((call) {
          // Parse date from string (e.g., "2026. 01. 12 오전 11:00")
          final dateTime = _parseDate(call.date);

          return Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: CallListItem(
              title: call.title,
              dateTime: dateTime,
              emotionScore: call.emotionScore,
              onTap: () {
                context.push('/calls/detail/${call.id}');
              },
            ),
          );
        }).toList(),
      ),
    );
  }

  DateTime _parseDate(String dateStr) {
    // Simple parsing for "2026. 01. 12 오전 11:00" format
    try {
      final parts = dateStr.split(' ');
      final year = int.parse(parts[0].replaceAll('.', '').trim());
      final month = int.parse(parts[1].replaceAll('.', '').trim());
      final day = int.parse(parts[2].trim());

      final isPm = parts[3] == '오후';
      final timeParts = parts[4].split(':');
      var hour = int.parse(timeParts[0]);
      final minute = int.parse(timeParts[1]);

      if (isPm && hour != 12) hour += 12;
      if (!isPm && hour == 12) hour = 0;

      return DateTime(year, month, day, hour, minute);
    } catch (e) {
      return DateTime.now();
    }
  }
}
