# Imagen oficial de Node.js compatible con el backend.
FROM node:20-alpine

ENV NODE_ENV=production
WORKDIR /app

# El contexto de compilación es la raíz del repositorio.
COPY backend/package.json ./package.json
RUN npm install --omit=dev --no-audit --no-fund

# Copia únicamente el código del backend.
COPY backend/src ./src

# Cloud Run inyecta PORT; el servidor escucha en 0.0.0.0.
EXPOSE 8080

# Ejecutar con el usuario sin privilegios incluido en la imagen.
USER node

CMD ["node", "src/server.js"]
