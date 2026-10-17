package com.agri.traceability.common;

import com.agri.traceability.entity.*;
import lombok.Data;

@Data
public class TraceQueryVO {
    private TraceBatch batch;
    private Product product;
    private OriginInfo origin;
    private QualityInspection inspection;
    private LogisticsInfo logistics;
}
