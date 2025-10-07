package com.am.register.util;

import com.am.register.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Serializes and deserializes transactions to/from JSON.
 * Used for suspending transactions to database.
 */
public class TransactionSerializer {

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    /**
     * Serializes a list of TransactionItems to JSON.
     */
    public static String serializeItems(List<TransactionItem> items) {
        return gson.toJson(items);
    }

    /**
     * Deserializes JSON back to list of TransactionItems.
     */
    public static List<TransactionItem> deserializeItems(String json) {
        Type listType = new TypeToken<List<TransactionItem>>(){}.getType();
        return gson.fromJson(json, listType);
    }

    /**
     * Creates a SuspendedTransaction from an active Transaction.
     */
    public static SuspendedTransaction createSuspension(
            Transaction transaction,
            String suspensionId,
            String note) {

        SuspendedTransaction suspension = new SuspendedTransaction();
        suspension.setSuspensionId(suspensionId);
        suspension.setSuspendedAt(LocalDateTime.now());
        suspension.setTransactionState(transaction.getState().name());
        suspension.setSubtotal(transaction.getSubtotal());
        suspension.setTax(transaction.getTaxAmount());
        suspension.setTotal(transaction.getTotal());
        suspension.setItemCount(transaction.getItemCount());
        suspension.setItemsJson(serializeItems(transaction.getItems()));
        suspension.setNote(note);

        return suspension;
    }

    /**
     * Restores a Transaction from a SuspendedTransaction.
     */
    public static Transaction restoreTransaction(SuspendedTransaction suspension) {
        Transaction transaction = new Transaction();

        // Restore items
        List<TransactionItem> items = deserializeItems(suspension.getItemsJson());

        // Manually rebuild transaction (can't directly set private list)
        for (TransactionItem txItem : items) {
            // Add each item with its quantity
            for (int i = 0; i < txItem.getQuantity(); i++) {
                transaction.addItem(txItem.getItem());
            }
        }

        // Restore state
        TransactionState state = TransactionState.valueOf(suspension.getTransactionState());
        if (state == TransactionState.TENDERING) {
            transaction.startTendering();
        }

        return transaction;
    }
}