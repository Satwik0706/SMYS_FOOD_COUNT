package com.satwik.oodapplication.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satwik.oodapplication.data.model.Attendance
import com.satwik.oodapplication.data.model.FoodCount
import com.satwik.oodapplication.data.model.User
import com.satwik.oodapplication.domain.repository.AttendanceRepository
import com.satwik.oodapplication.domain.repository.AuthRepository
import com.satwik.oodapplication.domain.repository.FoodCountRepository
import com.satwik.oodapplication.utils.Constants
import com.satwik.oodapplication.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AttendanceConflict(
    val student: User,
    val status: String, // Constants.ATTENDANCE_ABSENT, ATTENDANCE_PERMISSION, or ATTENDANCE_LEAVE
    val isDinnerOrdered: Boolean
)

@HiltViewModel
class AdminAttendanceViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val attendanceRepository: AttendanceRepository,
    private val foodCountRepository: FoodCountRepository
) : ViewModel() {

    private val _date = MutableStateFlow(LocalDate.now().toString())
    val date: StateFlow<String> = _date

    private val _students = MutableStateFlow<List<User>>(emptyList())
    val students: StateFlow<List<User>> = _students

    private val _attendanceMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val attendanceMap: StateFlow<Map<String, String>> = _attendanceMap

    private val _foodCounts = MutableStateFlow<Map<String, FoodCount>>(emptyMap())
    val foodCounts: StateFlow<Map<String, FoodCount>> = _foodCounts

    private val _conflicts = MutableStateFlow<List<AttendanceConflict>>(emptyList())
    val conflicts: StateFlow<List<AttendanceConflict>> = _conflicts

    private val _uiState = MutableStateFlow<Resource<Unit>?>(null)
    val uiState: StateFlow<Resource<Unit>?> = _uiState

    init {
        loadData()
        autoCleanup()
    }

    private fun autoCleanup() {
        viewModelScope.launch {
            // Delete records older than 2 days (Past 2 days + today = 3 days total retention)
            val cleanupDate = LocalDate.now().minusDays(2).toString()
            attendanceRepository.clearOldAttendance(cleanupDate)
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            authRepository.getUsersByRole(Constants.ROLE_STUDENT).collect {
                _students.value = it
            }
        }
        viewModelScope.launch {
            attendanceRepository.getAttendanceForDate(_date.value).collect { resource ->
                if (resource is Resource.Success) {
                    val map = resource.data?.associate { it.studentId to it.resolvedStatus } ?: emptyMap()
                    _attendanceMap.value = map
                }
            }
        }
        viewModelScope.launch {
            foodCountRepository.getAllFoodCounts(_date.value).collect { resource ->
                if (resource is Resource.Success) {
                    _foodCounts.value = resource.data?.associateBy { it.studentId } ?: emptyMap()
                }
            }
        }
    }

    fun setAttendanceStatus(studentId: String, status: String) {
        val current = _attendanceMap.value.toMutableMap()
        current[studentId] = status
        _attendanceMap.value = current
    }

    fun submitAttendance() {
        viewModelScope.launch {
            _uiState.value = Resource.Loading()
            
            val countsResource = foodCountRepository.getAllFoodCounts(_date.value).first()
            val countsMap = if (countsResource is Resource.Success) {
                (countsResource.data ?: emptyList()).associateBy { it.studentId }
            } else {
                _foodCounts.value
            }

            val foundConflicts = mutableListOf<AttendanceConflict>()

            _students.value.forEach { student ->
                val status = _attendanceMap.value[student.uid] ?: Constants.ATTENDANCE_PRESENT
                if (status != Constants.ATTENDANCE_PRESENT) {
                    val count = countsMap[student.uid]
                    val hasOrderedDinner = if (count != null) {
                        count.isOnLeave != true && count.isDinner
                    } else {
                        !student.isLeave && student.dinnerPref
                    }

                    if (hasOrderedDinner) {
                        foundConflicts.add(
                            AttendanceConflict(
                                student = student,
                                status = status,
                                isDinnerOrdered = true
                            )
                        )
                    }
                }
            }

            if (foundConflicts.isNotEmpty()) {
                _conflicts.value = foundConflicts
                _uiState.value = null // Stop loading to show the conflict report dialog
                return@launch
            }

            saveAttendanceInternal()
        }
    }

    fun confirmAndSaveWithConflicts() {
        _conflicts.value = emptyList()
        viewModelScope.launch {
            _uiState.value = Resource.Loading()
            saveAttendanceInternal()
        }
    }

    fun revertConflictsToPresent() {
        val currentMap = _attendanceMap.value.toMutableMap()
        _conflicts.value.forEach { conflict ->
            currentMap[conflict.student.uid] = Constants.ATTENDANCE_PRESENT
        }
        _attendanceMap.value = currentMap
        _conflicts.value = emptyList()

        viewModelScope.launch {
            _uiState.value = Resource.Loading()
            saveAttendanceInternal()
        }
    }

    fun dismissConflicts() {
        _conflicts.value = emptyList()
    }

    private suspend fun saveAttendanceInternal() {
        val list = _students.value.map { student ->
            val status = _attendanceMap.value[student.uid] ?: Constants.ATTENDANCE_PRESENT
            Attendance(
                studentId = student.uid,
                studentName = student.name,
                date = _date.value,
                isPresent = status == Constants.ATTENDANCE_PRESENT,
                status = status,
                batch = student.year ?: "Unknown"
            )
        }
        val result = attendanceRepository.saveAttendance(list)
        _uiState.value = result
        
        if (result is Resource.Success) {
            authRepository.getSession()?.let { admin ->
                authRepository.logAction(admin, "Submitted attendance for ${_date.value}")
            }
        }
    }
}
