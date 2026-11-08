import { Router } from "express";
import { register, login, googleLogin, getMe, logout } from "../controller/authController.js";
import { requireAuth, authLimiter } from "../middleware/auth.js";

const router = Router();

router.post("/register", authLimiter, register);
router.post("/login", authLimiter, login);
router.post("/google", authLimiter, googleLogin);
router.get("/me", requireAuth, getMe);
router.post("/logout", logout);

export default router;
