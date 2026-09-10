# Android Data Layer (Copia)

Esta carpeta es una copia aislada de la capa de datos de la aplicación Android. **No afecta al proyecto principal** y sirve únicamente para propósitos de referencia o migración futura.

## Estructura de Directorios

- **`api/`**: Contiene las interfaces y clientes de Retrofit para realizar peticiones HTTP al servidor backend real.
- **`data/`**: Contiene la lógica de la base de datos local (Room), incluyendo entidades, DAOs (Data Access Objects) y clases de repositorio para la mediación de datos.
- **`models/`**: Contiene los modelos de datos (Data classes o POJOs) utilizados en la aplicación, generalmente para serializar/deserializar JSON o para uso interno.

## Notas

- Esta es solo una copia. Si deseas modificar la lógica real de la app, debes hacerlo en el directorio original: `app/src/main/java/com/example/android/`.
- No requiere configuración adicional, ya que esta carpeta no se compila con el proyecto Android (es externa al source set de la app).
