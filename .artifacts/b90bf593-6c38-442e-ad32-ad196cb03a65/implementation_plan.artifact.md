# Implementation Plan - Dual Notification System (Admin App + Apps Script)

This plan enables both automated daily notifications (via Google Apps Script) and manual "Push" notifications directly from the Administrator dashboard in the Android app.

## User Review Required

> [!IMPORTANT]
> **FCM Server Key Security**: You must paste your **Firebase Server Key** into both the `Constants.kt` file in the Android project and the Google Apps Script.
> *Note: Sending push notifications directly from an app is convenient for internal tools, but the Server Key will be present in the compiled APK.*

## Proposed Changes

### Core Configuration

#### [MODIFY] [Constants.kt](file:///D:/oodapplication/app/src/main/java/com/satwik/oodapplication/utils/Constants.kt)
- Add the FCM endpoint URL.
- Add a placeholder for the `FCM_SERVER_KEY`.

### Data Layer

#### [MODIFY] [FirebaseNotificationRepositoryImpl.kt](file:///D:/oodapplication/app/src/main/java/com/satwik/oodapplication/data/repository/FirebaseNotificationRepositoryImpl.kt)
- Implement `sendPushNotification` using `HttpURLConnection` and `JSONObject`.
- Update `sendNotification` to trigger the push notification if the `isPush` flag is set to true by the admin.
- Add logic to map "Target Year" strings to FCM topics (e.g., "All" -> `all_students`).

### Infrastructure (External)

#### [NEW] [DailyNotification.gs](file:///D:/oodapplication/.artifacts/b90bf593-6c38-442e-ad32-ad196cb03a65/scratch/DailyNotification.gs)
- The Google Apps Script code for the daily timer.

## Verification Plan

### Automated Tests
- N/A (Requires live Firebase connection)

### Manual Verification
1. **Admin Trigger**:
   - Open the App as Admin.
   - Go to **Notification Center**.
   - Click **Send**.
   - Enter details and check **"Send as System Pop-up (Push)"**.
   - Verify that another device receives a pop-up notification immediately.
2. **Apps Script Trigger**:
   - Run the `sendDailyNotification` function manually in the Apps Script editor.
   - Verify that all subscribed devices receive the notification.
