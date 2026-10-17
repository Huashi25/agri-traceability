package com.agri.traceability.common;

import com.agri.traceability.entity.OrderInfo;
import com.agri.traceability.entity.OrderItem;
import lombok.Data;
import java.util.List;

@Data
public class OrderCreateDTO {
    private OrderInfo order;
    private List<OrderItem> items;
}
