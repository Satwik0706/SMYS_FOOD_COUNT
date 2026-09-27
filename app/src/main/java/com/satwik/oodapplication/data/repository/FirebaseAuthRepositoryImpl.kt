package com.satwik.oodapplication.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.satwik.oodapplication.data.model.AuditLog
import com.satwik.oodapplication.data.model.User
import com.satwik.oodapplication.domain.repository.AuthRepository
import com.satwik.oodapplication.utils.Constants
import com.satwik.oodapplication.utils.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject

import com.satwik.oodapplication.BuildConfig

class FirebaseAuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val sharedPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private var currentUser: User? = null

    override fun login(identifier: String, pass: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            val trimmedIdentifier = identifier.trim()
            val trimmedPass = pass.trim()

            // SECURITY NOTE: Hardcoded portal credentials below are legacy and should be migrated 
            // to Firebase Auth or a secure secret management system in the next major update.
            // 1. Check for hardcoded portals FIRST (Instant response, no network)
            when {
                trimmedIdentifier == "prakesh" && trimmedPass == "prakesh@123" -> {
                    val cookUser = User(uid = "cook_prakesh", name = "Prakesh", role = Constants.ROLE_COOK, email = "prakesh@smys.com")
                    saveSession(cookUser.uid, cookUser.role)
                    currentUser = cookUser
                    logAction(cookUser, "Logged In")
                    emit(Resource.Success(cookUser))
                    return@flow
                }
                trimmedIdentifier == "data@smys.com" && trimmedPass == "data@123" -> {
                    val dataUser = User(uid = "data_portal", name = "Data Entry", role = Constants.ROLE_DATA_ENTRY, email = "data@smys.com")
                    saveSession(dataUser.uid, dataUser.role)
                    currentUser = dataUser
                    logAction(dataUser, "Logged In")
                    emit(Resource.Success(dataUser))
                    return@flow
                }
                trimmedIdentifier == "maindata@smys.com" && trimmedPass == "data@1234" -> {
                    val mainDataUser = User(uid = "main_data_portal", name = "Main Data", role = Constants.ROLE_DATA_ENTRY, email = "maindata@smys.com")
                    saveSession(mainDataUser.uid, mainDataUser.role)
                    currentUser = mainDataUser
                    logAction(mainDataUser, "Logged In to Main Portal")
                    emit(Resource.Success(mainDataUser))
                    return@flow
                }
                trimmedIdentifier == "manager@smys.com" && trimmedPass == "manager070106" -> {
                    val managerUser = User(uid = "super_manager", name = "Manager", role = Constants.ROLE_MANAGER, email = "manager@smys.com")
                    saveSession(managerUser.uid, managerUser.role)
                    currentUser = managerUser
                    logAction(managerUser, "Logged In")
                    emit(Resource.Success(managerUser))
                    return@flow
                }
            }

            // 2. Search for the user in Firestore by Email OR Roll Number
            val queryTask = if (trimmedIdentifier.contains("@")) {
                firestore.collection(Constants.COLLECTION_USERS).whereEqualTo("email", trimmedIdentifier).get()
            } else {
                firestore.collection(Constants.COLLECTION_USERS).whereEqualTo("rollNumber", trimmedIdentifier).get()
            }
            
            var query = queryTask.await()
            
            if (query.isEmpty) {
                query = firestore.collection(Constants.COLLECTION_USERS)
                    .where(
                        com.google.firebase.firestore.Filter.or(
                            com.google.firebase.firestore.Filter.equalTo("email", trimmedIdentifier),
                            com.google.firebase.firestore.Filter.equalTo("rollNumber", trimmedIdentifier)
                        )
                    )
                    .get()
                    .await()
            }

            if (query.isEmpty) throw Exception("User not found in system")
            
            val userDoc = query.documents[0]
            val user = userDoc.toObject(User::class.java)?.copy(uid = userDoc.id) ?: throw Exception("Data format error")
            
            // 3. Verify Password (either stored password field or roll number)
            val storedPassword = userDoc.getString("password") ?: user.rollNumber
            
            if (storedPassword == trimmedPass) {
                // Update App Version in Firestore
                firestore.collection(Constants.COLLECTION_USERS).document(user.uid)
                    .update("appVersion", BuildConfig.VERSION_NAME)
                
                // SUCCESS: Logged in via Database match
                saveSession(user.uid, user.role)
                currentUser = user
                logAction(user, "Logged In")
                emit(Resource.Success(user))
            } else {
                // 4. Fallback: Only try Firebase Auth for Admins/Managers if DB password didn't match
                try {
                    auth.signInWithEmailAndPassword(user.email, trimmedPass).await()
                    
                    // Update App Version in Firestore
                    firestore.collection(Constants.COLLECTION_USERS).document(user.uid)
                        .update("appVersion", BuildConfig.VERSION_NAME)
                        
                    saveSession(user.uid, user.role)
                    currentUser = user
                    logAction(user, "Logged In")
                    emit(Resource.Success(user))
                } catch (e: Exception) {
                    throw Exception("Incorrect password")
                }
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Login failed"))
        }
    }

    private fun saveSession(uid: String, role: String) {
        // SECURITY FIX: Always clear any existing stale sessions before saving a new one
        sharedPrefs.edit().clear().apply() 

        sharedPrefs.edit()
            .putString("user_uid", uid)
            .putString("user_role", role)
            .apply()
    }

    override fun forgotPassword(identifier: String): Flow<Resource<String>> = flow {
        emit(Resource.Success("Please contact Sathpanta personally for security verification."))
    }

    override fun logout() {
        val user = currentUser ?: getSession()
        if (user != null) logAction(user, "Logged Out")
        auth.signOut()
        sharedPrefs.edit().remove("user_uid").remove("user_role").apply()
        currentUser = null
    }

    override fun getCurrentUser(uid: String): Flow<Resource<User>> = callbackFlow {
        val subscription = firestore.collection(Constants.COLLECTION_USERS).document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Error fetching user"))
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(User::class.java)
                if (user != null) {
                    trySend(Resource.Success(user))
                } else {
                    trySend(Resource.Error("User not found"))
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun resetPassword(uid: String, newPass: String) {
        firestore.collection(Constants.COLLECTION_USERS).document(uid)
            .update("password", newPass)
            .await()
    }

    override fun getSession(): User? {
        val savedUid = sharedPrefs.getString("user_uid", null)
        val savedRole = sharedPrefs.getString("user_role", null)
        
        return if (savedUid != null) {
            when (savedUid) {
                "cook_prakesh" -> User(uid = "cook_prakesh", name = "Prakesh", role = Constants.ROLE_COOK, email = "prakesh@smys.com")
                "data_portal" -> User(uid = "data_portal", name = "Data Entry", role = Constants.ROLE_DATA_ENTRY, email = "data@smys.com")
                "main_data_portal" -> User(uid = "main_data_portal", name = "Main Data", role = Constants.ROLE_DATA_ENTRY, email = "maindata@smys.com")
                "super_manager" -> User(uid = "super_manager", name = "Manager", role = Constants.ROLE_MANAGER, email = "manager@smys.com")
                else -> User(uid = savedUid, role = savedRole ?: "")
            }
        } else {
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                User(uid = firebaseUser.uid, email = firebaseUser.email ?: "")
            } else {
                currentUser
            }
        }
    }

    override fun getAllUsers(): Flow<List<User>> = callbackFlow {
        val subscription = firestore.collection(Constants.COLLECTION_USERS)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val users = snapshot.toObjects(User::class.java)
                    trySend(users)
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getUsersByRole(role: String): Flow<List<User>> = callbackFlow {
        val subscription = firestore.collection(Constants.COLLECTION_USERS)
            .whereEqualTo("role", role)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val users = snapshot.toObjects(User::class.java)
                    trySend(users)
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun addUser(user: User, pass: String) {
        // Force the password field into the data
        val userData = hashMapOf(
            "uid" to user.uid,
            "name" to user.name,
            "email" to user.email,
            "role" to user.role,
            "year" to user.year,
            "rollNumber" to user.rollNumber,
            "adminId" to user.adminId,
            "password" to pass,
            "breakfastPref" to user.breakfastPref,
            "lunchPref" to user.lunchPref,
            "snackPref" to user.snackPref,
            "dinnerPref" to user.dinnerPref,
            "isLeave" to user.isLeave,
            "noFoodPref" to user.noFoodPref
        )
        
        firestore.collection(Constants.COLLECTION_USERS)
            .document(user.uid)
            .set(userData)
            .await()
    }

    override suspend fun deleteUser(uid: String) {
        firestore.collection(Constants.COLLECTION_USERS).document(uid).delete().await()
    }

    override fun logAction(user: User, action: String) {
        val log = com.satwik.oodapplication.data.model.AuditLog(
            id = UUID.randomUUID().toString(),
            userId = user.uid,
            userName = user.name,
            action = action,
            timestamp = System.currentTimeMillis()
        )
        
        // Log Admin/Manager to main audit logs
        if (user.role == Constants.ROLE_ADMIN || user.role == Constants.ROLE_MANAGER) {
            firestore.collection(Constants.COLLECTION_LOGS).document(log.id).set(log)
        }
        
        // Always log student actions to a separate collection for the manager portal
        if (user.role == Constants.ROLE_STUDENT) {
            firestore.collection("student_logs").document(log.id).set(log)
        }
    }

    override fun getAppConfig(): Flow<Resource<com.satwik.oodapplication.data.model.AppConfig>> = callbackFlow {
        val subscription = firestore.collection(Constants.COLLECTION_SYSTEM)
            .document(Constants.DOCUMENT_CONFIG)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Error fetching config"))
                    return@addSnapshotListener
                }
                val config = snapshot?.toObject(com.satwik.oodapplication.data.model.AppConfig::class.java) 
                    ?: com.satwik.oodapplication.data.model.AppConfig()
                trySend(Resource.Success(config))
            }
        awaitClose { subscription.remove() }
    }
}
