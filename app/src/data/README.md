# Elegan Data Layer

Esta es la carpeta física oficial de datos para la aplicación Elegan:
`app/src/data/`

## Estructura
- `model/Models.kt`: entidades y modelos de datos.
- `repository/EleganRepository.kt`: repositorio principal conectado a los endpoints móviles del backend Elegan.
- `remote/`: clientes Retrofit, contratos de API y sesión autenticada.
- `storage/GcsStorageManager.kt`: utilidades relacionadas con Google Cloud Storage.

La persistencia remota se realiza mediante el backend propio y MongoDB. Android no accede directamente a la base de datos.
