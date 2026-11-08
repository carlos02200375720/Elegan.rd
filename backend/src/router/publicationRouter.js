import { Router } from "express";
import {
  getPublicationStats,
  getPublicationLikes,
  togglePublicationLike,
  getPublicationComments,
  createPublicationComment,
  sharePublication
} from "../controller/publicationController.js";
import { requireAuth } from "../middleware/auth.js";

const router = Router();

router.get("/stats", getPublicationStats);
router.get("/likes", requireAuth, getPublicationLikes);
router.put("/:productId/like", requireAuth, togglePublicationLike);
router.get("/:productId/comments", getPublicationComments);
router.post("/:productId/comments", requireAuth, createPublicationComment);
router.post("/:productId/share", requireAuth, sharePublication);

export default router;
