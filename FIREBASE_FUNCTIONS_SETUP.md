# Firebase Cloud Functions Setup Guide

## Overview
Firebase Cloud Functions have been set up to automatically send FCM push notifications to parents when triage events occur. The functions use the FCM V1 API (recommended, not deprecated).

## Files Created

1. **`functions/package.json`** - Node.js dependencies
2. **`functions/index.js`** - Cloud Functions code
3. **`functions/.eslintrc.js`** - ESLint configuration
4. **`firebase.json`** - Firebase project configuration
5. **`.firebaserc`** - Firebase project ID configuration
6. **`database.rules.json`** - Database security rules

## Setup Steps

### 1. Install Firebase CLI
```bash
npm install -g firebase-tools
```

### 2. Login to Firebase
```bash
firebase login
```

### 3. Install Function Dependencies
```bash
cd functions
npm install
cd ..
```

### 4. Deploy Functions
```bash
firebase deploy --only functions
```

## How It Works

### Flow:
1. **Child starts triage** → Android app writes to `/notifications/{parentId}/{notificationId}`
2. **Cloud Function triggers** → `sendNotification` function is called
3. **Function retrieves FCM token** → Gets parent's token from `/users/{parentId}/fcmToken`
4. **Function sends notification** → Uses FCM V1 API to send push notification
5. **Notification delivered** → Parent receives notification on their device

### Notification Types:

1. **Triage Start** (`triage_start`)
   - Title: "Breathing Assessment Started"
   - Body: "[Child Name] started a breathing assessment"

2. **Triage Escalation** (`triage_escalation`)
   - Title: "Emergency Guidance Shown"
   - Body: "[Child Name] - Emergency guidance shown. Please check on them."
   - Priority: High

3. **Rapid Rescue** (`rapid_rescue`)
   - Title: "Rapid Rescue Alert"
   - Body: "[Child Name] used rescue medication 3+ times in 3 hours. Consider seeking medical care."
   - Priority: High

## FCM Token Management

### Android App:
- When parent logs in, `ParentActivity` automatically gets and saves FCM token
- When token refreshes, `MyFirebaseMessagingService.onNewToken()` saves new token
- Token is stored at: `/users/{parentId}/fcmToken`

### Cloud Function:
- Automatically retrieves token from database when sending notifications
- Handles missing tokens gracefully (logs warning, doesn't crash)

## Testing

### Test Locally:
```bash
firebase emulators:start --only functions
```

### View Logs:
```bash
firebase functions:log
```

### Test Notification:
1. Trigger a triage session from child app
2. Check Firebase Console → Functions → Logs
3. Verify notification was sent

## Troubleshooting

### Function Not Triggering:
- Check Firebase Console → Functions → Logs for errors
- Verify database write is happening: Check `/notifications/{parentId}` in Firebase Console
- Ensure function is deployed: `firebase functions:list`

### Notifications Not Received:
- Verify FCM token exists: Check `/users/{parentId}/fcmToken` in Firebase Console
- Check notification permissions on Android device (Android 13+)
- Verify FCM V1 API is enabled in Firebase Console
- Check function logs for errors

### Token Issues:
- Token should be automatically saved when parent logs in
- If token is missing, parent should log out and log back in
- Check `ParentActivity` logs for token registration

## Database Rules

The `database.rules.json` file includes security rules:
- Users can only read/write their own data
- Notifications can be written by anyone (for Cloud Functions)
- Notifications can only be read by the parent

## Requirements

- Node.js 18+
- Firebase CLI installed
- Firebase project: `group7-66146`
- FCM V1 API enabled (already confirmed)
- Service account permissions (automatic with Firebase)

## Deployment Checklist

- [ ] Install Firebase CLI
- [ ] Login to Firebase
- [ ] Install function dependencies (`npm install` in `functions/` directory)
- [ ] Deploy functions (`firebase deploy --only functions`)
- [ ] Test by triggering a triage session
- [ ] Verify notifications are received
- [ ] Check function logs for any errors

## Next Steps

1. Deploy the functions using the commands above
2. Test with a real device (parent and child accounts)
3. Monitor function logs in Firebase Console
4. Adjust notification messages if needed in `functions/index.js`

