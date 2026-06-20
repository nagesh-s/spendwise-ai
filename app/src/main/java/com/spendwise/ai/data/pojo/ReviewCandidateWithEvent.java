package com.spendwise.ai.data.pojo;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.spendwise.ai.data.entity.IngestionEvent;
import com.spendwise.ai.data.entity.ReviewCandidate;

public class ReviewCandidateWithEvent {
    @Embedded
    public ReviewCandidate candidate;

    @Relation(parentColumn = "ingestionEventId", entityColumn = "id")
    public IngestionEvent ingestionEvent;
}
