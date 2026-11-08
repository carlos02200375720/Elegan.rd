import express from "express";
import cors from "cors";
import helmet from "helmet";
import rateLimit from "express-rate-limit";
import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";
import { MongoClient } from "mongodb";
import { OAuth2Client } from "google-auth-library";
import { randomUUID } from "node:crypto";
import multer from "multer";
import { Storage } from "@google-cloud/storage";

const required = ["MONGODB_URI", "JWT_SECRET"];
for (const key of required) {
  if (!process.env[key]) throw new Error(`Missing required environment variable: ${key}`);
}
if (process.env.JWT_SECRET.length < 32) {
  throw new Error("JWT_SECRET must contain at least 32 characters");
}

const app = express();
app.disable("x-powered-by");
app.use(helmet());
app.use(express.json({ limit: "1mb" }));

const allowedOrigins = (process.env.CORS_ORIGINS || "")
  .split(",").map((value) => value.trim()).filter(Boolean);
app.use(cors({
  origin(origin, callback) {
    if (!origin || allowedOrigins.length === 0 || allowedOrigins.includes(origin)) {
      return callback(null, true);
    }
    return callback(new Error("Origin not allowed"));
  }
}));

const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  limit: 30,
  standardHeaders: "draft-7",
  legacyHeaders: false
});
const googleClient = new OAuth2Client(process.env.GOOGLE_WEB_CLIENT_ID);
const mongoOptions = process.env.MONGODB_USERNAME && process.env.MONGODB_PASSWORD
  ? { auth: { username: process.env.MONGODB_USERNAME, password: process.env.MONGODB_PASSWORD } }
  : {};
const mongo = new MongoClient(process.env.MONGODB_URI, mongoOptions);
await mongo.connect();
const db = mongo.db(process.env.MONGODB_DATABASE || undefined);
const users = db.collection("users");
await users.createIndex({ email: 1 }, { unique: true, sparse: true });
await users.createIndex({ googleSub: 1 }, { unique: true, sparse: true });

function publicUser(user) {
  return {
    id: user._id,
    email: user.email || null,
    name: user.name || "",
    photoUrl: user.photoUrl || "",
    role: user.role || "customer",
    provider: user.provider || "password",
    createdAt: user.createdAt
  };
}

function issueToken(user) {
  return jwt.sign(
    { sub: String(user._id), role: user.role || "customer" },
    process.env.JWT_SECRET,
    { expiresIn: "7d", issuer: "elegan-movil-api", audience: "elegan-android" }
  );
}

async function requireAuth(req, res, next) {
  try {
    const header = req.headers.authorization || "";
    const token = header.startsWith("Bearer ") ? header.slice(7) : "";
    if (!token) return res.status(401).json({ error: "AUTH_REQUIRED" });
    const payload = jwt.verify(token, process.env.JWT_SECRET, {
      issuer: "elegan-movil-api", audience: "elegan-android"
    });
    const user = await users.findOne({ _id: payload.sub });
    if (!user) return res.status(401).json({ error: "USER_NOT_FOUND" });
    req.user = user;
    next();
  } catch {
    return res.status(401).json({ error: "INVALID_SESSION" });
  }
}

app.get("/health", (_req, res) => res.json({ status: "ok", service: "elegan-movil-api" }));

app.post("/api/movil/auth/register", authLimiter, async (req, res) => {
  try {
    const email = String(req.body?.email || "").trim().toLowerCase();
    const password = String(req.body?.password || "");
    const name = String(req.body?.name || "").trim();
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      return res.status(400).json({ error: "INVALID_EMAIL" });
    }
    if (password.length < 8 || password.length > 128) {
      return res.status(400).json({ error: "PASSWORD_REQUIREMENTS" });
    }
    if (!name || name.length > 100) return res.status(400).json({ error: "INVALID_NAME" });
    const passwordHash = await bcrypt.hash(password, 12);
    const user = {
      _id: randomUUID(), email, name, passwordHash,
      role: "customer", provider: "password", createdAt: new Date(), updatedAt: new Date()
    };
    await users.insertOne(user);
    return res.status(201).json({ token: issueToken(user), user: publicUser(user) });
  } catch (error) {
    if (error?.code === 11000) return res.status(409).json({ error: "EMAIL_ALREADY_REGISTERED" });
    console.error("register failed", error);
    return res.status(500).json({ error: "INTERNAL_ERROR" });
  }
});

