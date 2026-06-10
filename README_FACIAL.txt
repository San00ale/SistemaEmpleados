═══════════════════════════════════════════════════════════════
  MÓDULO DE RECONOCIMIENTO FACIAL — GUÍA DE INSTALACIÓN
  Sistema de Empleados — Universidad Técnica de Ambato
═══════════════════════════════════════════════════════════════

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. LIBRERÍA REQUERIDA: JavaCV
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
JavaCV es el wrapper oficial de OpenCV para Java.
Incluye: OpenCV, detección Haar, captura de cámara.

  URL DE DESCARGA:
  https://github.com/bytedeco/javacv/releases

  OPCIÓN A — Paquete completo (más fácil, ~300 MB):
    Descargar: javacv-platform-1.5.9-bin.zip
    Extraer y copiar TODOS los JAR a la carpeta lib/

  OPCIÓN B — Solo lo necesario (más ligero):
    Descargar estos JARs individualmente:
    ┌────────────────────────────────────────────────────────┐
    │  javacv-1.5.9.jar                                      │
    │  javacpp-1.5.9.jar                                     │
    │  opencv-4.6.0-1.5.9.jar                               │
    │  opencv-4.6.0-1.5.9-windows-x86_64.jar  ← su SO      │
    │  openblas-0.3.23-1.5.9.jar          ← OBLIGATORIO     │
    │  openblas-0.3.23-1.5.9-windows-x86_64.jar ← su SO    │
    │  ffmpeg-5.1.2-1.5.9.jar                               │
    │  ffmpeg-5.1.2-1.5.9-windows-x86_64.jar  ← su SO      │
    └────────────────────────────────────────────────────────┘

    Para Windows 32 bits: reemplazar "x86_64" por "x86"
    Para Linux:           reemplazar "windows" por "linux"
    Para macOS:           reemplazar "windows" por "macosx"

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1b. NETBEANS — AGREGAR lib/ AL PROYECTO
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Si NetBeans marca errores en import org.bytedeco.*:
    • Clic derecho en el proyecto → Properties → Libraries
    • Add JAR/Folder → seleccionar TODOS los .jar de lib/
    • O usar Clean and Build tras actualizar nbproject/project.properties

  Sin openblas-*.jar verá en ejecución:
    ClassNotFoundException: org.bytedeco.openblas.presets.openblas

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
2. ACTUALIZAR LA BASE DE DATOS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  BD nueva (recomendado):
    sql/bd_empleados.sql   → ya incluye empFotoRostro y empTieneFoto

  BD que ya existía sin columnas faciales:
    sql/actualizacion_facial.sql

  Columnas que usa Java (NO usar foto_rostro / tiene_foto):
    • empFotoRostro  LONGBLOB  → bytes JPEG del rostro
    • empTieneFoto   TINYINT   → 1 si tiene foto registrada

  Si ejecutó un script viejo con nombres incorrectos:
    sql/migrar_facial_legacy.sql

  Verificación en phpMyAdmin:
    SELECT empCedula, empTieneFoto,
           OCTET_LENGTH(empFotoRostro) AS bytes_foto, empIdUsuario
    FROM empleados WHERE empActivo=1;

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
2b. CONEXIÓN MySQL (Conexion.java)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  URL:  jdbc:mysql://localhost:3306/bd_empleados
  USER: root
  PASS: ""   ← cambiar si su XAMPP tiene contraseña

  Si aparece "Communications link failure":
    → Inicie MySQL en XAMPP antes de abrir el sistema.

  Driver: mysql-connector-j-9.6.0.jar (en lib/)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
3. CÓMO USAR EL RECONOCIMIENTO FACIAL
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  PASO 1 — REGISTRAR LA FOTO DEL EMPLEADO (solo ADMIN):
    • Iniciar sesión como administrador
    • Menú lateral → 📸 Registrar Foto Rostro
    • Buscar empleado por cédula
    • Asegurarse de tener buena iluminación
    • Presionar "Capturar Foto"
    • Si el rostro fue detectado → "Guardar en BD"

  PASO 2 — INICIAR SESIÓN CON FACIAL (cualquier empleado):
    • En la pantalla de login, ir a la pestaña "📷 Facial"
    • Colocarse frente a la cámara
    • El sistema reconoce automáticamente cada 2 segundos
    • O presionar "🔍 Reconocer" manualmente
    • Si hay coincidencia ≥ 60% → sesión iniciada

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
4. FLUJO COMPLETO DEL LOGIN FACIAL
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  ┌─────────────────────────────────────────┐
  │         PANTALLA DE LOGIN               │
  │  ┌──────────────┬──────────────────┐   │
  │  │ 🔑 Cred.    │  📷 Facial       │   │
  │  ├──────────────┴──────────────────┤   │
  │  │  TAB 1: usuario + contraseña    │   │
  │  │  TAB 2: webcam en tiempo real   │   │
  │  │          ↓                      │   │
  │  │  Detecta rostro con Haar Cascade│   │
  │  │          ↓                      │   │
  │  │  Compara con fotos en BD        │   │
  │  │          ↓                      │   │
  │  │  Similitud ≥ 60% → ACCESO OK   │   │
  │  └─────────────────────────────────┘   │
  └─────────────────────────────────────────┘

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
5. CONSEJOS PARA MEJOR RECONOCIMIENTO
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  ✔ Buena iluminación frontal (sin luz de fondo)
  ✔ Mirar directamente al lente de la cámara
  ✔ Sin lentes de sol, gorras o máscaras
  ✔ Registrar la foto en las mismas condiciones de uso
  ✔ Distancia de 30-60 cm de la cámara
  ✔ Si falla, ajustar UMBRAL_SIMILITUD en:
       src/sistema/util/ReconocimientoFacialService.java
       → línea: UMBRAL_SIMILITUD = 0.60  (bajar a 0.50 si necesita)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
6. ARCHIVO HAAR CASCADE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Ya incluido en el proyecto:
    src/sistema/util/haarcascade_frontalface_default.xml
  
  Este archivo es el modelo de detección facial de OpenCV.
  Se carga automáticamente al iniciar el módulo facial.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
7. REQUISITO: CUENTA DE ACCESO VINCULADA
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Para que el reconocimiento facial inicie sesión, el empleado
  DEBE tener una cuenta de usuario asignada (id_usuario no nulo).
  
  Si el empleado no tiene cuenta:
    → Admin: Gestión Empleados → Editar → marcar
      "Crear cuenta de acceso" y asignar username/contraseña.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
8. ARCHIVOS NUEVOS EN ESTE MÓDULO
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  sql/actualizacion_facial.sql           ← Ejecutar en BD
  src/sistema/dao/RostroDAO.java         ← CRUD fotos rostros
  src/sistema/util/ReconocimientoFacialService.java ← Lógica OpenCV
  src/sistema/util/haarcascade_frontalface_default.xml ← Modelo Haar
  src/sistema/view/PnlLoginFacial.java   ← Tab facial en login
  src/sistema/view/PnlCapturarRostro.java ← Captura foto empleado
  src/sistema/view/FrmLogin.java         ← MODIFICADO con 2 tabs
  src/sistema/view/FrmPrincipal.java     ← MODIFICADO menú facial
  src/sistema/model/Empleado.java        ← MODIFICADO + foto_rostro
