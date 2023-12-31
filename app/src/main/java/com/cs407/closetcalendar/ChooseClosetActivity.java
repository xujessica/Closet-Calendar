package com.cs407.closetcalendar;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class ChooseClosetActivity extends AppCompatActivity {

    private int draftID=-1;
    private String outfit=null;


    private ImageView imageView; // ImageView for the outfit
    private Uri pickedImageUri; // Uri of the image of the outfit chosen
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 101;
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 101;

    private ImageButton cameraButton; // button to open camera

    // ActivityResultLauncher for handling camera result
    private final ActivityResultLauncher<Void> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(), result -> {
                if (result != null) {
                    Log.d("Camera", result.toString());
                    imageView.setImageBitmap(result);
                    outfit = getImageUri(getApplicationContext(), result).toString();
                    Log.d("Camera", outfit);
                }
            }
    );

    private ImageButton albumButton;
    private ActivityResultLauncher<Intent> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        this.pickedImageUri = data.getData();
                        imageView.setImageURI(pickedImageUri);
                        this.outfit = pickedImageUri.toString();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_closet);

        SharedPreferences sharedPreferences = getSharedPreferences("<com.cs407.closetcalendar>", Context.MODE_PRIVATE);
        draftID = sharedPreferences.getInt("draftIDKey", -1); // extract draftID (should exist)

        imageView = findViewById(R.id.outfitImageViewChoose);
        cameraButton = findViewById(R.id.cameraButtonChoose);

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(ChooseClosetActivity.this,
                        Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                } else {
                    ActivityCompat.requestPermissions(ChooseClosetActivity.this,
                            new String[]{Manifest.permission.CAMERA},
                            CAMERA_PERMISSION_REQUEST_CODE);
                }
            }
        });

        // Choose from album/camera roll
        albumButton = findViewById(R.id.albumButtonChoose);
        albumButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openFilePicker();
            }
        });
    }

    /**
     * Lets user choose image from the phone's file directory. Launches photoPickerLauncher to save
     * image to the database and display it in the activity's ImageView
     */
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // needed to grant uri permissions

        // Set the initial directory
        File initialDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CameraX");
        Uri initialUri = Uri.fromFile(initialDirectory);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, initialUri);

        photoPickerLauncher.launch(intent);
    }


    public void onClickExitButtonChoose(View view){
        Intent intent = new Intent(this, NewEntryActivity.class);

        //don't save outfit string to the draftID database

        startActivity(intent);
    }

    public void onClickSaveButtonChoose(View view){
        Intent intent = new Intent(this, NewEntryActivity.class);

        //save the outfit string to the draftID database
        if(outfit!=null){
            DBHelper dbHelper = new DBHelper(getApplicationContext());
            dbHelper.updateOutfit(draftID, outfit);
        }

        startActivity(intent);
    }


    // camera functionality
    public void openCamera() {
        // Using the new cameraLauncher to start the camera activity
        cameraLauncher.launch(null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            }
        }
    }

    // reference: https://iqcode.com/code/other/how-to-convert-bitmap-to-uri-in-android
    public Uri getImageUri(Context context, Bitmap inImage) {

        // Check if the permission is granted
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(ChooseClosetActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            return null;
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }
}