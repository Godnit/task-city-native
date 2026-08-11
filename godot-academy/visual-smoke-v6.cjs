const puppeteer = require('puppeteer-core');
const fs = require('fs');

const BASE = 'http://127.0.0.1:8123/index.html';
const CHROME = process.env.CHROME_BIN || '/usr/bin/google-chrome';
const sizes = [
  {name:'small', width:360, height:800},
  {name:'regular', width:412, height:915},
];

function sleep(ms){ return new Promise(r=>setTimeout(r,ms)); }

(async()=>{
  if(!fs.existsSync(CHROME)) throw new Error(`Chrome not found: ${CHROME}`);
  fs.mkdirSync('/tmp/godot-v6-visual', {recursive:true});
  const browser = await puppeteer.launch({
    executablePath: CHROME,
    headless: true,
    args:['--no-sandbox','--disable-dev-shm-usage','--disable-gpu']
  });
  const errors=[];
  try{
    for(const size of sizes){
      const page = await browser.newPage();
      await page.setViewport({width:size.width,height:size.height,deviceScaleFactor:1});
      page.on('pageerror',e=>errors.push(`${size.name} pageerror: ${e.message}`));
      await page.goto(BASE,{waitUntil:'networkidle0'});
      const lessonCount = await page.evaluate(()=>DATA.lessons.length);
      await page.evaluate((n)=>{
        const all=Array.from({length:n},(_,i)=>i);
        const stageExams={}; const stageLabs={}; for(let i=1;i<=8;i++){stageExams[i]=true;stageLabs[i]=true;}
        localStorage.setItem('godot_academy_progress_v1',JSON.stringify({completed:all,examPassed:true,labPassed:true,stageExams,stageLabs}));
      },lessonCount);
      await page.reload({waitUntil:'networkidle0'});

      const routeFns=['home','academy','lab','progress'];
      for(const route of routeFns){
        await page.evaluate((r)=>go(r),route);
        await sleep(50);
        const m=await page.evaluate(()=>({
          html:document.documentElement.scrollWidth,
          viewport:document.documentElement.clientWidth,
          screen:document.querySelector('#screen')?.scrollWidth||0,
          screenClient:document.querySelector('#screen')?.clientWidth||0
        }));
        if(m.html>m.viewport+2 || m.screen>m.screenClient+2){
          errors.push(`${size.name} route ${route}: horizontal overflow html=${m.html}/${m.viewport}, screen=${m.screen}/${m.screenClient}`);
        }
      }

      for(let i=0;i<lessonCount;i++){
        await page.evaluate((idx)=>openLesson(idx),i);
        await sleep(35);
        const report=await page.evaluate(()=>{
          const viewport=document.documentElement.clientWidth;
          const screen=document.querySelector('#screen');
          const lesson=document.querySelector('.lessonPage');
          const isInsideScroller=(el)=>{
            let p=el.parentElement;
            while(p && p!==lesson){
              const s=getComputedStyle(p);
              if((s.overflowX==='auto'||s.overflowX==='scroll') && p.scrollWidth>p.clientWidth+1)return true;
              p=p.parentElement;
            }
            return false;
          };
          const offenders=[];
          lesson?.querySelectorAll('*').forEach(el=>{
            const s=getComputedStyle(el); if(s.position==='fixed'||s.display==='none'||isInsideScroller(el))return;
            const r=el.getBoundingClientRect();
            if(r.width>0 && (r.left < -2 || r.right > viewport+2)){
              offenders.push({tag:el.tagName,cls:el.className||'',left:Math.round(r.left),right:Math.round(r.right),w:Math.round(r.width)});
            }
          });
          const lr=lesson?.getBoundingClientRect();
          return {
            viewport,
            htmlScroll:document.documentElement.scrollWidth,
            screenScroll:screen?.scrollWidth||0,
            screenClient:screen?.clientWidth||0,
            lessonLeft:lr?.left||0,
            lessonRight:lr?.right||0,
            offenders:offenders.slice(0,8),
            hasV6:!!document.querySelector('.v6LessonHead') && !!document.querySelector('.v6Visual') && !!document.querySelector('.v6Understand') && !!document.querySelector('.v6Quiz')
          };
        });
        if(!report.hasV6) errors.push(`${size.name} lesson ${i+1}: missing v6 visual lesson structure`);
        if(report.htmlScroll>report.viewport+2 || report.screenScroll>report.screenClient+2 || report.lessonLeft<-2 || report.lessonRight>report.viewport+2 || report.offenders.length){
          errors.push(`${size.name} lesson ${i+1}: overflow ${JSON.stringify(report)}`);
        }
        if(size.name==='regular' && [6,8,9].includes(i)){
          await page.screenshot({path:`/tmp/godot-v6-visual/lesson-${String(i+1).padStart(2,'0')}.png`,fullPage:false});
        }
      }
      await page.close();
    }
  } finally { await browser.close(); }
  if(errors.length){console.error(errors.join('\n'));process.exit(1);}
  console.log('Visual smoke test OK: all lessons fit 360px and 412px widths with v6 lesson structure.');
})();
