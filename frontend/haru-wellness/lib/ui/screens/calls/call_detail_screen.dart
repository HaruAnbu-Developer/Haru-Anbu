import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../data/models/call_detail_model.dart';
import '../../theme/app_colors.dart';
import '../../widgets/detail_header.dart';
import '../../widgets/ai_summary_card.dart';
import '../../widgets/emotion_score_card.dart';
import '../../widgets/risk_detection_card.dart';
import '../../widgets/keyword_analysis_card.dart';

class CallDetailScreen extends StatelessWidget {
  final String? callId;

  const CallDetailScreen({
    super.key,
    this.callId,
  });

  @override
  Widget build(BuildContext context) {
    final callData = CallDetailDummyData.getById(callId)!;

    return Scaffold(
      backgroundColor: AppColors.background,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(22, 48, 22, 22),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Header with back button
              DetailHeader(
                title: callData.title,
                subtitle: callData.date,
                onBackPressed: () {
                  context.pop();
                },
              ),
              const SizedBox(height: 22),

              // Info Cards
              _buildInfoCards(callData),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildInfoCards(CallDetailModel data) {
    return Column(
      children: [
        // AI 요약 card
        AiSummaryCard(
          summary: data.aiSummary,
        ),
        const SizedBox(height: 16),

        // 감정 스코어 + 위험 감지 row
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: EmotionScoreCard(
                score: data.emotionScore,
                description: data.emotionDescription,
              ),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: RiskDetectionCard(
                description: data.riskDescription,
                risks: data.risks,
              ),
            ),
          ],
        ),
        const SizedBox(height: 16),

        // 긍정/부정 키워드 분석 card
        KeywordAnalysisCard(
          positiveKeywords: data.positiveKeywords,
          negativeKeywords: data.negativeKeywords,
        ),
      ],
    );
  }
}
