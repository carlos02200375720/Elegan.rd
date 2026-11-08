/**
 * Modelo y Esquema de la Colección 'supportMessages' para MongoDB
 * Ruta: backend/src/modelo/supportMessage.js
 * Base de datos: elegan-movil-api
 * Colección: supportMessages (Conversaciones de atención al cliente y Concierge)
 */

import { randomUUID } from "node:crypto";

export const SUPPORT_MESSAGES_COLLECTION_NAME = "supportMessages";

export const SupportMessageCollectionSchema = {
  $jsonSchema: {
    bsonType: "object",
    required: ["_id", "userId", "sender", "senderName", "text", "createdAt"],
    properties: {
      _id: {
        bsonType: "string",
        description: "Identificador único UUID v4 del mensaje"
      },
      userId: {
        bsonType: "string",
        description: "ID del cliente asociado a la conversación"
      },
      sender: {
        enum: ["user", "support", "admin"],
        description: "Tipo de remitente"
      },
      senderName: {
        bsonType: "string",
        description: "Nombre a mostrar del remitente (ej. Cliente o Elegan Concierge)"
      },
      text: {
        bsonType: "string",
        maxLength: 4000,
        description: "Texto del mensaje enviado"
      },
      createdAt: {
        bsonType: "date",
        description: "Fecha y hora de envío del mensaje"
      }
    }
  }
};

export const SupportMessageIndexes = [
  { key: { userId: 1, createdAt: 1 } }
];

export function createSupportMessageDocument(data = {}) {
  return {
    _id: String(data.id || data._id || randomUUID()),
    userId: String(data.userId),
    sender: data.sender || "user",
    senderName: String(data.senderName || "Cliente").trim(),
    text: String(data.text || "").trim(),
    createdAt: data.createdAt ? new Date(data.createdAt) : new Date()
  };
}

export function formatSupportMessageResponse(doc) {
  if (!doc) return null;
  const { _id, ...rest } = doc;
  return {
    ...rest,
    id: String(_id)
  };
}

export async function initializeSupportMessageCollection(db) {
  const collection = db.collection(SUPPORT_MESSAGES_COLLECTION_NAME);
  for (const index of SupportMessageIndexes) {
    await collection.createIndex(index.key, index.options || {});
  }
  return collection;
}

export default {
  SUPPORT_MESSAGES_COLLECTION_NAME,
  SupportMessageCollectionSchema,
  SupportMessageIndexes,
  createSupportMessageDocument,
  formatSupportMessageResponse,
  initializeSupportMessageCollection
};
