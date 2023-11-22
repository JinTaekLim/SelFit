package com.example.sfproject;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


public class PostActivity extends AppCompatActivity {

    ImageView postPic, postPic2, postPic3, imgProfile;
    TextView txtPostContent, txtPostDate, txtPostTitle, txtPostName, postContent;

    EditText editTextComment;
    Button btnAddComment;

    String PostKey ="Post_1";

    FirebaseUser firebaseUser;
    FirebaseAuth firebaseAuth;
    String currentUserId;
    FirebaseFirestore firebaseFirestore;
    SimpleDateFormat sdf =  new SimpleDateFormat("yyyy_MMdd_HHmm_ssSSS");
    String formattedDate;
    CommentAdapter commentAdapter;
    List<Comment> commentList;

    private String USER_UID="12qGDHfsKnPOtigLRr3rEmBooBo2";
    private FirebaseFirestore firestore;
    private StorageReference storageRef;
    private FirebaseStorage storage;

    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        LinearLayout postLinearLayout = findViewById(R.id.post_LL);

        // 위에서 정의한 createImageView() 메소드를 사용하여 ImageView를 생성합니다.
        ImageView imageView = createImageView(this);

        // 생성된 ImageView를 post_LL이라는 LinearLayout에 추가합니다.
        postLinearLayout.addView(imageView);


        Log.d("TAG", "실행");

        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference().child("/Profile/" + USER_UID);


        ImageView imgProfile = findViewById(R.id.img_profile);
        loadFirebaseImage_profile(imgProfile, "Profile_Photo.jpg");


        firestore = FirebaseFirestore.getInstance();
        setPostTitleFromFirestore();

        firestore = FirebaseFirestore.getInstance();
        firestore.collection("Post")
                .whereEqualTo("Post_Key", "Post_1")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // 해당하는 데이터가 있을 때의 처리
                                PostKey = document.getId(); // 문서의 이름(문서 ID) 가져오기
                                Log.d("TAG", "Post_1을 가진 문서 이름: " + PostKey);
                                USER_UID = document.getString("Write_UID"); // Write_UID 필드의 데이터 가져오기
                                Log.d("TAG", "Post_1을 가진 문서의 Write_UID: " + USER_UID);

                                // 이후 해당 documentName을 사용할 수 있습니다.
                            }
                        } else {
                            Log.d("TAG", "문서 조회 실패");
                        }
                    }
                });

        firestore.collection("Profile")
                .document(USER_UID) // USER_UID에 해당하는 document 가져오기
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userName = documentSnapshot.getString("name");
                        txtPostName.setText(userName); // profile_name TextView에 이름 설정
                    } else {
                        Log.d("TAG", "No such document");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.d("TAG", "Error getting document", e);
                });




        /* 댓글 */
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        currentUserId = FirebaseAuth.getInstance().getUid();

        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul")); // 한국 시간대로 설정
        formattedDate = sdf.format(new Date());


        firebaseFirestore = FirebaseFirestore.getInstance();
        RecyclerView recyclerView = findViewById(R.id.recyclerViewComments); // 여기에 XML에서 정의한 ID를 넣으세요
        commentList = new ArrayList<>();
        commentAdapter = new CommentAdapter(commentList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(commentAdapter);

        btnAddComment = findViewById(R.id.sendButton);
        editTextComment = findViewById(R.id.CommentEdt);

        txtPostTitle = findViewById(R.id.postTitle);
        txtPostContent = findViewById(R.id.postContent);
        txtPostDate = findViewById(R.id.postDate);
        txtPostName = findViewById(R.id.profile_name);

        imgProfile = findViewById(R.id.img_profile);
        /*
        postPic = findViewById(R.id.postPic);
        postPic2 = findViewById(R.id.postPic2);
        postPic3 = findViewById(R.id.postPic3);

         */

        /* 댓글 */

        btnAddComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCommentToFirestore();
            }
        });
    }



    /* 댓글 */
    private void addCommentToFirestore() {
        String commentContent = editTextComment.getText().toString().trim();

        // firebaseUser가 null이 아닌지 확인
        if (firebaseUser != null && !commentContent.isEmpty()) {
            String uid = firebaseUser.getUid();

            // Profile 컬렉션에서 idToken과 일치하는 문서 가져오기
            firebaseFirestore.collection("Profile")
                    .whereEqualTo("idToken", uid)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String uname = document.getString("name");
                                String uimg = document.getString("profileImageUrl");

                                // Comment 객체 생성
                                Comment comment = new Comment(commentContent, uid, uimg, uname);

                                // Firestore에 댓글 추가
                                firebaseFirestore.collection("comments")
                                        .add(comment)
                                        .addOnSuccessListener(documentReference -> {
                                            // 댓글 추가 성공 시
                                            editTextComment.setText(""); // 입력란 초기화
                                        })
                                        .addOnFailureListener(e -> {
                                            // 댓글 추가 실패 시
                                            // 실패 처리를 원하는대로 구현
                                        });
                            }
                        } else {
                            // Profile 컬렉션에서 데이터 가져오기 실패
                        }
                    });
        }

        /* 댓글 */

    }









    private ImageView createImageView(Context context) {
        ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 500));
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageView.setImageResource(R.drawable.sjg01);
        return imageView;
    }



    private void loadFirebaseImage_profile(ImageView imageView, String imagePath) {
        // Firebase Storage에 있는 이미지의 StorageReference 가져오기
        StorageReference imageRef = storageRef.child(imagePath);

        // 이미지의 다운로드 URL을 얻어오기
        imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                // Glide를 사용하여 이미지 로드
                Glide.with(PostActivity.this)
                        .load(uri)
                        .into(imageView);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // 이미지 로드 실패 시 처리
                Log.e("ImageLoad", "이미지 로드 실패: " + e.getMessage());
            }
        });
    }



    private void setPostTitleFromFirestore() {

        postContent = findViewById(R.id.postContent);



        firestore.collection("Post")
                .whereEqualTo("Post_Key", "Post_1")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String postTitle = document.getString("title");
                        txtPostTitle.setText(postTitle); // postTitle TextView에 Title 설정
                    }
                })
                .addOnFailureListener(e -> {
                    Log.d("TAG", "Error getting documents", e);
                });

        firestore.collection("Post")
                .whereEqualTo("Post_Key", "Post_1")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String Contentpost = document.getString("content");
                        postContent.setText(Contentpost);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.d("TAG", "Error getting documents", e);
                });
    }














    public void goToMainActivity(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void openProfileActivity(View view) {
        Intent intent = new Intent(this, ProfileActivity.class);
        startActivity(intent);
    }
}




