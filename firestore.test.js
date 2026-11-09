const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_test_user";
const BOB_UID = "bob_test_user";

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
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read saves or profiles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("saves").get());
});

test("Authenticated user: Alice can create and read her own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Chell",
      totalChambersCompleted: 3,
      totalPortalsFired: 42,
    })
  );
  const doc = await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  if (!doc.exists) throw new Error("Document should exist");
});

test("Authenticated user: Bob cannot read Alice's profile or saves", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Chell",
    });
    await context.firestore().collection("users").doc(ALICE_UID).collection("saves").doc("save1").set({
      userId: ALICE_UID,
      chamberId: "chamber_01",
      playerPosX: 100,
      playerPosY: 200,
      bluePortalActive: true,
      orangePortalActive: false,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("saves").doc("save1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("saves").get());
});

test("Authenticated user: Alice can save and query her own saves and records", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("saves").doc("save_quick").set({
      userId: ALICE_UID,
      chamberId: "chamber_02",
      chamberName: "Momentum Leap",
      playerPosX: 250.5,
      playerPosY: 400.0,
      playerVelX: 120.0,
      playerVelY: -350.0,
      bluePortalActive: true,
      orangePortalActive: true,
      bluePortalX: 200.0,
      bluePortalY: 500.0,
      bluePortalNormalX: 0.0,
      bluePortalNormalY: -1.0,
      orangePortalX: 800.0,
      orangePortalY: 100.0,
      orangePortalNormalX: 1.0,
      orangePortalNormalY: 0.0,
      elapsedSeconds: 15.4,
      portalsFired: 4,
      isCompleted: false,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("records").doc("chamber_02").set({
      userId: ALICE_UID,
      chamberId: "chamber_02",
      bestTimeSeconds: 14.8,
      minPortals: 4,
    })
  );

  const savesSnap = await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("saves").get());
  if (savesSnap.size !== 1) throw new Error("Expected 1 save");
});
