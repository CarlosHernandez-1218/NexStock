// NexStock - HU10 MOV-70
// Todos los días a las 8:00 (hora de Lima) revisa Firestore y, si hay stock crítico,
// productos agotados u órdenes vencidas, envía un push (FCM) a los Administradores.

const { onSchedule } = require("firebase-functions/v2/scheduler");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
Object.assign(exports, require("./devoluciones"));   // HU09: push de devolución autorizada
const db = admin.firestore();

const ADMINS_TOPIC = "admins";          // la app suscribe aquí a los Administradores
const CHANNEL_ID = "logistic_alerts";   // canal de notificación creado por la app
const ESTADOS_FINALES = ["RECIBIDA", "CANCELADA"];

// Hoy (fecha de Lima) como medianoche UTC, igual que guarda la app las fechas de entrega
function hoyUtcMillis() {
  const fecha = new Date().toLocaleDateString("en-CA", { timeZone: "America/Lima" }); // AAAA-MM-DD
  return Date.parse(`${fecha}T00:00:00Z`);
}

async function calcularAlertas() {
  const [productos, ordenes] = await Promise.all([
    db.collection("products").where("status", "==", "ACTIVO").get(),
    db.collection("ordenes_compra").get(),
  ]);

  let agotados = 0;
  let criticos = 0;
  productos.forEach((doc) => {
    const { stock = 0, minStock = 0 } = doc.data();
    if (stock <= 0) agotados++;
    else if (stock <= minStock) criticos++;
  });

  const hoy = hoyUtcMillis();
  let vencidas = 0;
  ordenes.forEach((doc) => {
    const { status, deliveryDate = 0 } = doc.data();
    if (!ESTADOS_FINALES.includes(status) && deliveryDate > 0 && deliveryDate < hoy) vencidas++;
  });

  return { criticos, agotados, vencidas };
}

function plural(n, uno, varios) {
  return `${n} ${n === 1 ? uno : varios}`;
}

exports.alertasLogisticas = onSchedule(
  { schedule: "0 8 * * *", timeZone: "America/Lima", region: "us-central1" },
  async () => {
    const { criticos, agotados, vencidas } = await calcularAlertas();
    logger.info("Revisión logística", { criticos, agotados, vencidas });

    if (criticos === 0 && agotados === 0 && vencidas === 0) return; // nada que avisar

    const partes = [];
    if (criticos > 0) partes.push(plural(criticos, "producto en stock crítico", "productos en stock crítico"));
    if (agotados > 0) partes.push(plural(agotados, "agotado", "agotados"));
    if (vencidas > 0) partes.push(plural(vencidas, "orden vencida", "órdenes vencidas"));

    await admin.messaging().send({
      topic: ADMINS_TOPIC,
      notification: {
        title: "NexStock: revisión logística del día",
        body: partes.join(" · "),
      },
      android: {
        priority: "high",
        notification: { channelId: CHANNEL_ID },
      },
    });
    logger.info("Push enviado al tema admins", { body: partes.join(" · ") });
  }
);
