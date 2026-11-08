import jwt from "jsonwebtoken";
import rateLimit from "express-rate-limit";
import { OAuth2Client } from "google-auth-library";
import { users, superAdmins } from "../db.js";

export const googleClient = new OAuth2Client(process.env.GOOGLE_WEB_CLIENT_ID);

export const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  limit: 30,
  standardHeaders: "draft-7",
  legacyHeaders: false
});

export function publicUser(user) {
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

export function issueToken(user) {
  return jwt.sign(
    { sub: String(user._id), role: user.role || "customer" },
    process.env.JWT_SECRET,
    { expiresIn: "7d", issuer: "elegan-movil-api", audience: "elegan-android" }
  );
}

export async function requireAuth(req, res, next) {
  try {
    const header = req.headers.authorization || "";
    const token = header.startsWith("Bearer ") ? header.slice(7) : "";
    if (!token) return res.status(401).json({ error: "AUTH_REQUIRED" });

    const payload = jwt.verify(token, process.env.JWT_SECRET, {
      issuer: "elegan-movil-api",
      audience: "elegan-android"
    });

    let user = await users.findOne({ _id: payload.sub });
    if (!user) user = await superAdmins.findOne({ _id: payload.sub });
    if (!user) return res.status(401).json({ error: "USER_NOT_FOUND" });

    req.user = user;
    next();
  } catch {
    return res.status(401).json({ error: "INVALID_SESSION" });
  }
}

export async function optionalAuth(req, _res, next) {
  try {
    const header = req.headers.authorization || "";
    const token = header.startsWith("Bearer ") ? header.slice(7) : "";
    if (token && token !== "guest" && token !== "null" && token !== "undefined") {
      const payload = jwt.verify(token, process.env.JWT_SECRET, {
        issuer: "elegan-movil-api",
        audience: "elegan-android"
      });
      let user = await users.findOne({ _id: payload.sub });
      if (!user) user = await superAdmins.findOne({ _id: payload.sub });
      if (user) req.user = user;
    }
  } catch {
    // Si no hay token o falló la verificación, continúa como cliente invitado sin error
  }
  next();
}

export function requireAdmin(req, res, next) {
  if (req.user?.role !== "admin" && req.user?.role !== "superadmin") {
    return res.status(403).json({ error: "ADMIN_REQUIRED" });
  }
  next();
}

export function owned(userId, id) {
  return { userId, _id: id };
}

export function cleanObject(value) {
  if (!value || typeof value !== "object" || Array.isArray(value)) return {};
  const result = { ...value };
  delete result._id;
  delete result.userId;
  delete result.role;
  delete result.createdAt;
  delete result.updatedAt;
  return result;
}

export function sendError(res, error, label) {
  console.error(label, error);
  return res.status(500).json({ error: "INTERNAL_ERROR" });
}
