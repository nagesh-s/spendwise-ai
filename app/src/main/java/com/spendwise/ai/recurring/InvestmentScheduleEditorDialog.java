package com.spendwise.ai.recurring;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.spendwise.ai.R;
import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Bucket;
import com.spendwise.ai.data.entity.Investment;
import com.spendwise.ai.data.entity.InvestmentSchedule;

import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dialog for creating or editing a recurring investment (SIP) schedule.
 */
public class InvestmentScheduleEditorDialog extends DialogFragment {

    private static final String ARG_SCHEDULE_ID = "schedule_id";

    private static final String[] FREQUENCIES = new String[] {
            "Daily",
            "Every 2 days",
            "Every 3 days",
            "Weekly",
            "Monthly",
            "Yearly"
    };

    private static final String[] ASSET_TYPE_LABELS = new String[] {
            "Stock",
            "Mutual Fund",
            "ETF",
            "Fixed Deposit",
            "Recurring Deposit",
            "PPF",
            "NPS",
            "Gold",
            "Savings",
            "Other"
    };

    private static final String[] ASSET_TYPE_VALUES = new String[] {
            Investment.ASSET_STOCK,
            Investment.ASSET_MF,
            Investment.ASSET_ETF,
            Investment.ASSET_FD,
            Investment.ASSET_RD,
            Investment.ASSET_PPF,
            Investment.ASSET_NPS,
            Investment.ASSET_GOLD,
            Investment.ASSET_SAVINGS,
            Investment.ASSET_OTHER
    };

    private RecurringExpensesViewModel viewModel;

    private TextInputEditText inputName;
    private TextInputEditText inputAmount;
    private TextInputEditText inputDay;
    private TextInputEditText inputPlatform;
    private TextInputEditText inputStartDate;
    private TextInputEditText inputEndDate;
    private AutoCompleteTextView spinnerBucket;
    private AutoCompleteTextView spinnerAssetType;
    private AutoCompleteTextView spinnerFrequency;

    private final List<Bucket> buckets = new ArrayList<>();
    private long selectedBucketId = -1L;
    private int selectedAssetIndex = 0;
    private String selectedFrequency = InvestmentSchedule.FREQUENCY_MONTHLY;
    private InvestmentSchedule existing;

