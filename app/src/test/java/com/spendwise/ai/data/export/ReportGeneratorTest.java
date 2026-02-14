package com.spendwise.ai.data.export;

import com.spendwise.ai.data.entity.Expense;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class ReportGeneratorTest {

    @Test
    public void generateCsv_appliesEscapingAndFallbackNames() {
        Locale original = Locale.getDefault();
        Locale.setDefault(Locale.US);
        try {
            Expense e1 = new Expense(0L, 12345L, "Coffee,with milk", 1L, null);
            Expense e2 = new Expense(1L, 2500L, "He said \"hi\"", 2L, 10L);
            e2.setAccountId(20L);

            Map<Long, String> categories = new HashMap<>();
            categories.put(1L, "Food & Dining");
            categories.put(2L, "Travel");

            Map<Long, String> accounts = new HashMap<>();
            accounts.put(20L, "Credit Card");

            Map<Long, String> merchants = new HashMap<>();
            merchants.put(10L, "Cafe \"Blue\"");

            String csv = new ReportGenerator().generateCsv(
                    Arrays.asList(e1, e2),
                    categories,
                    accounts,
                    merchants);

            assertTrue(csv.startsWith("Date,Category,Amount,Merchant,Account,Note\n"));
            assertTrue(csv.contains("1970-01-01,Food & Dining,123.45,Unknown,Unknown,\"Coffee,with milk\"\n"));
            assertTrue(csv.contains("1970-01-02,Travel,25.00,\"Cafe \"\"Blue\"\"\",Credit Card,\"He said \"\"hi\"\"\"\n"));
        } finally {
            Locale.setDefault(original);
        }
    }
}

