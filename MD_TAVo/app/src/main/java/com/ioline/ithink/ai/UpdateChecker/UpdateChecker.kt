package com.ioline.ithink.ai.UpdateChecker

import android.content.Context
import android.util.Log



class UpdateChecker(private val context: Context) {

    // Função de verificação de atualizações, agora com Result para um manuseio mais limpo
    suspend fun checkForUpdate(currentVersionCode: Int): Result<UpdateResult> {
        return try {
            // Realiza a solicitação ao servidor para verificar atualização
            val result = requestServer.checkUpdate(currentVersionCode)

            when (result) {
                is UpdateResult.UpdateAvailable -> {
                    Log.d("UpdateChecker", "Nova versão disponível: ${result.versionCode}")
                    Log.d("UpdateChecker","APK URL: ${result.url}")
                    // Retorna o sucesso com o resultado de atualização disponível
                    Result.success(result)
                }

                is UpdateResult.AlreadyUpToDate -> {
                    Log.d("UpdateChecker","App já está atualizada.")
                    // Retorna sucesso com a informação de que a app já está atualizada
                    Result.success(result)
                }

                is UpdateResult.Error -> {
                    Log.e("UpdateChecker","Erro ao verificar atualização.")
                    // Retorna falha com a informação de erro
                    Result.failure(Exception("Erro ao verificar atualização"))
                }
            }
        } catch (e: Exception) {
            Log.e("UpdateChecker","Exceção: ${e.message}")
            // Retorna falha em caso de exceção
            Result.failure(e)
        }
    }
}
