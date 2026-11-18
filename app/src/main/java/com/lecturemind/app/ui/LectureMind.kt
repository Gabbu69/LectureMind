@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.lecturemind.app.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import com.lecturemind.app.ui.icons.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.work.WorkManager
import com.lecturemind.app.*
import com.lecturemind.app.ai.*
import com.lecturemind.app.data.*
import com.lecturemind.app.recording.*
import kotlinx.coroutines.*
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable fun LectureMind(app: LectureMindApp) {
    var appearance by remember { mutableStateOf(app.settings.appearance) }
    LectureTheme(appearance) {
        val ready by app.ready.collectAsStateWithLifecycle()
        val dao=app.database.dao()
        val lectures by remember { dao.observeLectures() }.collectAsStateWithLifecycle(emptyList())
        val recording by RecordingService.state.collectAsStateWithLifecycle()
        var nav by rememberSaveable { mutableIntStateOf(0) }
        var selected by rememberSaveable { mutableStateOf<String?>(null) }
        var showNew by remember { mutableStateOf(false) }
        var draftTitle by rememberSaveable { mutableStateOf("") }
        var draftSubject by rememberSaveable { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        val scope=rememberCoroutineScope()
        fun startRecording() {
            scope.launch {
                var createdId: String? = null
                try {
                    val lecture=Lecture(title=draftTitle.trim().ifBlank { "Lecture · ${SimpleDateFormat("MMM d, h:mm a",Locale.getDefault()).format(Date())}" }.take(120),subject=draftSubject.trim().ifBlank { "General" }.take(60),status="recording")
                    withContext(Dispatchers.IO) { dao.insert(lecture) }
                    createdId=lecture.id
                    selected=lecture.id;showNew=false
                    RecordingService.command(app,RecordingService.START,lecture.id)
                } catch(_: Exception) {
                    createdId?.let { id -> runCatching { withContext(Dispatchers.IO) { dao.status(id,"saved","Recording could not start. The microphone is off; open the app and try again.") } } }
                    error="Could not start recording. Check microphone permission and storage, then try again."
                }
            }
        }
        val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if(result[Manifest.permission.RECORD_AUDIO]==true) startRecording() else error="Microphone permission is needed to record. You can enable it in Android Settings → Apps → LectureMind → Permissions."
        }
        BackHandler(selected!=null) { selected=null }
        Surface(Modifier.fillMaxSize()) {
            if(!ready) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { CircularProgressIndicator() }
            else if(selected!=null) {
                key(selected) { LectureScreen(app,selected!!,onBack={selected=null},onSettings={selected=null;nav=2}) }
            } else Scaffold(
                bottomBar={ NavigationBar(containerColor=MaterialTheme.colorScheme.surfaceContainerLow) {
                    listOf("Library" to Icons.AutoMirrored.Outlined.LibraryBooks,"Study" to Icons.Outlined.School,"Settings" to Icons.Outlined.Tune).forEachIndexed { i,(title,icon) ->
                        NavigationBarItem(selected=nav==i,onClick={nav=i},icon={Icon(icon,null)},label={Text(title)})
                    }
                } }
            ) { insets ->
                when(nav) {
                    2 -> SettingsScreen(app,appearance,{appearance=it})
                    else -> LibraryScreen(lectures,nav==1,recording,Modifier.padding(insets),onOpen={selected=it},onRecord={if(recording.lectureId!=null)selected=recording.lectureId else {draftTitle="";draftSubject="";showNew=true}},onSettings={nav=2})
                }
            }
        }
        if(showNew) {
            var consent by remember { mutableStateOf(false) }
            AlertDialog(onDismissRequest={showNew=false},icon={Icon(Icons.Outlined.Mic,None)},title={Text("A little clarity starts here")},text={
                Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                    Text("Save a lecture on your phone. Generate AI notes after class, whenever you're ready.")
                    OutlinedTextField(draftTitle,{draftTitle=it.take(120)},label={Text("Lecture title")},singleLine=true)
                    OutlinedTextField(draftSubject,{draftSubject=it.take(60)},label={Text("Subject")},singleLine=true)
                    Row(Modifier.fillMaxWidth().clickable { consent=!consent },verticalAlignment=Alignment.CenterVertically) { Checkbox(consent,{consent=it});Text("I have permission to record this lecture.",style=MaterialTheme.typography.bodyMedium) }
                    Text("Recording works offline. Nothing is sent to AI until you choose Generate.",style=MaterialTheme.typography.bodySmall)
                }
            },confirmButton={Button(enabled=consent,onClick={
                if(ContextCompat.checkSelfPermission(app,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED && (Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(app,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)) startRecording()
                else permission.launch(if(Build.VERSION.SDK_INT>=33) arrayOf(Manifest.permission.RECORD_AUDIO,Manifest.permission.POST_NOTIFICATIONS) else arrayOf(Manifest.permission.RECORD_AUDIO))
            }){Text("Start recording")}},dismissButton={TextButton(onClick={showNew=false}){Text("Cancel")}})
        }
        error?.let { AlertDialog(onDismissRequest={error=null},title={Text("Recording needs attention")},text={Text(it)},confirmButton={TextButton(onClick={error=null}){Text("Got it")}}) }
    }
}
private val None: String? = null

@Composable private fun LibraryScreen(lectures: List<Lecture>,study: Boolean,recording: RecordingState,modifier: Modifier,onOpen:(String)->Unit,onRecord:()->Unit,onSettings:()->Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    var subject by rememberSaveable { mutableStateOf("All") }
    val subjects=listOf("All")+lectures.map { it.subject }.distinct().sorted()
    val filtered=lectures.filter { (!study || it.notes.isNotBlank()) && (subject=="All" || it.subject==subject) && ("${it.title} ${it.subject} ${it.notes}".contains(search,true)) }
    LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(start=22.dp,end=22.dp,top=20.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.primary),contentAlignment=Alignment.Center) { Icon(Icons.Outlined.GraphicEq,null,tint=MaterialTheme.colorScheme.onPrimary) }
                    Text("LectureMind",style=MaterialTheme.typography.titleLarge,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
                IconButton(onClick=onSettings) { Icon(Icons.Outlined.Tune,"Open settings") }
            }
        }
        item {
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(if(study) "Make it stick." else "Listen now.\nUnderstand more.",style=MaterialTheme.typography.headlineLarge)
                Text(if(study) "Your notes, reviewers and practice questions, all in one place." else "Turn your lectures into knowledge you can keep.",color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if(!study) item {
            Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer),shape=RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(if(recording.lectureId!=null) "Your lecture is ${if(recording.paused) "paused" else "recording"}" else "Ready for your next lecture?",style=MaterialTheme.typography.titleMedium);Spacer(Modifier.height(6.dp));Text(if(recording.lectureId!=null) "${formatTime(recording.durationMs)} · Audio stays on your phone" else "Record offline. Add AI when you're ready.",style=MaterialTheme.typography.bodyMedium) }
                        Icon(Icons.Outlined.Mic,null,Modifier.padding(start=12.dp).size(36.dp),tint=MaterialTheme.colorScheme.primary)
                    }
                    Button(onClick=onRecord,Modifier.fillMaxWidth().heightIn(min=52.dp),shape=RoundedCornerShape(16.dp)) { Icon(if(recording.lectureId!=null) Icons.Outlined.GraphicEq else Icons.Outlined.Add,null);Spacer(Modifier.width(8.dp));Text(if(recording.lectureId!=null) "Return to recording" else "Record a lecture") }
                }
            }
        }
        item {
            val count=if(study) lectures.count { it.notes.isNotBlank() } else lectures.size
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Text(if(study) "Your study space" else "Your library",style=MaterialTheme.typography.titleLarge)
                Text("$count ${if(count==1) "lecture" else "lectures"}",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if(lectures.isNotEmpty()) item {
            OutlinedTextField(search,{search=it},Modifier.fillMaxWidth(),placeholder={Text("Search lectures and notes")},leadingIcon={Icon(Icons.Outlined.Search,null)},singleLine=true,shape=RoundedCornerShape(16.dp))
            if(subjects.size>2) Row(Modifier.horizontalScroll(rememberScrollState()).padding(top=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) { subjects.forEach { name -> FilterChip(selected=subject==name,onClick={subject=name},label={Text(name)}) } }
        }
        if(filtered.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(vertical=28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(76.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer),contentAlignment=Alignment.Center) { Icon(if(study) Icons.Outlined.AutoStories else Icons.Outlined.Headphones,null,Modifier.size(32.dp),tint=MaterialTheme.colorScheme.primary) }
                Text(if(search.isNotBlank()) "No matching lectures" else if(study) "Your next study session starts here" else "A fresh page for your ideas",style=MaterialTheme.typography.titleMedium)
                Text(if(search.isNotBlank()) "Try a different title, subject or keyword." else if(study) "Open a saved lecture and generate notes to create your first study set." else "Record your first lecture, bookmark the important parts, and make it yours.",Modifier.widthIn(max=300.dp),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(filtered,key={it.id}) { lecture -> LectureCard(lecture,{onOpen(lecture.id)}) }
        if(!study && lectures.isEmpty()) item {
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.padding(top=18.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) { Icon(Icons.Outlined.VerifiedUser,null,Modifier.size(20.dp),tint=MaterialTheme.colorScheme.primary);Text("Your recordings stay on your phone. You decide when to send them to Gemini or OpenAI.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
@Composable private fun LectureCard(lecture: Lecture,onClick:()->Unit) {
    Card(onClick=onClick,colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),shape=RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(18.dp),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer),contentAlignment=Alignment.Center) { Icon(if(lecture.notes.isNotBlank()) Icons.Outlined.AutoStories else Icons.Outlined.GraphicEq,null,tint=MaterialTheme.colorScheme.primary) }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) { Text(lecture.title,style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=TextOverflow.Ellipsis);Text("${lecture.subject} · ${formatTime(lecture.durationMs)}",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant);Text("${dateLabel(lecture.created)} · ${statusLabel(lecture.status)}",style=MaterialTheme.typography.labelSmall,color=if(lecture.status=="error") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }
            Icon(Icons.Outlined.ChevronRight,null,tint=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
private fun dateLabel(date: Long)=SimpleDateFormat("MMM d, yyyy",Locale.getDefault()).format(Date(date))
private fun statusLabel(status: String)=when(status){"ready"->"Ready to study";"processing","queued"->"Preparing study materials";"recording"->"Recording";"paused"->"Paused";"error"->"Needs attention";else->"Audio saved"}

@Composable private fun LectureScreen(app: LectureMindApp,id: String,onBack:()->Unit,onSettings:()->Unit) {
    val dao=app.database.dao();val scope=rememberCoroutineScope()
    val lecture by remember(id) { dao.observeLecture(id) }.collectAsStateWithLifecycle(null)
    val parts by remember(id) { dao.observeAudio(id) }.collectAsStateWithLifecycle(emptyList())
    val bookmarks by remember(id) { dao.observeBookmarks(id) }.collectAsStateWithLifecycle(emptyList())
    val attempts by remember(id) { dao.observeAttempts(id) }.collectAsStateWithLifecycle(emptyList())
    val recording by RecordingService.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable(id) { mutableIntStateOf(1) }
    var generate by remember { mutableStateOf(false) };var delete by remember { mutableStateOf(false) };var rename by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var seek by remember { mutableStateOf<Long?>(null) }
    val current=lecture
    if(current==null) { Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { CircularProgressIndicator() };return }
    val active=recording.lectureId==id
    val busy=current.status in listOf("queued","processing")
    val speeches=remember(parts) { parts.flatMap { runCatching { ContentCodec.speech(it.transcript) }.getOrDefault(emptyList()) } }
    Scaffold(topBar={TopAppBar(title={Text("Lecture workspace",style=MaterialTheme.typography.titleMedium,maxLines=1,overflow=TextOverflow.Ellipsis)},navigationIcon={IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Outlined.ArrowBack,"Back to library")}},actions={
        if(!active && !busy) { IconButton(onClick={rename=true}){Icon(Icons.Outlined.Edit,"Rename lecture")};IconButton(onClick={delete=true}){Icon(Icons.Outlined.DeleteOutline,"Delete lecture")} }
    })}) { insets ->
        LazyColumn(Modifier.fillMaxSize().padding(insets),contentPadding=PaddingValues(start=22.dp,end=22.dp,top=10.dp,bottom=36.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            item { Column(verticalArrangement=Arrangement.spacedBy(6.dp)) { Text(current.subject.uppercase(),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary);Text(current.title,style=MaterialTheme.typography.headlineMedium);Text("${dateLabel(current.created)} · ${statusLabel(current.status)}",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant) } }
            if(active) item { RecorderPanel(recording,onPause={RecordingService.command(app,RecordingService.PAUSE)},onMark={RecordingService.command(app,RecordingService.MARK)},onStop={RecordingService.command(app,RecordingService.STOP)}) }
            else if(parts.any { it.durationMs>0 && it.finalized }) item { PlayerPanel(parts.filter { it.durationMs>0 && it.finalized },seek,{seek=null}) }
            if(bookmarks.isNotEmpty()) item {
                Text("Important moments",style=MaterialTheme.typography.titleMedium)
                Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) { bookmarks.forEach { b -> AssistChip(enabled=!active,onClick={seek=b.positionMs},label={Text(formatTime(b.positionMs))},leadingIcon={Icon(Icons.Outlined.BookmarkBorder,null,Modifier.size(18.dp))}) } }
            }
            if(current.message.isNotBlank()) item { InfoCard(current.message,if(current.status=="error") Icons.Outlined.Info else Icons.Outlined.CheckCircle) }
            if(!active) item {
                if(busy) Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) { Text(if(current.status=="queued") "Waiting for internet…" else "Turning your lecture into a study set",style=MaterialTheme.typography.titleMedium);LinearProgressIndicator(Modifier.fillMaxWidth());Text(current.message.ifBlank { "Completed transcript sections are saved as we go." },style=MaterialTheme.typography.bodyMedium);TextButton(onClick={scope.launch { withContext(Dispatchers.IO) { WorkManager.getInstance(app).cancelUniqueWork("lecture-$id").result.get();dao.status(id,if(current.notes.isBlank()) "saved" else "ready","AI processing cancelled. Completed transcript sections have been kept.") } }}){Text("Cancel processing")} }
                } else Button(onClick={generate=true},Modifier.fillMaxWidth().heightIn(min=52.dp),shape=RoundedCornerShape(16.dp),enabled=current.durationMs>0) { Icon(Icons.Outlined.AutoAwesome,null);Spacer(Modifier.width(8.dp));Text(if(current.notes.isBlank()) "Generate study materials" else "Regenerate study materials") }
            }
            item {
                PrimaryScrollableTabRow(selectedTabIndex=tab,edgePadding=0.dp,containerColor=Color.Transparent) { listOf("Transcript","Notes","Reviewer","Quiz").forEachIndexed { i,label -> Tab(selected=tab==i,onClick={tab=i},text={Text(label)}) } }
            }
            when(tab) {
                0 -> {
                    if(speeches.isEmpty()) item { EmptyPanel(Icons.Outlined.Subtitles,"Your lecture, in words","Save your recording and generate study materials to see the original transcript here.") }
                    else { item { Text("Original AI transcript · Tap a timestamp to listen. OpenAI links to the start of each recording section. Check unclear words against the audio.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) };items(speeches,key={it.id}) { speech -> Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow)) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) { TextButton(onClick={seek=speech.startMs},contentPadding=PaddingValues(0.dp)){Icon(Icons.Outlined.PlayCircle,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text("${formatTime(speech.startMs)} · ${speech.id}")};SelectionContainerCompat(speech.text) } } } }
                }
                1 -> item { NotesPanel(current,app,false,{source->speeches.firstOrNull { it.id==source }?.let { seek=it.startMs }},onError={error=it}) }
                2 -> item { NotesPanel(current,app,true,{source->speeches.firstOrNull { it.id==source }?.let { seek=it.startMs }},onError={error=it}) }
                3 -> item { QuizPanel(current,attempts,app,{source->speeches.firstOrNull { it.id==source }?.let { seek=it.startMs }}) }
            }
        }
    }
    if(generate) GenerateDialog(app,current,parts,onSettings={generate=false;onSettings()},onDismiss={generate=false},onConfirm={provider,model,language,count->
        generate=false
        scope.launch {
            try { withContext(Dispatchers.IO) { dao.status(id,"queued","Waiting to begin. Completed transcripts will be reused.") };StudyWorker.enqueue(app,id,provider,model,language,count) }
            catch(_: Exception) { withContext(Dispatchers.IO){dao.status(id,"error","Unable to schedule AI processing. Try again.")} }
        }
    })
    if(delete) AlertDialog(onDismissRequest={delete=false},title={Text("Delete this lecture?")},text={Text("This removes its audio, bookmarks, notes, reviewer and quiz attempts from this phone. Previously shared exports are separate files.")},confirmButton={TextButton(onClick={scope.launch {
        try { withContext(Dispatchers.IO) {
            val root=File(app.filesDir,"audio").canonicalFile
            val target=File(root,id).canonicalFile
            check(target.parentFile==root && dao.lecture(id)!=null)
            Exports.clearLectureCache(app,id)
            if(target.exists())check(target.deleteRecursively()) { "Some audio files could not be removed." }
            val jobsRoot=File(app.filesDir,"jobs").canonicalFile;val jobs=File(jobsRoot,id).canonicalFile
            check(jobs.parentFile==jobsRoot)
            if(jobs.exists())check(jobs.deleteRecursively()) { "Some temporary files could not be removed." }
            dao.delete(id)
        };delete=false;onBack() } catch(_: Exception) { delete=false;error="Could not remove all local lecture files. Check storage and try deleting again." }
    }}){Text("Delete",color=MaterialTheme.colorScheme.error)}},dismissButton={TextButton(onClick={delete=false}){Text("Keep lecture")}})
    if(rename) {
        var title by remember { mutableStateOf(current.title) };var subject by remember { mutableStateOf(current.subject) }
        AlertDialog(onDismissRequest={rename=false},title={Text("Lecture details")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){OutlinedTextField(title,{title=it.take(120)},label={Text("Title")});OutlinedTextField(subject,{subject=it.take(60)},label={Text("Subject")})}},confirmButton={TextButton(enabled=title.isNotBlank(),onClick={scope.launch { withContext(Dispatchers.IO){dao.rename(id,title.trim(),subject.trim().ifBlank { "General" })};rename=false }}){Text("Save")}},dismissButton={TextButton(onClick={rename=false}){Text("Cancel")}})
    }
    error?.let { AlertDialog(onDismissRequest={error=null},title={Text("Lecture needs attention")},text={Text(it)},confirmButton={TextButton(onClick={error=null}){Text("OK")}}) }
}

@Composable private fun RecorderPanel(state: RecordingState,onPause:()->Unit,onMark:()->Unit,onStop:()->Unit) {
    Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) { Box(Modifier.size(9.dp).clip(CircleShape).background(if(state.paused) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFB3261E)));Text(if(state.saving) "Saving your lecture…" else if(state.paused) "PAUSED" else "RECORDING ON THIS PHONE",style=MaterialTheme.typography.labelMedium) }
            Text(formatTime(state.durationMs),style=MaterialTheme.typography.headlineLarge.copy(fontSize=48.sp,lineHeight=54.sp))
            Row(Modifier.fillMaxWidth().height(38.dp).semantics { contentDescription="Microphone level ${(state.level*100).toInt()} percent" },horizontalArrangement=Arrangement.spacedBy(4.dp),verticalAlignment=Alignment.CenterVertically) { repeat(28) { i -> val h=(8+(state.level*30)*(0.35f+0.65f*kotlin.math.abs(kotlin.math.sin(i*1.7f)))).dp;Box(Modifier.weight(1f).height(h).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha=if(state.paused)0.3f else 0.8f))) } }
            Text("Bookmark an idea now. Find it faster later.",style=MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick=onMark,enabled=!state.saving,modifier=Modifier.fillMaxWidth().heightIn(min=50.dp)){Icon(Icons.Outlined.BookmarkAdd,null);Spacer(Modifier.width(8.dp));Text("Mark important moment")}
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick=onPause,enabled=!state.saving,modifier=Modifier.weight(1f).heightIn(min=52.dp)) { Icon(if(state.paused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,null);Spacer(Modifier.width(6.dp));Text(if(state.paused)"Resume" else "Pause") }
                Button(onClick=onStop,enabled=!state.saving,modifier=Modifier.weight(1f).heightIn(min=52.dp)){Icon(Icons.Outlined.Stop,null);Spacer(Modifier.width(6.dp));Text("Save")}
            }
            Text("You can lock your screen. Recording continues with a visible notification.",style=MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable private fun PlayerPanel(parts: List<AudioPart>,seek: Long?,onSeekConsumed:()->Unit) {
    val context=LocalContext.current
    val player=remember(parts.map { it.path }) { ExoPlayer.Builder(context).build().apply { setAudioAttributes(androidx.media3.common.AudioAttributes.Builder().setUsage(androidx.media3.common.C.USAGE_MEDIA).setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_SPEECH).build(),true);setMediaItems(parts.map { MediaItem.fromUri(android.net.Uri.fromFile(File(it.path))) });prepare() } }
    var playing by remember { mutableStateOf(false) };var position by remember { mutableLongStateOf(0) };var playerError by remember { mutableStateOf<String?>(null) }
    val duration=parts.sumOf { it.durationMs }
    DisposableEffect(player) {
        val listener=object: androidx.media3.common.Player.Listener { override fun onPlayerError(error: androidx.media3.common.PlaybackException) { playerError="This audio could not be played. The saved file has been kept." } }
        player.addListener(listener);onDispose { player.removeListener(listener);player.release() }
    }
    LaunchedEffect(player) { while(true) { playing=player.isPlaying;position=parts.take(player.currentMediaItemIndex.coerceAtLeast(0)).sumOf { it.durationMs }+player.currentPosition.coerceAtLeast(0);delay(250) } }
    LaunchedEffect(seek,player) { if(seek!=null) { val (index,offset)=WavFile.position(parts.map { it.durationMs },seek);player.seekTo(index,offset);player.play();onSeekConsumed() } }
    Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),shape=RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                FilledIconButton(onClick={if(player.isPlaying)player.pause() else { if(position>=duration-100)player.seekTo(0,0);player.play() }},Modifier.size(52.dp)){Icon(if(playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,if(playing)"Pause audio" else "Play audio")}
                Column(Modifier.weight(1f).padding(start=12.dp)){Text("Lecture audio",style=MaterialTheme.typography.titleMedium);Text("${formatTime(position)} / ${formatTime(duration)}",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                IconButton(onClick={val (i,p)=WavFile.position(parts.map{it.durationMs},position-10000);player.seekTo(i,p)}){Icon(Icons.Outlined.Replay10,"Back 10 seconds")}
                IconButton(onClick={val (i,p)=WavFile.position(parts.map{it.durationMs},position+10000);player.seekTo(i,p)}){Icon(Icons.Outlined.Forward10,"Forward 10 seconds")}
            }
            Slider(value=position.toFloat().coerceIn(0f,duration.toFloat().coerceAtLeast(1f)),onValueChange={val(i,p)=WavFile.position(parts.map{it.durationMs},it.toLong());player.seekTo(i,p)},valueRange=0f..duration.toFloat().coerceAtLeast(1f),modifier=Modifier.semantics{contentDescription="Audio playback position"})
            playerError?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable private fun NotesPanel(lecture: Lecture,app: LectureMindApp,reviewer: Boolean,onSource:(String)->Unit,onError:(String)->Unit) {
    val text=if(reviewer) lecture.reviewer else lecture.notes
    var edit by remember { mutableStateOf(false) };var export by remember { mutableStateOf(false) }
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    if(text.isBlank()) { EmptyPanel(if(reviewer) Icons.Outlined.AutoStories else Icons.Outlined.Description,if(reviewer)"Build your understanding" else "From spoken ideas to clear notes",if(reviewer)"Your reviewer will collect the lecture's main ideas, definitions and questions for self-review." else "Generate a study set after recording. Your notes will be organized, source-linked, and editable.");return }
    Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Text(if(reviewer)"Your reviewer" else "Your notes",Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
            Row { if(!reviewer) IconButton(onClick={edit=true}){Icon(Icons.Outlined.Edit,"Edit notes")};IconButton(onClick={export=true}){Icon(Icons.Outlined.IosShare,"Share ${if(reviewer) "reviewer" else "notes"}")} }
        }
        Text("${lecture.generatedProvider} · ${lecture.generatedLanguage} · Check important facts against the recording.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow),shape=RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                text.lines().forEach { line ->
                    when {
                        line.startsWith("Sources:") -> Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) { line.removePrefix("Sources:").split(",").map{it.trim()}.filter{it.isNotBlank()}.forEach { source->AssistChip(onClick={onSource(source)},label={Text(source)},leadingIcon={Icon(Icons.Outlined.Headphones,null,Modifier.size(16.dp))}) } }
                        line.startsWith("## ") -> Text(line.removePrefix("## "),style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(top=8.dp))
                        line.isNotBlank() -> SelectionContainerCompat(line)
                    }
                }
            }
        }
    }
    if(edit) {
        var draft by remember { mutableStateOf(text) }
        AlertDialog(onDismissRequest={edit=false},title={Text("Edit your notes")},text={Column(Modifier.verticalScroll(rememberScrollState())) { Text("Your original transcript stays separate.",style=MaterialTheme.typography.bodySmall);Spacer(Modifier.height(12.dp));OutlinedTextField(draft,{draft=it.take(200000)},Modifier.fillMaxWidth().heightIn(min=180.dp,max=380.dp),label={Text("Notes")}) }},confirmButton={TextButton(onClick={scope.launch { withContext(Dispatchers.IO){app.database.dao().notes(lecture.id,draft)};edit=false }}){Text("Save notes")}},dismissButton={TextButton(onClick={edit=false}){Text("Cancel")}})
    }
    if(export) AlertDialog(onDismissRequest={export=false},title={Text("Share your ${if(reviewer) "reviewer" else "notes"}")},text={Text("Create a readable PDF or a plain text file. API keys and private settings are never included.")},confirmButton={TextButton(onClick={export=false;runCatching{Exports.share(context,lecture.title,text,true,lecture.id)}.onFailure{onError("Unable to create a PDF. Check free storage and try again.")}}){Text("Share PDF")}},dismissButton={TextButton(onClick={export=false;runCatching{Exports.share(context,lecture.title,text,false,lecture.id)}.onFailure{onError("Unable to share the file. Check free storage and try again.")}}){Text("Share text")}})
}
@Composable private fun SelectionContainerCompat(text: String) { androidx.compose.foundation.text.selection.SelectionContainer { Text(text,style=MaterialTheme.typography.bodyLarge) } }

