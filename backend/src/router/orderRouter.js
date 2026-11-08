import { Router } from "express";
import { getOrders, createOrder } from "../controller/orderController.js";
import { requireAuth } from "../middleware/auth.js";

const router = Router();

router.use(requireAuth);

router.get("/", getOrders);
router.post("/", createOrder);

export default router;
