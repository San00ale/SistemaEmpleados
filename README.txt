═══════════════════════════════════════════════════════════════
  SISTEMA DE REGISTRO DE EMPLEADOS — UNIVERSIDAD TÉCNICA DE AMBATO
  Versión 1.0  |  Java Swing + JasperReports + MySQL (XAMPP)
═══════════════════════════════════════════════════════════════

╔══════════════════════════════════╗
║   CREDENCIALES DE PRUEBA         ║
╠══════════════════════════════════╣
║  ADMIN    → admin   / admin123   ║
║  EMPLEADO → jperez  / emp123     ║
╚══════════════════════════════════╝

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. REQUISITOS PREVIOS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  • Java JDK 11 o superior
  • Apache NetBeans IDE (cualquier versión reciente)
  • XAMPP con MySQL activo (phpMyAdmin)
  • Apache Ant (incluido en NetBeans)
  • iReport / JasperReports ya descargados (usted los tiene)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
2. LIBRERÍAS NECESARIAS EN /lib
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Copie los siguientes JARs dentro de la carpeta  lib/
(los que ya tiene de JasperReports + el conector MySQL):

  JASPERREPORTS (de su instalación de iReport):
  ┌─────────────────────────────────────────────────────────┐
  │  jasperreports-X.X.X.jar           ← principal         │
  │  commons-beanutils-X.X.jar                             │
  │  commons-collections-X.X.jar                           │
  │  commons-digester-X.X.jar                              │
  │  commons-logging-X.X.jar                               │
  │  itext-X.X.X.jar   (o itextpdf)    ← para PDF         │
  │  poi-X.X.X.jar      (opcional, para Excel)             │
  │  jfreechart-X.X.X.jar (opcional, para gráficos)        │
  └─────────────────────────────────────────────────────────┘

  MYSQL CONNECTOR:
  ┌─────────────────────────────────────────────────────────┐
  │  mysql-connector-j-8.X.X.jar                           │
  │  (Descargar de: https://dev.mysql.com/downloads/       │
  │   connector/j/  → Platform Independent → ZIP)          │
  └─────────────────────────────────────────────────────────┘

  RUTA DE BÚSQUEDA DE JARS EN SU PC (iReport típico):
    C:\Program Files (x86)\Jaspersoft\iReport-X.X\lib\
    o donde haya instalado iReport.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
3. CREAR LA BASE DE DATOS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  1. Inicie XAMPP → Start Apache y MySQL.
  2. Abra http://localhost/phpmyadmin
  3. Menú: Importar → seleccione  sql/bd_empleados.sql
  4. Clic en "Continuar".
  ✔ Se creará la BD  bd_empleados  con tablas, vistas y datos de prueba.

  Si prefiere línea de comandos:
    mysql -u root -p < sql/bd_empleados.sql

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
4. ABRIR EL PROYECTO EN NETBEANS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  OPCIÓN A — Proyecto Ant existente (recomendado):
    File → Open Project → seleccione la carpeta SistemaEmpleados/
    NetBeans detectará el build.xml automáticamente.

  OPCIÓN B — Proyecto Java con fuentes existentes:
    File → New Project → Java with Ant → Java Project with Existing Sources
    Source Folder → agregar src/
    Libraries    → agregar todos los JARs de lib/

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
5. AGREGAR LIBRERÍAS EN NETBEANS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Botón derecho sobre el proyecto → Properties → Libraries
  → Add JAR/Folder → seleccione cada JAR de la carpeta lib/

  O directamente copie los JARs a lib/ y en build.xml ya están
  incluidos mediante:  <fileset dir="${lib.dir}"><include name="**/*.jar"/>

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
6. COMPILAR Y EJECUTAR
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Desde NetBeans:
    Run → Build Project   (Shift+F11)
    Run → Run Project     (F6)

  Desde terminal (en la raíz del proyecto):
    ant run

  Clase principal:  sistema.view.FrmLogin

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
7. COMPILAR REPORTES JRXML → JASPER
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Los archivos JRXML se compilan automáticamente en tiempo de
  ejecución si no existe el .jasper correspondiente.

  Para precompilar con iReport:
    Abra iReport → File → Open → seleccione cada .jrxml
    Build → Compile Report   → genera el .jasper en la misma carpeta

  Archivos de reporte en:
    src/sistema/reports/
      ReporteCompleto.jrxml   ← Empleado tiempo completo
      ReporteParcial.jrxml    ← Empleado tiempo parcial
      ReporteFecha.jrxml      ← Asistencias por fecha

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
8. ESTRUCTURA DEL PROYECTO
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  SistemaEmpleados/
  ├── build.xml                    ← Script Apache Ant
  ├── README.txt                   ← Este archivo
  ├── sql/
  │   └── bd_empleados.sql         ← Script completo de BD
  ├── lib/                         ← JARs externos (copiar aquí)
  └── src/
      └── sistema/
          ├── dao/
          │   ├── Conexion.java    ← Singleton conexión MySQL
          │   ├── UsuarioDAO.java  ← CRUD usuarios
          │   ├── EmpleadoDAO.java ← CRUD empleados
          │   └── AsistenciaDAO.java ← CRUD asistencias
          ├── model/
          │   ├── Usuario.java
          │   ├── Empleado.java
          │   └── Asistencia.java
          ├── util/
          │   ├── Conexion.java
          │   ├── Estilos.java     ← Colores UTA rojo/blanco
          │   ├── Sesion.java      ← Singleton sesión activa
          │   └── Utilidades.java  ← SHA-256, validar cédula, etc.
          ├── view/
          │   ├── FrmLogin.java          ← Pantalla de acceso
          │   ├── FrmPrincipal.java      ← Menú principal (ADMIN/EMPLEADO)
          │   ├── FrmEmpleados.java      ← Lista CRUD empleados
          │   ├── FrmEmpleadoForm.java   ← Alta/modificación empleado
          │   ├── FrmRegistroAsistencia.java ← Registro diario
          │   ├── FrmReporteEmpleado.java    ← Reporte mensual
          │   └── FrmReporteFecha.java       ← Reporte por fecha
          └── reports/
              ├── ReporteCompleto.jrxml  ← Tiempo completo
              ├── ReporteParcial.jrxml   ← Tiempo parcial
              └── ReporteFecha.jrxml     ← Por fecha

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
9. PERFILES Y PERMISOS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  ADMIN:
    ✔ Registrar asistencia
    ✔ Ver reporte por empleado
    ✔ Ver reporte por fecha
    ✔ Listar empleados
    ✔ Crear nuevo empleado
    ✔ Modificar empleado
    ✔ Eliminar empleado (baja lógica)

  EMPLEADO:
    ✔ Registrar asistencia
    ✔ Ver reporte por empleado
    ✔ Ver reporte por fecha
    ✘ Gestión de empleados (oculto en menú)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
