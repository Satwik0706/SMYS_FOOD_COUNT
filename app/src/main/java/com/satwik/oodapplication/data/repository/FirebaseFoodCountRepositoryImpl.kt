package com.satwik.oodapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.satwik.oodapplication.data.model.AuditLog
import com.satwik.oodapplication.data.model.FoodCount
import com.satwik.oodapplication.data.model.FoodRequest
import com.satwik.oodapplication.data.model.LockStatus
import com.satwik.oodapplication.data.model.User
import com.satwik.oodapplication.domain.repository.FoodCountRepository
import com.satwik.oodapplication.utils.Constants
import com.satwik.oodapplication.utils.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject

class FirebaseFoodCountRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : FoodCountRepository {

    override fun getStudentFoodCount(studentId: String, date: String): Flow<Resource<FoodCount>> = callbackFlow {
        val dailyRef = firestore.collection(Constants.COLLECTION_FOODCOUNTS).document("${studentId}_$date")
        val userRef = firestore.collection(Constants.COLLECTION_USERS).document(studentId)
        
        var user: User? = null
        var dailyRecord: FoodCount? = null

        fun emitMergedData() {
            val u = user
            val daily = dailyRecord
            
            // PORTION LOGIC (VERSION SYNCED):
            // 1. If a Daily Record field is present (not null), it means an explicit choice 
            //    or admin reset has happened for TODAY. This takes absolute priority.
            // 2. If a field is null, we copy the student's saved preference.
            
            val finalData = if (daily != null) {
                FoodCount(
                    studentId = studentId,
                    date = date,
                    breakfast = daily.breakfast ?: u?.breakfastPref ?: false,
                    lunch = daily.lunch ?: u?.lunchPref ?: false,
                    snack = daily.snack ?: u?.snackPref ?: false,
                    dinner = daily.dinner ?: u?.dinnerPref ?: false,
                    lunchBox = daily.lunchBox ?: false,
                    isLeave = daily.isLeave ?: u?.isLeave ?: false
                )
            } else if (u != null) {
                FoodCount(
                    studentId = studentId,
                    date = date,
                    breakfast = u.breakfastPref,
                    lunch = u.lunchPref,
                    snack = u.snackPref,
                    dinner = u.dinnerPref,
                    lunchBox = false,
                    isLeave = u.isLeave
                )
            } else null

            finalData?.let { trySend(Resource.Success(it)) }
        }

        val userSub = userRef.addSnapshotListener { snapshot, _ ->
            user = snapshot?.toObject(User::class.java)
            emitMergedData()
        }

        val dailySub = dailyRef.addSnapshotListener { snapshot, _ ->
            dailyRecord = snapshot?.toObject(FoodCount::class.java)
            emitMergedData()
        }
        
        awaitClose { 
            userSub.remove()
            dailySub.remove()
        }
    }

    override suspend fun submitFoodCount(foodCount: FoodCount): Resource<Unit> {
        return try {
            val docId = "${foodCount.studentId}_${foodCount.date}"
            val foodCountRef = firestore.collection(Constants.COLLECTION_FOODCOUNTS).document(docId)
            
            // ATOMIC SHIELD: Use a hard Map to ensure no nulls are written
            // and values are explicitly pinned as True or False.
            val dailyUpdates = mutableMapOf<String, Any>(
                "studentId" to foodCount.studentId,
                "date" to foodCount.date,
                "breakfast" to foodCount.isBreakfast,
                "lunch" to foodCount.isLunch,
                "snack" to foodCount.isSnack,
                "dinner" to foodCount.isDinner,
                "lunchBox" to foodCount.isLunchBox,
                "isLeave" to foodCount.isOnLeave,
                "submittedAt" to System.currentTimeMillis()
            )
            
            foodCountRef.set(dailyUpdates, com.google.firebase.firestore.SetOptions.merge()).await()
                
            // 2. Sync sticky preferences in User document
            val profileUpdates = mapOf(
                "breakfastPref" to foodCount.isBreakfast,
                "lunchPref" to foodCount.isLunch,
                "snackPref" to foodCount.isSnack,
                "dinnerPref" to foodCount.isDinner,
                "isLeave" to foodCount.isOnLeave
            )

            firestore.collection(Constants.COLLECTION_USERS)
                .document(foodCount.studentId)
                .update(profileUpdates)
                .await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Submission failed")
        }
    }

