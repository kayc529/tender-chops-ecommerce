package com.kaycheung.order_service.util;

import java.math.BigDecimal;

public class PriceFormatUtils {
    public static String formatPriceInCentsToDisplayString(Long priceInCents)
    {
        if(priceInCents==null)
        {
            return null;
        }
        BigDecimal price = BigDecimal.valueOf(priceInCents,2);
        return price.toPlainString();
    }
}
