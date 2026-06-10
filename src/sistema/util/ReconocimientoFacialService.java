package sistema.util;

import org.bytedeco.javacv.*;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.*;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de reconocimiento facial usando OpenCV vía JavaCV.
 *
 * Responsabilidades:
 *  - Capturar frames de la cámara integrada de la laptop (índice 0)
 *  - Detectar rostros con Haar Cascade (haarcascade_frontalface_default.xml)
 *  - Convertir frame capturado a bytes JPEG para almacenar en BD
 *  - Comparar rostro capturado contra fotos almacenadas en BD
 *
 * DEPENDENCIAS (JARs en lib/):
 *  javacv-platform-1.5.9.jar  (incluye OpenCV, FFmpeg, etc.)
 *  Descarga: https://github.com/bytedeco/javacv/releases
 *  → javacv-platform-1.5.9-bin.zip  (~300 MB, incluye todo)
 *
 * ALTERNATIVA LIGERA (solo lo necesario):
 *  javacv-1.5.9.jar
 *  opencv-4.6.0-1.5.9-windows-x86_64.jar  (o la plataforma correcta)
 */
public class ReconocimientoFacialService {

    // ── Índice de cámara y umbrales ──────────────────────────────
    /** Usa SOLO la cámara integrada de la laptop (índice 0). */
    private static final int CAMERA_INDEX = 0;

    /**
     * Umbral de similitud mínima (0.0 – 1.0).
     * 0.40 = 40% de similitud → reconocido.
     * Reducido desde 0.60 porque el algoritmo de distancia L2 es básico.
     * Ajustar según condiciones de iluminación del entorno.
     */
    public static final double UMBRAL_SIMILITUD = 0.40;

    // ── Singleton de cámara compartida ───────────────────────────
    /** Instancia compartida del grabber de cámara para evitar problemas de MSMF */
    private static FrameGrabber grabberCompartido = null;
    /** Contador de referencias para saber cuándo cerrar la cámara */
    private static int contadorReferencias = 0;

    // ── Haar Cascade para detección de rostros ───────────────────
    private static CascadeClassifier detector;
    private static final String CASCADE_RESOURCE =
            "/sistema/util/haarcascade_frontalface_default.xml";

    // ── Singleton ────────────────────────────────────────────────
    private static ReconocimientoFacialService instancia;

    private ReconocimientoFacialService() {
        cargarCascade();
    }

    public static ReconocimientoFacialService getInstance() {
        if (instancia == null) instancia = new ReconocimientoFacialService();
        return instancia;
    }

