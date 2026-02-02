import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';
import '../../theme/app_colors.dart';
import '../../widgets/primary_button.dart';
import '../../widgets/outlined_button_widget.dart';

class LandingScreen extends StatelessWidget {
  final VoidCallback? onGooglePressed;
  final VoidCallback? onKakaoPressed;
  final VoidCallback? onEmailPressed;

  const LandingScreen({
    super.key,
    this.onGooglePressed,
    this.onKakaoPressed,
    this.onEmailPressed,
  });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      resizeToAvoidBottomInset: false,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(8),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Spacer(),
              // Hero Image Section
              _buildHeroImage(),
              const Spacer(),
              // Buttons Section
              _buildButtonsSection(),
              const SizedBox(height: 8),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildHeroImage() {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        SizedBox(
          width: double.infinity,
          height: 200,
          child: Center(
            child: Image.asset(
              'assets/images/landing_image.png',
              fit: BoxFit.fitHeight,
            ),
          ),
        ),
        const SizedBox(height: 16),
        const Text(
          '매일의 안녕을 위해서',
          style: TextStyle(
            fontFamily: 'TAEBAEK',
            fontSize: 18,
            color: AppColors.primaryLight,
          ),
        ),
      ],
    );
  }

  Widget _buildButtonsSection() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Column(
        children: [
          // Social Login Buttons Row
          Row(
            children: [
              // Google Login Button
              Expanded(
                child: OutlinedButtonWidget(
                  text: '구글로 시작하기',
                  icon: SvgPicture.asset(
                    'assets/icons/google_logo.svg',
                    width: 16,
                    height: 16,
                  ),
                  onPressed: onGooglePressed,
                ),
              ),
              const SizedBox(width: 12),
              // Kakao Login Button
              Expanded(
                child: OutlinedButtonWidget(
                  text: '카카오로 시작하기',
                  icon: SvgPicture.asset(
                    'assets/icons/kakao_logo.svg',
                    width: 16,
                    height: 16,
                  ),
                  onPressed: onKakaoPressed,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          // Email Login Button
          PrimaryButton(
            text: '시작하기',
            onPressed: onEmailPressed,
          ),
        ],
      ),
    );
  }
}
