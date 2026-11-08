import { randomUUID } from "node:crypto";
import multer from "multer";
import { Storage } from "@google-cloud/storage";
import { sendError } from "../middleware/auth.js";

export const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 100 * 1024 * 1024 },
  fileFilter(_req, file, callback) {
    const allowed = /^(image\/(jpeg|png|webp|gif)|video\/(mp4|webm|quicktime))$/i;
    callback(allowed.test(file.mimetype) ? null : new Error("UNSUPPORTED_MEDIA_TYPE"), allowed.test(file.mimetype));
  }
});

const gcs = new Storage();

export async function uploadMedia(req, res) {
  try {
    if (!req.file) return res.status(400).json({ error: "FILE_REQUIRED" });
    const bucketName = process.env.GCS_BUCKET_NAME;
    if (!bucketName) return res.status(503).json({ error: "MEDIA_STORAGE_NOT_CONFIGURED" });

    const safeName = String(req.body?.name || req.file.originalname || "media").replace(/[^a-zA-Z0-9._-]/g, "_");
    const objectName = `movil/${Date.now()}-${randomUUID()}-${safeName}`;
    const file = gcs.bucket(bucketName).file(objectName);

    await file.save(req.file.buffer, {
      resumable: false,
      metadata: { contentType: req.file.mimetype, cacheControl: "public, max-age=31536000" }
    });

    const [url] = await file.getSignedUrl({
      action: "read",
      expires: Date.now() + 7 * 24 * 60 * 60 * 1000
    });

    res.status(201).json({
      url,
      objectName,
      contentType: req.file.mimetype,
      size: req.file.size
    });
  } catch (error) {
    sendError(res, error, "media upload failed");
  }
}
