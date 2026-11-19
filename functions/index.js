const {
  onDocumentCreated,
  onDocumentUpdated,
} = require("firebase-functions/v2/firestore");
const { logger } = require("firebase-functions");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

const DATABASE_ID = "ai-studio-android-lahorifo-70b911b7-52d3-4eb9-88ba-3940a68e0641";

function sanitizeTopicSegment(branchId) {
  return String(branchId || "branch_1").replace(/[^a-zA-Z0-9\-_.~%]/g, "_");
}

/**
 * Triggers real-time FCM push notifications to all assigned branch admins
 * (via their registered device tokens AND branch topic subscription) whenever
 * a new order is placed for their branch.
 */
exports.notifyBranchAdminsNewOrder = onDocumentCreated(
  {
    document: "orders/{orderId}",
    database: DATABASE_ID,
  },
  async (event) => {
    const orderData = event.data?.data();
    if (!orderData) return;

    const orderId = event.params.orderId;
    const branchId = String(orderData.branchId || "").trim();
    if (!branchId) {
      logger.warn("New order missing branchId; skipping branch admin FCM dispatch", { orderId });
      return;
    }

    const branchName = String(orderData.branchName || "Lahori Food Cafe");
    const customerName = String(orderData.customerName || "Customer");
    const orderType = String(orderData.orderType || "DELIVERY").toUpperCase();
    const totalPkr = Number(orderData.totalPkr || 0);
    const itemsSummary = String(orderData.itemsSummary || "New Order");
    const shortId = orderId.slice(-6).toUpperCase();
    const modeLabel = orderType === "PICKUP" ? "Pickup" : "Delivery";

    const title = `New ${modeLabel} Order #${shortId}`;
    const body = `${branchName} • Rs. ${totalPkr} — ${customerName}: ${itemsSummary}`;
    const tag = `new_order_${orderId}`;

    const commonData = {
      eventType: "NEW_ORDER",
      orderId,
      branchId,
      branchName,
      orderType,
      totalPkr: String(totalPkr),
      customerName,
      itemsSummary,
      status: String(orderData.status || "Confirmed"),
      title,
      body,
    };

    const androidConfig = {
      priority: "high",
      notification: {
        channelId: "lahori_branch_orders",
        color: "#E4002B",
        sound: "default",
        tag,
      },
    };

    const db = getFirestore(DATABASE_ID);
    const messaging = getMessaging();

    // 1. Resolve assigned branch admin emails from branch_admins collection
    const adminSnapshot = await db
      .collection("branch_admins")
      .where("branchId", "==", branchId)
      .get();

    const adminEmails = new Set();
    adminSnapshot.docs.forEach((doc) => {
      const email = String(doc.get("email") || "").trim().toLowerCase();
      if (email) adminEmails.add(email);
    });

    if (Array.isArray(orderData.branchAdminEmails)) {
      orderData.branchAdminEmails.forEach((entry) => {
        const email = String(entry || "").trim().toLowerCase();
        if (email) adminEmails.add(email);
      });
    }

    // 2. Collect unique FCM tokens for every matching branch admin user profile
    const tokenEntries = [];
    const seenTokens = new Set();

    await Promise.all(
      Array.from(adminEmails).map(async (email) => {
        const usersSnap = await db
          .collection("users")
          .where("email", "==", email)
          .get();

        await Promise.all(
          usersSnap.docs.map(async (userDoc) => {
            const tokensSnap = await userDoc.ref.collection("fcm_tokens").get();
            tokensSnap.docs.forEach((tokenDoc) => {
              const token = tokenDoc.get("token");
              if (typeof token === "string" && token.length >= 10 && !seenTokens.has(token)) {
                seenTokens.add(token);
                tokenEntries.push({ token, ref: tokenDoc.ref });
              }
            });
          })
        );
      })
    );

    let multicastSuccess = 0;
    let multicastFailure = 0;

    if (tokenEntries.length > 0) {
      const tokens = tokenEntries.map((entry) => entry.token);
      const response = await messaging.sendEachForMulticast({
        tokens,
        notification: { title, body },
        data: commonData,
        android: androidConfig,
      });
      multicastSuccess = response.successCount;
      multicastFailure = response.failureCount;

      const cleanupPromises = [];
      response.responses.forEach((sendResult, index) => {
        if (!sendResult.success) {
          const errorCode = sendResult.error?.code;
          if (
            errorCode === "messaging/registration-token-not-registered" ||
            errorCode === "messaging/invalid-registration-token"
          ) {
            cleanupPromises.push(tokenEntries[index].ref.delete());
          }
        }
      });
      if (cleanupPromises.length > 0) {
        await Promise.allSettled(cleanupPromises);
      }
    }

    // 3. Also send to the branch's FCM topic for instant delivery to active branch dashboards
    const topic = `branch_${sanitizeTopicSegment(branchId)}_orders`;
    let topicMessageId = "";
    try {
      topicMessageId = await messaging.send({
        topic,
        notification: { title, body },
        data: commonData,
        android: androidConfig,
      });
    } catch (topicErr) {
      logger.warn("Topic dispatch warning for new branch order", {
        orderId,
        branchId,
        topic,
        error: String(topicErr),
      });
    }

    logger.info("Dispatched branch admin new order FCM notifications", {
      orderId,
      branchId,
      adminCount: adminEmails.size,
      directTokens: tokenEntries.length,
      multicastSuccess,
      multicastFailure,
      topic,
      topicMessageId,
    });
  }
);

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
    const branchId = String(afterData.branchId || "");
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
        eventType: "ORDER_STATUS",
        orderId,
        branchId,
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
          tag: `order_status_${orderId}`,
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
