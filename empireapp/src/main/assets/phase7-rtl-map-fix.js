(function(){
'use strict';
// v0.7: keep the map's mathematical coordinate system LTR even when the app UI is RTL.
// Without an explicit left:0, an absolutely-positioned auto/auto child inside an RTL
// container gets a right-side static position in Android WebView. The camera then thinks
// x=0 starts at the viewport edge while the DOM actually starts hundreds of pixels left,
// which produced the large empty ocean strip and made America unreachable at low zoom.
const style=document.createElement('style');
style.id='p7RtlMapFix';
style.textContent=`
#viewport{direction:ltr!important;}
#viewport>#worldWrap{
  position:absolute!important;
  left:0!important;
  right:auto!important;
  top:0!important;
  bottom:auto!important;
  margin:0!important;
  padding:0!important;
}
`;
(document.head||document.documentElement).appendChild(style);

function hardReset(){
  const vp=document.getElementById('viewport');
  const wrap=document.getElementById('worldWrap');
  if(!vp||!wrap)return;
  // Reassert in inline style too; this avoids old WebView RTL static-position quirks.
  wrap.style.left='0px';
  wrap.style.right='auto';
  wrap.style.top='0px';
  if(document.getElementById('map')?.classList.contains('active') && typeof window.resetMap==='function'){
    window.resetMap();
  }
}

// phase6 installs asynchronously, so reset after it has taken control as well.
setTimeout(hardReset,80);
setTimeout(hardReset,220);
setTimeout(hardReset,500);
window.addEventListener('resize',()=>setTimeout(hardReset,60));
})();
