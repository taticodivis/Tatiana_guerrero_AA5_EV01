"use strict";

const form = document.getElementById("acceso");
const clave = document.getElementById("clave");
const confirmacion = document.getElementById("confirmacion");
const enviar = document.getElementById("enviar");
const cambiar = document.getElementById("cambiar");
const resultado = document.getElementById("resultado");
let registro = false;

function modo(nuevo) {
  registro = nuevo;
  form.action = registro ? "api/registro" : "api/login";
  document.getElementById("titulo").textContent = registro ? "Crear cuenta" : "Iniciar sesión";
  document.getElementById("introduccion").textContent = registro ? "Registra tus datos para verificar tu acceso." : "Ingresa tu usuario y contraseña.";
  document.getElementById("confirmacion-grupo").hidden = !registro;
  confirmacion.disabled = !registro;
  confirmacion.required = registro;
  clave.minLength = registro ? 12 : 1;
  clave.autocomplete = registro ? "new-password" : "current-password";
  clave.value = confirmacion.value = "";
  clave.type = "password";
  document.getElementById("mostrar").textContent = "Mostrar";
  document.getElementById("mostrar").setAttribute("aria-pressed", "false");
  enviar.textContent = registro ? "Crear cuenta" : "Entrar";
  cambiar.textContent = registro ? "Iniciar sesión" : "Registrarse";
  document.getElementById("pregunta").textContent = registro ? "¿Ya tienes cuenta?" : "¿No tienes cuenta?";
  resultado.textContent = "";
}

// Los mensajes se muestran como texto, sin interpretar HTML de la respuesta.
async function aviso(ok, titulo, mensaje) {
  resultado.textContent = mensaje;
  resultado.dataset.ok = String(ok);
  if (window.Swal) {
    await Swal.fire({ icon: ok ? "success" : "error", title: titulo, text: mensaje,
      confirmButtonText: "Entendido", confirmButtonColor: "#6549db" });
  }
}

cambiar.addEventListener("click", () => modo(!registro));
document.getElementById("mostrar").addEventListener("click", event => {
  const mostrar = clave.type === "password";
  clave.type = mostrar ? "text" : "password";
  event.currentTarget.textContent = mostrar ? "Ocultar" : "Mostrar";
  event.currentTarget.setAttribute("aria-pressed", String(mostrar));
});

form.addEventListener("submit", async event => {
  event.preventDefault();
  if (!form.reportValidity()) return;
  if (registro && clave.value !== confirmacion.value) {
    await aviso(false, "Revisa la confirmación", "Las contraseñas no coinciden.");
    return;
  }
  const eraRegistro = registro;
  const cuerpo = new URLSearchParams(new FormData(form));
  enviar.disabled = cambiar.disabled = true;
  enviar.textContent = "Procesando…";
  try {
    // El resultado visual depende de la respuesta real de la API.
    const response = await fetch(form.action, { method: "POST", body: cuerpo, signal: AbortSignal.timeout(15000) });
    const datos = await response.json();
    const ok = response.ok && datos.ok === true;
    await aviso(ok, ok ? (eraRegistro ? "Cuenta creada" : "Autenticación satisfactoria") : "No se pudo completar",
      datos.mensaje || "El servicio devolvió una respuesta inesperada.");
    if (ok && eraRegistro) {
      modo(false);
      resultado.textContent = "Tu cuenta fue creada. Ahora puedes iniciar sesión.";
      resultado.dataset.ok = "true";
    }
  } catch (error) {
    await aviso(false, "No se pudo conectar", "No se recibió una respuesta válida. Comprueba que el servidor esté iniciado e inténtalo de nuevo.");
  } finally {
    clave.value = confirmacion.value = "";
    enviar.disabled = cambiar.disabled = false;
    enviar.textContent = registro ? "Crear cuenta" : "Entrar";
  }
});
