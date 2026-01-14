package com.cheongchun.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "daily_questions")
public class DailyQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 질문 내용 (예: "첫 월급으로 무엇을 하셨나요?")

    @Column(nullable = false, unique = true)
    private LocalDate targetDate; // 질문이 노출되는 날짜

    @Column(name = "topic_id", length = 50)
    private String topicId; // AI 팀의 토픽 ID (예: "FIRST_SALARY")

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public DailyQuestion(String content, LocalDate targetDate, String topicId) {
        this.content = content;
        this.targetDate = targetDate;
        this.topicId = topicId;
    }
}