app.post("/api/movil/auth/login", authLimiter, async (req, res) => {
  try {
    const email = String(req.body?.email || "").trim().toLowerCase();
    const password = String(req.body?.password || "");
    const user = await users.findOne({ email });
    if (!user?.passwordHash || !(await bcrypt.compare(password, user.passwordHash))) {
      return res.status(401).json({ error: "INVALID_CREDENTIALS" });
    }
    return res.json({ token: issueToken(user), user: publicUser(user) });
  } catch (error) {
    console.error("login failed", error);
    return res.status(500).json({ error: "INTERNAL_ERROR" });
  }
});

app.post("/api/movil/auth/google", authLimiter, async (req, res) => {
  try {
    const idToken = String(req.body?.idToken || "");
    if (!idToken || !process.env.GOOGLE_WEB_CLIENT_ID) {
      return res.status(400).json({ error: "GOOGLE_SIGN_IN_NOT_CONFIGURED" });
    }
    const ticket = await googleClient.verifyIdToken({
      idToken, audience: process.env.GOOGLE_WEB_CLIENT_ID
    });
    const payload = ticket.getPayload();
    if (!payload?.sub || !payload.email || payload.email_verified !== true) {
      return res.status(401).json({ error: "INVALID_GOOGLE_IDENTITY" });
    }
    const email = payload.email.toLowerCase();
    let user = await users.findOne({ googleSub: payload.sub });
    if (!user) {
      user = await users.findOne({ email });
      if (user) {
        await users.updateOne(
          { _id: user._id },
          { $set: { googleSub: payload.sub, updatedAt: new Date() } }
        );
        user = { ...user, googleSub: payload.sub };
      } else {
        user = {
          _id: randomUUID(), email, name: payload.name || "",
          photoUrl: payload.picture || "", googleSub: payload.sub,
          role: "customer", provider: "google",
          createdAt: new Date(), updatedAt: new Date()
        };
        await users.insertOne(user);
      }
    }
    return res.json({ token: issueToken(user), user: publicUser(user) });
  } catch (error) {
    console.error("Google sign-in failed", error);
    return res.status(401).json({ error: "GOOGLE_AUTH_FAILED" });
  }
});

app.get("/api/movil/auth/me", requireAuth, (req, res) => {
  res.json({ user: publicUser(req.user) });
});

app.post("/api/movil/auth/logout", (_req, res) => {
  // JWT sessions expire after seven days; the Android client must discard its local token.
  res.status(204).end();
});


function requireAdmin(req, res, next) {
  if (req.user?.role !== "admin") return res.status(403).json({ error: "ADMIN_REQUIRED" });
  next();
}
function owned(userId, id) { return { userId, _id: id }; }
function cleanObject(value) {
  if (!value || typeof value !== "object" || Array.isArray(value)) return {};
  const result = { ...value };
  delete result._id;
  delete result.userId;
  delete result.role;
  delete result.createdAt;
  delete result.updatedAt;
  return result;
}
function sendError(res, error, label) {
  console.error(label, error);
  return res.status(500).json({ error: "INTERNAL_ERROR" });
}

