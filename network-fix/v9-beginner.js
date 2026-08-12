// Network Academy v9 — beginner continuity + cleaner guided lab + link deletion
(function(){
  const DATA=window.ACADEMY_DATA;
  if(!DATA||!Array.isArray(DATA.lessons)||!window.LAB)return;
  const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot',"'":'&#39;'}[c]));
  const L=s=>`<bdi class="v9Ltr" dir="ltr">${esc(s)}</bdi>`;
  const MASK='255.255.255.0';

  const B={
    6:{title:'كيف يجد الجهاز جهازًا آخر داخل نفس الشبكة؟',brief:'بعد أن عرفت IP، تعرّف على MAC وARP بهدوء',bridge:'أنت تعرف الآن أن لكل جهاز عنوان IP. بقي سؤال واحد: عندما يكون الجهازان داخل LAN نفسها، كيف تصل البيانات إلى بطاقة الشبكة الصحيحة فعلًا؟',idea:`داخل الشبكة المحلية يوجد لكل واجهة شبكة رقم آخر يسمى ${L('MAC Address')}. لا تحتاج حفظ شكله الآن. يكفي أن تفهم أن ${L('IP')} يحدد الجهاز منطقيًا، بينما ${L('MAC')} يساعد على التسليم داخل الشبكة المحلية.`,example:`حاسوب ${L('PC-1')} يريد الوصول إلى ${L('192.168.1.20')}. يعرف عنوان IP، لكنه لا يعرف بعد رقم MAC الخاص بالجهاز. فيرسل سؤالًا محليًا بسيطًا: «من صاحب هذا IP؟». هذا السؤال يسمى ${L('ARP')}.`,terms:[['MAC Address','رقم يخص واجهة الشبكة في الجهاز. يستخدم داخل LAN حتى تصل البيانات إلى الواجهة الصحيحة.'],['ARP','طريقة سؤال داخل الشبكة المحلية: من يملك عنوان IP هذا؟ فيجيب الجهاز بعنوان MAC الخاص به.']],steps:['الجهاز يعرف IP الذي يريد الوصول إليه.','باستخدام Subnet Mask يعرف أن الهدف داخل الشبكة المحلية نفسها.','إذا لم يعرف MAC للهدف، يرسل سؤال ARP داخل LAN.','الجهاز صاحب IP المطلوب يرد بعنوان MAC، ثم يستطيع المرسل إرسال البيانات إليه.'],remember:'لا تحفظ ARP كاختصار فقط. احفظ السؤال الذي يحله: «أنا أعرف IP، فكيف أعرف MAC للجهاز المحلي؟»',q:'جهاز يعرف IP لطابعة داخل LAN لكنه يحتاج معرفة عنوانها المحلي قبل الإرسال. ما الذي يساعده؟',opts:['ARP','Default Gateway للخروج إلى الإنترنت','اسم شبكة Wi‑Fi'],ans:0},
    7:{title:'ماذا يفعل Switch عندما ترسل البيانات؟',brief:'السويتش ليس الإنترنت؛ هو منظم المرور داخل LAN',bridge:'في الدرس السابق عرفت أن الأجهزة المحلية يمكن التعرف عليها بعنوان MAC. الآن سنرى لماذا يحتاج السويتش هذا العنوان.',idea:`السويتش ${L('Switch')} صندوق فيه عدة منافذ للكابلات. عندما تدخل إليه بيانات من جهاز، يحاول إرسالها إلى المنفذ الذي يوجد خلفه الجهاز المطلوب، بدل إرسالها عشوائيًا دائمًا.`,example:`لو كان ${L('PC-1')} في المنفذ 1 والطابعة في المنفذ 4، يتعلم السويتش مع الوقت أن عنوان MAC للطابعة موجود في المنفذ 4، فيرسل لها من هناك.`,terms:[['Port','فتحة في السويتش يُوصل بها كابل جهاز أو كابل إلى جهاز شبكي آخر.'],['Frame','اسم للحزمة المحلية التي ينقلها Ethernet داخل LAN. لا تحتاج تفاصيلها الآن.'],['MAC Table','قائمة صغيرة يتعلمها السويتش: أي عنوان MAC موجود خلف أي منفذ.']],steps:['تصل البيانات إلى السويتش من أحد المنافذ.','السويتش يتعلم من أي منفذ جاء الجهاز المرسل.','ينظر إلى عنوان MAC للجهاز المطلوب.','إذا كان يعرف منفذه يرسل إليه مباشرة؛ وإذا لم يعرفه بعد يبحث عنه داخل LAN ثم يتعلمه.'],remember:'السويتش يجمع الأجهزة داخل نفس الشبكة المحلية. الانتقال إلى شبكة أخرى وظيفة الراوتر، وليس السويتش العادي.',q:'ما الفائدة الأساسية من Switch في شبكة صغيرة فيها عدة أجهزة سلكية؟',opts:['ربط الأجهزة داخل LAN وإيصال البيانات للمنفذ المناسب','تحويل أسماء المواقع إلى IP','إعطاء كل جهاز شاشة أكبر'],ans:0,lab:true},
    8:{title:'متى نحتاج Router؟',brief:'عندما يكون الجهاز المطلوب في شبكة أخرى',bridge:'تعلمت أن Switch يخدم الأجهزة داخل LAN. وتعلمت أن Subnet Mask يخبر الجهاز هل الهدف داخل شبكته أم خارجها. هنا يأتي دور Router.',idea:`الراوتر ${L('Router')} يربط شبكتين مختلفتين أو أكثر. جهازك لا يرسل كل شيء إلى الراوتر؛ إذا كان الهدف محليًا يرسله داخل LAN، وإذا كان الهدف في شبكة أخرى يرسله إلى ${L('Default Gateway')}.`,example:`جهازك ${L('192.168.1.10/24')} يريد جهازًا عنوانه ${L('192.168.2.20/24')}. القناع يخبره أن الهدف في شبكة أخرى، فيرسل أولًا إلى الراوتر مثل ${L('192.168.1.1')}.`,terms:[['Router','جهاز يربط شبكات IP مختلفة ويسمح للبيانات بالانتقال بينها.'],['Routing','اختيار الاتجاه الذي تسلكه البيانات للوصول إلى شبكة أخرى. لا تحتاج جداول التوجيه المتقدمة الآن.']],steps:['الجهاز يقرأ IP الهدف مع Subnet Mask.','إذا كان الهدف خارج LAN، يرسل البيانات إلى Default Gateway.','الراوتر يستقبلها من جهة الشبكة الأولى.','يرسلها من الجهة التي تقود إلى الشبكة الثانية.'],remember:'في هذه المرحلة يكفي أن تفهم: شبكة مختلفة ← Gateway ← Router. تفاصيل Route وNext Hop ستأتي لاحقًا بعد أن يثبت هذا الأساس.',q:'PC في 192.168.1.x يريد جهازًا في 192.168.2.x. ما الجهاز الذي نحتاجه بين الشبكتين؟',opts:['Router','Switch فقط من دون توجيه','طابعة'],ans:0,lab:true},
    9:{title:'كيف أتأكد أن جهازين يستطيعان الوصول إلى بعضهما؟',brief:'استخدم Ping كسؤال بسيط: هل يصل الطريق ويرجع الرد؟',bridge:'أنت الآن تعرف التوصيل وIP وMask وGateway وSwitch وRouter. نحتاج أداة صغيرة تخبرنا هل الاتصال بين نقطتين يعمل.',idea:`أداة ${L('Ping')} ترسل رسالة اختبار إلى جهاز آخر وتنتظر ردًا. إذا عاد الرد، فهذا دليل أن الوصول الأساسي بين الجهازين يعمل.`,example:`إذا نجح ${L('Ping')} من ${L('PC-1')} إلى ${L('PC-2')} فهذا يعني أن هناك طريقًا يصل بينهما وأن العناوين الأساسية مناسبة. لكنه لا يعني أن كل البرامج والخدمات تعمل.`,terms:[['Ping','اختبار بسيط لمعرفة هل يمكن الوصول من جهاز إلى جهاز آخر.'],['ICMP','الاسم التقني لنوع رسائل يستخدمها Ping. لا تحتاج حفظ تفاصيله الآن.']],steps:['اختر الجهاز الذي سيبدأ الاختبار.','اختر الجهاز الذي تريد الوصول إليه.','اضغط Ping.','اقرأ النتيجة: نجاح يعني أن الرد عاد؛ فشل يعني أن نبحث عن سبب في التوصيل أو العناوين أو الطريق.'],remember:'Ping أداة فحص، وليس شيئًا يجعل الشبكة تعمل. هو فقط يخبرك هل الوصول الأساسي نجح أم لا.',q:'نجح Ping بين جهازين. ما الشيء الذي أثبته بشكل أساسي؟',opts:['أن الوصول الأساسي بينهما يعمل','أن كل مواقع الإنترنت تعمل حتمًا','أن الجهازين لهما نفس IP'],ans:0,lab:true},
    10:{title:'كيف تنتقل البيانات: كابل أم Wi‑Fi؟',brief:'ثلاث طرق شائعة لنقل البيانات من مكان إلى آخر',bridge:'حتى الآن ركزنا على الأجهزة والعناوين. لكن البيانات تحتاج طريقًا حقيقيًا تتحرك فيه بين الأجهزة.',idea:'قد يكون الطريق كابلًا نحاسيًا، أو أليافًا ضوئية، أو اتصالًا لاسلكيًا. الاختلاف هنا في طريقة حمل البيانات، وليس في معنى IP نفسه.',example:'حاسوب مكتبي قد يصل بالسويتش بكابل Ethernet، وهاتف يصل عبر Wi‑Fi، وبين مبنيين قد تستخدم الشركة Fiber لمسافة وسرعة أعلى.',terms:[['Ethernet','طريقة شائعة للشبكات السلكية؛ غالبًا تراها ككابل شبكة بين PC وSwitch.'],['Fiber','ألياف ضوئية تنقل البيانات باستخدام الضوء، وتناسب السرعات والمسافات الأكبر.'],['Wi‑Fi','اتصال لاسلكي يدخل الجهاز إلى الشبكة عبر Access Point أو الراوتر المنزلي.']],steps:['الجهاز يحتاج وسيلة اتصال بالشبكة.','الجهاز الثابت قد يستخدم Ethernet بكابل.','الهاتف واللابتوب غالبًا يستخدمان Wi‑Fi للحركة من دون كابل.','في مسافات أو سرعات أكبر قد تستخدم الشبكات Fiber.'],remember:'قوة إشارة Wi‑Fi لا تعني وحدها أن الإنترنت سريع. Wi‑Fi هو جزء من الطريق، وقد تكون المشكلة بعده.',q:'أي خيار يسمح للهاتف بالاتصال بالشبكة من دون كابل؟',opts:['Wi‑Fi','Ethernet بكابل فقط','Subnet Mask'],ans:0},
    11:{title:'كيف أبحث عن عطل من دون تخمين؟',brief:'افحص الأشياء التي تعلمتها بالترتيب، ولا تقفز إلى مصطلحات لم تدرسها',bridge:'هذا الدرس لا يضيف تقنية كبيرة جديدة. هدفه أن يجمع ما تعلمته في الوحدة الأولى في طريقة واحدة للفحص.',idea:'عندما لا يعمل الاتصال، لا تغيّر كل شيء. ابدأ بأبسط سؤال ثم انتقل للذي بعده. هكذا تعرف أين ظهر الخلل بدل أن تضيع بين إعدادات كثيرة.',example:`إذا فشل ${L('Ping')} بين جهازين في LAN، ابدأ: هل يوجد خط توصيل؟ ثم هل IP مختلف وصحيح لكل جهاز؟ ثم هل Mask مناسب؟ وإذا كان الهدف في شبكة أخرى فقط، افحص Gateway وRouter.`,terms:[['Troubleshooting','البحث عن سبب المشكلة خطوة بخطوة ثم اختبار الإصلاح.'],['Check','فحص شيء محدد بدل تغيير إعدادات عشوائية.']],steps:['افحص التوصيل أولًا: كابل أو Wi‑Fi أو خط في المعمل.','افحص IP: هل لكل جهاز عنوان مختلف وصحيح؟','افحص Subnet Mask: هل الجهازان محليان أم في شبكتين؟','إذا كان الهدف بعيدًا افحص Default Gateway والراوتر.','استخدم Ping بعد كل إصلاح لتعرف هل المشكلة اختفت.'],remember:'لن نستخدم DNS أو HTTP هنا لأنك لم تدرسهما بعد. سيأتيان في الوحدة التالية، وبعدها نضيفهما إلى خطوات التشخيص.',q:'جهاز لا يصل إلى جهاز آخر إطلاقًا. ما أفضل بداية بدل تغيير إعدادات كثيرة؟',opts:['التأكد من وجود اتصال ثم فحص IP خطوة خطوة','تغيير كل القيم مرة واحدة','البدء بخدمة لم ندرسها بعد'],ans:0,lab:true}
  };

  Object.entries(B).forEach(([k,b])=>{
    const l=DATA.lessons[+k]; if(!l)return;
    l.title=b.title;l.brief=b.brief;l.terms=b.terms.map(x=>x[0]);l.q=b.q;l.opts=b.opts;l.ans=b.ans;
    l.body=`<p>${b.idea}</p><div class="example">${b.example}</div>`;
  });

  function termCards(b){return b.terms.map(([n,d])=>`<div class="v9Term"><bdi dir="ltr">${esc(n)}</bdi><p>${d}</p></div>`).join('')}
  function openBeginnerLesson(i){
    const l=DATA.lessons[i],b=B[i],done=progress.completed.includes(i),lab=!!b.lab;
    current='lesson';header(`المرحلة ${l.stage} • الدرس ${i+1}`);nav();
    document.getElementById('screen').innerHTML=`<article class="lessonPage v9LessonPage">
      <button class="backBtn" onclick="current='academy';stageFilter=1;render();nav()">→ رجوع للدروس</button>
      <section class="v9Hero"><span>الوحدة الأولى • الدرس ${i+1}</span><h1>${esc(l.title)}</h1><p>${esc(l.brief)}</p></section>
      <section class="v9Block bridge"><div class="v9Head"><i>1</i><div><small>اربط الدرس بما قبله</small><h2>ما الذي أعرفه حتى الآن؟</h2></div></div><p>${b.bridge}</p></section>
      <section class="v9Block idea"><div class="v9Head"><i>2</i><div><small>الفكرة الجديدة</small><h2>ما هذا ببساطة؟</h2></div></div><p>${b.idea}</p><div class="v9Example"><b>مثال قريب</b><p>${b.example}</p></div></section>
      <section class="v9Block"><div class="v9Head"><i>3</i><div><small>كلمات جديدة فقط</small><h2>مصطلحات اليوم</h2></div></div><p class="v9Note">لا تحفظ الاسم أولًا. اقرأ الوظيفة بالعربي، ثم اربط الاسم بها.</p><div class="v9Terms">${termCards(b)}</div></section>
      <section class="v9Block"><div class="v9Head"><i>4</i><div><small>رتبها في ذهنك</small><h2>ماذا يحدث بالترتيب؟</h2></div></div><div class="v9Steps">${b.steps.map((s,n)=>`<div><b>${n+1}</b><p>${s}</p></div>`).join('')}</div><div class="v9Remember"><b>الخلاصة التي أريدك أن تتذكرها</b><p>${b.remember}</p></div></section>
      ${lab?`<section class="v9Block practice"><div class="v9Head"><i>5</i><div><small>طبّق بيدك</small><h2>تجربة قصيرة في المعمل</h2></div></div><p>ستدخل إلى مساحة فارغة، ثم تضع نقطة البداية وتنفذ التغييرات بنفسك خطوة بخطوة.</p><button class="primary v9LabLaunch" onclick="openLessonLab(${i})">افتح تجربة هذا الدرس ←</button></section>`:''}
      <section class="quiz quizV4 v9Quiz"><div class="v9Head"><i>${lab?6:5}</i><div><small>سؤال واحد للفهم</small><h2>هل اتضحت الفكرة؟</h2></div></div><p class="questionText">${esc(l.q)}</p><div id="opts">${l.opts.map((o,k)=>`<button class="option" onclick="answerLesson(${i},${k},this)">${esc(o)}</button>`).join('')}</div><div id="quizMsg" class="quizMsg"></div></section>
      ${done?`<button class="primary nextFooter" onclick="nextLesson(${i})">التالي ←</button>`:''}
    </article>`;
    document.getElementById('screen').scrollTop=0;
    setTimeout(()=>window.v8PolishText?.(),0);
  }

  const prevOpenLesson=window.openLesson;
  window.openLesson=function(i){if(B[i])return openBeginnerLesson(i);return prevOpenLesson(i)};

  function cleanLabState(){
    LAB.state.nodes=[];LAB.state.links=[];LAB.state.selected=null;LAB.state.connectFrom=null;LAB.state.unlinkFrom=null;LAB.state.mode='move';LAB.state.lastPath=[];LAB.save();
  }
  const prevOpenLessonLab=window.openLessonLab;
  window.openLessonLab=function(i){cleanLabState();window._lessonLabIndex=i;prevOpenLessonLab(i);setTimeout(()=>{resetLabView?.();decorateLab();},20)};

  function add(type,name,x,y,vals={}){LAB.add(type);const n=LAB.state.nodes[LAB.state.nodes.length-1];Object.assign(n,{name,x,y,...vals});return n}
  function finish(msg){LAB.save();LAB.render();toast(msg);setTimeout(decorateLab,20)}
  const prevPrepare=window.prepareLessonLab;
  window.prepareLessonLab=function(i){
    if(![7,8,9,11].includes(i))return prevPrepare(i);
    cleanLabState();LAB.render();
    if(i===7){add('pc','PC-1',140,300,{ip:'192.168.1.10',mask:MASK});add('switch','SW-1',320,150);add('pc','PC-2',500,300,{ip:'192.168.1.20',mask:MASK});return finish('وضعت الأجهزة فقط. ابدأ أنت بالتوصيل.');}
    if(i===8){add('pc','PC-A',120,300,{ip:'192.168.1.10',mask:MASK,gateway:''});add('router','R1',320,150,{dhcpServer:false,nat:false});add('pc','PC-B',520,300,{ip:'192.168.2.20',mask:MASK,gateway:''});return finish('وضعت جهازين في شبكتين مختلفتين وراوترًا بينهما، بلا توصيلات أو بوابات جاهزة.');}
    if(i===9){const a=add('pc','PC-1',140,300,{ip:'192.168.1.10',mask:MASK}),s=add('switch','SW-1',320,150),b=add('pc','PC-2',500,300,{ip:'192.168.1.20',mask:MASK});LAB.connect(a.id,s.id);LAB.connect(b.id,s.id);return finish('هذه المرة التوصيل والعناوين جاهزة حتى تركز على معنى Ping وحذف الرابطة.');}
    if(i===11){const a=add('pc','PC-1',140,300,{ip:'192.168.1.10',mask:MASK}),s=add('switch','SW-1',320,150),b=add('pc','PC-2',500,300,{ip:'192.168.1.20',mask:MASK});LAB.connect(a.id,s.id);return finish('هناك عطل واحد فقط في الرسم. ابحث عنه بهدوء ثم أصلحه.');}
  };

  const LAB_GUIDE={
    7:{title:'شاهد وظيفة Switch بيدك',goal:'كوّن LAN صغيرة ثم اختبر أن الجهازين يصلان إلى بعضهما.',steps:[['تهيئة نقطة البداية','اضغط الزر أسفل الخطوات. ستظهر PC-1 وSW-1 وPC-2 بلا روابط.','ثلاثة أجهزة بلا خطوط.'],['وصل الجهاز الأول','اضغط «توصيل»، ثم PC-1 ثم SW-1.','يظهر خط واحد بين PC-1 والسويتش.'],['وصل الجهاز الثاني','اضغط «توصيل»، ثم PC-2 ثم SW-1.','يظهر خط ثانٍ.'],['اختبر','في أدوات الشبكة اختر PC-1 مصدرًا وPC-2 هدفًا واضغط Ping.','يظهر نجاح الوصول داخل LAN.']]},
    8:{title:'مرّر البيانات بين شبكتين',goal:'سترى لماذا نحتاج Router وGateway عندما تختلف الشبكة.',steps:[['تهيئة نقطة البداية','اضغط الزر. ستظهر PC-A وR1 وPC-B بلا روابط.','PC-A عنوانه 192.168.1.10 وPC-B عنوانه 192.168.2.20.'],['أنشئ الطريق','استخدم «توصيل»: PC-A ↔ R1 ثم PC-B ↔ R1.','يظهر خطان إلى الراوتر.'],['اضبط جهتي الراوتر','افتح R1. في الواجهة المتجهة إلى PC-A اكتب 192.168.1.1 والقناع 255.255.255.0. وفي الواجهة المتجهة إلى PC-B اكتب 192.168.2.1 بنفس القناع، ثم احفظ.','للراوتر عنوان من كل شبكة.'],['اضبط البوابات','افتح PC-A واجعل Gateway = 192.168.1.1. افتح PC-B واجعل Gateway = 192.168.2.1.','كل PC يعرف عنوان الراوتر في شبكته.'],['اختبر','اختر PC-A وPC-B واضغط Ping.','ينجح الوصول بين الشبكتين عبر R1.']]},
    9:{title:'افهم Ping بالنجاح والفشل',goal:'اختبر شبكة صحيحة، احذف رابطة، ثم شاهد كيف تتغير النتيجة.',steps:[['تهيئة نقطة البداية','اضغط الزر. الشبكة ستكون جاهزة لهذا الاختبار فقط.','PC-1 وPC-2 متصلان بالسويتش وعنواناهما صحيحان.'],['اختبر أول مرة','اختر PC-1 وPC-2 واضغط Ping.','تظهر نتيجة نجاح.'],['احذف رابطة','اضغط «حذف رابطة»، ثم PC-2 ثم SW-1.','يختفي الخط بينهما.'],['أعد Ping','اضغط Ping بنفس المصدر والهدف.','يفشل الوصول لأن الطريق انقطع.'],['أعد التوصيل','اضغط «توصيل» ثم PC-2 ثم SW-1، وبعدها Ping.','يعود النجاح.']]},
    11:{title:'اكتشف عطلًا واحدًا بدون تخمين',goal:'المشكلة مرئية في الرسم؛ استخدم ما تعلمته فقط.',steps:[['تهيئة نقطة البداية','اضغط الزر. لا تغيّر IP الآن.','PC-1 متصل بالسويتش، بينما PC-2 غير متصل.'],['اختبر الأعراض','اختر PC-1 وPC-2 واضغط Ping.','يفشل Ping.'],['ابدأ بأبسط فحص','انظر إلى الخطوط في الرسم قبل فتح أي إعداد.','تلاحظ أن PC-2 لا يملك خطًا إلى SW-1.'],['أصلح السبب فقط','اضغط «توصيل»، ثم PC-2 ثم SW-1.','يظهر الخط المفقود.'],['أعد الاختبار','اضغط Ping مرة ثانية.','ينجح. هكذا عرفت أن سبب المشكلة كان التوصيل.']]}
  };

  function guideHtml(g){return g.steps.map((s,n)=>`<div class="guideStep v9GuideStep"><b>${n+1}</b><div><strong>${esc(s[0])}</strong><div class="v9Do"><b>نفّذ</b><p>${s[1]}</p></div><div class="v9Expected"><b>النتيجة المتوقعة</b><p>${s[2]}</p></div></div></div>`).join('')}
  function replaceGuide(){
    const i=Number.isInteger(window._lessonLabIndex)?window._lessonLabIndex:null,g=LAB_GUIDE[i];
    const box=document.querySelector('.guidedLab');if(!box)return;
    box.querySelectorAll('.v6GuideIntro,.v6StepExplain,.v7GuideIntro,.v7NoAuto').forEach(x=>x.remove());
    if(g&&box.dataset.v9guide!==String(i)){
      box.dataset.v9guide=String(i);const h=box.querySelector('h2'),p=box.querySelector(':scope>p'),steps=box.querySelector('.guideSteps');if(h)h.textContent=g.title;if(p)p.textContent=g.goal;if(steps)steps.innerHTML=guideHtml(g);
      const btns=box.querySelector('.guideBtns');if(btns)btns.innerHTML=`<button class="primary" onclick="prepareLessonLab(${i})">⚡ تهيئة نقطة البداية</button><button class="secondary" onclick="v9VerifyLab(${i})">✓ تحقق من المهمة</button>`;
    }else if(!g){
      box.querySelectorAll('.v7Idea').forEach(x=>{x.querySelector('b')&&(x.querySelector('b').textContent='السبب:')});
      box.querySelectorAll('.v7Do>b').forEach(x=>x.textContent='نفّذ');
      box.querySelectorAll('.v7See>b').forEach(x=>x.textContent='النتيجة:');
    }
  }

  window.v9VerifyLab=function(i){
    const n=LAB.state.nodes,link=(a,b)=>!!LAB.linkBetween(a.id,b.id);let ok=false;
    if(i===7){const p=n.filter(x=>x.type==='pc'),s=n.find(x=>x.type==='switch');ok=!!(p.length>=2&&s&&p.every(x=>link(x,s))&&LAB.canPing(p[0].id,p[1].id).ok)}
    if(i===8){const a=n.find(x=>x.name==='PC-A'),b=n.find(x=>x.name==='PC-B'),r=n.find(x=>x.name==='R1');ok=!!(a&&b&&r&&a.gateway==='192.168.1.1'&&b.gateway==='192.168.2.1'&&LAB.canPing(a.id,b.id).ok)}
    if(i===9){const p=n.filter(x=>x.type==='pc'),s=n.find(x=>x.type==='switch');ok=!!(p.length>=2&&s&&p.every(x=>link(x,s))&&LAB.canPing(p[0].id,p[1].id).ok)}
    if(i===11){const p=n.filter(x=>x.type==='pc'),s=n.find(x=>x.type==='switch');ok=!!(p.length>=2&&s&&p.every(x=>link(x,s))&&LAB.canPing(p[0].id,p[1].id).ok)}
    const out=document.getElementById('guideResult');
    if(ok){progress.labs=Array.isArray(progress.labs)?progress.labs:[];if(!progress.labs.includes(i)){progress.labs.push(i);saveProgress()}if(out)out.innerHTML='<div class="guideSuccess">✓ نجحت. لا تنتقل قبل أن تستطيع قول السبب بجملة بسيطة من عندك.</div>';toast('✅ نجحت المهمة')}
    else{if(out)out.innerHTML='<div class="guideFail">المهمة لم تكتمل. ارجع إلى آخر خطوة فقط، وقارن ما في الرسم بالنتيجة المتوقعة المكتوبة تحتها.</div>';toast('راجع آخر خطوة فقط')}
  };

  function removeLink(a,b){
    const l=LAB.linkBetween(a,b);if(!l)return false;
    LAB.state.links=LAB.state.links.filter(x=>x.id!==l.id);
    LAB.state.nodes.filter(n=>n.type==='router').forEach(r=>{if(r.interfaces)delete r.interfaces[l.id]});
    LAB.save();return true;
  }
  window.v9ToggleUnlink=function(){
    const active=LAB.state.mode==='unlink';LAB.state.mode=active?'move':'unlink';LAB.state.connectFrom=null;LAB.state.unlinkFrom=null;LAB.state.selected=null;LAB.render();toast(active?'تم إنهاء حذف الروابط':'اختر الجهاز الأول ثم الجهاز الثاني للرابطة التي تريد حذفها');
  };
  function bindUnlinkNodes(){
    if(LAB.state.mode!=='unlink')return;
    document.querySelectorAll('.netNode').forEach(el=>{el.classList.toggle('v9UnlinkFrom',el.dataset.id===LAB.state.unlinkFrom);el.onclick=e=>{e.stopPropagation();const id=el.dataset.id;if(!LAB.state.unlinkFrom){LAB.state.unlinkFrom=id;LAB.state.selected=id;LAB.render();toast('الآن اختر الجهاز الآخر المتصل به');return}const first=LAB.state.unlinkFrom;if(first===id){LAB.state.unlinkFrom=null;LAB.state.selected=null;LAB.render();return}if(removeLink(first,id)){LAB.state.mode='move';LAB.state.unlinkFrom=null;LAB.state.selected=null;LAB.render();toast('✓ حُذفت الرابطة')}else{LAB.state.unlinkFrom=null;LAB.state.selected=null;LAB.render();toast('لا توجد رابطة مباشرة بين هذين الجهازين')}}});
  }
  const oldLabRender=LAB.render.bind(LAB);
  LAB.render=function(){oldLabRender();setTimeout(()=>{decorateLab();bindUnlinkNodes()},0)};
  const oldToggleConnect=LAB.toggleConnect.bind(LAB);
  LAB.toggleConnect=function(){LAB.state.unlinkFrom=null;oldToggleConnect()};

  function decorateLab(){
    const screen=document.getElementById('screen');if(!screen)return;
    const i=Number.isInteger(window._lessonLabIndex)?window._lessonLabIndex:null;
    if(i!==null&&!screen.querySelector('.v9ReturnLesson')){
      const target=screen.querySelector('.pageLead')||screen.firstElementChild;if(target)target.insertAdjacentHTML('afterend',`<button class="v9ReturnLesson" onclick="returnToLesson()">↩ الرجوع إلى الدرس: ${esc(DATA.lessons[i]?.title||'')}</button>`);
    }
    const actions=screen.querySelector('.labActions');
    if(actions&&!actions.querySelector('#unlinkMode'))actions.insertAdjacentHTML('beforeend','<button id="unlinkMode" class="secondary v9UnlinkBtn" onclick="v9ToggleUnlink()">✂ حذف رابطة</button>');
    const ub=document.getElementById('unlinkMode');if(ub)ub.classList.toggle('active',LAB.state.mode==='unlink');
    replaceGuide();bindUnlinkNodes();
  }

  const root=document.getElementById('screen');if(root)new MutationObserver(()=>setTimeout(decorateLab,0)).observe(root,{childList:true,subtree:true});
  setTimeout(()=>{decorateLab();},0);
})();
