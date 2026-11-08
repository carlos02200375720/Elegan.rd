/**
 * Modelo y Esquema de la Colección 'publicationLikes' para MongoDB
 * Ruta: backend/src/modelo/publicationLike.js
 * Base de datos: elegan-movil-api
 * Colección: publicationLikes (Likes de publicaciones de productos)
 */

export const PUBLICATION_LIKES_COLLECTION_NAME = "publicationLikes";

export const PublicationLikeCollectionSchema = {
  $jsonSchema: {
    bsonType: "object",
    required: ["userId", "productId", "createdAt"],
    properties: {
      userId: {
        bsonType: "string",
        description: "ID del usuario que dio Me Gusta"
      },
      productId: {
        bsonType: "string",
        description: "ID de la publicación / producto valorado"
      },
      createdAt: {
        bsonType: "date",
        description: "Fecha y hora del Me Gusta"
      }
    }
  }
};

export const PublicationLikeIndexes = [
  { key: { userId: 1, productId: 1 }, options: { unique: true } },
  { key: { productId: 1 } },
  { key: { userId: 1 } }
];

export function createPublicationLikeDocument(userId, productId) {
  return {
    userId: String(userId),
    productId: String(productId),
    createdAt: new Date()
  };
}

export async function initializePublicationLikeCollection(db) {
  const collection = db.collection(PUBLICATION_LIKES_COLLECTION_NAME);
  for (const index of PublicationLikeIndexes) {
    await collection.createIndex(index.key, index.options || {});
  }
  return collection;
}

export default {
  PUBLICATION_LIKES_COLLECTION_NAME,
  PublicationLikeCollectionSchema,
  PublicationLikeIndexes,
  createPublicationLikeDocument,
  initializePublicationLikeCollection
};
