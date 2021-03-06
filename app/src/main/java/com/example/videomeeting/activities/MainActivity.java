package com.example.videomeeting.activities;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;

import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.videomeeting.R;
import com.example.videomeeting.adapter.UserAdapter;
import com.example.videomeeting.listeners.UsersListener;
import com.example.videomeeting.models.User;
import com.example.videomeeting.utilities.Constants;
import com.example.videomeeting.utilities.PreferanceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements UsersListener {

    private PreferanceManager preferanceManager;
    private List<User> users;
    private UserAdapter userAdapter;
    private TextView textErrorMessage;
    private ImageView  imageConference;
    private int REQUEST_CODE_BATTERY_OPTIMIZATIONS = 1;
    private RecyclerView usersRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferanceManager = new PreferanceManager(getApplicationContext());
        imageConference = findViewById(R.id.imageConference);
        TextView textTitle = findViewById(R.id.textTitle);
        textTitle.setText(String.format(
                "%s %s",
                preferanceManager.getString(Constants.KEY_FIRST_NAME),
                preferanceManager.getString(Constants.KEY_LAST_NAME)
        ));

        findViewById(R.id.textSignOut).setOnClickListener(view -> signOut());

        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(task -> {

            if(task.isSuccessful() && task.getResult()!=null ){
                sendFCMTokenToDatabase(task.getResult().getToken());
            }
        });
        usersRecyclerView = findViewById(R.id.userRecyclerView);
        textErrorMessage = findViewById(R.id.textErrorMessage);
        users = new ArrayList<>();
        userAdapter = new UserAdapter(users, this);
        usersRecyclerView.setAdapter(userAdapter);
        getUsers();
        checkForBatteryOptimizations();
    }
    private void getUsers(){
        //swipeRefreshLayout.setRefreshing(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                    //swipeRefreshLayout.setRefreshing(false);
                    String myUserId = preferanceManager.getString(Constants.KEY_USER_ID);
                    if(task.isSuccessful() && task.getResult()!=null) {
                        users.clear();
                        for(QueryDocumentSnapshot documentSnapshot: task.getResult())
                        {
                            //Here we will display the user list except for the
                            //currently signed in user, Because no one will have a
                            //meeting with himself.
                            //So we are excluding the signed in user from the list
                            if(myUserId.equals(documentSnapshot.getId())){
                                continue;
                            }
                            User user = new User();
                            user.firstName = documentSnapshot.getString(Constants.KEY_FIRST_NAME);
                            user.lastName = documentSnapshot.getString(Constants.KEY_LAST_NAME);
                            user.email = documentSnapshot.getString(Constants.KEY_EMAIL);
                            user.token = documentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                            users.add(user);
                        }
                        if(users.size() > 0){
                            userAdapter.notifyDataSetChanged();
                        }
                        else{
                            textErrorMessage.setText(String.format("%s ","No users available"));
                            textErrorMessage.setVisibility(View.VISIBLE);
                        }


                    }else {
                        textErrorMessage.setText(String.format("%s ","No users available"));
                        textErrorMessage.setVisibility(View.VISIBLE);
                    }
                });

    }

    //Firebase cloud messaging
    //FCM token is a token which we have to fetch from firebase while login.
    //Usually this token is saved is the user database and and used further to send push notifications to the users.

    private  void sendFCMTokenToDatabase(String token){

        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferanceManager.getString(Constants.KEY_USER_ID)
                );
        documentReference.update(Constants.KEY_FCM_TOKEN,token)
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Unable to send a token "+e.getMessage(), Toast.LENGTH_SHORT).show());

    }
    private void signOut(){

        Toast.makeText(this, "Signing out...", Toast.LENGTH_SHORT).show();
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferanceManager.getString(Constants.KEY_USER_ID)
                );

        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(aVoid -> {

                    preferanceManager.clearPreferences();
                    startActivity(new Intent(getApplicationContext(),SignInActivity.class));
                    finish();

                }).addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Unable to sign out", Toast.LENGTH_SHORT).show());

    }

    @Override
    public void initiateVideoMeeting(User user) {

        if(user.token == null || user.token.trim().isEmpty()){

            Toast.makeText(this, user.firstName+" "+user.lastName+" is not available for meeting", Toast.LENGTH_SHORT).show();

        }
        else
        {
            Intent intent = new Intent(getApplicationContext(), OutgoingInvitationActivity.class);
            //We can directly pass the user object as the User modal class implements Serializable intefacee
            intent.putExtra("user",user);
            intent.putExtra("type","video");
            startActivity(intent);
        }

    }

    @Override
    public void initiateAudioMeeting(User user) {

        if(user.token == null || user.token.trim().isEmpty()){
            Toast.makeText(this, user.firstName+" "+user.lastName+" is not available for meeting", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Intent intent = new Intent(getApplicationContext(), OutgoingInvitationActivity.class);
            intent.putExtra("user",user);
            intent.putExtra("type","audio");
            startActivity(intent);
        }
    }

    public void onMultipleUsersAction(Boolean isMultipleUsersSelected){
        if(isMultipleUsersSelected){
            imageConference.setVisibility(View.VISIBLE);
            imageConference.setOnClickListener(view -> {
                Intent intent = new Intent(getApplicationContext(),OutgoingInvitationActivity.class);
                intent.putExtra("selectedUsers", new Gson().toJson(userAdapter.getSelectedUsers()));
                intent.putExtra("type","video");
                intent.putExtra("isMultiple",true);
                startActivity(intent);
            });
        }else{
            imageConference.setVisibility(View.GONE);
        }
    }

    private void checkForBatteryOptimizations(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            PowerManager powerManager = (PowerManager)getSystemService(POWER_SERVICE);
            if(powerManager.isIgnoringBatteryOptimizations(getPackageName())){
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Warning");
                builder.setMessage("battery optimization is enabled. It can interrupt running background services");
                builder.setPositiveButton("Disable", (dialogInterface, i) -> {
                    Intent intent = new Intent((Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                    startActivityForResult(intent,REQUEST_CODE_BATTERY_OPTIMIZATIONS);
                });
                builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());
                builder.create().show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_BATTERY_OPTIMIZATIONS){
            checkForBatteryOptimizations();
        }
    }
    /*Note
    * Disabling battery optimization will not guarentee that your app works in the background after it is killed.
    *  Sometimes thers nothing we can do*, device manufactures optimize their OS in a way that makes it impossible to run
    *  ackground services in backgeounded or killed apps. You might be wondering how do other applications work even after they are killed
    *  like whatsapp or facebook.
    * The fact is that most popular social apps are whitelisted by device manufacturers, so if you kill that app it's service will
    * automatically start in the backgorund
     */
}