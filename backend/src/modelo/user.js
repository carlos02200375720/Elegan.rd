/**
 * Modelo y Esquema de la Colección 'users' para MongoDB
 * Ruta: backend/src/modelo/user.js
 * Base de datos: elegan-movil-api
 * Colección: users (Exclusiva para clientes comunes)
 */

import { randomUUID } from "node:crypto";

/**
 * Nombre oficial de la colección en MongoDB
 */
export const USERS_COLLECTION_NAME = "users";

/**
 * Esquema de validación JSON Schema para la colección 'users'
 * Sin campos repetidos, limpio y fácil de mantener.
 */
export const UserCollectionSchema = {
  $jsonSchema: {
    bsonType: "object",
    required: ["_id", "email", "role", "provider", "createdAt", "updatedAt"],
    properties: {
      _id: {
        bsonType: "string",
        description: "Identificador único UUID del cliente"
      },
      email: {
        bsonType: "string",
        pattern: "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$",
        description: "Correo electrónico único normalizado en minúsculas"
      },
      name: {
        bsonType: "string",
        maxLength: 120,
        description: "Nombre completo del cliente"
      },
      passwordHash: {
        bsonType: ["string", "null"],
        description: "Hash bcrypt de la contraseña (null si usa Google OAuth)"
      },
      photoUrl: {
        bsonType: "string",
        description: "URL pública de la foto de perfil o avatar"
      },
      googleSub: {
        bsonType: ["string", "null"],
        description: "ID de sujeto de Google OAuth"
      },
      role: {
        enum: ["customer"],
        description: "Rol fijo: cliente común ('customer')"
      },
      provider: {
        enum: ["password", "google"],
        description: "Proveedor de autenticación"
      },
      phone: {
        bsonType: "string",
        description: "Teléfono de contacto para entregas"
      },
      address: {
        bsonType: "string",
        description: "Dirección de entrega"
      },
      city: {
        bsonType: "string",
        description: "Ciudad o localidad"
      },
      isActive: {
        bsonType: "bool",
        description: "Estado de la cuenta (activa o inactiva)"
      },
      sharedPublications: {
        bsonType: "array",
        items: { bsonType: "string" },
        description: "IDs de publicaciones compartidas por el cliente"
      },
      pedidos: {
        bsonType: "array",
        description: "Lista de pedidos realizados por el cliente"
      },
      createdAt: {
        bsonType: "date",
        description: "Fecha y hora de registro"
      },
      updatedAt: {
        bsonType: "date",
        description: "Fecha y hora de última actualización"
      }
    }
  }
};

/**
 * Índices para optimizar consultas en la colección 'users'
 */
export const UserIndexes = [
  { key: { email: 1 }, options: { unique: true, sparse: true, name: "idx_users_email_unique" } },
  { key: { googleSub: 1 }, options: { unique: true, sparse: true, name: "idx_users_google_sub_unique" } },
  { key: { role: 1 }, options: { name: "idx_users_role" } },
  { key: { createdAt: -1 }, options: { name: "idx_users_created_at" } }
];

/**
 * Documento de ejemplo representativo
 */
export const sampleUserDocument = {
  _id: "550e8400-e29b-41d4-a716-446655440000",
  email: "cliente@elegan.com",
  name: "Carlos García",
  passwordHash: "$2a$12$e8Y5M9Qz0K7V9j4N0qHqU.abcdefghijklmnopqrstuvwxyz12345",
  photoUrl: "",
  googleSub: null,
  role: "customer",
  provider: "password",
  phone: "+1 809-555-0199",
  address: "Calle Principal #123, Ensanche Elegancia",
  city: "Santo Domingo",
  isActive: true,
  sharedPublications: [],
  pedidos: [],
  createdAt: new Date("2026-01-01T00:00:00.000Z"),
  updatedAt: new Date("2026-01-01T00:00:00.000Z")
};

/**
 * Crea un documento de cliente nuevo con estructura limpia
 */
export function createUserDocument({
  id = randomUUID(),
  email,
  name = "",
  passwordHash = null,
  photoUrl = "",
  googleSub = null,
  role = "customer",
  provider = "password",
  phone = "",
  address = "",
  city = "",
  isActive = true
}) {
  const normalizedEmail = String(email || "").trim().toLowerCase();
  if (!normalizedEmail || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail)) {
    throw new Error("El correo electrónico del cliente no tiene un formato válido");
  }

  const now = new Date();
  return {
    _id: String(id),
    email: normalizedEmail,
    name: String(name || "").trim(),
    passwordHash: passwordHash ? String(passwordHash) : null,
    photoUrl: String(photoUrl || "").trim(),
    googleSub: googleSub ? String(googleSub) : null,
    role: "customer",
    provider: provider === "google" ? "google" : "password",
    phone: String(phone || "").trim(),
    address: String(address || "").trim(),
    city: String(city || "").trim(),
    isActive: Boolean(isActive),
    sharedPublications: [],
    pedidos: [],
    createdAt: now,
    updatedAt: now
  };
}

/**
 * Proyección pública segura del cliente
 */
export function toPublicUser(user) {
  if (!user) return null;
  const orderItems = Array.isArray(user.pedidos) ? user.pedidos : (Array.isArray(user.orders) ? user.orders : []);

  return {
    id: user._id,
    email: user.email || null,
    name: user.name || "",
    photoUrl: user.photoUrl || "",
    role: "customer",
    provider: user.provider || "password",
    phone: user.phone || "",
    address: user.address || "",
    city: user.city || "",
    isActive: user.isActive !== false,
    sharedPublications: Array.isArray(user.sharedPublications) ? user.sharedPublications : [],
    pedidos: orderItems,
    orders: orderItems,
    createdAt: user.createdAt
  };
}

/**
 * Inicializa la colección 'users' e índices en la base de datos
 */
export async function initializeUserCollection(db) {
  const collection = db.collection(USERS_COLLECTION_NAME);
  for (const index of UserIndexes) {
    await collection.createIndex(index.key, index.options);
  }
  return collection;
}

export default {
  USERS_COLLECTION_NAME,
  UserCollectionSchema,
  UserIndexes,
  sampleUserDocument,
  createUserDocument,
  toPublicUser,
  initializeUserCollection
};