// Catálogo y anuncios: lectura pública; escritura exclusiva del administrador.
app.get("/api/movil/products", async (_req, res) => {
  try {
    const items = await db.collection("products").find({}).sort({ createdAt: -1 }).toArray();
    res.json({ items: items.map(({ _id, ...item }) => ({ ...item, id: String(_id) })) });
  } catch (error) { sendError(res, error, "products list failed"); }
});
app.get("/api/movil/products/:id", async (req, res) => {
  try {
    const item = await db.collection("products").findOne({ _id: req.params.id });
    if (!item) return res.status(404).json({ error: "PRODUCT_NOT_FOUND" });
    const { _id, ...data } = item;
    res.json({ item: { ...data, id: String(_id) } });
  } catch (error) { sendError(res, error, "product read failed"); }
});
app.post("/api/movil/products", requireAuth, requireAdmin, async (req, res) => {
  try {
    const data = cleanObject(req.body);
    if (!String(data.name || "").trim()) return res.status(400).json({ error: "INVALID_PRODUCT" });
    const item = { ...data, _id: String(data.id || randomUUID()), createdAt: new Date(), updatedAt: new Date() };
    delete item.id;
    await db.collection("products").insertOne(item);
    res.status(201).json({ id: item._id, item: { ...item, id: item._id, _id: undefined } });
  } catch (error) { sendError(res, error, "product create failed"); }
});
app.patch("/api/movil/products/:id", requireAuth, requireAdmin, async (req, res) => {
  try {
    const data = cleanObject(req.body);
    const result = await db.collection("products").updateOne({ _id: req.params.id }, { $set: { ...data, updatedAt: new Date() } });
    if (!result.matchedCount) return res.status(404).json({ error: "PRODUCT_NOT_FOUND" });
    res.json({ success: true });
  } catch (error) { sendError(res, error, "product update failed"); }
});
app.delete("/api/movil/products/:id", requireAuth, requireAdmin, async (req, res) => {
  try {
    await db.collection("products").deleteOne({ _id: req.params.id });
    res.json({ success: true });
  } catch (error) { sendError(res, error, "product delete failed"); }
});

app.get("/api/movil/announcements", async (_req, res) => {
  try {
    const items = await db.collection("announcements").find({ isActive: true }).sort({ createdAt: -1 }).toArray();
    res.json({ items: items.map(({ _id, ...item }) => ({ ...item, id: String(_id) })) });
  } catch (error) { sendError(res, error, "announcements list failed"); }
});
app.post("/api/movil/announcements", requireAuth, requireAdmin, async (req, res) => {
  try {
    const data = cleanObject(req.body);
    if (!String(data.title || "").trim()) return res.status(400).json({ error: "INVALID_ANNOUNCEMENT" });
    const item = { ...data, _id: String(data.id || randomUUID()), createdAt: new Date(), updatedAt: new Date() };
    delete item.id;
    await db.collection("announcements").insertOne(item);
    res.status(201).json({ id: item._id });
  } catch (error) { sendError(res, error, "announcement create failed"); }
});
app.patch("/api/movil/announcements/:id", requireAuth, requireAdmin, async (req, res) => {
  try {
    await db.collection("announcements").updateOne({ _id: req.params.id }, { $set: { ...cleanObject(req.body), updatedAt: new Date() } });
    res.json({ success: true });
  } catch (error) { sendError(res, error, "announcement update failed"); }
});
app.delete("/api/movil/announcements/:id", requireAuth, requireAdmin, async (req, res) => {
  try {
    await db.collection("announcements").deleteOne({ _id: req.params.id });
    res.json({ success: true });
  } catch (error) { sendError(res, error, "announcement delete failed"); }
});

