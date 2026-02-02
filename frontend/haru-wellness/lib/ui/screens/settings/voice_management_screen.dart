import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';
import '../../../data/models/voice_entry.dart';
import '../../theme/app_colors.dart';
import '../../theme/app_text_styles.dart';
import '../../widgets/detail_header.dart';
import '../../widgets/section_header.dart';
import '../../widgets/outlined_button_widget.dart';
import '../../widgets/settings/voice_list_item.dart';
import '../../widgets/settings/record_voice_dialog.dart';

class VoiceManagementScreen extends StatefulWidget {
  const VoiceManagementScreen({super.key});

  @override
  State<VoiceManagementScreen> createState() => _VoiceManagementScreenState();
}

class _VoiceManagementScreenState extends State<VoiceManagementScreen> {
  final List<VoiceEntry> _voices = [
    VoiceEntry(
      id: '1',
      alias: '아들 목소리',
      createdAt: DateTime(2024, 1, 15),
    ),
    VoiceEntry(
      id: '2',
      alias: '딸 목소리',
      createdAt: DateTime(2024, 2, 20),
    ),
  ];

  void _showRecordDialog() {
    showDialog(
      context: context,
      builder: (context) => RecordVoiceDialog(
        onComplete: (voice) {
          Navigator.of(context).pop();
          if (voice != null) {
            setState(() {
              _voices.add(voice);
            });
            ScaffoldMessenger.of(this.context).showSnackBar(
              const SnackBar(
                content: Text('음성이 등록되었습니다'),
                backgroundColor: AppColors.primary,
              ),
            );
          }
        },
      ),
    );
  }

  void _handleFileUpload() {
    // TODO: Implement file picker
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('파일 업로드 기능은 준비 중입니다'),
        backgroundColor: AppColors.primary,
      ),
    );
  }

  void _handlePlayVoice(VoiceEntry voice) {
    // TODO: Implement audio playback
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('${voice.alias} 재생'),
        backgroundColor: AppColors.primary,
      ),
    );
  }

  void _handleEditVoice(VoiceEntry voice) {
    final controller = TextEditingController(text: voice.alias);
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: Text(
          '별칭 수정',
          style: AppTextStyles.detailTitle,
        ),
        content: TextField(
          controller: controller,
          decoration: InputDecoration(
            hintText: '목소리 별칭',
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
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: Text(
              '취소',
              style: AppTextStyles.label.copyWith(
                color: AppColors.textTertiary,
              ),
            ),
          ),
          TextButton(
            onPressed: () {
              if (controller.text.isNotEmpty) {
                setState(() {
                  final index = _voices.indexWhere((v) => v.id == voice.id);
                  if (index != -1) {
                    _voices[index] = voice.copyWith(alias: controller.text);
                  }
                });
                Navigator.of(context).pop();
              }
            },
            child: Text(
              '저장',
              style: AppTextStyles.label.copyWith(
                color: AppColors.primary,
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _handleDeleteVoice(VoiceEntry voice) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: Text(
          '음성 삭제',
          style: AppTextStyles.detailTitle,
        ),
        content: Text(
          '${voice.alias}를 삭제하시겠습니까?',
          style: AppTextStyles.bodyMedium,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: Text(
              '취소',
              style: AppTextStyles.label.copyWith(
                color: AppColors.textTertiary,
              ),
            ),
          ),
          TextButton(
            onPressed: () {
              setState(() {
                _voices.removeWhere((v) => v.id == voice.id);
              });
              Navigator.of(context).pop();
              ScaffoldMessenger.of(this.context).showSnackBar(
                const SnackBar(
                  content: Text('음성이 삭제되었습니다'),
                  backgroundColor: AppColors.primary,
                ),
              );
            },
            child: Text(
              '삭제',
              style: AppTextStyles.label.copyWith(
                color: AppColors.emotionNegative,
              ),
            ),
          ),
        ],
      ),
    );
  }

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
              // Header
              DetailHeader(
                title: '음성 업로드',
                onBackPressed: () => context.pop(),
              ),
              const SizedBox(height: 32),

              // Registered voices
              const SectionHeader(title: '등록된 음성'),
              const SizedBox(height: 12),
              if (_voices.isEmpty)
                Container(
                  padding: const EdgeInsets.all(24),
                  decoration: BoxDecoration(
                    color: AppColors.cardBackground,
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: AppColors.cardBorder),
                  ),
                  child: Center(
                    child: Text(
                      '등록된 음성이 없습니다',
                      style: AppTextStyles.bodyMedium.copyWith(
                        color: AppColors.textTertiary,
                      ),
                    ),
                  ),
                )
              else
                ...List.generate(_voices.length, (index) {
                  final voice = _voices[index];
                  return Padding(
                    padding: EdgeInsets.only(
                      bottom: index < _voices.length - 1 ? 12 : 0,
                    ),
                    child: VoiceListItem(
                      voice: voice,
                      onPlay: () => _handlePlayVoice(voice),
                      onEdit: () => _handleEditVoice(voice),
                      onDelete: () => _handleDeleteVoice(voice),
                    ),
                  );
                }),
              const SizedBox(height: 24),

              // New voice registration
              const SectionHeader(title: '새 음성 등록'),
              const SizedBox(height: 12),
              OutlinedButtonWidget(
                text: '음성 녹음하기',
                icon: const Icon(
                  LucideIcons.mic,
                  size: 20,
                  color: AppColors.primaryDark,
                ),
                onPressed: _showRecordDialog,
              ),
              const SizedBox(height: 12),
              OutlinedButtonWidget(
                text: '파일 업로드',
                icon: const Icon(
                  LucideIcons.upload,
                  size: 20,
                  color: AppColors.primaryDark,
                ),
                onPressed: _handleFileUpload,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
