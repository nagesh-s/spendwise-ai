import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class TestSms {
    public static void main(String[] args) {
        String body = "you will be converted to monthly rental plan of Rs. 499 + taxes";
        Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?:Rs\\.?|INR|₹)\\s*([\\d,]+\\.?\\d*)",
            Pattern.CASE_INSENSITIVE);
        Pattern DEBIT_PATTERN = Pattern.compile(
            "(?:debited|debit|spent|paid|purchase|payment|withdrawn|withdraw|sent|transferred|txn)",
            Pattern.CASE_INSENSITIVE);
        Pattern CREDIT_PATTERN = Pattern.compile(
            "(?:credited|credit|received|refund|cashback|deposited)",
            Pattern.CASE_INSENSITIVE);

        System.out.println("Amount: " + AMOUNT_PATTERN.matcher(body).find());
        System.out.println("Debit: " + DEBIT_PATTERN.matcher(body).find());
        Matcher m = DEBIT_PATTERN.matcher(body);
        if(m.find()) { System.out.println("Debit matched: " + m.group()); }
        System.out.println("Credit: " + CREDIT_PATTERN.matcher(body).find());
        m = CREDIT_PATTERN.matcher(body);
        if(m.find()) { System.out.println("Credit matched: " + m.group()); }
    }
}
