import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';
import '../theme/app_colors.dart';
import '../theme/app_text_styles.dart';

class ProfileCard extends StatelessWidget {
  final String? imageUrl;
  final String name;
  final String guardianInfo;
  final VoidCallback? onTap;

  const ProfileCard({
    super.key,
    this.imageUrl,
    required this.name,
    required this.guardianInfo,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppColors.cardBackground,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: AppColors.cardBorder, width: 1),
        ),
        child: Row(
          children: [
            // Profile Image
            _buildProfileImage(),
            const SizedBox(width: 12),
            // Name and Guardian Info
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    name,
                    style: AppTextStyles.profileName,
                  ),
                  const SizedBox(height: 2),
                  Text(
                    guardianInfo,
                    style: AppTextStyles.profileSubtitle,
                  ),
                ],
              ),
            ),
            // Chevron Icon
            const Icon(
              LucideIcons.chevronRight,
              size: 22,
              color: AppColors.primary,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildProfileImage() {
    return Container(
      width: 56,
      height: 56,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: AppColors.cardBackgroundAlt,
      ),
      child: ClipOval(
        child: imageUrl != null
            ? Image.network(
                imageUrl!,
                fit: BoxFit.cover,
                errorBuilder: (context, error, stackTrace) {
                  return _buildPlaceholder();
                },
              )
            : _buildPlaceholder(),
      ),
    );
  }

  Widget _buildPlaceholder() {
    return const Center(
      child: Icon(
        LucideIcons.user,
        size: 28,
        color: AppColors.primaryLight,
      ),
    );
  }
}
