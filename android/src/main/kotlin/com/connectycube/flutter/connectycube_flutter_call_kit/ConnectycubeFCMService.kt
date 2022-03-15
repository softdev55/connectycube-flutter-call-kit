package com.connectycube.flutter.connectycube_flutter_call_kit

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.connectycube.flutter.connectycube_flutter_call_kit.utils.ContextHolder
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject

class ConnectycubeFCMService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        ContextHolder.applicationContext = applicationContext

        val data = remoteMessage.data
        Log.i("letsCube>> ok",data.toString())
        if (data.containsKey("signal_type")) {
            when (data["signal_type"]) {
                "startCall" -> {
                    Log.i("letsCube>> 1",data.toString())
                    processInviteCallEvent(applicationContext, data)
                }

                "endCall" -> {
                    Log.i("letsCube>> 2",data.toString())
                    processEndCallEvent(applicationContext, data)
                }

                "rejectCall" -> {
                    Log.i("letsCube>> 3",data.toString())
                    processEndCallEvent(applicationContext, data)
                }
            }

        }
    }

    private fun processEndCallEvent(applicationContext: Context, data: Map<String, String>) {
        val callId = data["session_id"] ?: return


        processCallEnded(applicationContext, callId)
    }

    private fun processInviteCallEvent(applicationContext: Context, data: Map<String, String>) {
        val callId = data["session_id"]

        if (callId == null || CALL_STATE_UNKNOWN != getCallState(
                applicationContext,
                callId
            )
        ) {
            return
        }

        val callType = data["call_type"]?.toInt()
        val callInitiatorId = data["caller_id"]?.toInt()
        val callInitiatorName = data["caller_name"]
        val callOpponentsString = data["call_opponents"]
        var callOpponents = ArrayList<Int>()
        if (callOpponentsString != null) {
            callOpponents = ArrayList(callOpponentsString.split(',').map { it.toInt() })
        }

        val userInfo = data["user_info"] ?: JSONObject(emptyMap<String, String>()).toString()

        if (callType == null || callInitiatorId == null || callInitiatorName == null || callOpponents.isEmpty()) {
            return
        }

        showCallNotification(
            applicationContext,
            callId,
            callType,
            callInitiatorId,
            callInitiatorName,
            callOpponents,
            userInfo
        )

        saveCallState(applicationContext, callId, CALL_STATE_PENDING)
        saveCallData(applicationContext, callId, data)
        saveCallId(applicationContext, callId)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        LocalBroadcastManager.getInstance(applicationContext)
            .sendBroadcast(Intent(ACTION_TOKEN_REFRESHED).putExtra(EXTRA_PUSH_TOKEN, token))
    }
}