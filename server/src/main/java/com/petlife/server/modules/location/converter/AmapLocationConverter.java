package com.petlife.server.modules.location.converter;

import com.petlife.server.modules.location.domain.entity.AmapConfigStatusEntity;
import com.petlife.server.modules.location.domain.entity.AmapGeocodeEntity;
import com.petlife.server.modules.location.domain.entity.AmapReverseGeocodeEntity;
import com.petlife.server.modules.location.dto.response.AmapConfigStatusResponse;
import com.petlife.server.modules.location.dto.response.AmapGeocodeResponse;
import com.petlife.server.modules.location.dto.response.AmapReverseGeocodeResponse;
import org.springframework.stereotype.Component;

/**
 * 高德地图能力响应转换器。
 */
@Component
public class AmapLocationConverter {

    public AmapConfigStatusResponse toConfigStatusResponse(AmapConfigStatusEntity entity) {
        return new AmapConfigStatusResponse(
            entity.providerCode(),
            entity.configured(),
            entity.baseUrl(),
            entity.capabilities(),
            entity.message()
        );
    }

    public AmapGeocodeResponse toGeocodeResponse(String requestedAddress, AmapGeocodeEntity entity) {
        if (entity == null) {
            return new AmapGeocodeResponse(
                false,
                requestedAddress,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            );
        }
        return new AmapGeocodeResponse(
            true,
            requestedAddress,
            entity.formattedAddress(),
            entity.country(),
            entity.province(),
            entity.city(),
            entity.district(),
            entity.cityCode(),
            entity.adcode(),
            entity.point().latitude(),
            entity.point().longitude(),
            entity.level()
        );
    }

    public AmapReverseGeocodeResponse toReverseGeocodeResponse(AmapReverseGeocodeEntity entity) {
        if (entity == null) {
            return new AmapReverseGeocodeResponse(
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            );
        }
        return new AmapReverseGeocodeResponse(
            true,
            entity.formattedAddress(),
            entity.country(),
            entity.province(),
            entity.city(),
            entity.district(),
            entity.township(),
            entity.cityCode(),
            entity.adcode(),
            entity.point().latitude(),
            entity.point().longitude()
        );
    }
}