    override fun getLockStatus(date: String): Flow<Resource<LockStatus>> = callbackFlow {
        val lockRef = firestore.collection(Constants.COLLECTION_LOCK_STATUS)
        
        // Listen only to the Global Document for real-time sync
        val subscription = lockRef.document(Constants.ACTIVE_LOCK_ID)
            .addSnapshotListener { snapshot, _ ->
                val status = snapshot?.toObject(LockStatus::class.java)
                if (status != null) {
                    trySend(Resource.Success(status))
                } else {
                    trySend(Resource.Success(LockStatus(date = Constants.ACTIVE_LOCK_ID)))
                }
            }
        
        awaitClose { subscription.remove() }
    }

    override suspend fun updateLockStatus(lockStatus: LockStatus): Resource<Unit> {
        return try {
            val batch = firestore.batch()
            val lockRef = firestore.collection(Constants.COLLECTION_LOCK_STATUS)
            
            // Map-based update to avoid overwriting new fields from old app versions
            val updates = mutableMapOf<String, Any>(
                "locked" to lockStatus.locked,
                "breakfastLocked" to lockStatus.breakfastLocked,
                "lunchLocked" to lockStatus.lunchLocked,
                "dinnerLocked" to lockStatus.dinnerLocked,
                "snackLocked" to lockStatus.snackLocked, // Ensure snack is included
                "date" to lockStatus.date
            )
            
            lockStatus.lockedBy?.let { updates["lockedBy"] = it }

            val activeDoc = lockRef.document(Constants.ACTIVE_LOCK_ID)
            batch.set(activeDoc, updates, com.google.firebase.firestore.SetOptions.merge())
            
            val today = LocalDate.now().toString()
            val legacyDoc = lockRef.document(today)
            batch.set(legacyDoc, updates, com.google.firebase.firestore.SetOptions.merge())
            
            batch.commit().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Update failed")
        }
    }

    override suspend fun updateSingleLock(field: String, value: Boolean): Resource<Unit> {
        return try {
            val lockRef = firestore.collection(Constants.COLLECTION_LOCK_STATUS)
            val today = LocalDate.now().toString()

            val updates = mapOf(field to value)
            
            // ATOMIC SYNC: We use set with merge to ensure we ONLY change the targeted lock.
            // This prevents the "Guest" Admin from being blocked or overwriting other data.
            val activeTask = lockRef.document(Constants.ACTIVE_LOCK_ID).set(updates, com.google.firebase.firestore.SetOptions.merge())
            val legacyTask = lockRef.document(today).set(updates, com.google.firebase.firestore.SetOptions.merge())

            activeTask.await()
            legacyTask.await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Update failed")
        }
    }

    override fun getSnackStatus(): Flow<Resource<com.satwik.oodapplication.data.model.SnackStatus>> = callbackFlow {
        // GLOBAL SYNC: Listen to the primary lock document so ALL app versions (old & new) stay synced
        val subscription = firestore.collection(Constants.COLLECTION_LOCK_STATUS)
            .document(Constants.ACTIVE_LOCK_ID)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Error"))
                    return@addSnapshotListener
                }
                
                // Extract just the snack lock from the global status
                val globalStatus = snapshot?.toObject(LockStatus::class.java)
                val snackStatus = com.satwik.oodapplication.data.model.SnackStatus(
                    locked = globalStatus?.snackLocked ?: false
                )
                trySend(Resource.Success(snackStatus))
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun updateSnackLock(isLocked: Boolean): Resource<Unit> {
        return try {
            // GLOBAL SYNC: Write to the main lock document so OLD app versions on other phones also lock
            val updates = mapOf("snackLocked" to isLocked)
            
            firestore.collection(Constants.COLLECTION_LOCK_STATUS)
                .document(Constants.ACTIVE_LOCK_ID)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Update failed")
        }
    }

