import { Router } from "express";
import authRouter from "./authRouter.js";
import productRouter from "./productRouter.js";
import announcementRouter from "./announcementRouter.js";
import savedRouter from "./savedRouter.js";
import publicationRouter from "./publicationRouter.js";
import cartRouter from "./cartRouter.js";
import orderRouter from "./orderRouter.js";
import messageRouter from "./messageRouter.js";
import profileRouter from "./profileRouter.js";
import mediaRouter from "./mediaRouter.js";

const apiRouter = Router();

apiRouter.use("/auth", authRouter);
apiRouter.use("/products", productRouter);
apiRouter.use("/announcements", announcementRouter);
apiRouter.use("/saved", savedRouter);
apiRouter.use("/publications", publicationRouter);
apiRouter.use("/cart", cartRouter);
apiRouter.use("/orders", orderRouter);
apiRouter.use("/messages", messageRouter);
apiRouter.use("/profile", profileRouter);
apiRouter.use("/media", mediaRouter);

export default apiRouter;
export {
  authRouter,
  productRouter,
  announcementRouter,
  savedRouter,
  publicationRouter,
  cartRouter,
  orderRouter,
  messageRouter,
  profileRouter,
  mediaRouter
};
