package com.aj.obd.ui.components

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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.aj.obd.ui.theme.SurfaceDark
import com.aj.obd.ui.theme.SurfaceVariantDark
import com.aj.obd.ui.theme.TelemetryGreen
import com.aj.obd.ui.theme.TextPrimary
import com.aj.obd.ui.theme.TextSecondary

@Composable
fun UpdateDialog(
    updateInfo: AppUpdateInfo,
    isDownloading: Boolean = false,
    downloadProgress: Float = 0f,
    downloadProgressText: String = "",
    onDismiss: () -> Unit,
    onDownload: (String) -> Unit
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
                    text = "ATUALIZAÇÃO OFICIAL DISPONÍVEL",
                    color = AmberGlow,
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
                        text = updateInfo.releaseNotes.ifBlank { "• Conexão rápida com scanner Bluetooth\n• Radar 3D holográfico com animação\n• Manômetros com agulha e luzes neon\n• Atualização direta in-app integrada" },
                        color = TextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Seção de Download In-App com Barra de Progresso
                if (isDownloading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceVariantDark, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "BAIXANDO ATUALIZAÇÃO...",
                            color = AmberGlow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = downloadProgressText.ifBlank { "Aguarde o download completar..." },
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = AmberGlow,
                            trackColor = Color(0xFF282834)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Fique no aplicativo. O instalador abrirá automaticamente.",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                } else {
                    // Botão Atualizar Agora
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
                            text = "ATUALIZAR DIRETO NO APP",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Lembrar Mais Tarde",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
