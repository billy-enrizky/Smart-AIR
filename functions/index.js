const {onValueCreated, onValueWritten} = require("firebase-functions/v2/database");
const admin = require("firebase-admin");

admin.initializeApp();

/**
 * Cloud Function that listens for new notifications in the database
 * and sends FCM push notifications to parents using FCM V1 API
 */
exports.sendNotification = onValueCreated(
    {
      ref: "/notifications/{parentId}/{notificationId}",
      region: "us-central1",
    },
    async (event) => {
      const notificationData = event.data.val();
      const parentId = event.params.parentId;
      const notificationId = event.params.notificationId;

      console.log("New notification created:", notificationId, "for parent:", parentId);

      try {
        // Get parent's FCM token
        const parentRef = admin.database().ref(`/users/${parentId}/fcmToken`);
        const parentSnapshot = await parentRef.once("value");
        const fcmToken = parentSnapshot.val();

        if (!fcmToken) {
          console.log("No FCM token found for parent:", parentId);
          return null;
        }

        // Build notification message based on type
        let title = "Asthma App Alert";
        let body = "";
        let priority = "high";

        switch (notificationData.type) {
          case "triage_start":
            title = "Breathing Assessment Started";
            body = `${notificationData.childName || "Your child"} ` +
                   "started a breathing assessment";
            break;
          case "triage_escalation":
            title = "Emergency Guidance Shown";
            body = `${notificationData.childName || "Your child"} - ` +
                   "Emergency guidance shown. Please check on them.";
            priority = "high";
            break;
          case "rapid_rescue":
            title = "Rapid Rescue Alert";
            body = notificationData.message ||
                   `${notificationData.childName || "Your child"} ` +
                   "used rescue medication 3+ times in 3 hours. " +
                   "Consider seeking medical care.";
            priority = "high";
            break;
          default:
            body = notificationData.message || "You have a new notification";
        }

        // Prepare FCM V1 message
        const message = {
          token: fcmToken,
          notification: {
            title: title,
            body: body,
          },
          android: {
            priority: priority,
            notification: {
              sound: "default",
              channelId: "triage_notifications",
              priority: priority === "high" ? "high" : "normal",
            },
          },
          apns: {
            headers: {
              "apns-priority": priority === "high" ? "10" : "5",
            },
            payload: {
              aps: {
                sound: "default",
                badge: 1,
              },
            },
          },
          data: {
            type: notificationData.type || "unknown",
            childId: notificationData.childId || "",
            childName: notificationData.childName || "",
            sessionId: notificationData.sessionId || "",
            timestamp: String(notificationData.timestamp || Date.now()),
          },
        };

        // Send notification using FCM V1 API
        const response = await admin.messaging().send(message);
        console.log("Successfully sent notification:", response);

        // Mark notification as sent
        await event.data.ref.update({
          sent: true,
          sentAt: admin.database.ServerValue.TIMESTAMP,
          fcmMessageId: response,
        });

        return {success: true, messageId: response};
      } catch (error) {
        console.error("Error sending notification:", error);

        // Mark notification as failed
        await event.data.ref.update({
          sent: false,
          error: error.message,
          failedAt: admin.database.ServerValue.TIMESTAMP,
        });

        throw error;
      }
    });

/**
 * Cloud Function to handle FCM token registration
 * Called when a parent updates their FCM token
 */
exports.onTokenUpdate = onValueWritten(
    {
      ref: "/users/{userId}/fcmToken",
      region: "us-central1",
    },
    async (event) => {
      const userId = event.params.userId;
      const newToken = event.data.after.val();
      const oldToken = event.data.before.val();

      if (newToken && newToken !== oldToken) {
        console.log("FCM token updated for user:", userId);
        // Token is now stored and ready to use
      }

      return null;
    });

/**
 * Helper function to send notification directly
 * (can be called from other functions)
 * @param {string} token - FCM token
 * @param {string} title - Notification title
 * @param {string} body - Notification body
 * @param {Object} data - Additional data payload
 * @param {string} priority - Notification priority (high/normal)
 * @return {Promise<string>} FCM message ID
 */
async function sendFCMNotification(token, title, body, data = {}, priority = "high") {
  const message = {
    token: token,
    notification: {
      title: title,
      body: body,
    },
    android: {
      priority: priority,
      notification: {
        sound: "default",
        channelId: "triage_notifications",
        priority: priority === "high" ? "high" : "normal",
      },
    },
    data: {
      ...data,
      timestamp: String(Date.now()),
    },
  };

  try {
    const response = await admin.messaging().send(message);
    console.log("Successfully sent FCM notification:", response);
    return response;
  } catch (error) {
    console.error("Error sending FCM notification:", error);
    throw error;
  }
}

module.exports = {sendFCMNotification};
