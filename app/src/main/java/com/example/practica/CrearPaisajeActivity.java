package com.example.practica;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.practica.Entitis.Paisaje;
import com.example.practica.Service.PaisajeService;

import com.google.android.gms.maps.GoogleMap;

import java.io.ByteArrayOutputStream;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CrearPaisajeActivity extends AppCompatActivity implements LocationListener {
    private LocationManager mLocationManager;
    private static final int OPEN_GALLERY_REQUEST = 1002;
    private static final int REQUEST_CAMERA = 1;
    String urlImage = "";
    private GoogleMap mMap;
    public Double latitude;
    public Double longitude;
    Paisaje paisaje = new Paisaje();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_paisaje);

        EditText ediname = findViewById(R.id.ediNombre);
        //EditText ediTipo = findViewById(R.id.ediTipo);
        Button crear = findViewById(R.id.btnCrear);
        //sacar cordenadas
        // Solicita los permisos de ubicación si no están concedidos
        if(
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED ||
                        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            String[] permissions = new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
            requestPermissions(permissions, 3000);

        }
        else {
            // configurar frecuencia de actualización de GPS GPSPROMIDER Y NETWORK
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1, this);
            Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            //Log.i("MAIN_APP: Location - ",  "Latitude: " + location.getLatitude());
            if(location != null){
                Log.i("MAIN_APP: Location - ",  "Latitude: " + location.getLatitude());
            }
            else {
                Log.i("MAIN_APP: Location - ",  "Location is null");
            }
        }

        crear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //---sacar coordenadas

                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl("https://647788dc9233e82dd53bd0e9.mockapi.io/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
                PaisajeService service = retrofit.create(PaisajeService.class);

                paisaje.nombre = String.valueOf(ediname.getText());
                //paisaje.tipo = String.valueOf(ediTipo.getText());
                String url = "https://demo-upn.bit2bittest.com/" + urlImage;
                paisaje.foto = url;
                paisaje.setLatitud(latitude);
                paisaje.setLongitud(longitude);

                // Llamar al servicio para guardar el nuevo usuario

                Call<Paisaje> call = service.create(paisaje);

                Log.i("CREAR BOTON ENTRO", "ENTRO");
                call.enqueue(new Callback<Paisaje>() {
                    @Override
                    public void onResponse(Call<Paisaje> call, Response<Paisaje> response) {
                        if (response.isSuccessful()) {

                            Log.i("MAIN::response.isSuccessful()", "EXITOSO");
                        } else {
                            // Manejar el error en caso de que no se haya podido guardar el usuario
                            Log.i("MAIN::error", "else");
                        }
                    }

                    @Override
                    public void onFailure(Call<Paisaje> call, Throwable t) {
                        // Manejar el error de la llamada al servicio
                        Log.i("MAIN::error llamada al servicio", "onFailure");
                    }
                });
                Toast.makeText(getApplicationContext(), "Pokemon Guardado", Toast.LENGTH_SHORT).show();
                //limpiar datos
                ediname.setText("");
                //ediTipo.setText("");
            }
        });
        Button tomarFoto = findViewById(R.id.btnCamara);
        tomarFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                handleOpenCamera();
            }
        });

    }

    private void handleOpenCamera() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // abrir camara
            Log.i("MAIN_APP", "Tiene permisos para abrir la camara");
            abrirCamara();
        } else {
            // solicitar el permiso
            Log.i("MAIN_APP", "No tiene permisos para abrir la camara, solicitando");
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 1000);
            handleOpenCamera();
        }
    }

    private void abrirCamara() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap bitmap = (Bitmap) extras.get("data");

            // Convierte el bitmap a una cadena Base64
            String base64Image = convertBitmapToBase64(bitmap);

            // Resto del código para enviar y guardar la imagen
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://demo-upn.bit2bittest.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            PaisajeService services = retrofit.create(PaisajeService.class);
            Call<PaisajeService.ImageResponse> call = services.saveImage(new PaisajeService.ImageToSave(base64Image));
            call.enqueue(new Callback<PaisajeService.ImageResponse>() {
                @Override
                public void onResponse(Call<PaisajeService.ImageResponse> call, Response<PaisajeService.ImageResponse> response) {
                    if (response.isSuccessful()) {
                        PaisajeService.ImageResponse imageResponse = response.body();
                        urlImage = imageResponse.getUrl();
                        Log.e("APPURL", urlImage);
                    } else {
                        Log.e("Error cargar imagen", response.toString());
                    }
                }

                @Override
                public void onFailure(Call<PaisajeService.ImageResponse> call, Throwable t) {
                    // Error de red o de la API
                    Log.i("Respuesta inactiva", "");
                }
            });
        }
        //galeria
        if (requestCode == OPEN_GALLERY_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();

            // Obtener la ruta del archivo de imagen a partir de la URI
            String imagePath = getPathFromUri(selectedImageUri);
            if (imagePath != null) {
                // Cargar la imagen desde el archivo en un objeto Bitmap
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);

                if (bitmap != null) {
                    // Convierte el bitmap a una cadena Base64
                    String base64Image = convertBitmapToBase64(bitmap);
                    // Resto del código para enviar y guardar la imagen
                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl("https://demo-upn.bit2bittest.com/")
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();

                    PaisajeService services = retrofit.create(PaisajeService.class);
                    Call<PaisajeService.ImageResponse> call = services.saveImage(new PaisajeService.ImageToSave(base64Image));
                    call.enqueue(new Callback<PaisajeService.ImageResponse>() {
                        @Override
                        public void onResponse(Call<PaisajeService.ImageResponse> call, Response<PaisajeService.ImageResponse> response) {
                            if (response.isSuccessful()) {
                                PaisajeService.ImageResponse imageResponse = response.body();
                                urlImage = imageResponse.getUrl();
                                Log.e("NewImageUrl", urlImage);

                                // Después de obtener la nueva URL, puedes continuar con el proceso de guardar el usuario en el mock API
                                // Aquí puedes llamar al servicio mock API y enviar la nueva URL como parte de los datos del usuario
                                paisaje.foto = urlImage;
                                // Resto del código para guardar el usuario en el mock API
                            } else {
                                Log.e("Error cargar imagen", response.toString());
                            }
                        }

                        @Override
                        public void onFailure(Call<PaisajeService.ImageResponse> call, Throwable t) {
                            // Error de red o de la API
                            Log.e("API Failure", t.getMessage());
                        }
                    });
                }
            }
        }

    }

    private String convertBitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, OPEN_GALLERY_REQUEST);
    }

    private String getPathFromUri(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            String imagePath = cursor.getString(columnIndex);
            cursor.close();
            return imagePath;
        }
        return null;
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        //mandar cordenadas actuales
        Log.i("MAIN_APP: Location AND", "Latitude: " + latitude);
        Log.i("MAIN_APP: Location AND", "Longitude: " + longitude);
        //TextView latitud = findViewById(R.id.textLatitud);
        //TextView longitud = findViewById(R.id.textLongitud);
        //latitud.setText(String.valueOf(latitude));
        //longitud.setText(String.valueOf(longitude));
        //cerrar servicio
        mLocationManager.removeUpdates(this);

    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Handle location provider status changes
    }

    @Override
    public void onProviderEnabled(String provider) {
        // Handle when a location provider is enabled
        Log.d("MAIN_APP", "onProviderEnabled: " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("MAIN_APP", "onProviderDisabled: " + provider);
    }
}