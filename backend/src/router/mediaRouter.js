import { Router } from "express";
import { upload, uploadMedia } from "../controller/mediaController.js";
import { requireAuth, requireAdmin } from "../middleware/auth.js";

const router = Router();

router.post("/", requireAuth, requireAdmin, upload.single("file"), uploadMedia);

export default router;
