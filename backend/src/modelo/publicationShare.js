/**
 * Modelo y Esquema de la Colección 'publicationShares' para MongoDB
 * Ruta: backend/src/modelo/publicationShare.js
 * Base de datos: elegan-movil-api
 * Colección: publicationShares
 */

export const PUBLICATION_SHARES_COLLECTION_NAME = "publicationShares";

export const PublicationShareCollectionSchema = {
  $jsonSchema: {
    bsonType: "object",
    required: ["userId", "productId", "createdAt"],
    properties: {
      userId: {
        bsonType: "string",
        description: "ID del usuario que compartió la publicación"
      },
      productId: {
        bsonType: "string",
        description: "ID de la publicación o producto compartido"
      },
      createdAt: {
        bsonType: "date",
        description: "Fecha y hora del evento de compartir"
      }
    }
  }
};

export const PublicationShareIndexes = [
  { key: { productId: 1 } },
  { key: { userId: 1 } }
];

export function createPublicationShareDocument(userId, productId) {
  return {
    userId: String(userId),
    productId: String(productId),
    createdAt: new Date()
  };
}

export async function initializePublicationShareCollection(db) {
  const collection = db.collection(PUBLICATION_SHARES_COLLECTION_NAME);
  for (const index of PublicationShareIndexes) {
    await collection.createIndex(index.key, index.options || {});
  }
  return collection;
}

export default {
  PUBLICATION_SHARES_COLLECTION_NAME,
  PublicationShareCollectionSchema,
  PublicationShareIndexes,
  createPublicationShareDocument,
  initializePublicationShareCollection
};
