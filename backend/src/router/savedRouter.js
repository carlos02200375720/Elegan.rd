import { Router } from "express";
import { getSavedProducts, toggleSavedProduct } from "../controller/publicationController.js";
import { requireAuth } from "../middleware/auth.js";

const router = Router();

router.get("/", requireAuth, getSavedProducts);
router.put("/:productId", requireAuth, toggleSavedProduct);

export default router;
