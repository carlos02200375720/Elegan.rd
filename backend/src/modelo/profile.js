/**
 * Modelo y Esquema de la Colección 'profiles' para MongoDB
 * Ruta: backend/src/modelo/profile.js
 * Base de datos: elegan-movil-api
 * Colección: profiles (Perfiles y preferencias extendidas de clientes)
 */

export const PROFILES_COLLECTION_NAME = "profiles";

export const ProfileCollectionSchema = {
  $jsonSchema: {
    bsonType: "object",
    required: ["userId", "updatedAt"],
    properties: {
      userId: {
        bsonType: "string",
        description: "ID del usuario propietario del perfil"
      },
      displayName: {
        bsonType: "string",
        description: "Nombre público preferido"
      },
      email: {
        bsonType: "string",
        description: "Correo de contacto del perfil"
      },
      phone: {
        bsonType: "string",
        description: "Teléfono de contacto para envíos"
      },
      address: {
        bsonType: "string",
        description: "Dirección de residencia o entrega"
      },
      city: {
        bsonType: "string",
        description: "Ciudad o provincia"
      },
      preferredSize: {
        bsonType: "string",
        description: "Talla de confección preferida (S, M, L, XL, etc.)"
      },
      preferredCategory: {
        bsonType: "string",
        description: "Categoría favorita"
      },
      createdAt: {
        bsonType: "date",
        description: "Fecha de creación del perfil"
      },
      updatedAt: {
        bsonType: "date",
        description: "Fecha de última actualización"
      }
    }
  }
};

export const ProfileIndexes = [
  { key: { userId: 1 }, options: { unique: true } }
];

export function createProfileDocument(data = {}) {
  const now = new Date();
  return {
    userId: String(data.userId),
    displayName: String(data.displayName || "").trim(),
    email: String(data.email || "").trim(),
    phone: String(data.phone || "").trim(),
    address: String(data.address || "").trim(),
    city: String(data.city || "").trim(),
    preferredSize: String(data.preferredSize || "").trim(),
    preferredCategory: String(data.preferredCategory || "").trim(),
    createdAt: data.createdAt ? new Date(data.createdAt) : now,
    updatedAt: now
  };
}

export function formatProfileResponse(doc, fallbackUser = {}) {
  if (!doc) {
    return {
      userId: String(fallbackUser._id || fallbackUser.id || ""),
      displayName: fallbackUser.name || "",
      email: fallbackUser.email || ""
    };
  }
  const { _id, ...rest } = doc;
  return {
    ...rest,
    id: _id ? String(_id) : undefined
  };
}

export async function initializeProfileCollection(db) {
  const collection = db.collection(PROFILES_COLLECTION_NAME);
  for (const index of ProfileIndexes) {
    await collection.createIndex(index.key, index.options || {});
  }
  return collection;
}

export default {
  PROFILES_COLLECTION_NAME,
  ProfileCollectionSchema,
  ProfileIndexes,
  createProfileDocument,
  formatProfileResponse,
  initializeProfileCollection
};
