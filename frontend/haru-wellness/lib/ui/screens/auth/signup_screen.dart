import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';
import '../../theme/app_colors.dart';
import '../../theme/app_text_styles.dart';
import '../../widgets/primary_button.dart';
import '../../widgets/custom_text_field.dart';
import '../../widgets/auth_header.dart';

class SignupScreen extends StatefulWidget {
  final VoidCallback? onBackPressed;
  final void Function(String email, String password)? onSignupPressed;
  final VoidCallback? onTermsPressed;
  final VoidCallback? onPrivacyPressed;

  const SignupScreen({
    super.key,
    this.onBackPressed,
    this.onSignupPressed,
    this.onTermsPressed,
    this.onPrivacyPressed,
  });

  @override
  State<SignupScreen> createState() => _SignupScreenState();
}

class _SignupScreenState extends State<SignupScreen> {
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();
  bool _obscurePassword = true;
  bool _obscureConfirmPassword = true;

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  void _togglePasswordVisibility() {
    setState(() {
      _obscurePassword = !_obscurePassword;
    });
  }

  void _toggleConfirmPasswordVisibility() {
    setState(() {
      _obscureConfirmPassword = !_obscureConfirmPassword;
    });
  }

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
              // Back Button and Header
              AuthHeader(
                title: '회원가입',
                subtitle: '이메일로 회원가입',
                onBackPressed: widget.onBackPressed,
              ),
              const SizedBox(height: 36),
              // Form Section
              _buildFormSection(),
              const Spacer(),
              // Signup Button
              _buildSignupButton(),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildFormSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Email Field
        CustomTextField(
          label: '이메일',
          controller: _emailController,
          keyboardType: TextInputType.emailAddress,
        ),
        const SizedBox(height: 16),
        // Password Field
        CustomTextField(
          label: '비밀번호',
          controller: _passwordController,
          obscureText: _obscurePassword,
          suffixIcon: GestureDetector(
            onTap: _togglePasswordVisibility,
            child: Icon(
              _obscurePassword ? LucideIcons.eyeOff : LucideIcons.eye,
              size: 24,
              color: AppColors.primaryLight,
            ),
          ),
        ),
        const SizedBox(height: 16),
        // Confirm Password Field
        CustomTextField(
          label: '비밀번호',
          controller: _confirmPasswordController,
          obscureText: _obscureConfirmPassword,
          suffixIcon: GestureDetector(
            onTap: _toggleConfirmPasswordVisibility,
            child: Icon(
              _obscureConfirmPassword ? LucideIcons.eyeOff : LucideIcons.eye,
              size: 24,
              color: AppColors.primaryLight,
            ),
          ),
        ),
        const SizedBox(height: 16),
        // Terms and Privacy Text
        _buildTermsText(),
      ],
    );
  }

  Widget _buildTermsText() {
    return RichText(
      text: TextSpan(
        style: AppTextStyles.bodyMedium.copyWith(
          height: 1.2,
        ),
        children: [
          const TextSpan(text: '가입절차를 진행하시면 하루안부의 '),
          TextSpan(
            text: '이용 약관',
            style: AppTextStyles.bodyMedium.copyWith(
              fontWeight: FontWeight.w600,
              decoration: TextDecoration.underline,
              height: 1.2,
            ),
            recognizer: TapGestureRecognizer()..onTap = widget.onTermsPressed,
          ),
          const TextSpan(text: '과 '),
          TextSpan(
            text: '개인정보 보호정책',
            style: AppTextStyles.bodyMedium.copyWith(
              fontWeight: FontWeight.w600,
              decoration: TextDecoration.underline,
              height: 1.2,
            ),
            recognizer: TapGestureRecognizer()..onTap = widget.onPrivacyPressed,
          ),
          const TextSpan(text: '에 동의하는 것으로 간주합니다. 약관을 잘 확인해 주세요.'),
        ],
      ),
    );
  }

  Widget _buildSignupButton() {
    return PrimaryButton(
      text: '회원가입',
      onPressed: () {
        widget.onSignupPressed?.call(
          _emailController.text,
          _passwordController.text,
        );
      },
    );
  }
}
