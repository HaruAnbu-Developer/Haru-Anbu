package com.cheongchun.backend.service.radio;

import com.cheongchun.backend.entity.DailyQuestion;
import com.cheongchun.backend.entity.RadioStory;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.repository.DailyQuestionRepository;
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

    private final DailyQuestionRepository dailyQuestionRepository;
    private final RadioStoryRepository radioStoryRepository;
    private final UserRepository userRepository;

    /**
     * 오늘의 질문 가져오기
     */
    public DailyQuestion getTodayQuestion() {
        LocalDate today = LocalDate.now();
        return dailyQuestionRepository.findByTargetDate(today)
                .orElseThrow(() -> new IllegalArgumentException("No question found for today (" + today + ")"));
    }

    /**
     * 사연 제출하기 (앱에서 직접 제출 시)
     */
    @Transactional
    public RadioStory submitStory(User user, Long questionId, String content) {
        DailyQuestion question = dailyQuestionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found with id: " + questionId));
        
        // Topic ID가 없으면 에러 혹은 기본값 처리 (여기서는 필수라고 가정)
        if (question.getTopicId() == null) {
            throw new IllegalStateException("Question does not have a linked topic ID.");
        }

        RadioStory story = new RadioStory();
        story.setTopicId(question.getTopicId());
        story.setUserId(user.getUsername()); // String(50)에 username 매핑
        story.setAnswerText(content);
        story.setIsShared(true); // 제출 시 기본적으로 공유 동의라고 가정 (혹은 파라미터로 받아야 함)
        story.setBroadcastDate(LocalDateTime.now().plusDays(1)); // 예: 다음날 방송
        
        return radioStoryRepository.save(story);
    }

    /**
     * 특정 날짜의 라디오 사연들 가져오기
     * (해당 날짜에 답변된 질문의 Topic ID로 조회)
     */
    @Transactional(readOnly = true)
    public List<RadioStory> getRadioStoriesForDate(LocalDate date) {
        // 1. 해당 날짜가 targetDate인 질문을 찾음
        DailyQuestion question = dailyQuestionRepository.findByTargetDate(date)
                .orElseThrow(() -> new IllegalArgumentException("No question found for date: " + date));

        // 2. 그 질문의 Topic ID로 사연들을 검색
        String topicId = question.getTopicId();
        if (topicId == null) {
            return List.of();
        }
        
        return radioStoryRepository.findByTopicId(topicId);
    }
}
