package app.fedilab.nitterizeme.sqlite;
/* Copyright 2020 Thomas Schneider
 *
 * This file is a part of UntrackMe
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * UntrackMe is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with UntrackMe; if not,
 * see <http://www.gnu.org/licenses>. */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

import app.fedilab.nitterizeme.helpers.Utils;

public class DefaultAppDAO {

    public Context context;
    private SQLiteDatabase db;


    public DefaultAppDAO(Context context, SQLiteDatabase db) {
        this.context = context;
        this.db = db;
    }

    public long insert(String packageName, ArrayList<String> concurrentPackages) {
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_DEFAULT_PACKAGE, packageName.trim());
        values.put(Sqlite.COL_CONCURRENT_PACKAGES, Utils.arrayToString(concurrentPackages));
        try {
            return db.insert(Sqlite.TABLE_DEFAULT_APPS, null, values);
        } catch (Exception ignored) {
            return -1;
        }
    }

    public void update(String packageName, ArrayList<String> concurrentPackages) {
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_CONCURRENT_PACKAGES, Utils.arrayToString(concurrentPackages));
        try {
            db.update(Sqlite.TABLE_DEFAULT_APPS, values, Sqlite.COL_DEFAULT_PACKAGE + " =  ? ", new String[]{packageName});
        } catch (Exception ignored) {
        }
    }

    public int removeApp(String packageName) {
        return db.delete(Sqlite.TABLE_DEFAULT_APPS, Sqlite.COL_DEFAULT_PACKAGE + " = \"" + packageName + "\"", null);
    }

    public int removeAll() {
        return db.delete(Sqlite.TABLE_DEFAULT_APPS, null, null);
    }

    public ArrayList<String> getConcurrent(String packageName) {
        try {
            Cursor c = db.query(Sqlite.TABLE_DEFAULT_APPS, null, Sqlite.COL_DEFAULT_PACKAGE + " = '" + packageName + "'", null, null, null, null, null);
            return cursorGetConcurrent(c);
        } catch (Exception e) {
            return null;
        }
    }

    public ArrayList<String> getDefault() {
        try {
            Cursor c = db.query(Sqlite.TABLE_DEFAULT_APPS, null, null, null, null, null, null, null);
            return cursorGetDefaults(c);
        } catch (Exception e) {
            return null;
        }
    }

    public String getDefault(ArrayList<String> packageNames) {
        try {
            Cursor c = db.query(Sqlite.TABLE_DEFAULT_APPS, null, Sqlite.COL_DEFAULT_PACKAGE + " IN ( " + Utils.arrayToString(packageNames) + ")", null, null, null, null, null);
            return getBestMatchIfExists(c, packageNames);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isPresent(String packageName) {
        Cursor mCount = db.rawQuery("select count(*) from " + Sqlite.TABLE_DEFAULT_APPS
                + " where " + Sqlite.COL_DEFAULT_PACKAGE + " = '" + packageName + "'", null);
        mCount.moveToFirst();
        int count = mCount.getInt(0);
        mCount.close();
        return count > 0;
    }

    private ArrayList<String> cursorGetConcurrent(Cursor c) {
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        ArrayList<String> items = null;
        if (c.moveToFirst()) {
            String concurrentStr = c.getString(c.getColumnIndex(Sqlite.COL_CONCURRENT_PACKAGES));
            items = Utils.stringToArray(concurrentStr);
        }
        c.close();
        return items;
    }


    private ArrayList<String> cursorGetDefaults(Cursor c) {
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        ArrayList<String> items = new ArrayList<>();
        while (c.moveToNext()) {
            String packageName = c.getString(c.getColumnIndex(Sqlite.COL_DEFAULT_PACKAGE));
            items.add(packageName);
        }
        c.close();
        return items;
    }


    private String getBestMatchIfExists(Cursor c, ArrayList<String> allPackages) {
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        ArrayList<String> concurrent = new ArrayList<>();
        while (c.moveToNext()) {
            String packageName = c.getString(c.getColumnIndex(Sqlite.COL_DEFAULT_PACKAGE));
            concurrent.addAll(Utils.stringToArray(packageName));
        }
        c.close();
        //Items will only returns concurrent for default apps.
        ArrayList<String> best = Utils.intersection(allPackages, concurrent);
        //The winner will be the one not in concurrent
        if (best.size() == 1) {
            return best.get(0);
        } else return null;
    }

}
