import { randomUUID } from "node:crypto";
import { db } from "../db.js";
import { cleanObject, sendError } from "../middleware/auth.js";

export async function getProducts(_req, res) {
  try {
    const items = await db.collection("products").find({}).sort({ createdAt: -1 }).toArray();
    res.json({ items: items.map(({ _id, ...item }) => ({ ...item, id: String(_id) })) });
  } catch (error) {
    sendError(res, error, "products list failed");
  }
}

export async function getProductById(req, res) {
  try {
    const item = await db.collection("products").findOne({ _id: req.params.id });
    if (!item) return res.status(404).json({ error: "PRODUCT_NOT_FOUND" });
    const { _id, ...data } = item;
    res.json({ item: { ...data, id: String(_id) } });
  } catch (error) {
    sendError(res, error, "product read failed");
  }
}

export async function createProduct(req, res) {
  try {
    const data = cleanObject(req.body);
    if (!String(data.name || "").trim()) {
      return res.status(400).json({ error: "INVALID_PRODUCT" });
    }
    const item = {
      ...data,
      _id: String(data.id || randomUUID()),
      createdAt: new Date(),
      updatedAt: new Date()
    };
    delete item.id;
    await db.collection("products").insertOne(item);
    res.status(201).json({ id: item._id, item: { ...item, id: item._id, _id: undefined } });
  } catch (error) {
    sendError(res, error, "product create failed");
  }
}

export async function updateProduct(req, res) {
  try {
    const data = cleanObject(req.body);
    const result = await db.collection("products").updateOne(
      { _id: req.params.id },
      { $set: { ...data, updatedAt: new Date() } }
    );
    if (!result.matchedCount) return res.status(404).json({ error: "PRODUCT_NOT_FOUND" });
    res.json({ success: true });
  } catch (error) {
    sendError(res, error, "product update failed");
  }
}

export async function deleteProduct(req, res) {
  try {
    await db.collection("products").deleteOne({ _id: req.params.id });
    res.json({ success: true });
  } catch (error) {
    sendError(res, error, "product delete failed");
  }
}
