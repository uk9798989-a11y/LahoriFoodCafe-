const { onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { logger } = require("firebase-functions");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

const DATABASE_ID = "ai-studio-android-lahorifo-70b911b7-52d3-4eb9-88ba-3940a68e0641";

/**
 * Triggers a real-time FCM push notification whenever an order transitions
 * from 'Preparing' to 'Dispatched' ('Ready for Pickup' for PICKUP orders,
 * 'Out for Delivery' for DELIVERY orders).
 */
exports.notifyOrderReadyForPickup = onDocumentUpdated(
  {
    document: "orders/{orderId}",
    database: DATABASE_ID,
  },
  async (event) => {
    const beforeData = event.data?.before?.data();
    const afterData = event.data?.after?.data();
    if (!beforeData || !afterData) return;

    const previousStatus = String(beforeData.status || "");
    const nextStatus = String(afterData.status || "");

    // Trigger specifically on the 'Preparing' -> 'Dispatched' (Ready for Pickup) transition
    if (previousStatus !== "Preparing" || nextStatus !== "Dispatched") {
      return;
    }

    const orderId = event.params.orderId;
    const userId = String(afterData.userId || "");
    if (!userId) {
      logger.warn("Order missing userId; skipping FCM dispatch", { orderId });
      return;
    }

    const orderType = String(afterData.orderType || "DELIVERY").toUpperCase();
    const isPickup = orderType === "PICKUP";
    const branchName = String(afterData.branchName || "Lahori Food Cafe");
    const itemsSummary = String(afterData.itemsSummary || "Your order");
    const shortId = orderId.slice(-6).toUpperCase();

    const title = isPickup
      ? "Lahori Food Cafe — Ready for Pickup"
      : "Lahori Food Cafe — Out for Delivery";
    const body = isPickup
      ? `Order #${shortId} is freshly packed and ready for pickup at ${branchName}!`
      : `Order #${shortId} from ${branchName} is out for delivery!`;

    const db = getFirestore(DATABASE_ID);
    const tokenSnapshot = await db
      .collection("users")
      .doc(userId)
      .collection("fcm_tokens")
      .get();

    if (tokenSnapshot.empty) {
      logger.info("No registered FCM tokens for user", { userId, orderId });
      return;
    }

    const tokenDocs = tokenSnapshot.docs;
    const tokens = tokenDocs
      .map((doc) => doc.get("token"))
      .filter((t) => typeof t === "string" && t.length >= 10);

    if (tokens.length === 0) return;

    const response = await getMessaging().sendEachForMulticast({
      tokens,
      notification: {
        title,
        body,
      },
      data: {
        orderId,
        previousStatus,
        status: isPickup ? "Ready for Pickup" : nextStatus,
        orderType,
        branchName,
        itemsSummary,
        title,
        body,
      },
      android: {
        priority: "high",
        notification: {
          channelId: "lahori_order_updates",
          color: "#E4002B",
          sound: "default",
        },
      },
    });

    // Prune expired or unregistered FCM tokens
    const cleanupPromises = [];
    response.responses.forEach((sendResult, index) => {
      if (!sendResult.success) {
        const errorCode = sendResult.error?.code;
        if (
          errorCode === "messaging/registration-token-not-registered" ||
          errorCode === "messaging/invalid-registration-token"
        ) {
          cleanupPromises.push(tokenDocs[index].ref.delete());
        }
      }
    });
    if (cleanupPromises.length > 0) {
      await Promise.allSettled(cleanupPromises);
    }

    logger.info("Sent FCM order status notification", {
      orderId,
      userId,
      previousStatus,
      nextStatus,
      isPickup,
      successCount: response.successCount,
      failureCount: response.failureCount,
    });
  }
);
