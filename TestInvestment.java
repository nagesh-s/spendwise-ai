import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class TestInvestment {
    public static void main(String[] args) {
        String body = "you will be converted to monthly rental plan of Rs. 499 + taxes";
        Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?i)(?:₹|rs\\.?|inr)\\s*([\\d,]+\\.?\\d*)");
        
        System.out.println("Amount: " + AMOUNT_PATTERN.matcher(body).find());
        
        boolean hasInvestmentKeyword = containsAny(body.toLowerCase(),
                "investment", "mutual fund", "mf", "sip", "folio",
                "etf", "shares", "stock", "demat", "buy order", "allotment",
                "units", "scheme", "amc", "upstox", "groww", "zerodha",
                "coin", "kite", "angel one", "icici direct", "paytm money",
                "et money", "mf central", "broker", "demat");
        boolean containsFdKeyword = containsAny(body.toLowerCase(), " fixed deposit", " fd ", "fd no", "fd number");
        boolean containsRdKeyword = containsAny(body.toLowerCase(), " recurring deposit", " rd ", "rd no", "rd number");
        System.out.println("InvKeyword: " + hasInvestmentKeyword + " FD: " + containsFdKeyword + " RD: " + containsRdKeyword);
    }
    
    private static boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
