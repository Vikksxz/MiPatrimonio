package com.mipatrimonio.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Clase Application de la app. Punto de entrada de Hilt.
 * Los canales de notificación se crearán aquí (T3.6).
 */
@HiltAndroidApp
class MiPatrimonioApp : Application()
