import 'package:flutter/material.dart';
import '../../theme/app_colors.dart';
import '../../theme/app_text_styles.dart';
import '../../widgets/today_call_card.dart';
import '../../widgets/emotion_score_card.dart';
import '../../widgets/risk_detection_card.dart';
import '../../widgets/gps_map_card.dart';
import '../../widgets/keyword_analysis_card.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(22, 48, 22, 22),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _buildHeader(),
              const SizedBox(height: 22),
              _buildDashboardCards(),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '하루 안부',
          style: AppTextStyles.greetingSubtitle,
        ),
        const SizedBox(height: 4),
        Text(
          '좋은 아침입니다.',
          style: AppTextStyles.greeting,
        ),
      ],
    );
  }

  Widget _buildDashboardCards() {
    return Column(
      children: [
        // 오늘의 안부 전화 card
        const TodayCallCard(
          summary:
              '오늘 통화에서는 새로운 이웃에 대한 대화를 주로 나누었습니다. 목소리가 밝고 기분이 좋아보이셨습니다. 다만, 최근 식습관에서 위험을 감지했습니다.',
          callTime: '오전 11:00',
          duration: '15분 32초',
        ),
        const SizedBox(height: 16),

        // 감정 스코어 + 위험 감지 row
        const Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: EmotionScoreCard(
                score: 98,
                description: '상태가 최상입니다!',
              ),
            ),
            SizedBox(width: 16),
            Expanded(
              child: RiskDetectionCard(
                description: '위험이 감지 되었습니다.',
                risks: ['식습관', '경미한 우울감'],
              ),
            ),
          ],
        ),
        const SizedBox(height: 16),

        // 실시간 GPS 위치 card
        const GpsMapCard(
          lastUpdated: '4초 전',
        ),
        const SizedBox(height: 16),

        // 긍정/부정 키워드 분석 card
        const KeywordAnalysisCard(
          positiveKeywords: ['새로운 이웃', '밝은 목소리', '좋은 기분'],
          negativeKeywords: ['식습관 위험'],
        ),
      ],
    );
  }
}
