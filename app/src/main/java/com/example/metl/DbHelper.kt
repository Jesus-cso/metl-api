package com.example.metl

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE productos (" +
                    "id INTEGER PRIMARY KEY, nombre TEXT NOT NULL, descripcion TEXT, " +
                    "precio REAL NOT NULL, stock REAL NOT NULL, proveedor TEXT, municipio TEXT)"
        )
        db.execSQL(
            "CREATE TABLE magueyes (" +
                    "codigo TEXT PRIMARY KEY, variedad TEXT, edad_anios INTEGER, ubicacion TEXT, " +
                    "humedad REAL, temperatura REAL, estado TEXT, nota TEXT)"
        )
        db.execSQL(
            "CREATE TABLE lecturas (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, maguey_codigo TEXT NOT NULL, " +
                    "humedad REAL, temperatura REAL, creado_en TEXT NOT NULL, " +
                    "UNIQUE (maguey_codigo, creado_en), " +
                    "FOREIGN KEY (maguey_codigo) REFERENCES magueyes(codigo) ON DELETE CASCADE)"
        )
        db.execSQL(
            "CREATE TABLE pedidos_locales (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, proveedor_id INTEGER NOT NULL, " +
                    "nota TEXT, items_json TEXT NOT NULL, creado_en TEXT DEFAULT CURRENT_TIMESTAMP, " +
                    "enviado INTEGER NOT NULL DEFAULT 0)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS lecturas")
        db.execSQL("DROP TABLE IF EXISTS magueyes")
        db.execSQL("DROP TABLE IF EXISTS productos")
        db.execSQL("DROP TABLE IF EXISTS pedidos_locales")
        onCreate(db)
    }

    companion object {
        const val DB_NAME = "metl.db"
        const val DB_VERSION = 1
    }
}