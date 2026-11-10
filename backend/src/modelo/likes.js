/**
 * Modelo y Esquema de la Colección Exclusiva 'likes' para MongoDB
 * Ruta: backend/src/modelo/likes.js
 * Base de datos: elegan-movil-api
 * Colección: likes (Exclusiva para almacenar todos los 'me gusta' de publicaciones y productos)
 */

import { randomUUID } from "node:crypto";

/**
 * Nombre oficial de la colección exclusiva de likes en MongoDB
 */
export const LIKES_COLLECTION_NAME = "likes";

/**
 * Esquema JSON Schema para la colección 'likes'
 */
export const LikeCollectionSchema = {
  $jsonSchema: {
    bsonType: "object",
    required: ["_id", "userId", "productId", "createdAt"],
    properties: {
      _id: {
        bsonType: "string",
        description: "Identificador único UUID v4 del like"
      },
      userId: {
        bsonType: "string",
        description: "ID del cliente que dio Me Gusta"
      },
      productId: {
        bsonType: "string",
        description: "ID del producto o publicación valorada"
      },
      createdAt: {
        bsonType: "date",
        description: "Fecha y hora exacta en la que se registró el like"
      }
    }
  }
};

/**
 * Índices para optimizar la colección exclusiva de 'likes'
 */
export const LikeIndexes = [
  { key: { userId: 1, productId: 1 }, options: { unique: true, name: "idx_likes_user_product_unique" } },
  { key: { productId: 1 }, options: { name: "idx_likes_product_id" } },
  { key: { userId: 1 }, options: { name: "idx_likes_user_id" } },
  { key: { createdAt: -1 }, options: { name: "idx_likes_created_at" } }
];

/**
 * Crea un documento para la colección 'likes'
 * @param {string} userId - ID del usuario
 * @param {string} productId - ID del producto
 * @returns {Object} Documento listo para insertar
 */
export function createLikeDocument(userId, productId) {
  return {
    _id: randomUUID(),
    userId: String(userId),
    productId: String(productId),
    createdAt: new Date()
  };
}

/**
 * Inicializa la colección 'likes' e índices en MongoDB
 * @param {import("mongodb").Db} db
 */
export async function initializeLikeCollection(db) {
  const collection = db.collection(LIKES_COLLECTION_NAME);
  for (const index of LikeIndexes) {
    await collection.createIndex(index.key, index.options || {});
  }
  return collection;
}

export default {
  LIKES_COLLECTION_NAME,
  LikeCollectionSchema,
  LikeIndexes,
  createLikeDocument,
  initializeLikeCollection
};
