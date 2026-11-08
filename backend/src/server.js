import express from "express";
import cors from "cors";
import helmet from "helmet";
import { connectDB, closeDB } from "./db.js";
import apiRouter from "./router/index.js";

const app = express();
app.disable("x-powered-by");
app.use(helmet());
app.use(express.json({ limit: "1mb" }));

const allowedOrigins = (process.env.CORS_ORIGINS || "")
  .split(",")
  .map((value) => value.trim())
  .filter(Boolean);

app.use(cors({
  origin(origin, callback) {
    if (!origin || allowedOrigins.length === 0 || allowedOrigins.includes(origin)) {
      return callback(null, true);
    }
    return callback(new Error("Origin not allowed"));
  }
}));

// Conexión inicial a MongoDB Atlas
await connectDB();

// Ruta de comprobación de salud para Cloud Run
app.get("/health", (_req, res) => {
  res.json({ status: "ok", service: "elegan-movil-api" });
});

// Rutas modulares de la API móvil
app.use("/api/movil", apiRouter);

// Manejador centralizado de errores
app.use((error, _req, res, _next) => {
  console.error("request failed", error);
  res.status(500).json({ error: "INTERNAL_ERROR" });
});

const port = Number(process.env.PORT || 8080);
const server = app.listen(port, "0.0.0.0", () => {
  console.log(`Elegan mobile API listening on port ${port}`);
});

async function shutdown() {
  console.log("Shutting down gracefully...");
  server.close(async () => {
    await closeDB();
    process.exit(0);
  });
}

process.on("SIGTERM", shutdown);
process.on("SIGINT", shutdown);

export default app;
