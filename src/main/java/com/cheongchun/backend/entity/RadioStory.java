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
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "community_radio_topics")
public class RadioStory { // AI 팀의 CommunityRadioTopic 스키마와 매핑

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "topic_id", length = 50)
    private String topicId; // 예: "FIRST_SALARY" (첫 월급 주제)

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId; // 답변한 어르신 ID (문자열로 저장됨)

    @Column(name = "answer_text", nullable = false, columnDefinition = "TEXT")
    private String answerText; // "나는 정장을 맞췄어"

    @Column(name = "is_shared", nullable = false)
    @ColumnDefault("false")
    private Boolean isShared = false; // 방송 공유 동의 여부

    @Column(name = "broadcast_date")
    private LocalDateTime broadcastDate; // 방송 예정일

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public RadioStory(String topicId, String userId, String answerText, LocalDateTime broadcastDate) {
        this.topicId = topicId;
        this.userId = userId;
        this.answerText = answerText;
        this.broadcastDate = broadcastDate;
    }
}
