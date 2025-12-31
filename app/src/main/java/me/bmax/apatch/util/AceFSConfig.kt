package me.bmax.apatch.util

import android.content.Context
import android.os.Build
import android.util.Log
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.Natives
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object AceFSConfig {
    private const val TAG = "AceFSConfig"
    const val CONFIG_PATH = "/data/adb/fp/AceFS.json"
    const val ACEFS_BINARY_PATH = "/data/adb/fp/AceFS-C"
    // const val SERVICE_PATH = "/data/adb/service.d/acefs.sh"

    suspend fun isAceFSKpmLoaded(): Boolean {
        return withContext(Dispatchers.IO) {
            val names = Natives.kernelPatchModuleList()
            if (Natives.kernelPatchModuleNum() <= 0) return@withContext false
            val nameList = names.split('\n').toList()
            nameList.contains("AceFS")
        }
    }

    suspend fun isAceFSRunning(): Boolean {
        return withContext(Dispatchers.IO) {
            val result = rootShellForResult("pidof AceFS-C")
            result.isSuccess && result.out.isNotEmpty()
        }
    }

    suspend fun getAceFSKPMInfo(): KPMInfo? {
        return withContext(Dispatchers.IO) {
            if (!isAceFSKpmLoaded()) return@withContext null
            
            try {
                val infoline = Natives.kernelPatchModuleInfo("AceFS")
                val spi = infoline.split('\n')
                val name = spi.find { it.startsWith("name=") }?.removePrefix("name=")
                val version = spi.find { it.startsWith("version=") }?.removePrefix("version=")
                val license = spi.find { it.startsWith("license=") }?.removePrefix("license=")
                val author = spi.find { it.startsWith("author=") }?.removePrefix("author=")
                val description = spi.find { it.startsWith("description=") }?.removePrefix("description=")
                val args = spi.find { it.startsWith("args=") }?.removePrefix("args=")
                
                KPMInfo(
                    version ?: "",
                    author ?: "",
                    description ?: "",
                    license ?: ""
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get AceFS KPM info", e)
                null
            }
        }
    }

    suspend fun getHideServiceStatus(): Boolean {
        return withContext(Dispatchers.IO) {
            val config = readConfig()
            config.enableHide
        }
    }

    data class KPMInfo(
        val version: String,
        val author: String,
        val description: String,
        val license: String
    )

    suspend fun installAceFS(context: Context) {
        withContext(Dispatchers.IO) {
            try {
      
                val assetManager = context.assets
                val cacheFile = File(context.cacheDir, "AceFS-C")
        
                try {
                    assetManager.open("AceFS-C").use { input ->
                        cacheFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "AceFS-C asset not found", e)
                    return@withContext
                }

                val shell = getRootShell()

                shell.newJob().add(
                    "mkdir -p /data/adb/fp",
                    "cp ${cacheFile.absolutePath} $ACEFS_BINARY_PATH",
                    "chmod 755 $ACEFS_BINARY_PATH",
                    "rm ${cacheFile.absolutePath}"
                ).exec()


                shell.newJob().add("nohup $ACEFS_BINARY_PATH >/dev/null 2>&1 &").exec()
                
                Log.i(TAG, "AceFS installed and started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to install AceFS", e)
            }
        }
    }

    data class ZygiskGroup(
        val pkgNames: List<String> = emptyList(),
        val paths: List<String> = emptyList()
    )

    data class Config(
        var umountPaths: List<String> = emptyList(),
        var hiddenPaths: List<String> = emptyList(),
        var fakeKernelVersion: String = "",
        var fakeBuildTime: String = "",
        var enableSuManage: Boolean = false,
        var enableHide: Boolean = false,
        var zygiskGroups: List<ZygiskGroup> = emptyList()
    )

    suspend fun readConfig(): Config {
        return withContext(Dispatchers.IO) {
            Natives.su()
            val file = File(CONFIG_PATH)
            if (!file.exists()) return@withContext Config()

            try {
                val content = file.readText()
                val json = JSONObject(content)
                
                // Try to read new group format first
                val groups = json.optJSONArray("umount_paths_zygisk_groups")?.let { arr ->
                    val list = mutableListOf<ZygiskGroup>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val packages = obj.optJSONArray("packages")?.let { pArr ->
                            List(pArr.length()) { pArr.getString(it) }
                        } ?: emptyList()
                        val paths = obj.optJSONArray("paths")?.let { pArr ->
                            List(pArr.length()) { pArr.getString(it) }
                        } ?: emptyList()
                        list.add(ZygiskGroup(packages, paths))
                    }
                    list
                } ?: emptyList()

                Config(
                    umountPaths = json.optJSONArray("umount_paths")?.let { arr ->
                        List(arr.length()) { arr.getString(it) }
                    } ?: emptyList(),
                    hiddenPaths = json.optJSONArray("hide_paths")?.let { arr ->
                        List(arr.length()) { arr.getString(it) }
                    } ?: emptyList(),
                    fakeKernelVersion = json.optString("fake_kernel_version", ""),
                    fakeBuildTime = json.optString("fake_kernel_build_time", ""),
                    enableSuManage = json.optInt("enable_folk_su_manage", 0) == 1,
                    enableHide = json.optInt("enable_hide", 0) == 1,
                    zygiskGroups = groups
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse config", e)
                Config()
            }
        }
    }

    suspend fun saveConfig(config: Config) {
        withContext(Dispatchers.IO) {
            Natives.su()
            val file = File(CONFIG_PATH)
            
            try {
                val json = JSONObject()
                
                // Preserve existing fields if file exists
                if (file.exists()) {
                    try {
                        val existing = JSONObject(file.readText())
                        val keys = existing.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            json.put(key, existing.get(key))
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                json.put("umount_paths", JSONArray(config.umountPaths))
                json.put("hide_paths", JSONArray(config.hiddenPaths))
                json.put("fake_kernel_version", config.fakeKernelVersion)
                json.put("fake_kernel_build_time", config.fakeBuildTime)
                json.put("enable_folk_su_manage", if (config.enableSuManage) 1 else 0)
                json.put("enable_hide", if (config.enableHide) 1 else 0)

                json.remove("umount_paths_zygisk_groups")
                json.remove("umount_paths_zygisk")

                if (!file.parentFile?.exists()!!) {
                    file.parentFile?.mkdirs()
                }
                file.writeText(json.toString(4).replace("\\/", "/"))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save config", e)
            }
        }
    }
}