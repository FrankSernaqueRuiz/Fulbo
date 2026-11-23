package pe.fulbo.app.feature.auth.data

import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import pe.fulbo.app.core.firebase.Colecciones
import pe.fulbo.app.core.firebase.Funciones
import pe.fulbo.app.core.model.PerfilUsuario
import pe.fulbo.app.core.model.ResenaJugador
import pe.fulbo.app.core.util.Resultado
import pe.fulbo.app.core.util.llamadaSegura
import pe.fulbo.app.feature.auth.domain.AuthRepository
import pe.fulbo.app.feature.auth.domain.DatosCompletarPerfil

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
) : AuthRepository {

    override val usuarioActualUid: String?
        get() = auth.currentUser?.uid

    override val estaAutenticado: Boolean
        get() = auth.currentUser != null

    override val authStateChanged: Flow<String?> =
        auth.authStateChanged.map { it?.uid }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observarMiPerfil(): Flow<PerfilUsuario?> {
        return authStateChanged.flatMapLatest { uid ->
            if (uid == null) {
                flowOf(null)
            } else {
                firestore.collection(Colecciones.USUARIOS).document(uid)
                    .snapshots
                    .map { doc ->
                        if (!doc.exists) {
                            null
                        } else {
                            try {
                                val nom = doc.get<String?>("nombre") ?: ""
                                val ape = doc.get<String?>("apellido") ?: ""
                                val ali = doc.get<String?>("alias") ?: nom
                                val ciu = doc.get<String?>("ciudad") ?: "Lima"
                                val pos = doc.get<String?>("posicion") ?: "medio"
                                val niv = doc.get<String?>("nivel") ?: "intermedio"
                                val repG = try { doc.get<Double?>("rep_global") } catch (_: Exception) { null }
                                val repP = try { doc.get<Double?>("rep_puntualidad")?.toFloat() } catch (_: Exception) { null }
                                val repA = try { doc.get<Double?>("rep_asistencia")?.toFloat() } catch (_: Exception) { null }
                                val repPag = try { doc.get<Double?>("rep_pago")?.toFloat() } catch (_: Exception) { null }
                                val repT = try { doc.get<Double?>("rep_trato")?.toFloat() } catch (_: Exception) { null }
                                val numC = try { doc.get<Int?>("num_calificaciones") ?: 0 } catch (_: Exception) { 0 }
                                val partJ = try { doc.get<Int?>("partidos_jugados") ?: 0 } catch (_: Exception) { 0 }
                                val fal = try { doc.get<Int?>("faltas") ?: 0 } catch (_: Exception) { 0 }
                                val pla = try { doc.get<String?>("plan") ?: "gratuito" } catch (_: Exception) { "gratuito" }
                                val fot = try { doc.get<String?>("foto_url") } catch (_: Exception) { null }

                                PerfilUsuario(
                                    uid = doc.id,
                                    nombre = nom,
                                    apellido = ape,
                                    alias = ali,
                                    ciudad = ciu,
                                    posicion = pos,
                                    nivel = niv,
                                    repGlobal = repG,
                                    repPuntualidad = repP,
                                    repAsistencia = repA,
                                    repPago = repPag,
                                    repTrato = repT,
                                    numCalificaciones = numC,
                                    partidosJugados = partJ,
                                    faltas = fal,
                                    plan = pla,
                                    fotoUrl = fot,
                                )
                            } catch (_: Exception) {
                                null
                            }
                        }
                    }
                    .catch { emit(null) }
            }
        }
    }

    override fun observarMisResenas(): Flow<List<ResenaJugador>> {
        return auth.authStateChanged.flatMapLatest { user ->
            val uid = user?.uid
            if (uid == null) {
                flowOf(emptyList())
            } else {
                try {
                    firestore.collectionGroup(Colecciones.CALIFICACIONES)
                        .snapshots
                        .map { snapshot ->
                            snapshot.documents.mapNotNull { doc ->
                                try {
                                    val receptor = doc.get<String?>("receptor_uid")
                                    if (receptor != uid) return@mapNotNull null
                                    val com = doc.get<String?>("comentario")
                                    if (com.isNullOrBlank()) return@mapNotNull null
                                    val emisor = try { doc.get<String?>("emisor_nombre") } catch (_: Exception) { null } ?: "Compañero de partido"
                                    ResenaJugador(
                                        id = doc.id,
                                        texto = com,
                                        autor = emisor,
                                    )
                                } catch (_: Exception) {
                                    null
                                }
                            }
                        }
                        .catch { emit(emptyList()) }
                } catch (_: Exception) {
                    flowOf(emptyList())
                }
            }
        }
    }

    override suspend fun iniciarSesion(correo: String, contrasena: String): Resultado<Unit> =
        llamadaSegura {
            auth.signInWithEmailAndPassword(correo.trim(), contrasena)
            Unit
        }

    override suspend fun crearCuenta(correo: String, contrasena: String): Resultado<String> =
        llamadaSegura {
            val result = auth.createUserWithEmailAndPassword(correo.trim(), contrasena)
            result.user?.uid ?: throw IllegalStateException("No se pudo obtener el UID del usuario creado")
        }

    override suspend fun completarPerfil(datos: DatosCompletarPerfil): Resultado<Unit> =
        llamadaSegura {
            val uid = auth.currentUser?.uid ?: throw IllegalStateException("No hay sesión activa para completar perfil")

            // 1. Guardar directamente en Firestore en usuarios/{uid}
            val refPerfil = firestore.collection(Colecciones.USUARIOS).document(uid)
            refPerfil.set(
                mapOf(
                    "nombre" to datos.nombre,
                    "apellido" to datos.apellido,
                    "alias" to datos.alias,
                    "ciudad" to datos.ciudad,
                    "posicion" to datos.posicion,
                    "nivel" to datos.nivel,
                    "ubicacion" to null,
                    "foto_url" to null,
                    "rep_global" to null,
                    "rep_puntualidad" to null,
                    "rep_asistencia" to null,
                    "rep_pago" to null,
                    "rep_trato" to null,
                    "num_calificaciones" to 0,
                    "partidos_jugados" to 0,
                    "faltas" to 0,
                    "plan" to "gratuito",
                    "plan_vence_en" to null,
                    "creado_en" to FieldValue.serverTimestamp,
                ),
                merge = true,
            )

            // Guardar privado/datos
            refPerfil.collection(Colecciones.PRIVADO).document("datos").set(
                mapOf(
                    "fecha_nacimiento" to datos.fechaNacimiento,
                    "acepto_terminos_en" to FieldValue.serverTimestamp,
                ),
                merge = true,
            )

            // 2. Si la Cloud Function estuviera desplegada se llama de fondo, pero no bloquea si da 404
            try {
                functions.httpsCallable(Funciones.COMPLETAR_PERFIL).invoke(datos)
            } catch (_: Exception) {
                // Función aún no desplegada en Firebase, perfil guardado con éxito directamente en Firestore
            }
            Unit
        }

    override suspend fun cerrarSesion(): Resultado<Unit> =
        llamadaSegura {
            auth.signOut()
        }

    override suspend fun eliminarCuenta(): Resultado<Unit> =
        llamadaSegura {
            try {
                functions.httpsCallable(Funciones.BORRAR_CUENTA).invoke()
            } catch (_: Exception) {
                // Fallback si la función no está desplegada
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    firestore.collection(Colecciones.USUARIOS).document(uid).delete()
                }
            }
            auth.signOut()
        }
}