// Favoritos, likes, comentarios y estadísticas quedan aislados por usuario autenticado.
app.get("/api/movil/saved", requireAuth, async (req, res) => {
  try {
    const rows = await db.collection("savedProducts").find({ userId: String(req.user._id) }).toArray();
    res.json({ ids: rows.map(x => x.productId) });
  } catch (error) { sendError(res, error, "saved products failed"); }
});
app.put("/api/movil/saved/:productId", requireAuth, async (req, res) => {
  try {
    const filter = { userId: String(req.user._id), productId: req.params.productId };
    if (req.body?.saved === false) await db.collection("savedProducts").deleteOne(filter);
    else await db.collection("savedProducts").updateOne(filter, { $setOnInsert: { ...filter, createdAt: new Date() } }, { upsert: true });
    res.json({ saved: req.body?.saved !== false });
  } catch (error) { sendError(res, error, "save product failed"); }
});
app.get("/api/movil/publications/stats", async (_req, res) => {
  try {
    const [likes, comments, shares, saves] = await Promise.all([
      db.collection("publicationLikes").aggregate([{ $group: { _id: "$productId", count: { $sum: 1 } } }]).toArray(),
      db.collection("publicationComments").aggregate([{ $group: { _id: "$productId", count: { $sum: 1 } } }]).toArray(),
      db.collection("publicationShares").aggregate([{ $group: { _id: "$productId", count: { $sum: 1 } } }]).toArray(),
      db.collection("savedProducts").aggregate([{ $group: { _id: "$productId", count: { $sum: 1 } } }]).toArray()
    ]);
    const ids = new Set([...likes, ...comments, ...shares, ...saves].map(x => x._id));
    const stats = {};
    for (const id of ids) stats[id] = {
      likesCount: likes.find(x => x._id === id)?.count || 0,
      commentsCount: comments.find(x => x._id === id)?.count || 0,
      sharesCount: shares.find(x => x._id === id)?.count || 0,
      savesCount: saves.find(x => x._id === id)?.count || 0
    };
    res.json({ stats });
  } catch (error) { sendError(res, error, "publication stats failed"); }
});
app.get("/api/movil/publications/likes", requireAuth, async (req, res) => {
  try {
    const rows = await db.collection("publicationLikes").find({ userId: String(req.user._id) }).toArray();
    res.json({ ids: rows.map(x => x.productId) });
  } catch (error) { sendError(res, error, "publication likes failed"); }
});
app.put("/api/movil/publications/:productId/like", requireAuth, async (req, res) => {
  try {
    const filter = { userId: String(req.user._id), productId: req.params.productId };
    const col = db.collection("publicationLikes");
    const exists = await col.findOne(filter);
    if (exists) await col.deleteOne(filter);
    else await col.insertOne({ ...filter, createdAt: new Date() });
    res.json({ liked: !exists });
  } catch (error) { sendError(res, error, "publication like failed"); }
});
app.get("/api/movil/publications/:productId/comments", async (req, res) => {
  try {
    const items = await db.collection("publicationComments").find({ productId: req.params.productId }).sort({ createdAt: 1 }).toArray();
    res.json({ items: items.map(({ _id, ...item }) => ({ ...item, id: String(_id) })) });
  } catch (error) { sendError(res, error, "comments list failed"); }
});
app.post("/api/movil/publications/:productId/comments", requireAuth, async (req, res) => {
  try {
    const value = String(req.body?.text || "").trim();
    if (!value || value.length > 2000) return res.status(400).json({ error: "INVALID_COMMENT" });
    const item = { _id: randomUUID(), productId: req.params.productId, userId: String(req.user._id), displayName: String(req.body?.displayName || req.user.name || "Cliente"), text: value, createdAt: new Date() };
    await db.collection("publicationComments").insertOne(item);
    res.status(201).json({ id: item._id });
  } catch (error) { sendError(res, error, "comment create failed"); }
});
app.post("/api/movil/publications/:productId/share", requireAuth, async (req, res) => {
  try {
    await db.collection("publicationShares").insertOne({ userId: String(req.user._id), productId: req.params.productId, createdAt: new Date() });
    res.status(201).json({ success: true });
  } catch (error) { sendError(res, error, "publication share failed"); }
});

