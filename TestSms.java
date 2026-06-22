import java.util.regex.*;
import java.util.Locale;

public class TestSms {
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(?i)(?:₹|\\brs\\.?|\\binr\\b)\\s*([\\d,]+\\.?\\d*)");

    public static boolean containsAny(String str, String... keywords) {
        for (String kw : keywords) {
            if (str.contains(kw)) return true;
        }
        return false;
    }

    public static void main(String[] args) {
        String body = "Dear investor, your sip under folio xxx in shceme nifty 50 thorught direct for rs 10,000.00 is processed and 59.994 units at NAV 167.929 dated xx-june-xxxx are alloted on reciept of funds. Regards -UTI MF";
        Matcher matcher = AMOUNT_PATTERN.matcher(body);
        long bestAmount = 0L;
        int bestScore = -1;

        while (matcher.find()) {
            String amountRaw = matcher.group(1);
            if (amountRaw == null) continue;
            try {
                long parsed = Math.round(Double.parseDouble(amountRaw.replace(",", "")) * 100d);
                if (parsed <= 0) continue;

                int matchStart = matcher.start();
                String preceding = body.substring(Math.max(0, matchStart - 30), matchStart).toLowerCase(Locale.US);
                System.out.println("Found amountRaw: " + amountRaw + " | Parsed: " + parsed + " | Preceding: [" + preceding + "]");

                int score = 1;
                if (containsAny(preceding, "debited", "invested", "purchased", "bought",
                        "sip of", "allotted", "amount")) {
                    score = 3;
                } else if (containsAny(preceding, "rs", "inr", "₹")) {
                    score = 2;
                }
                if (containsAny(preceding, "balance", "avl bal", "limit", "available", "value", "current", "total", "nav")) {
                    score = 0;
                }
                System.out.println("Score assigned: " + score);

                if (score > bestScore || (score == bestScore && parsed > bestAmount)) {
                    bestAmount = parsed;
                    bestScore = score;
                }
            } catch (Exception e) {}
        }
        System.out.println("Final Best Amount: " + bestAmount);
    }
}
