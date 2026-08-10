from pathlib import Path
p=Path('app/src/main/assets/web/index.html')
s=p.read_text()
s=s.replace('<body>\n<div id="hint">','''<body>
<div id="debug" style="display:none;position:absolute;top:6px;left:6px;right:6px;z-index:99;background:#8b1e1e;color:white;padding:8px;font:12px monospace;direction:ltr"></div>
<div id="hint">''',1)
s=s.replace('<script src="three.min.js"></script>','''<script>
window.onerror=function(msg,src,line,col,err){
  var d=document.getElementById('debug');
  if(d){d.style.display='block';d.textContent='JS ERROR: '+msg+' @'+line+':'+col;}
  document.body.dataset.error=String(msg);
};
</script>
<script src="three.min.js"></script>''',1)
s=s.replace("document.title='TASKCITY_THREE_READY';","document.body.dataset.ready='three-ok';document.title='TASKCITY_THREE_READY';",1)
p.write_text(s)
print('THREE_HTML_DIAGNOSTICS_OK')
