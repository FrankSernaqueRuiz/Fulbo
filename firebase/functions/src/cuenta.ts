// Borrar la cuenta desde la app: Apple lo exige a toda app que permita crear
// cuentas, y la Ley 29733 da al usuario el derecho de cancelacion de sus datos.
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { getAuth } from "firebase-admin/auth";
import { db, exigirSesion, refUsuario } from "./comun";

export const borrarCuenta = onCall(async (req) => {
  const uid = exigirSesion(req);

  // FUN-06: Exigir login reciente (menos de 5 minutos)
  const authTime = (req.auth?.token.auth_time ?? 0) * 1000;
  const cincoMinutosMs = 5 * 60 * 1000;
  if (Date.now() - authTime > cincoMinutosMs) {
    throw new HttpsError(
      "unauthenticated",
      "Por seguridad, debes volver a iniciar sesión antes de borrar tu cuenta."
    );
  }

  const perfil = refUsuario(uid);

  // FUN-05: Borrar o anonimizar datos relacionados
  // 1. Borrar si era dueño de cancha o suscripciones
  await db.doc(`duenos_cancha/${uid}`).delete().catch(() => {});
  await db.doc(`suscripciones/${uid}`).delete().catch(() => {});

  // 2. Anonimizar calificaciones emitidas (reemplazar emisor_uid por 'anonimo')
  const califsSnap = await db.collectionGroup("calificaciones").where("emisor_uid", "==", uid).get();
  if (!califsSnap.empty) {
    const batch = db.batch();
    califsSnap.forEach((doc) => {
      batch.update(doc.ref, { emisor_uid: "anonimo" });
    });
    await batch.commit();
  }

  // 3. Borrar perfil y subcolecciones y reputacion
  await db.recursiveDelete(perfil);
  await db.doc(`reputacion/${uid}`).delete().catch(() => {});

  // 4. Borrar usuario en Firebase Auth
  await getAuth().deleteUser(uid);
  return { ok: true };
});
