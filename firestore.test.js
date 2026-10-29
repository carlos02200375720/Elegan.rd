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
  const stats = user1Db.collection("publicationStats").doc("product1");
  const saveBatch = user1Db.batch();

  saveBatch.set(user1Saved, {
    userId: "user1",
    productId: "product1"
  });
  saveBatch.set(stats, {
    likesCount: 0,
    commentsCount: 0,
    sharesCount: 0,
    savesCount: 1,
    lastCommentId: ""
  });
  await assertSucceeds(saveBatch.commit());
  await assertSucceeds(user1Saved.get());
  await assertFails(otherUserSaved.get());
  await assertFails(otherUserSaved.set({
    userId: "user1",
    productId: "product2"
  }));
  const unsaveBatch = user1Db.batch();
  unsaveBatch.delete(user1Saved);
  unsaveBatch.update(stats, { savesCount: 0 });
  await assertSucceeds(unsaveBatch.commit());
});

test("Likes are unique per user and their counters change atomically", async () => {
  const user1Db = testEnv.authenticatedContext("user1").firestore();
  const user2Db = testEnv.authenticatedContext("user2").firestore();
  const like = user1Db.collection("users").doc("user1")
    .collection("likedPublications").doc("product1");
  const stats = user1Db.collection("publicationStats").doc("product1");
  const batch = user1Db.batch();

  batch.set(like, { userId: "user1", productId: "product1" });
  batch.set(stats, {
    likesCount: 1,
    commentsCount: 0,
    sharesCount: 0,
    savesCount: 0,
    lastCommentId: ""
  });
  await assertSucceeds(batch.commit());

  await assertSucceeds(like.get());
  await assertFails(
    user2Db.collection("users").doc("user1")
      .collection("likedPublications").doc("product1").get()
  );

  const unlikeBatch = user1Db.batch();
  unlikeBatch.delete(like);
  unlikeBatch.update(stats, { likesCount: 0 });
  await assertSucceeds(unlikeBatch.commit());
  await assertFails(stats.update({ likesCount: 50 }));
});

test("Authenticated users can add immutable comments with matching counters", async () => {
  const user1Db = testEnv.authenticatedContext("user1").firestore();
  const user2Db = testEnv.authenticatedContext("user2").firestore();
  const comment = user1Db.collection("products").doc("product1")
    .collection("comments").doc("comment1");
  const stats = user1Db.collection("publicationStats").doc("product1");
  const batch = user1Db.batch();

  batch.set(comment, {
    productId: "product1",
    userId: "user1",
    displayName: "Cliente",
    text: "Me gusta esta publicación"
  });
  batch.set(stats, {
    likesCount: 0,
    commentsCount: 1,
    sharesCount: 0,
    savesCount: 0,
    lastCommentId: "comment1"
  });
  await assertSucceeds(batch.commit());
  await assertSucceeds(
    user2Db.collection("products").doc("product1")
      .collection("comments").doc("comment1").get()
  );
  await assertFails(comment.update({ text: "Comentario editado" }));
});

test("Sharing a publication increments only its share counter", async () => {
  const userDb = testEnv.authenticatedContext("user1").firestore();
  const stats = userDb.collection("publicationStats").doc("product1");

  await assertSucceeds(stats.set({
    likesCount: 0,
    commentsCount: 0,
    sharesCount: 1,
    savesCount: 0,
    lastCommentId: ""
  }));
  await assertSucceeds(stats.update({ sharesCount: 2 }));
  await assertFails(stats.update({ likesCount: 100 }));
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
