package com.cheongchun.backend.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 루트 경로 컨트롤러 (context-path 없이 접근)
 * Google Search Console 도메인 인증을 위해 필요
 */
@Controller
public class RootController {

    /**
     * Google Search Console 도메인 인증 파일
     * URL: https://cheongchun-backend-40635111975.asia-northeast3.run.app/google32870450675243f1.html
     */
    @GetMapping("/google32870450675243f1.html")
    public ResponseEntity<String> getGoogleVerificationRoot() {
        String content = "google-site-verification: google32870450675243f1.html";
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
    }

    /**
     * 루트 경로 처리 - OAuth 로그인 성공 후 프론트엔드로 리다이렉트
     */
    @GetMapping("/")
    public ResponseEntity<String> redirectToFrontend() {
        String content = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>청춘 - 프론트엔드로 이동</title>
                    <meta charset="UTF-8">
                    <script>
                        // 프론트엔드 앱으로 리다이렉트
                        window.location.href = 'http://localhost:8081/main';
                    </script>
                </head>
                <body>
                    <p>프론트엔드 앱으로 이동 중...</p>
                </body>
                </html>
                """;
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(content);
    }

    /**
     * Google OAuth 동의 화면용 개인정보처리방침 페이지
     */
    @GetMapping("/privacy")
    public ResponseEntity<String> getPrivacyPolicy() {
        String content = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>개인정보처리방침 - 청춘</title>
                    <meta charset="UTF-8">
                </head>
                <body>
                    <h1>개인정보처리방침</h1>
                    <p>청춘 애플리케이션의 개인정보처리방침입니다.</p>
                    <p>수집하는 개인정보: 이메일, 이름, 프로필 사진</p>
                    <p>이용 목적: 서비스 제공 및 사용자 인증</p>
                    <p>보관 기간: 회원 탈퇴 시까지</p>
                </body>
                </html>
                """;
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(content);
    }

    /**
     * Google OAuth 동의 화면용 서비스 약관 페이지
     */
    @GetMapping("/terms")
    public ResponseEntity<String> getTermsOfService() {
        String content = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>서비스 약관 - 청춘</title>
                    <meta charset="UTF-8">
                </head>
                <body>
                    <h1>서비스 약관</h1>
                    <p>청춘 애플리케이션의 서비스 약관입니다.</p>
                    <p>1. 본 서비스는 만남과 소통을 목적으로 합니다.</p>
                    <p>2. 부적절한 콘텐츠 게시는 금지됩니다.</p>
                    <p>3. 개인정보는 안전하게 보호됩니다.</p>
                </body>
                </html>
                """;
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(content);
    }
}