package com.spendwise.ai.sms;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TransactionParserTest {

    @Test
    public void parse_extractsPayeeFromToClause() {
        ParsedTransaction transaction = TransactionParser.parse(
                "Rs.550.00 debited from A/c XX1234 to Airtel Payments Bank on 15-02-26. Ref 9988",
                System.currentTimeMillis());

        assertNotNull(transaction);
        assertEquals(ParsedTransaction.TransactionType.DEBIT, transaction.getType());
        assertEquals(55000L, transaction.getAmountCents());
        assertEquals("Airtel Payments Bank", transaction.getMerchant());
    }

    @Test
    public void parse_extractsAliasFromUpiHandle() {
        ParsedTransaction transaction = TransactionParser.parse(
                "INR 120.00 paid via UPI to milk.store-001@okaxis from account XX6789",
                System.currentTimeMillis());

        assertNotNull(transaction);
        assertEquals("milk store 001", transaction.getMerchant().toLowerCase());
    }

    @Test
    public void isTransactionSms_allowsTransactionalBodyWhenSenderUnknown() {
        boolean result = TransactionParser.isTransactionSms(
                "VK-ALERTS",
                "Rs 999 debited from your account via UPI txn ref 12345");

        assertTrue(result);
    }

    @Test
    public void isTransactionSms_handlesNullSenderWhenBodyIsTransactional() {
        boolean result = TransactionParser.isTransactionSms(
                null,
                "Rs 250 debited from account XX1234 via UPI ref 9988");

        assertTrue(result);
    }
}
