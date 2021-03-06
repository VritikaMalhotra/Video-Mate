package com.example.videomeeting.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.videomeeting.R;
import com.example.videomeeting.models.User;
import com.example.videomeeting.network.ApiClient;
import com.example.videomeeting.network.ApiService;
import com.example.videomeeting.utilities.Constants;
import com.example.videomeeting.utilities.PreferanceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.UUID;

public class OutgoingInvitationActivity extends AppCompatActivity {

    private PreferanceManager preferanceManager;
    private String inviterToken = null;
    private String meetingRoom = null;
    private String meetingType = null;

    private TextView textFirstChar;
    private TextView textUsername;
    private TextView textEmail;

    private int rejectionCount = 0;
    private int totalReceivers = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outgoing_invitation);
        preferanceManager = new PreferanceManager(getApplicationContext());


        ImageView imageMeetingType = findViewById(R.id.imageMeetingType);
        //Here we have user StringExtra because the type of item passed is String
        meetingType = getIntent().getStringExtra("type");

        if (meetingType != null) {
            if (meetingType.equals("video")) {

                imageMeetingType.setImageResource(R.drawable.ic_video);
            }
            else{
                imageMeetingType.setImageResource(R.drawable.ic_baseline_call_24);
            }
        }
         textFirstChar = findViewById(R.id.textFirstChar);
         textUsername = findViewById(R.id.textUsername);
         textEmail = findViewById(R.id.textEmail);

        //Here we have used SerializableExtra as the type of the Item passed is and object of class which implements Serializable
        User user = (User) getIntent().getSerializableExtra("user");
        if (user != null) {

            textFirstChar.setText(user.firstName.substring(0, 1));
            textUsername.setText(String.format("%s %s", user.firstName, user.lastName));
            textEmail.setText(user.email);
        }

        ImageView imageStopInvitation = findViewById(R.id.imageStopInvitation);
        imageStopInvitation.setOnClickListener(view -> {

            if(getIntent().getBooleanExtra("isMultiple",false)){
                Type type = new TypeToken<ArrayList<User>>(){}.getType();
                ArrayList<User> receivers = new Gson().fromJson(getIntent().getStringExtra("selectedUsers"),type);
                cancelInvitation(null,receivers);
            }else{
                if (user != null) {
                    cancelInvitation(user.token,null);

                }
            }
        });
        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    inviterToken = task.getResult().getToken();
                }
                //Here we check if there are multiple users or not.
                //if there are multiple users then we make an array list of all selectedUsers  along with their type
                //and send it as a parameter in initiateMeeting along with meeting type
                if(meetingType!=null){
                    if(getIntent().getBooleanExtra("isMultiple",false)){
                        Type type = new TypeToken<ArrayList<User>>(){}.getType();
                        ArrayList<User> receivers = new Gson().fromJson(getIntent().getStringExtra("selectedUsers"),type);
                        if(receivers != null){
                            totalReceivers = receivers.size();
                        }
                        initiateMeeting(meetingType,null,receivers);
                    }else{
                        //Here the condition of multiple users is false and so er send only only user
                        //with its meeting type and token
                        if (user != null) {
                            totalReceivers = 1;
                            initiateMeeting(meetingType, user.token,null);
                        }
                    }
                }
            }
        });
    }

    /*Documentation
    * initiateMeeting method creats the header and body needed to create the packet and calls sendingRemoteMessage method
    * The sendingRemoteMessage method uses the Retrofit ApiClient and ApiServices and calls MessagingService class.
    * In MessagingService class if the type is of remote message invitation,
    * then it sends the intent to start the activity incommingInvitationActivity to receivers phone.
    * if the type is of Remote message invitation response it sends invitation cancelled from here.
    * sendingRemoteMessage also creates a toast in its parent activity.
    * */
    private void initiateMeeting(String meetingType, String receiverToken, ArrayList<User> receivers) {

        try {
            // In here we are making body for our remote message.
            //Putting info into the data and passing it to the Body.

            JSONArray tokens = new JSONArray();

            if(receiverToken!=null){
                tokens.put(receiverToken);
            }
            if(receivers!=null && receivers.size()>0){
                StringBuilder userNames = new StringBuilder();
                for(int i=0;i<receivers.size();i++)
                {
                    tokens.put(receivers.get(i).token);
                    userNames.append(receivers.get(i).firstName).append(" ").append(receivers.get(i).lastName).append("\n");
                }
                textFirstChar.setVisibility(View.GONE);
                textEmail.setVisibility(View.GONE);
                textUsername.setText(userNames.toString());
            }

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            //Whatever content we put into data is the same content we receive in the MessagingService class
            //from the remoteMessage as the call goes to SendingRemoteMessage which calls sendRemoteMessage defined in MessagingService.

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION);
            data.put(Constants.REMOTE_MSG_MEETING_TYPE, meetingType);
            data.put(Constants.KEY_FIRST_NAME, preferanceManager.getString(Constants.KEY_FIRST_NAME));
            data.put(Constants.KEY_LAST_NAME, preferanceManager.getString(Constants.KEY_LAST_NAME));
            data.put(Constants.KEY_EMAIL, preferanceManager.getString(Constants.KEY_EMAIL));
            //When the receiver accepts or rejects the request, the sender must know what is the response and so we use this.
            data.put(Constants.REMOTE_MSG_INVITER_TOKEN, inviterToken);

            meetingRoom = preferanceManager.getString(Constants.KEY_USER_ID)+"_"+ UUID.randomUUID().toString().substring(0,5);
            data.put(Constants.REMOTE_MSG_MEETING_ROOM, meetingRoom);

            //The json Body should have data and Registration_ids(compulsory)
            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

            sendingRemoteMessage(body.toString(), Constants.REMOTE_MSG_INVITATION);

        } catch (Exception exception) {
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }

    }
    //Method for sending remote message using Retrofit Api.

    private void sendingRemoteMessage(String remoteMessageBody, String type) {

        ApiClient.getClient().create(ApiService.class).sendRemoteMessage(
                Constants.getRemoteMessageHeaders(), remoteMessageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    if (type.equals(Constants.REMOTE_MSG_INVITATION)) {
                        Toast.makeText(OutgoingInvitationActivity.this, "Invitation sent successfully", Toast.LENGTH_SHORT).show();
                    } else if (type.equals(Constants.REMOTE_MSG_INVITATION_RESPONSE)) {
                        Toast.makeText(OutgoingInvitationActivity.this, "Invitation cancelled", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                } else {
                    Toast.makeText(OutgoingInvitationActivity.this, response.message(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {

                Toast.makeText(OutgoingInvitationActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });

    }

    private void cancelInvitation(String receiverToken, ArrayList<User> receivers) {

        try {

            JSONArray tokens = new JSONArray();
            //Initially we used to put token for only one user
            if(receiverToken != null){
                tokens.put(receiverToken);
            }
            //After adding the meeting feature we place the token for all the receivers int the receivers array list
            if(receivers!=null && receivers.size()>0){
                for(User user : receivers){
                    tokens.put(user.token);
                }
            }

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION_RESPONSE);//Mesage type = meeting response
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE, Constants.REMOTE_MSG_INVITATION_CANCELLED);// Meeting respnse type = audio/video

            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);
            sendingRemoteMessage(body.toString(), Constants.REMOTE_MSG_INVITATION_RESPONSE);

        } catch (Exception exception) {
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    private BroadcastReceiver invitationResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE);
            if (type != null) {

                if (type.equals(Constants.REMOTE_MSG_INVITATION_ACCEPTED)) {
                    try{
                        URL serverURL = new URL("https://meet.jit.si");

                        JitsiMeetConferenceOptions.Builder builder = new JitsiMeetConferenceOptions.Builder();
                        builder.setServerURL(serverURL);
                        builder.setWelcomePageEnabled(false);
                        builder.setRoom(meetingRoom);
                        if(meetingType.equals("audio")) {
                            builder.setVideoMuted(true);
                        }
                        JitsiMeetConferenceOptions conferenceOptions =
                                new JitsiMeetConferenceOptions.Builder()
                                .setServerURL(serverURL)
                                .setWelcomePageEnabled(false)
                                .setRoom(meetingRoom)
                                .build();
                        JitsiMeetActivity.launch(OutgoingInvitationActivity.this,builder.build());
                        finish();

                    }catch(Exception exception)
                    {
                        Toast.makeText(context,exception.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    }

                } else if (type.equals(Constants.REMOTE_MSG_INVITATION_REJECTED)) {
                    rejectionCount += 1;
                    if(rejectionCount == totalReceivers){
                        Toast.makeText(context, "Invitation Rejected", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                }
            }
        }

    };

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                invitationResponseReceiver,
                new IntentFilter(Constants.REMOTE_MSG_INVITATION_RESPONSE)
        );
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                invitationResponseReceiver
        );
    }
}
