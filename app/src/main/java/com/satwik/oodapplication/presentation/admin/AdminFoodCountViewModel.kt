package com.satwik.oodapplication.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satwik.oodapplication.domain.repository.AuthRepository
import com.satwik.oodapplication.domain.repository.FoodCountRepository
import com.satwik.oodapplication.utils.Constants
import com.satwik.oodapplication.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class FoodCountReport(
    val totalBreakfast: Int = 0,
    val totalLunch: Int = 0,
    val totalSnack: Int = 0,
    val totalDinner: Int = 0,
    val totalOnLeave: Int = 0,
    val batchBreakdown: List<BatchFoodCount> = emptyList()
)

data class BatchFoodCount(
    val batchName: String,
    val breakfast: Int,
    val lunch: Int,
    val snack: Int,
    val dinner: Int,
    val onLeave: Int = 0,
    val students: List<StudentChoiceReport> = emptyList()
)

data class StudentChoiceReport(
    val name: String,
    val rollNumber: String,
    val breakfast: Boolean,
    val lunch: Boolean,
    val snack: Boolean,
    val dinner: Boolean,
    val lunchBox: Boolean,
    val isLeave: Boolean
)

@HiltViewModel
class AdminFoodCountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val foodCountRepository: FoodCountRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now().toString())
    val selectedDate: StateFlow<String> = _selectedDate

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val report: StateFlow<Resource<FoodCountReport>> = _selectedDate
        .flatMapLatest { date ->
            combine(
                authRepository.getUsersByRole(Constants.ROLE_STUDENT),
                foodCountRepository.getAllFoodCounts(date)
            ) { students, countsResource ->
                if (countsResource is Resource.Success) {
                    val countsMap = (countsResource.data ?: emptyList()).associateBy { it.studentId }
                    
                    var totalB = 0
                    var totalL = 0
                    var totalS = 0
                    var totalD = 0
                    var totalLeaveCount = 0
                    
                    val batchMap = mutableMapOf<String, MutableBatchData>()
                    
                    students.forEach { student ->
                        val count = countsMap[student.uid]
                        
                        // Priority: 1. Daily Record (if not null), 2. User Sticky Preferences
                        // We use the same exact logic as the Student Repo for total consistency.
                        val isStudentOnLeave = count?.isLeave ?: student.isLeave

                        val breakfast = count?.breakfast ?: student.breakfastPref
                        val lunch = count?.lunch ?: student.lunchPref
                        val snack = count?.snack ?: student.snackPref
                        val dinner = count?.dinner ?: student.dinnerPref
                        val lunchBox = count?.lunchBox ?: false

                        val finalB = if (isStudentOnLeave) false else breakfast
                        val finalL = if (isStudentOnLeave) false else lunch
                        val finalS = if (isStudentOnLeave) false else snack
                        val finalD = if (isStudentOnLeave) false else dinner
                        val finalLB = if (isStudentOnLeave) false else lunchBox

                        val batch = student.year ?: "Unknown"
                        val currentBatch = batchMap.getOrPut(batch) { MutableBatchData() }
                        
                        if (isStudentOnLeave) {
                            totalLeaveCount++
                            currentBatch.onLeave++
                        } else {
                            if (finalB) { totalB++; currentBatch.breakfast++ }
                            if (finalLB) { totalB++; currentBatch.breakfast++ } // Extra portion for lunch box
                            if (finalL) { totalL++; currentBatch.lunch++ }
                            if (finalS) { totalS++; currentBatch.snack++ }
                            if (finalD) { totalD++; currentBatch.dinner++ }
                        }
                        
                        currentBatch.students.add(
                            StudentChoiceReport(
                                name = student.name,
                                rollNumber = student.rollNumber ?: "N/A",
                                breakfast = finalB,
                                lunch = finalL,
                                snack = finalS,
                                dinner = finalD,
                                lunchBox = finalLB,
                                isLeave = isStudentOnLeave
                            )
                        )
                    }
                    
                    Resource.Success(
                        FoodCountReport(
                            totalBreakfast = totalB,
                            totalLunch = totalL,
                            totalSnack = totalS,
                            totalDinner = totalD,
                            totalOnLeave = totalLeaveCount,
                            batchBreakdown = batchMap.map { (name, data) ->
                                BatchFoodCount(
                                    name, 
                                    data.breakfast, 
                                    data.lunch, 
                                    data.snack, 
                                    data.dinner,
                                    data.onLeave,
                                    data.students.sortedBy { it.name }
                                )
                            }.sortedBy { it.batchName }
                        )
                    )
                } else if (countsResource is Resource.Error) {
                    Resource.Error(countsResource.message ?: "Unknown error")
                } else {
                    Resource.Loading()
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Resource.Loading()
        )

    fun setDate(date: String) {
        _selectedDate.value = date
    }
    
    private class MutableBatchData {
        var breakfast: Int = 0
        var lunch: Int = 0
        var snack: Int = 0
        var dinner: Int = 0
        var onLeave: Int = 0
        val students = mutableListOf<StudentChoiceReport>()
    }
}
