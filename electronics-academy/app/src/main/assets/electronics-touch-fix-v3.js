/* Electronics Academy v3 — robust Android touch layer */
(function(){
'use strict';
const A=()=>window.EA_PHASE1;
let drag=null, wireDrag=null;
const clamp=(v,a,b)=>Math.max(a,Math.min(b,v));
function node(id){const a=A();return a&&a.E.nodes.find(n=>n.id===+id)}
function def(n){const a=A();return n&&a&&a.EA_PARTS[n.type]}
function terminalPos(n,pid){const d=def(n),p=d&&d.ports.find(x=>x[0]===pid),side=p?p[3]:'right';return {x:n.x+(side==='right'?d.w/2:-d.w/2),y:n.y}}
function curve(a,b){const dx=Math.max(60,Math.abs(b.x-a.x)*.45),c1=a.x+(b.x>=a.x?dx:-dx),c2=b.x-(b.x>=a.x?dx:-dx);return `M ${a.x} ${a.y} C ${c1} ${a.y}, ${c2} ${b.y}, ${b.x} ${b.y}`}
function signalForWire(w){const a=A(),na=node(w.a),nb=node(w.b);if(!a||!na||!nb)return 'neutral';const pa=a.EA_PARTS[na.type].ports.find(p=>p[0]===w.ap),pb=a.EA_PARTS[nb.type].ports.find(p=>p[0]===w.bp);return pa&&pa[2]!=='neutral'?pa[2]:pb&&pb[2]!=='neutral'?pb[2]:'neutral'}
function redrawWiresOnly(){const a=A(),svg=document.getElementById('eaWires');if(!a||!svg)return;let html='';for(const w of a.E.wires){const na=node(w.a),nb=node(w.b);if(!na||!nb)continue;html+=`<path class="eaWire ${signalForWire(w)}" d="${curve(terminalPos(na,w.ap),terminalPos(nb,w.bp))}"/>`}
 if(a.E.tempWire){html+=`<path class="eaWire ${a.E.tempWire.cls||'neutral'} eaTempWire" d="${curve(a.E.tempWire.a,a.E.tempWire.b)}"/>`}
 svg.innerHTML=html;
}
function screenToWorld(cx,cy){const a=A(),v=document.getElementById('eaViewport');if(!a||!v)return {x:0,y:0};const r=v.getBoundingClientRect();return {x:(cx-r.left-a.E.panX)/a.E.zoom,y:(cy-r.top-a.E.panY)/a.E.zoom}}
function worldToScreen(p){const a=A(),v=document.getElementById('eaViewport');if(!a||!v)return {x:0,y:0};const r=v.getBoundingClientRect();return {x:r.left+a.E.panX+p.x*a.E.zoom,y:r.top+a.E.panY+p.y*a.E.zoom}}
function nearestPort(cx,cy,src){const a=A();if(!a)return null;let best=null,dist=Infinity;for(const n of a.E.nodes){for(const p of a.EA_PARTS[n.type].ports){if(src&&n.id===src.node&&p[0]===src.port)continue;const sp=worldToScreen(terminalPos(n,p[0])),d=Math.hypot(cx-sp.x,cy-sp.y);if(d<dist){dist=d;best={node:n.id,port:p[0],distance:d}}}}return best&&best.distance<=52?best:null}
function syncGuide(){const a=A(),g=document.getElementById('eaGuideSteps');if(!a||!g)return;const types=a.E.nodes.map(n=>n.type),hasB=types.includes('battery'),hasR=types.includes('resistor'),hasL=types.includes('led');const labels=['ضع بطارية','أضف مقاومة','أضف LED','اسحب الأسلاك بين النقاط','شغّل بأمان'];const st=[hasB,hasR,hasL,a.E.wires.length>=3,!!localStorage.getItem('ea_phase1_lab')];g.innerHTML=labels.map((x,i)=>`<span class="eaStep ${st[i]?'ok':''}">${st[i]?'✓ ':String(i+1)+' '}${x}</span>`).join('')}
function showWireHint(txt,ok=true){const h=document.getElementById('eaContextHint');if(h){h.className='eaHintBox '+(ok?'':'warn');h.textContent=txt}}

/* Let the existing component pointerdown select the component/refresh Inspector.
   We only intercept pointermove so the old code cannot rebuild DOM mid-drag. */
document.addEventListener('pointerdown',e=>{
 const a=A();if(!a)return;
 const term=e.target.closest&&e.target.closest('.terminal');
 if(term){wireDrag={pointerId:e.pointerId,src:{node:+term.dataset.node,port:term.dataset.port}};return}
 const el=e.target.closest&&e.target.closest('.eaComponent');
 if(!el)return;
 const n=node(el.dataset.id);if(!n)return;
 drag={pointerId:e.pointerId,node:n.id,startX:e.clientX,startY:e.clientY,ox:n.x,oy:n.y,el,moved:false};
},false);

document.addEventListener('pointermove',e=>{
 const a=A();if(!a)return;
 if(drag&&e.pointerId===drag.pointerId){
   e.preventDefault();e.stopImmediatePropagation();
   const n=node(drag.node);if(!n)return;
   const dx=(e.clientX-drag.startX)/a.E.zoom,dy=(e.clientY-drag.startY)/a.E.zoom;
   if(Math.hypot(dx,dy)>2)drag.moved=true;
   n.x=clamp(drag.ox+dx,70,1130);n.y=clamp(drag.oy+dy,70,790);
   const el=document.querySelector(`.eaComponent[data-id="${n.id}"]`);
   if(el){el.style.left=n.x+'px';el.style.top=n.y+'px'}
   redrawWiresOnly();
   return;
 }
 if(wireDrag&&e.pointerId===wireDrag.pointerId){
   e.preventDefault();e.stopImmediatePropagation();
   const src=node(wireDrag.src.node);if(!src)return;
   const d=def(src),pd=d.ports.find(p=>p[0]===wireDrag.src.port);
   a.E.dragPort={...wireDrag.src};
   a.E.tempWire={a:terminalPos(src,wireDrag.src.port),b:screenToWorld(e.clientX,e.clientY),cls:pd?pd[2]:'neutral'};
   redrawWiresOnly();
   const near=nearestPort(e.clientX,e.clientY,wireDrag.src);
   document.querySelectorAll('.terminal.eaDropTarget').forEach(x=>x.classList.remove('eaDropTarget'));
   if(near){const t=document.querySelector(`.terminal[data-node="${near.node}"][data-port="${near.port}"]`);if(t)t.classList.add('eaDropTarget')}
 }
},{capture:true,passive:false});

document.addEventListener('pointerup',e=>{
 const a=A();if(!a)return;
 if(drag&&e.pointerId===drag.pointerId){
   drag=null;
   /* Existing pointerup now runs normally and safely rebuilds once, after dragging ends. */
   return;
 }
 if(wireDrag&&e.pointerId===wireDrag.pointerId){
   const src=wireDrag.src,near=nearestPort(e.clientX,e.clientY,src);
   if(near&&near.node!==src.node){
     const duplicate=a.E.wires.some(w=>(w.a===src.node&&w.ap===src.port&&w.b===near.node&&w.bp===near.port)||(w.b===src.node&&w.bp===src.port&&w.a===near.node&&w.ap===near.port));
     if(!duplicate){a.E.wires.push({a:src.node,ap:src.port,b:near.node,bp:near.port});showWireHint('✓ تم توصيل السلك. يمكنك الآن سحب سلك آخر من أي نقطة توصيل.',true)}
   }else showWireHint('قرّب نهاية السلك من نقطة التوصيل الملونة حتى تتوهج ثم ارفع إصبعك.',false);
   a.E.dragPort=null;a.E.tempWire=null;a.E.powered=false;wireDrag=null;
   document.querySelectorAll('.terminal.eaDropTarget').forEach(x=>x.classList.remove('eaDropTarget'));
   redrawWiresOnly();syncGuide();
   /* Do not block the old once:pointerup handler: it removes itself and performs one clean render. */
 }
},{capture:true,passive:false});

document.addEventListener('pointercancel',e=>{if(drag&&drag.pointerId===e.pointerId)drag=null;if(wireDrag&&wireDrag.pointerId===e.pointerId){wireDrag=null;const a=A();if(a){a.E.dragPort=null;a.E.tempWire=null}redrawWiresOnly()}},{capture:true});

/* Visible touch feedback and easier discovery. */
document.addEventListener('pointerdown',e=>{const t=e.target.closest&&e.target.closest('.terminal');if(t){document.querySelectorAll('.terminal').forEach(x=>x.classList.remove('eaSourcePort'));t.classList.add('eaSourcePort')}},{capture:true});

/* Prevent accidental page scrolling while a component/terminal is being manipulated. */
document.addEventListener('touchmove',e=>{if(drag||wireDrag)e.preventDefault()},{passive:false,capture:true});

window.EA_TOUCH_FIX_V3={redrawWiresOnly,nearestPort};
})();
