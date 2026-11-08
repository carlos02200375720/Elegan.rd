/**
 * Modelo y Esquema de la Colección 'savedProducts' para MongoDB
 * Ruta: backend/src/modelo/savedProduct.js
 * Base de datos: elegan-movil-api
 * Colección: savedProducts (Productos guardados / favoritos por usuario)
 */

export const SAVED_PRODUCTS_COLLECTION_NAME = "savedProducts";

export const SavedProductCollectionSchema = {
  $jsonSchema: {
    bsonType: "object",
    required: ["userId", "productId", "createdAt"],
    properties: {
      userId: {
        bsonType: "string",
        description: "ID del usuario que guardó el producto"
      },
      productId: {
        bsonType: "string",
        description: "ID del producto guardado"
      },
      createdAt: {
        bsonType: "date",
        description: "Fecha y hora en la que se guardó"
      }
    }
  }
};

export const SavedProductIndexes = [
  { key: { userId: 1, productId: 1 }, options: { unique: true } },
  { key: { productId: 1 } },
  { key: { userId: 1 } }
];

export function createSavedProductDocument(userId, productId) {
  return {
    userId: String(userId),
    productId: String(productId),
    createdAt: new Date()
  };
}

export async function initializeSavedProductCollection(db) {
  const collection = db.collection(SAVED_PRODUCTS_COLLECTION_NAME);
  for (const index of SavedProductIndexes) {
    await collection.createIndex(index.key, index.options || {});
  }
  return collection;
}

export default {
  SAVED_PRODUCTS_COLLECTION_NAME,
  SavedProductCollectionSchema,
  SavedProductIndexes,
  createSavedProductDocument,
  initializeSavedProductCollection
};