// Carrito, órdenes, mensajes y perfil: el servidor deriva siempre el userId del JWT.
app.get("/api/movil/cart", requireAuth, async (req, res) => {
  try {
    const items = await db.collection("cart").find({ userId: String(req.user._id) }).sort({ createdAt: 1 }).toArray();
    res.json({ items: items.map(({ _id, ...item }) => ({ ...item, id: String(_id) })) });
  } catch (error) { sendError(res, error, "cart list failed"); }
});
app.post("/api/movil/cart", requireAuth, async (req, res) => {
  try {
    const data = cleanObject(req.body);
    const productId = String(data.productId || "");
    const product = await db.collection("products").findOne({ _id: productId });
    if (!product) return res.status(404).json({ error: "PRODUCT_NOT_FOUND" });
    const quantity = Number(data.quantity) || 1;
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > 99) return res.status(400).json({ error: "INVALID_QUANTITY" });
    const item = {
      _id: randomUUID(), userId: String(req.user._id), productId,
      name: product.name || "", price: Number(product.price) || 0,
      imageUrl: product.imageUrl || "", selectedSize: String(data.selectedSize || ""),
      selectedColor: String(data.selectedColor || ""), quantity,
      createdAt: new Date(), updatedAt: new Date()
    };
    await db.collection("cart").insertOne(item);
    res.status(201).json({ id: item._id });
  } catch (error) { sendError(res, error, "cart add failed"); }
});
app.patch("/api/movil/cart/:id", requireAuth, async (req, res) => {
  try {
    const quantity = Number(req.body?.quantity);
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > 99) return res.status(400).json({ error: "INVALID_QUANTITY" });
    const result = await db.collection("cart").updateOne(owned(String(req.user._id), req.params.id), { $set: { quantity, updatedAt: new Date() } });
    if (!result.matchedCount) return res.status(404).json({ error: "CART_ITEM_NOT_FOUND" });
    res.json({ success: true });
  } catch (error) { sendError(res, error, "cart update failed"); }
});
app.delete("/api/movil/cart/:id", requireAuth, async (req, res) => {
  try {
    await db.collection("cart").deleteOne(owned(String(req.user._id), req.params.id));
    res.json({ success: true });
  } catch (error) { sendError(res, error, "cart remove failed"); }
});
app.delete("/api/movil/cart", requireAuth, async (req, res) => {
  try { await db.collection("cart").deleteMany({ userId: String(req.user._id) }); res.json({ success: true }); }
  catch (error) { sendError(res, error, "cart clear failed"); }
});
app.get("/api/movil/orders", requireAuth, async (req, res) => {
  try {
    const filter = req.user.role === "admin" ? {} : { userId: String(req.user._id) };
    const items = await db.collection("orders").find(filter).sort({ createdAt: -1 }).toArray();
    res.json({ items: items.map(({ _id, ...item }) => ({ ...item, id: String(_id) })) });
  } catch (error) { sendError(res, error, "orders list failed"); }
});
app.post("/api/movil/orders", requireAuth, async (req, res) => {
  try {
    const data = cleanObject(req.body);
    if (!String(data.customerName || "").trim()) return res.status(400).json({ error: "INVALID_ORDER" });
    const userId = String(req.user._id);
    const cartItems = await db.collection("cart").find({ userId }).toArray();
    if (!cartItems.length) return res.status(400).json({ error: "CART_EMPTY" });
    const total = cartItems.reduce((sum, item) => sum + (Number(item.price) || 0) * (Number(item.quantity) || 0), 0);
    const itemCount = cartItems.reduce((sum, item) => sum + (Number(item.quantity) || 0), 0);
    const itemsSummary = cartItems.map(item => `${item.name} x${item.quantity}`).join(", ");
    const item = {
      ...data, total, itemCount, itemsSummary, _id: randomUUID(), userId,
      status: "confirmado", createdAt: new Date()
    };
    await db.collection("orders").insertOne(item);
    await db.collection("cart").deleteMany({ userId });
    res.status(201).json({ id: item._id });
  } catch (error) { sendError(res, error, "order create failed"); }
});
app.get("/api/movil/messages", requireAuth, async (req, res) => {
  try {
    const items = await db.collection("supportMessages").find({ userId: String(req.user._id) }).sort({ createdAt: 1 }).toArray();
    res.json({ items: items.map(({ _id, ...item }) => ({ ...item, id: String(_id) })) });
  } catch (error) { sendError(res, error, "messages list failed"); }
});
app.post("/api/movil/messages", requireAuth, async (req, res) => {
  try {
    const value = String(req.body?.text || "").trim();
    if (!value || value.length > 4000) return res.status(400).json({ error: "INVALID_MESSAGE" });
    const item = { _id: randomUUID(), userId: String(req.user._id), sender: "user", senderName: String(req.user.name || "Cliente"), text: value, createdAt: new Date() };
    await db.collection("supportMessages").insertOne(item);
    res.status(201).json({ id: item._id });
  } catch (error) { sendError(res, error, "message create failed"); }
});
app.get("/api/movil/profile", requireAuth, async (req, res) => {
  try {
    const item = await db.collection("profiles").findOne({ userId: String(req.user._id) });
    res.json({ item: item ? { ...item, id: String(item._id) } : { userId: String(req.user._id), displayName: req.user.name || "", email: req.user.email || "" } });
  } catch (error) { sendError(res, error, "profile read failed"); }
});
app.put("/api/movil/profile", requireAuth, async (req, res) => {
  try {
    const data = cleanObject(req.body);
    await db.collection("profiles").updateOne(
      { userId: String(req.user._id) },
      { $set: { ...data, userId: String(req.user._id), updatedAt: new Date() }, $setOnInsert: { createdAt: new Date() } },
      { upsert: true }
    );
    res.json({ success: true });
  } catch (error) { sendError(res, error, "profile update failed"); }
});


