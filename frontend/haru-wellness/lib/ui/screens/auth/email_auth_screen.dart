import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';
import '../../theme/app_colors.dart';
import '../../widgets/primary_button.dart';
import '../../widgets/outlined_button_widget.dart';
import '../../widgets/auth_header.dart';

class EmailAuthScreen extends StatelessWidget {
  final VoidCallback? onBackPressed;
  final VoidCallback? onLoginPressed;
  final VoidCallback? onSignupPressed;

  const EmailAuthScreen({
    super.key,
    this.onBackPressed,
    this.onLoginPressed,
    this.onSignupPressed,
  });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      resizeToAvoidBottomInset: false,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(22),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Header Section
              AuthHeader(
                title: '시작하기',
                subtitle: '이메일로 로그인 혹은 회원가입',
                onBackPressed: onBackPressed,
              ),
              const Spacer(),
              // Buttons Section
              _buildButtonsSection(),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildButtonsSection() {
    return Column(
      children: [
        // Login Button (Outlined)
        OutlinedButtonWidget(
          text: '계정이 있으신가요?',
          icon: const Icon(
            LucideIcons.logIn,
            size: 24,
            color: AppColors.textPrimary,
          ),
          onPressed: onLoginPressed,
        ),
        const SizedBox(height: 12),
        // Signup Button (Primary)
        PrimaryButton(
          text: '새로 오셨나요?',
          icon: const Icon(
            LucideIcons.pencil,
            size: 24,
            color: AppColors.buttonText,
          ),
          onPressed: onSignupPressed,
        ),
      ],
    );
  }
}
