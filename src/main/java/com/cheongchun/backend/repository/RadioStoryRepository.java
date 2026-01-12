package com.cheongchun.backend.repository;

import com.cheongchun.backend.entity.RadioStory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RadioStoryRepository extends JpaRepository<RadioStory, Long> {
    List<RadioStory> findByQuestionId(Long questionId);
}
