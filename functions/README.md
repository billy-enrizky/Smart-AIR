# Firebase Cloud Functions

This directory contains Firebase Cloud Functions that handle push notifications using FCM V1 API.

## Setup

1. Install Firebase CLI (if not already installed):
   ```bash
   npm install -g firebase-tools
   ```

2. Login to Firebase:
   ```bash
   firebase login
   ```

3. Install dependencies:
   ```bash
   cd functions
   npm install
   ```

## Functions

### `sendNotification`
- **Trigger**: Database write to `/notifications/{parentId}/{notificationId}`
- **Purpose**: Sends FCM push notifications to parents when triage events occur
- **Notification Types**:
  - `triage_start`: When child starts a breathing assessment
  - `triage_escalation`: When emergency guidance is shown
  - `rapid_rescue`: When child uses rescue medication 3+ times in 3 hours

### `onTokenUpdate`
- **Trigger**: Database write to `/users/{userId}/fcmToken`
- **Purpose**: Logs when FCM tokens are updated (for debugging)

## Deployment

1. Deploy functions:
   ```bash
   firebase deploy --only functions
   ```

2. Deploy specific function:
   ```bash
   firebase deploy --only functions:sendNotification
   ```

## Testing

1. Test locally with emulator:
   ```bash
   firebase emulators:start --only functions
   ```

2. View logs:
   ```bash
   firebase functions:log
   ```

## Requirements

- Node.js 18+
- Firebase project configured
- FCM V1 API enabled in Firebase Console
- Service account with appropriate permissions

## Notes

- The function automatically handles FCM token retrieval from the database
- Notifications are marked as sent/failed in the database
- Uses FCM V1 API (recommended, not legacy)
- Supports both Android and iOS notification formats