@Composable private fun QuizPanel(lecture: Lecture,attempts: List<Attempt>,app: LectureMindApp,onSource:(String)->Unit) {
    val questions=remember(lecture.quiz){runCatching{ContentCodec.quiz(lecture.quiz)}.getOrDefault(emptyList())}
    var started by rememberSaveable(lecture.quiz){mutableStateOf(false)}
    var answers by rememberSaveable(lecture.quiz){mutableStateOf(List(questions.size){-1})}
    var submitted by rememberSaveable(lecture.quiz){mutableStateOf(false)}
    var saving by remember{mutableStateOf(false)};val scope=rememberCoroutineScope()
    if(questions.isEmpty()) { EmptyPanel(Icons.Outlined.Quiz,"Practice with a purpose","Generate study materials to get practice questions based on your lecture, with explanations and audio references.");return }
    Column(verticalArrangement=Arrangement.spacedBy(18.dp)) {
        if(!started) {
            Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer),shape=RoundedCornerShape(22.dp)) { Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) { Icon(Icons.Outlined.School,null,Modifier.size(34.dp),tint=MaterialTheme.colorScheme.primary);Text("Put your understanding to the test",style=MaterialTheme.typography.titleLarge);Text("${questions.size} questions · Answers and explanations appear after you submit.");Button(onClick={started=true;submitted=false;answers=List(questions.size){-1}},Modifier.fillMaxWidth().heightIn(min=50.dp)){Text("Start practice quiz")} } }
        } else {
            if(submitted) InfoCard("${ContentCodec.score(questions,answers)} / ${questions.size} correct · Review the explanations below.",Icons.Outlined.CheckCircle)
            Text(if(submitted)"Your results" else "${answers.count{it>=0}} of ${questions.size} answered",style=MaterialTheme.typography.titleLarge)
            questions.forEachIndexed { index,q ->
                Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),shape=RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        Text("QUESTION ${index+1}",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary);Text(q.prompt,style=MaterialTheme.typography.titleMedium)
                        q.options.forEachIndexed { optionIndex,option ->
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if(submitted && optionIndex==q.answer)MaterialTheme.colorScheme.primaryContainer else Color.Transparent).clickable(enabled=!submitted){answers=answers.toMutableList().also{it[index]=optionIndex}}.padding(vertical=2.dp),verticalAlignment=Alignment.CenterVertically) { RadioButton(selected=answers[index]==optionIndex,onClick=if(submitted)null else {{answers=answers.toMutableList().also{it[index]=optionIndex}}});Text(option,Modifier.weight(1f).padding(end=10.dp)) }
                        }
                        if(submitted) { HorizontalDivider();Text(if(answers[index]==q.answer)"Correct" else "Correct answer: ${q.options[q.answer]}",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.SemiBold);Text(q.explanation);Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){q.sources.forEach{source->AssistChip(onClick={onSource(source)},label={Text("Listen · $source")})}} }
                    }
                }
            }
            if(!submitted) Button(onClick={saving=true;scope.launch { try { withContext(Dispatchers.IO){app.database.dao().attempt(Attempt(lectureId=lecture.id,correct=ContentCodec.score(questions,answers),total=questions.size,answers=JSONArray(answers).toString(),questions=lecture.quiz))};submitted=true } finally {saving=false} }},enabled=answers.all{it>=0} && !saving,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)){Text(if(saving)"Saving results…" else "Submit answers")}
            else OutlinedButton(onClick={started=false;submitted=false;answers=List(questions.size){-1}},Modifier.fillMaxWidth().heightIn(min=50.dp)){Text("Practice again")}
        }
        if(attempts.isNotEmpty()) { Text("Practice history",style=MaterialTheme.typography.titleMedium);attempts.take(10).forEach{attempt->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(dateLabel(attempt.created));Text("${attempt.correct} / ${attempt.total}",fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.primary)}} }
    }
}

