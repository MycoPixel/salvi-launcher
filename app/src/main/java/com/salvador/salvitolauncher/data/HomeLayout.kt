package com.salvador.salvitolauncher.data

import org.json.JSONArray
import org.json.JSONObject

/** Una celda de la pantalla de inicio: una app suelta o una carpeta creada por el usuario. */
sealed interface LayoutEntry {
    val key: String
}

data class AppEntry(val packageName: String) : LayoutEntry {
    override val key: String get() = "app:$packageName"
}

data class FolderEntry(
    val id: String,
    val name: String,
    val packages: List<String>
) : LayoutEntry {
    override val key: String get() = "folder:$id"
}

/**
 * Operaciones puras sobre el orden del inicio. Todas devuelven una lista nueva y no tocan
 * nada más, así que se pueden razonar (y probar) sin UI.
 */
object HomeLayoutOps {

    /**
     * Ajusta el layout guardado a las apps realmente visibles: quita las que ya no existen
     * (o están en el dock / ocultas) y agrega al final, por orden alfabético, las nuevas.
     *
     * @param visibleApps apps que pueden aparecer en el inicio, ya ordenadas alfabéticamente.
     */
    fun reconcile(saved: List<LayoutEntry>?, visibleApps: List<AppInfo>): List<LayoutEntry> {
        val visible = visibleApps.map { it.packageName }.toSet()
        val placed = HashSet<String>()
        val result = ArrayList<LayoutEntry>()

        for (entry in saved.orEmpty()) {
            when (entry) {
                is AppEntry -> {
                    if (entry.packageName in visible && placed.add(entry.packageName)) result.add(entry)
                }
                is FolderEntry -> {
                    val packages = entry.packages.filter { it in visible && placed.add(it) }
                    if (packages.isNotEmpty()) result.add(entry.copy(packages = packages))
                }
            }
        }
        for (app in visibleApps) {
            if (placed.add(app.packageName)) result.add(AppEntry(app.packageName))
        }
        return result
    }

    /** Mueve una celda para que quede en la posición [toIndex] del resultado. */
    fun move(layout: List<LayoutEntry>, key: String, toIndex: Int): List<LayoutEntry> {
        val item = layout.firstOrNull { it.key == key } ?: return layout
        val rest = layout.filter { it.key != key }
        return rest.toMutableList().apply { add(toIndex.coerceIn(0, size), item) }
    }

    /**
     * Suelta una app sobre otra celda: si el destino es una app se crea una carpeta nueva en su
     * lugar; si es una carpeta, la app entra en ella. Solo se pueden fusionar apps sueltas.
     */
    fun mergeInto(
        layout: List<LayoutEntry>,
        draggedKey: String,
        targetKey: String,
        newFolderName: String,
        newFolderId: String
    ): List<LayoutEntry> {
        if (draggedKey == targetKey) return layout
        val dragged = layout.firstOrNull { it.key == draggedKey } as? AppEntry ?: return layout
        val target = layout.firstOrNull { it.key == targetKey } ?: return layout

        return when (target) {
            is AppEntry -> layout.mapNotNull { entry ->
                when (entry.key) {
                    draggedKey -> null
                    targetKey -> FolderEntry(
                        id = newFolderId,
                        name = newFolderName,
                        packages = listOf(target.packageName, dragged.packageName)
                    )
                    else -> entry
                }
            }
            is FolderEntry -> layout.mapNotNull { entry ->
                when (entry.key) {
                    draggedKey -> null
                    targetKey -> target.copy(packages = target.packages + dragged.packageName)
                    else -> entry
                }
            }
        }
    }

    /** Saca una app de su carpeta y la deja justo después. Si la carpeta queda vacía, desaparece. */
    fun removeFromFolder(layout: List<LayoutEntry>, packageName: String): List<LayoutEntry> {
        val result = ArrayList<LayoutEntry>()
        for (entry in layout) {
            if (entry is FolderEntry && packageName in entry.packages) {
                val rest = entry.packages - packageName
                if (rest.isNotEmpty()) result.add(entry.copy(packages = rest))
                result.add(AppEntry(packageName))
            } else {
                result.add(entry)
            }
        }
        return result
    }

    /** Deshace una carpeta: sus apps quedan sueltas en el mismo lugar. */
    fun dissolve(layout: List<LayoutEntry>, folderId: String): List<LayoutEntry> {
        val result = ArrayList<LayoutEntry>()
        for (entry in layout) {
            if (entry is FolderEntry && entry.id == folderId) {
                entry.packages.forEach { result.add(AppEntry(it)) }
            } else {
                result.add(entry)
            }
        }
        return result
    }

    fun renameFolder(layout: List<LayoutEntry>, folderId: String, name: String): List<LayoutEntry> {
        return layout.map { entry ->
            if (entry is FolderEntry && entry.id == folderId) entry.copy(name = name) else entry
        }
    }

    // ------------------------------------------------------------------ persistencia

    fun toJson(layout: List<LayoutEntry>): String {
        val array = JSONArray()
        for (entry in layout) {
            val obj = JSONObject()
            when (entry) {
                is AppEntry -> {
                    obj.put("t", "app")
                    obj.put("p", entry.packageName)
                }
                is FolderEntry -> {
                    obj.put("t", "folder")
                    obj.put("id", entry.id)
                    obj.put("n", entry.name)
                    obj.put("p", JSONArray(entry.packages))
                }
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun fromJson(raw: String): List<LayoutEntry>? {
        return try {
            val array = JSONArray(raw)
            val result = ArrayList<LayoutEntry>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                when (obj.getString("t")) {
                    "app" -> result.add(AppEntry(obj.getString("p")))
                    "folder" -> {
                        val packages = obj.getJSONArray("p")
                        result.add(
                            FolderEntry(
                                id = obj.getString("id"),
                                name = obj.getString("n"),
                                packages = List(packages.length()) { packages.getString(it) }
                            )
                        )
                    }
                }
            }
            result
        } catch (_: Exception) {
            null
        }
    }
}
