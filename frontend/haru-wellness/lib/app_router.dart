import 'dart:async';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

// Auth Screens (in navigation flow order)
import 'ui/screens/auth/landing_screen.dart';
import 'domain/services/auth_service.dart';
import 'ui/screens/auth/email_auth_screen.dart';
import 'ui/screens/auth/login_screen.dart';
import 'ui/screens/auth/signup_screen.dart';

// Main Screens
import 'ui/screens/main_navigation.dart';
import 'ui/screens/home/home_screen.dart';
import 'ui/screens/calls/call_history_screen.dart';
import 'ui/screens/calls/call_detail_screen.dart';
import 'ui/screens/my/my_page_screen.dart';

// Settings Screens
import 'ui/screens/settings/call_schedule_screen.dart';
import 'ui/screens/settings/notification_settings_screen.dart';
import 'ui/screens/settings/voice_management_screen.dart';

class _AuthStateNotifier extends ChangeNotifier {
  late final StreamSubscription<AuthState> _subscription;

  _AuthStateNotifier() {
    _subscription = Supabase.instance.client.auth.onAuthStateChange.listen((data) {
      notifyListeners();
    });
  }

  @override
  void dispose() {
    _subscription.cancel();
    super.dispose();
  }
}

class AppRouter {
  static final AuthService _authService = AuthService();
  static final _AuthStateNotifier _authStateNotifier = _AuthStateNotifier();

  static Future<void> _handleGoogleSignIn(BuildContext context) async {
    try {
      showDialog(
        context: context,
        barrierDismissible: false,
        builder: (context) => const Center(
          child: CircularProgressIndicator(),
        ),
      );

      await _authService.signInWithGoogle();

      if (context.mounted) {
        Navigator.of(context).pop();
        router.go('/home');
      }
    } catch (e) {
      if (context.mounted) {
        Navigator.of(context).pop();
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('로그인 실패: $e')),
        );
      }
    }
  }

  static Future<void> _handleKakaoSignIn(BuildContext context) async {
    try {
      await _authService.signInWithKakao();
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('카카오 로그인 실패: $e')),
        );
      }
    }
  }

  static final List<String> _authRoutes = ['/', '/email-auth', '/login', '/signup'];

  static final GoRouter router = GoRouter(
    initialLocation: '/',
    refreshListenable: _authStateNotifier,
    redirect: (context, state) {
      final isLoggedIn = _authService.isLoggedIn;
      final isAuthRoute = _authRoutes.contains(state.matchedLocation);

      if (isLoggedIn && isAuthRoute) {
        return '/home';
      }

      if (!isLoggedIn && !isAuthRoute) {
        return '/';
      }

      return null;
    },
    routes: [
      // 1. Landing Screen - 앱 시작 화면
      GoRoute(
        path: '/',
        builder: (context, state) => LandingScreen(
          onGooglePressed: () => _handleGoogleSignIn(context),
          onKakaoPressed: () => _handleKakaoSignIn(context),
          onEmailPressed: () {
            router.push('/email-auth');
          },
        ),
      ),

      // 2. Email Auth Screen - 이메일 로그인/회원가입 선택
      GoRoute(
        path: '/email-auth',
        builder: (context, state) => EmailAuthScreen(
          onBackPressed: () {
            router.pop();
          },
          onLoginPressed: () {
            router.push('/login');
          },
          onSignupPressed: () {
            router.push('/signup');
          },
        ),
      ),

      // 3. Login Screen - 로그인
      GoRoute(
        path: '/login',
        builder: (context, state) => LoginScreen(
          onBackPressed: () {
            router.pop();
          },
          onLoginPressed: (email, password) {
            // TODO: Implement email login
            // Navigate to home after successful login
            router.go('/home');
          },
          onForgotPasswordPressed: () {
            // TODO: Implement forgot password
          },
        ),
      ),

      // 4. Signup Screen - 회원가입
      GoRoute(
        path: '/signup',
        builder: (context, state) => SignupScreen(
          onBackPressed: () {
            router.pop();
          },
          onSignupPressed: (email, password) {
            // TODO: Implement signup
            // Navigate to home after successful signup
            router.go('/home');
          },
          onTermsPressed: () {
            // TODO: Show terms
          },
          onPrivacyPressed: () {
            // TODO: Show privacy policy
          },
        ),
      ),

      // Call Detail Screen (outside of shell for no bottom nav)
      GoRoute(
        path: '/calls/detail/:id',
        builder: (context, state) {
          final callId = state.pathParameters['id'];
          return CallDetailScreen(callId: callId);
        },
      ),

      // Settings Screens (outside of shell for no bottom nav)
      GoRoute(
        path: '/settings/call-schedule',
        builder: (context, state) => const CallScheduleScreen(),
      ),
      GoRoute(
        path: '/settings/notifications',
        builder: (context, state) => const NotificationSettingsScreen(),
      ),
      GoRoute(
        path: '/settings/voice-management',
        builder: (context, state) => const VoiceManagementScreen(),
      ),

      // Main app with bottom navigation
      StatefulShellRoute.indexedStack(
        builder: (context, state, navigationShell) {
          return MainNavigation(navigationShell: navigationShell);
        },
        branches: [
          // Home tab
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/home',
                pageBuilder: (context, state) => const NoTransitionPage(
                  child: HomeScreen(),
                ),
              ),
            ],
          ),
          // Calls tab
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/calls',
                pageBuilder: (context, state) => const NoTransitionPage(
                  child: CallHistoryScreen(),
                ),
              ),
            ],
          ),
          // My Page tab
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/my',
                pageBuilder: (context, state) => const NoTransitionPage(
                  child: MyPageScreen(),
                ),
              ),
            ],
          ),
        ],
      ),
    ],
  );
}
