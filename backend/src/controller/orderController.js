import { randomUUID } from "node:crypto";
import { db } from "../db.js";
import { cleanObject, sendError } from "../middleware/auth.js";

export async function getOrders(req, res) {
  try {
    const filter = (req.user.role === "admin" || req.user.role === "superadmin")
      ? {}
      : { userId: String(req.user._id) };
    const items = await db.collection("orders").find(filter).sort({ createdAt: -1 }).toArray();
    res.json({ items: items.map(({ _id, ...item }) => ({ ...item, id: String(_id) })) });
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
    const cartItems = await db.collection("cart").find({ userId }).toArray();
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

    await db.collection("orders").insertOne(item);
    await db.collection("cart").deleteMany({ userId });
    res.status(201).json({ id: item._id });
  } catch (error) {
    sendError(res, error, "order create failed");
  }
}
