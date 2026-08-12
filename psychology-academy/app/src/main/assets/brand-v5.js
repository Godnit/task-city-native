/* Psychology Academy v6 — exact selected brand image + unified identity */
(function(){
'use strict';
const BRAND_IMAGE='psychology-icon-v6.webp';
const oldHome=window.home;
function mini(kind){
 const paths={
  paths:'<circle cx="50" cy="50" r="13"/><circle cx="18" cy="24" r="7"/><circle cx="82" cy="24" r="7"/><circle cx="18" cy="77" r="7"/><circle cx="82" cy="77" r="7"/><path d="M24 29l17 14M76 29L59 43M24 72l17-14M76 72L59 58"/>',
  lab:'<path d="M38 16h24M43 16v22L25 72a8 8 0 007 12h36a8 8 0 007-12L57 38V16"/><path d="M34 61h32"/><circle cx="43" cy="70" r="4"/><circle cx="57" cy="55" r="4"/>',
  cases:'<circle cx="31" cy="31" r="10"/><circle cx="69" cy="31" r="10"/><circle cx="50" cy="73" r="10"/><path d="M40 34h20M36 40l10 24M64 40L54 64"/><path d="M18 87h64"/>',
  review:'<circle cx="50" cy="50" r="28"/><circle cx="50" cy="50" r="17"/><path d="M34 51l10 10 22-25"/><path d="M50 14v8M86 50h-8M50 86v-8M14 50h8"/>'
 };
 return `<svg viewBox="0 0 100 100" aria-hidden="true"><defs><linearGradient id="mg${kind}" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#6ff5ef"/><stop offset=".55" stop-color="#65b8ff"/><stop offset=".8" stop-color="#8b7dff"/><stop offset="1" stop-color="#f4c45f"/></linearGradient><filter id="mf${kind}" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="2.5" result="b"/><feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter></defs><g fill="none" stroke="url(#mg${kind})" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round" filter="url(#mf${kind})">${paths[kind]||paths.paths}</g></svg>`;
}
window.brainArt=function(){return `<div class="brandHeroV5 exactBrandV6"><div class="brandGlowV5"></div><img src="${BRAND_IMAGE}" alt="" draggable="false"></div>`};
function applyBrand(){
 const m=document.querySelector('.brandMark');
 if(m){m.innerHTML=`<img src="${BRAND_IMAGE}" alt="" class="brandMarkV5 exactBrandImageV6" draggable="false">`;m.classList.add('brandMarkUnified')}
 const tiles=[['#qPaths','paths'],['#qLab','lab'],['#qCases','cases'],['#qReview','review']];
 tiles.forEach(([sel,k])=>{const t=document.querySelector(sel);const v=t&&t.querySelector('.tileVisual');if(v)v.innerHTML=mini(k)});
 document.querySelectorAll('.finalStageIcon,.stageVisual').forEach((el)=>el.classList.add('unifiedNeonIcon'));
}
if(typeof oldHome==='function')window.home=function(s){oldHome(s);applyBrand()};
const oldNav=window.nav;if(typeof oldNav==='function')window.nav=function(){oldNav();applyBrand()};
const oldPaths=window.paths;if(typeof oldPaths==='function')window.paths=function(s){oldPaths(s);applyBrand()};
setTimeout(()=>{applyBrand();if(typeof render==='function')render();setTimeout(applyBrand,0)},0);
})();