    override fun getAllFoodCounts(date: String): Flow<Resource<List<FoodCount>>> = callbackFlow {
        val subscription = firestore.collection(Constants.COLLECTION_FOODCOUNTS)
            .whereEqualTo("date", date)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Error"))
                    return@addSnapshotListener
                }
                val counts = snapshot?.toObjects(FoodCount::class.java) ?: emptyList()
                trySend(Resource.Success(counts))
            }
        awaitClose { subscription.remove() }
    }

    override fun getAuditLogs(): Flow<List<AuditLog>> = callbackFlow {
        val subscription = firestore.collection(Constants.COLLECTION_LOGS)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(snapshot.toObjects(AuditLog::class.java))
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getStudentLogs(): Flow<List<AuditLog>> = callbackFlow {
        val subscription = firestore.collection("student_logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(snapshot.toObjects(AuditLog::class.java))
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun clearOldLogs(beforeTimestamp: Long) {
        val snapshots = firestore.collection(Constants.COLLECTION_LOGS)
            .whereLessThan("timestamp", beforeTimestamp)
            .get()
            .await()
        
        val batch = firestore.batch()
        snapshots.documents.forEach { doc ->
            batch.delete(doc.reference)
        }
        batch.commit().await()
    }

    override suspend fun resetAllFoodCounts(date: String): Resource<Unit> {
        return try {
            val students = firestore.collection(Constants.COLLECTION_USERS)
                .whereEqualTo("role", Constants.ROLE_STUDENT)
                .get().await().toObjects(User::class.java)

            val batch = firestore.batch()
            val foodCountRef = firestore.collection(Constants.COLLECTION_FOODCOUNTS)
            val userRef = firestore.collection(Constants.COLLECTION_USERS)

            students.forEach { student ->
                // IMPORTANT: We only reset if NOT already on leave
                if (!student.isLeave) {
                    // 1. Physical Reset of Daily Record (Today)
                    val docId = "${student.uid}_$date"
                    
                    // ATOMIC OVERWRITE: We use a Map to ensure fields are explicitly FALSE
                    val dailyReset = mapOf(
                        "studentId" to student.uid,
                        "date" to date,
                        "breakfast" to false,
                        "lunch" to false,
                        "snack" to false,
                        "dinner" to false,
                        "lunchBox" to false,
                        "isLeave" to false,
                        "submittedAt" to System.currentTimeMillis()
                    )
                    batch.set(foodCountRef.document(docId), dailyReset)
                    
                    // 2. Physical Reset of Sticky Preferences (Tomorrow's copy starts from zero)
                    val stickyUpdates = mapOf(
                        "breakfastPref" to false,
                        "lunchPref" to false,
                        "snackPref" to false,
                        "dinnerPref" to false
                    )
                    batch.update(userRef.document(student.uid), stickyUpdates)
                }
            }

            batch.commit().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Reset failed")
        }
    }

    override suspend fun resetSpecificMeal(date: String, mealType: String): Resource<Unit> {
        return try {
            val students = firestore.collection(Constants.COLLECTION_USERS)
                .whereEqualTo("role", Constants.ROLE_STUDENT)
                .get().await().toObjects(User::class.java)

            val batch = firestore.batch()
            val foodCountRef = firestore.collection(Constants.COLLECTION_FOODCOUNTS)
            val userRef = firestore.collection(Constants.COLLECTION_USERS)

            val fieldName = mealType.lowercase()
            val prefName = "${fieldName}Pref"

            students.forEach { student ->
                if (!student.isLeave) {
                    // 1. Update Daily Record
                    val docId = "${student.uid}_$date"
                    val dailyUpdates = mutableMapOf<String, Any>(
                        fieldName to false,
                        "submittedAt" to System.currentTimeMillis()
                    )
                    if (fieldName == "breakfast") dailyUpdates["lunchBox"] = false
                    
                    batch.set(foodCountRef.document(docId), dailyUpdates, com.google.firebase.firestore.SetOptions.merge())
                    
                    // 2. Update Sticky Preference
                    batch.update(userRef.document(student.uid), prefName, false)
                }
            }

            batch.commit().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Meal reset failed")
        }
    }

    override suspend fun submitFoodRequest(request: FoodRequest): Resource<Unit> {
        return try {
            val docId = request.id.ifEmpty { firestore.collection(Constants.COLLECTION_REQUESTS).document().id }
            val finalRequest = request.copy(id = docId, timestamp = System.currentTimeMillis())
            
            firestore.collection(Constants.COLLECTION_REQUESTS)
                .document(docId)
                .set(finalRequest)
                .await()
            
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Request failed")
        }
    }

    override fun getPendingRequests(): Flow<Resource<List<FoodRequest>>> = callbackFlow {
        val subscription = firestore.collection(Constants.COLLECTION_REQUESTS)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Error"))
                    return@addSnapshotListener
                }
                val requests = snapshot?.toObjects(FoodRequest::class.java) ?: emptyList()
                trySend(Resource.Success(requests))
            }
        awaitClose { subscription.remove() }
    }

    override fun getStudentRequest(studentId: String, date: String): Flow<Resource<FoodRequest?>> = callbackFlow {
        val subscription = firestore.collection(Constants.COLLECTION_REQUESTS)
            .whereEqualTo("studentId", studentId)
            .whereEqualTo("date", date)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Error"))
                    return@addSnapshotListener
                }
                val request = snapshot?.toObjects(FoodRequest::class.java)?.firstOrNull()
                trySend(Resource.Success(request))
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun updateRequestStatus(
        requestId: String,
        status: String,
        adminNote: String?
    ): Resource<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val requestRef = firestore.collection(Constants.COLLECTION_REQUESTS).document(requestId)
                val request = transaction.get(requestRef).toObject(FoodRequest::class.java)
                    ?: throw Exception("Request not found")

                // Update Request Status
                transaction.update(requestRef, mapOf(
                    "status" to status,
                    "adminNote" to adminNote
                ))

                if (status == "APPROVED") {
                    val studentId = request.studentId
                    val date = request.date
                    
                    // 1. Update Food Count Document
                    val foodCountRef = firestore.collection(Constants.COLLECTION_FOODCOUNTS).document("${studentId}_$date")
                    val foodCountUpdates = mutableMapOf<String, Any>(
                        "isLeave" to false,
                        "submittedAt" to System.currentTimeMillis()
                    )
                    if (request.breakfast) foodCountUpdates["breakfast"] = true
                    if (request.lunch) foodCountUpdates["lunch"] = true
                    if (request.dinner) foodCountUpdates["dinner"] = true
                    
                    transaction.set(foodCountRef, foodCountUpdates, com.google.firebase.firestore.SetOptions.merge())

                    // 2. Update User Profile (Sticky)
                    val userRef = firestore.collection(Constants.COLLECTION_USERS).document(studentId)
                    val userUpdates = mutableMapOf<String, Any>(
                        "isLeave" to false
                    )
                    if (request.breakfast) userUpdates["breakfastPref"] = true
                    if (request.lunch) userUpdates["lunchPref"] = true
                    if (request.dinner) userUpdates["dinnerPref"] = true
                    
                    transaction.update(userRef, userUpdates)
                }
            }.await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Update failed")
        }
    }

    override suspend fun getAdminWhatsAppNumber(): Resource<String> {
        return try {
            val snapshot = firestore.collection(Constants.COLLECTION_SYSTEM)
                .document(Constants.DOCUMENT_ADMIN_CONTACT)
                .get()
                .await()
            val number = snapshot.getString("number") ?: ""
            Resource.Success(number)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Fetch failed")
        }
    }

    override suspend fun updateAdminWhatsAppNumber(number: String): Resource<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_SYSTEM)
                .document(Constants.DOCUMENT_ADMIN_CONTACT)
                .set(mapOf("number" to number))
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Update failed")
        }
    }
}
