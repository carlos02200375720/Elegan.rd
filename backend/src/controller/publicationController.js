import { randomUUID } from "node:crypto";
import { db } from "../db.js";
import { sendError } from "../middleware/auth.js";
import {
  SAVED_PRODUCTS_COLLECTION_NAME,
  PUBLICATION_LIKES_COLLECTION_NAME,
  PUBLICATION_COMMENTS_COLLECTION_NAME,
  PUBLICATION_SHARES_COLLECTION_NAME,
  formatPublicationCommentResponse
} from "../modelo/index.js";

export async function getSavedProducts(req, res) {
  try {
    const rows = await db.collection(SAVED_PRODUCTS_COLLECTION_NAME).find({ userId: String(req.user._id) }).toArray();
    res.json({ ids: rows.map(x => x.productId) });
  } catch (error) {
    sendError(res, error, "saved products failed");
  }
}

export async function toggleSavedProduct(req, res) {
  try {
    const filter = { userId: String(req.user._id), productId: req.params.productId };
    if (req.body?.saved === false) {
      await db.collection(SAVED_PRODUCTS_COLLECTION_NAME).deleteOne(filter);
    } else {
      await db.collection(SAVED_PRODUCTS_COLLECTION_NAME).updateOne(
        filter,
        { $setOnInsert: { ...filter, createdAt: new Date() } },
        { upsert: true }
      );
    }
    res.json({ saved: req.body?.saved !== false });
  } catch (error) {
    sendError(res, error, "save product failed");
  }
}

export async function getPublicationStats(_req, res) {
  try {
    const [likes, comments, shares, saves] = await Promise.all([
      db.collection(PUBLICATION_LIKES_COLLECTION_NAME).aggregate([{ $group: { _id: "$productId", count: { $sum: 1 } } }]).toArray(),
      db.collection(PUBLICATION_COMMENTS_COLLECTION_NAME).aggregate([{ $group: { _id: "$productId", count: { $sum: 1 } } }]).toArray(),
      db.collection(PUBLICATION_SHARES_COLLECTION_NAME).aggregate([{ $group: { _id: "$productId", count: { $sum: 1 } } }]).toArray(),
      db.collection(SAVED_PRODUCTS_COLLECTION_NAME).aggregate([{ $group: { _id: "$productId", count: { $sum: 1 } } }]).toArray()
    ]);
    const ids = new Set([...likes, ...comments, ...shares, ...saves].map(x => x._id));
    const stats = {};
    for (const id of ids) {
      stats[id] = {
        likesCount: likes.find(x => x._id === id)?.count || 0,
        commentsCount: comments.find(x => x._id === id)?.count || 0,
        sharesCount: shares.find(x => x._id === id)?.count || 0,
        savesCount: saves.find(x => x._id === id)?.count || 0
      };
    }
    res.json({ stats });
  } catch (error) {
    sendError(res, error, "publication stats failed");
  }
}

export async function getPublicationLikes(req, res) {
  try {
    const rows = await db.collection(PUBLICATION_LIKES_COLLECTION_NAME).find({ userId: String(req.user._id) }).toArray();
    res.json({ ids: rows.map(x => x.productId) });
  } catch (error) {
    sendError(res, error, "publication likes failed");
  }
}

export async function togglePublicationLike(req, res) {
  try {
    const filter = { userId: String(req.user._id), productId: req.params.productId };
    const col = db.collection(PUBLICATION_LIKES_COLLECTION_NAME);
    const exists = await col.findOne(filter);
    if (exists) {
      await col.deleteOne(filter);
    } else {
      await col.insertOne({ ...filter, createdAt: new Date() });
    }
    res.json({ liked: !exists });
  } catch (error) {
    sendError(res, error, "publication like failed");
  }
}

export async function getPublicationComments(req, res) {
  try {
    const items = await db.collection(PUBLICATION_COMMENTS_COLLECTION_NAME).find({ productId: req.params.productId }).sort({ createdAt: 1 }).toArray();
    res.json({ items: items.map(formatPublicationCommentResponse) });
  } catch (error) {
    sendError(res, error, "comments list failed");
  }
}

export async function createPublicationComment(req, res) {
  try {
    const value = String(req.body?.text || "").trim();
    if (!value || value.length > 2000) {
      return res.status(400).json({ error: "INVALID_COMMENT" });
    }
    const item = {
      _id: randomUUID(),
      productId: req.params.productId,
      userId: String(req.user._id),
      displayName: String(req.body?.displayName || req.user.name || "Cliente"),
      text: value,
      createdAt: new Date()
    };
    await db.collection(PUBLICATION_COMMENTS_COLLECTION_NAME).insertOne(item);
    res.status(201).json({ id: item._id });
  } catch (error) {
    sendError(res, error, "comment create failed");
  }
}

export async function sharePublication(req, res) {
  try {
    await db.collection(PUBLICATION_SHARES_COLLECTION_NAME).insertOne({
      userId: String(req.user._id),
      productId: req.params.productId,
      createdAt: new Date()
    });
    res.status(201).json({ success: true });
  } catch (error) {
    sendError(res, error, "publication share failed");
  }
}
