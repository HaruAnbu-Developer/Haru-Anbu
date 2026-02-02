import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';
import '../../../data/models/voice_entry.dart';
import '../../theme/app_colors.dart';
import '../../theme/app_text_styles.dart';

class VoiceListItem extends StatelessWidget {
  final VoiceEntry voice;
  final VoidCallback? onPlay;
  final VoidCallback? onEdit;
  final VoidCallback? onDelete;

  const VoiceListItem({
    super.key,
    required this.voice,
    this.onPlay,
    this.onEdit,
    this.onDelete,
  });

  String _formatDate(DateTime date) {
    return '${date.year}.${date.month.toString().padLeft(2, '0')}.${date.day.toString().padLeft(2, '0')}';
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.cardBackground,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: AppColors.cardBorder, width: 1),
      ),
      child: Row(
        children: [
          // Voice icon
          Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(
              color: AppColors.primaryLighter,
              borderRadius: BorderRadius.circular(22),
            ),
            child: const Icon(
              LucideIcons.audioLines,
              size: 22,
              color: AppColors.primary,
            ),
          ),
          const SizedBox(width: 12),
          // Voice info
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  voice.alias,
                  style: AppTextStyles.profileName,
                ),
                const SizedBox(height: 4),
                Text(
                  _formatDate(voice.createdAt),
                  style: AppTextStyles.profileSubtitle,
                ),
              ],
            ),
          ),
          // Action buttons
          Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              if (onPlay != null)
                _buildActionButton(LucideIcons.play, onPlay!),
              if (onEdit != null) ...[
                const SizedBox(width: 8),
                _buildActionButton(LucideIcons.pencil, onEdit!),
              ],
              if (onDelete != null) ...[
                const SizedBox(width: 8),
                _buildActionButton(LucideIcons.trash2, onDelete!),
              ],
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildActionButton(IconData icon, VoidCallback onTap) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: 36,
        height: 36,
        decoration: BoxDecoration(
          color: AppColors.background,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: AppColors.cardBorder, width: 1),
        ),
        child: Icon(
          icon,
          size: 18,
          color: AppColors.primary,
        ),
      ),
    );
  }
}
