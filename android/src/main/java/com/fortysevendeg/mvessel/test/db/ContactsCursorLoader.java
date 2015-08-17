package com.fortysevendeg.mvessel.test.db;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import com.fortysevendeg.mvessel.Database;

public class ContactsCursorLoader extends CursorLoader {

    private final Database database;

    public ContactsCursorLoader(Context context, Database database) {
        super(context);
        this.database = database;
    }

    @Override
    public Cursor loadInBackground() {
        return database.rawQuery(String.format(
                "SELECT %s, %s, %s FROM %s",
                ContactsOpenHelper.C_ID,
                ContactsOpenHelper.C_NAME,
                ContactsOpenHelper.C_AGE,
                ContactsOpenHelper.TABLE));
    }
}
