/**
 * Modelo y Esquema de la Colección 'products' para MongoDB
 * Ruta: backend/src/modelo/product.js
 * Base de datos: elegan-movil-api
 * Colección: products
 */

import { randomUUID } from "node:crypto";

export const PRODUCTS_COLLECTION_NAME = "products";

export const ProductCollectionSchema = {
  $jsonSchema: {
    bsonType: "object",
    required: ["_id", "name", "price", "createdAt", "updatedAt"],
    properties: {
      _id: {
        bsonType: "string",
        description: "Identificador único UUID v4 del producto"
      },
      name: {
        bsonType: "string",
        description: "Nombre comercial del producto de moda"
      },
      description: {
        bsonType: "string",
        description: "Descripción detallada de la prenda, confección y materiales"
      },
      category: {
        bsonType: "string",
        description: "Categoría de la tienda (gorra, gafa, camisa, correa, pantalone, zapato, interiore, perfume, etc.)"
      },
      price: {
        bsonType: "number",
        minimum: 0,
        description: "Precio de venta en USD"
      },
      imageUrl: {
        bsonType: "string",
        description: "URL de la imagen principal"
      },
      images: {
        bsonType: "array",
        items: { bsonType: "string" },
        description: "Galería de URLs de imágenes del producto"
      },
      videoUrl: {
        bsonType: "string",
        description: "URL del video del producto si aplica"
      },
      videoPoster: {
        bsonType: "string",
        description: "URL de la imagen miniatura del video"
      },
      hasVideo: {
        bsonType: "bool",
        description: "Indica si la publicación incluye video demostrativo"
      },
      mediaFormat: {
        enum: ["1:1", "3:4", "4:3"],
        description: "Formato visual de la publicación"
      },
      sizes: {
        bsonType: "array",
        items: { bsonType: "string" },
        description: "Tallas disponibles (S, M, L, XL, etc.)"
      },
      colors: {
        bsonType: "array",
        items: { bsonType: "string" },
        description: "Colores o tonalidades disponibles"
      },
      stock: {
        bsonType: "int",
        minimum: 0,
        description: "Cantidad de unidades en inventario"
      },
      isFeatured: {
        bsonType: "bool",
        description: "Indica si el producto destaca en la pantalla de inicio"
      },
      reviewCount: {
        bsonType: "int",
        description: "Total de valoraciones recibidas"
      },
      rating: {
        bsonType: "number",
        description: "Calificación promedio (0.0 a 5.0)"
      },
      createdAt: {
        bsonType: "date",
        description: "Fecha y hora de creación del registro"
      },
      updatedAt: {
        bsonType: "date",
        description: "Fecha y hora de la última modificación"
      }
    }
  }
};

export const ProductIndexes = [
  { key: { category: 1 } },
  { key: { isFeatured: 1 } },
  { key: { createdAt: -1 } }
];

export function createProductDocument(data = {}) {
  const now = new Date();
  return {
    _id: String(data.id || data._id || randomUUID()),
    name: String(data.name || "").trim(),
    description: String(data.description || "").trim(),
    category: String(data.category || "todo").toLowerCase().trim(),
    price: Number(data.price) || 0,
    imageUrl: String(data.imageUrl || "").trim(),
    images: Array.isArray(data.images) ? data.images : (data.imageUrl ? [data.imageUrl] : []),
    videoUrl: String(data.videoUrl || "").trim(),
    videoPoster: String(data.videoPoster || "").trim(),
    hasVideo: Boolean(data.hasVideo || data.videoUrl),
    mediaFormat: ["1:1", "3:4", "4:3"].includes(data.mediaFormat) ? data.mediaFormat : "1:1",
    sizes: Array.isArray(data.sizes) ? data.sizes : [],
    colors: Array.isArray(data.colors) ? data.colors : [],
    stock: Number.isInteger(data.stock) ? data.stock : 10,
    isFeatured: data.isFeatured !== false,
    reviewCount: Number(data.reviewCount) || 0,
    rating: Number(data.rating) || 5.0,
    createdAt: data.createdAt ? new Date(data.createdAt) : now,
    updatedAt: now
  };
}

export function formatProductResponse(doc) {
  if (!doc) return null;
  const { _id, ...rest } = doc;
  return {
    ...rest,
    id: String(_id)
  };
}

export async function initializeProductCollection(db) {
  const collection = db.collection(PRODUCTS_COLLECTION_NAME);
  for (const index of ProductIndexes) {
    await collection.createIndex(index.key, index.options || {});
  }
  return collection;
}

export default {
  PRODUCTS_COLLECTION_NAME,
  ProductCollectionSchema,
  ProductIndexes,
  createProductDocument,
  formatProductResponse,
  initializeProductCollection
};
