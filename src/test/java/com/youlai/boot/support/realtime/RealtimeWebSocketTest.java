package com.youlai.boot.support.realtime;

import com.youlai.boot.support.realtime.config.RealtimeConfig;
import com.youlai.boot.support.realtime.model.CreateRealtimeTicketRequest;
import com.youlai.boot.support.realtime.protocol.RealtimeMessageCodec;
import com.youlai.boot.support.realtime.protocol.RealtimeServerMessage;
import com.youlai.boot.support.realtime.security.RealtimeCredentialVerifier;
import com.youlai.boot.support.realtime.security.RealtimeCredentialVerifier.VerifiedIdentity;
import com.youlai.boot.support.realtime.security.RealtimeHandshakeHandler;
import com.youlai.boot.support.realtime.security.RealtimeHandshakeInterceptor;
import com.youlai.boot.support.realtime.security.RealtimeTicketService;
import org.apache.tomcat.websocket.server.WsSci;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 本地 Tomcat 上的票据授权、消息投递与心跳闭环，不连接外部存储
 *
 * @since 4.13.0
 */
class RealtimeWebSocketTest {
    private AnnotationConfigServletWebServerApplicationContext context;
    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final Instant expiresAt = Instant.now().plusSeconds(3_600).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);

    @BeforeEach
    void startLocalServer() {
        context = new AnnotationConfigServletWebServerApplicationContext(TestConfiguration.class);
        when(context.getBean(RealtimeCredentialVerifier.class).verify("access-token"))
                .thenReturn(new VerifiedIdentity(1L, true, expiresAt));
    }

    @AfterEach
    void stopLocalServer() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void connectsWithTicketTopicsAndAnswersHeartbeat() throws Exception {
        String ticket = createTicket();
        var listener = new Messages();
        WebSocket socket = connect(ticket, listener).get(5, TimeUnit.SECONDS);
        try {
            JsonNode ready = mapper.readTree(listener.next());
            assertThat(ready.path("type").asText()).isEqualTo("connection.ready");
            assertThat(ready.path("payload")).isEqualTo(mapper.readTree("{\"heartbeatTimeoutMs\":60000}"));
            assertThat(ready.size()).isEqualTo(4);
            verify(context.getBean(RealtimeCredentialVerifier.class), times(2)).verify("access-token");

            var manager = context.getBean(RealtimeSessionManager.class);
            manager.broadcast("dict", RealtimeServerMessage.create("dict.changed", Map.of("dictCode", "gender")));
            assertThat(mapper.readTree(listener.next()).path("type").asText()).isEqualTo("dict.changed");
            RealtimeSession state = currentSession(manager);
            synchronized (state) {
                state.nextPingAt = 0;
            }
            manager.maintain();
            JsonNode ping = mapper.readTree(listener.next());
            assertThat(ping.path("type").asText()).isEqualTo("heartbeat.ping");
            socket.sendText("{\"version\":1,\"type\":\"heartbeat.pong\",\"payload\":{\"pingId\":\""
                    + ping.path("id").asText() + "\"}}", true).get(5, TimeUnit.SECONDS);
            await().atMost(Duration.ofSeconds(5)).until(() -> {
                synchronized (state) {
                    return state.pendingPingId == null;
                }
            });

            assertThatThrownBy(() -> connect(ticket, new Messages()).join())
                    .hasCauseInstanceOf(WebSocketHandshakeException.class)
                    .satisfies(failure -> assertThat(((WebSocketHandshakeException) failure.getCause())
                            .getResponse().statusCode()).isEqualTo(401));
            assertThat(manager.presence().onlineConnections()).isEqualTo(1);
        } finally {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(5, TimeUnit.SECONDS);
        }
        await().atMost(Duration.ofSeconds(5))
                .until(() -> context.getBean(RealtimeSessionManager.class).presence().onlineConnections() == 0);
    }

    @Test
    void revokedCredentialRejectsHandshakeAndReleasesReservation() {
        String ticket = createTicket();
        when(context.getBean(RealtimeCredentialVerifier.class).verify("access-token"))
                .thenThrow(new BadCredentialsException("revoked"));

        assertThatThrownBy(() -> connect(ticket, new Messages()).join())
                .hasCauseInstanceOf(WebSocketHandshakeException.class)
                .satisfies(failure -> assertThat(((WebSocketHandshakeException) failure.getCause())
                        .getResponse().statusCode()).isEqualTo(401));
        var manager = context.getBean(RealtimeSessionManager.class);
        for (int index = 0; index < 500; index++) {
            assertThat(manager.reserve()).isNotNull();
        }
        assertThat(manager.reserve()).isNull();
    }

    @Test
    void ticketCreationRejectsUnauthorizedPresenceTopic() {
        when(context.getBean(RealtimeCredentialVerifier.class).verify("access-token"))
                .thenReturn(new VerifiedIdentity(1L, false, expiresAt));

        assertThatThrownBy(() -> context.getBean(RealtimeTicketService.class).create("access-token",
                new CreateRealtimeTicketRequest("client-instance-01", "web", Set.of("notice", "presence"))))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void handshakeRechecksTopicPermissionAndConsumesRejectedTicket() {
        String ticket = context.getBean(RealtimeTicketService.class).create("access-token",
                new CreateRealtimeTicketRequest("client-instance-01", "web", Set.of("notice", "presence"))).ticket();
        when(context.getBean(RealtimeCredentialVerifier.class).verify("access-token"))
                .thenReturn(new VerifiedIdentity(1L, false, expiresAt));

        assertThatThrownBy(() -> connect(ticket, new Messages()).join())
                .hasCauseInstanceOf(WebSocketHandshakeException.class)
                .satisfies(failure -> assertThat(((WebSocketHandshakeException) failure.getCause())
                        .getResponse().statusCode()).isEqualTo(403));
        assertThatThrownBy(() -> connect(ticket, new Messages()).join())
                .hasCauseInstanceOf(WebSocketHandshakeException.class)
                .satisfies(failure -> assertThat(((WebSocketHandshakeException) failure.getCause())
                        .getResponse().statusCode()).isEqualTo(401));
    }

    /**
     * 模拟已登录客户端领取一次性票据
     */
    private String createTicket() {
        return context.getBean(RealtimeTicketService.class)
                .create("access-token", new CreateRealtimeTicketRequest("client-instance-01", "web", Set.of("notice", "dict")))
                .ticket();
    }

    /**
     * 使用真实 HTTP Upgrade 建立本地连接
     */
    private java.util.concurrent.CompletableFuture<WebSocket> connect(String ticket, Messages listener) {
        int port = context.getWebServer().getPort();
        return http.newWebSocketBuilder().header("Origin", "http://localhost:" + port)
                .connectTimeout(Duration.ofSeconds(5))
                .buildAsync(URI.create("ws://localhost:" + port + "/api/v1/realtime/ws?ticket=" + ticket), listener);
    }

    /**
     * 读取连接以推进心跳时钟，避免测试等待 25 秒
     */
    @SuppressWarnings("unchecked")
    private RealtimeSession currentSession(RealtimeSessionManager manager) {
        return ((Map<String, RealtimeSession>) ReflectionTestUtils.getField(manager, "transports"))
                .values().iterator().next();
    }

    /**
     * 只注册实时通信与本地 Servlet 容器
     */
    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({RealtimeConfig.class, RealtimeWebSocketHandler.class, RealtimeSessionManager.class,
            RealtimeMessageSender.class, RealtimeMessageCodec.class, RealtimeHandshakeHandler.class,
            RealtimeHandshakeInterceptor.class, RealtimeTicketService.class})
    static class TestConfiguration {
        /**
         * 使用随机本地端口启动带 WebSocket 支持的 Tomcat
         */
        @Bean
        TomcatServletWebServerFactory webServerFactory() {
            var factory = new TomcatServletWebServerFactory(0);
            factory.addContextCustomizers(servletContext ->
                    servletContext.addServletContainerInitializer(new WsSci(), Set.of()));
            return factory;
        }

        /**
         * 将请求交给测试上下文中的 Spring MVC
         */
        @Bean
        ServletRegistrationBean<DispatcherServlet> dispatcherServlet(WebApplicationContext applicationContext) {
            return new ServletRegistrationBean<>(new DispatcherServlet(applicationContext), "/");
        }

        /**
         * 提供可控制登录态的身份校验替身
         */
        @Bean
        RealtimeCredentialVerifier verifier() {
            return mock(RealtimeCredentialVerifier.class);
        }

        /**
         * 用内存 Map 模拟票据写入与原子消费，校验仍走真实 TicketService
         */
        @Bean
        @SuppressWarnings("unchecked")
        StringRedisTemplate redis() {
            var redis = mock(StringRedisTemplate.class);
            ValueOperations<String, String> values = mock(ValueOperations.class);
            Map<String, String> tickets = new ConcurrentHashMap<>();
            when(redis.opsForValue()).thenReturn(values);
            doAnswer(call -> {
                tickets.put(call.getArgument(0), call.getArgument(1));
                return null;
            }).when(values).set(anyString(), anyString(), any(Duration.class));
            when(redis.execute(any(RedisScript.class), anyList()))
                    .thenAnswer(call -> tickets.remove(((java.util.List<String>) call.getArgument(1)).get(0)));
            return redis;
        }
    }

    /**
     * 收集完整文本帧，保留客户端流量控制
     */
    private static class Messages implements WebSocket.Listener {
        private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        private final StringBuilder partial = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
            partial.append(data);
            if (last) {
                messages.add(partial.toString());
                partial.setLength(0);
            }
            socket.request(1);
            return null;
        }

        /**
         * 等待一条完整消息，超时即失败
         */
        String next() throws InterruptedException {
            String message = messages.poll(5, TimeUnit.SECONDS);
            assertThat(message).as("服务器应在超时前返回消息").isNotNull();
            return message;
        }
    }
}
