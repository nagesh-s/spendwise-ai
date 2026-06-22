package com.spendwise.ai.data.pojo;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.spendwise.ai.data.entity.Holding;
import com.spendwise.ai.data.entity.InvestmentTransaction;
import com.spendwise.ai.data.entity.Provider;

import java.util.List;

public class HoldingWithTransactionsAndProvider {
    @Embedded
    public Holding holding;

    @Relation(
            parentColumn = "providerId",
            entityColumn = "id"
    )
    public Provider provider;

    @Relation(
            parentColumn = "id",
            entityColumn = "holdingId"
    )
    public List<InvestmentTransaction> transactions;
}
