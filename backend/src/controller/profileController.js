import { db } from "../db.js";
import { cleanObject, sendError } from "../middleware/auth.js";

export async function getProfile(req, res) {
  try {
    const item = await db.collection("profiles").findOne({ userId: String(req.user._id) });
    res.json({
      item: item
        ? { ...item, id: String(item._id) }
        : { userId: String(req.user._id), displayName: req.user.name || "", email: req.user.email || "" }
    });
  } catch (error) {
    sendError(res, error, "profile read failed");
  }
}

export async function updateProfile(req, res) {
  try {
    const data = cleanObject(req.body);
    await db.collection("profiles").updateOne(
      { userId: String(req.user._id) },
      {
        $set: { ...data, userId: String(req.user._id), updatedAt: new Date() },
        $setOnInsert: { createdAt: new Date() }
      },
      { upsert: true }
    );
    res.json({ success: true });
  } catch (error) {
    sendError(res, error, "profile update failed");
  }
}
