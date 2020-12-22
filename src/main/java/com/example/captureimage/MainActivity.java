package com.example.captureimage;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.multidex.MultiDex;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;


//import android.media.Image;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;


import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import java.io.ByteArrayOutputStream;
import java.io.File;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

//Google Cloud Client Library
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.Base64;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1p4beta1.TextAnnotation;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.cloud.vision.v1.Image;


import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context base){
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    //Variable Initialization
    private static final String TAG = "MyActivity";
    ImageView imageView;
    Button btOpen;
    TextView textView;
    byte[] handwrittenTextByteArray;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy); // override the policy

        //Assign Variables with Values
        imageView = findViewById(R.id.image_view);
        btOpen = findViewById(R.id.bt_open);
        textView = findViewById(R.id.text_view);

        //Request for Camera Access
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{
                            Manifest.permission.CAMERA
                    }, 100);
        }
        btOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Opens the Camera
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 100);

            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            //Get Captured Image
            Bitmap captureImage = (Bitmap) data.getExtras().get("data");
            //Set Capture Image to ImageView
            imageView.setImageBitmap(captureImage); // sets the bitmap as the content of the imageView


            //Adding bitmap to bundle for conversion
            //Convert to byte array
            //convert to base64
//            convert(imageBundle);
            //saving bitmap to file
            File savedImage = null;
//            try {
//                savedImage = saveCapturedImage(captureImage, "1") ;
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            //testing if the request works
            //File path = "/storage/emulated/0/Pictures/Image.jpg";
            File file = new File("/storage/self/primary/Pictures/Image.jpg");

//            try {
//                savedImage = testingRequest(file, "example");
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

            //encode the file to Base64
            String encodedImage = "";
            try {
                //method to encode
               encodedImage = encodeFileToBase64(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //detecting the text in the image
            try {
                //detecting the text
                String response = sendReceiveRequest(encodedImage);
                JSONObject jObj = new JSONObject(response);
                String convertedText = jObj.getJSONArray("responses").getJSONObject(0)
                        .getJSONArray("textAnnotations")
                        .getJSONObject(0)
                        .getString("description");
                System.out.println(convertedText);
                textView.setText(convertedText);//shows the Text
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inMutable = true;
                Bitmap bm = BitmapFactory.decodeByteArray(handwrittenTextByteArray, 0, handwrittenTextByteArray.length, options);
                imageView.setImageBitmap(bm);
                //TODO String -> Json -> extracted Text
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }


    //method for converting
    private String encodeFileToBase64(File file) throws IOException{
       // byte[] content = FileUtils.readFileToByteArray(filename);
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        //byte[] buf = new byte[(int)file.getTotalSpace()];
        handwrittenTextByteArray = new byte[(int)file.length()];
        in.read(handwrittenTextByteArray);
        return  Base64.encodeBase64String(handwrittenTextByteArray);



    }


    private File saveCapturedImage(Bitmap capturedImage, String imageId) throws IOException{

        FileOutputStream fileStream = null;
        File outFile = null;
        try{
            //File path = new File(getBaseContext().getFilesDir(), "CaptureImage" + File.separator + "Images");
            //File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            //File path = new File("storage/emulated/0/DCIM/Camera");
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES); //phone's public directory for pictures
            if(!path.exists()){
                path.mkdirs();
            }

            outFile = new File(path, imageId + ".jpeg"); //This is the file with the file path
            fileStream = new FileOutputStream(outFile);
            ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
            capturedImage.compress(Bitmap.CompressFormat.JPEG, 100, imageStream);
            byte[] byteArray = imageStream.toByteArray();
            fileStream.write(byteArray);
            fileStream.close();
            System.out.println(outFile);
        }
        catch(FileNotFoundException e){
            Log.e(TAG, "Saving image failed with ", e);
        }
        catch (IOException e){
            Log.e(TAG, "Saving image failed with ", e);
        }
        finally{
            assert fileStream != null;
            fileStream.close();
        }
        return outFile;
    }

    public static String sendReceiveRequest(String image) throws Exception{
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("application/json");
//        RequestBody body = RequestBody.create(mediaType, String.format("{\n  \"requests\": [\n    {\n      \"image\": {\n" +
//                "        \"content\": \"{" + image +
//                "}\"\n      },\n      \"features\": [\n        {\n          \"type\": \"DOCUMENT_TEXT_DETECTION\"\n        }\n      ]\n    }\n  ]\n}", image));
//        String request = "\"{  \\\"requests\\\": [    {      \\\"image\\\": {\" +\n" +
//                "\"        \\\"content\\\": \\\"{\" + image +\n" +
//                "\"}\\\"      },      \\\"features\\\": [        {          \\\"type\\\": \\\"DOCUMENT_TEXT_DETECTION\\\"        }      ]    }  ]}\"\n"
//        RequestBody body = RequestBody.create(mediaType, request);

        String json = String.format("{  \"requests\": [    {      \"image\": {        \"content\": \"%s\"      },      \"features\": [        {          \"type\": \"DOCUMENT_TEXT_DETECTION\"        }      ]    }  ]}", image);
        RequestBody body = RequestBody.create(mediaType, json);

        Request request = new Request.Builder() // request body in the format needed
                .url("https://vision.googleapis.com/v1/images:annotate")
                .method("POST", body)
                .addHeader("Authorization", "Bearer ya29.c.Kp0B5weHPphv0cGPr4jgCL5svlsVn5DOikjYpuXqaKXXWKExzzOQwsSA8UlGM2b5-SrbPJbbywN9dH92tZbFxEE9Mtw3nJLOysEQXpI-fW7eWKEPWnHcQzRzh2Y8TBaghvI6p5oDIU1iQhrR1Tv6pxhBet1oeQoDmZcCx169IDtLP2FjQczSjyX_CESwbHIKnyUKzsA5hfhSkgDedfGWYA")
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();
        ResponseBody responseBody = response.body();
        return responseBody.string();
    }
}







//


//    }



