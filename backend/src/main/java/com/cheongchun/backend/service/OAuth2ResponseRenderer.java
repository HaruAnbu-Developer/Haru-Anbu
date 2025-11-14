package com.cheongchun.backend.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class OAuth2ResponseRenderer {

    /**
     * OAuth2 로그인 성공 HTML 페이지 렌더링
     */
    public ResponseEntity<String> renderSuccessPage(String userId, String email, String name, String provider) {
        String html = String.format("""
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>로그인 성공</title>
                    <style>
                        body { 
                            font-family: 'Noto Sans KR', Arial, sans-serif; 
                            margin: 0; 
                            padding: 40px; 
                            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                            min-height: 100vh;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                        }
                        .container { 
                            background: white; 
                            padding: 40px; 
                            border-radius: 10px; 
                            box-shadow: 0 10px 30px rgba(0,0,0,0.3);
                            text-align: center;
                            max-width: 400px;
                            width: 100%%;
                        }
                        h1 { color: #4caf50; margin-bottom: 20px; }
                        .info { background: #f5f5f5; padding: 15px; border-radius: 5px; margin: 20px 0; }
                        button { 
                            background: #4285f4; 
                            color: white; 
                            border: none; 
                            padding: 15px 30px; 
                            border-radius: 5px; 
                            cursor: pointer; 
                            font-size: 16px;
                            margin: 10px;
                        }
                        button:hover { background: #357ae8; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>🎉 로그인 성공!</h1>
                        <p>청춘 애플리케이션에 성공적으로 로그인되었습니다.</p>
                        
                        <div class="info">
                            <p><strong>이름:</strong> %s</p>
                            <p><strong>이메일:</strong> %s</p>
                            <p><strong>사용자 ID:</strong> %s</p>
                            <p><strong>로그인 방식:</strong> %s</p>
                        </div>
                        
                        <button onclick="testAPI()">API 테스트</button>
                        <button onclick="goToMain()">메인으로 이동</button>
                        
                        <div id="result" style="margin-top: 20px;"></div>
                    </div>

                    <script>
                        // React Native WebView에 토큰 정보 전달
                        const userData = {
                            userId: '%s', 
                            email: '%s',
                            name: '%s',
                            provider: '%s'
                        };
                        
                        // WebView에 postMessage로 사용자 정보 전달 (React Native)
                        if (window.ReactNativeWebView) {
                            window.ReactNativeWebView.postMessage(JSON.stringify({
                                type: 'oauth_success',
                                data: userData
                            }));
                        } else {
                            // 웹 환경에서는 쿠키에 토큰이 이미 저장됨
                            document.querySelector('.container').innerHTML += 
                                '<div style="background: #e8f5e8; padding: 15px; border-radius: 5px; margin-top: 20px;">로그인이 완료되었습니다! 쿠키에 토큰이 안전하게 저장되었습니다.</div>';
                        }
                        
                        async function testAPI() {
                            try {
                                // 쿠키에서 자동으로 토큰을 읽어오므로 별도 헤더 설정 불필요
                                const response = await fetch('/auth/me', {
                                    credentials: 'include' // 쿠키 포함하여 요청
                                });
                                
                                const result = await response.json();
                                document.getElementById('result').innerHTML = 
                                    `<div style="background: #e8f5e8; padding: 10px; border-radius: 5px;">
                                        <strong>API 테스트 성공!</strong><br>
                                        <pre>${JSON.stringify(result, null, 2)}</pre>
                                    </div>`;
                            } catch (error) {
                                document.getElementById('result').innerHTML = 
                                    `<div style="background: #ffeaa7; padding: 10px; border-radius: 5px;">
                                        <strong>API 테스트 실패:</strong> ${error.message}
                                    </div>`;
                            }
                        }
                        
                        function goToMain() {
                            if (window.ReactNativeWebView) {
                                // React Native 앱 환경
                                window.ReactNativeWebView.postMessage(JSON.stringify({
                                    type: 'navigate_to_main'
                                }));
                            } else {
                                // 웹 브라우저 환경  
                                alert('로그인이 완료되었습니다! 메인 페이지로 이동합니다.');
                                window.location.href = '/';
                            }
                        }
                    </script>
                </body>
                </html>
                """, 
                name != null ? name : "N/A",
                email != null ? email : "N/A", 
                userId != null ? userId : "N/A",
                provider != null ? provider.toUpperCase() : "UNKNOWN",
                userId != null ? userId : "",
                email != null ? email : "",
                name != null ? name : "",
                provider != null ? provider : ""
        );

        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
    }

    /**
     * OAuth2 로그인 실패 HTML 페이지 렌더링
     */
    public ResponseEntity<String> renderErrorPage(String code, String message) {
        String html = String.format("""
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>로그인 실패</title>
                    <style>
                        body { 
                            font-family: 'Noto Sans KR', Arial, sans-serif; 
                            margin: 0; 
                            padding: 40px; 
                            background: linear-gradient(135deg, #ff6b6b 0%%, #ee5a52 100%%);
                            min-height: 100vh;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                        }
                        .container { 
                            background: white; 
                            padding: 40px; 
                            border-radius: 10px; 
                            box-shadow: 0 10px 30px rgba(0,0,0,0.3);
                            text-align: center;
                            max-width: 400px;
                            width: 100%%;
                        }
                        h1 { color: #e74c3c; margin-bottom: 20px; }
                        .error { background: #ffebee; padding: 15px; border-radius: 5px; margin: 20px 0; }
                        button { 
                            background: #e74c3c; 
                            color: white; 
                            border: none; 
                            padding: 15px 30px; 
                            border-radius: 5px; 
                            cursor: pointer; 
                            font-size: 16px;
                            margin: 10px;
                        }
                        button:hover { background: #c0392b; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>❌ 로그인 실패</h1>
                        <p>로그인 중 문제가 발생했습니다.</p>
                        
                        <div class="error">
                            <p><strong>오류 코드:</strong> %s</p>
                            <p><strong>오류 메시지:</strong> %s</p>
                        </div>
                        
                        <button onclick="goToLogin()">로그인 다시 시도</button>
                        
                        <div style="margin-top: 20px; color: #666; font-size: 14px;">
                            문제가 계속 발생하면 고객센터에 문의해주세요.
                        </div>
                    </div>

                    <script>
                        // React Native WebView에 에러 정보 전달
                        if (window.ReactNativeWebView) {
                            window.ReactNativeWebView.postMessage(JSON.stringify({
                                type: 'oauth_error',
                                error: {
                                    code: '%s',
                                    message: '%s'
                                }
                            }));
                        }
                        
                        function goToLogin() {
                            if (window.ReactNativeWebView) {
                                window.ReactNativeWebView.postMessage(JSON.stringify({
                                    type: 'navigate_to_login'
                                }));
                            } else {
                                window.location.href = '/login';
                            }
                        }
                    </script>
                </body>
                </html>
                """, 
                code != null ? code : "UNKNOWN_ERROR",
                message != null ? message : "알 수 없는 오류가 발생했습니다.",
                code != null ? code : "",
                message != null ? message : ""
        );

        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
    }
}