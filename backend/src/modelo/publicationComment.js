/**
 * Modelo y Esquema de la Colección 'publicationComments' para MongoDB
 * Ruta: backend/src/modelo/publicationComment.js
 * Base de datos: elegan-movil-api
 * Colección: publicationComments
 */

import { randomUUID } from "node:crypto";

export const PUBLICATION_COMMENTS_COLLECTION_NAME = "publicationComments";

export const PublicationCommentCollectionSchema = {
  $jsonSchema: {
    bsonType: "object",
    required: ["_id", "productId", "userId", "displayName", "text", "createdAt"],
    properties: {
      _id: {
        bsonType: "string",
        description: "Identificador único UUID v4 del comentario"
      },
      productId: {
        bsonType: "string",
        description: "ID de la publicación o producto comentado"
      },
      userId: {
        bsonType: "string",
        description: "ID del autor del comentario"
      },
      displayName: {
        bsonType: "string",
        description: "Nombre público a mostrar del cliente que comenta"
      },
      text: {
        bsonType: "string",
        maxLength: 2000,
        description: "Contenido del comentario"
      },
      createdAt: {
        bsonType: "date",
        description: "Fecha y hora de publicación"
      }
    }
  }
};

export const PublicationCommentIndexes = [
  { key: { productId: 1, createdAt: 1 } },
  { key: { userId: 1 } }
];

export function createPublicationCommentDocument(data = {}) {
  return {
    _id: String(data.id || data._id || randomUUID()),
    productId: String(data.productId),
    userId: String(data.userId),
    displayName: String(data.displayName || "Cliente").trim(),
    text: String(data.text || "").trim(),
    createdAt: data.createdAt ? new Date(data.createdAt) : new Date()
  };
}

export function formatPublicationCommentResponse(doc) {
  if (!doc) return null;
  const { _id, ...rest } = doc;
  return {
    ...rest,
    id: String(_id)
  };
}

export async function initializePublicationCommentCollection(db) {
  const collection = db.collection(PUBLICATION_COMMENTS_COLLECTION_NAME);
  for (const index of PublicationCommentIndexes) {
    await collection.createIndex(index.key, index.options || {});
  }
  return collection;
}

export default {
  PUBLICATION_COMMENTS_COLLECTION_NAME,
  PublicationCommentCollectionSchema,
  PublicationCommentIndexes,
  createPublicationCommentDocument,
  formatPublicationCommentResponse,
  initializePublicationCommentCollection
};
