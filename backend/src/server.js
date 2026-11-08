import express from "express";
import cors from "cors";
import helmet from "helmet";
import rateLimit from "express-rate-limit";
import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";
import { MongoClient } from "mongodb";
import { OAuth2Client } from "google-auth-library";
import { randomUUID } from "node:crypto";

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
