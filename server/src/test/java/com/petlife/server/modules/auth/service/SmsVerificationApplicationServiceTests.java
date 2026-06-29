package com.petlife.server.modules.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.auth.converter.SmsVerificationConverter;
import com.petlife.server.modules.auth.dto.request.AuthSmsSendRequest;
import com.petlife.server.modules.auth.persistence.SmsVerificationPersistenceMapper;
import com.petlife.server.modules.auth.persistence.command.CreateSmsVerificationCodeCommand;
import com.petlife.server.modules.auth.service.sms.SmsProvider;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class SmsVerificationApplicationServiceTests {

    @Test
    void shouldRejectInvalidConfiguredTestLoginCodeBeforeCreatingVerificationRecord() {
        SmsVerificationPersistenceMapper mapper = mock(SmsVerificationPersistenceMapper.class);
        SmsProvider smsProvider = mock(SmsProvider.class);
        when(mapper.findLatestSendRecord(eq("13900001111"), eq("login"))).thenReturn(null);
        when(mapper.countSendRecordsByMobileAndSceneSince(
            eq("13900001111"),
            eq("login"),
            any(LocalDateTime.class)
        )).thenReturn(0);
        when(mapper.countSendRecordsByIpAndSceneSince(
            eq("127.0.0.1"),
            eq("login"),
            any(LocalDateTime.class)
        )).thenReturn(0);

        AuthSmsTestLoginProperties properties = new AuthSmsTestLoginProperties();
        properties.setEnabled(true);
        properties.setMobileWhitelist(List.of("13900001111"));
        properties.setCode("12");

        SmsVerificationApplicationService service = new SmsVerificationApplicationService(
            mapper,
            new SmsVerificationConverter(),
            smsProvider,
            properties
        );
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRemoteAddr("127.0.0.1");

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> service.sendSmsCode(new AuthSmsSendRequest("13900001111", "login"), servletRequest)
        );

        assertEquals(ResponseCode.BAD_REQUEST, exception.getResponseCode());
        verify(mapper, never()).expireActiveCodesByMobileAndScene(eq("13900001111"), eq("login"));
        verify(mapper, never()).insertVerificationCode(any(CreateSmsVerificationCodeCommand.class));
    }
}
