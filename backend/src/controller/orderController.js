import { randomUUID } from "node:crypto";
import { db } from "../db.js";
import { cleanObject, sendError } from "../middleware/auth.js";
import {
  ORDERS_COLLECTION_NAME,
  CART_COLLECTION_NAME,
  USERS_COLLECTION_NAME,
  formatOrderResponse
} from "../modelo/index.js";

/**
 * Sincroniza la lista completa de pedidos en el documento del usuario en la colección 'users'
 * Manteniendo los campos 'orders' y 'pedidos' actualizados en tiempo real.
 */
async function syncUserOrders(userId) {
  try {
    const orders = await db
      .collection(ORDERS_COLLECTION_NAME)
      .find({ userId: String(userId) })
      .sort({ createdAt: -1 })
      .toArray();

    const formatted = orders.map((o) => ({
      id: String(o._id),
      customerName: o.customerName || "",
      customerEmail: o.customerEmail || "",
      customerPhone: o.customerPhone || "",
      shippingAddress: o.shippingAddress || "",
      city: o.city || "",
      notes: o.notes || "",
      paymentMethod: o.paymentMethod || "tarjeta",
      total: Number(o.total) || 0,
      itemCount: Number(o.itemCount) || 1,
      itemsSummary: o.itemsSummary || "",
      status: o.status || "confirmado",
      createdAt: o.createdAt || new Date()
    }));

    await db.collection(USERS_COLLECTION_NAME).updateOne(
      { $or: [{ _id: userId }, { _id: String(userId) }] },
      {
        $set: {
          pedidos: formatted,
          updatedAt: new Date()
        }
      }
    );
  } catch (err) {
    console.error("syncUserOrders failed:", err);
  }
}

export async function getOrders(req, res) {
  try {
    const filter = (req.user.role === "admin" || req.user.role === "superadmin")
      ? {}
      : { userId: String(req.user._id) };
    const items = await db.collection(ORDERS_COLLECTION_NAME).find(filter).sort({ createdAt: -1 }).toArray();
    res.json({ items: items.map(formatOrderResponse) });
  } catch (error) {
    sendError(res, error, "orders list failed");
  }
}

export async function createOrder(req, res) {
  try {
    const data = cleanObject(req.body);
    if (!String(data.customerName || "").trim()) {
      return res.status(400).json({ error: "INVALID_ORDER" });
    }
    const userId = String(req.user._id);
    const cartItems = await db.collection(CART_COLLECTION_NAME).find({ userId }).toArray();
    if (!cartItems.length) return res.status(400).json({ error: "CART_EMPTY" });

    const total = cartItems.reduce((sum, item) => sum + (Number(item.price) || 0) * (Number(item.quantity) || 0), 0);
    const itemCount = cartItems.reduce((sum, item) => sum + (Number(item.quantity) || 0), 0);
    const itemsSummary = cartItems.map(item => `${item.name} x${item.quantity}`).join(", ");

    const item = {
      ...data,
      total,
      itemCount,
      itemsSummary,
      _id: randomUUID(),
      userId,
      status: "confirmado",
      createdAt: new Date()
    };

    await db.collection(ORDERS_COLLECTION_NAME).insertOne(item);
    await db.collection(CART_COLLECTION_NAME).deleteMany({ userId });
    await syncUserOrders(req.user._id);

    res.status(201).json({ id: item._id });
  } catch (error) {
    sendError(res, error, "order create failed");
  }
}
