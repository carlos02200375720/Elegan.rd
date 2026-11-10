/**
 * Exportador principal de todos los modelos de colecciones de MongoDB
 * Ruta: backend/src/modelo/index.js
 */

export * from "./user.js";
export { default as UserModel } from "./user.js";

export * from "./superAdmin.js";
export { default as SuperAdminModel } from "./superAdmin.js";

export * from "./product.js";
export { default as ProductModel } from "./product.js";

export * from "./announcement.js";
export { default as AnnouncementModel } from "./announcement.js";

export * from "./guardada.js";
export { default as GuardadaModel } from "./guardada.js";

export * from "./savedProduct.js";
export { default as SavedProductModel } from "./savedProduct.js";

export * from "./likes.js";
export { default as LikeModel } from "./likes.js";

export * from "./publicationLike.js";
export { default as PublicationLikeModel } from "./publicationLike.js";

export * from "./comentario.js";
export { default as ComentarioModel } from "./comentario.js";

export * from "./publicationComment.js";
export { default as PublicationCommentModel } from "./publicationComment.js";

export * from "./publicationShare.js";
export { default as PublicationShareModel } from "./publicationShare.js";

export * from "./cart.js";
export { default as CartModel } from "./cart.js";

export * from "./order.js";
export { default as OrderModel } from "./order.js";

export * from "./supportMessage.js";
export { default as SupportMessageModel } from "./supportMessage.js";

export * from "./profile.js";
export { default as ProfileModel } from "./profile.js";
