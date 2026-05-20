package com.petlife.server.modules.location.service.provider;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.petlife.server.modules.location.domain.entity.AmapGeocodeEntity;
import com.petlife.server.modules.location.domain.entity.GeoPointEntity;
import com.petlife.server.modules.location.service.AmapWebServiceProperties;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AmapWebServiceClientTests {

    @Test
    void shouldParseGeocodeResponseWithoutCallingRealAmap() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://unit.test.amap");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        RestClient restClient = builder.build();
        AmapWebServiceProperties properties = new AmapWebServiceProperties();
        properties.setBaseUrl("https://unit.test.amap");
        properties.setKey("configured-for-unit-test");
        AmapWebServiceClient client = new AmapWebServiceClient(properties, restClient);

        server.expect(once(), requestTo(containsString("/v3/geocode/geo")))
            .andRespond(withSuccess("""
                {
                  "status": "1",
                  "info": "OK",
                  "geocodes": [
                    {
                      "formatted_address": "上海市徐汇区宠物友好路88号",
                      "country": "中国",
                      "province": "上海市",
                      "city": "上海市",
                      "district": "徐汇区",
                      "citycode": "021",
                      "adcode": "310104",
                      "location": "121.433000,31.188000",
                      "level": "门牌号"
                    }
                  ]
                }
                """, MediaType.APPLICATION_JSON));

        Optional<AmapGeocodeEntity> geocode = client.geocode("上海市徐汇区宠物友好路88号", "310000");

        assertTrue(geocode.isPresent());
        GeoPointEntity point = geocode.get().point();
        assertEquals(new BigDecimal("31.188000"), point.latitude());
        assertEquals(new BigDecimal("121.433000"), point.longitude());
        server.verify();
    }
}
