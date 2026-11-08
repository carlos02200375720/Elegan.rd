/**
 * Modelo y Esquema de la Colección 'announcements' para MongoDB
 * Ruta: backend/src/modelo/announcement.js
 * Base de datos: elegan-movil-api
 * Colección: announcements
 */

import { randomUUID } from "node:crypto";

export const ANNOUNCEMENTS_COLLECTION_NAME = "announcements";

export const AnnouncementCollectionSchema = {
  $jsonSchema: {
    bsonType: "object",
    required: ["_id", "title", "createdAt", "updatedAt"],
    properties: {
      _id: {
        bsonType: "string",
        description: "Identificador único UUID v4 del anuncio"
      },
      title: {
        bsonType: "string",
        description: "Título promocional del anuncio"
      },
      description: {
        bsonType: "string",
        description: "Detalle o mensaje del anuncio"
      },
      imageUrl: {
        bsonType: "string",
        description: "Imagen del banner o promoción"
      },
      tag: {
        bsonType: "string",
        description: "Etiqueta temática (ej. OFERTA, NUEVO, EXCLUSIVO)"
      },
      discount: {
        bsonType: ["string", "number"],
        description: "Porcentaje o texto del descuento aplicado"
      },
      isActive: {
        bsonType: "bool",
        description: "Indica si el anuncio se muestra a los usuarios"
      },
      createdAt: {
        bsonType: "date",
        description: "Fecha de creación"
      },
      updatedAt: {
        bsonType: "date",
        description: "Fecha de última modificación"
      }
    }
  }
};

export const AnnouncementIndexes = [
  { key: { isActive: 1 } },
  { key: { createdAt: -1 } }
];

export function createAnnouncementDocument(data = {}) {
  const now = new Date();
  return {
    _id: String(data.id || data._id || randomUUID()),
    title: String(data.title || "").trim(),
    description: String(data.description || "").trim(),
    imageUrl: String(data.imageUrl || "").trim(),
    tag: String(data.tag || "").trim(),
    discount: data.discount !== undefined ? data.discount : "",
    isActive: data.isActive !== false,
    createdAt: data.createdAt ? new Date(data.createdAt) : now,
    updatedAt: now
  };
}

export function formatAnnouncementResponse(doc) {
  if (!doc) return null;
  const { _id, ...rest } = doc;
  return {
    ...rest,
    id: String(_id)
  };
}

export async function initializeAnnouncementCollection(db) {
  const collection = db.collection(ANNOUNCEMENTS_COLLECTION_NAME);
  for (const index of AnnouncementIndexes) {
    await collection.createIndex(index.key, index.options || {});
  }
  return collection;
}

export default {
  ANNOUNCEMENTS_COLLECTION_NAME,
  AnnouncementCollectionSchema,
  AnnouncementIndexes,
  createAnnouncementDocument,
  formatAnnouncementResponse,
  initializeAnnouncementCollection
};
