# Guía Completa del Proyecto ECOLIMP

Este documento detalla el funcionamiento interno de la aplicación móvil y el backend en Python, además de explicar cómo desplegar el servidor de forma **completamente gratuita** para tu presentación en el instituto.

---

## 1. Arquitectura del Sistema

El proyecto sigue una arquitectura moderna **Cliente-Servidor (Offline-First)**:
*   **Cliente (App Android):** Escrita en Java. Su diseño permite que los trabajadores operen aunque no haya internet (los datos se guardan temporalmente en el celular usando SQLite).
*   **Servidor (Backend Python):** Creado con FastAPI. Centraliza la información, recibe las fotos, procesa la Inteligencia Artificial y expone los reportes.

---

## 2. ¿Cómo funcionan los apartados de la App?

### A. Login y Sesión (`LoginActivity.java`)
*   Valida el acceso del usuario.
*   Utiliza `SessionManager` para guardar un "token" en el celular (`SharedPreferences`). Así, si el trabajador cierra la app y la vuelve a abrir, no tiene que poner la contraseña de nuevo.

### B. Dashboard (`DashboardFragment.java`)
*   Es el panel de control. Lee la base de datos local (SQLite) y suma todo el peso y volumen recolectado en el día.
*   Muestra visualmente cuántos registros **no han sido enviados al servidor** (Pendientes de sincronización).
*   Tiene el botón principal para iniciar el envío masivo de datos a la nube.

### C. Scanner y Registro (`ScannerFragment` y `RecoleccionFragment`)
*   El operario puede usar la cámara para leer un Código QR pegado en el contenedor, lo que auto-completa el tipo de residuo.
*   **Integración de IA (Fotos):** Al tomar una fotografía del residuo, la imagen se envía al servidor Python (endpoint `/api/v1/analisis-foto`). El servidor simula pasar esta imagen por un modelo de visión artificial (como YOLO) y devuelve el tipo de residuo detectado y un porcentaje de confianza.
*   Si no hay internet, el registro se guarda en la base de datos local (`DatabaseHelper.java`) con una marca `isSynced = 0`.

### D. Historial y Reportes (`ReportesFragment.java`)
*   Muestra una lista en pantalla (`RecyclerView`) de todas las recolecciones hechas.
*   Utiliza un sistema de filtros (por fecha o tipo) para que el supervisor busque rápidamente un registro.

---

## 3. ¿Cómo presentar el proyecto en el Instituto? (Hosting Gratis)

Como mencionaste que **no llevarás tu laptop**, necesitas que el servidor Python esté alojado en internet 24/7. 

La mejor plataforma gratuita para proyectos de instituto con Python es **Render.com**. Render te dará un dominio real (ejemplo: `https://ecolimp-api.onrender.com`) que tu celular podrá consultar desde el Wi-Fi del instituto o con tus datos móviles.

### Pasos para subir el Backend a Render:

**Paso 1: Subir el código a GitHub**
1. Crea una cuenta en [GitHub.com](https://github.com/).
2. Crea un nuevo repositorio privado o público (ej. `ecolimp-backend`).
3. Sube únicamente los archivos de la carpeta `backend_ecolim` (`main.py` y `requirements.txt`) a ese repositorio.

**Paso 2: Crear el servidor en Render**
1. Entra a [Render.com](https://render.com/) y regístrate usando tu cuenta de GitHub.
2. Haz clic en el botón **"New +"** y selecciona **"Web Service"**.
3. Selecciona tu repositorio de GitHub `ecolimp-backend`.
4. Llena los datos así:
   * **Name:** ecolimp-api
   * **Language:** Python 3
   * **Branch:** main
   * **Build Command:** `pip install -r requirements.txt`
   * **Start Command:** `uvicorn main:app --host 0.0.0.0 --port $PORT`
   * **Instance Type:** Free (Gratis)
5. Haz clic en **"Create Web Service"**.

**Paso 3: Esperar y obtener la URL**
Render tardará unos 2 o 3 minutos en instalar las cosas. Cuando termine, arriba a la izquierda te mostrará un enlace verde, por ejemplo:
👉 `https://ecolimp-api.onrender.com`

**Paso 4: Conectar tu APK al servidor de Render**
1. Vuelve a Android Studio en tu PC.
2. Abre el archivo `Constants.java`.
3. Cambia la variable de la URL para que apunte a tu nuevo servidor:
   ```java
   public static final String API_BASE_URL = "https://ecolimp-api.onrender.com/api/v1";
   ```
4. Conecta tu celular por cable a la PC y haz clic en "Run" (el botón de Play verde) para instalar la aplicación definitiva. También puedes generar el APK y pasártelo por WhatsApp.

> [!IMPORTANT]
> **Prueba final:** Al hacer esto, puedes apagar tu laptop tranquilamente. Tu aplicación de celular instalada enviará los datos y las fotos a través de internet hacia los servidores de Render, y funcionará perfecto durante tu exposición en el instituto.
