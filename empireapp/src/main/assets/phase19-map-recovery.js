(function(){
'use strict';
if(window.__p19)return;window.__p19=true;
const s=document.createElement('style');
s.textContent='#viewport{background:#2b7681!important;overflow:hidden!important}#worldWrap,#world{visibility:visible!important;opacity:1!important;filter:none!important}.p11TourLayer,.p11Spot{display:none!important}';
document.head.appendChild(s);
function fix(force){
 const map=document.getElementById('map'),vp=document.getElementById('viewport'),wrap=document.getElementById('worldWrap'),world=document.getElementById('world'),badge=document.getElementById('zoomText');
 if(!map||!vp||!wrap||!world)return;
 if(!world.querySelector('.p2country')&&typeof window.renderMap==='function'){try{window.renderMap()}catch(e){}}
 const n=parseFloat((badge?.textContent||'').replace(/[^0-9.]/g,''));
 if(map.classList.contains('active')&&(force||!Number.isFinite(n)||n<=0)&&typeof window.resetMap==='function'){
   requestAnimationFrame(()=>requestAnimationFrame(()=>{try{window.resetMap()}catch(e){}}));
 }
}
const map=document.getElementById('map');
if(map)new MutationObserver(()=>{if(map.classList.contains('active'))setTimeout(()=>fix(true),80)}).observe(map,{attributes:true,attributeFilter:['class']});
setTimeout(()=>fix(false),300);setTimeout(()=>fix(false),900);
})();
