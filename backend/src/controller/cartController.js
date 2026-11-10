import { randomUUID } from "node:crypto";
import { db } from "../db.js";
import { cleanObject, sendError, owned } from "../middleware/auth.js";
import {
  CART_COLLECTION_NAME,
  PRODUCTS_COLLECTION_NAME,
  formatCartResponse
} from "../modelo/index.js";

export async function getCart(req, res) {
  try {
    const items = await db
      .collection(CART_COLLECTION_NAME)
      .find({ userId: String(req.user._id) })
      .sort({ createdAt: 1 })
      .toArray();
    res.json({ items: items.map(formatCartResponse) });
  } catch (error) {
    sendError(res, error, "cart list failed");
  }
}

export async function addToCart(req, res) {
  try {
    const data = cleanObject(req.body);
    const productId = String(data.productId || "");
    const product = await db.collection(PRODUCTS_COLLECTION_NAME).findOne({ _id: productId });
    if (!product) return res.status(404).json({ error: "PRODUCT_NOT_FOUND" });

    const quantity = Number(data.quantity) || 1;
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > 99) {
      return res.status(400).json({ error: "INVALID_QUANTITY" });
    }

    const filter = {
      userId: String(req.user._id),
      productId,
      selectedSize: String(data.selectedSize || ""),
      selectedColor: String(data.selectedColor || "")
    };

    const existing = await db.collection(CART_COLLECTION_NAME).findOne(filter);
    if (existing) {
      await db.collection(CART_COLLECTION_NAME).updateOne(
        { _id: existing._id },
        {
          $inc: { quantity },
          $set: {
            updatedAt: new Date(),
            name: product.name || "",
            price: Number(product.price) || 0,
            imageUrl: product.imageUrl || ""
          }
        }
      );
      return res.json({ id: existing._id });
    }

    const item = {
      ...filter,
      _id: randomUUID(),
      name: product.name || "",
      price: Number(product.price) || 0,
      imageUrl: product.imageUrl || "",
      quantity,
      createdAt: new Date(),
      updatedAt: new Date()
    };

    await db.collection(CART_COLLECTION_NAME).insertOne(item);
    return res.status(201).json({ id: item._id });
  } catch (error) {
    sendError(res, error, "cart add failed");
  }
}

export async function updateCartItem(req, res) {
  try {
    const quantity = Number(req.body?.quantity);
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > 99) {
      return res.status(400).json({ error: "INVALID_QUANTITY" });
    }
    const result = await db.collection(CART_COLLECTION_NAME).updateOne(
      owned(String(req.user._id), req.params.id),
      { $set: { quantity, updatedAt: new Date() } }
    );
    if (!result.matchedCount) return res.status(404).json({ error: "CART_ITEM_NOT_FOUND" });
    res.json({ success: true });
  } catch (error) {
    sendError(res, error, "cart update failed");
  }
}

export async function removeCartItem(req, res) {
  try {
    await db.collection(CART_COLLECTION_NAME).deleteOne(owned(String(req.user._id), req.params.id));
    res.json({ success: true });
  } catch (error) {
    sendError(res, error, "cart remove failed");
  }
}

export async function clearCart(req, res) {
  try {
    await db.collection(CART_COLLECTION_NAME).deleteMany({ userId: String(req.user._id) });
    res.json({ success: true });
  } catch (error) {
    sendError(res, error, "cart clear failed");
  }
}
