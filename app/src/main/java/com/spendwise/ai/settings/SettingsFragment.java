package com.spendwise.ai.settings;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.spendwise.ai.databinding.FragmentSettingsBinding;

/**
 * Fragment for app settings including dark mode, import/export.
 */
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SettingsViewModel viewModel;

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

        setupDarkModeToggle();
        setupDataButtons();
        setupAppVersion();
        observeData();
    }

    private void setupDarkModeToggle() {
        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setDarkModeEnabled(isChecked);
        });
    }

    private void setupDataButtons() {
        binding.cardExport.setOnClickListener(v -> {
            // TODO: Implement export with Storage Access Framework
            Toast.makeText(requireContext(), "Export feature coming soon", Toast.LENGTH_SHORT).show();
        });

        binding.cardImport.setOnClickListener(v -> {
            // TODO: Implement import with Storage Access Framework
            Toast.makeText(requireContext(), "Import feature coming soon", Toast.LENGTH_SHORT).show();
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
