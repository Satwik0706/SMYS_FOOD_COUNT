package com.satwik.oodapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.satwik.oodapplication.data.model.AppNotification
import com.satwik.oodapplication.domain.repository.NotificationRepository
import com.satwik.oodapplication.utils.Constants
import com.satwik.oodapplication.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class FirebaseNotificationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : NotificationRepository {

    override fun getAllNotifications(): Flow<Resource<List<AppNotification>>> = callbackFlow {
        val subscription = firestore.collection(Constants.COLLECTION_NOTIFICATIONS)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Error"))
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(AppNotification::class.java) ?: emptyList()
                trySend(Resource.Success(list))
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun sendNotification(notification: AppNotification): Resource<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_NOTIFICATIONS)
                .document(notification.id)
                .set(notification)
                .await()
            
            if (notification.isPush) {
                sendPushNotification(notification)
            }
            
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send notification")
        }
    }

    private suspend fun sendPushNotification(notification: AppNotification) = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("title", notification.title)
                put("body", notification.body)
            }

            val url = URL(Constants.PROXY_NOTIFICATION_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(json.toString())
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            connection.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun deleteNotification(notification: AppNotification): Resource<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_NOTIFICATIONS)
                .document(notification.id)
                .delete()
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete notification")
        }
    }
}
