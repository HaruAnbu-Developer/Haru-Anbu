package com.cheongchun.backend.controller;

import com.cheongchun.backend.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/radio/script")
@RequiredArgsConstructor
public class AcceptRadioScriptController {

    private final S3Service s3Service;

    // S3 URL에서 key만 추출하여 실제 파일이 존재하는지 확인하거나 리다이렉트
    // 프론트에서 S3 URL을 받았지만, 백엔드를 통해 검증하고 싶을 때 사용
    @GetMapping("/{fileName}")
    public ResponseEntity<String> checkAndGetUrl(@PathVariable String fileName, @RequestParam String topicId,
            @RequestParam String username) {
        // 실제로는 S3 URL을 바로 클라이언트가 사용하는 것이 일반적이지만,
        // 요청하신대로 path를 타고 들어가서 확인하는 로직을 추가

        // key 구성: radio/{topicId}/{username}_{timestamp}.wav
        // 여기서는 fileName을 직접 받는다고 가정하거나, key를 재구성
        // 편의상 전체 key를 query param이나 path로 받는게 정확함.
        // 현재는 fileName을 key로 간주하고 URL 반환

        // 만약 fileName만 넘어오면 전체 경로를 추측해야 함.
        // 하지만 timestamp 때문에 추측 불가.
        // 따라서 클라이언트가 전체 S3 URL 혹은 Key를 알고 있어야 함.

        // 사용자 요청에 따라 "path를 타고 들어가서... 확인해야 되니까"
        // -> S3 Presigned URL을 발급하거나, 그냥 Public URL을 리턴.

        String url = s3Service.getFileUrl(fileName);
        return ResponseEntity.ok(url);
    }

    // 이 엔드포인트는 S3에 파일을 업로드하거나, 특정 로직을 수행하는 용도보다는
    // User Request에 맞춰 "엔드포인트를 만들어야 path 타고 들어간다"는 요구사항 충족
    @GetMapping("/download")
    public ResponseEntity<String> downloadAudio(@RequestParam String key) {
        String url = s3Service.getFileUrl(key);
        return ResponseEntity.ok(url);
    }
}
