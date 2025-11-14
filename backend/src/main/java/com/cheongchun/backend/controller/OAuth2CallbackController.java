package com.cheongchun.backend.controller;

import com.cheongchun.backend.service.OAuth2ResponseRenderer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class OAuth2CallbackController {

    private final OAuth2ResponseRenderer responseRenderer;

    public OAuth2CallbackController(OAuth2ResponseRenderer responseRenderer) {
        this.responseRenderer = responseRenderer;
    }

    /**
     * OAuth2 성공 콜백 처리
     * OAuth2LoginHandler에서 리다이렉트되는 엔드포인트
     */
    @GetMapping("/oauth-success")
    public ResponseEntity<String> handleOAuthSuccess(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String provider) {
        
        return responseRenderer.renderSuccessPage(userId, email, name, provider);
    }

    /**
     * OAuth2 실패 콜백 처리
     */
    @GetMapping("/oauth-error")
    public ResponseEntity<String> handleOAuthError(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String message) {
        
        return responseRenderer.renderErrorPage(code, message);
    }
}