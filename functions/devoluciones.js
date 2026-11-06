// NexStock - HU09 MOV-60
// Cuando una devolución pasa a AUTORIZADA, avisa por push (FCM) a los Administradores.

const { onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");

const ADMINS_TOPIC = "admins";          // la app suscribe aquí a los Administradores
const CHANNEL_ID = "logistic_alerts";   // canal de notificación creado por la app

exports.devolucionAutorizada = onDocumentUpdated(
  // La región debe coincidir con la ubicación de tu base de Firestore
  { document: "devoluciones/{devolucionId}", region: "us-central1" },
  async (event) => {
    const antes = event.data.before.data();
    const despues = event.data.after.data();
    if (!antes || !despues) return;

    // Solo cuando el estado cambia a AUTORIZADA (no en otras ediciones del documento)
    if (antes.status === despues.status || despues.status !== "AUTORIZADA") return;

    const cuerpo =
      `${despues.productName} · ${despues.quantity} ${despues.unit} — ${despues.supplierName}. ` +
      `Autorizó: ${despues.authorizedByName || "Jefe de Línea"}`;

    await admin.messaging().send({
      topic: ADMINS_TOPIC,
      notification: { title: "Devolución autorizada", body: cuerpo },
      android: { priority: "high", notification: { channelId: CHANNEL_ID } },
    });
    logger.info("Push de devolución autorizada enviado", { id: event.params.devolucionId });
  }
);