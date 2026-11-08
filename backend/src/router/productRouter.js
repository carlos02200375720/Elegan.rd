import { Router } from "express";
import {
  getProducts,
  getProductById,
  createProduct,
  updateProduct,
  deleteProduct
} from "../controller/productController.js";
import { requireAuth, requireAdmin } from "../middleware/auth.js";

const router = Router();

router.get("/", getProducts);
router.get("/:id", getProductById);
router.post("/", requireAuth, requireAdmin, createProduct);
router.patch("/:id", requireAuth, requireAdmin, updateProduct);
router.delete("/:id", requireAuth, requireAdmin, deleteProduct);

export default router;
