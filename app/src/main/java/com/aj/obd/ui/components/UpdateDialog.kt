package com.aj.obd.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aj.obd.R
import com.aj.obd.core.update.AppUpdateInfo
import com.aj.obd.ui.theme.AmberGlow
import com.aj.obd.ui.theme.ElectricCyan
import com.aj.obd.ui.theme.RacingRed
import com.aj.obd.ui.theme.SurfaceDark
import com.aj.obd.ui.theme.SurfaceVariantDark
import com.aj.obd.ui.theme.TelemetryGreen
import com.aj.obd.ui.theme.TextPrimary
import com.aj.obd.ui.theme.TextSecondary
import java.io.File

@Composable
fun UpdateDialog(
    updateInfo: AppUpdateInfo,
    isDownloading: Boolean = false,
    downloadProgress: Float = 0f,
    downloadProgressText: String = "",
    downloadedFile: File? = null,
    updateError: String? = null,
    canInstall: Boolean = true,
    onDismiss: () -> Unit,
    onDownload: (String) -> Unit,
    onInstall: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenInBrowser: (String) -> Unit = {}
) {
    Dialog(onDismissRequest = { if (!isDownloading) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .border(1.5.dp, AmberGlow.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logotipo Oficial do OBD AJ
                Image(
                    painter = painterResource(id = R.drawable.logo_obdaj),
                    contentDescription = "Logo OBD AJ",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (updateInfo.hasUpdate) "ATUALIZAÇÃO OFICIAL DISPONÍVEL" else "VERSÃO MAIS RECENTE",
                    color = if (updateInfo.hasUpdate) AmberGlow else TelemetryGreen,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Versão Atual vs Nova Versão
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(SurfaceVariantDark, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Atual: v${updateInfo.currentVersion}",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(text = "➔", color = AmberGlow, fontSize = 14.sp)

                    Box(
                        modifier = Modifier
                            .background(TelemetryGreen.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .border(1.dp, TelemetryGreen, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Nova: v${updateInfo.latestVersion}",
                            color = TelemetryGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // =========================================================================
                // ESTADO 1: DOWNLOAD EM ANDAMENTO (BARRA DE PROGRESSO NO APP)
                // =========================================================================
                if (isDownloading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceVariantDark, RoundedCornerShape(10.dp))
                            .border(1.dp, AmberGlow.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = AmberGlow,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BAIXANDO NO APP...",
                                color = AmberGlow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = AmberGlow,
                            trackColor = Color.Black.copy(alpha = 0.5f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = downloadProgressText.ifBlank { "Baixando pacote APK oficial..." },
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Por favor, aguarde o término sem fechar o app.",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
                // =========================================================================
                // ESTADO 2: DOWNLOAD CONCLUÍDO (PRONTO PARA INSTALAR)
                // =========================================================================
                else if (downloadedFile != null && downloadedFile.exists()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceVariantDark, RoundedCornerShape(10.dp))
                            .border(1.dp, TelemetryGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = TelemetryGreen,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "DOWNLOAD CONCLUÍDO (100%)",
                            color = TelemetryGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (canInstall)
                                "O arquivo está pronto. Toque no botão abaixo para abrir o instalador nativo do Android."
                            else
                                "O Android requer autorização para instalar apps deste aplicativo. Toque para conceder e instalar.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (canInstall) {
                            Button(
                                onClick = onInstall,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TelemetryGreen,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "INSTALAR ATUALIZAÇÃO AGORA",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            Button(
                                onClick = onRequestPermission,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AmberGlow,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AUTORIZAR E INSTALAR",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                // =========================================================================
                // ESTADO 3: ERRO NO DOWNLOAD
                // =========================================================================
                else if (updateError != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceVariantDark, RoundedCornerShape(10.dp))
                            .border(1.dp, RacingRed.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = RacingRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "FALHA NO DOWNLOAD",
                                color = RacingRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = updateError,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onDownload(updateInfo.downloadUrl) },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberGlow, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "TENTAR NOVAMENTE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
                // =========================================================================
                // ESTADO 4: INICIAL - PRONTO PARA BAIXAR
                // =========================================================================
                else {
                    // Notas da versão / Changelog
                    Text(
                        text = "Novidades desta versão:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                            .background(SurfaceVariantDark, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = updateInfo.releaseNotes.ifBlank { "• Correção da leitura de sensores em adaptadores ELM327\n• Perfil universal OBD-II para todos os veículos\n• Leitura e limpeza universal de falhas (DTC)" },
                            color = TextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botão Principal: ATUALIZAR DIRETO NO APP
                    Button(
                        onClick = { onDownload(updateInfo.downloadUrl) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberGlow,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (updateInfo.hasUpdate) "ATUALIZAR AGORA (NO APP)" else "BAIXAR / REINSTALAR NO APP",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Lembrar Mais Tarde / Fechar",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
