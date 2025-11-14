package com.example.healplusapp.features.anamnese.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AnamneseDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOME TEXT NOT NULL,
                $COL_DATA_CONSULTA TEXT,
                $COL_LOCALIZACAO TEXT,
                $COL_JSON TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    companion object {
        const val DATABASE_NAME = "anamnese.db"
        const val DATABASE_VERSION = 1

        const val TABLE = "anamneses"
        const val COL_ID = "id"
        const val COL_NOME = "nome"
        const val COL_DATA_CONSULTA = "data_consulta"
        const val COL_LOCALIZACAO = "localizacao"
        const val COL_JSON = "dados_json"
    }
}


