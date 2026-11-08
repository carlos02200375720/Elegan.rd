import { Router } from "express";
import {
  getPublicationStats,
  getPublicationLikes,
  togglePublicationLike,
  getPublicationComments,
  createPublicationComment,
  sharePublication
} from "../controller/publicationController.js";
import { requireAuth, optionalAuth } from "../middleware/auth.js";

const router = Router();

router.get("/stats", getPublicationStats);
router.get("/likes", requireAuth, getPublicationLikes);
router.put("/:productId/like", requireAuth, togglePublicationLike);
router.get("/:productId/comments", getPublicationComments);
router.post("/:productId/comments", optionalAuth, createPublicationComment);
router.post("/:productId/share", optionalAuth, sharePublication);

export default router;
