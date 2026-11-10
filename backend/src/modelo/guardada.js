/**
 * Modelo y Esquema de la Colección Exclusiva 'guardadas' para MongoDB
 * Ruta: backend/src/modelo/guardada.js
 * Base de datos: elegan-movil-api
 * Colección: guardadas (Exclusiva para publicaciones y productos guardados por cada cliente)
 */

import { randomUUID } from "node:crypto";

/**
 * Nombre oficial de la colección exclusiva de publicaciones guardadas en MongoDB
 */
export const GUARDADAS_COLLECTION_NAME = "guardadas";
export const SAVED_PRODUCTS_COLLECTION_NAME = "guardadas";

/**
 * Esquema JSON Schema para la colección 'guardadas'
 */
export const GuardadaCollectionSchema = {
  $jsonSchema: {
    bsonType: "object",
    required: ["_id", "userId", "productId", "createdAt"],
    properties: {
      _id: {
        bsonType: "string",
        description: "Identificador único UUID v4 del elemento guardado"
      },
      userId: {
        bsonType: "string",
        description: "ID del usuario que guardó la publicación"
      },
      productId: {
        bsonType: "string",
        description: "ID de la publicación o producto guardado"
      },
      createdAt: {
        bsonType: "date",
        description: "Fecha y hora exacta en la que se guardó"
      }
    }
  }
};

/**
 * Índices para optimizar la colección 'guardadas'
 */
export const GuardadaIndexes = [
  { key: { userId: 1, productId: 1 }, options: { unique: true, name: "idx_guardadas_user_product_unique" } },
  { key: { productId: 1 }, options: { name: "idx_guardadas_product_id" } },
  { key: { userId: 1 }, options: { name: "idx_guardadas_user_id" } },
  { key: { createdAt: -1 }, options: { name: "idx_guardadas_created_at" } }
];

/**
 * Crea un documento para la colección 'guardadas'
 * @param {string} userId - ID del usuario
 * @param {string} productId - ID de la publicación o producto
 * @returns {Object} Documento listo para insertar
 */
export function createGuardadaDocument(userId, productId) {
  return {
    _id: randomUUID(),
    userId: String(userId),
    productId: String(productId),
    createdAt: new Date()
  };
}

/**
 * Inicializa la colección 'guardadas' e índices en MongoDB
 * @param {import("mongodb").Db} db
 */
export async function initializeGuardadaCollection(db) {
  const collection = db.collection(GUARDADAS_COLLECTION_NAME);
  for (const index of GuardadaIndexes) {
    await collection.createIndex(index.key, index.options || {});
  }
  return collection;
}

export default {
  GUARDADAS_COLLECTION_NAME,
  SAVED_PRODUCTS_COLLECTION_NAME,
  GuardadaCollectionSchema,
  GuardadaIndexes,
  createGuardadaDocument,
  initializeGuardadaCollection
};
