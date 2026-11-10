import { randomUUID } from "node:crypto";
import { db } from "../db.js";
import { sendError } from "../middleware/auth.js";
import {
  USERS_COLLECTION_NAME,
  GUARDADAS_COLLECTION_NAME,
  LIKES_COLLECTION_NAME,
  COMENTARIOS_COLLECTION_NAME,
  PUBLICATION_SHARES_COLLECTION_NAME,
  formatComentarioResponse
} from "../modelo/index.js";

export async function getSavedProducts(req, res) {
  try {
    const rows = await db.collection(GUARDADAS_COLLECTION_NAME).find({ userId: String(req.user._id) }).toArray();
    res.json({ ids: rows.map(x => x.productId) });
  } catch (error) {
    sendError(res, error, "saved products failed");
  }
}

export async function toggleSavedProduct(req, res) {
  try {
    const productId = String(req.params.productId);
    const userId = String(req.user._id);
    const filter = { userId, productId };
    const isSaved = req.body?.saved !== false;
    const col = db.collection(GUARDADAS_COLLECTION_NAME);

    if (!isSaved) {
      await col.deleteOne(filter);
    } else {
      await col.updateOne(
        filter,
        { $setOnInsert: { ...filter, createdAt: new Date() } },
        { upsert: true }
      );
    }
    res.json({ saved: isSaved });
  } catch (error) {
    sendError(res, error, "save product failed");
  }
}

export async function getPublicationStats(_req, res) {
  try {
    const [likes, comments, shares, saves] = await Promise.all([
      db.collection(LIKES_COLLECTION_NAME).aggregate([{ $group: { _id: "$productId", count: { $sum: 1 } } }]).toArray(),
      db.collection(COMENTARIOS_COLLECTION_NAME).aggregate([{ $group: { _id: "$productId", count: { $sum: 1 } } }]).toArray(),
      db.collection(PUBLICATION_SHARES_COLLECTION_NAME).aggregate([{ $group: { _id: "$productId", count: { $sum: 1 } } }]).toArray(),
      db.collection(GUARDADAS_COLLECTION_NAME).aggregate([{ $group: { _id: "$productId", count: { $sum: 1 } } }]).toArray()
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
    const rows = await db.collection(LIKES_COLLECTION_NAME).find({ userId: String(req.user._id) }).toArray();
    res.json({ ids: rows.map(x => x.productId) });
  } catch (error) {
    sendError(res, error, "publication likes failed");
  }
}

export async function togglePublicationLike(req, res) {
  try {
    const productId = String(req.params.productId);
    const userId = String(req.user._id);
    const filter = { userId, productId };
    const col = db.collection(LIKES_COLLECTION_NAME);
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
    const items = await db.collection(COMENTARIOS_COLLECTION_NAME).find({ productId: req.params.productId }).sort({ createdAt: 1 }).toArray();
    res.json({ items: items.map(formatComentarioResponse) });
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
    const productId = String(req.params.productId);
    const userId = req.user ? String(req.user._id) : `guest-${randomUUID().slice(0, 8)}`;
    const displayName = String(req.body?.displayName || req.user?.name || "Cliente").trim() || "Cliente";

    const item = {
      _id: randomUUID(),
      productId,
      userId,
      displayName,
      text: value,
      createdAt: new Date()
    };
    await db.collection(COMENTARIOS_COLLECTION_NAME).insertOne(item);

    res.status(201).json({ id: item._id });
  } catch (error) {
    sendError(res, error, "comment create failed");
  }
}

export async function sharePublication(req, res) {
  try {
    const productId = String(req.params.productId);
    const userId = req.user ? String(req.user._id) : `guest-${randomUUID().slice(0, 8)}`;

    await db.collection(PUBLICATION_SHARES_COLLECTION_NAME).insertOne({
      userId,
      productId,
      createdAt: new Date()
    });

    // Si el usuario está registrado, registrar en su documento de la colección 'users'
    if (req.user) {
      await db.collection(USERS_COLLECTION_NAME).updateOne(
        { _id: req.user._id },
        {
          $addToSet: {
            sharedPublications: productId
          },
          $set: { updatedAt: new Date() }
        }
      );
    }

    res.status(201).json({ success: true });
  } catch (error) {
    sendError(res, error, "publication share failed");
  }
}