@Composable private fun GenerateDialog(app: LectureMindApp,lecture: Lecture,parts: List<AudioPart>,onSettings:()->Unit,onDismiss:()->Unit,onConfirm:(String,String,String,Int)->Unit) {
    val provider=remember{app.settings.provider};val model=remember{app.settings.model(provider)};val language=remember{app.settings.language}
    val hasKey=remember{app.settings.hasKey(provider)}
    var count by remember{mutableIntStateOf(10)}
    val uncached=parts.filter{it.transcript.isBlank() && it.durationMs>0}
    AlertDialog(onDismissRequest=onDismiss,icon={Icon(Icons.Outlined.AutoAwesome,null)},title={Text(if(hasKey)"Create your study set" else "Connect your AI first")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("$provider · $model",fontWeight=FontWeight.SemiBold)
        if(!hasKey) Text("Add your own $provider API key in Settings. It is encrypted on this phone and never bundled into the app.") else {
            Text(if(uncached.isEmpty())"Your saved transcript will be sent to $provider. No audio needs to be uploaded again." else "${uncached.size} audio ${if(uncached.size==1) "section" else "sections"} (${formatTime(uncached.sumOf{it.durationMs})}) will be sent to $provider for transcription. The transcript is then used for study materials.")
            Text("Output: $language · Original lecture languages are preserved.",style=MaterialTheme.typography.bodyMedium)
            Text("Practice questions",style=MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(10,20,30).forEach{n->FilterChip(selected=count==n,onClick={count=n},label={Text("$n")})}}
            Text("Provider usage may cost money and is subject to your account's quota. You can cancel processing; requests already sent may still be billed.",style=MaterialTheme.typography.bodySmall)
            if(lecture.notes.isNotBlank()) Text("Regenerating replaces your notes, reviewer and quiz. Export edited notes first if you want to keep a copy.",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodyMedium)
            Text("AI can misunderstand speech. Use the audio references to check important details.",style=MaterialTheme.typography.bodySmall)
        }
    }},confirmButton={Button(onClick={if(hasKey)onConfirm(provider,model,language,count) else onSettings()}){Text(if(hasKey)"Send & generate" else "Open settings")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}

@Composable private fun SettingsScreen(app: LectureMindApp,appearance: String,onAppearance:(String)->Unit) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var provider by remember { mutableStateOf(app.settings.provider) };var language by remember{mutableStateOf(app.settings.language)}
    var key by remember(provider){mutableStateOf("")};var hasKey by remember(provider){mutableStateOf(app.settings.hasKey(provider))}
    var model by remember(provider){mutableStateOf(app.settings.model(provider))};var advanced by remember{mutableStateOf(false)}
    var testing by remember{mutableStateOf(false)};var message by remember{mutableStateOf<String?>(null)};var isError by remember{mutableStateOf(false)}
    var storage by remember{mutableLongStateOf(0)}
    DisposableEffect(Unit) { val window=context.activityOrNull()?.window;window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE);onDispose{window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)} }
    LaunchedEffect(Unit){storage=withContext(Dispatchers.IO){File(app.filesDir,"audio").walkTopDown().filter{it.isFile}.sumOf{it.length()}}}
    LazyColumn(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top+WindowInsetsSides.Horizontal)).imePadding(),contentPadding=PaddingValues(start=22.dp,end=22.dp,top=24.dp,bottom=120.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        item { Text("Make it yours.",style=MaterialTheme.typography.headlineLarge);Spacer(Modifier.height(8.dp));Text("Your AI, your language, your study space.",color=MaterialTheme.colorScheme.onSurfaceVariant) }
        item { SectionLabel("AI CONNECTION",Icons.Outlined.AutoAwesome) }
        item { Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow),shape=RoundedCornerShape(20.dp)) { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text("Choose your provider",style=MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp)){listOf("Gemini","OpenAI").forEach{p->FilterChip(selected=provider==p,onClick={if(!testing){provider=p;app.settings.provider=p;message=null}},label={Text(p)},leadingIcon=if(provider==p){{Icon(Icons.Outlined.Check,null,Modifier.size(16.dp))}}else null)}}
            Text(if(provider=="OpenAI")"Use an OpenAI API key. A ChatGPT login is not an API key." else "Use an API key from Google AI Studio.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(key,{key=it.take(512);message=null},Modifier.fillMaxWidth(),label={Text(if(hasKey)"Replace saved API key" else "$provider API key")},singleLine=true,visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),supportingText={Text(if(hasKey)"A key is saved and encrypted on this phone." else "Your key stays private. Paste it here on your phone.")})
            FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Button(enabled=key.isNotBlank() && !testing,onClick={runCatching{app.settings.saveKey(provider,key);key="";hasKey=true;isError=false;message="API key saved securely on this phone."}.onFailure{isError=true;message=it.message}}){Text("Save key")}
                OutlinedButton(enabled=hasKey && !testing,onClick={testing=true;message=null;val selectedProvider=provider;val selectedModel=app.settings.model(provider);scope.launch{try{withContext(Dispatchers.IO){AiProvider.create(selectedProvider,app.settings.key(selectedProvider),selectedModel).testConnection()};isError=false;message="Connected to $selectedProvider. Model access is available. Audio and generation are tested when you create a study set."}catch(e:Exception){isError=true;message=e.message ?: "Connection failed. Check your key and internet."}finally{testing=false}}}){if(testing)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)else Text("Test connection")}
            }
            message?.let{Text(it,color=if(isError)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.bodyMedium)}
            if(hasKey) TextButton(onClick={app.settings.removeKey(provider);hasKey=false;key="";message="Saved $provider key removed.";isError=false},enabled=!testing){Icon(Icons.Outlined.KeyOff,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text("Remove saved key")}
            TextButton(onClick={advanced=!advanced}){Text(if(advanced)"Hide model settings" else "Advanced model settings")}
            if(advanced){OutlinedTextField(model,{model=it.take(100)},label={Text("Study model ID")},supportingText={Text("Use a model your account can access. OpenAI speech uses gpt-transcribe.")});OutlinedButton(onClick={runCatching{app.settings.setModel(provider,model.trim());message="Model saved. Test the connection before processing.";isError=false}.onFailure{message="Use a valid model ID with letters, numbers, dots, hyphens or underscores.";isError=true}}){Text("Save model")}}
        } } }
        item { SectionLabel("STUDY PREFERENCES",Icons.Outlined.School) }
        item { Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow),shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Notes, reviewer and quiz language",style=MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){LectureLanguages.outputOptions.forEach{l->FilterChip(selected=language==l,onClick={language=l;app.settings.language=l},label={Text(l)})}}
            Text("Lecture languages: Ilonggo/Hiligaynon, Bisaya/Cebuano, Tagalog/Filipino and English. Mixed speech stays in its original language in the transcript. AI can mishear local words; use the audio to check unclear speech.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider();Text("Appearance",style=MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("System","Light","Dark").forEach{a->FilterChip(selected=appearance==a,onClick={app.settings.appearance=a;onAppearance(a)},label={Text(a)})}}
        }} }
        item { SectionLabel("ON THIS PHONE",Icons.Outlined.VerifiedUser) }
        item { InfoCard("Audio storage: ${"%.1f".format(storage/1024.0/1024.0)} MB\nSpeech audio uses about 115 MB per hour. Delete individual lectures from their workspace to free space.\n\nRecordings and notes stay in app-private storage. AI receives content only when you choose Send & generate. Android cloud backup is disabled. Uninstalling removes your local library; export anything you want to keep.",Icons.Outlined.PhonelinkLock) }
        item { Text("LectureMind 1.0.0\nMade for a clearer way to learn.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
@Composable private fun SectionLabel(text: String,icon: ImageVector) { Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){Icon(icon,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.primary);Text(text,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)} }
@Composable private fun InfoCard(text: String,icon: ImageVector) { Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainer),shape=RoundedCornerShape(16.dp)){Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){Icon(icon,null,Modifier.size(22.dp),tint=MaterialTheme.colorScheme.primary);Text(text,style=MaterialTheme.typography.bodyMedium)}} }
@Composable private fun EmptyPanel(icon: ImageVector,title: String,body: String) { Column(Modifier.fillMaxWidth().padding(vertical=24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Icon(icon,null,Modifier.size(32.dp),tint=MaterialTheme.colorScheme.primary);Text(title,style=MaterialTheme.typography.titleLarge);Text(body,color=MaterialTheme.colorScheme.onSurfaceVariant)} }
