package com.example.dev4puzzle_v3;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

public class MenuPrincipal extends AppCompatActivity implements View.OnClickListener {

    Button seleccionarimagen;
    Button hallOfFame;
    Button encenderMusica;
    Button apagarMusica;
    HomeWatcher mHomeWatcher;
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.example.dev4puzzle_v3";
    private static final int READ_REQUEST_CODE = 42;
    private boolean isChecked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_principal);

        seleccionarimagen = findViewById(R.id.seleccionarimagen);
        hallOfFame = findViewById(R.id.halloffame);
        // encenderMusica = findViewById(R.id.buttonOn);
        // apagarMusica = findViewById(R.id.buttonOff);

        seleccionarimagen.setOnClickListener(this);
        hallOfFame.setOnClickListener(this);
        // encenderMusica.setOnClickListener(this);
        // apagarMusica.setOnClickListener(this);

        //Empieza la música al iniciar la aplicación.
        //startService(new Intent(MenuPrincipal.this,ServicioMusica.class));

        // Vinculamos el servicio de música
        doBindService();
        Intent music = new Intent();
        music.setClass(this, ServicioMusica.class);
        startService(music);

        //Configuración del botón de activación de música.
        Switch sw = (Switch) findViewById(R.id.musica);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    AudioManager amanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                } else {
                    AudioManager amanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                }
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



    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.seleccionarimagen:
                startActivity(new Intent(getApplicationContext(),MainActivity.class));
                break;
            case R.id.halloffame:
                startActivity(new Intent(getApplicationContext(),HallOfFame.class));
                break;
            //Botones que habilitan el apagado y encendido de música.
            /*
            case R.id.buttonOn:
                startService(new Intent(MenuPrincipal.this,ServicioMusica.class));
                break;
            case R.id.buttonOff:
                stopService(new Intent(MenuPrincipal.this,ServicioMusica.class));
                break;*/
        }

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