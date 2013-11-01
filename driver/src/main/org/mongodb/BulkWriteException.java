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

package org.mongodb;

import org.mongodb.connection.ServerAddress;

import java.util.List;

/**
 * An exception that represents all errors associated with a bulk write operation.
 *
 * @since 3.0
 */
public class BulkWriteException extends MongoException {

    private static final long serialVersionUID = -4345399805987210275L;

    private final BulkWriteResult writeResult;
    private final List<BulkWriteError> errors;
    private final ServerAddress serverAddress;
    private final WriteConcernError writeConcernError;

    /**
     * Constructs a new instance.
     *
     * @param writeResult              the write result
     * @param writeErrors              the list of errors
     * @param writeConcernError        the write concern error
     * @param serverAddress            the server address.
     */
    public BulkWriteException(final BulkWriteResult writeResult, final List<BulkWriteError> writeErrors,
                              final WriteConcernError writeConcernError, final ServerAddress serverAddress) {
        super("Bulk write operation error on server " + serverAddress + ". "
              + (writeErrors.isEmpty() ? "" : "Write errors: " + writeErrors + ". ")
              + (writeConcernError == null ? "" : "Write concern error: " + writeConcernError + ". "));
        this.writeResult = writeResult;
        this.errors = writeErrors;
        this.writeConcernError = writeConcernError;
        this.serverAddress = serverAddress;
    }

    /**
     * The result of all successfully processed write operations.  This will never be null.
     *
     * @return the bulk write result
     */
    public BulkWriteResult getWriteResult() {
        return writeResult;
    }

    /**
     * The list of errors, which will not be null, but may be empty (if the write concern error is not null).
     *
     * @return the list of errors
     */
    public List<BulkWriteError> getWriteErrors() {
        return errors;
    }


    /**
     * The write concern error, which may be null (in which case the list of errors will not be empty).
     *
     * @return the write concern error
     */
    public WriteConcernError getWriteConcernError() {
        return writeConcernError;
    }

    /**
     * The address of the server which performed the bulk write operation.
     *
     * @return the address
     */
    public ServerAddress getServerAddress() {
        return serverAddress;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final BulkWriteException that = (BulkWriteException) o;

        if (!errors.equals(that.errors)) {
            return false;
        }
        if (!serverAddress.equals(that.serverAddress)) {
            return false;
        }
        if (writeConcernError != null ? !writeConcernError.equals(that.writeConcernError) : that.writeConcernError != null) {
            return false;
        }
        if (!writeResult.equals(that.writeResult)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = writeResult.hashCode();
        result = 31 * result + errors.hashCode();
        result = 31 * result + serverAddress.hashCode();
        result = 31 * result + (writeConcernError != null ? writeConcernError.hashCode() : 0);
        return result;
    }
}
