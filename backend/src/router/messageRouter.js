import { Router } from "express";
import { getMessages, createMessage } from "../controller/messageController.js";
import { requireAuth } from "../middleware/auth.js";

const router = Router();

router.use(requireAuth);

router.get("/", getMessages);
router.post("/", createMessage);

export default router;
