package com.cheongchun.backend.repository;

import com.cheongchun.backend.entity.RadioStory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RadioStoryRepository extends JpaRepository<RadioStory, Long> {
    List<RadioStory> findByTopicId(String topicId);

    // 방송 날짜로 검색 (범위 검색 등은 필요시 추가)
    List<RadioStory> findByBroadcastDateBetween(LocalDateTime start, LocalDateTime end);
}
