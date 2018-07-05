package com.example.administrator.ocr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public int REQUEST_ID_MULTIPLE_PERMISSIONS = 23;
    public Button btnCamera;
    public Uri mCropImageUri;
    public ImageView imgResult;


    public static final String DATA_PATH = Environment .getExternalStorageDirectory().toString() + "/OCR/";
    public static final String PHOTO_TAKEN = "BF4E84A309EC57F8C023C0A84D85B9C8B136F1B0";
    public static final String TAG = "OCR Image to Text";
    public static final String lang = "eng";
    public boolean _taken;
    public String _path;
    public TextView txtResult;
    public Bitmap  bitmapResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgResult=(ImageView) findViewById(R.id.imgresult);
        txtResult= (TextView) findViewById(R.id.txtresult);

        btnCamera = (Button) findViewById(R.id.btnCamera);
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //start camera and crop image
                CropImage.startPickImageActivity(MainActivity.this);
            }
        });

        createDirIfNotExists();

        GetPermissionDetails();
    }

    public void createDirIfNotExists() {

        String[] paths = new String[] { DATA_PATH, DATA_PATH + "tessdata/" };

        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.v(TAG, "ERROR: Creation of directory " + path + " on sdcard failed");
                    return;
                } else {
                    Log.v(TAG, "Created directory " + path + " on sdcard");
                }
            }

        }

        if (!(new File(DATA_PATH + "tessdata/" + lang + ".traineddata")).exists())
        {
            try
            {

                AssetManager assetManager = getAssets();
                InputStream in = assetManager.open("tessdata/" + lang + ".traineddata");
                OutputStream out = new FileOutputStream(DATA_PATH + "tessdata/" + lang + ".traineddata");

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                //while ((lenf = gin.read(buff)) > 0) {
                while ((len = in.read(buf)) > 0)
                {
                    out.write(buf, 0, len);
                }
                in.close();
                //gin.close();
                out.close();

                Log.v(TAG, "Copied " + lang + " traineddata");
            }
            catch (IOException e)
            {
                Log.e(TAG, "Was unable to copy " + lang + " traineddata " + e.toString());
            }
        }
    }

    ///////////////////////////////////////////////////// Get Runtime permissions /////////////////////////////////////////////
    public void  GetPermissionDetails() {

        //Check manual permission if andorid version >6.0 runtime permission
        boolean result=CheckPermissionsGranted();

        if(result==true)
        {
            //permission granted
            btnCamera.setEnabled(true);
        }
        else
        {
            //call for runtime permission
            requestPermission();
        }

    }

    //Check all permission are granted ro not granted
    public boolean CheckPermissionsGranted() {

        //this code for multiple permission to check like location and phone state
        int readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int storgePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

        if(readPermission==0 && storgePermission==0 && cameraPermission==0)
        {
            //permission is granted
            return true;
        }
        else
        {
            //permission not granted
            return false;
        }
    }

    //Requesting permission
    private void requestPermission() {

        //this code for multiple permission to check like location and phone state
        int readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int storgePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

        List<String> listPermissionsNeeded = new ArrayList<>();

        if (readPermission != PackageManager.PERMISSION_GRANTED)
        {
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if(storgePermission!=PackageManager.PERMISSION_GRANTED)
        {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if(cameraPermission!=PackageManager.PERMISSION_GRANTED)
        {
            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        }
        if (!listPermissionsNeeded.isEmpty())
        {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
        }
    }

    //This method will be called when the user will tap on allow or deny
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        //Checking the request code of our request
        if(requestCode == REQUEST_ID_MULTIPLE_PERMISSIONS){

            //If permission is granted
            if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                //permission granted
                btnCamera.setEnabled(true);
            }
            else
            {
                Toast.makeText(getApplicationContext(),"Oops You Dented Permissions !!!",Toast.LENGTH_LONG).show();
                finish();
            }
        }

        //Camera image crop result here
        if (mCropImageUri != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            // required permissions granted, start crop image activity
            startCropImageActivity(mCropImageUri);
        }

    }

    //Camera image
    @Override
    @SuppressLint("NewApi")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Uri imageUri = null;

        // handle result of pick image chooser
        if (requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE && resultCode == Activity.RESULT_OK)
        {
            imageUri = CropImage.getPickImageResultUri(this, data);

            // For API >= 23 we need to check specifically that we have permissions to read external storage.
            if (CropImage.isReadExternalStoragePermissionsRequired(this, imageUri))
            {
                // request permissions and handle the result in onRequestPermissionsResult()
                mCropImageUri = imageUri;
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            }
            else {

                // no permissions required or already grunted, can start crop image activity
                startCropImageActivity(imageUri);
            }
        }

        // handle result of CropImageActivity
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
        {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK)
            {
                try {

                    imgResult.setVisibility(View.VISIBLE);
                    imgResult.setImageURI(result.getUri());
                    txtResult.setText("");

                    _path = result.getUri().toString();
                    bitmapResult = MediaStore.Images.Media.getBitmap(this.getContentResolver(), result.getUri());


                    PhotoTaken photoTaken=new  PhotoTaken();
                    photoTaken.execute();

                }
                catch (Exception er)
                {
                    Toast.makeText(this, er.toString(), Toast.LENGTH_LONG).show();
                }

            }
            else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE)
            {
                Toast.makeText(this, "Cropping failed: " + result.getError(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startCropImageActivity(Uri imageUri){

        CropImage.activity(imageUri)
                .setGuidelines(CropImageView.Guidelines.ON)
                .setMultiTouchEnabled(true)
                .start(this);
    }

    //Image convert to text
    public class PhotoTaken extends AsyncTask<String,String,Bitmap> {

        ProgressDialog dialog=new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Please wait...");
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected Bitmap doInBackground(String... strings) {

            _taken = true;

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;

            //bitmapResult = BitmapFactory.decodeFile(_path, options);

            try
            {
                ExifInterface exif = new ExifInterface(_path);
                //int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_FLIP_VERTICAL);

                Log.v(TAG, "Orient: " + exifOrientation);

                int rotate = 0;

                switch (exifOrientation)
                {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        rotate = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        rotate = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        rotate = 270;
                        break;
                }

                Log.v(TAG, "Rotation: " + rotate);

                if (rotate != 0)
                {

                    // Getting width & height of the given image.
                    int w = bitmapResult.getWidth();
                    int h = bitmapResult.getHeight();

                    // Setting pre rotate
                    Matrix mtx = new Matrix();
                    mtx.preRotate(rotate);

                    // Rotating Bitmap
                    bitmapResult = Bitmap.createBitmap(bitmapResult, 0, 0, w, h, mtx, false);
                }

                // Convert to ARGB_8888, required by tess
                bitmapResult = bitmapResult.copy(Bitmap.Config.ARGB_8888, true);

            } catch (IOException e)
            {
                Log.e(TAG, "Couldn't correct orientation: " + e.toString());
            }
            return bitmapResult;
        }

        @Override
        protected void onPostExecute(Bitmap myBitmap) {

            if(myBitmap!=null)
            {

                try {

                    File tessdata = new File(DATA_PATH);
                    if (!tessdata.exists()) {
                        throw new IllegalArgumentException("Data path must contain subfolder tessdata!");
                    }
                    TessBaseAPI baseApi = new TessBaseAPI();
                    baseApi.init(DATA_PATH , lang);
                    baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_OSD);
                    //baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
                    baseApi.setImage(myBitmap);
                    String recognizedText = baseApi.getUTF8Text();
                    baseApi.end();

                    imgResult.setVisibility(View.GONE);
                    txtResult.setVisibility(View.VISIBLE);
                    txtResult.setText(recognizedText);
                }
                catch (Exception er)
                {
                    Toast.makeText(getApplicationContext(),er.toString(),Toast.LENGTH_LONG).show();
                }

                dialog.dismiss();
            }
            dialog.dismiss();

        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(MainActivity.PHOTO_TAKEN, _taken);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.i(TAG, "onRestoreInstanceState()");
        if (savedInstanceState.getBoolean(MainActivity.PHOTO_TAKEN)) {
            PhotoTaken photoTaken=new  PhotoTaken();
            photoTaken.execute();
        }
    }

}
