import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';
import '../../theme/app_colors.dart';
import '../../theme/app_text_styles.dart';
import '../../widgets/primary_button.dart';
import '../../widgets/custom_text_field.dart';
import '../../widgets/auth_header.dart';

class LoginScreen extends StatefulWidget {
  final VoidCallback? onBackPressed;
  final void Function(String email, String password)? onLoginPressed;
  final VoidCallback? onForgotPasswordPressed;

  const LoginScreen({
    super.key,
    this.onBackPressed,
    this.onLoginPressed,
    this.onForgotPasswordPressed,
  });

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _obscurePassword = true;

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  void _togglePasswordVisibility() {
    setState(() {
      _obscurePassword = !_obscurePassword;
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
                title: '로그인',
                subtitle: '이메일로 로그인',
                onBackPressed: widget.onBackPressed,
              ),
              const SizedBox(height: 36),
              // Form Section
              _buildFormSection(),
              const Spacer(),
              // Login Button
              _buildLoginButton(),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildFormSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.end,
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
        // Forgot Password Link
        GestureDetector(
          onTap: widget.onForgotPasswordPressed,
          child: Text(
            '비밀번호를 잊으셨나요?',
            style: AppTextStyles.link,
          ),
        ),
      ],
    );
  }

  Widget _buildLoginButton() {
    return PrimaryButton(
      text: '로그인',
      onPressed: () {
        widget.onLoginPressed?.call(
          _emailController.text,
          _passwordController.text,
        );
      },
    );
  }
}
