(() => {
  'use strict';

  const $ = id => document.getElementById(id);
  const video = $('camera');
  const canvas = $('landmarkCanvas');
  const ctx = canvas.getContext('2d');
  const stage = $('cameraStage');
  const statusPill = $('statusPill');
  const startHint = $('startHint');
  const gestureBadge = $('gestureBadge');
  const cube = $('cube');
  const cubeWorld = $('cubeWorld');
  const cubeButton = $('cubeModeButton');
  let hands;
  let stream;
  let processing = false;
  let running = false;
  let handFrames = 0;
  let screen = 'test';
  let lastFrameTime = performance.now();
  let fpsSmooth = 0;
  let calibration = {x:0.5, y:0.5};
  let cubeState = {x:0, y:0, scale:1, rx:-15, ry:25};

  function setStatus(type, label) {
    statusPill.className = `status-pill ${type}`;
    statusPill.querySelector('b').textContent = label;
  }

  function sizeCanvas() {
    const rect = stage.getBoundingClientRect();
    const ratio = Math.min(2, window.devicePixelRatio || 1);
    canvas.width = Math.round(rect.width * ratio);
    canvas.height = Math.round(rect.height * ratio);
    ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
  }

  function drawHand(points) {
    const w = canvas.clientWidth;
    const h = canvas.clientHeight;
    ctx.clearRect(0, 0, w, h);
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.shadowBlur = 11;
    ctx.shadowColor = '#28f2b5';
    ctx.strokeStyle = '#37e8b2';
    ctx.lineWidth = 4;
    GestureCore.CONNECTIONS.forEach(([a,b]) => {
      ctx.beginPath();
      ctx.moveTo(points[a].x*w, points[a].y*h);
      ctx.lineTo(points[b].x*w, points[b].y*h);
      ctx.stroke();
    });
    ctx.shadowBlur = 0;
    points.forEach((p, i) => {
      ctx.beginPath();
      ctx.fillStyle = [4,8,12,16,20].includes(i) ? '#ffffff' : '#24b5ff';
      ctx.arc(p.x*w, p.y*h, [4,8,12,16,20].includes(i) ? 6 : 4.2, 0, Math.PI*2);
      ctx.fill();
    });
  }

  function clearHand() {
    ctx.clearRect(0, 0, canvas.clientWidth, canvas.clientHeight);
  }

  function updateFingerCards(fingers) {
    Object.entries(fingers).forEach(([name, open]) => {
      const card = document.querySelector(`[data-finger="${name}"]`);
      card.classList.toggle('active', open);
      card.querySelector('small').textContent = open ? 'مفتوح' : 'مثني';
    });
  }

  function updateTest(analysis) {
    updateFingerCards(analysis.fingers);
    handFrames = Math.min(45, handFrames + 1);
    const progress = Math.round(handFrames / 45 * 100);
    $('progressRing').style.setProperty('--p', progress);
    $('progressRing').querySelector('span').textContent = `${progress}%`;
    $('testResult').textContent = progress < 100 ? 'الخطوط تتبع يدك بنجاح' : 'الاختبار ناجح — جرّب ثني أصابعك';
    if (progress >= 100) cubeButton.disabled = false;
  }

  function updateCube(analysis) {
    const mirroredX = 1 - analysis.center.x;
    const dx = Math.max(-0.5, Math.min(0.5, mirroredX - calibration.x));
    const dy = Math.max(-0.5, Math.min(0.5, analysis.center.y - calibration.y));
    const targetX = dx * stage.clientWidth * 1.45;
    const targetY = dy * stage.clientHeight * 1.18;
    const pinchNorm = Math.max(0.18, Math.min(1.65, analysis.pinch));
    const targetScale = 0.58 + ((pinchNorm - 0.18) / 1.47) * 1.15;
    const smooth = 0.18;
    cubeState.x += (targetX - cubeState.x) * smooth;
    cubeState.y += (targetY - cubeState.y) * smooth;
    cubeState.scale += (targetScale - cubeState.scale) * 0.14;
    cubeState.ry += dx * 3.4;
    cubeState.rx = -15 + dy * 38;
    cube.style.transform = `translate3d(${cubeState.x}px,${cubeState.y}px,0) scale(${cubeState.scale}) rotateX(${cubeState.rx}deg) rotateY(${cubeState.ry}deg)`;
    $('scaleReadout').textContent = `${Math.round(cubeState.scale*100)}%`;
    $('controlGesture').textContent = analysis.gesture.name;
    const horizontal = dx < -0.12 ? 'يسار' : dx > 0.12 ? 'يمين' : '';
    const vertical = dy < -0.12 ? 'أعلى' : dy > 0.12 ? 'أسفل' : '';
    $('moveReadout').textContent = [vertical,horizontal].filter(Boolean).join(' + ') || 'المنتصف';
  }

  function onResults(results) {
    const points = results.multiHandLandmarks && results.multiHandLandmarks[0];
    if (!points) {
      clearHand();
      handFrames = Math.max(0, handFrames - 2);
      gestureBadge.classList.add('hidden');
      startHint.classList.remove('hidden');
      setStatus('searching', 'ابحث عن اليد');
      if (screen === 'test') $('testResult').textContent = 'لم تظهر اليد كاملة داخل الإطار';
      return;
    }
    const analysis = GestureCore.analyze(points);
    drawHand(points);
    setStatus('ready', 'اليد ظاهرة');
    startHint.classList.add('hidden');
    gestureBadge.classList.remove('hidden');
    $('gestureEmoji').textContent = analysis.gesture.emoji;
    $('gestureName').textContent = analysis.gesture.name;
    if (screen === 'test') updateTest(analysis);
    else updateCube(analysis);
  }

  async function frameLoop() {
    if (!running) return;
    if (!processing && video.readyState >= 2) {
      processing = true;
      try {
        await hands.send({image: video});
        const now = performance.now();
        const fps = 1000 / Math.max(1, now - lastFrameTime);
        fpsSmooth = fpsSmooth ? fpsSmooth*0.88 + fps*0.12 : fps;
        $('fpsValue').textContent = Math.round(fpsSmooth);
        lastFrameTime = now;
      } catch (err) {
        console.error(err);
      } finally {
        processing = false;
      }
    }
    setTimeout(frameLoop, 18);
  }

  async function startCamera() {
    hideError();
    setStatus('waiting', 'جاري تحميل المتتبع');
    try {
      if (!window.Hands) throw new Error('ملفات تتبع اليد غير موجودة داخل التطبيق.');
      hands = new Hands({locateFile: file => `mediapipe/${file}`});
      hands.setOptions({
        maxNumHands: 1,
        modelComplexity: 0,
        minDetectionConfidence: 0.55,
        minTrackingConfidence: 0.52,
        selfieMode: false
      });
      hands.onResults(onResults);
      stream = await navigator.mediaDevices.getUserMedia({
        audio: false,
        video: {facingMode:'user', width:{ideal:640}, height:{ideal:480}, frameRate:{ideal:24,max:30}}
      });
      video.srcObject = stream;
      await video.play();
      sizeCanvas();
      running = true;
      setStatus('searching', 'ارفع يدك');
      frameLoop();
    } catch (err) {
      console.error(err);
      showError(err);
    }
  }

  function showError(err) {
    running = false;
    setStatus('error', 'الكاميرا متوقفة');
    $('errorPanel').classList.remove('hidden');
    const denied = /permission|denied|notallowed/i.test(String(err && (err.name || err.message)));
    $('errorTitle').textContent = denied ? 'لم يتم السماح بالكاميرا' : 'تعذّر تشغيل الكاميرا';
    $('errorMessage').textContent = denied
      ? 'افتح إعدادات التطبيق واسمح بإذن الكاميرا، ثم ارجع واضغط إعادة المحاولة.'
      : `أغلق أي تطبيق يستخدم الكاميرا ثم حاول مجددًا. ${err && err.message ? err.message : ''}`;
  }

  function hideError() {
    $('errorPanel').classList.add('hidden');
  }

  function setScreen(next) {
    screen = next;
    const isCube = next === 'cube';
    $('testPanel').classList.toggle('hidden', isCube);
    $('controlPanel').classList.toggle('hidden', !isCube);
    cubeWorld.classList.toggle('hidden', !isCube);
    $('screenTitle').textContent = isCube ? 'تحكم بالمكعب بيدك' : 'اختبار تتبع اليد';
    stage.classList.toggle('cube-mode', isCube);
  }

  cubeButton.addEventListener('click', () => {
    calibration = {x:0.5, y:0.5};
    setScreen('cube');
  });
  $('backToTest').addEventListener('click', () => setScreen('test'));
  $('calibrateButton').addEventListener('click', () => {
    cubeState.x = 0; cubeState.y = 0;
    calibration = {x:0.5, y:0.5};
    $('moveReadout').textContent = 'تمت المعايرة';
  });
  $('retryButton').addEventListener('click', () => {
    if (stream) stream.getTracks().forEach(track => track.stop());
    running = false;
    setTimeout(startCamera, 120);
  });
  window.addEventListener('resize', sizeCanvas);
  document.addEventListener('visibilitychange', () => {
    if (!document.hidden && !running) startCamera();
  });

  startCamera();
})();
