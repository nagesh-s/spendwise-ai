import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class TestExpense {
    public static void main(String[] args) {
        String msg = "the app is considering the rental plan that expires with a caution as you will be converted to monthly rental plan of Rs. 499 + taxes as a expense";
        System.out.println("Debit: " + Pattern.compile("(?:debited|debit|spent|paid|purchase|payment|withdrawn|withdraw|sent|transferred|txn)", Pattern.CASE_INSENSITIVE).matcher(msg).find());
        Matcher m = Pattern.compile("(?:debited|debit|spent|paid|purchase|payment|withdrawn|withdraw|sent|transferred|txn)", Pattern.CASE_INSENSITIVE).matcher(msg);
        while (m.find()) { System.out.println("Matched: " + m.group()); }
    }
}
