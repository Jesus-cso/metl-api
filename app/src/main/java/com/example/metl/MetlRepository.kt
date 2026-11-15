package com.example.metl
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MetlViewModel : ViewModel() {

    private val repo get() = MetlGraph.repo

    val loginLoading = MutableLiveData(false)
    val loginError = MutableLiveData<String?>(null)
    val loginOk = MutableLiveData(false)
    val loginRol = MutableLiveData<Rol?>(null)
    val productos = MutableLiveData<List<Producto>>(emptyList())

    val registroLoading = MutableLiveData(false)
    val registroError = MutableLiveData<String?>(null)
    val registroRol = MutableLiveData<Rol?>(null)

    val resumen = MutableLiveData<Resumen?>(null)
    val magueyes = MutableLiveData<List<Maguey>>(emptyList())
    val lote = MutableLiveData<Lote?>(null)
    val seleccionado = MutableLiveData<Maguey?>(null)

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            loginError.value = "Escribe tu correo y contraseña"
            return
        }
        viewModelScope.launch {
            loginLoading.value = true
            loginError.value = null
            repo.login(email, pass)
                .onSuccess { rol ->
                    loginRol.value = rol
                    loginOk.value = true
                }
                .onFailure { loginError.value = it.message ?: "No se pudo iniciar sesión" }
            loginLoading.value = false
        }
    }

    fun cargar() {
        viewModelScope.launch {
            resumen.value = repo.resumen()
            magueyes.value = repo.magueyes()
            lote.value = repo.lote()
            if (seleccionado.value == null) {
                seleccionado.value = magueyes.value.orEmpty().firstOrNull()
            }
        }
    }

    fun registrar(nombre: String, email: String, pass: String, rol: Rol) {
        viewModelScope.launch {
            registroLoading.value = true
            registroError.value = null
            repo.registrar(nombre, email, pass, rol)
                .onSuccess { registroRol.value = it }
                .onFailure { registroError.value = it.message ?: "No se pudo crear la cuenta" }
            registroLoading.value = false
        }
    }

    fun cargarProductos() {
        viewModelScope.launch { productos.value = repo.productos() }
    }

    fun seleccionar(id: String) {
        seleccionado.value = magueyes.value.orEmpty().firstOrNull { it.id == id }
    }
}