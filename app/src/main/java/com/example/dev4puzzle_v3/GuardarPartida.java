package com.example.dev4puzzle_v3;

import static com.example.dev4puzzle_v3.AdminSQLiteOpenHelper.TABLE_JUGADORES;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class GuardarPartida extends AppCompatActivity {

    AdminSQLiteOpenHelper db;
    private TextView nombreText;
    private TextView tiempoText;
    public String nombre;
    public String tiempo;
    long longTime;
    Button btnGuardar;
    HomeWatcher mHomeWatcher;
    private boolean isChecked = false;
    private static final int READ_REQUEST_CODE = 42;
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.example.dev4puzzle_v3";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guardar_partida);
        btnGuardar = findViewById(R.id.btnGuardar);

        nombreText = (TextView) findViewById(R.id.nombreJugador);
        tiempoText = (TextView) findViewById(R.id.tiempoJugador);

        nombre = getIntent().getStringExtra("nombre");
        nombreText.setText("Jugador: " + nombre);
        tiempo = getIntent().getStringExtra("tiempo");
        tiempoText.setText("Tiempo de partida: " + tiempo);
        longTime = getIntent().getLongExtra("longTime", 0);
        Log.d("CRONOMETRO", String.valueOf(longTime));

        // Vinculamos el servicio de música
        doBindService();
        Intent music = new Intent();
        music.setClass(this, ServicioMusica.class);
        startService(music);

        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Creación o actualización de la BBDD.
                AdminSQLiteOpenHelper adminSQLiteOpenHelper = new AdminSQLiteOpenHelper(GuardarPartida.this);
                SQLiteDatabase db = adminSQLiteOpenHelper.getWritableDatabase();

                //Cargar datos de la partida en la BBDD.
                ContentValues values = new ContentValues();
                values.put("nombre", nombre);
                values.put("tiempo", tiempo);
                values.put("puntuacion", longTime);
                db.insert(TABLE_JUGADORES, null, values);

                //Añadimos el calendario y su funcionalidad a nuestro botón de "Guardar" partida.
                if(!nombreText.getText().toString().isEmpty() && !tiempoText.getText().toString().isEmpty()){
                    Intent intent = new Intent(Intent.ACTION_INSERT);
                    intent.setData(CalendarContract.Events.CONTENT_URI);
                    intent.putExtra(CalendarContract.Events.TITLE, nombreText.getText().toString());
                    intent.putExtra(CalendarContract.Events.DESCRIPTION, tiempoText.getText().toString());
                    intent.putExtra(CalendarContract.Events.ALL_DAY, true);

                    if(intent.resolveActivity(getPackageManager()) !=null){
                        startActivity(intent);
                    } else {
                        Toast.makeText(GuardarPartida.this, "La app no soporta esta acción", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Toast.makeText(GuardarPartida.this, "Por favor complete los datos de partida", Toast.LENGTH_SHORT).show();
                }

                AlertDialogGuardarPartida();
            }

        });

        // Iniciamos el HomeWatcher
        mHomeWatcher = new HomeWatcher(this);
        mHomeWatcher.setOnHomePressedListener(new HomeWatcher.OnHomePressedListener() {
            @Override
            public void onHomePressed() {
                if (mServ != null) {
                    mServ.pauseMusic();
                }
            }

            @Override
            public void onHomeLongPressed() {
                if (mServ != null) {
                    mServ.pauseMusic();
                }
            }
        });
        mHomeWatcher.startWatch();
    }

    // Vinculamos el servicio de música
    private boolean mIsBound = false;
    private ServicioMusica mServ;
    private ServiceConnection Scon = new ServiceConnection() {

        public void onServiceConnected(ComponentName name, IBinder binder) {
            mServ = ((ServicioMusica.ServiceBinder) binder).getService();
        }

        public void onServiceDisconnected(ComponentName name) {
            mServ = null;
        }
    };

    // Vincular servicio
    void doBindService() {
        bindService(new Intent(this, ServicioMusica.class), Scon, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    // Desvincular servicio
    void doUnbindService() {
        if (mIsBound) {
            unbindService(Scon);
            mIsBound = false;
        }
    }

    //Método para recorrer la lista de puntuaciones almacenadas en la BBDD y compararlas
    /*private void compararResultados() {
        AdminSQLiteOpenHelper db = new AdminSQLiteOpenHelper(this);
        Cursor c = db.getDatos();
        ArrayList<Long> listaPuntuaciones = new ArrayList<Long>();
        Long puntuacion;
        long anterior = 0;

        if (c.moveToFirst()) {
            do {
                puntuacion = c.getLong(3);
                listaPuntuaciones.add(puntuacion);

            } while (c.moveToNext());
        }

        for (int x = 0; x < listaPuntuaciones.size(); x++) {

            if ((listaPuntuaciones.get(x) < anterior)){

                AlertDialog.Builder alertaRecord = new AlertDialog.Builder(this);
                alertaRecord.setMessage("Enhorabuena! Has conseguido un nuevo record! Tu puntuación es:"
                + listaPuntuaciones.get(x).toString() +".");
                break;
            }

            anterior = listaPuntuaciones.get(x);
        }
    }*/


    // Este método reanuda la música
    @Override
    protected void onResume() {
        super.onResume();

        if (mServ != null) {
            mServ.resumeMusic();
        }
    }

    // Este método pone la música en pausa
    @Override
    protected void onPause() {
        super.onPause();

        // Detectamos la pausa de la pantalla
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean isScreenOn = false;
        if (pm != null) {
            isScreenOn = pm.isScreenOn();
        }

        if (!isScreenOn) {
            if (mServ != null) {
                mServ.pauseMusic();
            }
        }
    }

    // Este método desvincula el servicio de música cuando no lo necesitamos
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Desvinculamos el servicio de música
        doUnbindService();
        Intent music = new Intent();
        music.setClass(this, ServicioMusica.class);
        stopService(music);
    }

    public void AlertDialogGuardarPartida() {
        Intent intent = new Intent(this, HallOfFame.class);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("GUARDAR PARTIDA");
        builder.setMessage("¡JUGADOR GUARDADO CON ÉXITO!\n\n" + "Tiempo de partida: " + tiempo + "\nNombre: " + nombre);

        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                startActivity(intent);
            }
        });;

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu miMenu){
        getMenuInflater().inflate(R.menu.overflow, miMenu);
        return true;
    }


    // Este método dispara la acción correspondiente al elegir cada opción del menú.
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ayuda:
                // Se abre la WebView con la ayuda
                Intent ayuda = new Intent(this, Ayuda.class);
                startActivity(ayuda);
                return true;
            case R.id.selector_musica:
                // Se abre el selector de música
                buscarPistaAudio();
                return true;
            case R.id.checkable_menu:
                isChecked = !item.isChecked();
                item.setChecked(isChecked);
                if (isChecked) {
                    AudioManager amanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                } else {
                    AudioManager amanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Este método permite acceder al selector de archivos para que podamos elegir un tema de música.
    public void buscarPistaAudio() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        // Filtramos para que solo muestre los archivos que se pueden abrir.
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri uri;

        if (data != null) {
            uri = data.getData();
            ServicioMusica.audioUri = uri;
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }
    }


}