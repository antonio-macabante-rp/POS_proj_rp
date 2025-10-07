package com.am.register.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents payment information for a transaction.
 */
@Data
@NoArgsConstructor
public class Payment {

    private PaymentMethod method = PaymentMethod.NONE;
    private CardType cardType;
    private double amountTendered = 0.0;
    private double changeAmount = 0.0;
    private LocalDateTime paymentTime;
    private boolean completed = false;

    /**
     * Creates a cash payment.
     */
    public static Payment createCashPayment(double amountTendered, double changeAmount) {
        Payment payment = new Payment();
        payment.method = PaymentMethod.CASH;
        payment.amountTendered = amountTendered;
        payment.changeAmount = changeAmount;
        payment.paymentTime = LocalDateTime.now();
        payment.completed = true;
        return payment;
    }

    /**
     * Creates a card payment.
     */
    public static Payment createCardPayment(CardType cardType, double amount) {
        Payment payment = new Payment();
        payment.method = PaymentMethod.CARD;
        payment.cardType = cardType;
        payment.amountTendered = amount;
        payment.changeAmount = 0.0;
        payment.paymentTime = LocalDateTime.now();
        payment.completed = true;
        return payment;
    }

    /**
     * Checks if payment is cash.
     */
    public boolean isCash() {
        return method == PaymentMethod.CASH;
    }

    /**
     * Checks if payment is card.
     */
    public boolean isCard() {
        return method == PaymentMethod.CARD;
    }
}