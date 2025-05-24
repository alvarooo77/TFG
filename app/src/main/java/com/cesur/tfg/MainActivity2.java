package com.cesur.tfg;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Authenticator;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMultipart;



public class MainActivity2 extends AppCompatActivity {

    private static String emailFrom = "correodepruebacesur@gmail.com";
    private static String passwordFrom = "zgvibtawzpegyycl";
    private Properties mProperties;
    private Session mSession;
    private MimeMessage mCorreo;

    //botones
    private Button buttonSend;
    private FloatingActionButton backMain;
    private EditText editTo;
    private EditText editSubject;
    private EditText editContent;
    private TextView textMiCorreo;
    private FloatingActionButton botonArchivo;
    private TextView nombreArchivo;
    private Uri adjuntoUri = null;
    private String adjuntoNombre = null;

    private ActivityResultLauncher<String> archivoLauncher;


    //Executor para tareas en segundo plano
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    // Handler para publicar resultados en el hilo principal
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        // inicializamos las vistas
        backMain = (FloatingActionButton) findViewById(R.id.backMain);
        buttonSend = (Button) findViewById(R.id.buttonSend);
        editTo = (EditText) findViewById(R.id.editTo);
        editSubject = (EditText) findViewById(R.id.editSubject);
        editContent = (EditText) findViewById(R.id.editContent);
        textMiCorreo = (TextView) findViewById(R.id.textMiCorreo);
        botonArchivo = (FloatingActionButton) findViewById(R.id.botonArchivo);
        nombreArchivo = (TextView) findViewById(R.id.nombreArchivo);

        nombreArchivo.setText("ningun archivo adjunto");

        textMiCorreo.setText(emailFrom);

