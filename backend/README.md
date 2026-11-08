# Elegan Android API

Backend Node.js/Express para conectar la app Android con MongoDB Atlas. La API es la única capa autorizada para acceder a MongoDB; nunca se deben incluir credenciales de Atlas en el APK.

## Variables de entorno

Configura en el servicio backend (no en Android):

- `MONGODB_URI`: URI privada de MongoDB Atlas.
- `MONGODB_USERNAME` y `MONGODB_PASSWORD`: secretos de GitHub ya existentes; el backend los usa como autenticación de MongoDB cuando ambos están definidos y la URI no contiene credenciales. Nunca los pases al cliente Android.
- `MONGODB_DATABASE`: nombre de la base, por defecto `elegan`.
- `JWT_SECRET`: secreto aleatorio de al menos 32 caracteres.
- `GOOGLE_WEB_CLIENT_ID`: OAuth Web Client ID usado para verificar los ID tokens de Google.
- `CORS_ORIGINS`: orígenes permitidos separados por coma, si se consume desde web.

## Ejecutar localmente

1. Copia `.env.example` a `.env` y completa los valores localmente.
2. Ejecuta `npm install`.
3. Ejecuta `npm start`.

## Rutas iniciales

- `GET /health`
- `POST /api/movil/auth/register` — `{ name, email, password }`
- `POST /api/movil/auth/login` — `{ email, password }`
- `POST /api/movil/auth/google` — `{ idToken }`
- `GET /api/movil/auth/me` — requiere `Authorization: Bearer <token>`
- `POST /api/movil/auth/logout`

Los registros nuevos reciben el rol `customer`; el cliente no puede asignarse privilegios administrativos.

## Estado de la migración

Esta API implementa la primera base de autenticación MongoDB. Aún no reemplaza las colecciones/operaciones de catálogo, carrito, pedidos, publicaciones, favoritos, comentarios, mensajería ni archivos multimedia que hoy usa `EleganRepository`. No elimines Firebase ni migres datos hasta implementar y probar esos contratos. La API debe desplegarse en Cloud Run antes de configurar Android para usarla.
