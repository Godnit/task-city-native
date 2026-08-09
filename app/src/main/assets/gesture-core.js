(function (root) {
  'use strict';

  const FINGERS = {
    thumb: [1, 2, 3, 4],
    index: [5, 6, 7, 8],
    middle: [9, 10, 11, 12],
    ring: [13, 14, 15, 16],
    pinky: [17, 18, 19, 20]
  };

  const CONNECTIONS = [
    [0,1],[1,2],[2,3],[3,4],
    [0,5],[5,6],[6,7],[7,8],
    [5,9],[9,10],[10,11],[11,12],
    [9,13],[13,14],[14,15],[15,16],
    [13,17],[17,18],[18,19],[19,20],[17,0]
  ];

  function dist(a, b) {
    return Math.hypot(a.x - b.x, a.y - b.y, (a.z || 0) - (b.z || 0));
  }

  function angle(a, b, c) {
    const ab = {x:a.x-b.x, y:a.y-b.y, z:(a.z||0)-(b.z||0)};
    const cb = {x:c.x-b.x, y:c.y-b.y, z:(c.z||0)-(b.z||0)};
    const dot = ab.x*cb.x + ab.y*cb.y + ab.z*cb.z;
    const mag = Math.hypot(ab.x,ab.y,ab.z) * Math.hypot(cb.x,cb.y,cb.z);
    if (!mag) return 0;
    return Math.acos(Math.max(-1, Math.min(1, dot / mag))) * 180 / Math.PI;
  }

  function isExtended(points, ids, isThumb) {
    const a = angle(points[ids[0]], points[ids[1]], points[ids[2]]);
    const b = angle(points[ids[1]], points[ids[2]], points[ids[3]]);
    const straightEnough = isThumb ? a > 130 && b > 135 : a > 145 && b > 145;
    const wristGain = dist(points[ids[3]], points[0]) > dist(points[ids[1]], points[0]) * (isThumb ? 1.05 : 1.12);
    return straightEnough && wristGain;
  }

  function analyze(points) {
    if (!points || points.length < 21) return null;
    const fingers = {};
    Object.keys(FINGERS).forEach(name => {
      fingers[name] = isExtended(points, FINGERS[name], name === 'thumb');
    });
    const extendedCount = Object.values(fingers).filter(Boolean).length;
    const palmWidth = Math.max(0.001, dist(points[5], points[17]));
    const pinch = dist(points[4], points[8]) / palmWidth;
    const palmIds = [0, 5, 9, 13, 17];
    const center = palmIds.reduce((acc, id) => ({x:acc.x+points[id].x, y:acc.y+points[id].y}), {x:0,y:0});
    center.x /= palmIds.length;
    center.y /= palmIds.length;

    let gesture = {id:'tracking', name:'أتتبع الحركة', emoji:'🖖'};
    if (pinch < 0.38 && (fingers.middle || fingers.ring || fingers.pinky)) {
      gesture = {id:'ok', name:'ممتاز', emoji:'👌'};
    } else if (extendedCount >= 4) {
      gesture = {id:'open', name:'كف مفتوح', emoji:'🖐️'};
    } else if (extendedCount <= 1 && pinch > 0.55) {
      gesture = {id:'fist', name:'قبضة', emoji:'✊'};
    } else if (fingers.index && fingers.middle && !fingers.ring && !fingers.pinky) {
      gesture = {id:'victory', name:'إصبعان', emoji:'✌️'};
    } else if (fingers.thumb && fingers.index && fingers.pinky && !fingers.middle && !fingers.ring) {
      gesture = {id:'love', name:'إشارة 🤟', emoji:'🤟'};
    }
    return {fingers, extendedCount, pinch, center, palmWidth, gesture};
  }

  const api = {FINGERS, CONNECTIONS, dist, angle, analyze};
  root.GestureCore = api;
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
})(typeof window !== 'undefined' ? window : globalThis);
