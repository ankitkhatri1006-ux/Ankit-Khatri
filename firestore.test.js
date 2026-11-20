const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_yodha_123";
const BOB_UID = "bob_yodha_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid, callsign = "VeerAlpha") {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    callsign,
    rankPoints: 1450,
    rankTier: "Gold",
    kills: 12,
    wins: 3,
    matchesPlayed: 15,
    seasonXp: 420,
    selectedGender: "BOY",
    hairStyle: "Fauji Fade",
    hairColor: "Jet Black",
    faceStyle: "Aviators",
    outfitStyle: "Maratha Armor",
    selectedEmote: "Bhangra Victory",
    primaryWeaponId: "m416",
    secondaryWeaponId: "awm",
    sidearmWeaponId: "p18c",
    meleeWeaponId: "desipan",
    unlockedAttachments: ["red_dot", "suppressor"],
    backpackItems: ["medkit", "frag_grenade"],
    graphicsPreset: "Ultra HD",
    antiCheatTrust: 100,
    createdAt: now,
    updatedAt: now,
  };
}

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

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("match_rooms").where("roomState", "==", "OPEN").get());
  await assertFails(unauthDb.collection("forum_posts").where("moderationStatus", "==", "PUBLISHED").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID)));
});

test("Owner can create and partially update profile; cross-user and ghost-field writes fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID)));

  // Partial update on mutable fields succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      selectedGender: "GIRL",
      outfitStyle: "Rajput Gold",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Immutable createdAt change fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Ghost field fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Future timestamp fails
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      ...validProfilePayload(BOB_UID, "BobPlayer"),
      createdAt: new Date(Date.now() + 60000),
    })
  );

  // Cross-user read & write fails
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ callsign: "Hijacked", updatedAt: new Date(Date.now() - 100) }));
});

test("Kavach Anti-Cheat logs are owner-scoped", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("anticheat_logs").doc("log_1").set({
      eventType: "SESSION_HASH",
      status: "VERIFIED",
      latencyMs: 18,
      integrityScore: 100,
      detail: "Clean memory and cadence telemetry",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("anticheat_logs").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("anticheat_logs").get());
});

test("Multiplayer match_rooms allow filtered OPEN list, squad join/voice update, and host controls", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("match_rooms").doc("room_jaipur_1").set({
      hostUid: ALICE_UID,
      hostName: "VeerAlpha",
      roomTitle: "Jaipur Ranked Squad",
      mapId: "jaipur_fort",
      modeId: "classic_br",
      rankBracket: "Gold",
      targetMmr: 1450,
      roomState: "OPEN",
      status: "WAITING",
      playerUids: [ALICE_UID],
      playerNames: ["VeerAlpha"],
      readyUids: [ALICE_UID],
      voiceActiveUids: [],
      maxPlayers: 4,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Filtered query matching rule succeeds; unfiltered query fails
  await assertSucceeds(bobDb.collection("match_rooms").where("roomState", "==", "OPEN").get());
  await assertFails(bobDb.collection("match_rooms").get());

  // Bob joins squad & enables voice
  await assertSucceeds(
    bobDb.collection("match_rooms").doc("room_jaipur_1").update({
      playerUids: [ALICE_UID, BOB_UID],
      playerNames: ["VeerAlpha", "TaraBravo"],
      voiceActiveUids: [BOB_UID],
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Non-host cannot change room status or delete room
  await assertFails(
    bobDb.collection("match_rooms").doc("room_jaipur_1").update({
      status: "IN_MATCH",
      updatedAt: new Date(Date.now() - 100),
    })
  );
  await assertFails(bobDb.collection("match_rooms").doc("room_jaipur_1").delete());

  // Host can start match
  await assertSucceeds(
    aliceDb.collection("match_rooms").doc("room_jaipur_1").update({
      status: "IN_MATCH",
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Community forum posts & comments enforce authorship, exists() parent check, and filtered queries", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("forum_posts").doc("post_1").set({
      authorUid: ALICE_UID,
      authorName: "VeerAlpha",
      authorRank: "Gold",
      category: "Loadouts",
      title: "Best M416 + AWM combo for Jaipur Fort",
      body: "Use vertical foregrip and suppressor on M416 with 4x scope for rooftop fights.",
      likesCount: 0,
      commentsCount: 0,
      likedByUids: [],
      moderationStatus: "PUBLISHED",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(bobDb.collection("forum_posts").where("moderationStatus", "==", "PUBLISHED").get());
  await assertFails(bobDb.collection("forum_posts").get());

  // Bob likes post
  await assertSucceeds(
    bobDb.collection("forum_posts").doc("post_1").update({
      likesCount: 1,
      likedByUids: [BOB_UID],
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob cannot edit Alice's title
  await assertFails(
    bobDb.collection("forum_posts").doc("post_1").update({
      title: "Hacked title",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob comments on existing post
  await assertSucceeds(
    bobDb.collection("forum_posts").doc("post_1").collection("comments").doc("c_1").set({
      postId: "post_1",
      authorUid: BOB_UID,
      authorName: "TaraBravo",
      body: "Agreed! Also carry 2 smokes for final zone.",
      moderationStatus: "PUBLISHED",
      createdAt: new Date(Date.now() - 400),
    })
  );

  // Comment on non-existent parent fails
  await assertFails(
    bobDb.collection("forum_posts").doc("missing_post").collection("comments").doc("c_2").set({
      postId: "missing_post",
      authorUid: BOB_UID,
      authorName: "TaraBravo",
      body: "Orphan comment",
      moderationStatus: "PUBLISHED",
      createdAt: new Date(Date.now() - 400),
    })
  );
});
