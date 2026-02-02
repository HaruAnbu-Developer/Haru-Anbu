import 'package:flutter/foundation.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

class AuthService {
  final SupabaseClient _supabase = Supabase.instance.client;

  Future<AuthResponse> signInWithGoogle() async {
    final webClientId = dotenv.env['GOOGLE_WEB_CLIENT_ID'] ?? '';
    final iosClientId = dotenv.env['GOOGLE_IOS_CLIENT_ID'] ?? '';

    final scopes = ['email', 'profile'];
    final googleSignIn = GoogleSignIn.instance;

    await googleSignIn.initialize(
      serverClientId: webClientId,
      clientId: iosClientId,
    );

    final googleUser = await googleSignIn.authenticate();

    final authorization =
        await googleUser.authorizationClient.authorizationForScopes(scopes) ??
            await googleUser.authorizationClient.authorizeScopes(scopes);

    final idToken = googleUser.authentication.idToken;

    if (idToken == null) {
      throw AuthException('ID Token을 가져올 수 없습니다.');
    }

    return await _supabase.auth.signInWithIdToken(
      provider: OAuthProvider.google,
      idToken: idToken,
      accessToken: authorization.accessToken,
    );
  }

  Future<void> signInWithKakao() async {
    await _supabase.auth.signInWithOAuth(
      OAuthProvider.kakao,
      redirectTo: kIsWeb ? null : 'com.haruwellness.app://login-callback',
      authScreenLaunchMode:
          kIsWeb ? LaunchMode.platformDefault : LaunchMode.externalApplication,
    );
  }

  Future<void> signOut() async {
    // Google 로그아웃
    try {
      await GoogleSignIn.instance.signOut();
    } catch (_) {}

    // Supabase 로그아웃
    await _supabase.auth.signOut();
  }

  User? get currentUser => _supabase.auth.currentUser;

  bool get isLoggedIn => currentUser != null;

  Stream<AuthState> get authStateChanges => _supabase.auth.onAuthStateChange;
}
