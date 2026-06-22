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
            "(?:\\bRs\\.?|\\bINR\\b|₹)\\s*([\\d,]+\\.?\\d*)",
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
            "(?:UPI|VPA)[:\\s]+([A-Za-z0-9._-]+@[A-Za-z0-9._-]+)",
            Pattern.CASE_INSENSITIVE);

    // Generic UPI handle anywhere in body
    private static final Pattern UPI_HANDLE_PATTERN = Pattern.compile(
            "([A-Za-z0-9._-]{2,}@[A-Za-z0-9._-]{2,})",
            Pattern.CASE_INSENSITIVE);

    // Payee after to/towards/beneficiary/payee
    private static final Pattern PAYEE_PATTERN = Pattern.compile(
            "(?:to|towards|beneficiary|payee)\\s+([A-Za-z][A-Za-z0-9\\s&'.,-]+?)(?:\\.|\\s+on|\\s+Ref|\\s+Info|\\s+Avl|$)",
            Pattern.CASE_INSENSITIVE);

    // Merchant/payee after "at" or "to" or "for"
    private static final Pattern MERCHANT_PATTERN = Pattern.compile(
            "(?:at|in|via|from|@)\\s+([A-Za-z][A-Za-z0-9\\s&'.,-]+?)(?:\\.|\\s+on|\\s+Ref|\\s+Info|\\s+Avl|$)",
            Pattern.CASE_INSENSITIVE);

    // Known bank sender IDs
    private static final Pattern BANK_SENDER_PATTERN = Pattern.compile(
            "(?:SBI|HDFC|ICICI|AXIS|KOTAK|PNB|BOB|IDBI|YES|INDUS|" +
                    "CANARA|UNION|FEDERAL|IDFCFB|RBL|CITI|AMEX|PAYTM|" +
                    "GPAY|PHONEPE|BHIM|UPI)",
            Pattern.CASE_INSENSITIVE);

    public static boolean isPromotionalSms(String body) {
        if (body == null || body.trim().isEmpty()) {
            return false;
        }
        String normalized = body.toLowerCase(java.util.Locale.US);
        String[] promoTokens = {
                "you are eligible for", "pre-approved", "pre approved",
                "apply now", "avail now", "limited offer", "special offer",
                "congratulations! you", "congrats!",
                "click here", "bit.ly/", "apply.link",
                "call to opt", "sms stop", "unsubscribe",
                "eligible for", "instant loan", "loan offer", "upgrade", "personal loan",
                "will be converted", "expires", "caution"
        };
        for (String token : promoTokens) {
            if (normalized.contains(token)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if an SMS is likely a bank transaction message.
     * 
     * @param sender SMS sender address
     * @param body   SMS body text
     * @return true if the message appears to be a transaction SMS
     */
    public static boolean isTransactionSms(String sender, String body) {
        if (body == null || isPromotionalSms(body))
            return false;
        String safeSender = sender == null ? "" : sender;

        // Check sender looks like a known bank sender or a DLT-style transactional
        // header.
        // Generic "XX-anything" senders are not trusted.
        boolean bankSender = safeSender.length() <= 15 &&
                (BANK_SENDER_PATTERN.matcher(safeSender).find() ||
                        safeSender.matches("^[A-Za-z]{2}-[A-Za-z0-9]{6}$"));

        // Must contain an amount
        boolean hasAmount = AMOUNT_PATTERN.matcher(body).find();

        // Must indicate a transaction
        boolean hasTransaction = DEBIT_PATTERN.matcher(body).find() ||
                CREDIT_PATTERN.matcher(body).find();

        boolean bankBodyKeywords = body.matches("(?is).*(a/?c|account|upi|card|txn|transaction|bank).*");
        return hasAmount && hasTransaction && (bankSender || bankBodyKeywords);
    }

    /**
     * Parse an SMS body to extract transaction details.
     * 
     * @param body            SMS message body
     * @param timestampMillis SMS timestamp
     * @return ParsedTransaction or null if parsing fails
     */
    @Nullable
    public static ParsedTransaction parse(String sender, String body, long timestampMillis) {
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

        if (amountCents <= 0) {
            return null;
        }

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
            merchant = normalizeUpiHandle(upiMatcher.group(1));
        } else {
            Matcher upiHandleMatcher = UPI_HANDLE_PATTERN.matcher(body);
            if (upiHandleMatcher.find()) {
                merchant = normalizeUpiHandle(upiHandleMatcher.group(1));
            } else {
                Matcher payeeMatcher = PAYEE_PATTERN.matcher(body);
                if (payeeMatcher.find()) {
                    merchant = cleanMerchant(payeeMatcher.group(1));
                } else {
                    Matcher merchantMatcher = MERCHANT_PATTERN.matcher(body);
                    if (merchantMatcher.find()) {
                        merchant = cleanMerchant(merchantMatcher.group(1));
                    }
                }
            }
        }

        return new ParsedTransaction(type, sender, amountCents, merchant,
                accountInfo, body, timestampMillis);
    }

    private static String normalizeUpiHandle(String handle) {
        if (handle == null) {
            return null;
        }
        String cleaned = handle.trim().replaceAll("^[^A-Za-z0-9]+|[^A-Za-z0-9@._-]+$", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        int atIndex = cleaned.indexOf('@');
        String alias = atIndex > 0 ? cleaned.substring(0, atIndex) : cleaned;
        String normalizedAlias = alias.replaceAll("[._-]+", " ").trim();
        if (!normalizedAlias.isEmpty()) {
            return normalizedAlias;
        }
        return cleaned;
    }

    private static String cleanMerchant(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.trim()
                .replaceAll("^[\\s,.:;\\-]+|[\\s,.:;\\-]+$", "")
                .replaceAll("(?i)\\b(?:upi|txn|ref|avl|info|account|a/c)\\b.*$", "")
                .trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
