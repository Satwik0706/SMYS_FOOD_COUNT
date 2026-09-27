package com.satwik.oodapplication.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satwik.oodapplication.data.model.FoodRequest
import com.satwik.oodapplication.data.model.LockStatus
import com.satwik.oodapplication.data.model.User
import com.satwik.oodapplication.domain.repository.AuthRepository
import com.satwik.oodapplication.domain.repository.FoodCountRepository
import com.satwik.oodapplication.utils.Constants
import com.satwik.oodapplication.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AdminSummary(
    val totalStudents: Int = 0,
    val presentStudents: Int = 0,
    val countSubmitted: Int = 0
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val foodCountRepository: FoodCountRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _today = LocalDate.now().toString()

    val summary: StateFlow<Resource<AdminSummary>> = combine(
        authRepository.getUsersByRole(Constants.ROLE_STUDENT),
        foodCountRepository.getAllFoodCounts(_today)
    ) { students, countsResource ->
        when (countsResource) {
            is Resource.Loading -> Resource.Loading()
            is Resource.Error -> Resource.Error(countsResource.message ?: "Error")
            is Resource.Success -> {
                val countsMap = countsResource.data?.associateBy { it.studentId } ?: emptyMap()
                var totalEating = 0
                
                students.forEach { student ->
                    val daily = countsMap[student.uid]
                    
                    // UNIFIED LOGIC: Match the calculation used in the detailed report
                    val onLeave = daily?.isLeave ?: student.isLeave
                    
                    if (!onLeave) {
                        val breakfast = daily?.breakfast ?: student.breakfastPref
                        val lunch = daily?.lunch ?: student.lunchPref
                        val snack = daily?.snack ?: student.snackPref
                        val dinner = daily?.dinner ?: student.dinnerPref
                        val lunchBox = daily?.lunchBox ?: false
                        
                        if (breakfast || lunch || snack || dinner || lunchBox) {
                            totalEating++
                        }
                    }
                }
                
                Resource.Success(AdminSummary(students.size, students.size, totalEating))
            }
        }
    }
    .flowOn(kotlinx.coroutines.Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())

    private val _lockStatus = MutableStateFlow<LockStatus>(LockStatus())
    val lockStatus: StateFlow<LockStatus> = _lockStatus

    val allStudents: StateFlow<List<User>> = authRepository.getUsersByRole(Constants.ROLE_STUDENT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingRequests: StateFlow<Resource<List<FoodRequest>>> = foodCountRepository.getPendingRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())

    private val _adminWhatsApp = MutableStateFlow<Resource<String>>(Resource.Loading())
    val adminWhatsApp: StateFlow<Resource<String>> = _adminWhatsApp

    init {
        loadLockStatus()
        loadAdminContact()
    }

    private fun loadAdminContact() {
        viewModelScope.launch {
            _adminWhatsApp.value = foodCountRepository.getAdminWhatsAppNumber()
        }
    }

    fun updateWhatsAppNumber(number: String) {
        viewModelScope.launch {
            foodCountRepository.updateAdminWhatsAppNumber(number)
            _adminWhatsApp.value = Resource.Success(number)
        }
    }

    fun approveRequest(requestId: String) {
        viewModelScope.launch {
            foodCountRepository.updateRequestStatus(requestId, "APPROVED")
            authRepository.getSession()?.let { admin ->
                authRepository.logAction(admin, "Approved missed count request: $requestId")
            }
        }
    }

    fun rejectRequest(requestId: String, reason: String) {
        viewModelScope.launch {
            foodCountRepository.updateRequestStatus(requestId, "REJECTED", reason)
            authRepository.getSession()?.let { admin ->
                authRepository.logAction(admin, "Rejected missed count request: $requestId")
            }
        }
    }

    private fun loadLockStatus() {
        viewModelScope.launch {
            foodCountRepository.getLockStatus(Constants.ACTIVE_LOCK_ID).collect { resource ->
                if (resource is Resource.Success) {
                    _lockStatus.value = resource.data ?: LockStatus(date = Constants.ACTIVE_LOCK_ID)
                }
            }
        }
    }

    fun toggleLock() {
        val current = _lockStatus.value
        val newValue = !current.locked
        viewModelScope.launch {
            foodCountRepository.updateSingleLock("locked", newValue)
            // Log Admin action
            authRepository.getSession()?.let { admin ->
                val action = if (newValue) "Locked Portal" else "Unlocked Portal"
                authRepository.logAction(admin, action)
            }
        }
    }

    fun toggleAutomation() {
        val current = _lockStatus.value
        val newValue = !current.automationEnabled
        viewModelScope.launch {
            foodCountRepository.updateSingleLock("automationEnabled", newValue)
            
            if (newValue) {
                com.satwik.oodapplication.worker.LockAutomationWorker.startImmediately(context)
            }

            // Log Admin action
            authRepository.getSession()?.let { admin ->
                val action = if (newValue) "Enabled Auto-Lock" else "Disabled Auto-Lock"
                authRepository.logAction(admin, action)
            }
        }
    }

    fun toggleMealLock(mealType: String) {
        val current = _lockStatus.value
        val field = when (mealType.lowercase()) {
            "breakfast" -> "breakfastLocked"
            "lunch" -> "lunchLocked"
            "dinner" -> "dinnerLocked"
            else -> return
        }
        
        val newValue = when (mealType.lowercase()) {
            "breakfast" -> !current.breakfastLocked
            "lunch" -> !current.lunchLocked
            "dinner" -> !current.dinnerLocked
            else -> false
        }

        viewModelScope.launch {
            foodCountRepository.updateSingleLock(field, newValue)
            
            // Log Admin action
            authRepository.getSession()?.let { admin ->
                val action = if (newValue) "Locked $mealType" else "Unlocked $mealType"
                authRepository.logAction(admin, action)
            }
        }
    }

    fun loadSummary() {
        // Function removed, summary is now a reactive StateFlow
    }

    fun resetAllCounts() {
        viewModelScope.launch {
            foodCountRepository.resetAllFoodCounts(_today)
            // Log Admin action
            authRepository.getSession()?.let { admin ->
                authRepository.logAction(admin, "Force reset all student counts to 0")
            }
        }
    }

    fun resetMeal(mealType: String) {
        viewModelScope.launch {
            foodCountRepository.resetSpecificMeal(_today, mealType)
            // Log Admin action
            authRepository.getSession()?.let { admin ->
                authRepository.logAction(admin, "Force reset $mealType count to 0")
            }
        }
    }

    val allLocksOff: Boolean get() {
        val lock = _lockStatus.value
        return !lock.locked && !lock.breakfastLocked && !lock.lunchLocked && !lock.dinnerLocked
    }
}