        backMain.setOnClickListener(            // volver al activity principal
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(MainActivity2.this, MainActivity.class);
                        startActivity(intent);
                    }
                }
        );


        archivoLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        adjuntoUri = uri;
                        adjuntoNombre = obtenerNombreDeUri(adjuntoUri);
                        if (nombreArchivo != null) {
                            nombreArchivo.setText("Archivo: " + adjuntoNombre);
                        } else {
                            nombreArchivo.setText("Archivo: nombre no disponible");
                        }
                        Log.d("MainActivity2", "Archivo adjunto URI: " + adjuntoUri.toString());
                    } else {
                        //si el usuario cancela la sellecion o no se puedo obtener la uri
                        adjuntoUri = null;
                        adjuntoNombre = null;
                        nombreArchivo.setText("Ningun archivo adjunto");
                        Toast.makeText(this, "No se seleccionó ningún archivo.", Toast.LENGTH_SHORT).show();
                    }
                });

        botonArchivo.setOnClickListener(v -> {
            abrirSelectorDeArchivos();
        });




        buttonSend.setOnClickListener(v -> {

            String emailToValue = editTo.getText().toString().trim();
            String subjectValue = editSubject.getText().toString().trim();
            String contentValue = editContent.getText().toString().trim();

            if (emailToValue.isEmpty() || subjectValue.isEmpty() || contentValue.isEmpty()) {
                mostrarDialogoDeMensaje("Campos incompletos", "Por favor, rellena todos los campos.");
                return;
            }
            enviarCorreo(emailToValue, subjectValue, contentValue);
        });
    }
    @SuppressLint("Range")
    private String obtenerNombreDeUri(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")){
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (displayNameIndex != -1) { // Verificar si la columna existe
                        result = cursor.getString(displayNameIndex);
                    }
                }
            } catch (Exception e){

                Log.e("MainActivity2", "Error al obtener el nombre del archivo: ", e);
            }
        }

        if (result == null){
            result = uri.getLastPathSegment();
            if (result != null && result.contains(":")) {
                result = result.substring(result.lastIndexOf(":") + 1);
            }

        }
        return result;
    }


    private void abrirSelectorDeArchivos() {
        archivoLauncher.launch("*/*");
    }

    private void mostrarDialogoDeMensaje(String titulo, String mensaje) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(titulo);
        builder.setMessage(mensaje);
        builder.setPositiveButton("Aceptar", (dialog, which) -> {

            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private String obtenerTipoMimeDeUri(Uri uri){
        String mimeType = null;
        if (uri == null) return "application/octet-stream";

        if(ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())){
            ContentResolver cr = getContentResolver();
            mimeType = cr.getType(uri);
        }else{
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            if (fileExtension != null){
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
            }
        }
        if (mimeType == null){
            mimeType = "application/octet-stream";
        }
        return mimeType;
    }

    private void enviarCorreo(String emailTo, String subject, String content) {

        mainThreadHandler.post(() -> {
            mostrarDialogoDeMensaje("Enviando correo", "Enviando correo por favor espere...");
        });

        executorService.execute(() -> {
            mProperties = new Properties();
            mProperties.put("mail.smtp.host", "smtp.gmail.com");
            mProperties.put("mail.smtp.ssl.trust", "smtp.gmail.com");
            mProperties.setProperty("mail.smtp.starttls.enable", "true");
            mProperties.setProperty("mail.smtp.port", "587");
            mProperties.setProperty("mail.smtp.ssl.protocols", "TLSv1.2");
            mProperties.setProperty("mail.smtp.auth", "true");

            Session mSession = Session.getInstance(mProperties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(emailFrom, passwordFrom);
                }
            });


            MimeMessage mCorreo = new MimeMessage(mSession);
            try {
                mCorreo.setFrom(new InternetAddress(emailFrom));
                mCorreo.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailTo));
                mCorreo.setSubject(subject);



                boolean tieneContenido = content != null && !content.trim().isEmpty();
                boolean tieneArchivo = adjuntoUri != null && adjuntoNombre != null;

                // 1º opcion ambos
                if(tieneContenido && tieneArchivo){
                    Multipart multipart = new MimeMultipart();

                    //parte del texto
                    MimeBodyPart textBodyPart = new MimeBodyPart();
                    textBodyPart.setText(content);
                    multipart.addBodyPart(textBodyPart);

                    //parte del archivo
                    MimeBodyPart attachmentBodyPart = new MimeBodyPart();
                    DataSource source = new UriDataSource(adjuntoUri,
                            obtenerTipoMimeDeUri(adjuntoUri), getContentResolver());
                    attachmentBodyPart.setDataHandler(new DataHandler(source));
                    attachmentBodyPart.setFileName(adjuntoNombre);
                    multipart.addBodyPart(attachmentBodyPart);

                    mCorreo.setContent(multipart);

                    //2º opcion: solo archivo
                }else if (tieneArchivo){
                    Multipart multipart = new MimeMultipart();

                    MimeBodyPart attachmentBodyPart = new MimeBodyPart();
                    DataSource source = new UriDataSource(adjuntoUri,
                            obtenerTipoMimeDeUri(adjuntoUri), getContentResolver());
                    attachmentBodyPart.setDataHandler(new DataHandler(source));
                    attachmentBodyPart.setFileName(adjuntoNombre);
                    multipart.addBodyPart(attachmentBodyPart);

                    mCorreo.setContent(multipart);


                    //3º opcion: solo tiene contenido
                }else if (tieneContenido){
                    mCorreo.setContent(content, "text/html; charset=utf-8");

                    //4º opcion: sin contenido ni archivo
                }else{
                    mCorreo.setText("");
                }

                Transport.send(mCorreo); // Operación de red principal

                // Publicar resultado en el hilo principal
                mainThreadHandler.post(() -> {
                    mostrarDialogoDeMensaje("Correo enviado", "El correo ha sido enviado correctamente");
                    limpiarCampos();
                });


            } catch (final MessagingException e) {
                e.printStackTrace();
                Logger.getLogger(MainActivity2.class.getName()).log(Level.SEVERE, "Error de MessagingException", e);
                // Error: Publicar resultado en el hilo principal
                mainThreadHandler.post(() -> {
                    mostrarDialogoDeMensaje("Error de envío", "No se pudo enviar el correo: " + e.getMessage());
                });
            } catch (Exception e) {
                e.printStackTrace();
            }

        });
    }

    private void limpiarCampos(){
        editTo.setText("");
        editSubject.setText("");
        editContent.setText("");
        adjuntoUri = null;
        adjuntoNombre = null;
        if (nombreArchivo != null){
            nombreArchivo.setText("ningun archivo adjunto");
        }
    }

}