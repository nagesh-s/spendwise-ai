package com.spendwise.ai.calendar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.spendwise.ai.common.ExpenseAdapter;
import com.spendwise.ai.common.ExpenseEditorDialogFragment;
import com.spendwise.ai.data.entity.Category;
import com.spendwise.ai.data.entity.Expense;
import com.spendwise.ai.data.repository.CategoryRepository;
import com.spendwise.ai.databinding.FragmentCalendarBinding;
import com.spendwise.ai.util.DateUtils;

import org.threeten.bp.LocalDate;
import org.threeten.bp.YearMonth;
import org.threeten.bp.format.DateTimeFormatter;
import java.util.List;

/**
 * Calendar fragment showing monthly calendar with expense list for selected
 * date.
 */
public class CalendarFragment extends Fragment {

    private FragmentCalendarBinding binding;
    private CalendarViewModel viewModel;
    private ExpenseAdapter expenseAdapter;
    private CategoryRepository categoryRepository;

    private static final DateTimeFormatter DATE_DISPLAY_FORMAT = DateTimeFormatter.ofPattern("EEEE, MMMM d");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CalendarViewModel.class);
        categoryRepository = new CategoryRepository(requireActivity().getApplication());

        setupCalendar();
        setupRecyclerView();
        setupFab();
        observeData();
    }

    private void setupCalendar() {
        MaterialCalendarView calendarView = binding.calendarView;

        // Set to current date
        calendarView.setSelectedDate(CalendarDay.today());

        // Style the calendar for dark mode compatibility
        int textColor = getResources().getColor(
                com.google.android.material.R.color.m3_ref_palette_dynamic_neutral10,
                requireContext().getTheme());

        // Check if dark mode is active
        int nightModeFlags = getResources().getConfiguration().uiMode &
                android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            // Use light text for dark mode
            textColor = getResources().getColor(
                    com.google.android.material.R.color.m3_ref_palette_dynamic_neutral90,
                    requireContext().getTheme());
        }

        // Date selection listener
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            LocalDate localDate = LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
            viewModel.setSelectedDate(localDate);
        });

        // Month change listener
        calendarView.setOnMonthChangedListener((widget, date) -> {
            YearMonth yearMonth = YearMonth.of(date.getYear(), date.getMonth());
            viewModel.setDisplayedMonth(yearMonth);
        });
    }

    private void setupRecyclerView() {
        expenseAdapter = new ExpenseAdapter();
        binding.recyclerExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerExpenses.setAdapter(expenseAdapter);

        // Handle expense click for editing
        expenseAdapter.setOnExpenseClickListener(expense -> {
            openExpenseEditor(expense.getId());
        });
    }

    private void setupFab() {
        binding.fabAddExpense.setOnClickListener(v -> openExpenseEditor(null));
        binding.btnAddExpenseEmpty.setOnClickListener(v -> openExpenseEditor(null));
        binding.btnToday.setOnClickListener(v -> jumpToToday());
    }

    private void jumpToToday() {
        CalendarDay today = CalendarDay.today();
        binding.calendarView.setCurrentDate(today, true);
        binding.calendarView.setSelectedDate(today);
        viewModel.selectToday();
        viewModel.setDisplayedMonth(YearMonth.now());
    }

    private void openExpenseEditor(@Nullable Long expenseId) {
        ExpenseEditorDialogFragment dialog = ExpenseEditorDialogFragment.newInstance(
                expenseId,
                viewModel.getSelectedDateEpochDay());
        dialog.show(getChildFragmentManager(), "expense_editor");
    }

    private void observeData() {
        // Observe selected date
        viewModel.getSelectedDate().observe(getViewLifecycleOwner(), date -> {
            binding.textSelectedDate.setText(date.format(DATE_DISPLAY_FORMAT));
        });

        // Observe expenses for selected date
        viewModel.getSelectedDateExpenses().observe(getViewLifecycleOwner(), expenses -> {
            updateExpensesList(expenses);
        });

        // Load categories for displaying in list
        categoryRepository.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            expenseAdapter.setCategories(categories);
        });

        // Observe daily totals for calendar decorators
        viewModel.getDailyTotals().observe(getViewLifecycleOwner(), dailyTotals -> {
            updateCalendarDecorators(dailyTotals);
        });
    }

    private void updateCalendarDecorators(List<com.spendwise.ai.data.pojo.DailyTotal> dailyTotals) {
        if (dailyTotals == null || dailyTotals.isEmpty())
            return;

        MaterialCalendarView calendarView = binding.calendarView;
        calendarView.removeDecorators();

        // Spending thresholds in cents (₹500 low, ₹2000 high)
        final long LOW_THRESHOLD = 50000; // ₹500
        final long HIGH_THRESHOLD = 200000; // ₹2000

        for (com.spendwise.ai.data.pojo.DailyTotal daily : dailyTotals) {
            LocalDate date = DateUtils.epochDayToDate(daily.getDateEpochDay());
            CalendarDay calendarDay = CalendarDay.from(date.getYear(), date.getMonthValue(), date.getDayOfMonth());

            SingleDayDecorator decorator = SingleDayDecorator.forAmount(
                    calendarDay,
                    daily.getTotalCents(),
                    LOW_THRESHOLD,
                    HIGH_THRESHOLD);
            calendarView.addDecorator(decorator);
        }
    }

    private void updateExpensesList(List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            binding.recyclerExpenses.setVisibility(View.GONE);
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
            binding.textSelectedDateTotal.setText(DateUtils.formatAmountCents(0));
        } else {
            binding.recyclerExpenses.setVisibility(View.VISIBLE);
            binding.layoutEmptyState.setVisibility(View.GONE);
            expenseAdapter.submitList(expenses);

            // Calculate total
            long total = 0;
            for (Expense expense : expenses) {
                total += expense.getAmountCents();
            }
            binding.textSelectedDateTotal.setText(DateUtils.formatAmountCents(total));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
