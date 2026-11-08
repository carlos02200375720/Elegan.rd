import { Router } from "express";
import {
  getAnnouncements,
  createAnnouncement,
  updateAnnouncement,
  deleteAnnouncement
} from "../controller/announcementController.js";
import { requireAuth, requireAdmin } from "../middleware/auth.js";

const router = Router();

router.get("/", getAnnouncements);
router.post("/", requireAuth, requireAdmin, createAnnouncement);
router.patch("/:id", requireAuth, requireAdmin, updateAnnouncement);
router.delete("/:id", requireAuth, requireAdmin, deleteAnnouncement);

export default router;
