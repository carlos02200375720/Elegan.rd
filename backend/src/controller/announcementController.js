import { randomUUID } from "node:crypto";
import { db } from "../db.js";
import { cleanObject, sendError } from "../middleware/auth.js";

export async function getAnnouncements(_req, res) {
  try {
    const items = await db.collection("announcements").find({ isActive: true }).sort({ createdAt: -1 }).toArray();
    res.json({ items: items.map(({ _id, ...item }) => ({ ...item, id: String(_id) })) });
  } catch (error) {
    sendError(res, error, "announcements list failed");
  }
}

export async function createAnnouncement(req, res) {
  try {
    const data = cleanObject(req.body);
    if (!String(data.title || "").trim()) {
      return res.status(400).json({ error: "INVALID_ANNOUNCEMENT" });
    }
    const item = {
      ...data,
      _id: String(data.id || randomUUID()),
      createdAt: new Date(),
      updatedAt: new Date()
    };
    delete item.id;
    await db.collection("announcements").insertOne(item);
    res.status(201).json({ id: item._id });
  } catch (error) {
    sendError(res, error, "announcement create failed");
  }
}

export async function updateAnnouncement(req, res) {
  try {
    const result = await db.collection("announcements").updateOne(
      { _id: req.params.id },
      { $set: { ...cleanObject(req.body), updatedAt: new Date() } }
    );
    if (!result.matchedCount) return res.status(404).json({ error: "ANNOUNCEMENT_NOT_FOUND" });
    res.json({ success: true });
  } catch (error) {
    sendError(res, error, "announcement update failed");
  }
}

export async function deleteAnnouncement(req, res) {
  try {
    await db.collection("announcements").deleteOne({ _id: req.params.id });
    res.json({ success: true });
  } catch (error) {
    sendError(res, error, "announcement delete failed");
  }
}
