(function(){
'use strict';
const NS='http://www.w3.org/2000/svg', W=1440, H=720;
let vp,svg,cam,shield,zoomText,k=1,cx=W/2,cy=H/2,drag=null,pinch=null,raf=0,moved=false,installed=false;
const clamp=(v,a,b)=>Math.max(a,Math.min(b,v));
function active(){return vp&&document.getElementById('map')?.classList.contains('active')&&vp.clientWidth>100&&vp.clientHeight>100}
function clampCam(){const vw=W/k,vh=H/k;cx=vw>=W?W/2:clamp(cx,vw/2,W-vw/2);cy=vh>=H?H/2:clamp(cy,vh/2,H-vh/2)}
function draw(){raf=0;if(!active())return;clampCam();cam.setAttribute('transform',`translate(${W/2} ${H/2}) scale(${k}) translate(${-cx} ${-cy})`);if(zoomText)zoomText.textContent=Math.round(k*100)+'%'}
function frame(){if(!raf)raf=requestAnimationFrame(draw)}
function fit(){k=1;cx=W/2;cy=H/2;draw();restyle();document.getElementById('regionCard')?.classList.remove('show')}
function screenWorld(x,y){return{x:cx-W/(2*k)+(x/vp.clientWidth)*(W/k),y:cy-H/(2*k)+(y/vp.clientHeight)*(H/k)}}
function zoomAt(f,x,y,finish=true){const a=screenWorld(x,y),nk=clamp(k*f,1,48);k=nk;cx=a.x+(W/2-(x/vp.clientWidth)*W)/k;cy=a.y+(H/2-(y/vp.clientHeight)*H)/k;draw();if(finish)restyle()}
function local(t){const r=vp.getBoundingClientRect();return{x:t.clientX-r.left,y:t.clientY-r.top,cx:t.clientX,cy:t.clientY}}
function clickMap(x,y){shield.style.pointerEvents='none';const e=document.elementFromPoint(x,y);shield.style.pointerEvents='auto';const c=e?.closest?.('.p2country');if(c)c.dispatchEvent(new MouseEvent('click',{bubbles:true,clientX:x,clientY:y}))}
function focusPlayer(){const code=(typeof game!=='undefined'&&game.me)||'YEM',e=cam.querySelector(`.p2country[data-code="${code}"]`);if(!e)return fit();try{const b=e.getBBox();k=6;cx=b.x+b.width/2;cy=b.y+b.height/2;draw();restyle()}catch(_){fit()}}
function setImp(e,p,v){e?.style.setProperty(p,v,'important')}
function restyle(){if(!cam)return;const t=k<1.7?0:k<3?1:k<5?2:k<9?3:k<16?4:5;
 const countryPx=[12,10,8,6.5,0,0][t];cam.querySelectorAll('.p2label').forEach(e=>{const on=countryPx>0;setImp(e,'opacity',on?(t>=3?'.72':'.96'):'0');if(on){setImp(e,'font-size',(countryPx/k).toFixed(4)+'px');setImp(e,'stroke-width',(.85/k).toFixed(4)+'px')}});
 const major=cam.querySelector('#p12MajorCities');if(major){major.style.display=t>=1?'block':'none';let i=0;major.querySelectorAll('g').forEach(g=>{const on=t>=3||(t===2?i%2===0:t===1?i%4===0:false);g.style.display=on?'block':'none';if(on){const tx=g.querySelector('text'),dot=g.querySelector('circle');if(tx){setImp(tx,'font-size',((t>=4?8:7.2)/k).toFixed(4)+'px');setImp(tx,'stroke-width',(.55/k).toFixed(4)+'px')}if(dot)dot.setAttribute('r',(1.25/k).toFixed(4))}i++})}
 const yg=cam.querySelector('#p12YemGov');if(yg){yg.style.display=t>=3?'block':'none';if(t>=3)yg.querySelectorAll('text').forEach(tx=>{setImp(tx,'font-size',(6.5/k).toFixed(4)+'px');setImp(tx,'stroke-width',(.45/k).toFixed(4)+'px')})}
 cam.querySelectorAll('#p9SeaLabels text').forEach(e=>{setImp(e,'opacity',t<=1?'.2':'0');setImp(e,'font-size',((t===0?13:10)/k).toFixed(4)+'px')});
 cam.querySelectorAll('.p2country').forEach(e=>{setImp(e,'stroke-width','.58px');setImp(e,'vector-effect','non-scaling-stroke');setImp(e,'shape-rendering','geometricPrecision')});
 const adm=cam.querySelector('#p9AdminMerged');if(adm){setImp(adm,'display',t>=1?'block':'none');setImp(adm,'stroke-width','.3px');setImp(adm,'vector-effect','non-scaling-stroke')}
 cam.querySelectorAll('#p11YemAdm1 path').forEach(e=>{setImp(e,'stroke-width','.34px');setImp(e,'vector-effect','non-scaling-stroke')});
 cam.querySelectorAll('#p11YemAdm2 path').forEach(e=>{setImp(e,'stroke-width','.2px');setImp(e,'vector-effect','non-scaling-stroke')});
 if(typeof game!=='undefined'&&game.lang==='ar')cam.querySelectorAll('.p2cityName').forEach(e=>{if(/[A-Za-z]/.test(e.textContent||''))setImp(e,'display','none')});
 const st=document.querySelector('.p9status,.p6status');if(st)st.textContent=t<1?'الدول':t<3?'الأقاليم والمدن':'المدن والتفاصيل';
}
function installCSS(){if(document.getElementById('p15style'))return;const s=document.createElement('style');s.id='p15style';s.textContent=`#viewport{direction:ltr!important;position:relative!important;overflow:hidden!important;touch-action:none!important;background:#2b7681!important}#viewport>#worldWrap{position:absolute!important;inset:0!important;width:100%!important;height:100%!important;transform:none!important}#world{width:100%!important;height:100%!important;display:block!important;overflow:hidden!important;shape-rendering:geometricPrecision!important;text-rendering:geometricPrecision!important}#p15shield{position:absolute!important;inset:0!important;z-index:7!important;background:transparent!important;touch-action:none!important}.p2tools{z-index:20!important}`;(document.head||document.documentElement).appendChild(s)}
function wrapMap(){let old=svg.querySelector('#p15Camera');if(old){cam=old;return}cam=document.createElementNS(NS,'g');cam.id='p15Camera';const nodes=[...svg.childNodes];for(const n of nodes)cam.appendChild(n);svg.appendChild(cam)}
function replaceButtons(){let bs=[...document.querySelectorAll('.p2tool')];if(bs.length<4)return;bs=bs.slice(0,4).map(x=>{const n=x.cloneNode(true);x.replaceWith(n);return n});bs[0].onclick=()=>zoomAt(1.4,vp.clientWidth/2,vp.clientHeight/2,true);bs[1].onclick=()=>zoomAt(1/1.4,vp.clientWidth/2,vp.clientHeight/2,true);bs[2].onclick=fit;bs[3].onclick=focusPlayer}
function installEvents(){shield.addEventListener('touchstart',e=>{e.preventDefault();moved=false;if(e.touches.length===1){const a=local(e.touches[0]);drag={x:a.x,y:a.y,cx:a.cx,cy:a.cy,sx:a.x,sy:a.y};pinch=null}else if(e.touches.length===2){const a=local(e.touches[0]),b=local(e.touches[1]),mx=(a.x+b.x)/2,my=(a.y+b.y)/2;pinch={dist:Math.hypot(a.x-b.x,a.y-b.y),start:k,anchor:screenWorld(mx,my)};drag=null}},{passive:false});
 shield.addEventListener('touchmove',e=>{e.preventDefault();if(e.touches.length===1&&drag){const a=local(e.touches[0]),dx=a.x-drag.x,dy=a.y-drag.y;if(Math.abs(a.x-drag.sx)+Math.abs(a.y-drag.sy)>5)moved=true;cx-=dx*(W/vp.clientWidth)/k;cy-=dy*(H/vp.clientHeight)/k;drag.x=a.x;drag.y=a.y;frame()}else if(e.touches.length===2&&pinch){moved=true;const a=local(e.touches[0]),b=local(e.touches[1]),mx=(a.x+b.x)/2,my=(a.y+b.y)/2,d=Math.hypot(a.x-b.x,a.y-b.y);k=clamp(pinch.start*d/Math.max(1,pinch.dist),1,48);cx=pinch.anchor.x+(W/2-(mx/vp.clientWidth)*W)/k;cy=pinch.anchor.y+(H/2-(my/vp.clientHeight)*H)/k;frame()}},{passive:false});
 shield.addEventListener('touchend',e=>{if(!e.touches.length){const tap=drag&&!moved?{x:drag.cx,y:drag.cy}:null,wasPinch=!!pinch;drag=null;pinch=null;draw();if(wasPinch)restyle();if(tap)clickMap(tap.x,tap.y)}else if(e.touches.length===1){const a=local(e.touches[0]);drag={x:a.x,y:a.y,cx:a.cx,cy:a.cy,sx:a.x,sy:a.y};pinch=null}},{passive:false});
 shield.addEventListener('touchcancel',()=>{drag=null;pinch=null;draw();restyle()},{passive:false})}
function install(){if(installed)return;const old=document.getElementById('viewport');if(!old||old.clientWidth<100||!old.querySelector('#world')||!old.querySelector('.p2country'))return setTimeout(install,180);const n=old.cloneNode(true);old.parentNode.replaceChild(n,old);vp=n;svg=vp.querySelector('#world');zoomText=document.getElementById('zoomText');installCSS();svg.setAttribute('viewBox','0 0 1440 720');svg.setAttribute('preserveAspectRatio','none');svg.removeAttribute('width');svg.removeAttribute('height');vp.querySelectorAll('.p9shield,#p13shield,#p14shield,#p15shield').forEach(e=>e.remove());wrapMap();shield=document.createElement('div');shield.id='p15shield';vp.appendChild(shield);installEvents();replaceButtons();installed=true;k=1;cx=W/2;cy=H/2;draw();restyle();
 const ob=new MutationObserver(()=>{if(svg.getAttribute('viewBox')!=='0 0 1440 720')svg.setAttribute('viewBox','0 0 1440 720')});ob.observe(svg,{attributes:true,attributeFilter:['viewBox']});
 const map=document.getElementById('map');if(map)new MutationObserver(()=>{if(map.classList.contains('active'))setTimeout(()=>{draw();restyle()},80)}).observe(map,{attributes:true,attributeFilter:['class']});
 window.addEventListener('resize',()=>setTimeout(()=>{draw();restyle()},60));
}
setTimeout(install,1700);
})();
