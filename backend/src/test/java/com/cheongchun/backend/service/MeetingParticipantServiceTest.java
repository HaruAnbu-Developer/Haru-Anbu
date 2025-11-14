package com.cheongchun.backend.service;

import com.cheongchun.backend.dto.MeetingParticipantDto;
import com.cheongchun.backend.entity.Meeting;
import com.cheongchun.backend.entity.MeetingParticipant;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.exception.BusinessException;
import com.cheongchun.backend.exception.ErrorCode;
import com.cheongchun.backend.repository.MeetingParticipantRepository;
import com.cheongchun.backend.repository.MeetingRepository;
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
@DisplayName("MeetingParticipantService 단위 테스트")
class MeetingParticipantServiceTest {

    @Mock
    private MeetingParticipantRepository participantRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @InjectMocks
    private MeetingParticipantService participantService;

    private User organizer;
    private User participant;
    private Meeting testMeeting;
    private MeetingParticipant testParticipant;
    private MeetingParticipantDto.JoinRequest joinRequest;

    @BeforeEach
    void setUp() {
        organizer = new User();
        organizer.setId(1L);
        organizer.setUsername("organizer");
        organizer.setName("주최자");

        participant = new User();
        participant.setId(2L);
        participant.setUsername("participant");
        participant.setName("참가자");

        testMeeting = new Meeting();
        testMeeting.setId(1L);
        testMeeting.setTitle("테스트 모임");
        testMeeting.setStartDate(LocalDateTime.now().plusDays(3));
        testMeeting.setEndDate(LocalDateTime.now().plusDays(3).plusHours(2));
        testMeeting.setMaxParticipants(10);
        testMeeting.setCurrentParticipants(1);
        testMeeting.setCreatedBy(organizer);
        testMeeting.setStatus(Meeting.Status.RECRUITING);

        testParticipant = new MeetingParticipant();
        testParticipant.setId(1L);
        testParticipant.setMeeting(testMeeting);
        testParticipant.setUser(participant);
        testParticipant.setStatus(MeetingParticipant.Status.PENDING);
        testParticipant.setApplicationMessage("참여하고 싶습니다");

        joinRequest = new MeetingParticipantDto.JoinRequest();
        joinRequest.setApplicationMessage("참여하고 싶습니다");
    }

    @Test
    @DisplayName("모임 참여 신청 성공 - 수동 승인 대기")
    void joinMeeting_Success_ManualApproval() {
        // Given
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.existsByMeetingAndUser(testMeeting, participant)).thenReturn(false);
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(testParticipant);

        // When
        MeetingParticipantDto.ParticipantResponse response =
                participantService.joinMeeting(1L, joinRequest, participant);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(MeetingParticipant.Status.PENDING);

