package com.spendwise.ai.data.pojo;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.spendwise.ai.data.entity.Holding;
import com.spendwise.ai.data.entity.Provider;

public class HoldingWithProvider {
    @Embedded
    public Holding holding;

    @Relation(
            parentColumn = "providerId",
            entityColumn = "id"
    )
    public Provider provider;
}
