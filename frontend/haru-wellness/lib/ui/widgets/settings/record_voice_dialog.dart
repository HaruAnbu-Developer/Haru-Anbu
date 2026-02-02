import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';
import '../../../data/models/voice_entry.dart';
import '../../theme/app_colors.dart';
import '../../theme/app_text_styles.dart';

enum RecordingState { idle, recording, recorded }

class RecordVoiceDialog extends StatefulWidget {
  final ValueChanged<VoiceEntry?> onComplete;

  const RecordVoiceDialog({
    super.key,
    required this.onComplete,
  });

  @override
  State<RecordVoiceDialog> createState() => _RecordVoiceDialogState();
}

class _RecordVoiceDialogState extends State<RecordVoiceDialog> {
  RecordingState _state = RecordingState.idle;
  Duration _recordedDuration = Duration.zero;
  final TextEditingController _aliasController = TextEditingController();

  @override
  void dispose() {
    _aliasController.dispose();
    super.dispose();
  }

  void _startRecording() {
    setState(() {
      _state = RecordingState.recording;
      _recordedDuration = Duration.zero;
    });
    // Simulate recording for demo (in real app, use audio recording package)
    Future.delayed(const Duration(seconds: 3), () {
      if (mounted && _state == RecordingState.recording) {
        setState(() {
          _state = RecordingState.recorded;
          _recordedDuration = const Duration(seconds: 3);
        });
      }
    });
  }

  void _stopRecording() {
    setState(() {
      _state = RecordingState.recorded;
    });
  }

  void _resetRecording() {
    setState(() {
      _state = RecordingState.idle;
      _recordedDuration = Duration.zero;
    });
  }

  void _saveVoice() {
    if (_aliasController.text.isNotEmpty && _state == RecordingState.recorded) {
      final voice = VoiceEntry(
        id: DateTime.now().millisecondsSinceEpoch.toString(),
        alias: _aliasController.text,
        createdAt: DateTime.now(),
      );
      widget.onComplete(voice);
    }
  }

  String _formatDuration(Duration duration) {
    final minutes = duration.inMinutes.toString().padLeft(2, '0');
    final seconds = (duration.inSeconds % 60).toString().padLeft(2, '0');
    return '$minutes:$seconds';
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      title: Text(
        '음성 녹음',
        style: AppTextStyles.detailTitle,
        textAlign: TextAlign.center,
      ),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // Recording indicator
          Container(
            width: 80,
            height: 80,
            decoration: BoxDecoration(
              color: _state == RecordingState.recording
                  ? AppColors.emotionNegative.withValues(alpha: 0.1)
                  : AppColors.primaryLighter,
              borderRadius: BorderRadius.circular(40),
            ),
            child: Icon(
              _state == RecordingState.recorded
                  ? LucideIcons.check
                  : LucideIcons.mic,
              size: 36,
              color: _state == RecordingState.recording
                  ? AppColors.emotionNegative
                  : AppColors.primary,
            ),
          ),
          const SizedBox(height: 16),
          // Status text
          Text(
            _state == RecordingState.idle
                ? '녹음 버튼을 눌러 시작하세요'
                : _state == RecordingState.recording
                    ? '녹음 중...'
                    : '녹음 완료',
            style: AppTextStyles.cardSubtitle,
          ),
          if (_state != RecordingState.idle) ...[
            const SizedBox(height: 8),
            Text(
              _formatDuration(_recordedDuration),
              style: AppTextStyles.scoreNumber.copyWith(
                color: AppColors.primary,
              ),
            ),
          ],
          const SizedBox(height: 24),
          // Recording button
          if (_state != RecordingState.recorded)
            GestureDetector(
              onTap: _state == RecordingState.idle
                  ? _startRecording
                  : _stopRecording,
              child: Container(
                width: 64,
                height: 64,
                decoration: BoxDecoration(
                  color: _state == RecordingState.recording
                      ? AppColors.emotionNegative
                      : AppColors.primary,
                  borderRadius: BorderRadius.circular(32),
                ),
                child: Icon(
                  _state == RecordingState.recording
                      ? LucideIcons.square
                      : LucideIcons.mic,
                  size: 28,
                  color: Colors.white,
                ),
              ),
            ),
          if (_state == RecordingState.recorded) ...[
            // Alias input
            TextField(
              controller: _aliasController,
              decoration: InputDecoration(
                hintText: '목소리 별칭 (예: 아들 목소리)',
                hintStyle: AppTextStyles.label.copyWith(
                  color: AppColors.textTertiary,
                ),
                filled: true,
                fillColor: AppColors.cardBackground,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(8),
                  borderSide: BorderSide(color: AppColors.cardBorder),
                ),
                enabledBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(8),
                  borderSide: BorderSide(color: AppColors.cardBorder),
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(8),
                  borderSide: BorderSide(color: AppColors.primary, width: 2),
                ),
                contentPadding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 14,
                ),
              ),
            ),
            const SizedBox(height: 12),
            // Re-record button
            TextButton(
              onPressed: _resetRecording,
              child: Text(
                '다시 녹음하기',
                style: AppTextStyles.label.copyWith(
                  color: AppColors.primary,
                ),
              ),
            ),
          ],
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => widget.onComplete(null),
          child: Text(
            '취소',
            style: AppTextStyles.label.copyWith(
              color: AppColors.textTertiary,
            ),
          ),
        ),
        if (_state == RecordingState.recorded)
          TextButton(
            onPressed: _aliasController.text.isNotEmpty ? _saveVoice : null,
            child: Text(
              '저장',
              style: AppTextStyles.label.copyWith(
                color: _aliasController.text.isNotEmpty
                    ? AppColors.primary
                    : AppColors.textTertiary,
              ),
            ),
          ),
      ],
    );
  }
}
