package com.example.myreminderai

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.* 
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var voiceCallback: ((String) -> Unit)? = null
    private val voiceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) voiceCallback?.invoke(spoken)
    }
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        ReminderAlarm.createChannel(this)
        setContent { ReminderApp(onVoice = { callback -> startVoice(callback) }) }
    }
    private fun startVoice(callback: (String) -> Unit) {
        voiceCallback = callback
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "अपना रिमाइंडर बोलें")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try { voiceLauncher.launch(intent) } catch (_: Exception) {
            val fallback = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM) }
            try { voiceLauncher.launch(fallback) } catch (_: Exception) { callback("") }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderApp(onVoice: ((String) -> Unit) -> Unit) {
    val ctx = LocalContext.current
    val store = remember { ReminderStore(ctx) }
    var list by remember { mutableStateOf(store.all()) }
    var text by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(0) }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var status by remember { mutableStateOf("") }
    val blue = Color(0xFF2867DA)
    val fmtDate = remember { SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()) }
    val fmtTime = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    fun refresh() { list = store.all() }
    fun chooseDateTime(onChosen: (Long) -> Unit) {
        val now = Calendar.getInstance()
        DatePickerDialog(ctx, { _, y, m, d ->
            TimePickerDialog(ctx, { _, hour, minute ->
                val cal = Calendar.getInstance().apply { set(y, m, d, hour, minute, 0); set(Calendar.SECOND, 0) }
                onChosen(cal.timeInMillis)
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).show()
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
    }
    fun addReminder(raw: String) {
        val parsed = ReminderParser.parse(raw)
        if (parsed == null) {
            status = "समय समझ नहीं आया। Date & time चुनकर सेव करें।"
            chooseDateTime { time ->
                val item = ReminderItem(System.currentTimeMillis(), raw.trim(), time, false, raw.contains("whatsapp", true) || raw.contains("व्हाट्स", true))
                store.add(item); ReminderAlarm.schedule(ctx, item); refresh(); status = "Reminder सेव हो गया ✓"; text = ""
            }
            return
        }
        val item = ReminderItem(System.currentTimeMillis(), parsed.first, parsed.second, false, raw.contains("whatsapp", true) || raw.contains("व्हाट्स", true))
        store.add(item); ReminderAlarm.schedule(ctx, item); refresh(); status = "Reminder सेव हो गया ✓"; text = ""
    }
    MaterialTheme(colorScheme = lightColorScheme(primary = blue, secondary = Color(0xFF239B60))) {
        Scaffold(bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.TaskAlt, null) }, label = { Text("Home") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.CalendarMonth, null) }, label = { Text("Calendar") })
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Default.Message, null) }, label = { Text("WhatsApp") })
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().background(Color(0xFFF3F6FA)).padding(padding).padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text(if (tab == 1) "Calendar" else if (tab == 2) "WhatsApp Reminders" else "Good day", color = Color(0xFF6A7C91)); Text("My Reminder AI", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF172B49)) }
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = blue, modifier = Modifier.size(30.dp))
                }
                if (tab == 0) {
                    Card(colors = CardDefaults.cardColors(containerColor = blue), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("What do you need to remember?", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            OutlinedTextField(value = text, onValueChange = { text = it }, placeholder = { Text("कल सुबह 8 बजे वॉक करना…") }, modifier = Modifier.fillMaxWidth(), minLines = 2, colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedTextColor = Color.DarkGray, unfocusedTextColor = Color.DarkGray))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(onClick = { onVoice { spoken -> text = spoken; if (spoken.isNotBlank()) addReminder(spoken) } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = blue)) { Icon(Icons.Default.Mic, null); Spacer(Modifier.width(6.dp)); Text("Speak") }
                                Button(onClick = { if (text.isNotBlank()) addReminder(text) }, modifier = Modifier.weight(1f)) { Text("Add Reminder") }
                            }
                        }
                    }
                    if (status.isNotBlank()) Text(status, color = Color(0xFF239B60), modifier = Modifier.padding(vertical = 8.dp))
                    Row(Modifier.padding(top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text("Upcoming Reminders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text("${list.count { !it.done }} pending", color = Color.Gray) }
                    ReminderList(list.filter { !it.done }.sortedBy { it.whenMillis }, fmtDate, fmtTime, onDone = { store.markDone(it.id); ReminderAlarm.cancel(ctx, it.id); refresh() }, onDelete = { store.remove(it.id); ReminderAlarm.cancel(ctx, it.id); refresh() }, onWhatsApp = { openWhatsApp(ctx) })
                } else if (tab == 1) {
                    val cal = selectedDate
                    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Selected date", fontWeight = FontWeight.SemiBold, color = Color.Gray)
                            Text(fmtDate.format(cal.time), style = MaterialTheme.typography.titleLarge, color = Color(0xFF172B49))
                            Button(onClick = { chooseDateTime { ts -> selectedDate = Calendar.getInstance().apply { timeInMillis = ts }; } }) { Text("Choose date") }
                        }
                    }
                    val sameDay = list.filter { item -> val c = Calendar.getInstance().apply { timeInMillis = item.whenMillis }; c.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR) }.sortedBy { it.whenMillis }
                    Text("Events & Reminders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp))
                    ReminderList(sameDay, fmtDate, fmtTime, onDone = { store.markDone(it.id); ReminderAlarm.cancel(ctx, it.id); refresh() }, onDelete = { store.remove(it.id); ReminderAlarm.cancel(ctx, it.id); refresh() }, onWhatsApp = { openWhatsApp(ctx) })
                } else {
                    Text("WhatsApp follow-up reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF172B49))
                    Text("ऐसे रिमाइंडर बनाएँ जिनमें WhatsApp पर किसी को मैसेज/फॉलो-अप करना हो। समय पर ऐप आपको नोटिफिकेशन देगा।", color = Color.Gray, modifier = Modifier.padding(vertical = 10.dp))
                    ReminderList(list.filter { it.whatsApp && !it.done }.sortedBy { it.whenMillis }, fmtDate, fmtTime, onDone = { store.markDone(it.id); ReminderAlarm.cancel(ctx, it.id); refresh() }, onDelete = { store.remove(it.id); ReminderAlarm.cancel(ctx, it.id); refresh() }, onWhatsApp = { openWhatsApp(ctx) })
                    Button(onClick = { tab = 0; text = "WhatsApp पर मैसेज करना याद दिलाना" }) { Text("+ Add WhatsApp reminder") }
                }
            }
        }
    }
}


