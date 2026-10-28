# Elegan Data Layer

Esta es la carpeta física oficial de datos para la aplicación Elegan:
`app/src/data/`

## Estructura
- `model/Models.kt`: Entidades y modelos de datos (Product, CartItem, Order, UserProfile, Announcement, StoreCategory).
- `repository/EleganRepository.kt`: Repositorio principal de datos con Firestore y sincronización local.
- `storage/GcsStorageManager.kt`: Administrador de Google Cloud Storage con bucket `bucket-elegan-android`.
- `util/FirestoreErrorHandler.kt`: Manejador centralizado de errores de Firestore.
