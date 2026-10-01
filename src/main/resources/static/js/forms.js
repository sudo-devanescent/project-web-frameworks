/* ============================================================
   MÓDULO: FORMULARIO DE RESERVA
   Valida y procesa el envío del formulario "Nueva reserva".
   La validación usa el mecanismo nativo de Bootstrap
   (clase was-validated) y el guardado se simula sin conexión
   a un backend o base de datos real.
   ============================================================= */

/**
 * Inicializa el manejador de envío del formulario de reserva.
 * Expone la función ocultarAlertas en el ámbito global porque el
 * botón "Cancelar" la invoca desde su atributo onclick en el HTML.
 */
export function initForms() {
    document.getElementById('form-reserva').addEventListener('submit', function (evento) {
        // Evita que la página se recargue al enviar el formulario
        evento.preventDefault();

        // Oculta posibles alertas de intentos anteriores
        ocultarAlertas();

        // Si hay campos obligatorios sin completar, muestra el error
        if (!this.checkValidity()) {
            evento.stopPropagation();
            this.classList.add('was-validated');
            document.getElementById('alerta-error').classList.remove('d-none');
            return;
        }

        // Simulación de guardado exitoso: muestra el mensaje de éxito,
        // limpia la validación y resetea el formulario
        document.getElementById('alerta-exito').classList.remove('d-none');
        this.classList.remove('was-validated');
        this.reset();
    });

    // Expone la función en el ámbito global para el atributo onclick
    window.ocultarAlertas = ocultarAlertas;
}

/**
 * Oculta las alertas de éxito y de error del formulario de reserva.
 * Se utiliza al enviar el formulario y al cancelar la operación.
 */
function ocultarAlertas() {
    document.getElementById('alerta-exito').classList.add('d-none');
    document.getElementById('alerta-error').classList.add('d-none');
}
