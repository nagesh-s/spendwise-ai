import com.spendwise.ai.sms.InvestmentSmsParser;
import com.spendwise.ai.sms.ParsedInvestmentSignal;

public class TestParser {
    public static void main(String[] args) {
        String body = "Dear investor, your sip under folio xxx in shceme nifty 50 thorught direct for rs 10,000.00 is processed and 59.994 units at NAV 167.929 dated xx-june-xxxx are alloted on reciept of funds. Regards -UTI MF";
        String sender = "UTIMF";
        long timestamp = System.currentTimeMillis();
        
        System.out.println("Testing InvestmentSmsParser...");
        ParsedInvestmentSignal signal = InvestmentSmsParser.parse(sender, body, timestamp);
        
        if (signal == null) {
            System.out.println("Parse returned NULL!");
        } else {
            System.out.println("Parsed successfully!");
            System.out.println("Amount Cents: " + signal.getAmountCents());
            System.out.println("Platform: " + signal.getPlatform());
            System.out.println("Investment Name: " + signal.getInvestmentName());
            System.out.println("Asset Type: " + signal.getAssetType());
            System.out.println("Signal Type: " + signal.getSignalType());
            System.out.println("Confidence: " + signal.getConfidence());
        }
    }
}
