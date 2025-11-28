package com.example.moneyrecord;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moneyrecord.data.Expense;
import com.example.moneyrecord.data.ExpenseDao;
import com.example.moneyrecord.data.MoneyDatabase;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String PREF_FILE = "money_record_prefs";
    private static final String KEY_BUDGET = "pref_budget";
    private static final String KEY_CURRENCY = "pref_currency";

    private EditText descriptionInput;
    private EditText amountInput;
    private EditText budgetInput;
    private EditText currencyInput;
    private TextView totalExpensesView;
    private TextView budgetStatusView;
    private TextView emptyStateView;
    private RecyclerView expenseList;

    private ExpenseDao expenseDao;
    private ExpenseAdapter adapter;
    private SharedPreferences preferences;
    private ExecutorService databaseExecutor;
    private double currentTotal = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        preferences = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
        databaseExecutor = Executors.newSingleThreadExecutor();
        expenseDao = MoneyDatabase.getInstance(getApplicationContext()).expenseDao();

        setupViews();
        setupRecyclerView();
        loadPreferences();
        observeExpenses();
    }

    private void setupViews() {
        descriptionInput = findViewById(R.id.description_input);
        amountInput = findViewById(R.id.amount_input);
        budgetInput = findViewById(R.id.budget_input);
        currencyInput = findViewById(R.id.currency_input);
        totalExpensesView = findViewById(R.id.total_expenses);
        budgetStatusView = findViewById(R.id.budget_status);
        emptyStateView = findViewById(R.id.expense_empty_state);
        expenseList = findViewById(R.id.expense_list);

        Button addExpenseButton = findViewById(R.id.add_expense_button);
        Button savePreferencesButton = findViewById(R.id.save_preferences_button);

        addExpenseButton.setOnClickListener(v -> addExpense());
        savePreferencesButton.setOnClickListener(v -> savePreferences());
    }

    private void setupRecyclerView() {
        expenseList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExpenseAdapter();
        adapter.setCurrencySymbol(getCurrencySymbol());
        expenseList.setAdapter(adapter);
    }

    private void loadPreferences() {
        float savedBudget = preferences.getFloat(KEY_BUDGET, 0f);
        String savedCurrency = getCurrencySymbol();

        if (savedBudget > 0) {
            budgetInput.setText(String.format(Locale.getDefault(), "%.2f", savedBudget));
        }
        currencyInput.setText(savedCurrency);
    }

    private void observeExpenses() {
        expenseDao.getAllExpenses().observe(this, expenses -> {
            adapter.setExpenses(expenses);
            if (expenses == null || expenses.isEmpty()) {
                emptyStateView.setVisibility(View.VISIBLE);
                expenseList.setVisibility(View.GONE);
            } else {
                emptyStateView.setVisibility(View.GONE);
                expenseList.setVisibility(View.VISIBLE);
            }
            updateTotals(expenses);
        });
    }

    private void addExpense() {
        String title = descriptionInput.getText().toString().trim();
        String amountText = amountInput.getText().toString().trim();

        if (title.isEmpty()) {
            descriptionInput.setError(getString(R.string.description_hint));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            amountInput.setError(getString(R.string.amount_hint));
            return;
        }

        if (amount <= 0) {
            amountInput.setError(getString(R.string.amount_hint));
            return;
        }

        Expense expense = new Expense(title, amount, System.currentTimeMillis());
        databaseExecutor.execute(() -> expenseDao.insert(expense));

        descriptionInput.setText("");
        amountInput.setText("");
        Toast.makeText(this, R.string.expense_added, Toast.LENGTH_SHORT).show();
    }

    private void savePreferences() {
        String currency = currencyInput.getText().toString().trim();
        String budgetText = budgetInput.getText().toString().trim();

        if (currency.isEmpty()) {
            currency = "$";
        }

        float budgetValue = 0f;
        if (!budgetText.isEmpty()) {
            try {
                budgetValue = Float.parseFloat(budgetText);
            } catch (NumberFormatException e) {
                budgetInput.setError(getString(R.string.budget_hint));
                return;
            }
        }

        preferences.edit()
                .putString(KEY_CURRENCY, currency)
                .putFloat(KEY_BUDGET, budgetValue)
                .apply();

        adapter.setCurrencySymbol(currency);
        updateBudgetStatus();
        totalExpensesView.setText(formatCurrency(currentTotal));

        Toast.makeText(this, R.string.preferences_saved, Toast.LENGTH_SHORT).show();
    }

    private void updateTotals(List<Expense> expenses) {
        currentTotal = 0.0;
        if (expenses != null) {
            for (Expense expense : expenses) {
                currentTotal += expense.getAmount();
            }
        }
        totalExpensesView.setText(formatCurrency(currentTotal));
        updateBudgetStatus();
    }

    private void updateBudgetStatus() {
        float budget = preferences.getFloat(KEY_BUDGET, 0f);
        if (budget <= 0) {
            budgetStatusView.setText(R.string.budget_not_set);
            budgetStatusView.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            return;
        }

        double remaining = budget - currentTotal;
        if (remaining < 0) {
            budgetStatusView.setText(R.string.budget_exceeded);
            budgetStatusView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        } else {
            String formattedRemaining = formatCurrency(remaining);
            String message = getString(R.string.budget_remaining, formattedRemaining);
            budgetStatusView.setText(message);
            budgetStatusView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        }
    }

    private String formatCurrency(double value) {
        return String.format(Locale.getDefault(), "%s%.2f", getCurrencySymbol(), value);
    }

    private String getCurrencySymbol() {
        return preferences.getString(KEY_CURRENCY, "$");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        databaseExecutor.shutdown();
    }
}
