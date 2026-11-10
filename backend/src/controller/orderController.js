import { randomUUID } from "node:crypto";
import { db } from "../db.js";
import { cleanObject, sendError } from "../middleware/auth.js";
import {
  ORDERS_COLLECTION_NAME,
  CART_COLLECTION_NAME,
  formatOrderResponse
} from "../modelo/index.js";

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

    res.status(201).json({ id: item._id });
  } catch (error) {
    sendError(res, error, "order create failed");
  }
}
