package com.example.videomeeting.firebase;
import android.content.Intent;
import com.example.videomeeting.activities.IncomingInvitationActivity;
import com.example.videomeeting.utilities.Constants;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

//Firebase messaging service can only work if the app is running in background.
//If the app is killed from background or foreground, the app service will not run
// and the receiver will not receive the call/

public class MessagingService extends FirebaseMessagingService {


    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String type = remoteMessage.getData().get(Constants.REMOTE_MSG_TYPE);
        if(type!=null){

            if(type.equals(Constants.REMOTE_MSG_INVITATION)){

                Intent intent = new Intent(getApplicationContext(), IncomingInvitationActivity.class);
                intent.putExtra(
                        Constants.REMOTE_MSG_MEETING_TYPE,
                        remoteMessage.getData().get(Constants.REMOTE_MSG_MEETING_TYPE)
                );
                intent.putExtra(
                        Constants.KEY_FIRST_NAME,
                        remoteMessage.getData().get(Constants.KEY_FIRST_NAME)
                );
                intent.putExtra(
                        Constants.KEY_LAST_NAME,
                        remoteMessage.getData().get(Constants.KEY_LAST_NAME)
                        );
                intent.putExtra(
                        Constants.KEY_EMAIL,
                        remoteMessage.getData().get(Constants.KEY_EMAIL)
                );
                intent.putExtra(
                      Constants.REMOTE_MSG_INVITER_TOKEN,
                      remoteMessage.getData().get(Constants.REMOTE_MSG_INVITER_TOKEN)
                    );
                intent.putExtra(
                        Constants.REMOTE_MSG_INVITER_TOKEN,
                        remoteMessage.getData().get(Constants.REMOTE_MSG_INVITER_TOKEN)
                );
                intent.putExtra(
                        Constants.REMOTE_MSG_MEETING_ROOM,
                        remoteMessage.getData().get(Constants.REMOTE_MSG_MEETING_ROOM)
                );
                //We are starting an activity from a non activity class, wee need to a flag.
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }else if(type.equals(Constants.REMOTE_MSG_INVITATION_RESPONSE)){

                //Remote message invitation response is accepted or rejected.
                //The value is originally set and displayed as a toast in IncomingInvitationActivity.
                //We have to diaply that value in senders screen also and so we use this MessagingService class
                //and sent the broadcast message to OutgoingInvitation Activity.
                //Here we are using the same intent in Incoming and outgoing activity and so w have broadcasted the intent.
                //The receiver in both the classes receives the intent and decides further
                Intent intent = new Intent(Constants.REMOTE_MSG_INVITATION_RESPONSE);
                intent.putExtra(
                        Constants.REMOTE_MSG_INVITATION_RESPONSE,
                        remoteMessage.getData().get(Constants.REMOTE_MSG_INVITATION_RESPONSE)
                );
                //From here we have sent the intent to OutgoingInvitationActivity as we have to display the toast there.
                //This is bascicall the response of accept or reject from the receivers phone.
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }

        }

    }
}