        verify(meetingRepository).findById(1L);
        verify(participantRepository).existsByMeetingAndUser(testMeeting, participant);
        verify(participantRepository).save(any(MeetingParticipant.class));
    }

    @Test
    @DisplayName("모임 참여 신청 실패 - 모임을 찾을 수 없음")
    void joinMeeting_MeetingNotFound() {
        // Given
        when(meetingRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> participantService.joinMeeting(999L, joinRequest, participant))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEETING_NOT_FOUND);

        verify(meetingRepository).findById(999L);
        verify(participantRepository, never()).save(any(MeetingParticipant.class));
    }

    @Test
    @DisplayName("모임 참여 신청 실패 - 이미 신청한 모임")
    void joinMeeting_AlreadyApplied() {
        // Given
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.existsByMeetingAndUser(testMeeting, participant)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> participantService.joinMeeting(1L, joinRequest, participant))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ALREADY_JOINED_MEETING);

        verify(participantRepository).existsByMeetingAndUser(testMeeting, participant);
        verify(participantRepository, never()).save(any(MeetingParticipant.class));
    }

    @Test
    @DisplayName("모임 참여 신청 실패 - 모집 마감된 모임")
    void joinMeeting_ClosedMeeting() {
        // Given
        testMeeting.setStatus(Meeting.Status.CLOSED);
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));

        // When & Then
        assertThatThrownBy(() -> participantService.joinMeeting(1L, joinRequest, participant))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEETING_ALREADY_CLOSED);

        verify(meetingRepository).findById(1L);
        verify(participantRepository, never()).save(any(MeetingParticipant.class));
    }

    @Test
    @DisplayName("모임 참여 신청 실패 - 모임 시작 1시간 전")
    void joinMeeting_TooCloseToStartTime() {
        // Given
        testMeeting.setStartDate(LocalDateTime.now().plusMinutes(30)); // 30분 후
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));

        // When & Then
        assertThatThrownBy(() -> participantService.joinMeeting(1L, joinRequest, participant))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEETING_START_SOON);

        verify(meetingRepository).findById(1L);
        verify(participantRepository, never()).save(any(MeetingParticipant.class));
    }

    @Test
    @DisplayName("모임 참여 신청 실패 - 정원 초과")
    void joinMeeting_MeetingFull() {
        // Given
        testMeeting.setCurrentParticipants(10); // 정원 가득 참
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));

        // When & Then
        assertThatThrownBy(() -> participantService.joinMeeting(1L, joinRequest, participant))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEETING_FULL);

        verify(meetingRepository).findById(1L);
        verify(participantRepository, never()).save(any(MeetingParticipant.class));
    }

    @Test
    @DisplayName("모임 참여 신청 실패 - 자신이 주최한 모임")
    void joinMeeting_OwnMeeting() {
        // Given
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));

        // When & Then
        assertThatThrownBy(() -> participantService.joinMeeting(1L, joinRequest, organizer))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CANNOT_JOIN_OWN_MEETING);

        verify(meetingRepository).findById(1L);
        verify(participantRepository, never()).save(any(MeetingParticipant.class));
    }

    @Test
    @DisplayName("참여 신청 승인 성공")
    void approveApplication_Success() {
        // Given
        when(participantRepository.findById(1L)).thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(testParticipant);
        doNothing().when(meetingRepository).updateParticipantCount(1L);

        // When
        MeetingParticipantDto.ParticipantResponse response =
                participantService.approveApplication(1L, organizer);

        // Then
        assertThat(response).isNotNull();
        verify(participantRepository).findById(1L);
        verify(participantRepository).save(any(MeetingParticipant.class));
        verify(meetingRepository).updateParticipantCount(1L);
    }

    @Test
    @DisplayName("참여 신청 승인 실패 - 권한 없음")
    void approveApplication_Unauthorized() {
        // Given
        User otherUser = new User();
        otherUser.setId(3L);
        otherUser.setRole(User.Role.USER);

        when(participantRepository.findById(1L)).thenReturn(Optional.of(testParticipant));

        // When & Then
        assertThatThrownBy(() -> participantService.approveApplication(1L, otherUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NO_PERMISSION_TO_MANAGE);

        verify(participantRepository).findById(1L);
        verify(participantRepository, never()).save(any(MeetingParticipant.class));
    }

    @Test
    @DisplayName("참여 신청 승인 실패 - 이미 처리된 신청")
    void approveApplication_AlreadyProcessed() {
        // Given
        testParticipant.setStatus(MeetingParticipant.Status.APPROVED);
        when(participantRepository.findById(1L)).thenReturn(Optional.of(testParticipant));

        // When & Then
        assertThatThrownBy(() -> participantService.approveApplication(1L, organizer))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARTICIPATION_STATUS);

        verify(participantRepository).findById(1L);
        verify(participantRepository, never()).save(any(MeetingParticipant.class));
    }

    @Test
    @DisplayName("참여 신청 거절 성공")
    void rejectApplication_Success() {
        // Given
        MeetingParticipantDto.RejectRequest rejectRequest = new MeetingParticipantDto.RejectRequest();
        rejectRequest.setReason("정원이 가득 찼습니다");

        when(participantRepository.findById(1L)).thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(testParticipant);

        // When
        MeetingParticipantDto.ParticipantResponse response =
                participantService.rejectApplication(1L, rejectRequest, organizer);

        // Then
        assertThat(response).isNotNull();
        verify(participantRepository).findById(1L);
        verify(participantRepository).save(any(MeetingParticipant.class));
    }

    @Test
    @DisplayName("참여 신청 취소 성공")
    void cancelApplication_Success() {
        // Given
        testParticipant.setStatus(MeetingParticipant.Status.PENDING);
        when(participantRepository.findByMeetingIdAndUserId(1L, 2L))
                .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(testParticipant);

        // When
        participantService.cancelApplication(1L, participant);

        // Then
        verify(participantRepository).findByMeetingIdAndUserId(1L, 2L);
        verify(participantRepository).save(any(MeetingParticipant.class));
    }

    @Test
    @DisplayName("참여 신청 취소 실패 - 신청 내역 없음")
    void cancelApplication_NotFound() {
        // Given
        when(participantRepository.findByMeetingIdAndUserId(1L, 2L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> participantService.cancelApplication(1L, participant))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARTICIPATION_NOT_FOUND);

        verify(participantRepository).findByMeetingIdAndUserId(1L, 2L);
        verify(participantRepository, never()).save(any(MeetingParticipant.class));
    }
}