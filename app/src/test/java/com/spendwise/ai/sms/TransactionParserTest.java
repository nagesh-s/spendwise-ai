package com.spendwise.ai.sms;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TransactionParserTest {

    @Test
    public void isTransactionSms_detectsValidBankDebitMessage() {
        String sender = "VK-HDFCBK";
        String body = "Rs.1,234.50 debited from A/c XX1234 on 14-02-26. UPI: milkstore@upi";

        assertTrue(TransactionParser.isTransactionSms(sender, body));
    }

    @Test
    public void isTransactionSms_rejectsNonTransactionalPromotionalMessage() {
        String sender = "AD-MARKET";
        String body = "INR 500 off on your next order. Tap now to claim.";

        assertFalse(TransactionParser.isTransactionSms(sender, body));
    }

    @Test
    public void parse_debitMessage_extractsAmountTypeMerchantAndAccount() {
        String body = "Rs.1,234.50 debited from A/c XX1234 on 14-02-26. UPI: milkstore@upi Ref 1245";

        ParsedTransaction parsed = TransactionParser.parse(body, 1700000000000L);

        assertNotNull(parsed);
        assertEquals(ParsedTransaction.TransactionType.DEBIT, parsed.getType());
        assertEquals(123450L, parsed.getAmountCents());
        assertEquals("milkstore@upi", parsed.getMerchant());
        assertEquals("XX1234", parsed.getAccountInfo());
    }

    @Test
    public void parse_creditMessage_extractsCreditTypeAndAmount() {
        String body = "INR 500.00 credited to account ending 4321 on 14-02-26.";

        ParsedTransaction parsed = TransactionParser.parse(body, 1700000000000L);

        assertNotNull(parsed);
        assertEquals(ParsedTransaction.TransactionType.CREDIT, parsed.getType());
        assertEquals(50000L, parsed.getAmountCents());
        assertEquals("XX4321", parsed.getAccountInfo());
    }

    @Test
    public void parse_returnsNullForInvalidOrZeroAmount() {
        assertNull(TransactionParser.parse("Rs 0 debited from A/c XX1234", 1700000000000L));
        assertNull(TransactionParser.parse("Amount debited from A/c XX1234", 1700000000000L));
    }
}
