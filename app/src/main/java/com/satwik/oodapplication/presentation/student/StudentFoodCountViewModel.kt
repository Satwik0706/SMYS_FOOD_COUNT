package com.satwik.oodapplication.presentation.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satwik.oodapplication.data.model.FoodCount
import com.satwik.oodapplication.data.model.FoodRequest
import com.satwik.oodapplication.data.model.LockStatus
import com.satwik.oodapplication.data.model.User
import com.satwik.oodapplication.domain.repository.AuthRepository
import com.satwik.oodapplication.domain.repository.FoodCountRepository
import com.satwik.oodapplication.utils.Constants
import com.satwik.oodapplication.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class StudentFoodCountViewModel @Inject constructor(
    private val repository: FoodCountRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _foodCountState = MutableStateFlow<Resource<FoodCount>>(Resource.Loading())
    val foodCountState: StateFlow<Resource<FoodCount>> = _foodCountState

    private val _lockStatus = MutableStateFlow<Resource<LockStatus>>(Resource.Loading())
    val lockStatus: StateFlow<Resource<LockStatus>> = _lockStatus

    private val _snackStatus = MutableStateFlow<Resource<com.satwik.oodapplication.data.model.SnackStatus>>(Resource.Loading())
    val snackStatus: StateFlow<Resource<com.satwik.oodapplication.data.model.SnackStatus>> = _snackStatus

    private val _studentRequest = MutableStateFlow<Resource<FoodRequest?>>(Resource.Loading())
    val studentRequest: StateFlow<Resource<FoodRequest?>> = _studentRequest

    private val _adminWhatsApp = MutableStateFlow<Resource<String>>(Resource.Loading())
    val adminWhatsApp: StateFlow<Resource<String>> = _adminWhatsApp

    private val _requestActionState = MutableStateFlow<Resource<Unit>?>(null)
    val requestActionState: StateFlow<Resource<Unit>?> = _requestActionState

    fun loadData(studentId: String, date: String = LocalDate.now().toString()) {
        viewModelScope.launch {
            repository.getStudentFoodCount(studentId, date).collect {
                _foodCountState.value = it
            }
        }
        viewModelScope.launch {
            repository.getLockStatus(Constants.ACTIVE_LOCK_ID).collect {
                _lockStatus.value = it
            }
        }
        viewModelScope.launch {
            repository.getSnackStatus().collect {
                _snackStatus.value = it
            }
        }
        viewModelScope.launch {
            repository.getStudentRequest(studentId, date).collect {
                _studentRequest.value = it
            }
        }
        viewModelScope.launch {
            _adminWhatsApp.value = repository.getAdminWhatsAppNumber()
        }
    }

    fun submitRequest(user: User, breakfast: Boolean, lunch: Boolean, dinner: Boolean) {
        viewModelScope.launch {
            _requestActionState.value = Resource.Loading()
            val request = FoodRequest(
                studentId = user.uid,
                studentName = user.name,
                studentYear = user.year ?: "N/A",
                date = LocalDate.now().toString(),
                breakfast = breakfast,
                lunch = lunch,
                dinner = dinner
            )
            val result = repository.submitFoodRequest(request)
            _requestActionState.value = result
            
            if (result is Resource.Success) {
                authRepository.logAction(user, "Submitted missed count request for ${request.date}")
            }
        }
    }

    fun resetRequestState() {
        _requestActionState.value = null
    }

    fun toggleMeal(studentId: String, date: String, mealType: String) {
        val lock = (_lockStatus.value as? Resource.Success)?.data
        val snackLock = (_snackStatus.value as? Resource.Success)?.data
        val masterLocked = lock?.locked ?: false
        if (masterLocked) return

        val currentResource = _foodCountState.value
        if (currentResource !is Resource.Success) return
        
        val current = currentResource.data ?: return
        
        // Safety: Prevent toggle if isLeave is ON (except for the Leave toggle itself)
        if (current.isOnLeave && mealType.lowercase() != "leave") return

        val updated = when (mealType.lowercase()) {
            "breakfast" -> {
                if (lock?.breakfastLocked == true) return
                val nextState = !current.isBreakfast
                current.copy(
                    breakfast = nextState,
                    // If breakfast is turned OFF, lunch box must also be OFF
                    lunchBox = if (!nextState) false else current.isLunchBox
                )
            }
            "lunchbox" -> {
                if (lock?.breakfastLocked == true) return
                val nextState = !current.isLunchBox
                if (nextState) {
                    // MUTEX: If Lunch Box is ON -> Breakfast must be ON, Lunch must be OFF
                    current.copy(lunchBox = true, breakfast = true, lunch = false)
                } else {
                    current.copy(lunchBox = false)
                }
            }
            "lunch" -> {
                if (lock?.lunchLocked == true) return
                val nextState = !current.isLunch
                if (nextState) {
                    // MUTEX: If Lunch is ON -> Lunch Box must be OFF
                    current.copy(lunch = true, lunchBox = false)
                } else {
                    current.copy(lunch = false)
                }
            }
            "snack" -> {
                if (snackLock?.locked == true) return
                current.copy(snack = !current.isSnack)
            }
            "dinner" -> {
                if (lock?.dinnerLocked == true) return
                current.copy(dinner = !current.isDinner)
            }
            "leave" -> {
                // Cannot change leave status if main meals are locked
                if (lock?.breakfastLocked == true || lock?.lunchLocked == true || lock?.dinnerLocked == true) return

                val nextLeaveState = !current.isOnLeave
                if (nextLeaveState) {
                    // Force everything to 0 when going on leave
                    current.copy(isLeave = true, breakfast = false, lunch = false, snack = false, dinner = false, lunchBox = false)
                } else {
                    current.copy(isLeave = false)
                }
            }
            else -> current
        }

        viewModelScope.launch {
            // Optimistic UI update
            _foodCountState.value = Resource.Success(updated)
            // Persist (This now uses the 'updated' object which has NO NULLS for meal fields)
            repository.submitFoodCount(updated)
            
            // Log action
            authRepository.getSession()?.let { user ->
                authRepository.logAction(user, "Toggled $mealType to ${if (mealType.lowercase() == "leave") updated.isOnLeave else "updated"}")
            }
        }
    }
}
