package com.cheongchun.backend.service.Radio;

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
import java.util.List;

@Service
@RequiredArgsConstructor
public class RadioService {

    private final DailyQuestionRepository dailyQuestionRepository;
    private final RadioStoryRepository radioStoryRepository;
    private final UserRepository userRepository; // To fetch current user if needed, or valid user

    /**
     * 오늘의 질문 가져오기
     */
    public DailyQuestion getTodayQuestion() {
        LocalDate today = LocalDate.now();
        return dailyQuestionRepository.findByTargetDate(today)
                .orElseThrow(() -> new IllegalArgumentException("No question found for today (" + today + ")"));
    }

    /**
     * 사연 제출하기
     */
    @Transactional
    public RadioStory submitStory(User user, Long questionId, String content) {
        DailyQuestion question = dailyQuestionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found with id: " + questionId));

        RadioStory story = new RadioStory(user, question, content);
        return radioStoryRepository.save(story);
    }

    /**
     * 특정 날짜의 질문에 대한 사연들(라디오 콘텐츠) 가져오기
     * 보통 "어제"의 질문에 대한 답변들을 "오늘" 라디오로 듣기 때문에 date 파라미터가 유동적일 수 있음.
     */
    @Transactional(readOnly = true)
    public List<RadioStory> getRadioStoriesForDate(LocalDate date) {
        // 1. 해당 날짜가 targetDate인 질문을 찾음
        DailyQuestion question = dailyQuestionRepository.findByTargetDate(date)
                .orElseThrow(() -> new IllegalArgumentException("No question found for date: " + date));

        // 2. 그 질문에 달린 사연들을 가져옴
        return radioStoryRepository.findByQuestionId(question.getId());
    }
}
