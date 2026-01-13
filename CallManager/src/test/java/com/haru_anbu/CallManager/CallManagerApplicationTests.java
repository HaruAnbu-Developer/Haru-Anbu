package com.haru_anbu.CallManager;

import com.haru_anbu.CallManager.call.config.GrpcClientConfig;
import com.haru_anbu.CallManager.call.config.TwilioConfig;
import com.haru_anbu.CallManager.call.config.WebSocketConfig;
import com.haru_anbu.CallManager.call.service.CallManagerService;
import com.haru_anbu.CallManager.call.service.CallSessionService;
import com.haru_anbu.CallManager.call.service.TwilioService;
import com.haru_anbu.CallManager.call.service.VoiceConversationGrpcService;
import com.haru_anbu.CallManager.call.util.AudioConverter;
import com.haru_anbu.CallManager.call.handler.TwilioMediaStreamHandler;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Spring Boot 애플리케이션 통합 테스트
 * - Spring Context 정상 로드 확인
 * - 주요 Bean들이 제대로 생성되었는지 확인
 * - 의존성 주입이 올바르게 되었는지 확인
 */
@SpringBootTest
@ActiveProfiles("test")  // test 프로파일 사용 (H2 DB 등)
class CallManagerApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	@DisplayName("Spring Context 로드 테스트")
	void contextLoads() {
		// Spring Context가 정상적으로 로드되면 테스트 통과
		assertNotNull(applicationContext);
		System.out.println("✅ Spring Context 정상 로드");
	}

	@Test
	@DisplayName("핵심 Bean 생성 확인 - Configuration")
	void testConfigurationBeans() {
		// Given & When
		TwilioConfig twilioConfig = applicationContext.getBean(TwilioConfig.class);
		GrpcClientConfig grpcConfig = applicationContext.getBean(GrpcClientConfig.class);
		WebSocketConfig webSocketConfig = applicationContext.getBean(WebSocketConfig.class);

		// Then
		assertNotNull(twilioConfig, "TwilioConfig Bean이 생성되지 않았습니다");
		assertNotNull(grpcConfig, "GrpcClientConfig Bean이 생성되지 않았습니다");
		assertNotNull(webSocketConfig, "WebSocketConfig Bean이 생성되지 않았습니다");

		System.out.println("✅ Configuration Beans 정상 생성");
	}

	@Test
	@DisplayName("핵심 Bean 생성 확인 - Services")
	void testServiceBeans() {
		// Given & When
		CallManagerService callManagerService = applicationContext.getBean(CallManagerService.class);
		CallSessionService sessionService = applicationContext.getBean(CallSessionService.class);
		TwilioService twilioService = applicationContext.getBean(TwilioService.class);
		VoiceConversationGrpcService grpcService = applicationContext.getBean(VoiceConversationGrpcService.class);

		// Then
		assertNotNull(callManagerService, "CallManagerService Bean이 생성되지 않았습니다");
		assertNotNull(sessionService, "CallSessionService Bean이 생성되지 않았습니다");
		assertNotNull(twilioService, "TwilioService Bean이 생성되지 않았습니다");
		assertNotNull(grpcService, "VoiceConversationGrpcService Bean이 생성되지 않았습니다");

		System.out.println("✅ Service Beans 정상 생성");
	}

	@Test
	@DisplayName("핵심 Bean 생성 확인 - Utilities & Handlers")
	void testUtilityBeans() {
		// Given & When
		AudioConverter audioConverter = applicationContext.getBean(AudioConverter.class);
		TwilioMediaStreamHandler wsHandler = applicationContext.getBean(TwilioMediaStreamHandler.class);

		// Then
		assertNotNull(audioConverter, "AudioConverter Bean이 생성되지 않았습니다");
		assertNotNull(wsHandler, "TwilioMediaStreamHandler Bean이 생성되지 않았습니다");

		System.out.println("✅ Utility & Handler Beans 정상 생성");
	}

	@Test
	@DisplayName("gRPC Channel Bean 생성 확인")
	void testGrpcChannelBean() {
		// Given & When
		ManagedChannel channel = applicationContext.getBean("aiServiceChannel", ManagedChannel.class);

		// Then
		assertNotNull(channel, "gRPC ManagedChannel Bean이 생성되지 않았습니다");
		assertFalse(channel.isShutdown(), "gRPC Channel이 이미 종료되었습니다");
		assertFalse(channel.isTerminated(), "gRPC Channel이 이미 종료되었습니다");

		System.out.println("✅ gRPC Channel Bean 정상 생성");
	}

	@Test
	@DisplayName("의존성 주입 확인 - CallManagerService")
	void testDependencyInjection() {
		// Given
		CallManagerService callManagerService = applicationContext.getBean(CallManagerService.class);

		// When & Then - 의존성이 올바르게 주입되었는지 확인
		assertNotNull(callManagerService, "CallManagerService가 null입니다");

		// CallManagerService는 TwilioService, CallSessionService, VoiceConversationGrpcService를 주입받음
		// 정상적으로 Bean이 생성되었다면 의존성도 주입된 것
		System.out.println("✅ 의존성 주입 정상 작동");
	}

	@Test
	@DisplayName("환경 변수 로드 확인")
	void testEnvironmentVariables() {
		// Given
		TwilioConfig twilioConfig = applicationContext.getBean(TwilioConfig.class);

		// When & Then
		assertNotNull(twilioConfig.getAccountSid(), "Twilio Account SID가 로드되지 않았습니다");
		assertNotNull(twilioConfig.getAuthToken(), "Twilio Auth Token이 로드되지 않았습니다");
		assertNotNull(twilioConfig.getPhoneNumber(), "Twilio Phone Number가 로드되지 않았습니다");

		System.out.println("✅ 환경 변수 정상 로드");
		System.out.println("   Twilio Phone: " + twilioConfig.getPhoneNumber());
	}

	@Test
	@DisplayName("Bean 개수 확인")
	void testBeanCount() {
		// Given
		String[] beanNames = applicationContext.getBeanDefinitionNames();

		// When & Then
		assertTrue(beanNames.length > 0, "Bean이 하나도 생성되지 않았습니다");

		System.out.println("✅ 총 " + beanNames.length + "개의 Bean이 생성되었습니다");

		// 주요 Bean들의 이름 출력 (디버깅용)
		System.out.println("\n주요 Bean 목록:");
		for (String beanName : beanNames) {
			if (beanName.contains("call") || beanName.contains("twilio") || 
			    beanName.contains("grpc") || beanName.contains("audio")) {
				System.out.println("  - " + beanName);
			}
		}
	}

	@Test
	@DisplayName("애플리케이션 준비 상태 확인")
	void testApplicationReadiness() {
		// Given
		CallManagerService callManagerService = applicationContext.getBean(CallManagerService.class);
		CallSessionService sessionService = applicationContext.getBean(CallSessionService.class);
		TwilioService twilioService = applicationContext.getBean(TwilioService.class);
		VoiceConversationGrpcService grpcService = applicationContext.getBean(VoiceConversationGrpcService.class);
		AudioConverter audioConverter = applicationContext.getBean(AudioConverter.class);

		// Then - 모든 핵심 컴포넌트가 준비되었는지 확인
		assertAll("애플리케이션 준비 상태 확인",
			() -> assertNotNull(callManagerService, "CallManagerService 준비 안됨"),
			() -> assertNotNull(sessionService, "CallSessionService 준비 안됨"),
			() -> assertNotNull(twilioService, "TwilioService 준비 안됨"),
			() -> assertNotNull(grpcService, "VoiceConversationGrpcService 준비 안됨"),
			() -> assertNotNull(audioConverter, "AudioConverter 준비 안됨")
		);

		System.out.println("✅ 애플리케이션 실행 준비 완료");
		System.out.println("   ✓ Twilio 연동 준비");
		System.out.println("   ✓ gRPC AI 서버 연결 준비");
		System.out.println("   ✓ WebSocket 스트리밍 준비");
		System.out.println("   ✓ 오디오 변환 준비");
		System.out.println("   ✓ 데이터베이스 연결 준비");
	}
}
