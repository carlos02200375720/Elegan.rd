# Admin Directory
Esta carpeta está reservada para módulos o recursos de administración (app/src/admin).

## Publicación de videos de productos

La publicación sube videos, portadas y fotos de galería a Firebase Storage; Firestore guarda las URLs de descarga. Activa Firebase Storage en el proyecto y confirma que la configuración Firebase de Android tenga definido el bucket predeterminado. Despliega las reglas incluidas en `storage.rules` con `firebase deploy --only storage`.

La app genera una portada a partir del video si no se selecciona una foto. Los videos se pueden reproducir desde la ficha del producto; Inicio y la galería usan la portada y las fotos. Los archivos elegidos antes de esta actualización que tengan una URI `content://` guardada en Firestore deben volver a publicarse desde el dispositivo original.
