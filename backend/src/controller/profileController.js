import { db } from "../db.js";
import { cleanObject, sendError } from "../middleware/auth.js";
import { PROFILES_COLLECTION_NAME, formatProfileResponse } from "../modelo/index.js";

export async function getProfile(req, res) {
  try {
    const item = await db.collection(PROFILES_COLLECTION_NAME).findOne({ userId: String(req.user._id) });
    res.json({
      item: formatProfileResponse(item, req.user)
    });
  } catch (error) {
    sendError(res, error, "profile read failed");
  }
}

export async function updateProfile(req, res) {
  try {
    const data = cleanObject(req.body);
    await db.collection(PROFILES_COLLECTION_NAME).updateOne(
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
