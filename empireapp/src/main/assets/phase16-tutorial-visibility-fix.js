(function(){
'use strict';
if(document.getElementById('p16TutorialFix')) return;
const s=document.createElement('style');
s.id='p16TutorialFix';
s.textContent=`
/* Absolute safety mode for Android WebView: the guided tutorial never paints over the map. */
.p11TourLayer,.p11TourLayer::before,.p11TourLayer::after{
  background:transparent!important;
  background-color:transparent!important;
  box-shadow:none!important;
  filter:none!important;
  backdrop-filter:none!important;
  pointer-events:none!important;
}
/* Keep the spotlight node for tutorial positioning logic, but make it completely non-rendering. */
.p11Spot{
  display:none!important;
  visibility:hidden!important;
  opacity:0!important;
  box-shadow:none!important;
  background:none!important;
  border:0!important;
  filter:none!important;
  backdrop-filter:none!important;
  pointer-events:none!important;
}
.p11Tip{
  z-index:302!important;
  opacity:1!important;
  visibility:visible!important;
  background:linear-gradient(180deg,#29433f 0%,#172725 100%)!important;
  border:1px solid #83a29b!important;
  box-shadow:0 10px 28px rgba(0,0,0,.52)!important;
  pointer-events:auto!important;
}
.p11Arrow{z-index:303!important;opacity:1!important;filter:none!important}
#viewport,#worldWrap,#world,#p15Camera{
  display:block!important;
  visibility:visible!important;
  opacity:1!important;
  filter:none!important;
  backdrop-filter:none!important;
}
`;
(document.head||document.documentElement).appendChild(s);

function forceSafeTutorial(){
  document.querySelectorAll('.p11Spot').forEach(el=>{
    el.style.setProperty('display','none','important');
    el.style.setProperty('visibility','hidden','important');
    el.style.setProperty('opacity','0','important');
    el.style.setProperty('box-shadow','none','important');
    el.style.setProperty('background','none','important');
    el.style.setProperty('border','0','important');
  });
  ['viewport','worldWrap','world','p15Camera'].forEach(id=>{
    const el=document.getElementById(id);
    if(!el)return;
    el.style.setProperty('visibility','visible','important');
    el.style.setProperty('opacity','1','important');
    el.style.setProperty('filter','none','important');
    el.style.setProperty('backdrop-filter','none','important');
  });
  const layer=document.getElementById('p11TourLayer');
  if(layer){
    layer.style.setProperty('background','transparent','important');
    layer.style.setProperty('background-color','transparent','important');
    layer.style.setProperty('box-shadow','none','important');
    layer.style.setProperty('filter','none','important');
    layer.style.setProperty('backdrop-filter','none','important');
  }
}
forceSafeTutorial();
new MutationObserver(forceSafeTutorial).observe(document.body||document.documentElement,{subtree:true,childList:true,attributes:true,attributeFilter:['style','class']});
})();
