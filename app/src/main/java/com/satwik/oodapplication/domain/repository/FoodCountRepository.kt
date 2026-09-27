package com.satwik.oodapplication.domain.repository

import com.satwik.oodapplication.data.model.AuditLog
import com.satwik.oodapplication.data.model.FoodCount
import com.satwik.oodapplication.data.model.FoodRequest
import com.satwik.oodapplication.data.model.LockStatus
import com.satwik.oodapplication.utils.Resource
import kotlinx.coroutines.flow.Flow

interface FoodCountRepository {
    fun getStudentFoodCount(studentId: String, date: String): Flow<Resource<FoodCount>>
    suspend fun submitFoodCount(foodCount: FoodCount): Resource<Unit>
    fun getLockStatus(date: String): Flow<Resource<LockStatus>>
    suspend fun updateLockStatus(lockStatus: LockStatus): Resource<Unit>
    suspend fun updateSingleLock(field: String, value: Boolean): Resource<Unit>
    
    // Snack Management
    fun getSnackStatus(): Flow<Resource<com.satwik.oodapplication.data.model.SnackStatus>>
    suspend fun updateSnackLock(isLocked: Boolean): Resource<Unit>

    fun getAllFoodCounts(date: String): Flow<Resource<List<FoodCount>>>
    fun getAuditLogs(): Flow<List<AuditLog>>
    fun getStudentLogs(): Flow<List<AuditLog>>
    suspend fun clearOldLogs(beforeTimestamp: Long)
    suspend fun resetAllFoodCounts(date: String): Resource<Unit>
    suspend fun resetSpecificMeal(date: String, mealType: String): Resource<Unit>

    // Missed Count Requests
    suspend fun submitFoodRequest(request: FoodRequest): Resource<Unit>
    fun getPendingRequests(): Flow<Resource<List<FoodRequest>>>
    fun getStudentRequest(studentId: String, date: String): Flow<Resource<FoodRequest?>>
    suspend fun updateRequestStatus(requestId: String, status: String, adminNote: String? = null): Resource<Unit>
    suspend fun getAdminWhatsAppNumber(): Resource<String>
    suspend fun updateAdminWhatsAppNumber(number: String): Resource<Unit>
}
