package com.spendwise.ai.sms;

import org.junit.Test;
import static org.junit.Assert.*;

public class TransactionParserTestSms {
    @Test
    public void testProblemSms() {
        String msg = "you will be converted to monthly rental plan of Rs. 499 + taxes";
        boolean isPromotional = TransactionParser.isPromotionalSms(msg);
        boolean isTxn = TransactionParser.isTransactionSms("VM-JIO", msg);
        System.out.println("isPromo: " + isPromotional + " isTxn: " + isTxn);
        
        ParsedTransaction txn = TransactionParser.parse("VM-JIO", msg, System.currentTimeMillis());
        System.out.println("Parsed: " + (txn != null));
    }
}
