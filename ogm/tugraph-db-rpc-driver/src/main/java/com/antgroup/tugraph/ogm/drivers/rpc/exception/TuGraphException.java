/*
 * Modifications Copyright 2022 "Ant Group"
 * Copyright (c) 2002-2022 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.antgroup.tugraph.ogm.drivers.rpc.exception;

public class TuGraphException extends RuntimeException {
    private static final long serialVersionUID = -80579062276712566L;
    private final String code;

    public TuGraphException(String message) {
        this("N/A", message);
    }

    public TuGraphException(String message, Throwable cause) {
        this("N/A", message, cause);
    }

    public TuGraphException(String code, String message) {
        this(code, message, (Throwable) null);
    }

    public TuGraphException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /** @deprecated */
    @Deprecated
    public String tugraphErrorCode() {
        return this.code;
    }

    public String code() {
        return this.code;
    }
}
