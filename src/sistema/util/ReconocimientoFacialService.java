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

public class ReconocimientoFacialService {

    private static final int CAMERA_INDEX = 0;

    public static final double UMBRAL_SIMILITUD = 0.65;

    private static CascadeClassifier detector;
    private static final String CASCADE_RESOURCE =
            "/sistema/util/haarcascade_frontalface_default.xml";

    private static ReconocimientoFacialService instancia;

    private ReconocimientoFacialService() {
        cargarCascade();
    }

    public static ReconocimientoFacialService getInstance() {
        if (instancia == null) instancia = new ReconocimientoFacialService();
        return instancia;
    }

    private void cargarCascade() {
        try {
            InputStream is = getClass().getResourceAsStream(CASCADE_RESOURCE);
            if (is == null) {
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

    public FrameGrabber abrirCamara() throws Exception {
        OpenCVFrameGrabber nuevoGrabber = new OpenCVFrameGrabber(CAMERA_INDEX);
        nuevoGrabber.setFormat("DIB");
        nuevoGrabber.setImageMode(FrameGrabber.ImageMode.COLOR);
        nuevoGrabber.setImageWidth(320);
        nuevoGrabber.setImageHeight(240);
        nuevoGrabber.start();
        return nuevoGrabber;
    }

    public FrameGrabber abrirCamara(int indice) throws Exception {
        if (indice != CAMERA_INDEX) {
            throw new IllegalArgumentException(
                "Solo se soporta la camara integrada (indice " + CAMERA_INDEX + "). " +
                "Recibido: " + indice);
        }
        return abrirCamara();
    }

    public void cerrarCamara(FrameGrabber grabber) {
        if (grabber != null) {
            try {
                grabber.stop();
                grabber.release();
            } catch (Exception e) {
                System.err.println("[ReconocimientoFacial] Error al cerrar camara: " + e.getMessage());
            }
        }
    }

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

    public BufferedImage matABufferedImage(Mat mat) {
        if (mat == null || mat.empty()) return null;
        Frame frame = null;
        try {
            OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
            frame = converterToMat.convert(mat);
            Java2DFrameConverter converterToImage = new Java2DFrameConverter();
            BufferedImage img = converterToImage.convert(frame);
            if (img == null) {
                System.err.println("[ReconocimientoFacial] matABufferedImage devolvió null");
            }
            return img;
        } catch (Exception e) {
            System.err.println("[ReconocimientoFacial] Error en matABufferedImage: " + e.getMessage());
            return null;
        } finally {
            if (frame != null) {
                try { frame.close(); } catch (Exception e) { }
            }
        }
    }

    public Mat frameAMat(Frame frame) {
        if (frame == null) return null;
        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
        return converter.convert(frame);
    }

    public byte[] matABytes(Mat mat) {
        if (mat == null || mat.empty()) return null;
        BytePointer bp = new BytePointer();
        imencode(".jpg", mat, bp);
        byte[] bytes = new byte[(int) bp.limit()];
        bp.get(bytes);
        bp.deallocate();
        return bytes;
    }

    public Mat bytesAMat(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        Mat encoded = new Mat(1, bytes.length, CV_8UC1);
        encoded.data().put(bytes);
        return imdecode(encoded, IMREAD_COLOR);
    }

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

        Rect mayor = rostros.get(0);
        for (int i = 1; i < rostros.size(); i++) {
            Rect r = rostros.get(i);
            if (r.width() * r.height() > mayor.width() * mayor.height()) {
                mayor = r;
            }
        }

        int margen = (int)(mayor.width() * 0.20);
        int x = Math.max(0, mayor.x() - margen);
        int y = Math.max(0, mayor.y() - margen);
        int w = Math.min(mat.cols() - x, mayor.width()  + margen * 2);
        int h = Math.min(mat.rows() - y, mayor.height() + margen * 2);

        return new Mat(mat, new Rect(x, y, w, h));
    }

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
                rectangle(mat,
                    new Point(r.x(), r.y()),
                    new Point(r.x() + r.width(), r.y() + r.height()),
                    new Scalar(0, 0, 180, 255),
                    2, LINE_8, 0);
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

    public double calcularSimilitud(Mat rostroCapturado, byte[] rostroAlmacenado) {
        if (rostroCapturado == null || rostroAlmacenado == null) return 0.0;
        try {
            Mat matAlmacenado = bytesAMat(rostroAlmacenado);
            if (matAlmacenado == null || matAlmacenado.empty()) return 0.0;

            Mat r1 = new Mat();
            Mat r2 = new Mat();
            resize(rostroCapturado, r1, new Size(100, 100));
            resize(matAlmacenado,   r2, new Size(100, 100));

            Mat g1 = new Mat();
            Mat g2 = new Mat();
            cvtColor(r1, g1, COLOR_BGR2GRAY);
            cvtColor(r2, g2, COLOR_BGR2GRAY);

            equalizeHist(g1, g1);
            equalizeHist(g2, g2);

            GaussianBlur(g1, g1, new Size(3, 3), 0);
            GaussianBlur(g2, g2, new Size(3, 3), 0);

            double distanciaL2 = norm(g1, NORM_L2, g2);
            double distanciaMax = 255.0 * Math.sqrt(100.0 * 100.0);
            double similitudL2 = 1.0 - Math.min(1.0, distanciaL2 / distanciaMax);

            Mat resultadoCorrelacion = new Mat();
            matchTemplate(g1, g2, resultadoCorrelacion, TM_CCOEFF_NORMED);
            double correlacion = resultadoCorrelacion.ptr(0).getFloat(0);
            correlacion = Math.max(0, correlacion);
            resultadoCorrelacion.deallocate();

            double similitud = 0.4 * similitudL2 + 0.6 * correlacion;

            r1.deallocate(); r2.deallocate();
            g1.deallocate(); g2.deallocate();
            matAlmacenado.deallocate();
            return Math.max(0.0, Math.min(1.0, similitud));
        } catch (Exception e) {
            System.err.println("[ReconocimientoFacial] Error comparando rostros: " + e.getMessage());
            return 0.0;
        }
    }

    public static class ResultadoReconocimiento {
        public final boolean reconocido;
        public final sistema.model.Empleado empleado;
        public final double similitud;
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
                    String.format("Bienvenido, %s (%.0f%% de coincidencia)",
                            mejorCandidato.getNombreCompleto(), mejorSimilitud * 100));
        } else {
            return new ResultadoReconocimiento(false, null, mejorSimilitud,
                    String.format("Rostro no reconocido (%.0f%% minimo %.0f%%)",
                            mejorSimilitud * 100, UMBRAL_SIMILITUD * 100));
        }
    }

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

        double promedio = similitudes.stream()
                .mapToDouble(Double::doubleValue).average().orElse(0);

        if (promedio >= UMBRAL_SIMILITUD && candidato != null) {
            return new ResultadoReconocimiento(true, candidato, promedio,
                    String.format("Bienvenido, %s (%.0f%%)",
                            candidato.getNombreCompleto(), promedio * 100));
        } else {
            return new ResultadoReconocimiento(false, null, promedio,
                    String.format("No reconocido (%.0f%%)", promedio * 100));
        }
    }
}