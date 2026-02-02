import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';
import '../../../domain/services/auth_service.dart';
import '../../theme/app_colors.dart';
import '../../theme/app_text_styles.dart';
import '../../widgets/profile_card.dart';
import '../../widgets/menu_item_widget.dart';
import '../../widgets/section_header.dart';

class MyPageScreen extends StatelessWidget {
  const MyPageScreen({super.key});

  void _handleLogout(BuildContext context) {
    showDialog(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('로그아웃'),
        content: const Text('정말 로그아웃 하시겠습니까?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () async {
              Navigator.of(dialogContext).pop();
              try {
                await AuthService().signOut();
                if (context.mounted) {
                  context.go('/');
                }
              } catch (e) {
                if (context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('로그아웃 실패: $e')),
                  );
                }
              }
            },
            child: const Text('로그아웃'),
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
              Text(
                '마이페이지',
                style: AppTextStyles.screenTitle,
              ),
              const SizedBox(height: 22),

              // Profile Card
              ProfileCard(
                name: '최성환님',
                guardianInfo: '김경수, 이춘화님 보호자',
                onTap: () {
                  // Navigate to profile edit
                },
              ),
              const SizedBox(height: 22),

              // 안부 전화 Section
              _buildCallSection(context),
              const SizedBox(height: 22),

              // 앱 설정 Section
              _buildSettingsSection(context),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildCallSection(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const SectionHeader(title: '안부 전화'),
        const SizedBox(height: 12),
        MenuItemWidget(
          title: '안부 스케쥴',
          icon: const Icon(
            LucideIcons.calendar,
            size: 22,
            color: AppColors.primary,
          ),
          onTap: () {
            context.push('/settings/call-schedule');
          },
        ),
        const SizedBox(height: 12),
        MenuItemWidget(
          title: '음성 업로드',
          icon: const Icon(
            LucideIcons.audioLines,
            size: 22,
            color: AppColors.primary,
          ),
          onTap: () {
            context.push('/settings/voice-management');
          },
        ),
      ],
    );
  }

  Widget _buildSettingsSection(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const SectionHeader(title: '앱 설정'),
        const SizedBox(height: 12),
        MenuItemWidget(
          title: '알림 설정',
          icon: const Icon(
            LucideIcons.chevronRight,
            size: 22,
            color: AppColors.primary,
          ),
          onTap: () {
            context.push('/settings/notifications');
          },
        ),
        const SizedBox(height: 12),
        MenuItemWidget(
          title: '도움말',
          icon: const Icon(
            LucideIcons.chevronRight,
            size: 22,
            color: AppColors.primary,
          ),
          onTap: () {
            // Navigate to help
          },
        ),
        const SizedBox(height: 12),
        MenuItemWidget(
          title: '로그아웃',
          icon: const Icon(
            LucideIcons.logOut,
            size: 22,
            color: AppColors.primary,
          ),
          onTap: () => _handleLogout(context),
        ),
      ],
    );
  }
}
