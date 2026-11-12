package co.edu.uniquindio.sga.application.apartamento;

import co.edu.uniquindio.sga.domain.apartamento.AlmacenImagenes;
import org.springframework.stereotype.Service;

/**
 * Sube una imagen al almacén configurado y devuelve su URL. La imagen se asocia al
 * apartamento después, al guardar su galería; si el almacén no responde, el administrador
 * puede registrar una URL existente.
 */
@Service
public class ImagenServicio {

    private final AlmacenImagenes almacen;

    public ImagenServicio(AlmacenImagenes almacen) {
        this.almacen = almacen;
    }

    public String subir(String nombre, String tipoContenido, byte[] contenido) {
        return almacen.guardar(new AlmacenImagenes.Archivo(nombre, tipoContenido, contenido));
    }
}
