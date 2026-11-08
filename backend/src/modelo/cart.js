/**
 * Modelo y Esquema de la Colección 'cart' para MongoDB
 * Ruta: backend/src/modelo/cart.js
 * Base de datos: elegan-movil-api
 * Colección: cart (Elementos del carrito de compras por usuario)
 */

import { randomUUID } from "node:crypto";

export const CART_COLLECTION_NAME = "cart";

export const CartCollectionSchema = {
  $jsonSchema: {
    bsonType: "object",
    required: ["_id", "userId", "productId", "quantity", "createdAt", "updatedAt"],
    properties: {
      _id: {
        bsonType: "string",
        description: "Identificador único UUID v4 de la línea del carrito"
      },
      userId: {
        bsonType: "string",
        description: "ID del usuario propietario del carrito"
      },
      productId: {
        bsonType: "string",
        description: "ID del producto seleccionado"
      },
      name: {
        bsonType: "string",
        description: "Nombre del producto en el momento de agregarlo"
      },
      price: {
        bsonType: "number",
        minimum: 0,
        description: "Precio unitario del producto"
      },
      imageUrl: {
        bsonType: "string",
        description: "Imagen de la prenda seleccionada"
      },
      selectedSize: {
        bsonType: "string",
        description: "Talla elegida por el cliente"
      },
      selectedColor: {
        bsonType: "string",
        description: "Color elegido por el cliente"
      },
      quantity: {
        bsonType: "int",
        minimum: 1,
        maximum: 99,
        description: "Cantidad de unidades añadidas"
      },
      createdAt: {
        bsonType: "date",
        description: "Fecha en la que se añadió al carrito"
      },
      updatedAt: {
        bsonType: "date",
        description: "Fecha de última modificación"
      }
    }
  }
};

export const CartIndexes = [
  { key: { userId: 1, productId: 1, selectedSize: 1, selectedColor: 1 } },
  { key: { userId: 1, createdAt: 1 } }
];

export function createCartDocument(data = {}) {
  const now = new Date();
  return {
    _id: String(data.id || data._id || randomUUID()),
    userId: String(data.userId),
    productId: String(data.productId),
    name: String(data.name || ""),
    price: Number(data.price) || 0,
    imageUrl: String(data.imageUrl || ""),
    selectedSize: String(data.selectedSize || ""),
    selectedColor: String(data.selectedColor || ""),
    quantity: Math.max(1, Math.min(99, Number(data.quantity) || 1)),
    createdAt: data.createdAt ? new Date(data.createdAt) : now,
    updatedAt: now
  };
}

export function formatCartResponse(doc) {
  if (!doc) return null;
  const { _id, ...rest } = doc;
  return {
    ...rest,
    id: String(_id)
  };
}

export async function initializeCartCollection(db) {
  const collection = db.collection(CART_COLLECTION_NAME);
  for (const index of CartIndexes) {
    await collection.createIndex(index.key, index.options || {});
  }
  return collection;
}

export default {
  CART_COLLECTION_NAME,
  CartCollectionSchema,
  CartIndexes,
  createCartDocument,
  formatCartResponse,
  initializeCartCollection
};
