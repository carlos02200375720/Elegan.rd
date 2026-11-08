import { MongoClient } from "mongodb";

// Valores por defecto seguros para evitar que el contenedor falle si no se pasaron en Cloud Run
process.env.JWT_SECRET = process.env.JWT_SECRET || "elegan_production_jwt_secret_key_2026_sartorial_luxury_secure_auth";
process.env.GOOGLE_WEB_CLIENT_ID = process.env.GOOGLE_WEB_CLIENT_ID || "587849039182-3m8b8iiarigl5me5h70dq9hivt5qtmrd.apps.googleusercontent.com";
process.env.MONGODB_DATABASE = process.env.MONGODB_DATABASE || "elegan.rd.db";

// Si el usuario pegó el JSON completo de google-services.json en lugar de solo el client_id, extraerlo limpiamente
if (process.env.GOOGLE_WEB_CLIENT_ID && process.env.GOOGLE_WEB_CLIENT_ID.trim().startsWith("{")) {
  try {
    const parsed = JSON.parse(process.env.GOOGLE_WEB_CLIENT_ID);
    const oauthClients = parsed?.client?.[0]?.oauth_client || [];
    const webClient = oauthClients.find(c => c.client_type === 3) || oauthClients[0];
    process.env.GOOGLE_WEB_CLIENT_ID = webClient?.client_id || "587849039182-3m8b8iiarigl5me5h70dq9hivt5qtmrd.apps.googleusercontent.com";
  } catch {
    process.env.GOOGLE_WEB_CLIENT_ID = "587849039182-3m8b8iiarigl5me5h70dq9hivt5qtmrd.apps.googleusercontent.com";
  }
}

if (!process.env.MONGODB_URI) {
  throw new Error("Missing required environment variable: MONGODB_URI");
}
if (process.env.JWT_SECRET.length < 32) {
  throw new Error("JWT_SECRET must contain at least 32 characters");
}

const mongoOptions = process.env.MONGODB_USERNAME && process.env.MONGODB_PASSWORD
  ? { auth: { username: process.env.MONGODB_USERNAME, password: process.env.MONGODB_PASSWORD } }
  : {};

export const mongo = new MongoClient(process.env.MONGODB_URI, mongoOptions);

export let db = null;
export let users = null;
export let superAdmins = null;

export const superAdminEmail = (process.env.SUPER_ADMIN_EMAIL || "cg0220037@gmail.com").toLowerCase();

export async function connectDB() {
  await mongo.connect();
  db = mongo.db(process.env.MONGODB_DATABASE || undefined);
  users = db.collection("users");
  superAdmins = db.collection("super-admins");

  // Índices para usuarios y administradores
  await users.createIndex({ email: 1 }, { unique: true, sparse: true });
  await users.createIndex({ googleSub: 1 }, { unique: true, sparse: true });
  await superAdmins.createIndex({ email: 1 }, { unique: true, sparse: true });
  await superAdmins.createIndex({ googleSub: 1 }, { unique: true, sparse: true });

  // Garantizar que la cuenta del superadministrador principal resida en 'super-admins'
  const existingSuperAdminInUsers = await users.findOne({ email: superAdminEmail });
  if (existingSuperAdminInUsers) {
    const existingInSuperAdmins = await superAdmins.findOne({ email: superAdminEmail });
    if (!existingInSuperAdmins) {
      await superAdmins.insertOne({
        ...existingSuperAdminInUsers,
        role: "superadmin",
        permissions: ["all"],
        updatedAt: new Date()
      });
    }
    await users.deleteOne({ _id: existingSuperAdminInUsers._id });
  }

  console.log("✅ MongoDB Atlas conectado exitosamente.");
  return { db, users, superAdmins };
}

export async function closeDB() {
  await mongo.close();
}
