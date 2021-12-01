package com.example.dev4puzzle_v3;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import static java.lang.Math.abs;

public class PuzzleActivity extends AppCompatActivity {


    AdminSQLiteOpenHelper db;
    ArrayList<PuzzlePiece> pieces;
    String mCurrentPhotoPath;
    String mCurrentPhotoUri;
    HomeWatcher mHomeWatcher;
    private boolean isChecked = false;
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.example.dev4puzzle_v3";
    private static final int READ_REQUEST_CODE = 42;

    Chronometer cronometro;
    String nombreJugador;
    String registroActual;
    long playedTime = 0;

    int facil = 0;
    int intermedio = 0;
    int dificil = 0;
    private long Null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_puzzle);

        final RelativeLayout layout = findViewById(R.id.layout);
        final ImageView imageView = findViewById(R.id.ImageView);

        Intent intent = getIntent();
        final String assetName = intent.getStringExtra("assetName");
        mCurrentPhotoPath = intent.getStringExtra("mCurrentPhotoPath");
        mCurrentPhotoUri = intent.getStringExtra("mCurrentPhotoUri");

        //Se declara el objeto Cronometro para que aparezca en pantalla.
        cronometro = (Chronometer)findViewById(R.id.chronometer);

        // Vinculamos el servicio de música
        doBindService();
        Intent music = new Intent();
        music.setClass(this, ServicioMusica.class);
        startService(music);

        // Ejecuta el código relacionado con la imagen después de que se haya diseñado la vista
        // tener todas las dimensiones calculadas
        imageView.post(new Runnable() {
            @Override
            public void run() {
                //Utiliza la imagen seleccionada en la aplicación.
                if (assetName != null) {
                    setPicFromAsset(assetName, imageView);
                    //Se inicia el cronometro al empezar la partida.
                    cronometro.start();
                //Utiliza la foto realizada con la camara.
                } else if (mCurrentPhotoPath != null) {
                    setPicFromPath(mCurrentPhotoPath, imageView);
                    cronometro.start();
                //Utiliza la galeria de imágenes del teléfono.
                } else if (mCurrentPhotoUri != null) {
                    imageView.setImageURI(Uri.parse(mCurrentPhotoUri));
                    cronometro.start();
                }

                pieces = splitImage();
                TouchListener touchListener = new TouchListener(PuzzleActivity.this);
                // Orden aleatorio de las piezas
                Collections.shuffle(pieces);
                for (PuzzlePiece piece : pieces) {
                    piece.setOnTouchListener(touchListener);
                    layout.addView(piece);
                    // aleatorizar la posición, en la parte inferior de la pantalla
                    RelativeLayout.LayoutParams lParams = (RelativeLayout.LayoutParams) piece.getLayoutParams();
                    lParams.leftMargin = new Random().nextInt(layout.getWidth() - piece.pieceWidth);
                    lParams.topMargin = layout.getHeight() - piece.pieceHeight;
                    piece.setLayoutParams(lParams);
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

    private void setPicFromAsset(String assetName, ImageView imageView) {
        // Obtener las dimensiones de la Vista
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        AssetManager am = getAssets();
        try {
            InputStream is = am.open("img/" + assetName);
            // Obtener las dimensiones del mapa de bits
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, new Rect(-1, -1, -1, -1), bmOptions);
            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // Determina cuánto reducir la imagen
            int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

            is.reset();

            // Decodifica el archivo de imagen en un mapa de bits de tamaño para llenar la vista
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            Bitmap bitmap = BitmapFactory.decodeStream(is, new Rect(-1, -1, -1, -1), bmOptions);
            imageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private ArrayList<PuzzlePiece> splitImage() {

        int piecesNumber =  12;
        int rows = 4;
        int cols = 3;

        // Se recoge el valor de las variables del menú de selección de nivel.
        facil = getIntent().getIntExtra("facil", 0);
        //Se asignan al log de Android studio las variables para que imprima su valor por consola.
        Log.d("NIVEL FACIL", String.valueOf(facil));
        intermedio = getIntent().getIntExtra("intermedio",0);
        Log.d("NIVEL INTERMEDIO", String.valueOf(intermedio));
        dificil = getIntent().getIntExtra("dificil",0 );;
        Log.d("NIVEL DIFICL", String.valueOf(dificil));

        if (facil == 1){
            piecesNumber = 12;
            rows = 4;
            cols = 3;
        } else if (intermedio == 1){
            piecesNumber = 24;
            rows = 6;
            cols = 4;
        } else if (dificil == 1){
            piecesNumber = 54;
            rows = 9;
            cols = 6;
        }

        ImageView imageView = findViewById(R.id.ImageView);
        ArrayList<PuzzlePiece> pieces = new ArrayList<>(piecesNumber);

        // Obtener el mapa de bits escalado de la imagen de origen
        BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
        Bitmap bitmap = drawable.getBitmap();

        int[] dimensions = getBitmapPositionInsideImageView(imageView);
        int scaledBitmapLeft = dimensions[0];
        int scaledBitmapTop = dimensions[1];
        int scaledBitmapWidth = dimensions[2];
        int scaledBitmapHeight = dimensions[3];

        int croppedImageWidth = scaledBitmapWidth - 2 * abs(scaledBitmapLeft);
        int croppedImageHeight = scaledBitmapHeight - 2 * abs(scaledBitmapTop);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledBitmapWidth, scaledBitmapHeight, true);
        Bitmap croppedBitmap = Bitmap.createBitmap(scaledBitmap, abs(scaledBitmapLeft), abs(scaledBitmapTop), croppedImageWidth, croppedImageHeight);

        // Calcula el con y la altura de las piezas
        int pieceWidth = croppedImageWidth/cols;
        int pieceHeight = croppedImageHeight/rows;

        // Crea cada pieza de mapa de bits y agrégala a la matriz resultante
        int yCoord = 0;
        for (int row = 0; row < rows; row++) {
            int xCoord = 0;
            for (int col = 0; col < cols; col++) {
                // calcular el desplazamiento para cada pieza
                int offsetX = 0;
                int offsetY = 0;
                if (col > 0) {
                    offsetX = pieceWidth / 3;
                }
                if (row > 0) {
                    offsetY = pieceHeight / 3;
                }

                // aplica el desplazamiento a cada pieza
                Bitmap pieceBitmap = Bitmap.createBitmap(croppedBitmap, xCoord - offsetX, yCoord - offsetY, pieceWidth + offsetX, pieceHeight + offsetY);
                PuzzlePiece piece = new PuzzlePiece(getApplicationContext());
                piece.setImageBitmap(pieceBitmap);
                piece.xCoord = xCoord - offsetX + imageView.getLeft();
                piece.yCoord = yCoord - offsetY + imageView.getTop();
                piece.pieceWidth = pieceWidth + offsetX;
                piece.pieceHeight = pieceHeight + offsetY;

                // este mapa de bits contendrá la imagen de la pieza final del rompecabezas
                Bitmap puzzlePiece = Bitmap.createBitmap(pieceWidth + offsetX, pieceHeight + offsetY, Bitmap.Config.ARGB_8888);

                // dibujar ruta
                int bumpSize = pieceHeight / 4;
                Canvas canvas = new Canvas(puzzlePiece);
                Path path = new Path();
                path.moveTo(offsetX, offsetY);
                if (row == 0) {
                    // pieza lateral superior
                    path.lineTo(pieceBitmap.getWidth(), offsetY);
                } else {
                    // golpe superior
                    path.lineTo(offsetX + (pieceBitmap.getWidth() - offsetX) / 3, offsetY);
                    path.cubicTo(offsetX + (pieceBitmap.getWidth() - offsetX) / 6, offsetY - bumpSize, offsetX + (pieceBitmap.getWidth() - offsetX) / 6 * 5, offsetY - bumpSize, offsetX + (pieceBitmap.getWidth() - offsetX) / 3 * 2, offsetY);
                    path.lineTo(pieceBitmap.getWidth(), offsetY);
                }

                if (col == cols - 1) {
                    // pieza lateral derecha
                    path.lineTo(pieceBitmap.getWidth(), pieceBitmap.getHeight());
                } else {
                    // golpe a la derecha
                    path.lineTo(pieceBitmap.getWidth(), offsetY + (pieceBitmap.getHeight() - offsetY) / 3);
                    path.cubicTo(pieceBitmap.getWidth() - bumpSize,offsetY + (pieceBitmap.getHeight() - offsetY) / 6, pieceBitmap.getWidth() - bumpSize, offsetY + (pieceBitmap.getHeight() - offsetY) / 6 * 5, pieceBitmap.getWidth(), offsetY + (pieceBitmap.getHeight() - offsetY) / 3 * 2);
                    path.lineTo(pieceBitmap.getWidth(), pieceBitmap.getHeight());
                }

                if (row == rows - 1) {
                    // pieza lateral inferior
                    path.lineTo(offsetX, pieceBitmap.getHeight());
                } else {
                    // golpe de fondo
                    path.lineTo(offsetX + (pieceBitmap.getWidth() - offsetX) / 3 * 2, pieceBitmap.getHeight());
                    path.cubicTo(offsetX + (pieceBitmap.getWidth() - offsetX) / 6 * 5,pieceBitmap.getHeight() - bumpSize, offsetX + (pieceBitmap.getWidth() - offsetX) / 6, pieceBitmap.getHeight() - bumpSize, offsetX + (pieceBitmap.getWidth() - offsetX) / 3, pieceBitmap.getHeight());
                    path.lineTo(offsetX, pieceBitmap.getHeight());
                }

                if (col == 0) {
                    // pieza lateral izquierda
                    path.close();
                } else {
                    // golpe a la izquierda
                    path.lineTo(offsetX, offsetY + (pieceBitmap.getHeight() - offsetY) / 3 * 2);
                    path.cubicTo(offsetX - bumpSize, offsetY + (pieceBitmap.getHeight() - offsetY) / 6 * 5, offsetX - bumpSize, offsetY + (pieceBitmap.getHeight() - offsetY) / 6, offsetX, offsetY + (pieceBitmap.getHeight() - offsetY) / 3);
                    path.close();
                }

                // enmascarar la pieza
                Paint paint = new Paint();
                paint.setColor(0XFF000000);
                paint.setStyle(Paint.Style.FILL);

                canvas.drawPath(path, paint);
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
                canvas.drawBitmap(pieceBitmap, 0, 0, paint);

                // dibuja un borde blanco
                Paint border = new Paint();
                border.setColor(0X80FFFFFF);
                border.setStyle(Paint.Style.STROKE);
                border.setStrokeWidth(8.0f);
                canvas.drawPath(path, border);

                // dibuja un borde negro
                border = new Paint();
                border.setColor(0X80000000);
                border.setStyle(Paint.Style.STROKE);
                border.setStrokeWidth(3.0f);
                canvas.drawPath(path, border);

                // establece el mapa de bits resultante en la pieza
                piece.setImageBitmap(puzzlePiece);

                pieces.add(piece);
                xCoord += pieceWidth;
            }
            yCoord += pieceHeight;
        }

        return pieces;
    }

    private int[] getBitmapPositionInsideImageView(ImageView imageView) {
        int[] ret = new int[4];

        if (imageView == null || imageView.getDrawable() == null)
            return ret;

        // Obtener las dimensiones de la imagen
        // Obtener valores de matriz de imagen y colocarlos en una matriz
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extrae los valores de la escala usando las constantes (si se mantiene la relación de aspecto, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Obtener el elemento dibujable (también podría obtener el mapa de bits detrás del elemento dibujable y getWidth / getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calcula las dimensiones reales
        final int actW = Math.round(origW * scaleX);
        final int actH = Math.round(origH * scaleY);

        ret[2] = actW;
        ret[3] = actH;

        // Obtener la posición de la imagen
        // Suponemos que la imagen está centrada en ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - actH)/2;
        int left = (int) (imgViewW - actW)/2;

        ret[0] = left;
        ret[1] = top;

        return ret;
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

    public void AlertDialog() {
        //Se declara Intent para guardar las variables y pasarlas al Activity guardar partida.
        Intent intent2 = new Intent(this, GuardarPartida.class);
        //Se declara método para que aparezca la ventana de dialogo.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //Se guarda el valor del cronometro en ese momento.
        registroActual=cronometro.getText().toString();
        //Se para el cronometro.
        cronometro.stop();
        playedTime = SystemClock.elapsedRealtime() - cronometro.getBase();
        Log.d("CRONOMETRO", String.valueOf(playedTime));
        intent2.putExtra("longTime", playedTime);
        builder.setTitle("FIN DE LA PARTIDA");
        //Se declara variable para solicitar texto al usuario en ventana.
        final EditText input = new EditText(this);
        builder.setView(input);
        //Falta añadir dato de puntuación y tiempo de partida.
        builder.setMessage("Enhorabuena has resuelto el puzzle!\n" + "Tiempo de resolución: " + registroActual + "\nIngresa tu nombre: ");
        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Se guarda el dato dado por el usuario.
                nombreJugador = input.getText().toString().trim();
                //Imprime en la consola de Android studio el valor de las variables.
                Log.d("NOMBRE JUGADOR", String.valueOf(nombreJugador));
                Log.d("GUARDADO", String.valueOf(registroActual));
                //Recoge el valor introducido por el usuario en el nombre y el tiempo de partida.
                intent2.putExtra("nombre", nombreJugador);
                intent2.putExtra("tiempo", registroActual);
                compararPuntuacion();
                startActivity(intent2);
            }

        });;
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void Enviar(View view){
        Intent intent = new Intent(this, GuardarPartida.class);
        intent.putExtra("nombre", nombreJugador);
        intent.putExtra("tiempo", registroActual);
        startActivity(intent);
    }

    public void checkGameOver() {
        if (isGameOver()) {
            AlertDialog();
            stopService(new Intent(PuzzleActivity.this,ServicioMusica.class));
        }
    }

    private boolean isGameOver() {
        for (PuzzlePiece piece : pieces) {
            if (piece.canMove) {
                return false;
            }
        }

        return true;
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

    private void setPicFromPath(String mCurrentPhotoPath, ImageView imageView) {
        // Obtener las dimensiones de la Vista
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        // Obtener las dimensiones del mapa de bits
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determina cuánto reducir la imagen
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decodifica el archivo de imagen en un mapa de bits de tamaño para llenar la vista
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        Bitmap rotatedBitmap = bitmap;

        // rotar mapa de bits si es necesario
        try {
            ExifInterface ei = new ExifInterface(mCurrentPhotoPath);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotatedBitmap = rotateImage(bitmap, 90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotatedBitmap = rotateImage(bitmap, 180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotatedBitmap = rotateImage(bitmap, 270);
                    break;
            }
        } catch (IOException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }

        imageView.setImageBitmap(rotatedBitmap);
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    private long sacarMenorPuntuaciones(){
        db = new AdminSQLiteOpenHelper(this);
        Cursor c = db.getDatos();
        ArrayList<Long> puntuacion = new ArrayList<Long>();
        long puntuaciones = 0;
        long puntuacionMenor;

        if (c.moveToFirst()) {
            do {
                puntuaciones = c.getLong(3);
                puntuacion.add(puntuaciones);

            } while (c.moveToNext());
        }

        puntuacionMenor = Collections.min(puntuacion);
        Log.d("puntuacionMin", String.valueOf(puntuacionMenor));

        return puntuacionMenor;
    }

    public void compararPuntuacion(){
        long puntuacionBD = 0;
        puntuacionBD = sacarMenorPuntuaciones();
        if (playedTime < puntuacionBD){
            crearCanalNotificacion();
            NotificationCompat.Builder builder = new NotificationCompat.Builder(PuzzleActivity.this, "CHANNEL_NEW_RECORD")
                    .setSmallIcon(R.drawable.notification_icon)
                    .setContentTitle("Dev4Puzzle")
                    .setContentText("¡Enhorabuena, has batido un nuevo récord! " + registroActual);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(PuzzleActivity.this);

            notificationManager.notify(1, builder.build());
        }

    }

    // Este método crea el canal que permite enviar las notificaciones de los récords.
    private void crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("CHANNEL_NEW_RECORD", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

}