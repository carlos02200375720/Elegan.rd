/**
 * Modelo y Esquema de la Colección 'users' para MongoDB
 * Ruta: backend/src/modelo/user.js
 * Base de datos: elegan-movil-api
 * Colección: users (Exclusiva para clientes comunes)
 */

import { randomUUID } from "node:crypto";

/**
 * Nombre oficial de la colección en la base de datos MongoDB
 */
export const USERS_COLLECTION_NAME = "users";

/**
 * Esquema de validación JSON Schema para la colección MongoDB ($jsonSchema)
 * Restringe la colección a clientes comunes ('customer'). Los administradores residen en 'super-admins'.
 */
export const UserCollectionSchema = {
  $jsonSchema: {
    bsonType: "object",
    required: ["_id", "email", "role", "provider", "createdAt", "updatedAt"],
    properties: {
      _id: {
        bsonType: "string",
        description: "Identificador único UUID v4 del cliente (clave primaria _id)"
      },
      email: {
        bsonType: "string",
        pattern: "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$",
        description: "Correo electrónico único del cliente, normalizado en minúsculas"
      },
      name: {
        bsonType: "string",
        maxLength: 120,
        description: "Nombre completo del cliente"
      },
      passwordHash: {
        bsonType: ["string", "null"],
        description: "Hash bcrypt de la contraseña (null si utiliza Google OAuth)"
      },
      photoUrl: {
        bsonType: "string",
        description: "URL pública de la foto de perfil o avatar del cliente"
      },
      googleSub: {
        bsonType: ["string", "null"],
        description: "Identificador único de sujeto de Google (sub)"
      },
      role: {
        enum: ["customer"],
        description: "Rol fijo del usuario: cliente común ('customer')"
      },
      provider: {
        enum: ["password", "google"],
        description: "Proveedor de autenticación: 'password' (correo y contraseña) o 'google'"
      },
      phone: {
        bsonType: "string",
        description: "Número telefónico del cliente para entregas y notificaciones"
      },
      address: {
        bsonType: "string",
        description: "Dirección de residencia o entrega de pedidos"
      },
      city: {
        bsonType: "string",
        description: "Ciudad de residencia o entrega"
      },
      isActive: {
        bsonType: "bool",
        description: "Estado de la cuenta del cliente (activa o inactiva)"
      },
      createdAt: {
        bsonType: "date",
        description: "Fecha y hora exacta en la que se registró el cliente"
      },
      updatedAt: {
        bsonType: "date",
        description: "Fecha y hora de la última actualización de los datos del cliente"
      }
    }
  }
};

/**
 * Definición de índices requeridos para la colección 'users'
 */
export const UserIndexes = [
  { key: { email: 1 }, options: { unique: true, sparse: true, name: "idx_users_email_unique" } },
  { key: { googleSub: 1 }, options: { unique: true, sparse: true, name: "idx_users_google_sub_unique" } },
  { key: { role: 1 }, options: { name: "idx_users_role" } },
  { key: { createdAt: -1 }, options: { name: "idx_users_created_at" } }
];

/**
 * Documento de ejemplo representativo para un cliente común
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
  createdAt: new Date("2026-01-01T00:00:00.000Z"),
  updatedAt: new Date("2026-01-01T00:00:00.000Z")
};

/**
 * Crea un documento de cliente común con estructura y validaciones predeterminadas
 * @param {Object} params - Datos para crear el documento de cliente
 * @returns {Object} Documento listo para insertar en MongoDB (collection("users"))
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
    createdAt: now,
    updatedAt: now
  };
}

/**
 * Convierte un documento de cliente a su proyección pública segura
 * @param {Object} user - Documento de la colección 'users'
 * @returns {Object|null}
 */
export function toPublicUser(user) {
  if (!user) return null;
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
    createdAt: user.createdAt
  };
}

/**
 * Inicializa la colección 'users' e índices en la base de datos
 * @param {import("mongodb").Db} db - Instancia de la base de datos MongoDB
 * @returns {Promise<import("mongodb").Collection>}
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