    // ── Inicialización del detector Haar ──────────────────────────
    private void cargarCascade() {
        try {
            // Extraer el XML del classpath a un archivo temporal
            InputStream is = getClass().getResourceAsStream(CASCADE_RESOURCE);
            if (is == null) {
                // Intentar con ruta del sistema (OpenCV instalado)
                detector = new CascadeClassifier(
                    "haarcascade_frontalface_default.xml");
            } else {
                File tmp = File.createTempFile("haarcascade", ".xml");
                tmp.deleteOnExit();
                try (FileOutputStream fos = new FileOutputStream(tmp)) {
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = is.read(buf)) != -1) fos.write(buf, 0, n);
                }
                detector = new CascadeClassifier(tmp.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("[ReconocimientoFacial] Error al cargar cascade: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // CAPTURA DE CÁMARA
    // ─────────────────────────────────────────────────────────────

    /**
     * Abre la cámara integrada de la laptop.
     * Usa siempre el índice 0 (cámara integrada).
     * Implementa singleton para evitar problemas de MSMF en Windows.
     */
    public FrameGrabber abrirCamara() throws Exception {
        synchronized (ReconocimientoFacialService.class) {
            if (grabberCompartido == null) {
                grabberCompartido = new OpenCVFrameGrabber(CAMERA_INDEX);
                ((OpenCVFrameGrabber)grabberCompartido).setImageWidth(640);
                ((OpenCVFrameGrabber)grabberCompartido).setImageHeight(480);
                grabberCompartido.start();
            } else {
                // Intentar reiniciar si está detenido
                try {
                    grabberCompartido.start();
                } catch (Exception e) {
                    // Si falla, crear una nueva instancia
                    grabberCompartido = new OpenCVFrameGrabber(CAMERA_INDEX);
                    ((OpenCVFrameGrabber)grabberCompartido).setImageWidth(640);
                    ((OpenCVFrameGrabber)grabberCompartido).setImageHeight(480);
                    grabberCompartido.start();
                }
            }
            contadorReferencias++;
            return grabberCompartido;
        }
    }

    /**
     * Abre la cámara especificada.
     * @deprecated Use {@link #abrirCamara()} instead. Solo se soporta la cámara integrada.
     */
    @Deprecated
    public FrameGrabber abrirCamara(int indice) throws Exception {
        if (indice != CAMERA_INDEX) {
            throw new IllegalArgumentException(
                "Solo se soporta la cámara integrada (índice " + CAMERA_INDEX + "). " +
                "Recibido: " + indice);
        }
        return abrirCamara();
    }

    /**
     * Cierra la cámara de forma segura.
     * Implementa singleton con contador de referencias para evitar problemas de MSMF.
     * No cierra realmente la cámara para evitar problemas de MSMF.
     */
    public void cerrarCamara(FrameGrabber grabber) {
        synchronized (ReconocimientoFacialService.class) {
            contadorReferencias--;
            // NO cerrar la cámara realmente para evitar problemas de MSMF
            // Solo decrementar el contador. La cámara se mantendrá abierta.
            if (contadorReferencias < 0) {
                contadorReferencias = 0;
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // CONVERSIÓN DE FRAMES
    // ─────────────────────────────────────────────────────────────

    /** Convierte un Frame de JavaCV a BufferedImage para mostrar en Swing. */
    public BufferedImage frameABufferedImage(Frame frame) {
        if (frame == null) return null;
        try {
            Java2DFrameConverter converter = new Java2DFrameConverter();
            BufferedImage img = converter.convert(frame);
            if (img == null) {
                System.err.println("[ReconocimientoFacial] frameABufferedImage devolvió null");
            }
            return img;
        } catch (Exception e) {
            System.err.println("[ReconocimientoFacial] Error en frameABufferedImage: " + e.getMessage());
            return null;
        }
    }

    /** Convierte un Frame de JavaCV a Mat de OpenCV para procesar. */
    public Mat frameAMat(Frame frame) {
        if (frame == null) return null;
        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
        return converter.convert(frame);
    }

    /** Convierte un Mat de OpenCV a bytes JPEG para almacenar en BD. */
    public byte[] matABytes(Mat mat) {
        if (mat == null || mat.empty()) return null;
        BytePointer bp = new BytePointer();
        imencode(".jpg", mat, bp);
        byte[] bytes = new byte[(int) bp.limit()];
        bp.get(bytes);
        bp.deallocate();
        return bytes;
    }

    /** Convierte bytes JPEG (de BD) a Mat de OpenCV para comparar. */
    public Mat bytesAMat(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        Mat encoded = new Mat(1, bytes.length, CV_8UC1);
        encoded.data().put(bytes);
        return imdecode(encoded, IMREAD_COLOR);
    }

    // ─────────────────────────────────────────────────────────────
    // DETECCIÓN DE ROSTROS
    // ─────────────────────────────────────────────────────────────

    /**
     * Detecta si hay al menos un rostro en el frame actual.
     * @return true si se detectó al menos un rostro
     */
    public boolean hayRostro(Frame frame) {
        if (frame == null || detector == null) return false;
        Mat mat = frameAMat(frame);
        if (mat == null || mat.empty()) return false;
        RectVector rostros = new RectVector();
        Mat gris = new Mat();
        cvtColor(mat, gris, COLOR_BGR2GRAY);
        equalizeHist(gris, gris);
        detector.detectMultiScale(gris, rostros, 1.1, 3, 0, new Size(60, 60), new Size());
        return !rostros.empty();
    }

    /**
     * Detecta rostros y retorna el Mat recortado del primer rostro encontrado.
     * Retorna null si no se detecta ningún rostro.
     */
    public Mat detectarPrimerRostro(Frame frame) {
        if (frame == null || detector == null) return null;
        Mat mat = frameAMat(frame);
        if (mat == null || mat.empty()) return null;

        Mat gris = new Mat();
        cvtColor(mat, gris, COLOR_BGR2GRAY);
        equalizeHist(gris, gris);

        RectVector rostros = new RectVector();
        detector.detectMultiScale(gris, rostros, 1.1, 3, 0,
                new Size(60, 60), new Size());

        if (rostros.empty()) return null;

        // Tomar el rostro más grande (más cercano a la cámara)
        Rect mayor = rostros.get(0);
        for (int i = 1; i < rostros.size(); i++) {
            Rect r = rostros.get(i);
            if (r.width() * r.height() > mayor.width() * mayor.height()) {
                mayor = r;
            }
        }

        // Agregar margen del 20% para incluir frente y mentón
        int margen = (int)(mayor.width() * 0.20);
        int x = Math.max(0, mayor.x() - margen);
        int y = Math.max(0, mayor.y() - margen);
        int w = Math.min(mat.cols() - x, mayor.width()  + margen * 2);
        int h = Math.min(mat.rows() - y, mayor.height() + margen * 2);

        return new Mat(mat, new Rect(x, y, w, h));
    }

    /**
     * Dibuja rectángulos sobre los rostros detectados en el frame.
     * Útil para el preview en tiempo real (feedback visual al usuario).
     * @return Mat con los rectángulos dibujados
     */
    public Mat dibujarRostros(Frame frame) {
        if (frame == null || detector == null) return null;
        try {
            Mat mat = frameAMat(frame);
            if (mat == null || mat.empty()) return null;

            Mat gris = new Mat();
            cvtColor(mat, gris, COLOR_BGR2GRAY);
            equalizeHist(gris, gris);

            RectVector rostros = new RectVector();
            detector.detectMultiScale(gris, rostros, 1.1, 3, 0,
                    new Size(60, 60), new Size());

            for (int i = 0; i < rostros.size(); i++) {
                Rect r = rostros.get(i);
                // Rectángulo rojo UTA
                rectangle(mat,
                    new Point(r.x(), r.y()),
                    new Point(r.x() + r.width(), r.y() + r.height()),
                    new Scalar(0, 0, 180, 255),   // BGR → rojo
                    2, LINE_8, 0);
                // Etiqueta
                putText(mat, "Rostro detectado",
                    new Point(r.x(), r.y() - 8),
                    FONT_HERSHEY_SIMPLEX, 0.55,
                    new Scalar(0, 0, 180, 255), 2, LINE_8, false);
            }
            gris.deallocate();
            return mat;
        } catch (Exception e) {
            System.err.println("[ReconocimientoFacial] Error en dibujarRostros: " + e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // COMPARACIÓN DE ROSTROS
    // ─────────────────────────────────────────────────────────────

    /**
     * Compara dos imágenes de rostros usando distancia L2 mejorada.
     * Usa imágenes más grandes (150x150) para mayor precisión.
     *
     * @param rostroCapturado Mat del rostro capturado por la cámara
     * @param rostroAlmacenado bytes de la foto almacenada en BD
     * @return valor de similitud entre 0.0 (diferente) y 1.0 (idéntico)
     */
    public double calcularSimilitud(Mat rostroCapturado, byte[] rostroAlmacenado) {
        if (rostroCapturado == null || rostroAlmacenado == null) return 0.0;
        try {
            Mat matAlmacenado = bytesAMat(rostroAlmacenado);
            if (matAlmacenado == null || matAlmacenado.empty()) return 0.0;

            // Redimensionar ambas imágenes al mismo tamaño para comparar
            // Usar tamaño mayor (150x150) para mayor precisión
            Mat r1 = new Mat();
            Mat r2 = new Mat();
            resize(rostroCapturado, r1, new Size(150, 150));
            resize(matAlmacenado,   r2, new Size(150, 150));

            // Convertir a escala de grises
            Mat g1 = new Mat();
            Mat g2 = new Mat();
            cvtColor(r1, g1, COLOR_BGR2GRAY);
            cvtColor(r2, g2, COLOR_BGR2GRAY);

            // Ecualizar histograma para normalizar iluminación
            equalizeHist(g1, g1);
            equalizeHist(g2, g2);

            // Distancia L2 entre ambas imágenes
            // Máximo teórico para 150x150 gris: 255 * sqrt(150*150) = 38250
            double distanciaL2 = norm(g1, NORM_L2, g2);
            double distanciaMax = 255.0 * Math.sqrt(150.0 * 150.0);
            double similitud = 1.0 - Math.min(1.0, distanciaL2 / distanciaMax);

            // Limpiar memoria nativa
            r1.deallocate(); r2.deallocate();
            g1.deallocate(); g2.deallocate();
            matAlmacenado.deallocate();
            return Math.max(0.0, similitud);
        } catch (Exception e) {
            System.err.println("[ReconocimientoFacial] Error comparando rostros: " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Resultado del reconocimiento facial.
     */
    public static class ResultadoReconocimiento {
        public final boolean reconocido;
        public final sistema.model.Empleado empleado;   // null si no reconocido
        public final double similitud;                  // 0.0 – 1.0
        public final String mensaje;

        public ResultadoReconocimiento(boolean reconocido,
                                       sistema.model.Empleado empleado,
                                       double similitud,
                                       String mensaje) {
            this.reconocido = reconocido;
            this.empleado   = empleado;
            this.similitud  = similitud;
            this.mensaje    = mensaje;
        }
    }

    /**
     * Intenta reconocer un rostro capturado comparándolo contra
     * todos los empleados con foto registrada en la BD.
     *
     * @param frame       Frame actual de la cámara
     * @param empleados   Lista de empleados con foto (cargada desde BD)
     * @return ResultadoReconocimiento con el empleado identificado o null
     */
    public ResultadoReconocimiento reconocer(Frame frame,
                                             List<sistema.model.Empleado> empleados) {
        if (frame == null || empleados == null || empleados.isEmpty()) {
            return new ResultadoReconocimiento(false, null, 0,
                    "No hay empleados con foto registrada.");
        }

        Mat rostroCapturado = detectarPrimerRostro(frame);
        if (rostroCapturado == null) {
            return new ResultadoReconocimiento(false, null, 0,
                    "No se detectó ningún rostro. Mire directamente a la cámara.");
        }

        double mejorSimilitud = 0;
        sistema.model.Empleado mejorCandidato = null;

        for (sistema.model.Empleado emp : empleados) {
            double sim = calcularSimilitud(rostroCapturado, emp.getEmpFotoRostro());
            if (sim > mejorSimilitud) {
                mejorSimilitud = sim;
                mejorCandidato = emp;
            }
        }

        rostroCapturado.deallocate();

        if (mejorSimilitud >= UMBRAL_SIMILITUD) {
            return new ResultadoReconocimiento(true, mejorCandidato, mejorSimilitud,
                    String.format("✅ Bienvenido, %s (%.0f%% de coincidencia)",
                            mejorCandidato.getNombreCompleto(), mejorSimilitud * 100));
        } else {
            return new ResultadoReconocimiento(false, null, mejorSimilitud,
                    String.format("❌ Rostro no reconocido (%.0f%% — mínimo %.0f%%)",
                            mejorSimilitud * 100, UMBRAL_SIMILITUD * 100));
        }
    }

    /**
     * Captura múltiples frames y promedia la similitud para mayor precisión.
     * Usa 5 intentos con 200ms entre cada uno.
     */
    public ResultadoReconocimiento reconocerConReintentos(FrameGrabber grabber,
                                                          List<sistema.model.Empleado> empleados,
                                                          int intentos) {
        List<Double> similitudes = new ArrayList<>();
        sistema.model.Empleado candidato = null;
        double mejorGlobal = 0;

        for (int i = 0; i < intentos; i++) {
            try {
                Frame frame = grabber.grab();
                ResultadoReconocimiento r = reconocer(frame, empleados);
                similitudes.add(r.similitud);
                if (r.similitud > mejorGlobal) {
                    mejorGlobal = r.similitud;
                    candidato = r.empleado;
                }
                Thread.sleep(200);
            } catch (Exception e) {
                System.err.println("[ReconocimientoFacial] Error en intento " + i + ": " + e.getMessage());
            }
        }

        // Promedio de similitudes
        double promedio = similitudes.stream()
                .mapToDouble(Double::doubleValue).average().orElse(0);

        if (promedio >= UMBRAL_SIMILITUD && candidato != null) {
            return new ResultadoReconocimiento(true, candidato, promedio,
                    String.format("✅ Bienvenido, %s (%.0f%%)",
                            candidato.getNombreCompleto(), promedio * 100));
        } else {
            return new ResultadoReconocimiento(false, null, promedio,
                    String.format("❌ No reconocido (%.0f%%)", promedio * 100));
        }
    }
}
