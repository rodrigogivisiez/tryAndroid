package com.tullyapp.tully.Utils;

import java.math.BigDecimal;

/**
 * Created by macbookpro on 29/09/17.
 */

public class Percent {

    public static final BigDecimal DIVISOR_PERCENT = new BigDecimal(100);

    private int value;

    public Percent(int value) {
        if(value < 0 || value > 100){
            throw new IllegalArgumentException("Percentage value must be in <0;100> range");
        }
        this.value = value;
    }

    public void setPercent(int value){
        this.value = value;
    }

    public BigDecimal asBigDecimal() {
        return new BigDecimal(value).divide(DIVISOR_PERCENT);
    }
}