    public static InvestmentScheduleEditorDialog newInstance(long scheduleId) {
        InvestmentScheduleEditorDialog dialog = new InvestmentScheduleEditorDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_SCHEDULE_ID, scheduleId);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireParentFragment()).get(RecurringExpensesViewModel.class);

        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_investment_schedule_editor, null);

        inputName = view.findViewById(R.id.input_name);
        inputAmount = view.findViewById(R.id.input_amount);
        inputDay = view.findViewById(R.id.input_day);
        inputPlatform = view.findViewById(R.id.input_platform);
        inputStartDate = view.findViewById(R.id.input_start_date);
        inputEndDate = view.findViewById(R.id.input_end_date);
        spinnerBucket = view.findViewById(R.id.spinner_bucket);
        spinnerAssetType = view.findViewById(R.id.spinner_asset_type);
        spinnerFrequency = view.findViewById(R.id.spinner_frequency);

        setupStaticSpinners();
        updateDayHint();
        inputStartDate.setText(LocalDate.now().toString());

        loadBucketsAndMaybeExisting();

        long scheduleId = getArguments() == null ? -1L : getArguments().getLong(ARG_SCHEDULE_ID, -1L);
        String title = scheduleId <= 0
                ? getString(R.string.recurring_investment_add_title)
                : getString(R.string.recurring_investment_edit_title);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setView(view)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(di -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (save()) {
                dialog.dismiss();
            }
        }));
        return dialog;
    }

    private void setupStaticSpinners() {
        ArrayAdapter<String> frequencyAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                FREQUENCIES);
        spinnerFrequency.setAdapter(frequencyAdapter);
        spinnerFrequency.setText("Monthly", false);
        spinnerFrequency.setOnItemClickListener((parent, view, position, id) -> {
            selectedFrequency = mapFrequency(position);
            updateDayHint();
        });

        ArrayAdapter<String> assetAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                ASSET_TYPE_LABELS);
        spinnerAssetType.setAdapter(assetAdapter);
        spinnerAssetType.setText(ASSET_TYPE_LABELS[0], false);
        spinnerAssetType.setOnItemClickListener((parent, view, position, id) -> selectedAssetIndex = position);
    }

    private void loadBucketsAndMaybeExisting() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Bucket> loadedBuckets = viewModel.getAllBucketsSync();
            if (loadedBuckets != null) {
                buckets.clear();
                buckets.addAll(loadedBuckets);
            }

            androidx.fragment.app.FragmentActivity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(this::bindBucketSpinnerAndLoadExisting);
        });
    }

    private void bindBucketSpinnerAndLoadExisting() {
        if (!isAdded()) {
            return;
        }
        String[] names = new String[buckets.size()];
        for (int i = 0; i < buckets.size(); i++) {
            names[i] = buckets.get(i).getName();
        }
        ArrayAdapter<String> bucketAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                names);
        spinnerBucket.setAdapter(bucketAdapter);
        spinnerBucket.setOnItemClickListener((parent, view, position, id) -> {
            selectedBucketId = buckets.get(position).getId();
        });

        if (!buckets.isEmpty()) {
            selectedBucketId = buckets.get(0).getId();
            spinnerBucket.setText(buckets.get(0).getName(), false);
        }

        loadExisting();
    }

    private void loadExisting() {
        long scheduleId = getArguments() == null ? -1L : getArguments().getLong(ARG_SCHEDULE_ID, -1L);
        if (scheduleId <= 0) {
            return;
        }
        AppDatabase.databaseWriteExecutor.execute(() -> {
            existing = viewModel.getInvestmentScheduleById(scheduleId);
            if (existing == null) {
                return;
            }
            androidx.fragment.app.FragmentActivity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(() -> applyExisting(existing));
        });
    }

    private void applyExisting(@NonNull InvestmentSchedule schedule) {
        if (!isAdded()) {
            return;
        }
        inputName.setText(schedule.getName());
        inputAmount.setText(String.format(Locale.getDefault(), "%.2f", schedule.getAmountCents() / 100.0));
        inputDay.setText(String.valueOf(schedule.getDayOfPeriod()));
        inputPlatform.setText(schedule.getPlatform());
        inputStartDate.setText(LocalDate.ofEpochDay(schedule.getStartEpochDay()).toString());
        if (schedule.getEndEpochDay() != null) {
            inputEndDate.setText(LocalDate.ofEpochDay(schedule.getEndEpochDay()).toString());
        }

        selectedBucketId = schedule.getBucketId();
        for (Bucket bucket : buckets) {
            if (bucket.getId() == schedule.getBucketId()) {
                spinnerBucket.setText(bucket.getName(), false);
                break;
            }
        }

        selectedAssetIndex = resolveAssetIndex(schedule.getAssetType());
        spinnerAssetType.setText(ASSET_TYPE_LABELS[selectedAssetIndex], false);

        selectedFrequency = schedule.getFrequency();
        spinnerFrequency.setText(resolveFrequencyLabel(schedule.getFrequency()), false);
        updateDayHint();
    }

    private void updateDayHint() {
        if (InvestmentSchedule.FREQUENCY_WEEKLY.equals(selectedFrequency)) {
            inputDay.setEnabled(true);
            inputDay.setHint(getString(R.string.recurring_day_hint_weekly));
        } else if (InvestmentSchedule.FREQUENCY_MONTHLY.equals(selectedFrequency)
                || InvestmentSchedule.FREQUENCY_YEARLY.equals(selectedFrequency)) {
            inputDay.setEnabled(true);
            inputDay.setHint(getString(R.string.recurring_day_hint_monthly));
        } else {
            inputDay.setEnabled(false);
            inputDay.setText("1");
            inputDay.setHint(getString(R.string.recurring_day_hint_not_required));
        }
    }

    private boolean save() {
        String name = getText(inputName);
        String amountText = getText(inputAmount);
        String dayText = getText(inputDay);
        String platform = getNullableText(inputPlatform);
        String startDateText = getText(inputStartDate);
        String endDateText = getNullableText(inputEndDate);

        if (TextUtils.isEmpty(name)) {
            toast(R.string.recurring_investment_error_name);
            return false;
        }
        if (TextUtils.isEmpty(amountText)) {
            toast(R.string.recurring_investment_error_amount);
            return false;
        }
        if (selectedBucketId <= 0) {
            toast(R.string.recurring_investment_error_bucket);
            return false;
        }

        long amountCents;
        try {
            amountCents = Math.round(Double.parseDouble(amountText) * 100d);
        } catch (NumberFormatException e) {
            toast(R.string.recurring_investment_error_amount);
            return false;
        }
        if (amountCents <= 0) {
            toast(R.string.recurring_investment_error_amount);
            return false;
        }

        int dayOfPeriod = 1;
        if (InvestmentSchedule.FREQUENCY_WEEKLY.equals(selectedFrequency)
                || InvestmentSchedule.FREQUENCY_MONTHLY.equals(selectedFrequency)
                || InvestmentSchedule.FREQUENCY_YEARLY.equals(selectedFrequency)) {
            if (TextUtils.isEmpty(dayText)) {
                toast(R.string.recurring_investment_error_day);
                return false;
            }
            try {
                dayOfPeriod = Integer.parseInt(dayText);
            } catch (NumberFormatException e) {
                toast(R.string.recurring_investment_error_day);
                return false;
            }
            if (InvestmentSchedule.FREQUENCY_WEEKLY.equals(selectedFrequency)) {
                if (dayOfPeriod < 1 || dayOfPeriod > 7) {
                    toast(R.string.recurring_investment_error_day_weekly);
                    return false;
                }
            } else if (dayOfPeriod < 1 || dayOfPeriod > 31) {
                toast(R.string.recurring_investment_error_day);
                return false;
            }
        }

        LocalDate startDate;
        try {
            startDate = LocalDate.parse(startDateText);
        } catch (DateTimeParseException e) {
            toast(R.string.recurring_investment_error_start_date);
            return false;
        }

        Long endEpochDay = null;
        if (!TextUtils.isEmpty(endDateText)) {
            try {
                LocalDate endDate = LocalDate.parse(endDateText);
                if (endDate.isBefore(startDate)) {
                    toast(R.string.recurring_investment_error_end_date);
                    return false;
                }
                endEpochDay = endDate.toEpochDay();
            } catch (DateTimeParseException e) {
                toast(R.string.recurring_investment_error_end_date);
                return false;
            }
        }

        String assetType = ASSET_TYPE_VALUES[Math.max(0, Math.min(ASSET_TYPE_VALUES.length - 1, selectedAssetIndex))];

        if (existing != null) {
            existing.setName(name);
            existing.setBucketId(selectedBucketId);
            existing.setAssetType(assetType);
            existing.setPlatform(platform);
            existing.setAmountCents(amountCents);
            existing.setFrequency(selectedFrequency);
            existing.setDayOfPeriod(dayOfPeriod);
            existing.setStartEpochDay(startDate.toEpochDay());
            existing.setEndEpochDay(endEpochDay);
            viewModel.updateInvestmentSchedule(existing);
            toast(R.string.recurring_investment_saved);
        } else {
            InvestmentSchedule schedule = new InvestmentSchedule(
                    name,
                    selectedBucketId,
                    assetType,
                    platform,
                    amountCents,
                    selectedFrequency,
                    dayOfPeriod,
                    startDate.toEpochDay());
            schedule.setEndEpochDay(endEpochDay);
            viewModel.insertInvestmentSchedule(schedule);
            toast(R.string.recurring_investment_saved);
        }
        return true;
    }

    @NonNull
    private String mapFrequency(int position) {
        switch (position) {
            case 0:
                return InvestmentSchedule.FREQUENCY_DAILY;
            case 1:
                return InvestmentSchedule.FREQUENCY_EVERY_2_DAYS;
            case 2:
                return InvestmentSchedule.FREQUENCY_EVERY_3_DAYS;
            case 3:
                return InvestmentSchedule.FREQUENCY_WEEKLY;
            case 4:
                return InvestmentSchedule.FREQUENCY_MONTHLY;
            case 5:
                return InvestmentSchedule.FREQUENCY_YEARLY;
            default:
                return InvestmentSchedule.FREQUENCY_MONTHLY;
        }
    }

    @NonNull
    private String resolveFrequencyLabel(@NonNull String frequency) {
        switch (frequency) {
            case InvestmentSchedule.FREQUENCY_DAILY:
                return "Daily";
            case InvestmentSchedule.FREQUENCY_EVERY_2_DAYS:
                return "Every 2 days";
            case InvestmentSchedule.FREQUENCY_EVERY_3_DAYS:
                return "Every 3 days";
            case InvestmentSchedule.FREQUENCY_WEEKLY:
                return "Weekly";
            case InvestmentSchedule.FREQUENCY_MONTHLY:
                return "Monthly";
            case InvestmentSchedule.FREQUENCY_YEARLY:
                return "Yearly";
            default:
                return "Monthly";
        }
    }

    private int resolveAssetIndex(@Nullable String assetType) {
        if (assetType == null) {
            return 0;
        }
        for (int i = 0; i < ASSET_TYPE_VALUES.length; i++) {
            if (ASSET_TYPE_VALUES[i].equals(assetType)) {
                return i;
            }
        }
        return 0;
    }

    @NonNull
    private String getText(@NonNull TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    @Nullable
    private String getNullableText(@NonNull TextInputEditText input) {
        String value = getText(input);
        return value.isEmpty() ? null : value;
    }

    private void toast(int messageRes) {
        if (!isAdded()) {
            return;
        }
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show();
    }
}
