package com.spendwise.ai.settings;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
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
    private ActivityResultLauncher<Intent> exportLauncher;
    private ActivityResultLauncher<Intent> importLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register export launcher
        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            exportData(uri);
                        }
                    }
                });

        // Register import launcher
        importLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            importData(uri);
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

        // Biometric Lock toggle
        setupBiometricToggle();
    }

    private void setupBiometricToggle() {
        // Check if biometric is available
        boolean available = com.spendwise.ai.security.BiometricHelper.isBiometricAvailable(requireContext());
        binding.cardBiometric.setEnabled(available);
        binding.switchBiometric.setEnabled(available);

        if (!available) {
            binding.switchBiometric.setChecked(false);
            return;
        }

        // Set initial state
        boolean enabled = com.spendwise.ai.security.BiometricHelper.isBiometricEnabled(requireContext());
        binding.switchBiometric.setChecked(enabled);

        binding.switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            com.spendwise.ai.security.BiometricHelper.setBiometricEnabled(requireContext(), isChecked);
            if (isChecked) {
                android.widget.Toast.makeText(requireContext(),
                        "Biometric lock enabled", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDataButtons() {
        binding.cardExport.setOnClickListener(v -> launchExport());
        binding.cardImport.setOnClickListener(v -> launchImport());
    }

    private void launchExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "spendwise_backup.json");
        exportLauncher.launch(intent);
    }

    private void launchImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        importLauncher.launch(intent);
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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
