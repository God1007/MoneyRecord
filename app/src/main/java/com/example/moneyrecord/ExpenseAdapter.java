package com.example.moneyrecord;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moneyrecord.data.Expense;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private final List<Expense> expenses = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
    private String currencySymbol = "";

    public void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
        notifyDataSetChanged();
    }

    public void setExpenses(List<Expense> newExpenses) {
        expenses.clear();
        if (newExpenses != null) {
            expenses.addAll(newExpenses);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenses.get(position);
        holder.titleView.setText(expense.getTitle());
        holder.amountView.setText(formatCurrency(expense.getAmount()));
        holder.dateView.setText(dateFormat.format(new Date(expense.getCreatedAt())));
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    private String formatCurrency(double amount) {
        return String.format(Locale.getDefault(), "%s%.2f", currencySymbol, amount);
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        final TextView titleView;
        final TextView amountView;
        final TextView dateView;

        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.expense_title);
            amountView = itemView.findViewById(R.id.expense_amount);
            dateView = itemView.findViewById(R.id.expense_date);
        }
    }
}