@Composable
fun ReminderList(items: List<ReminderItem>, dateFmt: SimpleDateFormat, timeFmt: SimpleDateFormat, onDone: (ReminderItem) -> Unit, onDelete: (ReminderItem) -> Unit, onWhatsApp: () -> Unit) {
    if (items.isEmpty()) { Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { Text("No reminders here yet.", color = Color.Gray) }; return }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        items(items, key = { it.id }) { item ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (item.whatsApp) Icons.Default.Message else Icons.Default.Schedule, null, tint = if (item.whatsApp) Color(0xFF239B60) else Color(0xFF2867DA), modifier = Modifier.size(30.dp))
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(item.title, fontWeight = FontWeight.SemiBold, color = Color(0xFF172B49))
                        Text("${dateFmt.format(Date(item.whenMillis))} · ${timeFmt.format(Date(item.whenMillis))}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        if (item.whatsApp) Text("WhatsApp follow-up", color = Color(0xFF239B60), style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = { onDone(item) }) { Icon(Icons.Default.CheckCircle, "Done", tint = Color(0xFF239B60)) }
                    IconButton(onClick = { onDelete(item) }) { Icon(Icons.Default.Delete, "Delete", tint = Color.Gray) }
                }
            }
        }
    }
}
fun openWhatsApp(ctx: android.content.Context) { try { ctx.startActivity(Intent(Intent.ACTION_VIEW).apply { data = android.net.Uri.parse("https://wa.me/"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) { } }
