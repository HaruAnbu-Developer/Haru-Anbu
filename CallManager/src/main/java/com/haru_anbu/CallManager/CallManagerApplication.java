package com.haru_anbu.CallManager;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CallManagerApplication {

    public static void main(String[] args) {
        // 1. .env 파일 로드
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing() 
                .load();

		System.out.println("DEBUG: DATABASE_URL from .env => " + dotenv.get("DATABASE_URL"));
        
		// 2. entries()를 사용하여 로드된 변수들을 System Property로 등록
        // DotenvEntry 객체는 getKey()와 getValue()를 제공합니다.
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });

        // 3. 스프링 애플리케이션 실행
        SpringApplication.run(CallManagerApplication.class, args);
    }
}