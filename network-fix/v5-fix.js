// Network Academy v5 — full visual palette + clear daylight mode
(function(){
  const PALETTES=[
    {name:'سماوي تقني',a:'#00bfe8',b:'#18caa7',rgb:'0,191,232',rgb2:'24,202,167',deep:'#08769a',dark1:'#0d4160',dark2:'#08283f'},
    {name:'بنفسجي رقمي',a:'#7567ff',b:'#b460ff',rgb:'117,103,255',rgb2:'180,96,255',deep:'#5840c8',dark1:'#392f78',dark2:'#211d4d'},
    {name:'برتقالي شبكي',a:'#ff8b32',b:'#ffc34d',rgb:'255,139,50',rgb2:'255,195,77',deep:'#c85d14',dark1:'#6c371d',dark2:'#412514'},
    {name:'أخضر أنظمة',a:'#10b981',b:'#51d99b',rgb:'16,185,129',rgb2:'81,217,155',deep:'#087a58',dark1:'#164f40',dark2:'#0b3028'}
  ];
  window.applySettings=function(){
    document.body.classList.toggle('light',settings.theme==='light');
    const t=PALETTES[(settings.accent||0)%PALETTES.length],r=document.documentElement.style;
    r.setProperty('--accent',t.a); r.setProperty('--accent2',t.b);
    r.setProperty('--accent-rgb',t.rgb); r.setProperty('--accent2-rgb',t.rgb2);
    r.setProperty('--accent-deep',t.deep); r.setProperty('--accent-dark1',t.dark1); r.setProperty('--accent-dark2',t.dark2);
    document.body.dataset.palette=String((settings.accent||0)%PALETTES.length);
    const meta=document.querySelector('meta[name="theme-color"]');
    if(meta) meta.setAttribute('content',settings.theme==='light'?'#f4f8fb':'#071525');
  };
  window.cycleAccent=function(){
    settings.accent=((settings.accent||0)+1)%PALETTES.length;
    saveSettings(); applySettings(); header(); nav(); render();
    toast('تم تغيير هوية الواجهة إلى: '+PALETTES[settings.accent].name);
  };
  window.toggleTheme=function(){
    settings.theme=settings.theme==='light'?'dark':'light';
    saveSettings(); applySettings(); header(); nav(); render();
    toast(settings.theme==='light'?'تم تفعيل الوضع النهاري الواضح':'تم تفعيل الوضع الليلي');
  };
  applySettings();
  try{header();nav();render();}catch(e){}
})();