10. REGLAS DE NEGOCIO IMPLEMENTADAS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  • Jornada mañana : 08:00 – 13:00
  • Almuerzo fijo  : 13:00 – 14:00 (no se registra)
  • Jornada tarde  : 14:00 – 17:00
  • Total diario   : 8 horas

  TIEMPO COMPLETO:
    • Sueldo fijo : $1,500.00 / mes
    • Descuento   : $0.20 × cada minuto de atraso
    • Atraso mañana = entrada real > 08:00
    • Atraso tarde  = entrada real > 14:00

  TIEMPO PARCIAL:
    • Tarifa : $5.00 / hora
    • Máximo : 8 horas diarias
    • Pago   : horas_trabajadas × $5.00

  VALIDACIONES:
    • No duplicar registro por empleado-día (UNIQUE en BD)
    • Cédula ecuatoriana de 10 dígitos con dígito verificador
    • Hora entrada < hora salida en cada jornada
    • Contraseñas almacenadas como SHA-256

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
11. SOLUCIÓN DE PROBLEMAS COMUNES
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  ❌ "Communications link failure"
     → Verifique que MySQL esté activo en XAMPP.
     → Revise URL en Conexion.java (puerto 3306 por defecto).

  ❌ "Access denied for user 'root'@'localhost'"
     → XAMPP sin contraseña: PASS = "" (ya configurado).
     → Si tiene contraseña, actualice PASS en Conexion.java.

  ❌ "ClassNotFoundException: com.mysql.cj.jdbc.Driver"
     → El JAR mysql-connector-j no está en lib/.

  ❌ Error al generar PDF
     → Verifique que jasperreports-X.X.X.jar esté en lib/.
     → Los archivos .jrxml deben estar en src/sistema/reports/

  ❌ "No suitable driver found"
     → El mysql-connector no se carga; asegúrese de agregarlo
       como Library en NetBeans (Properties > Libraries).

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
11. DOCKER (MySQL + aplicación)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Requisito: Docker Desktop instalado y en ejecución.

  A) Solo base de datos (recomendado en Windows con NetBeans):
     copy .env.example .env
     docker compose up -d mysql

     MySQL queda en localhost:3307 (usuario root / clave root).
     En NetBeans, al ejecutar, defina variables de entorno:
       DB_HOST=localhost
       DB_PORT=3307
       DB_USER=root
       DB_PASSWORD=root

  B) Imagen completa de la aplicación (Linux):
     docker compose --profile app up -d --build

     La interfaz Swing en contenedor requiere pantalla gráfica;
     en Windows use la opción A y ejecute el .jar o NetBeans en su PC.

  Comandos útiles:
     docker compose logs -f mysql
     docker compose down
     docker compose down -v    (borra datos de MySQL)

  Archivos: Dockerfile, docker-compose.yml, docker/
