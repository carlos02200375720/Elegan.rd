/**
 * Modelo y Esquema de la Colección 'orders' para MongoDB
 * Ruta: backend/src/modelo/order.js
 * Base de datos: elegan-movil-api
 * Colección: orders (Pedidos y órdenes de compra confirmadas)
 */

import { randomUUID } from "node:crypto";

export const ORDERS_COLLECTION_NAME = "orders";

export const OrderCollectionSchema = {
  $jsonSchema: {
    bsonType: "object",
    required: ["_id", "userId", "customerName", "total", "status", "createdAt"],
    properties: {
      _id: {
        bsonType: "string",
        description: "Identificador único UUID v4 de la orden"
      },
      userId: {
        bsonType: "string",
        description: "ID del usuario que realizó la compra"
      },
      customerName: {
        bsonType: "string",
        description: "Nombre del destinatario"
      },
      customerEmail: {
        bsonType: "string",
        description: "Correo de contacto del pedido"
      },
      customerPhone: {
        bsonType: "string",
        description: "Teléfono de entrega"
      },
      shippingAddress: {
        bsonType: "string",
        description: "Dirección física de envío"
      },
      city: {
        bsonType: "string",
        description: "Ciudad o provincia de entrega"
      },
      notes: {
        bsonType: "string",
        description: "Instrucciones de entrega adicionales"
      },
      paymentMethod: {
        bsonType: "string",
        description: "Método de pago seleccionado (tarjeta, contra_entrega, etc.)"
      },
      total: {
        bsonType: "number",
        minimum: 0,
        description: "Importe total del pedido en USD"
      },
      itemCount: {
        bsonType: "int",
        minimum: 1,
        description: "Cantidad total de prendas"
      },
      itemsSummary: {
        bsonType: "string",
        description: "Resumen legible de los artículos incluidos"
      },
      status: {
        enum: ["confirmado", "en_proceso", "enviado", "entregado", "cancelado"],
        description: "Estado logístico de la orden"
      },
      createdAt: {
        bsonType: "date",
        description: "Fecha y hora de creación de la orden"
      }
    }
  }
};

export const OrderIndexes = [
  { key: { userId: 1, createdAt: -1 } },
  { key: { status: 1 } },
  { key: { createdAt: -1 } }
];

export function createOrderDocument(data = {}) {
  return {
    _id: String(data.id || data._id || randomUUID()),
    userId: String(data.userId),
    customerName: String(data.customerName || "").trim(),
    customerEmail: String(data.customerEmail || "").trim(),
    customerPhone: String(data.customerPhone || "").trim(),
    shippingAddress: String(data.shippingAddress || "").trim(),
    city: String(data.city || "").trim(),
    notes: String(data.notes || "").trim(),
    paymentMethod: String(data.paymentMethod || "tarjeta").trim(),
    total: Number(data.total) || 0,
    itemCount: Number(data.itemCount) || 1,
    itemsSummary: String(data.itemsSummary || "").trim(),
    status: data.status || "confirmado",
    createdAt: data.createdAt ? new Date(data.createdAt) : new Date()
  };
}

export function formatOrderResponse(doc) {
  if (!doc) return null;
  const { _id, ...rest } = doc;
  return {
    ...rest,
    id: String(_id),
    email: rest.email || rest.customerEmail || "",
    phone: rest.phone || rest.customerPhone || "",
    address: rest.address || rest.shippingAddress || "",
    createdAt: rest.createdAt instanceof Date
      ? rest.createdAt.toISOString()
      : (rest.createdAt || null)
  };
}

export async function initializeOrderCollection(db) {
  const collection = db.collection(ORDERS_COLLECTION_NAME);
  for (const index of OrderIndexes) {
    await collection.createIndex(index.key, index.options || {});
  }
  return collection;
}

export default {
  ORDERS_COLLECTION_NAME,
  OrderCollectionSchema,
  OrderIndexes,
  createOrderDocument,
  formatOrderResponse,
  initializeOrderCollection
};
