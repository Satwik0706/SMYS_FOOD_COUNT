package com.satwik.oodapplication.data.model

import com.google.firebase.firestore.PropertyName

data class FoodRequest(
    @get:PropertyName("id")
    @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("studentId")
    @set:PropertyName("studentId")
    var studentId: String = "",

    @get:PropertyName("studentName")
    @set:PropertyName("studentName")
    var studentName: String = "",

    @get:PropertyName("studentYear")
    @set:PropertyName("studentYear")
    var studentYear: String = "",

    @get:PropertyName("date")
    @set:PropertyName("date")
    var date: String = "",

    @get:PropertyName("breakfast")
    @set:PropertyName("breakfast")
    var breakfast: Boolean = false,

    @get:PropertyName("lunch")
    @set:PropertyName("lunch")
    var lunch: Boolean = false,

    @get:PropertyName("dinner")
    @set:PropertyName("dinner")
    var dinner: Boolean = false,

    @get:PropertyName("timestamp")
    @set:PropertyName("timestamp")
    var timestamp: Long = 0L,

    @get:PropertyName("status")
    @set:PropertyName("status")
    var status: String = "PENDING",

    @get:PropertyName("originalValue")
    @set:PropertyName("originalValue")
    var originalValue: Boolean = true,

    @get:PropertyName("adminNote")
    @set:PropertyName("adminNote")
    var adminNote: String? = null
)
