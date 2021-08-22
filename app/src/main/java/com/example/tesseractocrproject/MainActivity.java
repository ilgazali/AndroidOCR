package com.example.tesseractocrproject;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity {

    private static TesseractOCR mTessOCR;
    private ProgressDialog mProgressDialog;
    private Context context;
    protected String mCurrentPhotoPath;
    private Uri photoURI1;
    private Uri oldPhotoURI;

    private static final String errorFileCreate = "Error file create!";
    private static final String errorConvert = "Error convert!";

    private static final int REQUEST_IMAGE1_CAPTURE = 1;
    private static final int GALLERY_REQUEST_CODE = 5;


    public static Bitmap capturedImg;

    public static Bitmap bmp;

    //   public Mat mat;
    //   public Mat grayMat;
    //   public Mat destMat;

    @BindView(R.id.ocr_image)
    ImageView firstImage;

    @BindView(R.id.ocr_text)
    TextView ocrText;


    int PERMISSION_ALL = 1;
    boolean flagPermissions = false;

    String[] PERMISSIONS = {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA
    };

    /*
      private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

          @Override
          public void onManagerConnected(int status) {
              switch (status) {
                  case LoaderCallbackInterface.SUCCESS:
                  {
                      Log.i("OpenCV", "OpenCV loaded successfully");

                       mat = new Mat();
                       grayMat = new Mat(); //Gray image
                       destMat = new Mat(); //Image after corrosion

                  } break;
                  default:
                  {
                      super.onManagerConnected(status);
                  } break;
              }
          }
      };
  */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = MainActivity.this;

        ButterKnife.bind(this);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        if (!flagPermissions) {
            checkPermissions();
        }
        String language = "tur";
        mTessOCR = new TesseractOCR(this, language);

    }

    /*
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
*/
    //choose img from gallery
    private void pickImgFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        String[] mimeTypes = {"image/jpeg", "image/png", "image/jpg"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE1_CAPTURE) {   /*  CAMERA INTENT  */

            if (resultCode == RESULT_OK) {
                bmp = null;
                try {
                    InputStream is = context.getContentResolver().openInputStream(photoURI1);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    bmp = BitmapFactory.decodeStream(is, null, options);


                } catch (Exception ex) {
                    Log.i(getClass().getSimpleName(), ex.getMessage());
                    Toast.makeText(context, errorConvert, Toast.LENGTH_SHORT).show();
                }

                firstImage.setImageBitmap(bmp);


                doOCR(bmp);

                OutputStream os;
                try {
                    os = new FileOutputStream(photoURI1.getPath());
                    if (bmp != null) {
                        bmp.compress(Bitmap.CompressFormat.JPEG, 100, os);

                    }
                    os.flush();
                    os.close();
                } catch (Exception ex) {

                    Log.e(getClass().getSimpleName(), ex.getMessage());
                    Toast.makeText(context, errorFileCreate, Toast.LENGTH_SHORT).show();

                }

            } else {

                    photoURI1 = oldPhotoURI;
                    firstImage.setImageURI(photoURI1);

            }

        } else {    /* PICK FROM GALLERY INTENT */

            capturedImg = null;

            Uri selectedImageURI = data.getData();

            try {
                capturedImg = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageURI);

            } catch (IOException e) {
                e.printStackTrace();
            }

            firstImage.setImageBitmap(capturedImg);
            doOCR(capturedImg);

        }

    }

    @OnClick(R.id.select_button)
    void onClickSelectButton(){
        pickImgFromGallery();

    }



    @OnClick(R.id.scan_button)
    void onClickScanButton() {
        // check permissions
        if (!flagPermissions) {
            checkPermissions();
            return;
        }
        //prepare intent
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(context, errorFileCreate, Toast.LENGTH_SHORT).show();
                Log.i("File error", ex.toString());
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                oldPhotoURI = photoURI1;
                photoURI1 = Uri.fromFile(photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI1);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE1_CAPTURE);
            }
        }
    }
    // to create a file
    public File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("MMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    // to check permissions
    void checkPermissions() {
        if (!hasPermissions(context, PERMISSIONS)) {
            requestPermissions(PERMISSIONS,
                    PERMISSION_ALL);
            flagPermissions = false;
        }
        flagPermissions = true;
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
    // for OCR
    private void doOCR(final Bitmap bitmap) {



        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog.show(this, "Processing",
                    "Doing OCR...", true);
        } else {
            mProgressDialog.show();
        }
        new Thread(new Runnable() {
            public void run() {
                final String srcText = mTessOCR.getOCRResult(bitmap);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (srcText != null && !srcText.equals("")) {
                            ocrText.setText(srcText);
                        }
                        mProgressDialog.dismiss();
                    }
                });
            }
        }).start();
    }

   /*
    public Bitmap processImage(Bitmap bmp){


// 1. Read the original image and convert it to OpenCV's Mat data format

        // Mat mat = new Mat();
        Bitmap bmp32 = bmp.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, mat);

//2. Convert strong original image to grayscale image

        // Mat grayMat = new Mat(); //Gray image
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGB2GRAY);

//3. Binarize grayscale images

        Mat binaryMat = new Mat(grayMat.height(),grayMat.width(),CvType.CV_8UC1);
        Imgproc.threshold(grayMat, binaryMat, 20, 255, Imgproc.THRESH_BINARY);

//4. The image is corroded --- it becomes wider and thicker after corrosion. It is easy to identify ---use 3*3 pictures to corrode

        // Mat destMat = new Mat(); //Image after corrosion
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.erode(binaryMat,destMat,element);

        Bitmap bmpProcessed = Bitmap.createBitmap(destMat.width(), destMat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(destMat, bmpProcessed);

        return bmpProcessed;
    }
*/

}