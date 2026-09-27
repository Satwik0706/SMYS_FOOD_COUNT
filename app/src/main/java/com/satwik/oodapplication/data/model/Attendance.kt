package com.satwik.oodapplication.data.model

import com.google.firebase.firestore.PropertyName
import com.satwik.oodapplication.utils.Constants

data class Attendance(
    val studentId: String = "",
    val studentName: String = "",
    val date: String = "",
    val status: String = Constants.ATTENDANCE_PRESENT,
    @get:PropertyName("isPresent")
    @PropertyName("isPresent")
    val isPresent: Boolean = status == Constants.ATTENDANCE_PRESENT,
    val batch: String = ""
) {
    val resolvedStatus: String
        get() = if (status.isNotBlank()) status else if (isPresent) Constants.ATTENDANCE_PRESENT else Constants.ATTENDANCE_ABSENT
}
