package com.agri.traceability.common;

import com.agri.traceability.entity.Cart;
import com.agri.traceability.entity.Product;
import lombok.Data;

@Data
public class CartVO {
    private Cart cart;
    private Product product;
}
