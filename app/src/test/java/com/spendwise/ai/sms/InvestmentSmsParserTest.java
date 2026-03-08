package com.spendwise.ai.sms;

import com.spendwise.ai.data.entity.Investment;

import org.junit.Assert;
import org.junit.Test;

public class InvestmentSmsParserTest {

    @Test
    public void parse_withCommonRef_shouldShareCorrelationKey() {
        String debit = "Rs 5000 debited for SIP order Ref AB12CD34 in Parag Parikh Flexi Cap via Upstox";
        String allotment = "Units allotted for Parag Parikh Flexi Cap. Ref AB12CD34. Amount Rs 5000";

        ParsedInvestmentSignal debitSignal = InvestmentSmsParser.parse("AD-UPSTOX", debit, 1700000000000L);
        ParsedInvestmentSignal allotmentSignal = InvestmentSmsParser.parse("AD-UPSTOX", allotment, 1700003600000L);

        Assert.assertNotNull(debitSignal);
        Assert.assertNotNull(allotmentSignal);
        Assert.assertEquals(debitSignal.getCorrelationKey(), allotmentSignal.getCorrelationKey());
    }

    @Test
    public void parse_fdMessage_shouldInferFixedDeposit() {
        String fdMessage = "Your FD booked successfully. FD no FD998877. Amount INR 100000 at HDFC Bank.";
        ParsedInvestmentSignal signal = InvestmentSmsParser.parse("VK-HDFCBK", fdMessage, 1700000000000L);

        Assert.assertNotNull(signal);
        Assert.assertEquals(Investment.ASSET_FD, signal.getAssetType());
        Assert.assertEquals(10_000_000L, signal.getAmountCents());
    }

    @Test
    public void parse_correctionMessage_shouldFlagCorrection() {
        String correctionMessage = "Updated allotment: Ref AB12CD34 corrected amount Rs 4985 for SIP.";
        ParsedInvestmentSignal signal = InvestmentSmsParser.parse("AD-UPSTOX", correctionMessage, 1700000000000L);

        Assert.assertNotNull(signal);
        Assert.assertTrue(signal.isCorrection());
    }

    @Test
    public void parse_brokerDebitWithoutUnits_shouldStillInferInvestment() {
        String debitMessage = "Rs 5000 debited via UPI to Upstox. Ref 77AB8899.";
        ParsedInvestmentSignal signal = InvestmentSmsParser.parse("VK-HDFCBK", debitMessage, 1700000000000L);

        Assert.assertNotNull(signal);
        Assert.assertEquals("Upstox", signal.getPlatform());
        Assert.assertEquals(ParsedInvestmentSignal.SignalType.INVESTMENT_DEBIT, signal.getSignalType());
    }
}
