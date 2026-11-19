const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const CUSTOMER_A = "customer_a_uid";
const CUSTOMER_B = "customer_b_uid";
const BRANCH_MGR_UID = "branch_mgr_uid";
const SUPER_ADMIN_UID = "super_admin_uid";
const SUPER_ADMIN_EMAIL = "uk9798989@gmail.com";
const BRANCH_1_EMAIL = "branch1.manager@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("menu_items").get());
  await assertFails(unauthDb.collection("orders").get());
  await assertFails(unauthDb.collection("users").doc(CUSTOMER_A).get());
});

test("Menu manager: Customer cannot mutate menu; SuperAdmin can CRUD menu", async () => {
  const custDb = testEnv.authenticatedContext(CUSTOMER_A, { email: "cust@gmail.com", email_verified: true }).firestore();
  const superDb = testEnv.authenticatedContext(SUPER_ADMIN_UID, { email: SUPER_ADMIN_EMAIL, email_verified: true }).firestore();
  const now = new Date(Date.now() - 2000);

  const validMenuPayload = {
    name: "Zinger Burger",
    description: "Crispy thigh fillet with spicy mayo",
    category: "SIGNATURE BURGERS",
    pricePkr: 650,
    imageKey: "zinger_burger",
    badgeText: "BESTSELLER",
    available: true,
    sortOrder: 1,
    createdAt: now,
    updatedAt: now,
  };

  await assertFails(custDb.collection("menu_items").doc("zinger_1").set(validMenuPayload));
  await assertSucceeds(superDb.collection("menu_items").doc("zinger_1").set(validMenuPayload));
  await assertSucceeds(custDb.collection("menu_items").get());
  await assertSucceeds(
    superDb.collection("menu_items").doc("zinger_1").update({
      pricePkr: 690,
      imageUrl: "https://firebasestorage.googleapis.com/v0/b/gen-lang-client-0251605363.firebasestorage.app/o/menu-items%2Fzinger_1%2Fphoto.jpg?alt=media",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  // Clearing imageUrl back to empty string succeeds (fallback to Artwork Theme)
  await assertSucceeds(
    superDb.collection("menu_items").doc("zinger_1").update({
      imageUrl: "",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  // Oversized or non-string imageUrl is rejected
  await assertFails(
    superDb.collection("menu_items").doc("zinger_1").update({
      imageUrl: "data:image/jpeg;base64," + "a".repeat(220050),
      updatedAt: new Date(Date.now() - 200),
    })
  );
  await assertFails(
    superDb.collection("menu_items").doc("zinger_bad").set({
      ...validMenuPayload,
      imageUrl: 12345,
    })
  );
  // Creating a menu item with imageUrl directly succeeds for SuperAdmin
  await assertSucceeds(
    superDb.collection("menu_items").doc("pasta_custom").set({
      ...validMenuPayload,
      name: "Lahori Special Pasta",
      imageUrl: "https://images.example.com/lahori-pasta.jpg",
    })
  );
  await assertFails(custDb.collection("menu_items").doc("zinger_1").delete());
  await assertSucceeds(superDb.collection("menu_items").doc("zinger_1").delete());
  await assertSucceeds(superDb.collection("menu_items").doc("pasta_custom").delete());
});

test("User profile isolation and privilege escalation guard", async () => {
  const custDb = testEnv.authenticatedContext(CUSTOMER_A, { email: "cust@gmail.com", email_verified: true }).firestore();
  const now = new Date(Date.now() - 2000);

  // Cannot self-assign SuperAdmin or BranchAdmin
  await assertFails(
    custDb.collection("users").doc(CUSTOMER_A).set({
      userId: CUSTOMER_A,
      email: "cust@gmail.com",
      displayName: "Customer A",
      role: "SuperAdmin",
      assignedBranchId: "",
      favoriteItemIds: [],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid Customer profile succeeds
  await assertSucceeds(
    custDb.collection("users").doc(CUSTOMER_A).set({
      userId: CUSTOMER_A,
      email: "cust@gmail.com",
      displayName: "Customer A",
      role: "Customer",
      assignedBranchId: "",
      favoriteItemIds: ["zinger_1"],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cannot escalate role on update
  await assertFails(
    custDb.collection("users").doc(CUSTOMER_A).update({
      role: "BranchAdmin",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Spoofed unverified SuperAdmin email is denied", async () => {
  const spoofDb = testEnv.authenticatedContext("spoof_uid", { email: SUPER_ADMIN_EMAIL, email_verified: false }).firestore();
  const now = new Date(Date.now() - 2000);
  await assertFails(
    spoofDb.collection("menu_items").doc("zinger_1").set({
      name: "Fake",
      description: "Spoof",
      category: "EVERYDAY VALUE",
      pricePkr: 100,
      imageKey: "zinger_burger",
      badgeText: "",
      available: true,
      sortOrder: 1,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Order isolation, guest checkout payload, and BranchAdmin scoping", async () => {
  const custADb = testEnv.authenticatedContext(CUSTOMER_A, { email: "custA@gmail.com", email_verified: true }).firestore();
  const custBDb = testEnv.authenticatedContext(CUSTOMER_B, { email: "custB@gmail.com", email_verified: true }).firestore();
  const branchMgrDb = testEnv.authenticatedContext(BRANCH_MGR_UID, { email: BRANCH_1_EMAIL, email_verified: true }).firestore();
  const superDb = testEnv.authenticatedContext(SUPER_ADMIN_UID, { email: SUPER_ADMIN_EMAIL, email_verified: true }).firestore();
  const now = new Date(Date.now() - 2000);

  const orderPayload = {
    userId: CUSTOMER_A,
    guestSessionId: "guest_a1b2c3d4",
    guestCheckout: true,
    customerName: "Ali Guest",
    customerEmail: "custA@gmail.com",
    customerPhone: "03001234567",
    orderType: "DELIVERY",
    deliveryAddress: "Main KLP Road, Taranda Muhammad Panah",
    branchId: "branch_1",
    branchName: "Lahori Food Cafe — Taranda Muhammad Panah",
    branchAdminEmails: [BRANCH_1_EMAIL],
    items: [{ itemId: "zinger_1", name: "Zinger Burger", quantity: 2, unitPricePkr: 650, lineTotalPkr: 1300 }],
    itemsSummary: "2x Zinger Burger",
    promoCode: "LAHORI20",
    discountPkr: 260,
    subtotalPkr: 1300,
    deliveryFeePkr: 150,
    totalPkr: 1190,
    status: "Confirmed",
    createdAt: now,
    updatedAt: now,
  };

  await assertSucceeds(custADb.collection("orders").doc("order_1").set(orderPayload));
  await assertSucceeds(custADb.collection("orders").where("userId", "==", CUSTOMER_A).get());
  await assertFails(custADb.collection("orders").get()); // unfiltered list denied
  await assertFails(custBDb.collection("orders").doc("order_1").get());
  await assertFails(custADb.collection("orders").doc("order_1").update({ status: "Delivered", updatedAt: new Date(Date.now() - 1000) }));

  // Seed BranchAdmin assignment doc keyed by lowercase email
  await assertSucceeds(
    superDb.collection("branch_admins").doc(BRANCH_1_EMAIL).set({
      email: BRANCH_1_EMAIL,
      role: "BranchAdmin",
      branchId: "branch_1",
      branchName: "Lahori Food Cafe — Taranda Muhammad Panah",
      createdAt: now,
      updatedAt: now,
    })
  );

  // BranchAdmin can query assigned branch orders via branchId and update status
  await assertSucceeds(branchMgrDb.collection("orders").where("branchId", "==", "branch_1").get());
  await assertSucceeds(branchMgrDb.collection("orders").doc("order_1").update({ status: "Preparing", updatedAt: new Date(Date.now() - 1000) }));

  // SuperAdmin can list all orders across branches
  await assertSucceeds(superDb.collection("orders").get());
});

test("Promotions: SuperAdmin can CRUD promos; Customer can only read promos", async () => {
  const custDb = testEnv.authenticatedContext(CUSTOMER_A, { email: "cust@gmail.com", email_verified: true }).firestore();
  const superDb = testEnv.authenticatedContext(SUPER_ADMIN_UID, { email: SUPER_ADMIN_EMAIL, email_verified: true }).firestore();
  const now = new Date(Date.now() - 2000);

  const promoPayload = {
    code: "LAHORI20",
    discountType: "PERCENT",
    discountValue: 20,
    minOrderPkr: 500,
    active: true,
    createdAt: now,
    updatedAt: now,
  };

  await assertFails(custDb.collection("promotions").doc("lahori20").set(promoPayload));
  await assertSucceeds(superDb.collection("promotions").doc("lahori20").set(promoPayload));
  await assertSucceeds(custDb.collection("promotions").where("code", "==", "LAHORI20").get());
  await assertFails(custDb.collection("promotions").doc("lahori20").delete());
  await assertSucceeds(superDb.collection("promotions").doc("lahori20").delete());
});

test("Reviews: Customer can review only own Delivered order; cross-user access denied", async () => {
  const custADb = testEnv.authenticatedContext(CUSTOMER_A, { email: "custA@gmail.com", email_verified: true }).firestore();
  const custBDb = testEnv.authenticatedContext(CUSTOMER_B, { email: "custB@gmail.com", email_verified: true }).firestore();
  const superDb = testEnv.authenticatedContext(SUPER_ADMIN_UID, { email: SUPER_ADMIN_EMAIL, email_verified: true }).firestore();
  const now = new Date(Date.now() - 2000);

  const orderPayload = {
    userId: CUSTOMER_A,
    guestSessionId: "",
    guestCheckout: false,
    customerName: "Ali",
    customerEmail: "custA@gmail.com",
    customerPhone: "03001234567",
    orderType: "DELIVERY",
    deliveryAddress: "Main KLP Road, Taranda Muhammad Panah",
    branchId: "branch_1",
    branchName: "Lahori Food Cafe — Taranda Muhammad Panah",
    branchAdminEmails: [],
    items: [{ itemId: "zinger_1", name: "Zinger Burger", quantity: 1, unitPricePkr: 650, lineTotalPkr: 650 }],
    itemsSummary: "1x Zinger Burger",
    promoCode: "",
    discountPkr: 0,
    subtotalPkr: 650,
    deliveryFeePkr: 150,
    totalPkr: 800,
    status: "Confirmed",
    createdAt: now,
    updatedAt: now,
  };

  await assertSucceeds(custADb.collection("orders").doc("order_rev_1").set(orderPayload));

  const reviewPayload = {
    orderId: "order_rev_1",
    userId: CUSTOMER_A,
    customerName: "Ali",
    branchId: "branch_1",
    branchName: "Lahori Food Cafe — Taranda Muhammad Panah",
    rating: 5,
    comment: "Crispy and hot!",
    itemsSummary: "1x Zinger Burger",
    createdAt: now,
    updatedAt: now,
  };

  // Non-existent review pre-check get() succeeds without throwing PERMISSION_DENIED
  await assertSucceeds(custADb.collection("reviews").doc("order_rev_1").get());

  // Denied while order is still Confirmed
  await assertFails(custADb.collection("reviews").doc("order_rev_1").set(reviewPayload));

  // Mark order Delivered
  await assertSucceeds(superDb.collection("orders").doc("order_rev_1").update({ status: "Delivered", updatedAt: new Date(Date.now() - 1000) }));

  // Customer A can submit and update review; Customer B cannot read or overwrite existing review
  await assertSucceeds(custADb.collection("reviews").doc("order_rev_1").set(reviewPayload));
  await assertSucceeds(custADb.collection("reviews").doc("order_rev_1").update({
    rating: 4,
    comment: "Updated rating",
    updatedAt: new Date(Date.now() - 500),
  }));
  await assertSucceeds(custADb.collection("reviews").where("userId", "==", CUSTOMER_A).get());
  await assertFails(custBDb.collection("reviews").doc("order_rev_1").get());
  await assertFails(custBDb.collection("reviews").get());
  await assertSucceeds(superDb.collection("reviews").get());
});

test("FCM tokens: owner can register/update/delete hashed token doc; cross-user and invalid writes denied", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const custADb = testEnv.authenticatedContext(CUSTOMER_A, { email: "custA@gmail.com", email_verified: true }).firestore();
  const custBDb = testEnv.authenticatedContext(CUSTOMER_B, { email: "custB@gmail.com", email_verified: true }).firestore();
  const now = new Date(Date.now() - 2000);
  const tokenId = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

  const validTokenPayload = {
    userId: CUSTOMER_A,
    token: "fcm_sample_token_value_1234567890abcdef",
    platform: "android",
    appVersion: "1.0",
    createdAt: now,
    updatedAt: now,
  };

  // Unauthenticated and cross-user access denied
  await assertFails(unauthDb.collection("users").doc(CUSTOMER_A).collection("fcm_tokens").doc(tokenId).set(validTokenPayload));
  await assertFails(custBDb.collection("users").doc(CUSTOMER_A).collection("fcm_tokens").doc(tokenId).set(validTokenPayload));

  // Invalid platform or future timestamp denied
  await assertFails(
    custADb.collection("users").doc(CUSTOMER_A).collection("fcm_tokens").doc("bad_plat").set({
      ...validTokenPayload,
      platform: "ios",
    })
  );
  await assertFails(
    custADb.collection("users").doc(CUSTOMER_A).collection("fcm_tokens").doc("bad_time").set({
      ...validTokenPayload,
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Owner create, list, partial update, and delete succeed
  await assertSucceeds(custADb.collection("users").doc(CUSTOMER_A).collection("fcm_tokens").doc(tokenId).set(validTokenPayload));
  await assertSucceeds(custADb.collection("users").doc(CUSTOMER_A).collection("fcm_tokens").get());
  await assertSucceeds(
    custADb.collection("users").doc(CUSTOMER_A).collection("fcm_tokens").doc(tokenId).update({
      token: "fcm_refreshed_token_value_abcdef123456",
      appVersion: "1.0",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(custBDb.collection("users").doc(CUSTOMER_A).collection("fcm_tokens").get());
  await assertFails(custBDb.collection("users").doc(CUSTOMER_A).collection("fcm_tokens").doc(tokenId).delete());
  await assertSucceeds(custADb.collection("users").doc(CUSTOMER_A).collection("fcm_tokens").doc(tokenId).delete());
});

test("Feedback: Owner can submit food & service ratings and query own feedback; cross-user & invalid writes denied", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const custADb = testEnv.authenticatedContext(CUSTOMER_A, { email: "custA@gmail.com", email_verified: true }).firestore();
  const custBDb = testEnv.authenticatedContext(CUSTOMER_B, { email: "custB@gmail.com", email_verified: true }).firestore();
  const superDb = testEnv.authenticatedContext(SUPER_ADMIN_UID, { email: SUPER_ADMIN_EMAIL, email_verified: true }).firestore();
  const now = new Date(Date.now() - 2000);

  const validFeedbackPayload = {
    userId: CUSTOMER_A,
    customerName: "Ali Customer",
    customerEmail: "custA@gmail.com",
    branchId: "branch_1",
    branchName: "Lahori Food Cafe — Taranda Muhammad Panah",
    foodRating: 5,
    serviceRating: 4,
    comment: "Crispy zinger and super polite staff!",
    createdAt: now,
    updatedAt: now,
  };

  // Unauthenticated and spoofed userId writes denied
  await assertFails(unauthDb.collection("feedback").doc("fb_1").set(validFeedbackPayload));
  await assertFails(custBDb.collection("feedback").doc("fb_1").set(validFeedbackPayload));

  // Invalid rating bounds (0 or 6) denied
  await assertFails(custADb.collection("feedback").doc("fb_bad_food").set({ ...validFeedbackPayload, foodRating: 0 }));
  await assertFails(custADb.collection("feedback").doc("fb_bad_svc").set({ ...validFeedbackPayload, serviceRating: 6 }));

  // Valid create succeeds; filtered owner query succeeds; cross-user read/list/delete denied
  await assertSucceeds(custADb.collection("feedback").doc("fb_1").set(validFeedbackPayload));
  await assertSucceeds(custADb.collection("feedback").where("userId", "==", CUSTOMER_A).get());
  await assertFails(custADb.collection("feedback").get()); // unfiltered list denied for non-SuperAdmin
  await assertFails(custBDb.collection("feedback").doc("fb_1").get());
  await assertFails(custADb.collection("feedback").doc("fb_1").update({ foodRating: 1, updatedAt: new Date(Date.now() - 1000) }));
  await assertFails(custBDb.collection("feedback").doc("fb_1").delete());

  // SuperAdmin can read and delete all feedback
  await assertSucceeds(superDb.collection("feedback").get());
  await assertSucceeds(superDb.collection("feedback").doc("fb_1").delete());
});

test("Multi-branch scalability: SuperAdmin manages unlimited branches & multiple branch admins with strict per-branch isolation and instant revocation", async () => {
  const custADb = testEnv.authenticatedContext(CUSTOMER_A, { email: "custA@gmail.com", email_verified: true }).firestore();
  const branch1MgrADb = testEnv.authenticatedContext(BRANCH_MGR_UID, { email: BRANCH_1_EMAIL, email_verified: true }).firestore();
  const branch1MgrBDb = testEnv.authenticatedContext("b1_mgr2_uid", { email: "branch1.shift2@gmail.com", email_verified: true }).firestore();
  const branch10MgrDb = testEnv.authenticatedContext("b10_mgr_uid", { email: "branch10.mgr@gmail.com", email_verified: true }).firestore();
  const superDb = testEnv.authenticatedContext(SUPER_ADMIN_UID, { email: SUPER_ADMIN_EMAIL, email_verified: true }).firestore();
  const now = new Date(Date.now() - 2000);

  const branch10Payload = {
    name: "Lahori Food Cafe — Khanpur Bypass",
    shortLabel: "Branch 10 (Khanpur)",
    shortAddress: "Bypass Chowk, Khanpur, Rahim Yar Khan",
    areas: ["Khanpur", "Bypass Road"],
    latitude: 28.6478,
    longitude: 70.6562,
    mapUrl: "https://share.google/exampleKhanpur",
    openHours: "12:00 PM - 2:00 AM",
    deliveryRadiusKm: 15,
    active: true,
    sortOrder: 10,
    createdAt: now,
    updatedAt: now,
  };

  // Only SuperAdmin can create/modify branches; BranchAdmins & Customers can read branches
  await assertFails(custADb.collection("branches").doc("branch_10").set(branch10Payload));
  await assertFails(branch1MgrADb.collection("branches").doc("branch_10").set(branch10Payload));
  await assertSucceeds(superDb.collection("branches").doc("branch_10").set(branch10Payload));
  await assertSucceeds(custADb.collection("branches").get());

  // Assign multiple admins to branch_1 and an admin to dynamic branch_10
  await assertFails(
    custADb.collection("branch_admins").doc(BRANCH_1_EMAIL).set({
      email: BRANCH_1_EMAIL, role: "BranchAdmin", branchId: "branch_1", branchName: "Lahori Food Cafe TMP", createdAt: now, updatedAt: now,
    })
  );
  await assertFails(
    superDb.collection("branch_admins").doc(BRANCH_1_EMAIL).set({
      email: BRANCH_1_EMAIL, role: "SuperAdmin", branchId: "branch_1", branchName: "Lahori Food Cafe TMP", createdAt: now, updatedAt: now,
    })
  );

  await assertSucceeds(
    superDb.collection("branch_admins").doc(BRANCH_1_EMAIL).set({
      email: BRANCH_1_EMAIL, role: "BranchAdmin", branchId: "branch_1", branchName: "Lahori Food Cafe — Taranda Muhammad Panah", createdAt: now, updatedAt: now,
    })
  );
  await assertSucceeds(
    superDb.collection("branch_admins").doc("branch1.shift2@gmail.com").set({
      email: "branch1.shift2@gmail.com", role: "BranchAdmin", branchId: "branch_1", branchName: "Lahori Food Cafe — Taranda Muhammad Panah", createdAt: now, updatedAt: now,
    })
  );
  await assertSucceeds(
    superDb.collection("branch_admins").doc("branch10.mgr@gmail.com").set({
      email: "branch10.mgr@gmail.com", role: "BranchAdmin", branchId: "branch_10", branchName: branch10Payload.name, createdAt: now, updatedAt: now,
    })
  );

  // Branch admin can query ONLY own staff record
  await assertSucceeds(branch10MgrDb.collection("branch_admins").where("email", "==", "branch10.mgr@gmail.com").get());
  await assertFails(branch10MgrDb.collection("branch_admins").get());
  await assertFails(custADb.collection("branch_admins").where("email", "==", BRANCH_1_EMAIL).get());

  // Customer places orders at branch_1 and branch_10
  const baseOrder = {
    userId: CUSTOMER_A,
    guestSessionId: "",
    guestCheckout: false,
    customerName: "Order One",
    customerEmail: "custA@gmail.com",
    customerPhone: "03001234567",
    orderType: "PICKUP",
    deliveryAddress: "Self Pickup",
    branchAdminEmails: [],
    items: [{ itemId: "zinger_1", name: "Zinger Burger", quantity: 1, unitPricePkr: 350, lineTotalPkr: 350 }],
    itemsSummary: "1x Zinger Burger",
    promoCode: "",
    discountPkr: 0,
    subtotalPkr: 350,
    deliveryFeePkr: 0,
    totalPkr: 350,
    status: "Confirmed",
    createdAt: now,
    updatedAt: now,
  };

  await assertSucceeds(custADb.collection("orders").doc("ord_b1").set({ ...baseOrder, branchId: "branch_1", branchName: "Lahori Food Cafe — Taranda Muhammad Panah" }));
  await assertSucceeds(custADb.collection("orders").doc("ord_b10").set({ ...baseOrder, branchId: "branch_10", branchName: branch10Payload.name }));

  // Both branch_1 admins see branch_1 order immediately (even without branchAdminEmails backfill); CANNOT see branch_10
  await assertSucceeds(branch1MgrADb.collection("orders").where("branchId", "==", "branch_1").get());
  await assertSucceeds(branch1MgrBDb.collection("orders").where("branchId", "==", "branch_1").get());
  await assertSucceeds(branch1MgrBDb.collection("orders").doc("ord_b1").update({ status: "Preparing", updatedAt: new Date(Date.now() - 500) }));
  await assertFails(branch1MgrADb.collection("orders").where("branchId", "==", "branch_10").get());
  await assertFails(branch1MgrADb.collection("orders").doc("ord_b10").get());
  await assertFails(branch1MgrADb.collection("orders").get()); // unfiltered denied

  // branch_10 manager sees ONLY branch_10 orders; CANNOT touch branch_1 or global menu/branches
  await assertSucceeds(branch10MgrDb.collection("orders").where("branchId", "==", "branch_10").get());
  await assertSucceeds(branch10MgrDb.collection("orders").doc("ord_b10").update({ status: "Preparing", updatedAt: new Date(Date.now() - 400) }));
  await assertFails(branch10MgrDb.collection("orders").doc("ord_b1").get());
  await assertFails(branch10MgrDb.collection("orders").doc("ord_b1").update({ status: "Delivered", updatedAt: new Date(Date.now() - 300) }));
  await assertFails(branch10MgrDb.collection("menu_items").doc("hack").delete());

  // Instant revocation: deleting branch1.shift2@gmail.com immediately blocks branch_1 access
  await assertSucceeds(superDb.collection("branch_admins").doc("branch1.shift2@gmail.com").delete());
  await assertFails(branch1MgrBDb.collection("orders").where("branchId", "==", "branch_1").get());
  await assertFails(branch1MgrBDb.collection("orders").doc("ord_b1").get());
  await assertFails(branch1MgrBDb.collection("orders").doc("ord_b1").update({ status: "Dispatched", updatedAt: new Date(Date.now() - 200) }));
});

test("Branch Admin Promo & Seasonal Banner isolation: scoped strictly to assigned branchId", async () => {
  const superDb = testEnv.authenticatedContext(SUPER_ADMIN_UID, { email: SUPER_ADMIN_EMAIL, email_verified: true }).firestore();
  const branch1MgrDb = testEnv.authenticatedContext("b1_promo_mgr", { email: "b1.promo@gmail.com", email_verified: true }).firestore();
  const branch2MgrDb = testEnv.authenticatedContext("b2_promo_mgr", { email: "b2.promo@gmail.com", email_verified: true }).firestore();
  const custDb = testEnv.authenticatedContext(CUSTOMER_A, { email: "cust@gmail.com", email_verified: true }).firestore();
  const now = new Date(Date.now() - 2000);
  const expiresNextWeek = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000);

  // Assign Branch 1 & Branch 2 managers
  await assertSucceeds(
    superDb.collection("branch_admins").doc("b1.promo@gmail.com").set({
      email: "b1.promo@gmail.com", role: "BranchAdmin", branchId: "branch_1", branchName: "Lahori Food Cafe — Taranda Muhammad Panah", createdAt: now, updatedAt: now,
    })
  );
  await assertSucceeds(
    superDb.collection("branch_admins").doc("b2.promo@gmail.com").set({
      email: "b2.promo@gmail.com", role: "BranchAdmin", branchId: "branch_2", branchName: "Lahori Food Cafe — Zahir Pir", createdAt: now, updatedAt: now,
    })
  );

  // SuperAdmin creates a global promo
  await assertSucceeds(
    superDb.collection("promotions").doc("global20").set({
      code: "GLOBAL20", discountType: "PERCENT", discountValue: 20, minOrderPkr: 500, active: true, branchId: "", branchName: "", expiresAt: null, createdAt: now, updatedAt: now,
    })
  );

  // Branch 1 Admin creates a temporary promo for branch_1 -> SUCCEEDS
  await assertSucceeds(
    branch1MgrDb.collection("promotions").doc("branch_1_tmp15").set({
      code: "TMP15", discountType: "PERCENT", discountValue: 15, minOrderPkr: 400, active: true, branchId: "branch_1", branchName: "Lahori Food Cafe TMP", expiresAt: expiresNextWeek, createdAt: now, updatedAt: now,
    })
  );

  // Branch 1 Admin updates own branch promo -> SUCCEEDS
  await assertSucceeds(
    branch1MgrDb.collection("promotions").doc("branch_1_tmp15").update({
      discountValue: 25, updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Branch 1 Admin CANNOT create global promo or branch_2 promo, nor mutate global/branch_2 promos
  await assertFails(
    branch1MgrDb.collection("promotions").doc("fakeglobal").set({
      code: "FAKEGLOBAL", discountType: "PERCENT", discountValue: 50, minOrderPkr: 0, active: true, branchId: "", branchName: "", expiresAt: null, createdAt: now, updatedAt: now,
    })
  );
  await assertFails(
    branch1MgrDb.collection("promotions").doc("branch_2_hack").set({
      code: "HACK", discountType: "FLAT", discountValue: 100, minOrderPkr: 0, active: true, branchId: "branch_2", branchName: "Zahir Pir", expiresAt: expiresNextWeek, createdAt: now, updatedAt: now,
    })
  );
  await assertFails(
    branch1MgrDb.collection("promotions").doc("global20").update({ active: false, updatedAt: new Date(Date.now() - 800) })
  );
  await assertFails(
    branch1MgrDb.collection("promotions").doc("branch_1_tmp15").update({ branchId: "branch_2", updatedAt: new Date(Date.now() - 700) })
  );
  await assertFails(
    branch2MgrDb.collection("promotions").doc("branch_1_tmp15").delete()
  );

  // Branch 1 Admin creates, updates seasonal banner for branch_1 (including direct cloud photo data URI) -> SUCCEEDS
  const validBanner = {
    branchId: "branch_1",
    branchName: "Lahori Food Cafe TMP",
    title: "Winter Pizza Fest",
    subtitle: "Exclusive TMP hot deal with free drink!",
    promoCode: "TMP15",
    badgeText: "SEASONAL OFFER",
    imageUrl: "data:image/jpeg;base64," + "A".repeat(4096),
    targetCategory: "EXTREME PIZZAS",
    active: true,
    expiresAt: expiresNextWeek,
    createdAt: now,
    updatedAt: now,
  };
  await assertSucceeds(branch1MgrDb.collection("branch_banners").doc("banner_tmp_1").set(validBanner));
  await assertSucceeds(branch1MgrDb.collection("branch_banners").doc("banner_tmp_1").update({ title: "Updated Winter Fest", updatedAt: new Date(Date.now() - 600) }));

  // SuperAdmin saves a direct cloud photo data URI on a menu item -> SUCCEEDS
  await assertSucceeds(
    superDb.collection("menu_items").doc("lfc_zinger_cloud_photo").set({
      name: "Zinger Burger",
      description: "Crispy golden fillet",
      category: "BURGERS",
      pricePkr: 450,
      imageKey: "zinger_burger",
      imageUrl: "data:image/jpeg;base64," + "B".repeat(8192),
      badgeText: "BESTSELLER",
      available: true,
      sortOrder: 1,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Branch 1 Admin CANNOT create banner for branch_2 or reassign branchId
  await assertFails(branch1MgrDb.collection("branch_banners").doc("banner_b2_hack").set({ ...validBanner, branchId: "branch_2" }));
  await assertFails(branch1MgrDb.collection("branch_banners").doc("banner_tmp_1").update({ branchId: "branch_2", updatedAt: new Date(Date.now() - 500) }));
  await assertFails(branch2MgrDb.collection("branch_banners").doc("banner_tmp_1").delete());

  // Customer can read banners & promos, CANNOT create/delete
  await assertSucceeds(custDb.collection("branch_banners").get());
  await assertSucceeds(custDb.collection("promotions").get());
  await assertFails(custDb.collection("branch_banners").doc("cust_banner").set(validBanner));
  await assertFails(custDb.collection("branch_banners").doc("banner_tmp_1").delete());

  // Branch 1 Admin can delete own promo & banner
  await assertSucceeds(branch1MgrDb.collection("promotions").doc("branch_1_tmp15").delete());
  await assertSucceeds(branch1MgrDb.collection("branch_banners").doc("banner_tmp_1").delete());

  // Revoking Branch 1 Admin immediately blocks further banner/promo creation
  await assertSucceeds(superDb.collection("branch_admins").doc("b1.promo@gmail.com").delete());
  await assertFails(branch1MgrDb.collection("branch_banners").doc("banner_after_revoke").set(validBanner));
});





