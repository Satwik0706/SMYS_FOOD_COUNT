package com.satwik.oodapplication.utils

object Constants {
    const val ROLE_MANAGER = "manager"
    const val ROLE_ADMIN = "admin"
    const val ROLE_STUDENT = "student"
    const val ROLE_COOK = "cook"
    const val ROLE_DATA_ENTRY = "data_entry"

    const val COLLECTION_USERS = "users"
    const val COLLECTION_MENU = "menu"
    const val COLLECTION_NOTIFICATIONS = "notifications"
    const val COLLECTION_FOODCOUNTS = "foodcounts"
    const val COLLECTION_LOCK_STATUS = "lockstatus"
    const val COLLECTION_ATTENDANCE = "attendance"
    const val COLLECTION_LOGS = "logs"
    const val COLLECTION_REQUESTS = "requests"
    const val COLLECTION_BACKUPS = "backups"
    const val COLLECTION_SYSTEM = "system"
    const val DOCUMENT_CONFIG = "config"
    const val DOCUMENT_ADMIN_CONTACT = "admin_contact"

    const val ACTIVE_LOCK_ID = "active_lock_status"
    const val SNACK_LOCK_ID = "snack_lock_status"

    const val ATTENDANCE_ABSENT = "Absent"
    const val ATTENDANCE_LEAVE = "Leave"
    const val ATTENDANCE_PERMISSION = "Permission"
    const val ATTENDANCE_PRESENT = "Present"
    const val TOPIC_YEAR1 = "students_year1"
    const val TOPIC_YEAR2 = "students_year2"
    const val TOPIC_YEAR3 = "students_year3"
    const val TOPIC_YEAR4 = "students_year4"

    // Apps Script Proxy URL
    const val PROXY_NOTIFICATION_URL = "https://script.google.com/macros/s/AKfycbzzApcF4kD3qP7WPMI0p5rc8u7kDkd5iL8Vx20IRshhdd5Nu_ITv76I4E1yNXYwjKw/exec"

    const val PRIVACY_POLICY_URL = "https://sites.google.com/view/smys-food-count-privacy-policy/home"
    const val PREF_TERMS_ACCEPTED = "terms_accepted"
}
