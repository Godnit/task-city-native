from pathlib import Path
import sys
root=Path(sys.argv[1] if len(sys.argv)>1 else '.')
chart=root/'app/src/main/java/com/maimon/xaulab/ChartView.kt'
s=chart.read_text()
s=s.replace('val left=0f;val right=width-context.dp(78);val top=context.dp(8).toFloat();val bottom=height-context.dp(34).toFloat()', 'val left=0f;val right=(width-context.dp(78)).toFloat();val top=context.dp(8).toFloat();val bottom=(height-context.dp(34)).toFloat()')
chart.write_text(s)
main=root/'app/src/main/java/com/maimon/xaulab/MainActivity.kt'
s=main.read_text()
s=s.replace('padding=0}', 'setPadding(0,0,0,0)}')
s=s.replace('private fun topBar(title:String, actions:List<Pair<String,()->Unit>>=emptyList()):View{', 'private fun topBar(title: String, actions: List<Pair<String, () -> Unit>> = emptyList()): View {')
s=s.replace('val close=actionButton("←"){chartView?.clearSelection();visibility=View.GONE}', 'val close=actionButton("←"){chartView?.clearSelection();row.visibility=View.GONE}')
s=s.replace('val play=actionButton(if(replayPlaying)"Ⅱ":"▶"){replayPlaying=!replayPlaying;render(Screen.REPLAY);if(replayPlaying)scheduleReplay()}', 'val play=actionButton(if (replayPlaying) "Ⅱ" else "▶"){replayPlaying=!replayPlaying;render(Screen.REPLAY);if(replayPlaying)scheduleReplay()}')
main.write_text(s)
