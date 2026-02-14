package com.spendwise.ai.settings;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.spendwise.ai.data.DataBackupManager;
import com.spendwise.ai.databinding.FragmentSettingsBinding;

/**
 * Fragment for app settings including dark mode, import/export.
 */
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SettingsViewModel viewModel;
    private DataBackupManager backupManager;

    // SAF launchers for file operations
    private ActivityResultLauncher<Intent> jsonBackupLauncher;
    private ActivityResultLauncher<Intent> jsonRestoreLauncher;
    private ActivityResultLauncher<Intent> csvExportLauncher;
    private ActivityResultLauncher<Intent> pdfExportLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register export launcher
        // Register JSON backup launcher
        jsonBackupLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            exportData(uri);
                        }
                    }
                });

        // Register JSON restore launcher
        jsonRestoreLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            importData(uri);
                        }
                    }
                });

        // Register CSV export launcher
        csvExportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            viewModel.exportCsv(uri);
                        }
                    }
                });

        // Register PDF export launcher
        pdfExportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            viewModel.exportPdf(uri);
                        }
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        backupManager = new DataBackupManager(requireContext());

        setupDarkModeToggle();
        setupFeatureNavigation();
        setupDataButtons();
        setupAppVersion();
        observeData();
    }

    private void setupDarkModeToggle() {
        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setDarkModeEnabled(isChecked);
        });
    }

    private void setupFeatureNavigation() {
        // Quick Tiles
        binding.cardQuickTiles.setOnClickListener(v -> {
            androidx.navigation.Navigation.findNavController(v)
                    .navigate(com.spendwise.ai.R.id.navigation_quick_tiles);
        });

        // Recurring Expenses
        binding.cardRecurring.setOnClickListener(v -> {
            androidx.navigation.Navigation.findNavController(v)
                    .navigate(com.spendwise.ai.R.id.navigation_recurring);
        });

        // Financial Goals
        binding.cardGoals.setOnClickListener(v -> {
            androidx.navigation.Navigation.findNavController(v)
                    .navigate(com.spendwise.ai.R.id.navigation_goals);
        });

        // Analytics Dashboard
        binding.cardAnalytics.setOnClickListener(v -> {
            androidx.navigation.Navigation.findNavController(v)
                    .navigate(com.spendwise.ai.R.id.navigation_analytics);
        });

        // Auto Transaction Reader
        binding.cardAutoTransactions.setOnClickListener(v -> {
            androidx.navigation.Navigation.findNavController(v)
                    .navigate(com.spendwise.ai.R.id.navigation_auto_transactions);
        });

        // Biometric Lock toggle
        setupBiometricToggle();
    }

    private void setupBiometricToggle() {
        boolean capability = com.spendwise.ai.security.BiometricHelper.hasBiometricCapability(requireContext());
        binding.cardBiometric.setEnabled(true);
        binding.switchBiometric.setEnabled(capability);

        // Set initial state
        boolean enabled = com.spendwise.ai.security.BiometricHelper.isBiometricEnabled(requireContext());
        binding.switchBiometric.setChecked(enabled);

        if (!capability) {
            binding.switchBiometric.setChecked(false);
            com.spendwise.ai.security.BiometricHelper.setBiometricEnabled(requireContext(), false);
            binding.cardBiometric.setOnClickListener(v -> Toast.makeText(
                    requireContext(),
                    getString(com.spendwise.ai.R.string.biometric_not_supported),
                    Toast.LENGTH_LONG).show());
            return;
        }

        binding.cardBiometric.setOnClickListener(v -> binding.switchBiometric.toggle());
        binding.switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !com.spendwise.ai.security.BiometricHelper.isBiometricAvailable(requireContext())) {
                binding.switchBiometric.setChecked(false);
                Toast.makeText(requireContext(),
                        getString(com.spendwise.ai.R.string.biometric_setup_required),
                        Toast.LENGTH_LONG).show();
                openSecuritySettings();
                return;
            }

            com.spendwise.ai.security.BiometricHelper.setBiometricEnabled(requireContext(), isChecked);
            if (isChecked) {
                android.widget.Toast.makeText(requireContext(),
                        "Biometric lock enabled", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openSecuritySettings() {
        Intent enrollIntent = new Intent(Settings.ACTION_BIOMETRIC_ENROLL);
        enrollIntent.putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
                        | androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL);
        try {
            startActivity(enrollIntent);
        } catch (Exception ignored) {
            startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
        }
    }

    private void setupDataButtons() {
        binding.cardExport.setOnClickListener(v -> launchJsonBackup());
        binding.cardImport.setOnClickListener(v -> launchJsonRestore());

        binding.cardExportCsv.setOnClickListener(v -> launchCsvExport());
        binding.cardExportPdf.setOnClickListener(v -> launchPdfExport());
    }

    private void launchJsonBackup() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "spendwise_backup.json");
        jsonBackupLauncher.launch(intent);
    }

    private void launchJsonRestore() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        jsonRestoreLauncher.launch(intent);
    }

    private void launchCsvExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "spendwise_report.csv");
        csvExportLauncher.launch(intent);
    }

    private void launchPdfExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, "spendwise_report.pdf");
        pdfExportLauncher.launch(intent);
    }

    private void exportData(Uri destinationUri) {
        Toast.makeText(requireContext(), "Exporting data...", Toast.LENGTH_SHORT).show();

        backupManager.exportData(destinationUri, new DataBackupManager.BackupCallback() {
            @Override
            public void onSuccess(String message) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void importData(Uri sourceUri) {
        Toast.makeText(requireContext(), "Importing data...", Toast.LENGTH_SHORT).show();

        backupManager.importData(sourceUri, new DataBackupManager.BackupCallback() {
            @Override
            public void onSuccess(String message) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void setupAppVersion() {
        try {
            PackageInfo pInfo = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            binding.textAppVersion.setText("Version " + pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            binding.textAppVersion.setText("Version 1.0.0");
        }
    }

    private void observeData() {
        // Observe dark mode state
        viewModel.getDarkModeEnabled().observe(getViewLifecycleOwner(), enabled -> {
            binding.switchDarkMode.setChecked(enabled != null && enabled);
        });

        viewModel.getExportStatus().observe(getViewLifecycleOwner(), status -> {
            if (status != null) {
                Toast.makeText(requireContext(), status, Toast.LENGTH_LONG).show();
                viewModel.clearStatus();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
