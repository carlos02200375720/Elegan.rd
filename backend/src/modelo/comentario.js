/**
 * Modelo y Esquema de la Colección Exclusiva 'comentarios' para MongoDB
 * Ruta: backend/src/modelo/comentario.js
 * Base de datos: elegan-movil-api
 * Colección: comentarios (Exclusiva para almacenar todos los comentarios de publicaciones y productos)
 */

import { randomUUID } from "node:crypto";

/**
 * Nombre oficial de la colección exclusiva de comentarios en MongoDB
 */
export const COMENTARIOS_COLLECTION_NAME = "comentarios";

/**
 * Esquema JSON Schema de validación para la colección 'comentarios'
 */
export const ComentarioCollectionSchema = {
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
        description: "ID del autor del comentario (o ID de invitado)"
      },
      displayName: {
        bsonType: "string",
        maxLength: 100,
        description: "Nombre público del cliente que comenta"
      },
      text: {
        bsonType: "string",
        maxLength: 2000,
        description: "Contenido textual del comentario"
      },
      createdAt: {
        bsonType: "date",
        description: "Fecha y hora exacta en la que se publicó el comentario"
      }
    }
  }
};

/**
 * Índices para optimizar la colección exclusiva de 'comentarios'
 */
export const ComentarioIndexes = [
  { key: { productId: 1, createdAt: 1 }, options: { name: "idx_comentarios_product_date" } },
  { key: { userId: 1 }, options: { name: "idx_comentarios_user_id" } },
  { key: { createdAt: -1 }, options: { name: "idx_comentarios_created_at" } }
];

/**
 * Crea un documento para la colección 'comentarios'
 * @param {Object} data
 * @returns {Object} Documento listo para insertar
 */
export function createComentarioDocument(data = {}) {
  return {
    _id: String(data.id || data._id || randomUUID()),
    productId: String(data.productId),
    userId: String(data.userId),
    displayName: String(data.displayName || "Cliente").trim() || "Cliente",
    text: String(data.text || "").trim(),
    createdAt: data.createdAt ? new Date(data.createdAt) : new Date()
  };
}

/**
 * Formatea un comentario para la respuesta JSON de la API
 * @param {Object} doc - Documento de MongoDB
 * @returns {Object|null}
 */
export function formatComentarioResponse(doc) {
  if (!doc) return null;
  const { _id, ...rest } = doc;
  return {
    ...rest,
    id: String(_id)
  };
}

/**
 * Inicializa la colección 'comentarios' e índices en MongoDB
 * @param {import("mongodb").Db} db
 */
export async function initializeComentarioCollection(db) {
  const collection = db.collection(COMENTARIOS_COLLECTION_NAME);
  for (const index of ComentarioIndexes) {
    await collection.createIndex(index.key, index.options || {});
  }
  return collection;
}

export default {
  COMENTARIOS_COLLECTION_NAME,
  ComentarioCollectionSchema,
  ComentarioIndexes,
  createComentarioDocument,
  formatComentarioResponse,
  initializeComentarioCollection
};