const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 100 * 1024 * 1024 },
  fileFilter(_req, file, callback) {
    const allowed = /^(image\/(jpeg|png|webp|gif)|video\/(mp4|webm|quicktime))$/i;
    callback(allowed.test(file.mimetype) ? null : new Error("UNSUPPORTED_MEDIA_TYPE"), allowed.test(file.mimetype));
  }
});
const gcs = new Storage();
app.post("/api/movil/media", requireAuth, requireAdmin, upload.single("file"), async (req, res) => {
  try {
    if (!req.file) return res.status(400).json({ error: "FILE_REQUIRED" });
    const bucketName = process.env.GCS_BUCKET_NAME;
    if (!bucketName) return res.status(503).json({ error: "MEDIA_STORAGE_NOT_CONFIGURED" });
    const safeName = String(req.body?.name || req.file.originalname || "media").replace(/[^a-zA-Z0-9._-]/g, "_");
    const objectName = `movil/${Date.now()}-${randomUUID()}-${safeName}`;
    const file = gcs.bucket(bucketName).file(objectName);
    await file.save(req.file.buffer, {
      resumable: false,
      metadata: { contentType: req.file.mimetype, cacheControl: "public, max-age=31536000" }
    });
    const [url] = await file.getSignedUrl({ action: "read", expires: Date.now() + 7 * 24 * 60 * 60 * 1000 });
    res.status(201).json({ url, objectName, contentType: req.file.mimetype, size: req.file.size });
  } catch (error) { sendError(res, error, "media upload failed"); }
});

app.use((error, _req, res, _next) => {
  console.error("request failed", error);
  res.status(500).json({ error: "INTERNAL_ERROR" });
});

const port = Number(process.env.PORT || 8080);
app.listen(port, "0.0.0.0", () => {
  console.log(`Elegan mobile API listening on port ${port}`);
});

async function shutdown() {
  await mongo.close();
  process.exit(0);
}
process.on("SIGTERM", shutdown);
process.on("SIGINT", shutdown);
