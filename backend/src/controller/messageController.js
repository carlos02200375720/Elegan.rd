import { randomUUID } from "node:crypto";
import { db } from "../db.js";
import { sendError } from "../middleware/auth.js";
import {
  SUPPORT_MESSAGES_COLLECTION_NAME,
  formatSupportMessageResponse
} from "../modelo/index.js";

export async function getMessages(req, res) {
  try {
    const items = await db.collection(SUPPORT_MESSAGES_COLLECTION_NAME).find({ userId: String(req.user._id) }).sort({ createdAt: 1 }).toArray();
    res.json({ items: items.map(formatSupportMessageResponse) });
  } catch (error) {
    sendError(res, error, "messages list failed");
  }
}

export async function createMessage(req, res) {
  try {
    const value = String(req.body?.text || "").trim();
    if (!value || value.length > 4000) {
      return res.status(400).json({ error: "INVALID_MESSAGE" });
    }
    const userId = String(req.user._id);
    const item = {
      _id: randomUUID(),
      userId,
      sender: "user",
      senderName: String(req.user.name || "Cliente"),
      text: value,
      createdAt: new Date()
    };
    await db.collection(SUPPORT_MESSAGES_COLLECTION_NAME).insertOne(item);

    const normalized = value.toLowerCase();
    const reply = normalized.includes("envio") || normalized.includes("entrega")
      ? "Gracias por tu consulta. Nuestro equipo de Elegan te ayudará con la información de entrega de tu pedido."
      : normalized.includes("talla") || normalized.includes("medida")
        ? "Gracias por escribirnos. Nuestro equipo puede orientarte con las tallas y medidas disponibles."
        : normalized.includes("devolucion") || normalized.includes("cambio")
          ? "Recibimos tu consulta sobre cambios o devoluciones. Un asesor de Elegan te dará seguimiento."
          : normalized.includes("descuento") || normalized.includes("promocion")
            ? "Gracias por consultar nuestras promociones. Revisa los anuncios y ofertas vigentes en la tienda."
            : "Gracias por comunicarte con Elegan. Hemos recibido tu consulta y nuestro equipo te dará seguimiento.";

    await db.collection(SUPPORT_MESSAGES_COLLECTION_NAME).insertOne({
      _id: randomUUID(),
      userId,
      sender: "support",
      senderName: "Elegan Concierge",
      text: reply,
      createdAt: new Date(Date.now() + 1)
    });

    res.status(201).json({ id: item._id });
  } catch (error) {
    sendError(res, error, "message create failed");
  }
}
