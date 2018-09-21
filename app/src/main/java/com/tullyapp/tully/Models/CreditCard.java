package com.tullyapp.tully.Models;

import java.io.Serializable;

/**
 * Created by kathan on 22/02/18.
 */

public class CreditCard implements Serializable{

    public String cardNumber;
    public int expireMonth;
    public int year;
    public String cvc;

    public CreditCard(String cardNumber, int expireMonth, int year, String cvc) {
        this.cardNumber = cardNumber;
        this.expireMonth = expireMonth;
        this.year = year;
        this.cvc = cvc;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public int getExpireMonth() {
        return expireMonth;
    }

    public int getYear() {
        return year;
    }

    public String getCvc() {
        return cvc;
    }
}
