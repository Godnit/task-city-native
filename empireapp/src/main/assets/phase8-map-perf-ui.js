(function(){
'use strict';
const NS='http://www.w3.org/2000/svg';
let svg=null,vp=null,wrap=null,zoomText=null,zoomObserver=null,lastZoomBucket='';
function isAR(){return typeof game!=='undefined'&&game.lang==='ar'}
function tr(a,e){return isAR()?a:e}
function proj(lon,lat){return{x:(lon+180)*4,y:(90-lat)*4}}
function inject(){
 if(document.getElementById('p8style'))return;
 const s=document.createElement('style');s.id='p8style';s.textContent=`
#viewport{direction:ltr!important;overflow:hidden!important;touch-action:none!important;background:#2b7681!important}
#viewport>#worldWrap{position:absolute!important;left:0!important;right:auto!important;top:0!important;bottom:auto!important;margin:0!important;padding:0!important;transform-origin:0 0!important;will-change:transform!important}
#world{shape-rendering:geometricPrecision!important;text-rendering:geometricPrecision!important}
#world .p2country{stroke:#2d403e!important;stroke-width:.72!important;vector-effect:non-scaling-stroke!important;stroke-linejoin:round!important;stroke-linecap:round!important}
#world .p2admin{display:none!important}
#world.p6admin #p8AdminMerged,#viewport.p6moving #world.p6admin #p8AdminMerged{display:block!important;stroke:#465653!important;stroke-width:.38!important;opacity:.78!important;fill:none!important;vector-effect:non-scaling-stroke!important;stroke-linejoin:round!important;stroke-linecap:round!important;shape-rendering:geometricPrecision!important;pointer-events:none!important}
#viewport.p6moving #world{shape-rendering:geometricPrecision!important}
#viewport.p6moving #world .p2city.p6show{display:block!important}
#p8SeaLabels{pointer-events:none!important}
#p8SeaLabels .p8sea{fill:#d2e7e8!important;stroke:none!important;opacity:.24!important;font-style:italic!important;font-weight:500!important;text-anchor:middle!important;letter-spacing:.15px!important}
#p8SeaLabels .p8sea.p8minor{opacity:.18!important}
.p6status{left:10px!important;right:auto!important;top:78px!important;min-width:0!important;width:auto!important;padding:4px 8px!important;border-radius:12px!important;font-size:9px!important;opacity:.72!important;pointer-events:none!important}
.p2tools{right:10px!important;top:92px!important;gap:5px!important}
.p2tool{width:34px!important;height:34px!important;font-size:17px!important;border-radius:7px!important}
#zoomText{font-size:11px!important;min-width:82px!important;text-align:center!important}
`;(document.head||document.documentElement).appendChild(s);
}
function mergeAdmin(){
 if(!svg||document.getElementById('p8AdminMerged'))return;
 const paths=[...svg.querySelectorAll('.p2admin')];
 if(!paths.length){setTimeout(mergeAdmin,80);return}
 const d=[];for(const p of paths){const v=p.getAttribute('d');if(v)d.push(v)}
 if(!d.length)return;
 const merged=document.createElementNS(NS,'path');merged.id='p8AdminMerged';merged.classList.add('p2admin');merged.setAttribute('d',d.join(' '));
 paths[0].parentNode.insertBefore(merged,paths[0]);for(const p of paths)p.remove();
}
function removeOldSea(){if(!svg)return;svg.querySelectorAll('.p2sea').forEach(e=>e.remove());const old=document.getElementById('p8SeaLabels');if(old)old.remove()}
function addSeaLabel(g,key,lon,lat,minor){const p=proj(lon,lat),t=document.createElementNS(NS,'text');t.classList.add('p8sea');if(minor)t.classList.add('p8minor');t.dataset.sea=key;t.setAttribute('x',p.x);t.setAttribute('y',p.y);g.appendChild(t)}
function buildSea(){
 if(!svg)return;removeOldSea();const g=document.createElementNS(NS,'g');g.id='p8SeaLabels';
 addSeaLabel(g,'med',18,35.6,false);addSeaLabel(g,'black',34.5,43.6,true);addSeaLabel(g,'red',38.2,21,true);addSeaLabel(g,'arabian',64,14,false);addSeaLabel(g,'indian',79,-20,false);addSeaLabel(g,'atlantic',-38,18,false);addSeaLabel(g,'baltic',19,57,true);addSeaLabel(g,'caspian',51,41,true);
 svg.appendChild(g);localizeSea();
}
function localizeSea(){
 const names={med:["البحر المتوسط","Mediterranean Sea"],black:["البحر الأسود","Black Sea"],red:["البحر الأحمر","Red Sea"],arabian:["بحر العرب","Arabian Sea"],indian:["المحيط الهندي","Indian Ocean"],atlantic:["المحيط الأطلسي","Atlantic Ocean"],baltic:["بحر البلطيق","Baltic Sea"],caspian:["بحر قزوين","Caspian Sea"]};
 document.querySelectorAll('#p8SeaLabels [data-sea]').forEach(e=>{const a=names[e.dataset.sea];if(a)e.textContent=isAR()?a[0]:a[1]});updateVisualScale();
}
function currentScale(){const z=zoomText&&zoomText.textContent?parseFloat(zoomText.textContent.replace(/[^0-9.]/g,'')):NaN;return Number.isFinite(z)&&z>0?z/100:1}
function updateVisualScale(){
 if(!svg)return;const sc=currentScale();const ratio=Math.max(.45,sc);
 // Screen-space label sizes remain stable. This updates only when zoom changes, never on pan.
 const countryScreen=sc<1.0?8.6:sc<1.8?9.2:9.6;
 const cityScreen=sc<2.2?7.0:7.4;
 const seaScreen=sc<1.1?10.2:sc<1.8?9.2:8.2;
 svg.querySelectorAll('.p2label').forEach(e=>{e.style.fontSize=(countryScreen/ratio).toFixed(2)+'px';e.style.strokeWidth=(1.0/ratio).toFixed(2)+'px'});
 svg.querySelectorAll('.p2cityName').forEach(e=>{e.style.fontSize=(cityScreen/ratio).toFixed(2)+'px';e.style.strokeWidth=(.9/ratio).toFixed(2)+'px'});
 svg.querySelectorAll('#p8SeaLabels .p8sea').forEach(e=>{e.style.fontSize=(seaScreen/ratio).toFixed(2)+'px'});
 const seas=document.getElementById('p8SeaLabels');if(seas)seas.style.display=sc>3.0?'none':'block';
}
function observeZoom(){
 zoomText=document.getElementById('zoomText');if(!zoomText){setTimeout(observeZoom,100);return}
 if(zoomObserver)zoomObserver.disconnect();zoomObserver=new MutationObserver(()=>{
   const v=Math.round(currentScale()*20)/20;const key=String(v);if(key===lastZoomBucket)return;lastZoomBucket=key;requestAnimationFrame(updateVisualScale);
 });
 zoomObserver.observe(zoomText,{childList:true,characterData:true,subtree:true});updateVisualScale();
}
function fixLayerOrigin(){if(!wrap)return;wrap.style.left='0px';wrap.style.right='auto';wrap.style.top='0px';wrap.style.bottom='auto'}
function patchLanguage(){const old=window.setLang;if(typeof old==='function'&&!old.__p8){const fn=function(l){old(l);setTimeout(()=>{localizeSea();updateVisualScale()},60)};fn.__p8=true;window.setLang=fn}}
function install(){
 vp=document.getElementById('viewport');wrap=document.getElementById('worldWrap');svg=document.getElementById('world');
 if(!vp||!wrap||!svg||!svg.querySelector('.p2country')){setTimeout(install,80);return}
 inject();fixLayerOrigin();mergeAdmin();buildSea();observeZoom();patchLanguage();
 // No map-detail rebuild on pan. The merged internal-border path stays visible and moves as one GPU layer.
 window.addEventListener('resize',()=>{fixLayerOrigin();setTimeout(updateVisualScale,50)});
 setTimeout(()=>{mergeAdmin();fixLayerOrigin();updateVisualScale()},220);
}
setTimeout(install,140);
})();