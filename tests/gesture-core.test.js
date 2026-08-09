const assert = require('assert');
const core = require('../app/src/main/assets/gesture-core.js');

assert.strictEqual(core.CONNECTIONS.length, 21, 'hand skeleton must have 21 connections');
assert.strictEqual(Object.keys(core.FINGERS).length, 5, 'five fingers are defined');
assert(Math.abs(core.dist({x:0,y:0},{x:3,y:4}) - 5) < 1e-9, 'distance works');
assert(Math.abs(core.angle({x:0,y:0},{x:1,y:0},{x:2,y:0}) - 180) < 1e-9, 'angle works');
assert.strictEqual(core.analyze([]), null, 'invalid landmark arrays are rejected');

console.log('gesture-core tests passed');
