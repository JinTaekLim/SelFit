package com.example.sfproject;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class Post_CreateActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private Button customButton, regButton , customButton_popup;
    private EditText titleEditText, contentEditText;
    private List<Uri> selectedImageUris = new ArrayList<>();
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private FirebaseFirestore db;

    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    String User_UID = currentUser.getUid();

    private SimpleDateFormat sdf =  new SimpleDateFormat("yyyy_MMdd_HHmm_ssSSS");

    // final String formattedDate = sdf.format(new Date());
    public String formattedDate;

    public void goToMainActivity(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_create);

        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul")); // 한국 시간대로 설정
        formattedDate = sdf.format(new Date());

//        sdf = new SimpleDateFormat("yyyy_MMdd_HHmm_ssSSS");
  //      sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));


        FirebaseApp.initializeApp(this);
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        db = FirebaseFirestore.getInstance();

        titleEditText = findViewById(R.id.title_et);
        contentEditText = findViewById(R.id.content_et);
        customButton = findViewById(R.id.customButton);
        regButton = findViewById(R.id.reg_button);
        customButton_popup = findViewById(R.id.customButton_popup);

        customButton_popup.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(Post_CreateActivity.this, Post_Create_popup.class);
                startActivity(intent);
            }
        });
        customButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        regButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String title = titleEditText.getText().toString();
                String content = contentEditText.getText().toString();
                Map<String, Object> data = new HashMap<>();
                data.put("title", title);
                data.put("content", content);

                for (int i = 0; i < selectedImageUris.size(); i++) {
                    Uri imageUri = selectedImageUris.get(i);
                    uploadImageToStorage(imageUri, i, data);
                }
            }
        });
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST);
    }

    private void uploadImageToStorage(Uri imageUri, final int index, final Map<String, Object> postData) {

        final String documentName = "MainPost-" + sdf.format(new Date()) + "-" + index;

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] imageData = baos.toByteArray();

            if (index == 0) {
                // 이미지를 저장할 경로 지정
                StorageReference profileRef = storageRef.child("Profile/" + User_UID);

                // 기존에 저장된 사진들 중 가장 큰 번호 찾기
                profileRef.listAll()
                        .addOnSuccessListener(listResult -> {
                            int maxPhotoNum = -1;

                            for (StorageReference item : listResult.getItems()) {
                                String itemName = item.getName();
                                if (itemName.startsWith("Profile_photo-")) {
                                    // "Profile_photo-" 다음의 번호 파싱 (".jpg"를 제거하고 숫자로 변환)
                                    String[] parts = itemName.split("-");
                                    String numWithExtension = parts[1];
                                    String[] numParts = numWithExtension.split("\\.");
                                    int num = Integer.parseInt(numParts[0]);
                                    maxPhotoNum = Math.max(maxPhotoNum, num);
                                }
                            }

                            // 새로운 번호 계산
                            int newPhotoNum = maxPhotoNum + 1;

                            // 새로운 이미지를 저장할 경로 생성
                            StorageReference newImageRef = profileRef.child("/Profile_photo-" + newPhotoNum + ".jpg");

                            UploadTask uploadTask = newImageRef.putFile(imageUri);
                        })
                        .addOnFailureListener(e -> {
                            // 오류 처리
                        });
            }

            StorageReference imageRef = storageRef.child("MainPost_images/" + formattedDate +"/  "+ documentName + ".jpg");
            UploadTask uploadTask = imageRef.putBytes(imageData);

            uploadTask.addOnSuccessListener(taskSnapshot -> {


                imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    //String imageUrl = uri.toString();
                    //postData.put("imageUrl(" + (index + 1) + ")", imageUrl);

                    if (index == selectedImageUris.size() - 1) {
                        // 마지막 이미지까지 업로드되었으면 Firestore에 데이터 저장
                        saveDataToFirestore(formattedDate, postData);
                    }
                });
            }).addOnFailureListener(e -> {

            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveDataToFirestore(String documentName, Map<String, Object> postData) {

        postData.put("Writer_User", User_UID);

        db.collection("Post")
                .document(formattedDate)
                .set(postData)
                .addOnSuccessListener(aVoid -> {

                    finish();
                })
                .addOnFailureListener(e -> {

                });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {

                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    selectedImageUris.add(imageUri);
                    insertImageIntoEditText(imageUri);
                }
            } else if (data.getData() != null) {

                Uri imageUri = data.getData();
                selectedImageUris.add(imageUri);
                insertImageIntoEditText(imageUri);
            }
        }
    }

    private void insertImageIntoEditText(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            SpannableString spannableString = new SpannableString(" ");
            ImageSpan imageSpan = new ImageSpan(this, bitmap);
            spannableString.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            int cursorPosition = contentEditText.getSelectionStart();
            contentEditText.getText().insert(cursorPosition, spannableString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
