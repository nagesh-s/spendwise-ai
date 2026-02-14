package com.spendwise.ai.sms;

import androidx.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regex-based SMS parser for Indian bank transaction messages.
 * 
 * Supports common patterns from:
 * - SBI, HDFC, ICICI, Axis, Kotak, PNB, BOB, etc.
 * - UPI apps (GPay, PhonePe, Paytm)
 * - Credit card transactions
 * 
 * Pattern format examples:
 * "Rs.1234.56 debited from A/c XX1234 on 14-02-26. UPI: merchant@upi"
 * "INR 500.00 spent on your Card ending 4321 at AMAZON"
 * "Your a/c XX5678 is credited with Rs 10,000.00"
 */
public class TransactionParser {

    // Amount patterns - matches Rs/INR/₹ followed by amount with optional commas
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?:Rs\\.?|INR|₹)\\s*([\\d,]+\\.?\\d*)",
            Pattern.CASE_INSENSITIVE);

    // Debit keywords
    private static final Pattern DEBIT_PATTERN = Pattern.compile(
            "(?:debited|debit|spent|paid|purchase|payment|withdrawn|withdraw|sent|transferred|txn)",
            Pattern.CASE_INSENSITIVE);

    // Credit keywords
    private static final Pattern CREDIT_PATTERN = Pattern.compile(
            "(?:credited|credit|received|refund|cashback|deposited)",
            Pattern.CASE_INSENSITIVE);

    // Account number pattern (last 4 digits)
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile(
            "(?:a/?c|account|card|acct)\\s*(?:no\\.?)?\\s*(?:ending\\s*)?(?:XX|xx|\\*+)?\\s*(\\d{4})",
            Pattern.CASE_INSENSITIVE);

    // UPI merchant pattern
    private static final Pattern UPI_PATTERN = Pattern.compile(
            "(?:UPI|VPA)[:\\s]+([\\w.@]+)",
            Pattern.CASE_INSENSITIVE);

    // Merchant/payee after "at" or "to" or "for"
    private static final Pattern MERCHANT_PATTERN = Pattern.compile(
            "(?:at|to|for|@)\\s+([A-Za-z][A-Za-z0-9\\s&'.,-]+?)(?:\\.|\\s+on|\\s+Ref|\\s+Info|$)",
            Pattern.CASE_INSENSITIVE);

    // Known bank sender IDs
    private static final Pattern BANK_SENDER_PATTERN = Pattern.compile(
            "(?:SBI|HDFC|ICICI|AXIS|KOTAK|PNB|BOB|IDBI|YES|INDUS|" +
                    "CANARA|UNION|FEDERAL|IDFCFB|RBL|CITI|AMEX|PAYTM|" +
                    "GPAY|PHONEPE|BHIM|UPI)",
            Pattern.CASE_INSENSITIVE);

    /**
     * Check if an SMS is likely a bank transaction message.
     * 
     * @param sender SMS sender address
     * @param body   SMS body text
     * @return true if the message appears to be a transaction SMS
     */
    public static boolean isTransactionSms(String sender, String body) {
        if (sender == null || body == null)
            return false;

        // Check sender looks like a bank shortcode (starts with xx- or has bank name)
        boolean bankSender = sender.length() <= 15 &&
                (BANK_SENDER_PATTERN.matcher(sender).find() ||
                        sender.matches("^[A-Za-z]{2}-.*"));

        // Must contain an amount
        boolean hasAmount = AMOUNT_PATTERN.matcher(body).find();

        // Must indicate a transaction
        boolean hasTransaction = DEBIT_PATTERN.matcher(body).find() ||
                CREDIT_PATTERN.matcher(body).find();

        return bankSender && hasAmount && hasTransaction;
    }

    /**
     * Parse an SMS body to extract transaction details.
     * 
     * @param body            SMS message body
     * @param timestampMillis SMS timestamp
     * @return ParsedTransaction or null if parsing fails
     */
    @Nullable
    public static ParsedTransaction parse(String body, long timestampMillis) {
        if (body == null || body.isEmpty())
            return null;

        // Extract amount
        Matcher amountMatcher = AMOUNT_PATTERN.matcher(body);
        if (!amountMatcher.find())
            return null;

        String amountStr = amountMatcher.group(1);
        if (amountStr == null)
            return null;
        amountStr = amountStr.replace(",", "");

        long amountCents;
        try {
            amountCents = Math.round(Double.parseDouble(amountStr) * 100);
        } catch (NumberFormatException e) {
            return null;
        }

        if (amountCents <= 0)
            return null;

        // Determine transaction type
        ParsedTransaction.TransactionType type;
        if (CREDIT_PATTERN.matcher(body).find()) {
            type = ParsedTransaction.TransactionType.CREDIT;
        } else if (DEBIT_PATTERN.matcher(body).find()) {
            type = ParsedTransaction.TransactionType.DEBIT;
        } else {
            return null; // Can't determine type
        }

        // Extract account info
        String accountInfo = null;
        Matcher accountMatcher = ACCOUNT_PATTERN.matcher(body);
        if (accountMatcher.find()) {
            accountInfo = "XX" + accountMatcher.group(1);
        }

        // Extract merchant
        String merchant = null;
        Matcher upiMatcher = UPI_PATTERN.matcher(body);
        if (upiMatcher.find()) {
            merchant = upiMatcher.group(1);
        } else {
            Matcher merchantMatcher = MERCHANT_PATTERN.matcher(body);
            if (merchantMatcher.find()) {
                merchant = merchantMatcher.group(1).trim();
            }
        }

        return new ParsedTransaction(type, amountCents, merchant,
                accountInfo, body, timestampMillis);
    }
}
