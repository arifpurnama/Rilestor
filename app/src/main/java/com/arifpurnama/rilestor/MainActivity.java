package com.arifpurnama.rilestor;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.automl.FirebaseAutoMLLocalModel;
import com.google.firebase.ml.vision.automl.FirebaseAutoMLRemoteModel;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceAutoMLImageLabelerOptions;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    FirebaseAutoMLRemoteModel remoteModel; // For loading the model remotely
    FirebaseVisionImageLabeler labeler; //For running the image labeler
    FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder optionsBuilder; // Which option is use to run the labeler local or remotely
    ProgressDialog progressDialog; //Show the progress dialog while model is downloading...
    FirebaseModelDownloadConditions conditions; //Conditions to download the model
    FirebaseVisionImage image; // preparing the input image
    private FirebaseAutoMLLocalModel localModel;
    TextView textView; // Displaying the label for the input image
    Button button; // To select the image from device
    ImageView imageView; //To display the selected image


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.TextResult);
        button = findViewById(R.id.btnSelectImage);
        imageView = findViewById(R.id.viewImage);
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Please Wait....");
        progressDialog.setCanceledOnTouchOutside(false);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CropImage.activity().start(MainActivity.this);
            }
        });
    }

    private void setLabelerFromLocalModel(Uri uri) {
        localModel = new FirebaseAutoMLLocalModel.Builder()
                .setAssetFilePath("manifest.json")
                .build();
        try {
            FirebaseVisionOnDeviceAutoMLImageLabelerOptions options =
                    new FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder(localModel)
                            .setConfidenceThreshold(0.0f)
                            .build();
            labeler = FirebaseVision.getInstance().getOnDeviceAutoMLImageLabeler(options);
            image = FirebaseVisionImage.fromFilePath(MainActivity.this, uri);
            processImageLabeler(labeler, image);
        } catch (FirebaseMLException | IOException e) {
            e.printStackTrace();
        }
    }

    private void processImageLabeler(FirebaseVisionImageLabeler labeler, FirebaseVisionImage image) {
        labeler.processImage(image).addOnCompleteListener(new OnCompleteListener<List<FirebaseVisionImageLabel>>() {
            @Override
            public void onComplete(@NonNull Task<List<FirebaseVisionImageLabel>> task) {
                progressDialog.cancel();
                //show result
                for (FirebaseVisionImageLabel label : task.getResult()) {

                    String eachlabel = label.getText().toUpperCase();
                    float confidence = label.getConfidence();

                    if (confidence < 0.1) {
                        textView.setText(" Gambar tidak dikenal \n Akurasi - " + ("" + confidence * 100).subSequence(0, 4) + "%" + "\n\n");
                    } else {
                        textView.append(eachlabel + " - " + ("" + confidence * 100).subSequence(0, 4) + "%" + "\n\n");
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("OnFail", "" + e);
                Toast.makeText(MainActivity.this, "Something went wrong! " + e, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                if (result != null) {
                    Uri uri = result.getUri(); //path of image in phone
                    imageView.setImageURI(uri); //set image in imageview
                    textView.setText(""); //so that previous text don't get append with new one
                    setLabelerFromLocalModel(uri);
                } else
                    progressDialog.cancel();
            } else
                progressDialog.cancel();
        }
    }
}