const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

function baseRoom(hostUid, pastDate) {
  return {
    roomCode: "AB3K9Z",
    hostId: hostUid,
    status: "LOBBY",
    impostorCount: 1,
    turnDurationSeconds: 45,
    selectedCategory: "ALL",
    currentRound: 0,
    category: "",
    genre: "",
    hint: "",
    memberUids: [hostUid],
    playerNames: { [hostUid]: "Alice" },
    playerScores: { [hostUid]: 0 },
    turnOrder: [],
    currentTurnIndex: 0,
    timerPaused: false,
    readyUids: [],
    votes: {},
    revealedImpostorUids: [],
    createdAt: pastDate,
    updatedAt: pastDate,
  };
}

test("Unauthenticated user cannot read or write users, rooms, or assignments", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      gamesPlayed: 0,
      wins: 0,
      impostorRounds: 0,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(unauthDb.collection("rooms").doc("AB3K9Z").get());
  await assertFails(unauthDb.collection("rooms").get());
  await assertFails(
    unauthDb
      .collection("rooms")
      .doc("AB3K9Z")
      .collection("assignments")
      .doc(ALICE_UID)
      .get()
  );
});

test("User profile CRUD enforces ownership, bounds, and rejects ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Agent Alice",
      gamesPlayed: 0,
      wins: 0,
      impostorRounds: 0,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Update nickname + stats
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Renamed",
      gamesPlayed: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject cross-user read/write
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Room lifecycle: create, join, rename, assignments isolation, voting, guess, and queries", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // 1. Alice creates room AB3K9Z
  await assertSucceeds(
    aliceDb.collection("rooms").doc("AB3K9Z").set(baseRoom(ALICE_UID, created))
  );

  // Non-existent room check returns cleanly without permission error
  await assertSucceeds(bobDb.collection("rooms").doc("ZZZZ99").get());

  // 2. Bob reads LOBBY room and joins
  await assertSucceeds(bobDb.collection("rooms").doc("AB3K9Z").get());
  await assertSucceeds(
    bobDb
      .collection("rooms")
      .doc("AB3K9Z")
      .update({
        memberUids: [ALICE_UID, BOB_UID],
        [`playerNames.${BOB_UID}`]: "Bob",
        [`playerScores.${BOB_UID}`]: 0,
        updatedAt: new Date(Date.now() - 5000),
      })
  );

  // 3. Bob renames himself
  await assertSucceeds(
    bobDb
      .collection("rooms")
      .doc("AB3K9Z")
      .update({
        [`playerNames.${BOB_UID}`]: "Detective Bob",
        updatedAt: new Date(Date.now() - 4000),
      })
  );

  // Bob cannot spoof Alice's name or change impostorCount
  await assertFails(
    bobDb
      .collection("rooms")
      .doc("AB3K9Z")
      .update({
        [`playerNames.${ALICE_UID}`]: "Spoofed",
        updatedAt: new Date(Date.now() - 3500),
      })
  );
  await assertFails(
    bobDb.collection("rooms").doc("AB3K9Z").update({
      impostorCount: 4,
      updatedAt: new Date(Date.now() - 3500),
    })
  );

  // 4. Query alignment: filtered memberUids succeeds, unfiltered fails
  await assertSucceeds(
    bobDb
      .collection("rooms")
      .where("memberUids", "array-contains", BOB_UID)
      .get()
  );
  await assertFails(bobDb.collection("rooms").get());

  // 5. Host starts round & writes private assignments
  const roundTs = new Date(Date.now() - 3000);
  await assertSucceeds(
    aliceDb
      .collection("rooms")
      .doc("AB3K9Z")
      .collection("assignments")
      .doc(ALICE_UID)
      .set({
        uid: ALICE_UID,
        role: "NORMAL",
        secretWord: "Espresso",
        category: "Food & Drink",
        genre: "Coffeehouse Beverages",
        hint: "Morning caffeine shot",
        savior: true,
        director: false,
        round: 1,
        updatedAt: roundTs,
      })
  );
  await assertSucceeds(
    aliceDb
      .collection("rooms")
      .doc("AB3K9Z")
      .collection("assignments")
      .doc(BOB_UID)
      .set({
        uid: BOB_UID,
        role: "IMPOSTOR",
        secretWord: "",
        category: "Food & Drink",
        genre: "Coffeehouse Beverages",
        hint: "Morning caffeine shot",
        savior: false,
        director: false,
        round: 1,
        updatedAt: roundTs,
      })
  );

  // Private hostSecrets: Alice (host) can write/read, Bob (non-host) CANNOT read/write
  await assertSucceeds(
    aliceDb
      .collection("rooms")
      .doc("AB3K9Z")
      .collection("hostSecrets")
      .doc("current")
      .set({
        hostId: ALICE_UID,
        secretWord: "Espresso",
        impostorUids: [BOB_UID],
        ultimateKillerUid: BOB_UID,
        saviorUid: ALICE_UID,
        directorUid: CHARLIE_UID,
        round: 1,
        updatedAt: roundTs,
      })
  );
  await assertSucceeds(
    aliceDb
      .collection("rooms")
      .doc("AB3K9Z")
      .collection("hostSecrets")
      .doc("current")
      .get()
  );
  await assertFails(
    bobDb
      .collection("rooms")
      .doc("AB3K9Z")
      .collection("hostSecrets")
      .doc("current")
      .get()
  );
  await assertFails(
    bobDb.collection("rooms").doc("AB3K9Z").collection("hostSecrets").get()
  );

  // Host transitions room to REVEAL
  await assertSucceeds(
    aliceDb.collection("rooms").doc("AB3K9Z").update({
      status: "REVEAL",
      currentRound: 1,
      category: "Food & Drink",
      genre: "Coffeehouse Beverages",
      hint: "Morning caffeine shot",
      turnOrder: [ALICE_UID, BOB_UID],
      currentTurnIndex: 0,
      turnStartedAt: roundTs,
      readyUids: [],
      votes: {},
      revealedImpostorUids: [],
      updatedAt: roundTs,
    })
  );

  // Stranger Charlie cannot read non-LOBBY room or join it
  await assertFails(charlieDb.collection("rooms").doc("AB3K9Z").get());
  await assertFails(
    charlieDb
      .collection("rooms")
      .doc("AB3K9Z")
      .update({
        memberUids: [ALICE_UID, BOB_UID, CHARLIE_UID],
        [`playerNames.${CHARLIE_UID}`]: "Charlie",
        [`playerScores.${CHARLIE_UID}`]: 0,
        updatedAt: new Date(Date.now() - 2500),
      })
  );

  // Private assignments: Bob can read his own, CANNOT read Alice's, CANNOT list
  await assertSucceeds(
    bobDb
      .collection("rooms")
      .doc("AB3K9Z")
      .collection("assignments")
      .doc(BOB_UID)
      .get()
  );
  await assertFails(
    bobDb
      .collection("rooms")
      .doc("AB3K9Z")
      .collection("assignments")
      .doc(ALICE_UID)
      .get()
  );
  await assertFails(
    bobDb.collection("rooms").doc("AB3K9Z").collection("assignments").get()
  );
  // Bob cannot overwrite his own assignment to NORMAL
  await assertFails(
    bobDb
      .collection("rooms")
      .doc("AB3K9Z")
      .collection("assignments")
      .doc(BOB_UID)
      .update({
        role: "NORMAL",
        secretWord: "Espresso",
        updatedAt: new Date(Date.now() - 2000),
      })
  );

  // 6. Bob marks ready in REVEAL
  await assertSucceeds(
    bobDb
      .collection("rooms")
      .doc("AB3K9Z")
      .update({
        readyUids: [BOB_UID],
        updatedAt: new Date(Date.now() - 1800),
      })
  );

  // 7. Host moves to PLAYING, Bob submits impostorGuess
  await assertSucceeds(
    aliceDb.collection("rooms").doc("AB3K9Z").update({
      status: "PLAYING",
      turnStartedAt: new Date(Date.now() - 1500),
      updatedAt: new Date(Date.now() - 1500),
    })
  );
  await assertSucceeds(
    bobDb.collection("rooms").doc("AB3K9Z").update({
      impostorGuess: "Espresso",
      impostorGuessBy: BOB_UID,
      updatedAt: new Date(Date.now() - 1200),
    })
  );

  // 7b. Secret round actions (Ultimate Killer + Agent Prediction) privacy checks
  await assertSucceeds(
    bobDb
      .collection("rooms")
      .doc("AB3K9Z")
      .collection("secretActions")
      .doc(BOB_UID)
      .set({
        uid: BOB_UID,
        round: 1,
        killedVoterUid: ALICE_UID,
        updatedAt: new Date(Date.now() - 1300),
      })
  );
  // Host (Alice) cannot peek at Bob's secretAction during PLAYING
  await assertFails(
    aliceDb
      .collection("rooms")
      .doc("AB3K9Z")
      .collection("secretActions")
      .doc(BOB_UID)
      .get()
  );
  await assertFails(
    aliceDb.collection("rooms").doc("AB3K9Z").collection("secretActions").get()
  );
  // Bob cannot write into Alice's secretActions doc
  await assertFails(
    bobDb
      .collection("rooms")
      .doc("AB3K9Z")
      .collection("secretActions")
      .doc(ALICE_UID)
      .set({
        uid: ALICE_UID,
        round: 1,
        predictedImpostorUid: BOB_UID,
        updatedAt: new Date(Date.now() - 1250),
      })
  );

  // 8. Host moves to VOTING, Bob casts vote, Host can now read secretActions for tally
  await assertSucceeds(
    aliceDb.collection("rooms").doc("AB3K9Z").update({
      status: "VOTING",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    aliceDb.collection("rooms").doc("AB3K9Z").collection("secretActions").get()
  );
  await assertSucceeds(
    bobDb
      .collection("rooms")
      .doc("AB3K9Z")
      .update({
        [`votes.${BOB_UID}`]: ALICE_UID,
        updatedAt: new Date(Date.now() - 800),
      })
  );

  // 8b. Update room settings with abilities enabled
  await assertSucceeds(
    aliceDb.collection("rooms").doc("AB3K9Z").update({
      gameMode: "CLASSIC",
      impostorCount: 1,
      turnDurationSeconds: 45,
      selectedCategory: "ALL",
      hintsEnabled: true,
      categoryCluesEnabled: true,
      timerEnabled: true,
      impostorAbilityEnabled: true,
      saviorAgentEnabled: true,
      agentDirectorEnabled: true,
      agentPredictionEnabled: true,
      updatedAt: new Date(Date.now() - 750),
    })
  );

  // 8c. Agent prediction & Savior shield secretAction mirroring Kotlin payload
  await assertSucceeds(
    aliceDb
      .collection("rooms")
      .doc("AB3K9Z")
      .collection("secretActions")
      .doc(ALICE_UID)
      .set({
        uid: ALICE_UID,
        round: 1,
        killedVoterUid: "",
        protectedPlayerUid: ALICE_UID,
        predictedImpostorUid: CHARLIE_UID,
        updatedAt: new Date(Date.now() - 700),
      })
  );

  // 8d. Reveal results with Ultimate Killer, Savior, Director, predictions, and negative delta score
  await assertSucceeds(
    aliceDb.collection("rooms").doc("AB3K9Z").update({
      status: "RESULTS",
      revealedWord: "Espresso",
      revealedImpostorUids: [BOB_UID],
      revealedKillerUid: BOB_UID,
      revealedNullifiedVoterUid: "",
      revealedSaviorUid: ALICE_UID,
      revealedProtectedUid: ALICE_UID,
      revealedSaviorSaved: true,
      revealedDirectorUid: CHARLIE_UID,
      revealedPredictions: { [ALICE_UID]: CHARLIE_UID },
      revealedRoundDeltas: { [ALICE_UID]: -1, [BOB_UID]: 2 },
      impostorGuessCorrect: true,
      playerScores: { [ALICE_UID]: -1, [BOB_UID]: 2 },
      timerPaused: true,
      updatedAt: new Date(Date.now() - 600),
    })
  );

  // 9. Shadow update rejected
  await assertFails(
    aliceDb.collection("rooms").doc("AB3K9Z").update({
      ghostField: "hack",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
