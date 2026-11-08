/**
 * Modelo y Esquema de la Colección 'super-admins' para MongoDB
 * Ruta: backend/src/modelo/superAdmin.js
 * Base de datos: elegan-movil-api
 * Colección: super-admins
 */

import { randomUUID } from "node:crypto";

/**
 * Nombre oficial de la colección en MongoDB para superadministradores
 */
export const SUPER_ADMINS_COLLECTION_NAME = "super-admins";

/**
 * Esquema de validación JSON Schema para MongoDB ($jsonSchema)
 * Restringe la colección a cuentas de superadministradores con permisos elevados.
 */
export const SuperAdminCollectionSchema = {
  $jsonSchema: {
    bsonType: "object",
    required: ["_id", "email", "role", "provider", "createdAt", "updatedAt"],
    properties: {
      _id: {
        bsonType: "string",
        description: "Identificador único UUID v4 del superadministrador (clave primaria)"
      },
      email: {
        bsonType: "string",
        pattern: "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$",
        description: "Correo electrónico único del superadministrador, normalizado en minúsculas"
      },
      name: {
        bsonType: "string",
        maxLength: 120,
        description: "Nombre completo del superadministrador"
      },
      passwordHash: {
        bsonType: ["string", "null"],
        description: "Hash bcrypt de la contraseña (null si utiliza acceso directo con Google OAuth)"
      },
      photoUrl: {
        bsonType: "string",
        description: "URL pública de la foto de perfil del superadministrador"
      },
      googleSub: {
        bsonType: ["string", "null"],
        description: "ID de sujeto único de Google OAuth (sub)"
      },
      role: {
        enum: ["superadmin", "admin"],
        description: "Nivel de acceso administrativo supremo ('superadmin')"
      },
      provider: {
        enum: ["google", "password"],
        description: "Método de autenticación del superadministrador"
      },
      permissions: {
        bsonType: "array",
        items: { bsonType: "string" },
        description: "Lista de permisos especiales asignados (ej. 'manage_products', 'manage_announcements', 'all')"
      },
      phone: {
        bsonType: "string",
        description: "Teléfono corporativo o de contacto directo"
      },
      isActive: {
        bsonType: "bool",
        description: "Indica si el acceso administrativo se encuentra activo"
      },
      createdAt: {
        bsonType: "date",
        description: "Fecha y hora de alta del superadministrador"
      },
      updatedAt: {
        bsonType: "date",
        description: "Fecha y hora de la última modificación de credenciales o perfil"
      }
    }
  }
};

/**
 * Índices requeridos para la colección 'super-admins'
 */
export const SuperAdminIndexes = [
  { key: { email: 1 }, options: { unique: true, sparse: true, name: "idx_superadmins_email_unique" } },
  { key: { googleSub: 1 }, options: { unique: true, sparse: true, name: "idx_superadmins_google_sub_unique" } },
  { key: { role: 1 }, options: { name: "idx_superadmins_role" } },
  { key: { createdAt: -1 }, options: { name: "idx_superadmins_created_at" } }
];

/**
 * Documento de ejemplo representativo de un superadministrador
 */
export const sampleSuperAdminDocument = {
  _id: "3c9b7400-e29b-41d4-a716-446655440001",
  email: "cg0220037@gmail.com",
  name: "Carlos García (Super Administrador)",
  passwordHash: null,
  photoUrl: "https://lh3.googleusercontent.com/a/default-user",
  googleSub: "109876543210987654321",
  role: "superadmin",
  provider: "google",
  permissions: ["all", "manage_products", "manage_announcements", "manage_orders", "manage_users"],
  phone: "+1 809-000-0000",
  isActive: true,
  createdAt: new Date("2026-01-01T00:00:00.000Z"),
  updatedAt: new Date("2026-01-01T00:00:00.000Z")
};

/**
 * Fábrica para crear un nuevo documento de superadministrador
 * @param {Object} params - Datos del superadministrador
 * @returns {Object} Documento listo para la colección 'super-admins'
 */
export function createSuperAdminDocument({
  id = randomUUID(),
  email,
  name = "Super Administrador",
  passwordHash = null,
  photoUrl = "",
  googleSub = null,
  role = "superadmin",
  provider = "google",
  permissions = ["all"],
  phone = "",
  isActive = true
}) {
  const normalizedEmail = String(email || "").trim().toLowerCase();
  if (!normalizedEmail || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail)) {
    throw new Error("El correo electrónico del superadministrador no es válido");
  }

  const now = new Date();
  return {
    _id: String(id),
    email: normalizedEmail,
    name: String(name || "").trim(),
    passwordHash: passwordHash ? String(passwordHash) : null,
    photoUrl: String(photoUrl || "").trim(),
    googleSub: googleSub ? String(googleSub) : null,
    role: "superadmin",
    provider: provider === "password" ? "password" : "google",
    permissions: Array.isArray(permissions) ? permissions : ["all"],
    phone: String(phone || "").trim(),
    isActive: Boolean(isActive),
    createdAt: now,
    updatedAt: now
  };
}

/**
 * Proyecta el documento de superadministrador a su formato seguro
 * @param {Object} admin - Documento de superadministrador
 * @returns {Object|null}
 */
export function toPublicSuperAdmin(admin) {
  if (!admin) return null;
  return {
    id: admin._id,
    email: admin.email || null,
    name: admin.name || "Super Administrador",
    photoUrl: admin.photoUrl || "",
    role: "superadmin",
    provider: admin.provider || "google",
    permissions: admin.permissions || ["all"],
    phone: admin.phone || "",
    isActive: admin.isActive !== false,
    createdAt: admin.createdAt
  };
}

/**
 * Inicializa la colección 'super-admins' e índices en MongoDB
 * @param {import("mongodb").Db} db - Instancia de base de datos MongoDB
 * @returns {Promise<import("mongodb").Collection>}
 */
export async function initializeSuperAdminCollection(db) {
  const collection = db.collection(SUPER_ADMINS_COLLECTION_NAME);
  for (const index of SuperAdminIndexes) {
    await collection.createIndex(index.key, index.options);
  }
  return collection;
}

export default {
  SUPER_ADMINS_COLLECTION_NAME,
  SuperAdminCollectionSchema,
  SuperAdminIndexes,
  sampleSuperAdminDocument,
  createSuperAdminDocument,
  toPublicSuperAdmin,
  initializeSuperAdminCollection
};
