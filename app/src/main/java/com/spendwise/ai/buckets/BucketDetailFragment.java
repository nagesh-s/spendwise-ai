package com.spendwise.ai.buckets;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.spendwise.ai.R;
import com.spendwise.ai.data.entity.Investment;
import com.spendwise.ai.notifications.InvestmentNotificationSyncHelper;
import com.spendwise.ai.work.WorkScheduler;
import com.spendwise.ai.voice.VoiceInputHelper;
import com.spendwise.ai.voice.VoiceInvestmentParser;

import org.threeten.bp.LocalDate;

import java.util.List;
import java.util.Locale;

/**
 * Drill-down fragment for a single bucket.
 */
public class BucketDetailFragment extends Fragment implements InvestmentAdapter.OnInvestmentClickListener {

    private static final int REQUEST_RECORD_AUDIO = 2101;

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

    private BucketDetailViewModel viewModel;
    private InvestmentAdapter adapter;
    private long bucketId = -1;
    private String bucketName = "Bucket";
    private TextView textTotalValue;
    private TextView textTotalInvested;
    private TextView textTotalPnl;
    private TextView textHoldingsCount;
    private View emptyState;
    private RecyclerView recyclerInvestments;
    private VoiceInputHelper voiceInputHelper;
    private final VoiceInvestmentParser voiceInvestmentParser = new VoiceInvestmentParser();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bucket_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(BucketDetailViewModel.class);
        voiceInputHelper = new VoiceInputHelper(requireContext());
        voiceInputHelper.setListener(new VoiceInputHelper.VoiceListener() {
            @Override
            public void onVoiceText(String text) {
                handleVoiceInvestmentText(text);
            }

            @Override
            public void onVoiceError(String error) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(requireContext(),
                        getString(R.string.bucket_detail_voice_error_format, error),
                        Toast.LENGTH_SHORT).show();
            }
        });

        if (getArguments() != null) {
            bucketId = getArguments().getLong("bucketId", -1);
            bucketName = getArguments().getString("bucketName", "Bucket");
        }

        recyclerInvestments = view.findViewById(R.id.recycler_investments);
        TextView textBucketName = view.findViewById(R.id.text_detail_bucket_name);
        textTotalValue = view.findViewById(R.id.text_detail_total_value);
        textTotalInvested = view.findViewById(R.id.text_detail_total_invested);
        textTotalPnl = view.findViewById(R.id.text_detail_total_pnl);
        textHoldingsCount = view.findViewById(R.id.text_detail_holdings_count);
        emptyState = view.findViewById(R.id.layout_empty_investments);

        textBucketName.setText(bucketName);
        updateSummary(null);

        adapter = new InvestmentAdapter(this);
        recyclerInvestments.setAdapter(adapter);

        view.findViewById(R.id.btn_voice_investment).setOnClickListener(v -> startVoiceInvestmentInput());
        view.findViewById(R.id.btn_import_holdings).setOnClickListener(v -> showImportOptionsDialog());
        view.findViewById(R.id.fab_add_investment).setOnClickListener(v -> {
            showInvestmentEditorDialog(null);
        });

        if (bucketId >= 0) {
            viewModel.getInvestmentsForBucket(bucketId).observe(getViewLifecycleOwner(), investments -> {
                adapter.submitList(investments);
                updateSummary(investments);
                if (investments == null || investments.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                    recyclerInvestments.setVisibility(View.GONE);
                } else {
                    emptyState.setVisibility(View.GONE);
                    recyclerInvestments.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    @Override
    public void onInvestmentClick(Investment investment) {
        showInvestmentEditorDialog(investment);
    }

    @Override
    public void onDestroyView() {
        if (voiceInputHelper != null) {
            voiceInputHelper.destroy();
            voiceInputHelper = null;
        }
        super.onDestroyView();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceInvestmentInput();
            } else if (isAdded()) {
                Toast.makeText(requireContext(), R.string.bucket_detail_voice_permission_needed, Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    private void startVoiceInvestmentInput() {
        if (!hasAudioPermission()) {
            requestPermissions(new String[] { Manifest.permission.RECORD_AUDIO }, REQUEST_RECORD_AUDIO);
            return;
        }
        if (voiceInputHelper != null) {
            Toast.makeText(requireContext(), R.string.bucket_detail_voice_listening, Toast.LENGTH_SHORT).show();
            voiceInputHelper.startListening();
        }
    }

    private boolean hasAudioPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void handleVoiceInvestmentText(@Nullable String text) {
        if (!isAdded()) {
            return;
        }
        VoiceInvestmentParser.ParsedInvestment parsedInvestment = voiceInvestmentParser.parse(text);
        if (!parsedInvestment.isValid()) {
            Toast.makeText(requireContext(), R.string.bucket_detail_voice_parse_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        showInvestmentEditorDialog(null, parsedInvestment);
    }

    private void showImportOptionsDialog() {
        if (!isAdded()) {
            return;
        }
        String[] options = new String[] {
                getString(R.string.bucket_import_option_sms),
                getString(R.string.bucket_import_option_notifications)
        };
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.bucket_detail_import_holdings)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            WorkScheduler.runSmsAutoImportNow(requireContext());
                            Toast.makeText(requireContext(), R.string.bucket_import_sms_started, Toast.LENGTH_SHORT)
                                    .show();
                            break;
                        case 1:
                            startNotificationImport();
                            break;
                        default:
                            break;
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void startNotificationImport() {
        if (!isAdded()) {
            return;
        }
        final android.content.Context context = requireContext();
        if (!InvestmentNotificationSyncHelper.isNotificationAccessEnabled(context)) {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.bucket_import_notifications_access_title)
                    .setMessage(R.string.bucket_import_notifications_access_message)
                    .setPositiveButton(R.string.bucket_import_notifications_open_settings,
                            (dialog, which) -> InvestmentNotificationSyncHelper
                                    .openNotificationAccessSettings(context))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return;
        }
        InvestmentNotificationSyncHelper.requestSyncNow(context);
        Toast.makeText(context, R.string.bucket_import_notifications_started, Toast.LENGTH_SHORT).show();
    }

    private void showInvestmentEditorDialog(@Nullable Investment existingInvestment) {
        showInvestmentEditorDialog(existingInvestment, null);
    }

    private void showInvestmentEditorDialog(@Nullable Investment existingInvestment,
            @Nullable VoiceInvestmentParser.ParsedInvestment voicePrefill) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(existingInvestment == null
                ? R.string.bucket_detail_investment_dialog_add_title
                : R.string.bucket_detail_investment_dialog_edit_title);

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_investment_editor, null, false);
        TextInputLayout nameInputLayout = dialogView.findViewById(R.id.input_investment_name_layout);
        TextInputEditText nameInput = dialogView.findViewById(R.id.input_investment_name);
        Spinner assetSpinner = dialogView.findViewById(R.id.spinner_asset_type);
        TextInputLayout investedInputLayout = dialogView.findViewById(R.id.input_invested_layout);
        TextInputEditText investedInput = dialogView.findViewById(R.id.input_invested);
        TextInputEditText currentValueInput = dialogView.findViewById(R.id.input_current);
        TextInputEditText jobTitleInput = dialogView.findViewById(R.id.input_job_title);
        TextInputEditText platformInput = dialogView.findViewById(R.id.input_platform);

        ArrayAdapter<String> assetAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                ASSET_TYPE_LABELS);
        assetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        assetSpinner.setAdapter(assetAdapter);

        if (existingInvestment != null) {
            nameInput.setText(existingInvestment.getName());
            investedInput.setText(formatAmountForInput(existingInvestment.getInvestedAmountCents()));
            currentValueInput.setText(formatAmountForInput(existingInvestment.getCurrentValueCents()));
            if (existingInvestment.getJobTitle() != null) {
                jobTitleInput.setText(existingInvestment.getJobTitle());
            }
            if (existingInvestment.getPlatform() != null) {
                platformInput.setText(existingInvestment.getPlatform());
            }
            assetSpinner.setSelection(getAssetTypeIndex(existingInvestment.getAssetType()));
        } else if (voicePrefill != null) {
            if (voicePrefill.investmentName != null) {
                nameInput.setText(voicePrefill.investmentName);
            }
            if (voicePrefill.platform != null) {
                platformInput.setText(voicePrefill.platform);
            }
            if (voicePrefill.amountCents > 0) {
                String amountText = formatAmountForInput(voicePrefill.amountCents);
                investedInput.setText(amountText);
                currentValueInput.setText(amountText);
            }
            assetSpinner.setSelection(getAssetTypeIndex(voicePrefill.assetType));
        }

        builder.setView(dialogView);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(existingInvestment == null
                ? R.string.bucket_detail_investment_save
                : R.string.bucket_detail_investment_update, null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(di -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            if (name.isEmpty()) {
                nameInputLayout.setError(getString(R.string.bucket_detail_investment_error_name));
                return;
            }
            nameInputLayout.setError(null);

            Long investedAmountCents = parseAmountToCents(investedInput.getText().toString().trim());
            Long currentValueCents = parseAmountToCents(currentValueInput.getText().toString().trim());
            if (investedAmountCents == null || currentValueCents == null || investedAmountCents < 0
                    || currentValueCents < 0) {
                investedInputLayout.setError(getString(R.string.bucket_detail_investment_error_amount));
                return;
            }
            investedInputLayout.setError(null);

            String assetType = ASSET_TYPE_VALUES[assetSpinner.getSelectedItemPosition()];
            String jobTitle = normalizeText(jobTitleInput.getText().toString());
            String platform = normalizeText(platformInput.getText().toString());
            long epochDayToday = LocalDate.now().toEpochDay();

            if (existingInvestment == null) {
                Investment newInvestment = new Investment(
                        bucketId,
                        name,
                        assetType,
                        investedAmountCents,
                        currentValueCents,
                        epochDayToday);
                newInvestment.setJobTitle(jobTitle);
                newInvestment.setPlatform(platform);
                viewModel.addInvestment(newInvestment);
                Toast.makeText(requireContext(), R.string.bucket_detail_investment_saved, Toast.LENGTH_SHORT).show();
            } else {
                existingInvestment.setName(name);
                existingInvestment.setAssetType(assetType);
                existingInvestment.setInvestedAmountCents(investedAmountCents);
                existingInvestment.setCurrentValueCents(currentValueCents);
                existingInvestment.setJobTitle(jobTitle);
                existingInvestment.setPlatform(platform);
                existingInvestment.setLastUpdatedEpochDay(epochDayToday);
                viewModel.updateInvestment(existingInvestment);
                Toast.makeText(requireContext(), R.string.bucket_detail_investment_updated, Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void updateSummary(@Nullable List<Investment> investments) {
        long totalValueCents = 0L;
        long totalInvestedCents = 0L;
        int holdingCount = 0;

        if (investments != null) {
            holdingCount = investments.size();
            for (Investment investment : investments) {
                totalValueCents += investment.getCurrentValueCents();
                totalInvestedCents += investment.getInvestedAmountCents();
            }
        }

        textTotalValue.setText(formatCurrency(totalValueCents));
        textTotalInvested.setText(formatCurrency(totalInvestedCents));
        textHoldingsCount.setText(getResources().getQuantityString(
                R.plurals.bucket_detail_holdings_count,
                holdingCount,
                holdingCount));

        long pnlCents = totalValueCents - totalInvestedCents;
        if (totalInvestedCents > 0) {
            double pnlPercent = ((double) pnlCents / totalInvestedCents) * 100.0;
            String sign = pnlCents >= 0 ? "+" : "";
            textTotalPnl.setText(String.format(Locale.getDefault(), "%s%s (%.1f%%)",
                    sign,
                    formatCurrency(pnlCents),
                    pnlPercent));
            textTotalPnl.setTextColor(pnlCents >= 0
                    ? ContextCompat.getColor(requireContext(), R.color.sw_income)
                    : ContextCompat.getColor(requireContext(), R.color.sw_error));
        } else {
            textTotalPnl.setText(formatCurrency(0));
            textTotalPnl.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        }
    }

    private String formatCurrency(long amountCents) {
        return String.format(Locale.getDefault(), "₹%,.0f", amountCents / 100.0);
    }

    private String formatAmountForInput(long amountCents) {
        return String.format(Locale.getDefault(), "%.2f", amountCents / 100.0);
    }

    private Long parseAmountToCents(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return (long) (Double.parseDouble(value) * 100);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizeText(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int getAssetTypeIndex(String assetType) {
        for (int i = 0; i < ASSET_TYPE_VALUES.length; i++) {
            if (ASSET_TYPE_VALUES[i].equals(assetType)) {
                return i;
            }
        }
        return 0;
    }
}
