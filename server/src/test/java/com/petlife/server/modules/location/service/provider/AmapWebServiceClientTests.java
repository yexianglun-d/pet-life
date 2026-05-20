package com.petlife.server.modules.location.service.provider;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.location.domain.entity.AmapReverseGeocodeEntity;
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

    @Test
    void shouldParseReverseGeocodeResponseWithArrayCityWithoutCallingRealAmap() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://unit.test.amap");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        RestClient restClient = builder.build();
        AmapWebServiceClient client = new AmapWebServiceClient(configuredProperties(), restClient);

        server.expect(once(), requestTo(containsString("/v3/geocode/regeo")))
            .andRespond(withSuccess("""
                {
                  "status": "1",
                  "info": "OK",
                  "regeocode": {
                    "formatted_address": "上海市徐汇区宠物友好路88号",
                    "addressComponent": {
                      "country": "中国",
                      "province": "上海市",
                      "city": [],
                      "district": "徐汇区",
                      "township": "湖南路街道",
                      "citycode": "021",
                      "adcode": "310104"
                    }
                  }
                }
                """, MediaType.APPLICATION_JSON));

        Optional<AmapReverseGeocodeEntity> reverseGeocode = client.reverseGeocode(
            new GeoPointEntity(new BigDecimal("31.188000"), new BigDecimal("121.433000"))
        );

        assertTrue(reverseGeocode.isPresent());
        assertEquals("上海市徐汇区宠物友好路88号", reverseGeocode.get().formattedAddress());
        assertEquals(null, reverseGeocode.get().city());
        assertEquals("徐汇区", reverseGeocode.get().district());
        server.verify();
    }

    @Test
    void shouldReturnEmptyWhenAmapLocationIsInvalid() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://unit.test.amap");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        RestClient restClient = builder.build();
        AmapWebServiceClient client = new AmapWebServiceClient(configuredProperties(), restClient);

        server.expect(once(), requestTo(containsString("/v3/geocode/geo")))
            .andRespond(withSuccess("""
                {
                  "status": "1",
                  "info": "OK",
                  "geocodes": [
                    {
                      "formatted_address": "无效坐标样例",
                      "location": "not-a-coordinate"
                    }
                  ]
                }
                """, MediaType.APPLICATION_JSON));

        Optional<AmapGeocodeEntity> geocode = client.geocode("无效坐标样例", null);

        assertTrue(geocode.isEmpty());
        server.verify();
    }

    @Test
    void shouldExposeAmapErrorInfoCodeAsBusinessException() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://unit.test.amap");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        RestClient restClient = builder.build();
        AmapWebServiceClient client = new AmapWebServiceClient(configuredProperties(), restClient);

        server.expect(once(), requestTo(containsString("/v3/geocode/geo")))
            .andRespond(withSuccess("""
                {
                  "status": "0",
                  "info": "INVALID_USER_KEY",
                  "infocode": "10001"
                }
                """, MediaType.APPLICATION_JSON));

        BusinessException exception = assertThrows(BusinessException.class, () -> client.geocode("上海市徐汇区", null));

        assertEquals(ResponseCode.MAP_PROVIDER_REQUEST_FAILED, exception.getResponseCode());
        assertTrue(exception.getMessage().contains("INVALID_USER_KEY"));
        assertTrue(exception.getMessage().contains("10001"));
        server.verify();
    }

    private AmapWebServiceProperties configuredProperties() {
        AmapWebServiceProperties properties = new AmapWebServiceProperties();
        properties.setBaseUrl("https://unit.test.amap");
        properties.setKey("configured-for-unit-test");
        return properties;
    }
}
