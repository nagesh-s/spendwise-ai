package com.spendwise.ai.data.export;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import com.spendwise.ai.data.entity.Expense;
import com.spendwise.ai.util.DateUtils;

import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportGenerator {

    private static final String CSV_HEADER = "Date,Category,Amount,Merchant,Account,Note\n";
    private static final int PAGE_WIDTH = 595; // A4 width in points
    private static final int PAGE_HEIGHT = 842; // A4 height in points
    private static final int PAGE_MARGIN_HORIZONTAL = 24;
    private static final int PAGE_MARGIN_TOP = 24;
    private static final int PAGE_MARGIN_BOTTOM = 24;
    private static final int ROW_HEIGHT = 24;
    private static final int TABLE_HEADER_HEIGHT = 24;
    private static final int[] COLUMN_WIDTHS = { 74, 110, 96, 90, 105, 72 };
    private static final String[] COLUMN_HEADERS = { "Date", "Category", "Merchant", "Account", "Note", "Amount" };

    private static final int COLOR_TITLE_BG = 0xFF0B5D57;
    private static final int COLOR_TITLE_TEXT = 0xFFFFFFFF;
    private static final int COLOR_TEXT_PRIMARY = 0xFF111827;
    private static final int COLOR_TEXT_SECONDARY = 0xFF6B7280;
    private static final int COLOR_TABLE_HEADER_BG = 0xFFE7F2F0;
    private static final int COLOR_TABLE_LINE = 0xFFD1D5DB;
    private static final int COLOR_TABLE_ALT_ROW = 0xFFF8FAFC;
    private static final int COLOR_AMOUNT = 0xFF0F766E;
    private static final int COLOR_SUMMARY_BG = 0xFFF3F4F6;
    private static final int CELL_PADDING = 5;

    public String generateCsv(List<Expense> expenses, Map<Long, String> categoryNames, Map<Long, String> accountNames,
            Map<Long, String> merchantNames) {
        StringBuilder sb = new StringBuilder();
        sb.append(CSV_HEADER);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Expense expense : expenses) {
            String date = DateUtils.epochDayToDate(expense.getDateEpochDay()).format(dateFormatter);
            String category = categoryNames.getOrDefault(expense.getCategoryId(), "Unknown");
            String account = accountNames.getOrDefault(expense.getAccountId(), "Unknown");
            double amount = expense.getAmountCents() / 100.0;

            String merchantName = "Unknown";
            if (expense.getMerchantId() != null) {
                merchantName = merchantNames.getOrDefault(expense.getMerchantId(), "Unknown");
            }

            String note = escapeCsv(expense.getDescription());

            sb.append(String.format(Locale.US, "%s,%s,%.2f,%s,%s,%s\n",
                    date, escapeCsv(category), amount, escapeCsv(merchantName), escapeCsv(account), note));
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
            Map<Long, String> categoryNames, Map<Long, String> accountNames, Map<Long, String> merchantNames,
            String title) throws IOException {
        PdfDocument document = new PdfDocument();
        int pageNumber = 1;
        PdfDocument.Page page = document.startPage(
                new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create());
        Canvas canvas = page.getCanvas();

        Paint sectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint subtitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint headerCellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint rowTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint amountTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint rowAltPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        titlePaint.setColor(COLOR_TITLE_TEXT);
        titlePaint.setTextSize(18f);
        titlePaint.setFakeBoldText(true);

        subtitlePaint.setColor(COLOR_TEXT_SECONDARY);
        subtitlePaint.setTextSize(10f);

        headerCellPaint.setColor(COLOR_TEXT_PRIMARY);
        headerCellPaint.setTextSize(10f);
        headerCellPaint.setFakeBoldText(true);

        rowTextPaint.setColor(COLOR_TEXT_PRIMARY);
        rowTextPaint.setTextSize(9.5f);

        amountTextPaint.set(rowTextPaint);
        amountTextPaint.setTextAlign(Paint.Align.RIGHT);
        amountTextPaint.setColor(COLOR_AMOUNT);
        amountTextPaint.setFakeBoldText(true);

        rowAltPaint.setColor(COLOR_TABLE_ALT_ROW);
        linePaint.setColor(COLOR_TABLE_LINE);
        linePaint.setStrokeWidth(1f);

        long totalCents = 0L;
        LocalDate minDate = null;
        LocalDate maxDate = null;
        for (Expense expense : expenses) {
            totalCents += expense.getAmountCents();
            LocalDate d = DateUtils.epochDayToDate(expense.getDateEpochDay());
            if (minDate == null || d.isBefore(minDate)) {
                minDate = d;
            }
            if (maxDate == null || d.isAfter(maxDate)) {
                maxDate = d;
            }
        }

        DateTimeFormatter periodFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        String periodText = expenses.isEmpty()
                ? "No transactions"
                : minDate.format(periodFormatter) + " to " + maxDate.format(periodFormatter);

        int y = drawPageHeader(canvas, sectionPaint, titlePaint, subtitlePaint, title, pageNumber, periodText,
                expenses.size(), totalCents, true);
        y = drawTableHeader(canvas, sectionPaint, headerCellPaint, linePaint, y);

        int rowIndex = 0;
        DateTimeFormatter rowDateFormatter = DateTimeFormatter.ofPattern("dd MMM");

        if (expenses.isEmpty()) {
            drawEmptyStateRow(canvas, rowTextPaint, linePaint, y);
        } else {
            for (Expense expense : expenses) {
                if (y + ROW_HEIGHT > PAGE_HEIGHT - PAGE_MARGIN_BOTTOM) {
                    document.finishPage(page);
                    pageNumber++;
                    page = document.startPage(
                            new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create());
                    canvas = page.getCanvas();
                    y = drawPageHeader(canvas, sectionPaint, titlePaint, subtitlePaint, title, pageNumber, periodText,
                            expenses.size(), totalCents, false);
                    y = drawTableHeader(canvas, sectionPaint, headerCellPaint, linePaint, y);
                }

                if (rowIndex % 2 == 0) {
                    canvas.drawRect(
                            PAGE_MARGIN_HORIZONTAL,
                            y,
                            PAGE_WIDTH - PAGE_MARGIN_HORIZONTAL,
                            y + ROW_HEIGHT,
                            rowAltPaint);
                }

                String date = DateUtils.epochDayToDate(expense.getDateEpochDay()).format(rowDateFormatter);
                String category = categoryNames.getOrDefault(expense.getCategoryId(), "-");
                String merchant = expense.getMerchantId() != null
                        ? merchantNames.getOrDefault(expense.getMerchantId(), "-")
                        : "-";
                String account = expense.getAccountId() != null
                        ? accountNames.getOrDefault(expense.getAccountId(), "Cash")
                        : "Cash";
                String note = expense.getDescription() != null ? expense.getDescription() : "-";
                String amount = DateUtils.formatAmountCents(expense.getAmountCents());

                int colLeft = PAGE_MARGIN_HORIZONTAL;
                drawCellText(canvas, rowTextPaint, date, colLeft, y, COLUMN_WIDTHS[0], ROW_HEIGHT, false);
                colLeft += COLUMN_WIDTHS[0];
                drawCellText(canvas, rowTextPaint, category, colLeft, y, COLUMN_WIDTHS[1], ROW_HEIGHT, false);
                colLeft += COLUMN_WIDTHS[1];
                drawCellText(canvas, rowTextPaint, merchant, colLeft, y, COLUMN_WIDTHS[2], ROW_HEIGHT, false);
                colLeft += COLUMN_WIDTHS[2];
                drawCellText(canvas, rowTextPaint, account, colLeft, y, COLUMN_WIDTHS[3], ROW_HEIGHT, false);
                colLeft += COLUMN_WIDTHS[3];
                drawCellText(canvas, rowTextPaint, note, colLeft, y, COLUMN_WIDTHS[4], ROW_HEIGHT, false);
                colLeft += COLUMN_WIDTHS[4];
                drawCellText(canvas, amountTextPaint, amount, colLeft, y, COLUMN_WIDTHS[5], ROW_HEIGHT, true);

                canvas.drawLine(
                        PAGE_MARGIN_HORIZONTAL,
                        y + ROW_HEIGHT,
                        PAGE_WIDTH - PAGE_MARGIN_HORIZONTAL,
                        y + ROW_HEIGHT,
                        linePaint);
                y += ROW_HEIGHT;
                rowIndex++;
            }
        }

        document.finishPage(page);
        document.writeTo(outputStream);
        document.close();
    }

    private int drawPageHeader(Canvas canvas, Paint sectionPaint, Paint titlePaint, Paint subtitlePaint,
            String title, int pageNumber, String periodText, int transactionCount, long totalCents, boolean firstPage) {
        int left = PAGE_MARGIN_HORIZONTAL;
        int right = PAGE_WIDTH - PAGE_MARGIN_HORIZONTAL;
        int y = PAGE_MARGIN_TOP;

        sectionPaint.setColor(COLOR_TITLE_BG);
        canvas.drawRect(left, y, right, y + 54, sectionPaint);

        drawLeftText(canvas, titlePaint, title, left + 12, y + 18, 54);
        subtitlePaint.setColor(COLOR_TITLE_TEXT);
        drawLeftText(
                canvas,
                subtitlePaint,
                "Page " + pageNumber,
                left + 12,
                y + 36,
                54);

        y += 66;

        if (firstPage) {
            subtitlePaint.setColor(COLOR_TEXT_SECONDARY);
            drawLeftText(canvas, subtitlePaint, "Statement Period: " + periodText, left, y, 16);
            y += 20;

            int cardGap = 12;
            int cardWidth = (right - left - cardGap) / 2;
            int cardHeight = 44;

            sectionPaint.setColor(COLOR_SUMMARY_BG);
            canvas.drawRoundRect(left, y, left + cardWidth, y + cardHeight, 6, 6, sectionPaint);
            canvas.drawRoundRect(left + cardWidth + cardGap, y, right, y + cardHeight, 6, 6, sectionPaint);

            subtitlePaint.setColor(COLOR_TEXT_SECONDARY);
            drawLeftText(canvas, subtitlePaint, "Transactions", left + 10, y + 7, 18);
            drawLeftText(canvas, subtitlePaint, "Total Spent", left + cardWidth + cardGap + 10, y + 7, 18);

            Paint summaryValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            summaryValuePaint.setColor(COLOR_TEXT_PRIMARY);
            summaryValuePaint.setTextSize(14f);
            summaryValuePaint.setFakeBoldText(true);

            drawLeftText(canvas, summaryValuePaint, String.valueOf(transactionCount), left + 10, y + 20, 18);
            drawLeftText(canvas, summaryValuePaint, DateUtils.formatAmountCents(totalCents),
                    left + cardWidth + cardGap + 10, y + 20, 18);

            y += cardHeight + 14;
        }

        return y;
    }

    private int drawTableHeader(Canvas canvas, Paint sectionPaint, Paint headerCellPaint, Paint linePaint, int y) {
        int left = PAGE_MARGIN_HORIZONTAL;
        int right = PAGE_WIDTH - PAGE_MARGIN_HORIZONTAL;
        sectionPaint.setColor(COLOR_TABLE_HEADER_BG);
        canvas.drawRect(left, y, right, y + TABLE_HEADER_HEIGHT, sectionPaint);

        int colLeft = left;
        for (int i = 0; i < COLUMN_HEADERS.length; i++) {
            boolean alignRight = i == COLUMN_HEADERS.length - 1;
            drawCellText(canvas, headerCellPaint, COLUMN_HEADERS[i], colLeft, y, COLUMN_WIDTHS[i], TABLE_HEADER_HEIGHT,
                    alignRight);
            colLeft += COLUMN_WIDTHS[i];
        }
        canvas.drawLine(left, y + TABLE_HEADER_HEIGHT, right, y + TABLE_HEADER_HEIGHT, linePaint);
        return y + TABLE_HEADER_HEIGHT;
    }

    private void drawEmptyStateRow(Canvas canvas, Paint rowTextPaint, Paint linePaint, int y) {
        int left = PAGE_MARGIN_HORIZONTAL;
        int right = PAGE_WIDTH - PAGE_MARGIN_HORIZONTAL;
        drawCellText(canvas, rowTextPaint, "No expenses found for this period.", left, y, right - left, ROW_HEIGHT,
                false);
        canvas.drawLine(left, y + ROW_HEIGHT, right, y + ROW_HEIGHT, linePaint);
    }

    private void drawCellText(Canvas canvas, Paint paint, String text, int left, int top, int width, int rowHeight,
            boolean alignRight) {
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float baseline = top + (rowHeight - (metrics.bottom - metrics.top)) / 2f - metrics.top;

        String safeText = text == null ? "-" : text;
        float availableWidth = width - (CELL_PADDING * 2f);
        String fitted = ellipsizeToWidth(safeText, paint, availableWidth);

        float x;
        if (alignRight) {
            x = left + width - CELL_PADDING;
        } else {
            x = left + CELL_PADDING;
        }

        Paint.Align originalAlign = paint.getTextAlign();
        paint.setTextAlign(alignRight ? Paint.Align.RIGHT : Paint.Align.LEFT);
        canvas.drawText(fitted, x, baseline, paint);
        paint.setTextAlign(originalAlign);
    }

    private void drawLeftText(Canvas canvas, Paint paint, String text, int left, int top, int areaHeight) {
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float baseline = top + (areaHeight - (metrics.bottom - metrics.top)) / 2f - metrics.top;
        canvas.drawText(text, left, baseline, paint);
    }

    private String ellipsizeToWidth(String text, Paint paint, float maxWidth) {
        if (text == null) {
            return "";
        }
        if (paint.measureText(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        float ellipsisWidth = paint.measureText(ellipsis);
        if (ellipsisWidth > maxWidth) {
            return "";
        }

        int end = text.length();
        while (end > 0 && paint.measureText(text, 0, end) + ellipsisWidth > maxWidth) {
            end--;
        }
        if (end <= 0) {
            return ellipsis;
        }
        return text.substring(0, end) + ellipsis;
    }
}
