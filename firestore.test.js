const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085
    }
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

test("Unauthenticated user cannot read products", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("products").get());
});

test("Authenticated user can read and create valid product", async () => {
  const authDb = testEnv.authenticatedContext("user123").firestore();
  await assertSucceeds(authDb.collection("products").doc("p1").set({
    name: "Gorra Clásica Elegan",
    description: "Gorra de paño premium",
    category: "gorra",
    price: 45.0,
    imageUrl: "https://example.com/gorra.jpg",
    inStock: true
  }));
  await assertSucceeds(authDb.collection("products").doc("p1").get());
});

test("User cannot read or write to other user's cart", async () => {
  const user1Db = testEnv.authenticatedContext("user1").firestore();
  const user2Db = testEnv.authenticatedContext("user2").firestore();

  // User 1 adds item to own cart
  await assertSucceeds(user1Db.collection("users").doc("user1").collection("cart").doc("item1").set({
    userId: "user1",
    productId: "p1",
    name: "Gorra Clásica",
    price: 45.0,
    quantity: 1
  }));

  // User 2 cannot access user 1 cart
  await assertFails(user2Db.collection("users").doc("user1").collection("cart").doc("item1").get());
  await assertFails(user2Db.collection("users").doc("user1").collection("cart").doc("item2").set({
    userId: "user1",
    productId: "p2",
    name: "Gafas",
    price: 90.0,
    quantity: 1
  }));
});

test("Saved publications persist per user and cannot be accessed by another user", async () => {
  const user1Db = testEnv.authenticatedContext("user1").firestore();
  const user2Db = testEnv.authenticatedContext("user2").firestore();
  const user1Saved = user1Db.collection("users").doc("user1")
    .collection("savedPublications").doc("product1");
  const otherUserSaved = user2Db.collection("users").doc("user1")
    .collection("savedPublications").doc("product1");

  await assertSucceeds(user1Saved.set({
    userId: "user1",
    productId: "product1"
  }));
  await assertSucceeds(user1Saved.get());
  await assertFails(otherUserSaved.get());
  await assertFails(otherUserSaved.set({
    userId: "user1",
    productId: "product2"
  }));
  await assertSucceeds(user1Saved.delete());
});

test("User can create order in own orders collection but not update or delete", async () => {
  const user1Db = testEnv.authenticatedContext("user1").firestore();

  await assertSucceeds(user1Db.collection("users").doc("user1").collection("orders").doc("ord1").set({
    userId: "user1",
    customerName: "Carlos Gómez",
    email: "carlos@example.com",
    address: "Calle 123",
    paymentMethod: "Tarjeta",
    total: 135.0,
    status: "confirmado"
  }));

  // Orders are immutable
  await assertFails(user1Db.collection("users").doc("user1").collection("orders").doc("ord1").update({
    status: "cancelado"
  }));
});
