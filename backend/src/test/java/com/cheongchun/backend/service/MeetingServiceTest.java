package com.cheongchun.backend.service;

import com.cheongchun.backend.dto.CreatingMeetingRequest;
import com.cheongchun.backend.entity.Meeting;
import com.cheongchun.backend.entity.MeetingParticipant;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.exception.BusinessException;
import com.cheongchun.backend.exception.ErrorCode;
import com.cheongchun.backend.repository.MeetingParticipantRepository;
import com.cheongchun.backend.repository.MeetingRepository;
import com.cheongchun.backend.repository.UserWishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MeetingService 단위 테스트")
class MeetingServiceTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MeetingParticipantRepository participantRepository;

    @Mock
    private UserWishlistRepository wishlistRepository;

    @Mock
    private MeetingParticipantService meetingParticipantService;

    @InjectMocks
    private MeetingService meetingService;

    private User testUser;
    private Meeting testMeeting;
    private CreatingMeetingRequest validMeetingRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("organizer");
        testUser.setEmail("organizer@example.com");
        testUser.setName("주최자");

        testMeeting = new Meeting();
        testMeeting.setId(1L);
        testMeeting.setTitle("테스트 모임");
        testMeeting.setDescription("테스트 모임 설명");
        testMeeting.setCategory(Meeting.Category.HOBBY);
        testMeeting.setLocation("서울");
        testMeeting.setStartDate(LocalDateTime.now().plusDays(2));
        testMeeting.setEndDate(LocalDateTime.now().plusDays(2).plusHours(2));
        testMeeting.setMaxParticipants(10);
        testMeeting.setCurrentParticipants(1);
        testMeeting.setFee(0);
        testMeeting.setCreatedBy(testUser);
        testMeeting.setStatus(Meeting.Status.RECRUITING);

        validMeetingRequest = new CreatingMeetingRequest();
        validMeetingRequest.setTitle("테스트 모임");
        validMeetingRequest.setDescription("테스트 모임 설명");
        validMeetingRequest.setCategory(Meeting.Category.HOBBY);
        validMeetingRequest.setLocation("서울");
        validMeetingRequest.setAddress("서울시 강남구");
        validMeetingRequest.setStartDate(LocalDateTime.now().plusDays(2));
        validMeetingRequest.setEndDate(LocalDateTime.now().plusDays(2).plusHours(2));
        validMeetingRequest.setMaxParticipants(10);
        validMeetingRequest.setFee(0);
        validMeetingRequest.setAutoApprovalLimit(0);
    }

    @Test
    @DisplayName("모임 생성 성공")
    void createMeeting_Success() {
        // Given
        when(meetingRepository.existsByTitleAndCreatedByAndStartDate(
                anyString(), any(User.class), any(LocalDateTime.class)
        )).thenReturn(false);
        when(meetingRepository.save(any(Meeting.class))).thenReturn(testMeeting);
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(new MeetingParticipant());

        // When
        CreatingMeetingRequest.MeetingResponse response = meetingService.createMeeting(validMeetingRequest, testUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("테스트 모임");
        assertThat(response.getCategory()).isEqualTo(Meeting.Category.HOBBY);

        verify(meetingRepository).existsByTitleAndCreatedByAndStartDate(
                anyString(), any(User.class), any(LocalDateTime.class)
        );
        verify(meetingRepository, times(2)).save(any(Meeting.class)); // 생성 + 주최자 참여 후 업데이트
        verify(participantRepository).save(any(MeetingParticipant.class));
    }

    @Test
    @DisplayName("모임 생성 실패 - 중복 모임")
    void createMeeting_DuplicateMeeting() {
        // Given
        when(meetingRepository.existsByTitleAndCreatedByAndStartDate(
                anyString(), any(User.class), any(LocalDateTime.class)
        )).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> meetingService.createMeeting(validMeetingRequest, testUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 같은 시간에 동일한 제목의 모임이 존재합니다");

        verify(meetingRepository, never()).save(any(Meeting.class));
    }

    @Test
    @DisplayName("모임 생성 실패 - 시작 시간이 현재로부터 1시간 이내")
    void createMeeting_StartTimeTooSoon() {
        // Given
        validMeetingRequest.setStartDate(LocalDateTime.now().plusMinutes(30)); // 30분 후

        // When & Then
        assertThatThrownBy(() -> meetingService.createMeeting(validMeetingRequest, testUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);

        verify(meetingRepository, never()).save(any(Meeting.class));
    }

    @Test
    @DisplayName("모임 생성 실패 - 종료 시간이 시작 시간보다 이전")
    void createMeeting_EndTimeBeforeStartTime() {
        // Given
        validMeetingRequest.setEndDate(validMeetingRequest.getStartDate().minusHours(1));

        // When & Then
        assertThatThrownBy(() -> meetingService.createMeeting(validMeetingRequest, testUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);

        verify(meetingRepository, never()).save(any(Meeting.class));
    }

    @Test
    @DisplayName("모임 생성 실패 - 모임 시간이 12시간 초과")
    void createMeeting_DurationTooLong() {
        // Given
        validMeetingRequest.setEndDate(validMeetingRequest.getStartDate().plusHours(13));

        // When & Then
        assertThatThrownBy(() -> meetingService.createMeeting(validMeetingRequest, testUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);

        verify(meetingRepository, never()).save(any(Meeting.class));
    }

    @Test
    @DisplayName("모임 조회 성공")
    void getMeetingById_Success() {
        // Given
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndStatus(anyLong(), any()))
                .thenReturn(java.util.Collections.emptyList());

        // When
        CreatingMeetingRequest.MeetingResponse response = meetingService.getMeetingById(1L, testUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("테스트 모임");

        verify(meetingRepository).findById(1L);
    }

    @Test
    @DisplayName("모임 조회 실패 - 존재하지 않는 모임")
    void getMeetingById_NotFound() {
        // Given
        when(meetingRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> meetingService.getMeetingById(999L, testUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEETING_NOT_FOUND);

        verify(meetingRepository).findById(999L);
    }

    @Test
    @DisplayName("모임 수정 성공")
    void updateMeeting_Success() {
        // Given
        testMeeting.setStartDate(LocalDateTime.now().plusDays(5)); // 충분한 시간 확보

        CreatingMeetingRequest.UpdateMeetingRequest updateRequest = new CreatingMeetingRequest.UpdateMeetingRequest();
        updateRequest.setTitle("수정된 모임");
        updateRequest.setDescription("수정된 설명");

        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(meetingRepository.save(any(Meeting.class))).thenReturn(testMeeting);
        when(participantRepository.findByMeetingIdAndStatus(anyLong(), any()))
                .thenReturn(java.util.Collections.emptyList());

        // When
        CreatingMeetingRequest.MeetingResponse response = meetingService.updateMeeting(1L, updateRequest, testUser);

        // Then
        assertThat(response).isNotNull();
        verify(meetingRepository).findById(1L);
        verify(meetingRepository).save(any(Meeting.class));
    }

    @Test
    @DisplayName("모임 수정 실패 - 권한 없음 (다른 사용자)")
    void updateMeeting_Unauthorized() {
        // Given
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");

        CreatingMeetingRequest.UpdateMeetingRequest updateRequest = new CreatingMeetingRequest.UpdateMeetingRequest();
        updateRequest.setTitle("수정 시도");

        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));

        // When & Then
        assertThatThrownBy(() -> meetingService.updateMeeting(1L, updateRequest, otherUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_USER_CREDENTIALS);

        verify(meetingRepository).findById(1L);
        verify(meetingRepository, never()).save(any(Meeting.class));
    }

    @Test
    @DisplayName("모임 수정 실패 - 시작 2시간 전부터는 수정 불가")
    void updateMeeting_TooCloseToStartTime() {
        // Given
        testMeeting.setStartDate(LocalDateTime.now().plusHours(1)); // 1시간 후 시작

        CreatingMeetingRequest.UpdateMeetingRequest updateRequest = new CreatingMeetingRequest.UpdateMeetingRequest();
        updateRequest.setTitle("수정 시도");

        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));

        // When & Then
        assertThatThrownBy(() -> meetingService.updateMeeting(1L, updateRequest, testUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);

        verify(meetingRepository).findById(1L);
        verify(meetingRepository, never()).save(any(Meeting.class));
    }

    @Test
    @DisplayName("모임 삭제 성공")
    void deleteMeeting_Success() {
        // Given
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.countApprovedParticipants(1L)).thenReturn(1L); // 주최자만
        when(meetingRepository.save(any(Meeting.class))).thenReturn(testMeeting);

        // When
        meetingService.deleteMeeting(1L, testUser);

        // Then
        verify(meetingRepository).findById(1L);
        verify(participantRepository).countApprovedParticipants(1L);
        verify(meetingRepository).save(any(Meeting.class));
    }

    @Test
    @DisplayName("모임 삭제 실패 - 참가자가 있는 모임")
    void deleteMeeting_HasParticipants() {
        // Given
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.countApprovedParticipants(1L)).thenReturn(5L); // 주최자 포함 5명

        // When & Then
        assertThatThrownBy(() -> meetingService.deleteMeeting(1L, testUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("참가자가 있는 모임은 삭제할 수 없습니다");

        verify(meetingRepository).findById(1L);
        verify(participantRepository).countApprovedParticipants(1L);
        verify(meetingRepository, never()).save(any(Meeting.class));
    }
}