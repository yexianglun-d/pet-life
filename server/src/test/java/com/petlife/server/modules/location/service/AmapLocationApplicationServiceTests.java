package com.petlife.server.modules.location.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.petlife.server.modules.location.converter.AmapLocationConverter;
import com.petlife.server.modules.location.dto.response.AmapConfigStatusResponse;
import org.junit.jupiter.api.Test;

class AmapLocationApplicationServiceTests {

    @Test
    void shouldReturnClearConfigStatusWhenAmapKeyIsMissing() {
        AmapWebServiceProperties properties = new AmapWebServiceProperties();
        AmapLocationApplicationService service = new AmapLocationApplicationService(
            properties,
            null,
            new AmapLocationConverter()
        );

        AmapConfigStatusResponse response = service.getConfigStatus();

        assertEquals("amap", response.providerCode());
        assertEquals("PETLIFE_AMAP_WEB_SERVICE_KEY", response.requiredConfigKey());
        assertFalse(response.configured());
        assertTrue(response.message().contains("未配置"));
    }

    @Test
    void shouldReturnClearConfigStatusWhenAmapKeyIsConfigured() {
        AmapWebServiceProperties properties = new AmapWebServiceProperties();
        properties.setKey("configured-for-unit-test");
        AmapLocationApplicationService service = new AmapLocationApplicationService(
            properties,
            null,
            new AmapLocationConverter()
        );

        AmapConfigStatusResponse response = service.getConfigStatus();

        assertEquals("PETLIFE_AMAP_WEB_SERVICE_KEY", response.requiredConfigKey());
        assertTrue(response.configured());
        assertTrue(response.capabilities().contains("geocode"));
        assertTrue(response.message().contains("已就绪"));
    }
}
