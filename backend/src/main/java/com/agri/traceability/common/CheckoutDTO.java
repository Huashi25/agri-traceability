package com.agri.traceability.common;

import lombok.Data;
import java.util.List;

/**
 * 购物车结算DTO
 */
@Data
public class CheckoutDTO {
    private Long userId;
    private List<Long> cartIds;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
}
