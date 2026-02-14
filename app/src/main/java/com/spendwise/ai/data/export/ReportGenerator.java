package com.spendwise.ai.data.export;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;

import com.spendwise.ai.data.entity.Account;
import com.spendwise.ai.data.entity.Category;
import com.spendwise.ai.data.entity.Expense;
import com.spendwise.ai.util.DateUtils;

import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportGenerator {

    private static final String CSV_HEADER = "Date,Category,Amount,Merchant,Account,Note\n";

    public String generateCsv(List<Expense> expenses, Map<Long, String> categoryNames, Map<Long, String> accountNames) {
        StringBuilder sb = new StringBuilder();
        sb.append(CSV_HEADER);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Expense expense : expenses) {
            String date = expense.getDate().format(dateFormatter);
            String category = categoryNames.getOrDefault(expense.getCategoryId(), "Unknown");
            String account = accountNames.getOrDefault(expense.getAccountId(), "Unknown");
            double amount = expense.getAmountCents() / 100.0;
            String merchant = escapeCsv(expense.getMerchantName());
            String note = escapeCsv(expense.getDescription());

            sb.append(String.format("%s,%s,%.2f,%s,%s,%s\n",
                    date, escapeCsv(category), amount, merchant, escapeCsv(account), note));
        }

        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null)
            return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public void generatePdf(Context context, OutputStream outputStream, List<Expense> expenses,
            Map<Long, String> categoryNames, Map<Long, String> accountNames, String title) throws IOException {

        PdfDocument document = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();

        // A4 size in PostScript points (approx 595x842)
        int pageWidth = 595;
        int pageHeight = 842;
        int margin = 50;
        int yPosition = margin;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Title
        titlePaint.setTextSize(24);
        titlePaint.setColor(Color.BLACK);
        titlePaint.setFakeBoldText(true);
        canvas.drawText(title, margin, yPosition, titlePaint);
        yPosition += 40;

        // Headers
        paint.setTextSize(12);
        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(true);

        int[] colWidths = { 80, 100, 80, 100, 100 }; // Date, Category, Amount, Merchant, Note
        String[] headers = { "Date", "Category", "Amount", "Merchant", "Note" };
        int xPosition = margin;

        for (int i = 0; i < headers.length; i++) {
            canvas.drawText(headers[i], xPosition, yPosition, paint);
            xPosition += colWidths[i];
        }

        yPosition += 20;
        canvas.drawLine(margin, yPosition - 5, pageWidth - margin, yPosition - 5, paint);

        // Content
        paint.setFakeBoldText(false);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd");

        for (Expense expense : expenses) {
            if (yPosition > pageHeight - margin) {
                document.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.getPages().size() + 1)
                        .create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                yPosition = margin;
                // Redraw headers? Optional. Let's keep it simple.
            }

            xPosition = margin;
            String date = expense.getDate().format(dateFormatter);
            String category = categoryNames.getOrDefault(expense.getCategoryId(), "-");
            String amount = DateUtils.formatAmountCents(expense.getAmountCents());
            String merchant = expense.getMerchantName() != null ? expense.getMerchantName() : "-";
            String note = expense.getDescription() != null ? expense.getDescription() : "";

            // Truncate strings to fit
            merchant = truncate(merchant, 15);
            note = truncate(note, 15);
            category = truncate(category, 12);

            canvas.drawText(date, xPosition, yPosition, paint);
            xPosition += colWidths[0];
            canvas.drawText(category, xPosition, yPosition, paint);
            xPosition += colWidths[1];
            canvas.drawText(amount, xPosition, yPosition, paint);
            xPosition += colWidths[2];
            canvas.drawText(merchant, xPosition, yPosition, paint);
            xPosition += colWidths[3];
            canvas.drawText(note, xPosition, yPosition, paint);

            yPosition += 20;
        }

        document.finishPage(page);
        document.writeTo(outputStream);
        document.close();
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength)
            return text;
        return text.substring(0, maxLength - 1) + "…";
    }
}
