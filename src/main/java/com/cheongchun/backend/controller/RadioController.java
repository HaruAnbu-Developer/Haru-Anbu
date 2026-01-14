package com.cheongchun.backend.controller;

import com.cheongchun.backend.entity.DailyQuestion;
import com.cheongchun.backend.entity.RadioStory;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.service.radio.RadioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/radio")
@RequiredArgsConstructor
public class RadioController {

    private final RadioService radioService;
    private final com.cheongchun.backend.repository.UserRepository userRepository;

    // 1. 오늘의 질문 조회
    @GetMapping("/today-question")
    public ResponseEntity<DailyQuestion> getTodayQuestion() {
        return ResponseEntity.ok(radioService.getTodayQuestion());
    }

    // 2. 사연 제출
    @PostMapping("/story")
    public ResponseEntity<RadioStory> submitStory(@AuthenticationPrincipal Object principal,
            @RequestParam Long questionId,
            @RequestBody String content) {
        User user = getUserFromPrincipal(principal);
        return ResponseEntity.ok(radioService.submitStory(user, questionId, content));
    }

    // 3. (다음날) 라디오 사연들 조회
    // date: YYYY-MM-DD 형식 (예: 어제 질문에 대한 답변을 오늘 들으려면 어제 날짜를 넣음)
    @GetMapping("/stories")
    public ResponseEntity<List<RadioStory>> getRadioStories(@RequestParam String date) {
        LocalDate targetDate = LocalDate.parse(date);
        return ResponseEntity.ok(radioService.getRadioStoriesForDate(targetDate));
    }

    private User getUserFromPrincipal(Object principal) {
        if (principal instanceof User) {
            return (User) principal;
        } else if (principal instanceof com.cheongchun.backend.security.CustomOAuth2User) {
            com.cheongchun.backend.security.CustomOAuth2User oauth2User = (com.cheongchun.backend.security.CustomOAuth2User) principal;
            return userRepository.findById(oauth2User.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
        }
        throw new IllegalArgumentException("Unauthorized");
    }
}
