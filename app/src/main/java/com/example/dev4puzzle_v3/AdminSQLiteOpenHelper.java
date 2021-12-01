package com.example.dev4puzzle_v3;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;

public class AdminSQLiteOpenHelper extends SQLiteOpenHelper {

    //Se crean las variables obligatorias para la BBDD.
    private static final int DATABASE_VERSION = 1; //Si se quieren actualizar los datos de la BD ++
    protected static final String DATABASE_NOMBRE = "baseDatosPartidas.db";
    public static final String TABLE_JUGADORES = "Partidas";

    public AdminSQLiteOpenHelper(Context context) {
        super(context, DATABASE_NOMBRE, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase BaseDeDatos) {
        //Crea la tabla
        BaseDeDatos.execSQL("CREATE TABLE " + TABLE_JUGADORES + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombre TEXT NOT NULL," +
                "tiempo TEXT NOT NULL," +
                "puntuacion BIGINT NOT NULL)");


        BaseDeDatos.execSQL("INSERT INTO Partidas (nombre, tiempo, puntuacion) VALUES ('Dev4Puzzle','10:10:10','4452258')");

    }

    @Override
    public void onUpgrade(SQLiteDatabase BadeDeDatos, int oldVersion, int newVersion) {
        //Actualiza la BD si la versión se ha modificado.
        String sql = "DROP TABLE IF EXISTS TABLE_JUGADORES";
        BadeDeDatos.execSQL(sql);

        onCreate(BadeDeDatos);

    }

    //Método para recoger los datos de la tabla.
    public Cursor getDatos(){
        String columnas[] = {"id","nombre","tiempo","puntuacion"};
        Cursor c = this.getReadableDatabase().query(TABLE_JUGADORES, columnas, null, null, null, null, "tiempo");
        return c;
    }
}
