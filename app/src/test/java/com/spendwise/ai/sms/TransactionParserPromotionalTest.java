package com.spendwise.ai.sms;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TransactionParserPromotionalTest {
    @Test
    public void rentalPlanMessage_isNotATransaction() {
        String message = "you will be converted to monthly rental plan of Rs. 499 + taxes";

        assertTrue(TransactionParser.isPromotionalSms(message));
        assertNull(TransactionParser.parse("VM-JIO", message, System.currentTimeMillis()));
    }
}
