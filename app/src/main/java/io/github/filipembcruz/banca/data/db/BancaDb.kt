package io.github.filipembcruz.banca.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Plain SQLite. Every schema change after version 1 must be an ALTER/CREATE step in [onUpgrade];
 * never drop user tables (saved, history, sources, topics).
 */
class BancaDb(context: Context) : SQLiteOpenHelper(context, "banca.db", null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE items (
                tab TEXT NOT NULL, id TEXT NOT NULL, title TEXT NOT NULL, link TEXT NOT NULL,
                real_link TEXT, source TEXT NOT NULL, domain TEXT, published INTEGER NOT NULL DEFAULT 0,
                image TEXT, summary TEXT, topic TEXT, related TEXT, position INTEGER NOT NULL DEFAULT 0,
                fetched INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(tab, id))"""
        )
        db.execSQL("CREATE INDEX items_id ON items(id)")
        db.execSQL(
            """CREATE TABLE saved (
                id TEXT PRIMARY KEY, title TEXT NOT NULL, link TEXT NOT NULL, real_link TEXT,
                source TEXT NOT NULL, domain TEXT, published INTEGER NOT NULL DEFAULT 0, image TEXT,
                summary TEXT, topic TEXT, related TEXT, saved_at INTEGER NOT NULL, body TEXT)"""
        )
        db.execSQL(
            """CREATE TABLE history (
                id TEXT PRIMARY KEY, title TEXT NOT NULL, link TEXT NOT NULL, source TEXT NOT NULL,
                domain TEXT, image TEXT, opened_at INTEGER NOT NULL)"""
        )
        db.execSQL("CREATE TABLE hidden (id TEXT PRIMARY KEY, at INTEGER NOT NULL)")
        db.execSQL(
            """CREATE TABLE sources (
                domain TEXT PRIMARY KEY, name TEXT, weight REAL NOT NULL DEFAULT 0,
                blocked INTEGER NOT NULL DEFAULT 0)"""
        )
        db.execSQL("CREATE TABLE topics (topic TEXT PRIMARY KEY, weight REAL NOT NULL DEFAULT 0)")
        db.execSQL("CREATE TABLE notified (id TEXT PRIMARY KEY, at INTEGER NOT NULL, rule TEXT, pending INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("CREATE TABLE links (link TEXT PRIMARY KEY, real TEXT, image TEXT, at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE kv (k TEXT PRIMARY KEY, v TEXT, at INTEGER NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 1 is the first schema. Add migrations here as: if (oldVersion < 2) { db.execSQL(...) }
    }

    companion object {
        const val VERSION = 1
    }
}
