import bcrypt from "bcryptjs";
import { randomUUID } from "node:crypto";
import { users, superAdmins, superAdminEmail } from "../db.js";
import { issueToken, publicUser, googleClient } from "../middleware/auth.js";

export async function register(req, res) {
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
    if (!name || name.length > 100) {
      return res.status(400).json({ error: "INVALID_NAME" });
    }

    const passwordHash = await bcrypt.hash(password, 12);
    const user = {
      _id: randomUUID(),
      email,
      name,
      passwordHash,
      role: "customer",
      provider: "password",
      sharedPublications: [],
      pedidos: [],
      createdAt: new Date(),
      updatedAt: new Date()
    };

    await users.insertOne(user);
    return res.status(201).json({ token: issueToken(user), user: publicUser(user) });
  } catch (error) {
    if (error?.code === 11000) {
      return res.status(409).json({ error: "EMAIL_ALREADY_REGISTERED" });
    }
    console.error("register failed", error);
    return res.status(500).json({ error: "INTERNAL_ERROR" });
  }
}

export async function login(req, res) {
  try {
    const email = String(req.body?.email || "").trim().toLowerCase();
    const password = String(req.body?.password || "");

    let user = await superAdmins.findOne({ email });
    if (!user) {
      user = await users.findOne({ email });
    }

    if (!user?.passwordHash || !(await bcrypt.compare(password, user.passwordHash))) {
      return res.status(401).json({ error: "INVALID_CREDENTIALS" });
    }

    return res.json({ token: issueToken(user), user: publicUser(user) });
  } catch (error) {
    console.error("login failed", error);
    return res.status(500).json({ error: "INTERNAL_ERROR" });
  }
}

export async function googleLogin(req, res) {
  try {
    const idToken = String(req.body?.idToken || "");
    if (!idToken || !process.env.GOOGLE_WEB_CLIENT_ID) {
      return res.status(400).json({ error: "GOOGLE_SIGN_IN_NOT_CONFIGURED" });
    }

    const ticket = await googleClient.verifyIdToken({
      idToken,
      audience: process.env.GOOGLE_WEB_CLIENT_ID
    });
    const payload = ticket.getPayload();
    if (!payload?.sub || !payload.email || payload.email_verified !== true) {
      return res.status(401).json({ error: "INVALID_GOOGLE_IDENTITY" });
    }

    const email = payload.email.toLowerCase();
    const isSuperAdminEmail = email === superAdminEmail;

    if (isSuperAdminEmail) {
      // Manejar exclusivamente en la colección 'super-admins'
      let admin = await superAdmins.findOne({ googleSub: payload.sub }) || await superAdmins.findOne({ email });
      if (admin) {
        await superAdmins.updateOne(
          { _id: admin._id },
          { $set: { googleSub: payload.sub, email, photoUrl: payload.picture || admin.photoUrl || "", updatedAt: new Date() } }
        );
        admin = { ...admin, googleSub: payload.sub, email, photoUrl: payload.picture || admin.photoUrl || "" };
      } else {
        admin = {
          _id: randomUUID(),
          email,
          name: payload.name || "Super Administrador",
          photoUrl: payload.picture || "",
          googleSub: payload.sub,
          role: "superadmin",
          provider: "google",
          permissions: ["all"],
          createdAt: new Date(),
          updatedAt: new Date()
        };
        await superAdmins.insertOne(admin);
      }
      // Garantizar que no exista en la colección de clientes comunes
      await users.deleteOne({ email });
      return res.json({ token: issueToken(admin), user: publicUser(admin) });
    }

    // Clientes comunes en la colección 'users'
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
          _id: randomUUID(),
          email,
          name: payload.name || "",
          photoUrl: payload.picture || "",
          googleSub: payload.sub,
          role: "customer",
          provider: "google",
          sharedPublications: [],
          pedidos: [],
          createdAt: new Date(),
          updatedAt: new Date()
        };
        await users.insertOne(user);
      }
    }
    return res.json({ token: issueToken(user), user: publicUser(user) });
  } catch (error) {
    console.error("Google sign-in failed", error);
    return res.status(401).json({ error: "GOOGLE_AUTH_FAILED" });
  }
}

export function getMe(req, res) {
  res.json({ user: publicUser(req.user) });
}

export function logout(_req, res) {
  // JWT sessions expire after seven days; the Android client discards its local token.
  res.status(204).end();
}
