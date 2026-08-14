(function(){
'use strict';
let tries=0;
function run(){
  tries++;
  try{
    const svg=document.getElementById('world');
    if(!svg||!svg.querySelector('.p2country')){
      if(tries<30)setTimeout(run,60);
      return;
    }
    const paths=[...svg.querySelectorAll('.p2admin')];
    if(paths.length>1){
      const d=paths.map(p=>p.getAttribute('d')||'').filter(Boolean).join(' ');
      if(d){
        const merged=document.createElementNS('http://www.w3.org/2000/svg','path');
        merged.setAttribute('class','p2admin p20AdminMerged');
        merged.setAttribute('d',d);
        paths[0].parentNode.insertBefore(merged,paths[0]);
        paths.forEach(p=>p.remove());
      }
    }
    const loading=document.getElementById('p2loading');
    if(loading&&loading.classList.contains('hide'))loading.remove();
    window.__p20Optimized=true;
  }catch(_){ if(tries<30)setTimeout(run,60); }
}
run();
})();
