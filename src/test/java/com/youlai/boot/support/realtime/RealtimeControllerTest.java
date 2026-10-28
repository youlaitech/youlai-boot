package com.youlai.boot.support.realtime;

import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.support.realtime.config.RealtimeProperties;
import com.youlai.boot.support.realtime.controller.RealtimeController;
import com.youlai.boot.support.realtime.model.CreateRealtimeTicketRequest;
import com.youlai.boot.support.realtime.model.PresenceSnapshot;
import com.youlai.boot.support.realtime.security.RealtimeTicketService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 关闭实时通信与临时故障的响应语义
 *
 * @since 4.13.0
 */
class RealtimeControllerTest {
    private final RealtimeTicketService tickets = mock(RealtimeTicketService.class);
    private final RealtimeSessionManager sessions = mock(RealtimeSessionManager.class);
    private final RealtimeProperties properties = new RealtimeProperties();
    private final RealtimeController controller = new RealtimeController(tickets, sessions, properties);
    private final CreateRealtimeTicketRequest request =
            new CreateRealtimeTicketRequest("client-instance-01", "web", Set.of("notice"));

    @Test
    void disabledFeatureReturnsDedicatedCodeWithoutIssuingTicket() {
        properties.setEnabled(false);
        var response = controller.createTicket(request, "Bearer token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().getCode()).isEqualTo("B0102");
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        verifyNoInteractions(tickets);
    }

    @Test
    void stoppingServerRemainsRetryable() {
        when(sessions.isStopping()).thenReturn(true);
        var response = controller.createTicket(request, "Bearer token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().getCode()).isNotEqualTo(ResultCode.REALTIME_DISABLED.getCode());
        verifyNoInteractions(tickets);
    }

    @Test
    void storageFailureRemainsRetryable() {
        when(tickets.create("token", request)).thenThrow(new DataAccessResourceFailureException("offline"));
        var response = controller.createTicket(request, "Bearer token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().getCode()).isNotEqualTo(ResultCode.REALTIME_DISABLED.getCode());
    }

    @Test
    void missingCredentialIsUnauthorized() {
        assertThat(controller.createTicket(request, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(tickets);
    }

    @Test
    void forbiddenTopicReturns403WithoutRetryableCode() {
        when(tickets.create("token", request)).thenThrow(new AccessDeniedException("forbidden"));
        var response = controller.createTicket(request, "Bearer token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getCode()).isEqualTo(ResultCode.ACCESS_UNAUTHORIZED.getCode());
    }

    @Test
    void ticketTopicsAreRequiredAndLimitedToSupportedTopics() {
        assertThatThrownBy(() -> new CreateRealtimeTicketRequest("client-instance-01", "web", Set.of("dict")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateRealtimeTicketRequest("client-instance-01", "web", Set.of("notice", "unknown")))
                .isInstanceOf(IllegalArgumentException.class);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(new CreateRealtimeTicketRequest("client-instance-01", "web", null)))
                    .isNotEmpty();
        }
    }

    @Test
    void disabledFeatureStillAllowsPresenceCalibration() {
        properties.setEnabled(false);
        var snapshot = new PresenceSnapshot(0, 0, "node-1", "2026-01-01T00:00:00Z");
        when(sessions.presence()).thenReturn(snapshot);

        var response = controller.getPresence();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).isEqualTo(snapshot);
    }
}
