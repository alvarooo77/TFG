package com.cesur.tfg;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;

public class MainActivity extends AppCompatActivity  {

    private FloatingActionButton sendEmail;
    private RecyclerView recycler;
    private EmailAdapter adapter;
    private ArrayList<EmailObjeto> listaDatos;

    private static final String TAG = "MainActivity";

    // configuracion del servidor imap
    private static final String IMAP_HOST = "imap.gmail.com";
    private static final String IMAP_PORT = "993";
    private static final String USER_EMAIL = "correodepruebacesur@gmail.com";
    private static final String USER_PASSWORD = "zgvibtawzpegyycl";
    private static final String FOLDER_NAME = "INBOX";

    // ExecutorService para tareas en segundo plano
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    //handler para comunicar con el hilo principal
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        sendEmail = (FloatingActionButton) findViewById(R.id.sendEmail);

        recycler = (RecyclerView) findViewById(R.id.recyclerView);
        recycler.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false));

        //inicializar listaDAtos y adapter
        listaDatos = new ArrayList<>();
        adapter = new EmailAdapter(listaDatos);

        recycler.setAdapter(adapter);
        recycler.setLayoutManager(new LinearLayoutManager(this));


        cargarCorreosReales();

        // ir a la activity para enviar email
        sendEmail.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(MainActivity.this, MainActivity2.class);
                        startActivity(intent);
                    }
                }
        );
    }


    private void cargarCorreosReales(){
        Toast.makeText(this, "Cargando correos ...", Toast.LENGTH_SHORT).show();
        executorService.execute(() -> {
            Store store = null;
            Folder emailFolder = null;
            final ArrayList<EmailObjeto> correosCargados = new ArrayList<>();
            boolean success = false;
            final String[] uiErrorMensaje = {null}; // Array para poder modificarlo en lambda

            try {
                Properties properties = new Properties();
                properties.put("mail.store.protocol", "imaps");
                properties.put("mail.imaps.host", IMAP_HOST);
                properties.put("mail.imap.port",IMAP_PORT);
                properties.put("mail.imap.ssl.enable", "true");
                //timeout
                properties.put("mail.imaps.connectiontimeout", "10000"); // 10 segundos
                properties.put("mail.imaps.timeout", "10000");

                Session emailSession = Session.getInstance(properties);
                emailSession.setDebug(true);

                store = emailSession.getStore("imaps");
                Log.d(TAG, "Conectando con el servidor IMAP...");
                store.connect(USER_EMAIL, USER_PASSWORD);
                Log.d(TAG, "Conectado con el servidor IMAP");

                emailFolder = store.getFolder(FOLDER_NAME);
                if(emailFolder == null || !emailFolder.exists()){
                    throw new MessagingException("Carpeta no valida: " + FOLDER_NAME);
                }
                emailFolder.open(Folder.READ_ONLY); //solo lectura

                Log.d(TAG, "Carpeta '" + FOLDER_NAME + "' abierta. Total de mensajes: " + emailFolder.getMessageCount());

                // Obtener los mensajes
                int messageCount = emailFolder.getMessageCount();
                if (messageCount == 0) {
                    Log.d(TAG, "No hay mensajes en la carpeta.");
                    success = true;

                } else {
                    int start = 1;
                    int end = messageCount;
                    Message[] messages = emailFolder.getMessages(start,end);
                    Log.d(TAG, "Obteniendo mensajes desde " + start + " hasta " + end + ". Total obtenidos: " + messages.length);


                    FetchProfile fp = new FetchProfile();
                    fp.add(FetchProfile.Item.ENVELOPE); // Remitente, destinatario, asunto, fecha
                    fp.add(FetchProfile.Item.FLAGS);    // informa si el mensaje ha sido leido o no, etc
                    emailFolder.fetch(messages, fp);

                    Log.d(TAG, "Procesando " + messages.length + " mensajes...");
                    // Iterar en orden inverso para mostrar los más recientes primero si se quiere

                    for (int i = messages.length - 1; i >= 0; i--) { // Iterar inverso para más recientes primero
                        Message message = messages[i];
                        String subject = message.getSubject();
                        if (subject == null || subject.trim().isEmpty()) {
                            subject = "(Sin Asunto)";
                        }

                        String from = "(Desconocido)";
                        if (message.getFrom() != null && message.getFrom().length > 0) {
                            Address sender = message.getFrom()[0];
                            if (sender instanceof InternetAddress) {
                                from = ((InternetAddress) sender).getPersonal();
                                if (from == null || from.trim().isEmpty()) {
                                    from = ((InternetAddress) sender).getAddress();
                                }
                            } else {
                                from = sender.toString();
                            }
                        }

                        // conversion de la fecha correo a un string con formato
                        String dateStr = "";
                        Date receivedDate = message.getReceivedDate();
                        if (receivedDate != null) {
                            dateStr = dateFormat.format(receivedDate);
                        }

                        correosCargados.add(new EmailObjeto(from, subject, dateStr));
                    }
                    // FIN BUCLE FOR
                    success = true;

                }

            } catch (MessagingException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    if (emailFolder != null && emailFolder.isOpen()) {
                        emailFolder.close(false);
                        Log.d(TAG, "Carpeta cerrada.");
                    }
                    if (store != null && store.isConnected()) {
                        store.close();
                        Log.d(TAG, "Conexión cerrada.");
                    }
                } catch (MessagingException e) {
                    Log.e(TAG, "Error al cerrar recursos de mensajería: " + e.getMessage(), e);
                }
                // FINAL BLOQUE FINALLY
            }
            final boolean finalOpSuccess = success;
            mainThreadHandler.post(() -> {
                if (finalOpSuccess) {
                    if (adapter != null) {
                        adapter.updateData(correosCargados); // Actualiza el adaptador con los datos
                        if (correosCargados.isEmpty()) {
                            Toast.makeText(MainActivity.this, "No hay correos nuevos.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Correos actualizados.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "El adaptador es nulo, no se puede actualizar la UI.");
                    }
                } else {
                    // Hubo un error, mostrar mensaje de error
                    if (uiErrorMensaje[0] != null) {
                        Toast.makeText(MainActivity.this, uiErrorMensaje[0], Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Error desconocido al cargar correos.", Toast.LENGTH_LONG).show();
                    }
                }
            });

        });
    }
}

