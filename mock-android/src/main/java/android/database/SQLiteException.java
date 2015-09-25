/*
 * The MIT License (MIT)
 *
 * Copyright (C) 2012 47 Degrees, LLC http://47deg.com hello@47deg.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 */

package android.database;

public class SQLiteException extends android.database.SQLException {

    public SQLiteException() {
        super();
    }

    public SQLiteException(java.lang.String error) {
        super(error);
    }

    public SQLiteException(java.lang.String error, java.lang.Throwable cause) {
        super(error, cause);
    }
}
