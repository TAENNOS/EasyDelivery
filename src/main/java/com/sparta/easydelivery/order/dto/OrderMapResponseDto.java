package com.sparta.easydelivery.order.dto;

import com.sparta.easydelivery.order.entity.OrderStatusEnum;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;

@Getter
public class OrderMapResponseDto {

    Map<OrderStatusEnum, List<OrderResponseDto>> orderMap = new HashMap<>();

    public void setOrderToMap(OrderStatusEnum key, List<OrderResponseDto> responseDtoList){
        orderMap.put(key, responseDtoList);
    }
}
