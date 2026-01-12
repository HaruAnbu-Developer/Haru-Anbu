package com.cheongchun.backend.exception;

public class MeetingNotFoundException extends BusinessException {
    public MeetingNotFoundException() {
        super(ErrorCode.MEETING_NOT_FOUND);
    }

    public MeetingNotFoundException(String message) {
        super(ErrorCode.MEETING_NOT_FOUND, message);
    }
}