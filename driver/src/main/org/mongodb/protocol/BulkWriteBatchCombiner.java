/*
 * Copyright (c) 2008 - 2014 MongoDB Inc. <http://mongodb.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongodb.protocol;

import org.mongodb.BulkWriteError;
import org.mongodb.BulkWriteException;
import org.mongodb.BulkWriteResult;
import org.mongodb.BulkWriteUpsert;
import org.mongodb.WriteConcern;
import org.mongodb.WriteConcernError;
import org.mongodb.connection.ServerAddress;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static java.util.Arrays.asList;
import static org.mongodb.assertions.Assertions.notNull;

/**
 * This class is not part of the public API.  Do not use!
 */
public class BulkWriteBatchCombiner {
    private final ServerAddress serverAddress;
    private final boolean ordered;
    private final WriteConcern writeConcern;

    private int insertedCount;
    private int updatedCount;
    private int removedCount;
    private int modifiedCount;
    private final Set<BulkWriteUpsert> writeUpserts = new TreeSet<BulkWriteUpsert>(new Comparator<BulkWriteUpsert>() {
        @Override
        public int compare(final BulkWriteUpsert o1, final BulkWriteUpsert o2) {
            return (o1.getIndex() < o2.getIndex()) ? -1 : ((o1.getIndex() == o2.getIndex()) ? 0 : 1);
        }
    });
    private final Set<BulkWriteError> writeErrors = new TreeSet<BulkWriteError>(new Comparator<BulkWriteError>() {
        @Override
        public int compare(final BulkWriteError o1, final BulkWriteError o2) {
            return (o1.getIndex() < o2.getIndex()) ? -1 : ((o1.getIndex() == o2.getIndex()) ? 0 : 1);
        }
    });
    private final List<WriteConcernError> writeConcernErrors = new ArrayList<WriteConcernError>();

    public BulkWriteBatchCombiner(final ServerAddress serverAddress, final boolean ordered, final WriteConcern writeConcern) {
        this.writeConcern = notNull("writeConcern", writeConcern);
        this.ordered = ordered;
        this.serverAddress = notNull("serverAddress", serverAddress);
    }

    public void addResult(final BulkWriteResult result, final IndexMap indexMap) {
        insertedCount += result.getInsertedCount();
        updatedCount += result.getUpdatedCount();
        removedCount += result.getRemovedCount();
        modifiedCount += result.getModifiedCount();
        mergeUpserts(result.getUpserts(), indexMap);
    }

    public void addErrorResult(final BulkWriteException exception, final IndexMap indexMap) {
        addResult(exception.getWriteResult(), indexMap);
        mergeWriteErrors(exception.getWriteErrors(), indexMap);
        mergeWriteConcernError(exception.getWriteConcernError());
    }

    public void addWriteErrorResult(final BulkWriteError writeError, final IndexMap indexMap) {
        notNull("writeError", writeError);
        mergeWriteErrors(asList(writeError), indexMap);
    }

    public void addWriteConcernErrorResult(final WriteConcernError writeConcernError) {
        notNull("writeConcernError", writeConcernError);
        mergeWriteConcernError(writeConcernError);
    }

    public void addErrorResult(final List<BulkWriteError> writeErrors,
                               final WriteConcernError writeConcernError, final IndexMap indexMap) {
        mergeWriteErrors(writeErrors, indexMap);
        mergeWriteConcernError(writeConcernError);
    }

    private void mergeWriteConcernError(final WriteConcernError writeConcernError) {
        if (writeConcernError != null) {
            if (writeConcernErrors.isEmpty()) {
                writeConcernErrors.add(writeConcernError);
            } else if (!writeConcernError.equals(writeConcernErrors.get(writeConcernErrors.size() - 1))) {
                writeConcernErrors.add(writeConcernError);
            }
        }
    }

    private void mergeWriteErrors(final List<BulkWriteError> newWriteErrors, final IndexMap indexMap) {
        for (BulkWriteError cur : newWriteErrors) {
            this.writeErrors.add(new BulkWriteError(cur.getCode(), cur.getMessage(), cur.getDetails(), indexMap.map(cur.getIndex())
            ));
        }
    }

    private void mergeUpserts(final List<BulkWriteUpsert> upserts, final IndexMap indexMap) {
        for (BulkWriteUpsert bulkWriteUpsert : upserts) {
            writeUpserts.add(new BulkWriteUpsert(indexMap.map(bulkWriteUpsert.getIndex()), bulkWriteUpsert.getId()));
        }
    }

    public BulkWriteResult getResult() {
        throwOnError();
        return createResult();
    }

    public boolean shouldStopSendingMoreBatches() {
        return ordered && hasWriteErrors();
    }

    private void throwOnError() {
        if (!writeErrors.isEmpty() || !writeConcernErrors.isEmpty()) {
            throw new BulkWriteException(createResult(),
                                         new ArrayList<BulkWriteError>(writeErrors),
                                         writeConcernErrors.isEmpty() ? null : writeConcernErrors.get(writeConcernErrors.size() - 1),
                                         serverAddress);
        }
    }

    private BulkWriteResult createResult() {
        return writeConcern.isAcknowledged()
               ? new AcknowledgedBulkWriteResult(insertedCount, updatedCount, removedCount, modifiedCount,
                                                 new ArrayList<BulkWriteUpsert>(writeUpserts))
               : new UnacknowledgedBulkWriteResult();
    }

    private boolean hasWriteErrors() {
        return !writeErrors.isEmpty();
    }
}
