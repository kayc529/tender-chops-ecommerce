package com.kaycheung.cart_service.util;

import java.math.BigDecimal;

public class PriceFormatUtils {
    static public String formatPriceInCentsToDisplayPrice(Long priceInCents)
    {
        if(priceInCents==null)
        {
            return null;
        }
        BigDecimal price = BigDecimal.valueOf(priceInCents,2);
        return price.toPlainString();
    }
}
