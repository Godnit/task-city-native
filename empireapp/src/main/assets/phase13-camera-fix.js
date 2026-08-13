(function(){
'use strict';
const WW=1440,WH=720;
let vp,svg,shield,zoomText;
let scale=1,minScale=1,maxScale=50,camX=WW/2,camY=WH/2,raf=0;
let drag=null,pinch=null,moved=false,lastTier=-1,installed=false,expectedVB='0 0 1440 720';
function clamp(v,a,b){return Math.max(a,Math.min(b,v))}
function mapActive(){return document.getElementById('map')?.classList.contains('active')}
function recalc(){if(!vp||vp.clientWidth<100||vp.clientHeight<100)return false;minScale=vp.clientWidth/WW;if(!Number.isFinite(minScale)||minScale<=0)minScale=.6;maxScale=Math.max(50,minScale*50);return true}
function clampCam(){const vw=vp.clientWidth/scale,vh=vp.clientHeight/scale;camX=vw>=WW?WW/2:clamp(camX,vw/2,WW-vw/2);camY=vh>=WH?WH/2:clamp(camY,vh/2,WH-vh/2)}
function render(){raf=0;if(!recalc())return;clampCam();const vw=vp.clientWidth/scale,vh=vp.clientHeight/scale,x=camX-vw/2,y=camY-vh/2;expectedVB=`${x.toFixed(4)} ${y.toFixed(4)} ${vw.toFixed(4)} ${vh.toFixed(4)}`;svg.setAttribute('viewBox',expectedVB);svg.setAttribute('preserveAspectRatio','none');if(zoomText)zoomText.textContent=Math.round(scale*100)+'%'}
function queue(){if(!raf)raf=requestAnimationFrame(render)}
function fit(){if(!recalc())return;scale=minScale;camX=WW/2;camY=WH/2;lastTier=-1;render();applyTier(true);document.getElementById('regionCard')?.classList.remove('show')}
function worldAt(x,y){const vw=vp.clientWidth/scale,vh=vp.clientHeight/scale;return{x:camX-vw/2+x/scale,y:camY-vh/2+y/scale}}
function zoomAt(f,x,y,finish=true){if(!recalc())return;const a=worldAt(x,y),ns=clamp(scale*f,minScale,maxScale);scale=ns;camX=a.x+(vp.clientWidth/2-x)/scale;camY=a.y+(vp.clientHeight/2-y)/scale;render();if(finish)applyTier(false)}
function ratio(){return scale/Math.max(minScale,.0001)}
function tier(){const r=ratio();return r<1.4?0:r<2.2?1:r<3.4?2:r<5?3:r<8?4:r<13?5:r<22?6:7}
function setImp(el,p,v){el?.style.setProperty(p,v,'important')}
function arabicOnly(){if(typeof game==='undefined'||game.lang!=='ar')return;svg.querySelectorAll('.p2cityName').forEach(t=>{if(/[A-Za-z]/.test(t.textContent||''))setImp(t,'display','none')});svg.querySelectorAll('#p11YemLabels text').forEach(t=>setImp(t,'display','none'))}
function applyTier(force){if(!svg)return;const t=tier();if(!force&&t===lastTier)return;lastTier=t;
 const countryPx=[10,9.4,8.8,8,7,5.8,0,0][t];
 svg.querySelectorAll('.p2label').forEach(el=>{const on=countryPx>0;setImp(el,'opacity',on?(t>=4?'.68':'.96'):'0');if(on){setImp(el,'font-size',(countryPx/scale).toFixed(4)+'px');setImp(el,'stroke-width',(.8/scale).toFixed(4)+'px')}});
 if(typeof game!=='undefined'&&game.lang==='ar')svg.querySelectorAll('.p2city').forEach(e=>setImp(e,'display','none'));
 const cg=document.getElementById('p12MajorCities');if(cg){const show=t>=2;cg.style.display=show?'block':'none';let i=0;cg.querySelectorAll('g').forEach(g=>{let on=false;if(show){if(t>=4)on=true;else if(t===3)on=i%2===0;else on=i%4===0}g.style.display=on?'block':'none';if(on){const tx=g.querySelector('text'),dot=g.querySelector('circle'),fpx=t>=6?7.8:t>=4?7.2:6.7;if(tx){setImp(tx,'font-size',(fpx/scale).toFixed(4)+'px');setImp(tx,'stroke-width',(.62/scale).toFixed(4)+'px')}if(dot)dot.setAttribute('r',(1.25/scale).toFixed(4))}i++})}
 const yg=document.getElementById('p12YemGov');if(yg){const on=t>=5;yg.style.display=on?'block':'none';if(on)yg.querySelectorAll('text').forEach(tx=>{setImp(tx,'font-size',(6.3/scale).toFixed(4)+'px');setImp(tx,'stroke-width',(.45/scale).toFixed(4)+'px')})}
 svg.querySelectorAll('#p9SeaLabels text').forEach(e=>{setImp(e,'opacity',t<=2?'.22':'0');setImp(e,'font-size',((t===0?12:10)/scale).toFixed(4)+'px')});
 svg.querySelectorAll('.p2country').forEach(e=>{setImp(e,'stroke-width','.55px');setImp(e,'vector-effect','non-scaling-stroke');setImp(e,'shape-rendering','geometricPrecision')});
 const adm=document.getElementById('p9AdminMerged');if(adm){setImp(adm,'display',t>=2?'block':'none');setImp(adm,'stroke-width','.28px');setImp(adm,'vector-effect','non-scaling-stroke');setImp(adm,'shape-rendering','geometricPrecision')}
 svg.querySelectorAll('#p11YemAdm1 path').forEach(e=>{setImp(e,'stroke-width','.34px');setImp(e,'vector-effect','non-scaling-stroke')});
 svg.querySelectorAll('#p11YemAdm2 path').forEach(e=>{setImp(e,'stroke-width','.18px');setImp(e,'vector-effect','non-scaling-stroke')});
 arabicOnly();
 const st=document.querySelector('.p9status,.p6status');if(st)st.textContent=t<2?'الدول':t<5?'الأقاليم والمدن':'المدن والتفاصيل';
}
function point(e){const b=vp.getBoundingClientRect();return{x:e.clientX-b.left,y:e.clientY-b.top,cx:e.clientX,cy:e.clientY}}
function clickUnder(x,y){shield.style.pointerEvents='none';const el=document.elementFromPoint(x,y);shield.style.pointerEvents='auto';const c=el?.closest?.('.p2country');if(c)c.dispatchEvent(new MouseEvent('click',{bubbles:true,clientX:x,clientY:y}))}
function installTouch(){const old=document.querySelector('.p9shield');if(!old)return false;shield=old.cloneNode(false);shield.className='p9shield';shield.id='p13shield';old.replaceWith(shield);shield.style.pointerEvents='auto';shield.style.touchAction='none';
 shield.addEventListener('touchstart',e=>{e.preventDefault();moved=false;if(e.touches.length===1){const p=point(e.touches[0]);drag={x:p.x,y:p.y,cx:p.cx,cy:p.cy,sx:p.x,sy:p.y}}else if(e.touches.length===2){drag=null;const a=point(e.touches[0]),b=point(e.touches[1]),mx=(a.x+b.x)/2,my=(a.y+b.y)/2;pinch={dist:Math.hypot(a.x-b.x,a.y-b.y),start:scale,anchor:worldAt(mx,my)}}},{passive:false});
 shield.addEventListener('touchmove',e=>{e.preventDefault();if(e.touches.length===1&&drag){const p=point(e.touches[0]),dx=p.x-drag.x,dy=p.y-drag.y;if(Math.abs(p.x-drag.sx)+Math.abs(p.y-drag.sy)>5)moved=true;camX-=dx/scale;camY-=dy/scale;drag.x=p.x;drag.y=p.y;queue()}else if(e.touches.length===2&&pinch){moved=true;const a=point(e.touches[0]),b=point(e.touches[1]),mx=(a.x+b.x)/2,my=(a.y+b.y)/2,d=Math.hypot(a.x-b.x,a.y-b.y);scale=clamp(pinch.start*d/Math.max(1,pinch.dist),minScale,maxScale);camX=pinch.anchor.x+(vp.clientWidth/2-mx)/scale;camY=pinch.anchor.y+(vp.clientHeight/2-my)/scale;queue()}},{passive:false});
 shield.addEventListener('touchend',e=>{if(e.touches.length===0){const was=drag;drag=null;pinch=null;render();applyTier(false);if(was&&!moved)clickUnder(was.cx,was.cy)}else if(e.touches.length===1){const p=point(e.touches[0]);drag={x:p.x,y:p.y,cx:p.cx,cy:p.cy,sx:p.x,sy:p.y};pinch=null}},{passive:false});
 shield.addEventListener('touchcancel',()=>{drag=null;pinch=null;render();applyTier(false)},{passive:false});return true}
function replaceButtons(){const bs=[...document.querySelectorAll('.p2tool')];if(bs.length<4)return;const fresh=bs.slice(0,4).map(b=>{const n=b.cloneNode(true);b.replaceWith(n);return n});fresh[0].onclick=()=>zoomAt(1.45,vp.clientWidth/2,vp.clientHeight/2,true);fresh[1].onclick=()=>zoomAt(1/1.45,vp.clientWidth/2,vp.clientHeight/2,true);fresh[2].onclick=fit;fresh[3].onclick=()=>{const code=(typeof game!=='undefined'&&game.me)||'YEM',el=svg.querySelector(`.p2country[data-code="${code}"]`);if(!el)return;const b=el.getBBox();scale=clamp(Math.max(minScale*6,6),minScale,maxScale);camX=b.x+b.width/2;camY=b.y+b.height/2;render();applyTier(true)}}
function installCSS(){if(document.getElementById('p13style'))return;const s=document.createElement('style');s.id='p13style';s.textContent=`#viewport{direction:ltr!important;position:relative!important;overflow:hidden!important;touch-action:none!important;background:#2b7681!important}#viewport>#worldWrap{position:absolute!important;inset:0!important;width:100%!important;height:100%!important;margin:0!important;padding:0!important;transform:none!important;will-change:auto!important}#world{width:100%!important;height:100%!important;display:block!important;overflow:hidden!important;shape-rendering:geometricPrecision!important;text-rendering:geometricPrecision!important}#world .p2country{stroke:#263a39!important;stroke-width:.55px!important;vector-effect:non-scaling-stroke!important;shape-rendering:geometricPrecision!important}#p9AdminMerged{stroke-width:.28px!important;vector-effect:non-scaling-stroke!important;shape-rendering:geometricPrecision!important}#p13shield{position:absolute!important;left:0!important;right:0!important;top:48px!important;bottom:58px!important;z-index:12!important;background:transparent!important}.p2tools{z-index:20!important}`;(document.head||document.documentElement).appendChild(s)}
function install(){if(installed)return;vp=document.getElementById('viewport');svg=document.getElementById('world');zoomText=document.getElementById('zoomText');if(!vp||!svg||!zoomText)return setTimeout(install,150);installCSS();if(!installTouch())return setTimeout(install,150);replaceButtons();installed=true;
 const obs=new MutationObserver(()=>{const now=svg.getAttribute('viewBox')||'';if(now!==expectedVB)svg.setAttribute('viewBox',expectedVB)});obs.observe(svg,{attributes:true,attributeFilter:['viewBox']});
 const map=document.getElementById('map');if(map)new MutationObserver(()=>{if(mapActive())setTimeout(()=>{fit();applyTier(true)},30)}).observe(map,{attributes:true,attributeFilter:['class']});
 const oldLang=window.setLang;if(oldLang&&!oldLang.__p13){const f=function(l){oldLang(l);setTimeout(()=>applyTier(true),60)};f.__p13=true;window.setLang=f}
 window.addEventListener('resize',()=>{if(mapActive())setTimeout(fit,40)});if(mapActive())fit();else{recalc();scale=minScale;camX=WW/2;camY=WH/2;render()}applyTier(true)}
setTimeout(install,1250);
})();
