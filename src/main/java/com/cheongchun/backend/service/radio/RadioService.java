package com.cheongchun.backend.service.radio;

import com.cheongchun.backend.entity.DailyQuestion;
import com.cheongchun.backend.entity.RadioStory;
import com.cheongchun.backend.entity.User;

import com.cheongchun.backend.repository.RadioStoryRepository;
import com.cheongchun.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RadioService {

    private final com.cheongchun.backend.service.AiClient aiClient;
    private final com.cheongchun.backend.service.S3Service s3Service;
    private final RadioStoryRepository radioStoryRepository;
    private final UserRepository userRepository; // User 로딩용

    /**
     * 오늘의 질문 가져오기 (AI 생성)
     */
    public DailyQuestion getTodayQuestion() {
        var response = aiClient.getDailyQuestion();

        // DB에 저장하지 않고 임시 객체 반환
        DailyQuestion question = new DailyQuestion();
        question.setContent(response.getQuestion());
        question.setTopicId(response.getTopicId());
        question.setTargetDate(LocalDate.now());
        question.setCreatedAt(LocalDateTime.now());

        return question;
    }

    /**
     * 사연 제출하기 (AI가 라디오 리라이팅 & TTS 생성)
     */
    /**
     * 사연 제출하기 (AI가 라디오 리라이팅 & TTS 생성 -> S3 업로드)
     */
    @Transactional
    public RadioResult submitStory(User user, String topicId, String content) {
        // 1. AI Core에 라디오 생성 요청
        var response = aiClient.generateRadio(String.valueOf(user.getId()), user.getUsername(), content);

        // 2. S3 업로드
        String fileName = "radio/" + topicId + "/" + user.getUsername() + "_" + System.currentTimeMillis() + ".wav";
        String s3Url = s3Service.uploadFile(response.getAudioData().toByteArray(), fileName);

        // 3. 답변 DB 저장 (히스토리용)
        RadioStory story = new RadioStory();
        story.setTopicId(topicId);
        story.setUserId(user.getUsername());
        story.setAnswerText(content); // 원본 답변
        story.setIsShared(true);
        story.setBroadcastDate(LocalDateTime.now());
        // story.setAudioUrl(s3Url); // 엔티티에 필드가 있다면 추가, 현재는 없음
        radioStoryRepository.save(story);

        // 4. 결과 반환 (S3 URL & 대본)
        return new RadioResult(response.getScript(), s3Url);
    }

    /**
     * 특정 날짜의 라디오 사연들 가져오기
     */
    @Transactional(readOnly = true)
    public List<RadioStory> getRadioStoriesForDate(LocalDate date) {
        // DB 최적화: date 필드를 사용하거나 topicId 패턴을 사용해야 함.
        // 현재는 topicId가 날짜(YYYY-MM-DD)라고 가정하고 조회
        String topicId = date.toString();
        return radioStoryRepository.findByTopicId(topicId);
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class RadioResult {
        private String script;
        private String audioUrl;
    }
}
