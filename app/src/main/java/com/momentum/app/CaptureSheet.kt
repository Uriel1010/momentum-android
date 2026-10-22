package com.momentum.app
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CaptureSheet(initial:String,projectId:String?,projects:List<Project>,onDismiss:()->Unit,onSave:(Task)->Unit) {
 var title by rememberSaveable{mutableStateOf(initial)}
 var step by rememberSaveable{mutableStateOf("")}
 var estimate by rememberSaveable{mutableStateOf<Int?>(null)}
 var details by rememberSaveable{mutableStateOf(false)}
 val focus=remember{FocusRequester()}
 val keyboard=LocalSoftwareKeyboardController.current
 val sheet=rememberModalBottomSheetState(skipPartiallyExpanded=true)
 fun save(){if(title.isNotBlank()){keyboard?.hide();onSave(Task(title=title.trim(),projectId=projectId,nextAction=step.trim(),estimate=estimate))}}
 ModalBottomSheet(onDismissRequest=onDismiss,sheetState=sheet) {
  val window=LocalWindowInfo.current
  LaunchedEffect(sheet.currentValue,window.isWindowFocused){
   if(sheet.currentValue==SheetValue.Expanded && window.isWindowFocused){
    withFrameNanos{};focus.requestFocus();keyboard?.show()
   }
  }
  Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
   Heading(tr(R.string.v3_capture))
   projectId?.let{id->projects.find{it.id==id}?.let{Eyebrow(it.title)}}
   OutlinedTextField(title,{title=it},Modifier.fillMaxWidth().focusRequester(focus).testTag("quickCapture"),label={Text(tr(R.string.capture))},maxLines=5,keyboardOptions=KeyboardOptions(imeAction=ImeAction.Done),keyboardActions=KeyboardActions(onDone={save()}))
   PrimaryAction(tr(R.string.save),::save,enabled=title.isNotBlank())
   TextButton({details=!details}){Text(tr(R.string.v3_details))}
   if(details){
    OutlinedTextField(step,{step=it},Modifier.fillMaxWidth(),label={Text(tr(R.string.next_action))})
    Choices(listOf("" to tr(R.string.v3_no_estimate),"2" to "2 "+tr(R.string.minutes),"5" to "5 "+tr(R.string.minutes),"15" to "15 "+tr(R.string.minutes)),estimate?.toString()?:"",{estimate=it.toIntOrNull()})
   }
  }
 }

}
