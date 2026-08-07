package com.bulldozer_collective.modules.crm.sync.services;

/**
 * What one canonical-linking batch achieved.
 *
 * @param linked   rows attached to a canonical entity in this batch
 * @param moreToDo whether linkable rows remain. Combined with {@code linked}, this is what stops the task from
 *                 rescheduling itself for ever: rows remaining but nothing linked means no progress is being
 *                 made, so the task gives up and lets the next sync try again.
 */
public record CrmLinkProgress(int linked, boolean moreToDo) {

    public boolean shouldContinue() {
        return this.moreToDo && this.linked > 0;
    }
}
