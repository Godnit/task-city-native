// Network Academy v7 — gateway verifier hotfix
(function(){
  const previous=window.verifyLessonLab;
  if(typeof previous!=='function')return;
  window.verifyLessonLab=function(i){
    if(i!==5)return previous(i);
    const nodes=LAB.state.nodes||[];
    const pc=nodes.find(n=>n.name==='PC-1'||n.type==='pc');
    const internet=nodes.find(n=>n.type==='cloud'||n.name==='Internet');
    const ok=!!(pc&&internet&&pc.gateway==='192.168.1.1'&&LAB.canPing(pc.id,internet.id).ok);
    const box=document.getElementById('guideResult');
    if(ok){
      progress.labs=Array.isArray(progress.labs)?progress.labs:[];
      if(!progress.labs.includes(i)){progress.labs.push(i);saveProgress()}
      if(box)box.innerHTML='<div class="guideSuccess">✓ نجحت. PC‑1 عرف أن الهدف خارج LAN فأرسل الحزمة إلى Gateway 192.168.1.1، ثم مرّت عبر Router إلى Internet.</div>';
      toast('✅ نجحت مهمة Default Gateway');
    }else{
      if(box)box.innerHTML='<div class="guideFail">راجع PC‑1: يجب أن يكون Gateway = 192.168.1.1، ثم اختر PC‑1 مصدرًا وInternet هدفًا واختبر Ping.</div>';
      toast('راجع Gateway في PC‑1 ثم Ping إلى Internet');
    }
  };
})();
